# Developer Plan: FE Source Refactor

## Mục tiêu

Refactor `FE/src` theo application-document v2 để giảm độ phức tạp của
frontend, làm rõ ranh giới giữa view, component, service, type và utility,
đồng thời giữ nguyên behavior nghiệp vụ, API contract và visual contract hiện
tại.

Plan này là refactor kỹ thuật theo từng vertical slice. Không tạo business
feature mới, không đổi endpoint hoặc tự suy diễn contract còn thiếu.

## Requirement và nguồn đối chiếu

- Application version áp dụng: `v2`.
- `document/application-doc/v2/ApplicationContext.md` — source precedence,
  backend authorization, session và kiến trúc logic.
- `document/application-doc/v2/RequirementBaseline.md` — baseline và DoD.
- `document/application-doc/v2/modules/07-AccessQualityAndAcceptanceModule.md`
  — quality, access, usability, status và acceptance scenarios.
- `document/application-doc/v2/FrontendApiGuide.md` — typed service boundary,
  error, date, pagination và calculation integration.
- `document/application-doc/v2/frontend-api/00-common-contract.md` — wire
  envelope, status semantics và auth transport.
- `document/application-doc/v2/frontend-api/07-enums-and-known-drift.md` —
  canonical wire enum và các contract drift không được tự sửa ở FE.
- `FE/AGENTS.override.md` cùng `FE/agent-rules/00-foundation.md`,
  `01-auth-routing-security.md`, `02-domain-rules.md`,
  `03-api-data-boundaries.md` và `04-quality-documentation.md`.

## Hiện trạng đã khảo sát

- `FE/src` có khoảng 297 file, hơn 34.000 dòng.
- Có khoảng 100 Vue components, 85 unit specs và 67 Storybook stories.
- Các view lớn nhất gồm:
  - `StudentDetailView.vue`: khoảng 1.045 dòng.
  - `CalculationOperationsView.vue`: khoảng 802 dòng.
  - `RetakeResultView.vue`: khoảng 748 dòng.
  - `ScorebookWorkspaceView.vue`: khoảng 738 dòng.
  - `TranscriptViewerView.vue`: khoảng 695 dòng.
  - `AttendanceWorkspaceView.vue`: khoảng 663 dòng.
- `FE/src/styles.css` khoảng 1.347 dòng và đang chứa global, layout, form,
  table, academic, attendance, enrollment và Storybook styles.
- Nhiều view lặp việc lấy token, xử lý `401/403`, loading/error state,
  pagination, academic context và date formatting.
- `apiClient.ts` đã là HTTP boundary đúng; các service hiện đã typed nhưng
  còn được tổ chức phẳng.
- Full Vitest hiện có baseline failure tại `apiClient.spec.ts` liên quan
  behavior `403`; failure này phải được phân biệt với regression do refactor.

## Nguyên tắc tương thích

- Backend tiếp tục là nguồn quyết định cuối cùng về authorization, validation,
  lifecycle, calculation và audit.
- Giữ nguyên API path, request/response type, wire enum, route path và status
  semantics hiện tại.
- Không thêm endpoint, DTO field, enum hoặc role claim mới.
- Không tính official average trong frontend và không thay đổi flow
  `IN_PROGRESS`/`FINISH`.
- Giữ nguyên `401` clear session/redirect và `403` giữ session/access denied.
- Không thêm Pinia/Vuex, Axios, Tailwind, Bootstrap hoặc UI framework mới.
- Không xem menu ẩn hoặc route guard là authorization thay thế backend.
- Không thay đổi Student legacy chỉ để làm lại architecture; chỉ sửa khi cần
  giữ compatibility với shared boundary và có regression test.

## Phạm vi

### 1. Baseline và characterization

Trước mỗi nhóm refactor, ghi nhận behavior và chạy các kiểm tra liên quan.

Bổ sung hoặc củng cố coverage cho:

- `401` clear session và redirect.
- `403` giữ session.
- `404/409` hiển thị đúng trạng thái.
- server-side pagination.
- date-only `yyyy-MM-dd` không bị lệch timezone.
- điểm `0` khác với ô chưa nhập.
- transcript `IN_PROGRESS` không hiển thị như kết quả mới nhất.
- redirect và giới hạn workspace Student/Teacher hiện tại.

