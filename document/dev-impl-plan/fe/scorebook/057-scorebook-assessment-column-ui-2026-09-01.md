# Developer Plan 057: Scorebook & Assessment Column UI

## Trạng thái và phiên bản áp dụng

- Application-document version: `v2`.
- Status: `Amendment 57.1 approved and implemented; frontend/visual QA passed; backend PMD/build blocked outside amendment scope`.
- Ngày lập plan: `2026-09-01`.
- Phạm vi: Frontend Vue 3/TypeScript/PrimeVue.
- Wireframe: [`document/wireframes/fe/scorebook/057-scorebook-assessment-column-ui/index.html`](../../../wireframes/fe/scorebook/057-scorebook-assessment-column-ui/index.html).
- Wireframe chỉ phục vụ review, không phải production code và không đồng nghĩa plan đã được phê duyệt.

## 1. Mục tiêu

Xây dựng một workspace FE v2 để giáo viên/giáo vụ thao tác sổ điểm theo đúng
`classSubject`, lớp và học kỳ; tập trung vào lifecycle scorebook, cấu hình
`AssessmentColumn` và nhập điểm trên score grid.

Kết quả mong muốn:

1. Chọn đúng context học vụ và class-subject trước khi tải sổ điểm.
2. Tạo/mở scorebook bằng API hiện có và hiển thị rõ `DRAFT`, `OPEN`,
   `PUBLISHED`, `CLOSED`.
3. Tạo, đổi tên và vô hiệu hóa assessment column theo đúng loại `KTTT`, `KTĐK`,
   `KTCK`.
4. Nhập điểm từng học sinh hoặc hàng loạt; giữ đúng score status và optimistic
   version.
5. Phân biệt ô chưa nhập với điểm `0`; backend vẫn là nguồn validation,
   authorization và calculation chính thức.

## 2. Requirement và nguồn đối chiếu

- `document/application-doc/v2/ApplicationContext.md` và
  `RequirementBaseline.md`.
- `modules/04-AssessmentAndScoringModule.md`: `FR-SCORE-001..008`,
  `BR-SCORE-001..016`, `FR-SKILL-001..003`, `BR-SKILL-001..007`.
- `modules/05-ScoreChangeAndCalculationModule.md`: lifecycle/calculation boundary;
  Plan 057 không triển khai score-change workflow.
- `modules/07-AccessQualityAndAcceptanceModule.md`: quyền theo assignment,
  usability và acceptance liên quan score entry.
- `data-model/05-AssessmentAndScores.md`: scorebook, assessment column,
  student score và version.
- `frontend-api/05-scorebook-change-audit.md` và
  `frontend-api/07-enums-and-known-drift.md`.
- `FE/AGENTS.override.md`, `FE/agent-rules/00-foundation.md`,
  `02-domain-rules.md`, `03-api-data-boundaries.md`,
  `04-quality-documentation.md`.
- Backend implementation đã đối chiếu: `ScorebookController`,
  `ScoreEntryController` và response/request DTO tương ứng.

## 3. Phạm vi

### 3.1. In-scope

- Route authenticated `/v2/scorebooks`, name `v2-scorebooks`.
- Navigation tĩnh trong shell v2; không suy role/capability từ login response.
- Context gồm academic year, semester, class và class-subject/subject.
- Tạo scorebook bằng `{ classSubjectId }` khi context chưa có scorebook và API cho
  phép; tải scorebook bằng `scorebookId` khi đã có id.
- Lifecycle action: create, open, publish; không có close endpoint trong contract
  hiện tại nên không tạo nút close giả.
- Assessment-column list và action create/update/deactivate.
- Skill-weight panel chỉ hiển thị khi response/context xác định subject là `SKILL`;
  nếu dữ liệu context không đủ để xác định an toàn, phần này là `TBD/blocked`.
- Paged score grid, mặc định page `0`, size `10`; dynamic columns theo response.
- Single score entry và bulk score entry theo một assessment column.
- `SCORED`, `ABSENT`, `EXEMPTED`, `CANCELLED`; điểm `0.0` hợp lệ.
- Missing key trong `scores` map được trình bày là “Chưa nhập”; không thêm
  `NOT_ENTERED` vào wire enum.
- Dùng `expectedVersion` khi sửa score đã có version; `409` yêu cầu reload, không
  ghi đè im lặng.
- Loading, empty, validation, forbidden, not-found, conflict và retryable error.
- Responsive table với local horizontal scroll và dialog usable trên viewport nhỏ.

