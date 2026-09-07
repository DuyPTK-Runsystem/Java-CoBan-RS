# Developer Plan: Academic Year & Semester UI

## Trạng thái phê duyệt

- Application version: `v2`.
- Status: `Approved; implementation is being delivered in incremental steps`.
- Implementation status: `Completed for the approved frontend scope`.
- Wireframe đã được lưu trong repository để review theo yêu cầu trực tiếp của
  người dùng; việc lưu wireframe không đồng nghĩa phê duyệt plan hoặc cho phép
  triển khai FE source.
- Wireframe:
  [`document/wireframes/fe/academic/052-academic-year-semester/index.html`](../../../../wireframes/fe/academic/052-academic-year-semester/index.html).

## Mục tiêu

Xây dựng UI quản lý năm học và học kỳ trên authenticated v2 shell, gồm hai màn
danh sách; thao tác tạo/sửa năm học, tạo/sửa học kỳ và xem trạng thái học kỳ
được trình bày bằng dialog trên màn danh sách tương ứng. UI hiển thị và chuyển
trạng thái theo contract hiện có, đồng thời cho phép xem trạng thái khóa học kỳ
cùng báo cáo mức độ hoàn thành dữ liệu điểm.

UI phải dùng Vue 3, TypeScript, PrimeVue và shared API/state foundation của Plan
051. Backend tiếp tục là nguồn validation và authorization cuối cùng.

## Requirement và nguồn đối chiếu

- `document/application-doc/v2/ApplicationContext.md`.
- `document/application-doc/v2/RequirementBaseline.md`.
- `document/application-doc/v2/modules/01-AcademicStructureModule.md`:
  `FR-AY-001..007`, `BR-AY-001..006`, `FR-SEM-001..008` và
  `BR-SEM-001..009`.
- `document/application-doc/v2/modules/05-ScoreChangeAndCalculationModule.md`:
  `FR-SEM-009`, `BR-SEM-010..015`.
- `document/application-doc/v2/modules/07-AccessQualityAndAcceptanceModule.md`:
  quyền quản lý năm học/học kỳ, `NFR-PERFORMANCE-003..004`,
  `NFR-USABILITY-003..005` và `NFR-AUDITABILITY-006`.
- `document/application-doc/v2/FrontendApiGuide.md`.
- `document/application-doc/v2/frontend-api/00-common-contract.md`.
- `document/application-doc/v2/frontend-api/02-academic-structure.md`.
- `document/application-doc/v2/frontend-api/07-enums-and-known-drift.md`.
- `document/application-doc/v2/data-model/02-AcademicCatalog.md`.
- `FE/AGENTS.override.md` và các rule `00-foundation`,
  `01-auth-routing-security`, `02-domain-rules`, `03-api-data-boundaries`,
  `04-quality-documentation`.
- Backend controller/request/response DTO hiện tại trong
  `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/`.
- FE foundation hiện tại: `FE/src/services/apiClient.ts`,
  `FE/src/components/AuthenticatedLayout.vue`, shared state components,
  `FE/src/router/index.ts` và `FE/src/styles.css`.

## Hiện trạng và contract constraints

- FE chưa có type/service/view/component cho Academic Year hoặc Semester.
- `/v2` đã có authenticated shell và outlet từ Plan 051; chưa có business child
  route.
- API Academic Year chỉ cho `ADMIN` và `ACADEMIC_OFFICE`, trả danh sách không
  phân trang và không có query search/filter.
- API Semester list yêu cầu `academicYearId`; không có endpoint lấy riêng một
  Academic Year hoặc Semester theo ID. Direct route tới danh sách học kỳ phải
  load danh sách năm học rồi resolve Academic Year context; dialog edit/status
  chỉ mở từ entity đã có trong list state.
- Auth response/JWT chưa expose role/capability. FE không được suy luận role để
  ẩn menu hoặc chặn route; `403` giữ session và hiển thị access denied.
- Academic Year create/update DTO nhận status trực tiếp. Endpoint close riêng
  được dùng cho thao tác đóng có confirmation.