Failure đã có trước refactor phải được ghi nhận riêng, không tự gán là
regression. Nếu một phase chạm trực tiếp behavior đó thì test contract phải
được xử lý trong cùng phase.

### 2. Chuẩn hóa foundation dùng chung

Khu vực dự kiến:

- `FE/src/types/api.ts`
- `FE/src/types/ui.ts`
- `FE/src/services/apiClient.ts`
- `FE/src/services/authSession.ts`
- `FE/src/router/index.ts`
- `FE/src/services/studentNavigation.ts`
- `FE/src/utils/*`
- `FE/src/components/PageState.vue`
- `FE/src/components/FormAlert.vue`
- `FE/src/components/EmptyState.vue`
- `FE/src/components/ServerPagination.vue`

Thực hiện:

- Chuẩn hóa helper hiển thị `ApiError`, giảm việc dùng trực tiếp
  `error.message` không nhất quán.
- Chuẩn hóa cách view lấy session/token nhưng giữ nguyên storage contract.
- Hợp nhất date formatter trùng logic thành boundary rõ ràng; giữ wrapper
  tương thích cho Student, calculation task và score-change request.
- Chuẩn hóa việc dùng shared state components cho loading, empty, forbidden,
  validation, conflict và success.
- Rà soát route guard/menu policy để logic không bị lặp giữa router và shell.
- Không tạo generic repository hoặc generic API abstraction che giấu HTTP
  semantics.

### 3. Refactor theo vertical slice

Không di chuyển toàn bộ 182 component trong một lần. Chỉ nhóm file theo domain
khi file đó được chạm tới, dùng explicit imports và tránh barrel gây vòng phụ
thuộc.

Thứ tự đề xuất:

1. Academic catalog và các list view đơn giản: năm học, học kỳ, khối, lớp,
   môn học và class-subject.
2. Student workspace: list, form và detail. `StudentDetailView.vue` được tách
   thành profile, enrollment history, attendance history, term transcript,
   annual transcript và calculation status.
3. Attendance và transcript: tách context selection, session/history/summary
   tabs và transcript status, giữ nguyên semantics dữ liệu.
4. Enrollment và teacher: giữ nguyên transfer, assignment scope, capacity
   warning và teacher read-only.
5. Scorebook, score change, retake và calculation: thực hiện sau cùng vì có
   rủi ro cao hơn về optimistic version, status, mutation và background task.

Sau từng slice:

- view chỉ giữ route parameter, orchestration, tab state và điều phối dữ liệu;
- component nhận typed props và phát typed emits;
- API call nằm trong service hoặc composable có boundary rõ ràng;
- DTO API được tách khỏi editable form state khi semantics khác nhau;
- test và Storybook được di chuyển/cập nhật cùng source;
- không chuyển business calculation hoặc authorization vào presentation layer.

### 4. Tách CSS có kiểm soát

Tách `FE/src/styles.css` theo trách nhiệm thành các nhóm global/base, layout,
shared form/table/state, academic/enrollment, attendance, scorebook/transcript
và Storybook-only surface.

Giữ `styles.css` làm entrypoint import trong giai đoạn chuyển tiếp. Không đổi
palette, spacing hoặc layout hàng loạt. Giữ nguyên class hiện tại trước khi
thực hiện cleanup tên class ở plan riêng.

### 5. Test và Storybook

- Giữ toàn bộ story hiện có khi component được refactor.
- Story mới chỉ được thêm cho state materially khác nhau, không tạo máy móc
  cho mọi page.
- Storybook không được gọi backend thật.
- Tập trung test vào state loading, empty, error, forbidden, validation,
  conflict, retry và background processing.

## Out-of-scope

- Backend, database, migration hoặc API contract.
- Business feature v2 mới hoặc route/view placeholder.
- Bổ sung role/capability contract và sidebar role-aware hoàn chỉnh.
- Thay đổi quyền nghiệp vụ, lifecycle hoặc redirect behavior hiện tại.
- Thay thế UI framework hoặc đưa state management library mới vào project.
- Redesign visual toàn bộ.
- Tự tính điểm trung bình hoặc tự suy diễn enum/field còn thiếu.
- Sửa baseline failure không liên quan, trừ khi phase trực tiếp chạm behavior đó.