### 3.2. Out-of-scope

- Backend endpoint/DTO/schema/migration/authorization change.
- Score-change request, approve/reject/cancel và audit-log screen.
- Transcript, retake, calculation-task/retry/recalculate screen.
- Tính `Đtbmh`, skill score, `Đtbhk`, `ĐtbmhCN`, `Đtbcn` trong frontend.
- CSV import/export, offline editing, spreadsheet paste nâng cao.
- Role-aware navigation khi auth contract chưa expose role/capability.
- Student self-service transcript/scorebook view.

## 4. Kiến trúc và screen flow

```text
ScorebookWorkspaceView
├── ScorebookContextPanel
├── ScorebookStatusHeader
├── AssessmentColumnPanel
│   └── AssessmentColumnDialog
├── ScoreGrid
│   └── ScoreEntryDialog
└── scorebookApi
```

Dependency direction:

```text
view orchestration -> typed component props/emits
view orchestration -> typed scorebookApi -> backend API
```

Child components không tự tải page data. Khi context thay đổi, view reset
scorebook, grid, page, selected cell, dialog và error cũ trước khi tải context mới.
Response cũ trả về sau khi context đã đổi phải bị bỏ qua.

## 5. Bố cục và interaction theo wireframe

Workspace có hai tab chính:

1. `Bảng điểm`: context, lifecycle header, paged score grid, cell status và dialog
   nhập điểm.
2. `Cấu hình cột`: summary cấu trúc, column table, create/edit/deactivate và
   skill-weight card có điều kiện.

Lifecycle/action matrix dự kiến:

| Status | Xem cấu hình | Sửa cột | Nhập điểm | Publish |
|---|---:|---:|---:|---:|
| `DRAFT` | Có | Có theo backend | Không cho đến khi open | Không/validation backend |
| `OPEN` | Có | Có theo backend | Có | Có theo backend |
| `PUBLISHED` | Có | Read-only mặc định | Không sửa trực tiếp | Không |
| `CLOSED` | Có | Không | Không | Không |

Matrix này là UX guard; backend vẫn quyết định cuối cùng và có thể trả `403/409`.

## 6. API contract sử dụng

### Scorebook lifecycle/config

```text
POST   /api/v2/scorebooks
GET    /api/v2/scorebooks/{scorebookId}
POST   /api/v2/scorebooks/{scorebookId}/open
POST   /api/v2/scorebooks/{scorebookId}/columns
PUT    /api/v2/assessment-columns/{columnId}
DELETE /api/v2/assessment-columns/{columnId}
PUT    /api/v2/scorebooks/{scorebookId}/skill-weight
POST   /api/v2/scorebooks/{scorebookId}/publish
```

### Score grid/mutation

```text
GET  /api/v2/scorebooks/{scorebookId}/score-entries?page=&size=
PUT  /api/v2/assessment-columns/{columnId}/students/{studentId}/score
POST /api/v2/assessment-columns/{columnId}/scores/bulk
```

Canonical FE wire values:

```ts
type AssessmentType = 'KTTT' | 'KTĐK' | 'KTCK'
type ScoreStatus = 'SCORED' | 'ABSENT' | 'EXEMPTED' | 'CANCELLED'
type ScorebookStatus = 'DRAFT' | 'OPEN' | 'PUBLISHED' | 'CLOSED'
```

## 7. Validation và business-boundary UI

- `SCORED` yêu cầu `scoreValue` trong `0..10`; giá trị `0` không bị coi là trống.
- Status khác `SCORED` gửi `scoreValue: null`.
- Note tối đa 500 ký tự; column name tối đa 100 ký tự.
- Create column yêu cầu `assessmentType`, `columnNo > 0`; update contract hiện chỉ
  cho phép đổi `columnName`.
- Column deactivate dùng confirmation và reload scorebook/grid sau `204`.
- FE có thể hiển thị cấu trúc còn thiếu trước publish, nhưng không thay thế
  `ScorebookConfigurationValidator` của backend.
- Không tính official average trong browser và không gọi GET để kích hoạt tính lại.
- `401` dùng shared auth flow; `403` giữ session và hiển thị access denied;
  `404` hiển thị resource/context không tồn tại; `409` hiển thị conflict và reload.

## 8. File/khu vực dự kiến thay đổi sau approval