- Semester có endpoint lifecycle riêng cho activate, lock và reopen. UI không
  dùng update DTO để tạo transition tùy ý.
- `GET /completeness-report` trả report gần nhất hoặc live preview khi chưa có
  report persisted. Dữ liệu chưa hoàn chỉnh là warning, không chặn khóa.
- Semester response không expose `lockSource` và chỉ expose `lockedBy` dạng ID.
  FE không suy luận tên người thực hiện hoặc nguồn `MANUAL/AUTOMATIC`.
- Không có endpoint đóng Semester riêng. `CLOSED` chỉ được hiển thị read-only.

## Phạm vi in-scope

### 1. Typed Academic API boundary

Tạo `academicApi.ts` dùng shared `apiClient`, không gọi raw `fetch` từ view hoặc
component. Định nghĩa type wire chính xác cho:

- `AcademicYearStatus = DRAFT | ACTIVE | CLOSED`.
- `SemesterStatus = DRAFT | ACTIVE | LOCKED | CLOSED`.
- Academic Year create/update/response.
- Semester create/update/reopen/response.
- `SemesterLockReportStatus = COMPLETE | INCOMPLETE | FAILED`.
- Completeness report và summary counts/details.

Date-only giữ `yyyy-MM-dd`. `LocalDateTime` giữ local datetime string theo wire
contract; không dùng `Date.toISOString()` làm lệch ngày/giờ.

Service methods dự kiến:

| Method | Endpoint | FE use case |
|---|---|---|
| `GET` | `/api/v2/academic-years` | Danh sách và load entity cho edit/direct route |
| `POST` | `/api/v2/academic-years` | Tạo năm học |
| `PUT` | `/api/v2/academic-years/{id}` | Sửa năm học chưa đóng |
| `POST` | `/api/v2/academic-years/{id}/close` | Đóng năm học có confirmation |
| `GET` | `/api/v2/semesters?academicYearId={id}` | Danh sách học kỳ và load entity |
| `POST` | `/api/v2/semesters` | Tạo học kỳ, mặc định `DRAFT` |
| `PUT` | `/api/v2/semesters/{id}` | Sửa metadata học kỳ hợp lệ |
| `POST` | `/api/v2/semesters/{id}/activate` | `DRAFT -> ACTIVE` |
| `GET` | `/api/v2/semesters/{id}/completeness-report` | Preview/report trước khóa và màn lock status |
| `POST` | `/api/v2/semesters/{id}/lock` | `ACTIVE -> LOCKED` sau confirmation |
| `POST` | `/api/v2/semesters/{id}/reopen` | `LOCKED -> ACTIVE` với reason |

### 2. Routes và screen flow

Đăng ký child routes dưới `/v2`:

```text
/v2/academic-years
/v2/academic-years/:academicYearId/semesters
```

Thêm navigation tĩnh `Năm học & học kỳ` vào v2 shell. Không thêm role metadata
hoặc role guard khi contract auth chưa hỗ trợ. Form và trạng thái học kỳ là
dialog state do list view sở hữu; không tạo page route/view riêng và không đổi
URL chỉ để mở/đóng dialog.

### 3. Academic Year list và form

Danh sách:

- loading, empty, error, forbidden và success state qua shared components;
- tìm theo code và filter status cục bộ trên full list hiện tại;
- status tag tiếng Việt nhưng giữ wire value trong type/data;
- action theo trạng thái: xem học kỳ, sửa năm chưa `CLOSED`, đóng năm chưa
  `CLOSED` qua confirmation;
- không thêm delete vì không nằm trong scope người dùng yêu cầu.

Dialog dùng chung create/edit mở trên màn danh sách năm học:

- `code`, `startDate`, `endDate`, `status`, `notes`;
- nút tạo/sửa chỉ set dialog mode và selected Academic Year trong list view;
- đóng/hủy dialog giữ nguyên filter và dữ liệu danh sách hiện tại;
- create cho chọn `DRAFT` hoặc `ACTIVE`;
- edit chỉ cho năm chưa `CLOSED`; `CLOSED` hiển thị read-only;
- validate required, max length và `endDate > startDate` ở FE để phản hồi sớm;
- backend `400/409` vẫn là kết quả authoritative, gồm duplicate code và chỉ một
  Academic Year `ACTIVE`.