## Files/khu vực dự kiến thay đổi

### Foundation

- `FE/src/services/apiClient.ts` và `apiClient.spec.ts`.
- `FE/src/services/authSession.ts`.
- `FE/src/services/studentNavigation.ts`.
- `FE/src/router/index.ts` và test liên quan.
- `FE/src/types/api.ts`, `FE/src/types/ui.ts`.
- `FE/src/utils/*` và test formatter liên quan.

### Shared components

- `FE/src/components/PageState.*`.
- `FE/src/components/EmptyState.*`.
- `FE/src/components/StatusTag.*`.
- `FE/src/components/FormAlert.*`.
- `FE/src/components/ServerPagination.*`.
- `FE/src/components/ConfirmAction.*`.
- `FE/src/components/AuthenticatedLayout.*` nếu cần để giữ shell contract.

### Vertical slices và style

- Các view/component thuộc academic, student, attendance, transcript,
  enrollment, teacher, scorebook, retake và calculation theo từng phase.
- `FE/src/styles.css` và các stylesheet con nếu việc tách CSS được duyệt
  trong phase tương ứng.
- `FE/README.md` chỉ khi hướng dẫn foundation hiện tại không còn đúng.

Danh sách trên là vùng dự kiến. Khi triển khai phải kiểm tra lại từng file và
không tạo file placeholder không có behavior hoặc test rõ ràng.

## Validation

Theo script hiện có trong `FE/package.json`, trước khi báo hoàn tất phải chạy:

```bash
npm run lint
npm run test
npm run test:coverage
npm run build
npm run build-storybook
git diff --check
```

Ngoài ra:

- chạy focused tests theo từng vertical slice;
- kiểm tra Storybook state deterministic;
- kiểm tra browser desktop/mobile cho login, academic list, Student detail,
  attendance, scorebook và transcript khi phase tương ứng hoàn tất;
- không báo PASS nếu quality gate bị fail hoặc browser validation chưa chạy;
- không sửa thủ công generated coverage/report artifacts.

## Acceptance criteria

- View lớn được tách thành các vùng có trách nhiệm rõ ràng và dễ test.
- Không còn raw API call trong presentation component.
- Logic auth/error/date không tiếp tục nhân bản không cần thiết trong view.
- Không thay đổi endpoint, DTO, enum, backend permission hoặc calculation
  behavior.
- Component bị ảnh hưởng vẫn giữ được Storybook coverage phù hợp.
- Test suite không có regression mới; baseline failure được ghi rõ nếu chưa
  xử lý trong scope.
- Mỗi quality gate có kết quả thực tế `PASS`, `FAIL`, `BLOCKED` hoặc `NOT RUN`.
- Có Dev Note sau implementation, ghi đúng files, validation, deviation và
  risk còn lại.

## Trạng thái triển khai

- Plan status: `Completed; frontend source refactor scope and domain directory reorganization implemented and validated`.
- User approval: đã nhận được trước implementation và tái xác nhận qua chỉ dẫn hoàn thiện gom nhóm file.
- Đã hoàn tất:
  - Auth/session boundary, date formatter boundary, error-message cleanup.
  - Student Detail 4-tab presentation split, Teaching Assignment auth migration và Attendance session/report orchestration split.
  - Scorebook dialog state, Calculation retry state, Retake dialog state và Transcript tab state được tách thành typed composables.
  - Tách CSS theo domain: foundation, academic/enrollment, attendance, transcript, scorebook, calculation, retake và responsive stylesheet.
  - Tái cấu trúc toàn diện các thư mục phẳng gom quá nhiều file (`FE/src/components` và `FE/src/views`) thành các thư mục domain riêng biệt (`academic`, `attendance`, `auth`, `calculation`, `common`, `enrollment`, `retake`, `scorebook`, `score-change`, `student`, `teacher`, `transcript`, `shell`), kèm cập nhật router dynamic imports, vite coverage include patterns và colocation của component/spec/stories.
- Hoàn tất theo scope: views giữ orchestration/lifecycle cần thiết, typed presentation, dialog/context state, API-error/date/session boundaries, CSS và file tree đã được phân bổ chuẩn mực theo responsibility và domain.
- Dev Note triển khai: `document/dev-note/fe/tooling/072-frontend-source-refactor-2026-09-08.md`.