| File/khu vực | Mục đích |
|---|---|
| `FE/src/types/scorebook.ts` | DTO, enum, request và editable state |
| `FE/src/services/scorebookApi.ts` | Typed API wrapper |
| `FE/src/services/scorebookApi.spec.ts` | Path/query/body/token/204 tests |
| `FE/src/router/index.ts` | Route `/v2/scorebooks` |
| `FE/src/views/AuthenticatedV2ShellView.vue` | Navigation tĩnh |
| `FE/src/views/ScorebookWorkspaceView.vue` | Context, lifecycle, tab và reload orchestration |
| `FE/src/components/ScorebookContextPanel.vue` | Academic/class-subject context |
| `FE/src/components/ScorebookStatusHeader.vue` | Status/action summary |
| `FE/src/components/AssessmentColumnPanel.vue` | Column table và structure state |
| `FE/src/components/AssessmentColumnDialog.vue` | Create/update column form |
| `FE/src/components/ScoreGrid.vue` | Dynamic score grid và paging |
| `FE/src/components/ScoreEntryDialog.vue` | Single score editor |
| `FE/src/components/BulkScoreEntryDialog.vue` | Bulk items cho một column, nếu interaction được duyệt |
| `FE/src/**/*.spec.ts`, `*.stories.ts` | Deterministic test/story states |
| `FE/src/styles.css` | Chỉ CSS responsive/cục bộ cần thiết |
| `document/dev-note/fe/scorebook/057-...md` | Tạo sau implementation |

Không sửa backend, migration, Postman hoặc v1 screen trong plan này.

## 9. Test và validation dự kiến sau approval

### Service tests

- Serialize đúng lifecycle, column, score-grid page/size, single/bulk body.
- Gửi canonical `KTĐK`, không vô tình gửi `KTDK`.
- Giữ điểm `0`, nullable value theo status và `expectedVersion`.
- Xử lý `204 No Content` mà không parse JSON.

### View/component tests

- Đổi context reset state và bỏ qua stale response.
- Missing score map entry hiển thị “Chưa nhập”; score `0` hiển thị `0.0`.
- Dynamic columns giữ thứ tự `assessmentType`/`columnNo` từ backend.
- Lifecycle guard, confirm deactivate/publish và reload sau mutation.
- Validation score, status, note, column name/number và bulk item.
- Loading, empty, `401`, `403`, `404`, `409`, network error và pagination.
- Storybook không cần live backend; có state `DRAFT`, `OPEN`, `PUBLISHED`, empty,
  forbidden, conflict, column dialog và score dialog.

### Quality gates

Sau implementation phải chạy đúng scripts hiện có:

```text
npm run lint
npm run test
npm run test:coverage
npm run build
npm run build-storybook
```

Chưa chạy các quality gate này trong phase plan/wireframe vì chưa có production
source change.

## 10. Rủi ro, assumption và blocker

- API hiện không có endpoint tìm scorebook theo `classSubjectId`. Nếu
  class-subject response không trả `scorebookId`, context không thể tự tải scorebook
  đã tồn tại; phải xác minh source/fixture trước implementation và báo blocker thay
  vì phát minh endpoint.
- Auth response chưa expose role/capability; navigation/action visibility không
  phải security boundary.
- Subject type phải có trong context để bật skill-weight đúng. Không suy luận skill
  subject từ tên môn.
- Backend assignment guard có thể từ chối teacher dù controller cho role
  `TEACHER`; UI phải giữ `403` rõ ràng.
- Grid phân trang theo server nên bulk dialog chỉ thao tác items người dùng nhập/chọn,
  không giả định đang có toàn bộ roster.
- Update conflict phải reload version; không tự retry mutation gây ghi đè.

## 11. Output mong đợi

- Một workspace `/v2/scorebooks` bám wireframe đã duyệt.
- Cấu hình cột và score grid dùng API thật, không phát minh wire contract.
- UI phân biệt score `0`, missing score, non-scored status và lifecycle.
- Typed service/component boundary, tests, Storybook và validation evidence sau
  khi plan được approve.

## 12. Approval gate

Plan 057 đã được người dùng approve qua agent message ngày 2026-09-01 và đã có
implementation checkpoint. Review ngày 2026-09-02 xác định checkpoint này chưa đáp
ứng đầy đủ scope/acceptance của plan. Phần Amendment 57.1 bên dưới supersede các
statement mâu thuẫn về trạng thái hoàn thành, blocker, role contract và validation.

Original approval không tự động phê duyệt Amendment 57.1. Chỉ bắt đầu remediation
sau khi người dùng xác nhận rõ `Approve Amendment 57.1` qua agent.

---

## 13. Amendment 57.1 — Remediation after implementation review