### 4. Semester list và form

Danh sách luôn có Academic Year context đã load:

- hiển thị code/name, display order, date range, automatic lock time và status;
- sort theo thứ tự API trả về; không local re-sort dữ liệu server một cách khác;
- action matrix:
  - `DRAFT`: edit, activate;
  - `ACTIVE`: edit, xem completeness/lock status, lock;
  - `LOCKED`: xem lock status, reopen;
  - `CLOSED`: view-only;
- confirmation cho activate và lock; reopen dùng dialog bắt buộc reason.

Dialog dùng chung create/edit mở trên màn danh sách học kỳ:

- `code`, `name`, `displayOrder`, `startDate`, `endDate`, `automaticLockAt`;
- đóng/hủy dialog giữ nguyên Academic Year context và dữ liệu danh sách;
- create không expose arbitrary status, để backend default `DRAFT`;
- edit không dùng field status để bypass lifecycle endpoints;
- validate required/positive/max length, date order và date nằm trong Academic
  Year; overlap vẫn do backend quyết định và trả `409`.

### 5. Semester status dialog

- Mở dialog từ Semester entity đã có trong list state; không điều hướng sang
  page route riêng.
- Load completeness report qua endpoint hiện có.
- Hiển thị status, `lockedAt`, `lockedBy` ID, `lockReason`, `reopenUntil`,
  `automaticLockAt` và lifecycle timeline.
- Hiển thị `COMPLETE`, `INCOMPLETE` hoặc `FAILED`, evaluated time, checkpoint,
  bảy summary count và `details` do backend trả về.
- `INCOMPLETE` là warning không chặn thao tác lock theo `BR-SEM-010`.
- Không tự tính completeness hoặc suy luận lock source/tên người thực hiện.
- Hỗ trợ lock từ ACTIVE và reopen từ LOCKED trong cùng dialog/list context.
- Đóng dialog giữ nguyên Academic Year context và vị trí màn danh sách học kỳ.

### 6. Component, test và Storybook

Component presentation dự kiến:

- `AcademicYearTable.vue`.
- `AcademicYearDialog.vue`.
- `SemesterTable.vue`.
- `SemesterDialog.vue`.
- `SemesterStatusDialog.vue`.
- Dialog/action state tái sử dụng `ConfirmAction`, `FormAlert`, `PageState` và
  `StatusTag` từ Plan 051 khi contract hiện có phù hợp.

Storybook chỉ dành cho reusable component/dialog state có giá trị review, dùng
dữ liệu deterministic và không gọi backend. Hai list view giữ orchestration,
route params, selected entity, dialog open/mode state, API calls và page state.

## Out-of-scope

- Backend, database, migration, requirement hoặc wire-contract changes.
- Delete Academic Year và các API quản lý Grade/Class/Subject.
- Semester notifications, dispatch/retry và completeness decision checkpoint.
- Role-aware navigation/route guard hoặc suy luận role từ JWT/username.
- Endpoint/user lookup giả để đổi `lockedBy` ID thành tên.
- Endpoint đóng Semester giả hoặc arbitrary status transition qua update DTO.
- Server-side search/filter/page khi backend chưa expose contract.
- Official score calculation ở frontend.
- Refactor legacy Student screens ngoài thay đổi navigation tối thiểu cần thiết.

## Khu vực/file dự kiến thay đổi khi được duyệt