### 13.1. Lý do cập nhật

Static review và quality-gate review ngày `2026-09-02` ghi nhận production route đã
được tạo nhưng core workflow chưa hoàn chỉnh:

1. Existing scorebook không thể được tìm và mở lại sau reload/context change.
2. Score grid luôn ở page `0`, size `10`; chưa có điều khiển server pagination.
3. Context hiển thị technical `classSubjectId` thay vì tên/mã môn rõ ràng.
4. Bulk entry luôn chọn column đầu tiên và chỉ hỗ trợ `SCORED`.
5. `409` chỉ hiện lỗi, chưa reload authoritative score/version.
6. Score dialog thiếu import `InputText`, cho phép hai chữ số thập phân và có thể
   submit `SCORED` không có value.
7. Skill-weight UI, confirm publish/deactivate và dedicated error states chưa đủ.
8. Chưa có view/component tests; aggregate coverage không include module Scorebook.
9. Dev Note có statement mâu thuẫn giữa “production workspace đã triển khai” và
   “production workspace chưa triển khai ở checkpoint này”.

Quality gates `lint`, full `test`, aggregate `test:coverage`, production `build` và
`build-storybook` đã PASS tại thời điểm review, nhưng đây không phải bằng chứng cho
functional completeness hoặc Scorebook coverage vì module mới không nằm trong
coverage include.

### 13.2. Current codebase decisions và contract delta

#### Existing-scorebook lookup — backend prerequisite bắt buộc

Current backend có `ScorebookRepository.findByClassSubjectId(...)`, nhưng public API
chỉ đọc scorebook bằng `scorebookId`; `ResClassSubjectDTO` vẫn chỉ trả
`id`, `classId`, `subjectId`, `semesterId`, `status`.

Amendment đề xuất contract tối thiểu:

```text
GET /api/v2/scorebooks/by-class-subject/{classSubjectId}
Authorization: ADMIN | ACADEMIC_OFFICE | TEACHER
Response: 200 ResScorebookDTO khi tồn tại
          404 khi class-subject hợp lệ nhưng chưa có scorebook
```

- Controller chỉ expose lookup; service phải dùng guard/assignment scope hiện có.
- `404` là trạng thái “chưa có scorebook”, không phải network/server failure.
- Chỉ `ADMIN`/`ACADEMIC_OFFICE` được thấy action tạo mới; `TEACHER` không gọi create.
- Không dùng `POST /scorebooks` như lookup vì duplicate create hiện trả `409`.
- Không thêm `scorebookId` giả vào FE `ClassSubject` trước khi backend contract thay đổi.

Đây là mở rộng backend nhỏ nhưng bắt buộc để đạt mục tiêu route theo context. Nếu
backend delta không được duyệt, Plan 057 phải giữ trạng thái `BLOCKED` cho core
existing-scorebook workflow và không được báo completed.

#### Role/capability contract mới trong current worktree

Current worktree đã bổ sung `roles` vào `ResUserDTO` và `UserSummary`. Khi thay đổi
này được phê duyệt/merge, FE dùng `session.user.roles`, không đọc JWT và không suy
role từ username:

```text
ADMIN | ACADEMIC_OFFICE -> lookup, create, open, columns, weights, score entry, publish
TEACHER                 -> lookup, open, columns, weights, score entry, publish
                          nhưng vẫn phụ thuộc subject-teaching assignment backend
STUDENT                 -> không vào staff scorebook workspace của Plan 057
```

Nếu `roles` vắng mặt do session cũ/compatibility response, UI không hiển thị action
đặc quyền dựa trên suy đoán; có thể yêu cầu refresh account/login. Backend luôn là
authorization authority cuối cùng.

### 13.3. Remediation scope

#### Phase A — Contract lookup và context identity

- Thêm backend lookup endpoint nêu tại 13.2, service/controller tests và cập nhật
  `FrontendApiGuide` scorebook contract.
- Thêm `fetchScorebookByClassSubject(...)` trong `scorebookApi.ts`.
- Khi chọn class-subject: lookup scorebook; `200` tải scorebook/grid, `404` chuyển
  sang explicit empty state, các lỗi khác giữ semantics chuẩn.
- FE tải/mapping Subject catalog hiện có để label class-subject bằng
  `subject.code · subject.name`; class label dùng `classCode` và fallback
  `className`, không dùng technical id làm label chính.
- Context change reset grid/page/dialog/errors và bỏ qua stale responses.

#### Phase B — Lifecycle, capability và destructive confirmation

- Tạo action chỉ hiển thị cho Office role; teacher không gửi create request.
- `Mở sổ`, `Công bố`, edit/deactivate column, score mutation dùng lifecycle guard
  như UX aid; backend vẫn được phép trả `403/409`.
- Publish và deactivate column bắt buộc qua `ConfirmDialog`/shared confirmation.
- Mọi column mutation reload cả scorebook metadata và score grid.
- `PUBLISHED`/`CLOSED` hiển thị read-only rõ ràng; không tạo close action vì contract
  chưa có endpoint tương ứng.

#### Phase C — Paged score grid và complete score editing

- `ScorebookWorkspaceView` sở hữu `page`/`size`; `ScoreGrid` emit page change và tái
  sử dụng `ServerPagination` hiện có.
- Pagination dùng `grid.page`, `grid.size`, `grid.totalElements`, `grid.totalPages`;
  không local-page một response đã được server page.
- Bulk action có target assessment column rõ ràng: action tại column header hoặc
  selector column; không hard-code `columns[0]`.
- Bulk dialog prefill score/status/note/version của page hiện tại, chỉ gửi row đã
  thay đổi và hỗ trợ `SCORED | ABSENT | EXEMPTED | CANCELLED`.
- Existing score item gửi `expectedVersion`; new item gửi `null`/không gửi version
  theo backend contract.
- `409` không auto-retry mutation: hiển thị conflict, reload grid/scorebook, rebind
  selected cell bằng version mới và yêu cầu người dùng review lại trước khi lưu.

#### Phase D — Form correctness và skill weights

- Import/register đúng PrimeVue `InputText` trong `ScoreEntryDialog`.
- Single/bulk score chỉ cho tối đa một chữ số thập phân.
- Client validation bắt buộc `scoreValue` khi status là `SCORED`, range `0..10`,
  note tối đa 500; status khác `SCORED` phải gửi `scoreValue: null`.
- Assessment column validation: type bắt buộc, `columnNo` là integer dương,
  `columnName` tối đa 100; update chỉ gửi `columnName`.
- Xác định `SubjectType` từ Subject catalog bằng `subjectId`, không suy từ tên.
- Với subject `SKILL`, thêm `SkillWeightDialog/Panel`: ba weight trong `0..100`,
  tổng bằng `100`, `KTCK >= KTTT` và `KTCK >= KTĐK`; backend kiểm tra cuối cùng.

#### Phase E — Error states, test, coverage và documentation

- Tách loading/empty/forbidden/not-found/conflict/retryable-error theo vùng context,
  lifecycle, grid và dialog; không tái sử dụng stale global error trong dialog mới.
- Bổ sung service tests cho lookup và toàn bộ lifecycle/column/weight/single/bulk
  API methods, không chỉ pagination/by-code/delete.
- Bổ sung component tests cho context label, score 0/missing/status, one-decimal
  validation, column selection, pagination, confirmation và skill weights.
- Bổ sung view tests cho lookup `200/404`, role capability, context reset/stale
  response, page changes, lifecycle reload, `401/403/404/409` và retry.
- Cập nhật `vite.config.ts` coverage include để Scorebook view/components/service
  thực sự xuất hiện trong report; không dùng aggregate coverage cũ làm bằng chứng.
- Sửa Dev Note theo thay đổi thực tế sau implementation; bỏ statement mâu thuẫn và
  ghi từng quality gate đúng trạng thái.
- Visual QA bắt buộc trên Storybook và production route ở desktop/mobile. Nếu
  browser MCP không khả dụng thì ghi `NOT RUN`; không kết luận UI completed.

### 13.4. File/khu vực dự kiến thay đổi cho Amendment 57.1