```text
FE/src/types/academic.ts
FE/src/services/academicApi.ts
FE/src/services/academicApi.spec.ts
FE/src/components/AcademicYearTable.vue
FE/src/components/AcademicYearTable.spec.ts
FE/src/components/AcademicYearDialog.vue
FE/src/components/AcademicYearDialog.spec.ts
FE/src/components/SemesterTable.vue
FE/src/components/SemesterTable.spec.ts
FE/src/components/SemesterDialog.vue
FE/src/components/SemesterDialog.spec.ts
FE/src/components/SemesterStatusDialog.vue
FE/src/components/SemesterStatusDialog.spec.ts
FE/src/views/AcademicYearListView.vue
FE/src/views/SemesterListView.vue
FE/src/router/index.ts
FE/src/views/AuthenticatedV2ShellView.vue
FE/src/styles.css
FE/src/**/*.stories.ts (chỉ state cần thiết)
document/dev-note/fe/academic/052-academic-year-semester-ui-2026-08-27.md
```

Danh sách là vùng dự kiến. Implementation phải kiểm tra lại component boundary
để không tạo placeholder hoặc abstraction không cần thiết.

## Test plan và validation dự kiến

### Unit/service tests

- URL/query/body cho toàn bộ service method in-scope.
- Date/LocalDateTime serialization không timezone shift.
- Academic Year local search/filter và status action matrix.
- Academic Year dialog create/edit/closed read-only, validation date và
  close/cancel không làm mất list filter state.
- Semester route context, list order và action matrix theo status.
- Semester dialog date-in-year, overlap `409`, positive display order và
  close/cancel giữ Academic Year context.
- Lock preview/report `COMPLETE/INCOMPLETE/FAILED` và details.
- Semester status dialog, lock confirmation, reopen reason validation và
  `401/403/404/409` behavior.
- Direct route tới Semester list resolve Academic Year context và not-found state.

### Mandatory quality gates

```bash
cd FE
npm run lint
npm run test
npm run test:coverage
npm run build
npm run build-storybook
```

## Acceptance criteria

1. Người dùng authenticated mở được route Năm học & Học kỳ trong v2 shell.
2. Office user xem, tìm/lọc, tạo, sửa và đóng Academic Year đúng API contract;
   tạo/sửa dùng dialog trên màn danh sách.
3. Chỉ một Academic Year `ACTIVE`; conflict backend được hiển thị rõ.
4. Người dùng xem Semester theo đúng Academic Year context và direct URL hoạt
   động dù không có GET-by-ID endpoint.
5. Office user tạo/sửa Semester bằng dialog và thực hiện activate/lock/reopen
   đúng action matrix; không có arbitrary status mutation.
6. Trước lock, UI hiển thị completeness report/preview; incomplete là warning
   và không chặn lock.
7. Semester status dialog hiển thị metadata/report từ backend, không suy luận
   lock source, người dùng hoặc calculation.
8. `CLOSED` được phân biệt rõ là dữ liệu lịch sử/read-only.
9. `401` xóa session; `403` giữ session và hiển thị access denied.
10. Đóng/hủy dialog không đổi route, không mất filter/Academic Year list context.
11. Không có raw `fetch` trong presentation component; mandatory gates pass.

## Rủi ro và cách giảm thiểu

- Academic Year list không có server pagination/filter: dùng local filter chỉ
  vì endpoint trả full dataset; ghi rõ contract gap, không dựng query giả.
- Không có GET-by-ID: Semester list view load Academic Year list rồi resolve ID;
  các dialog chỉ dùng selected entity đã có, không hỗ trợ deep-link dialog giả.
- Auth chưa expose role: navigation tĩnh và backend authorization; không coi
  hidden action là security.
- Academic Year update nhận status trực tiếp: UI giới hạn transition được duyệt
  và dùng close endpoint riêng để tránh bypass confirmation.
- Semester DTO cho phép status trong update nhưng lifecycle có endpoint riêng:
  form không expose arbitrary status.
- `lockedBy` không có user summary và response không có `lockSource`: chỉ hiển
  thị dữ liệu thật, không suy đoán.
- Local datetime không có offset: giữ wire value/local form value, không tự
  chuyển UTC.
- Dialog gom nhiều behavior trong hai list view: tách component dialog typed
  props/emits, còn API call và selected entity state do view sở hữu để tránh
  presentation component gọi service trực tiếp.

## Approval status

- `Approved by user message; implementation is being delivered in incremental
  steps, starting with Storybook UI review artifacts`.