| File/khu vực | Thay đổi dự kiến |
|---|---|
| `BE/.../scorebook/controller/ScorebookController.java` | Lookup by class-subject |
| `BE/.../scorebook/service/ScorebookService.java` và lifecycle/guard liên quan | Resolve existing scorebook có scope |
| `BE/.../scorebook/controller/*Test.java`, `service/*Test.java` | Lookup success/not-found/forbidden |
| `document/application-doc/v2/frontend-api/05-scorebook-change-audit.md` | Ghi contract lookup mới |
| `FE/src/types/scorebook.ts` | Query/UI state nếu cần, không bịa DTO field |
| `FE/src/services/scorebookApi.ts` và `.spec.ts` | Lookup + complete API tests |
| `FE/src/views/ScorebookWorkspaceView.vue` và `.spec.ts` | Orchestration, capability, page, conflict reload |
| `FE/src/components/ScorebookContextPanel.vue` và spec | Business labels |
| `FE/src/components/ScoreGrid.vue` và spec | Pagination + per-column bulk action |
| `FE/src/components/ScoreEntryDialog.vue` và spec | Import + one-decimal/required validation |
| `FE/src/components/BulkScoreEntryDialog.vue` và spec | Status/value/version/change tracking |
| `FE/src/components/AssessmentColumnPanel.vue`, dialog và specs | Confirm + validation + reload |
| `FE/src/components/SkillWeightPanel.vue` hoặc dialog + spec | SKILL-only weights |
| `FE/vite.config.ts` | Include Scorebook files trong coverage |
| `FE/src/components/ScorebookWorkspaceReview.*` | Đồng bộ fixtures/states sau remediation |
| `document/dev-note/fe/scorebook/057-...md` | Factual completion note sau validation |

Không sửa score-change request, audit-log screen, transcript, retake, calculation
operations hoặc official average calculation trong Amendment này.

### 13.5. Acceptance criteria cập nhật

- `AC-057.1-01`: Chọn class-subject có scorebook -> lookup trả `200`, UI mở đúng
  existing scorebook sau reload, không gửi duplicate create.
- `AC-057.1-02`: Chọn class-subject chưa có scorebook -> Office thấy create;
  Teacher thấy empty/no-create state; không biến `404` thành generic failure.
- `AC-057.1-03`: Context hiển thị mã/tên lớp và mã/tên môn; không yêu cầu người dùng
  chọn bằng technical id.
- `AC-057.1-04`: Có thể đi tới page 2+ và nhập điểm cho học sinh ngoài 10 row đầu.
- `AC-057.1-05`: Bulk entry chọn được mọi active column trên page và hỗ trợ đủ bốn
  score statuses với value/version hợp lệ.
- `AC-057.1-06`: Điểm `0.0` lưu được; missing entry vẫn là “Chưa nhập”; `1.25`,
  `SCORED + null` và non-scored + value bị chặn trước request.
- `AC-057.1-07`: Version conflict hiển thị `409`, reload authoritative value/version
  và không tự ghi đè hoặc tự retry mutation.
- `AC-057.1-08`: Publish/deactivate cần confirmation; column mutation refresh metadata
  và grid.
- `AC-057.1-09`: Skill subject hiển thị/cập nhật weight hợp lệ; academic subject
  không hiển thị skill-weight form.
- `AC-057.1-10`: `401`, `403`, `404`, `409`, empty và network failure có state đúng;
  `403` không xóa authenticated session.
- `AC-057.1-11`: Targeted service/view/component tests PASS và coverage report liệt
  kê các Scorebook files mới.
- `AC-057.1-12`: Storybook và production UI được click-through ở desktop/mobile;
  nếu visual QA chưa chạy thì status không được ghi completed.

### 13.6. Validation bắt buộc sau remediation

Backend lookup delta:

```text
./gradlew.bat test
./gradlew.bat checkstyleMain
./gradlew.bat checkstyleTest
./gradlew.bat pmdMain
./gradlew.bat pmdTest
./gradlew.bat build
```

Frontend:

```text
npm.cmd run lint
npm.cmd run test
npm.cmd run test:coverage
npm.cmd run build
npm.cmd run build-storybook
```

Ngoài full suite, báo riêng targeted Scorebook test count và coverage của module.
Mọi command chưa chạy hoặc visual QA chưa thực hiện phải ghi `NOT RUN/BLOCKED`,
không ghi `PASS` hoặc `completed`.

### 13.7. Approval gate cho Amendment 57.1

- Trạng thái hiện tại: `Approved on 2026-09-02; implementation delivered; frontend and visual QA passed; backend quality gate blocked outside amendment scope`.
- Approval cũ của Plan 057 chỉ chứng minh original scope đã được phép triển khai;
  không cho phép backend lookup delta hoặc remediation mới.
- Người dùng đã approve Amendment 57.1; Phase A -> E đã được triển khai và ghi nhận trong Dev Note 057.
- Không đánh dấu `COMPLETED` cho đến khi backend PMD/build PASS; production workspace visual QA desktop/mobile đã PASS với authenticated staff session/live backend.
- Nếu lookup contract hoặc role contract thay đổi khác đề xuất, dừng phần ảnh hưởng,
  cập nhật amendment và chờ phê duyệt lại.
