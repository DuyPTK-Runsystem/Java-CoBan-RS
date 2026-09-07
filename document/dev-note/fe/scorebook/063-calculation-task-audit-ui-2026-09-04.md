# Dev Note 063 — Calculation Task & Audit UI

- **Developer Plan**: `document/dev-impl-plan/fe/scorebook/063-calculation-task-audit-ui-2026-09-03.md`
- **Approval**: Người dùng đã phê duyệt Plan 063 qua prompt / agent message (`approve plan 63`).
- **Ngày thực hiện**: `2026-09-04`.
- **Trạng thái implementation**: `Completed`.
- **Wireframe tham chiếu**: `document/wireframes/fe/scorebook/063-calculation-task-audit-ui/README.md` và `index.html`.

## 1. Phạm vi đã thực hiện

### 1.1. Typed API Boundary
- Tạo `FE/src/types/calculationTask.ts`:
  - Enums: `CalculationTaskStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'`, `CalculationTaskType = 'STUDENT_YEAR_RECALC'`.
  - DTO: `ResCalculationTaskDTO` (taskId, studentId, studentCode, academicYearId, taskType, requestedVersion, status, attemptCount, maxAttempts, availableAt, lockedAt, workerId, lastError, createdAt, startedAt, completedAt).
  - Request DTO: `ReqFilterCalculationTaskDTO` (status, studentId, studentCode, academicYearId, page, size).
  - Page wrapper: `CalculationTaskPage`.
- Tạo `FE/src/types/scoreAudit.ts`:
  - DTO: `ResScoreAuditLogDTO` (auditLogId, actorUserId, actorUsername, action, entityType, entityId, beforeData, afterData, requestId, ipAddress, occurredAt).
  - Request DTO: `ReqFilterScoreAuditLogDTO` (entityType, entityId, studentId, studentCode, action, actorUserId, fromOccurredAt, toOccurredAt, page, size).
  - Page wrapper: `ScoreAuditLogPage`.
- Cập nhật `FE/src/types/transcript.ts`:
  - Bổ sung các trường backend đầy đủ vào `ResTranscriptCalculationStatusDTO`: `studentCode`, `academicYearId`, `semesterId`, `isUpToDate`, `calculatedAt`, `lastError`.
- Tạo `FE/src/utils/calculationTaskDate.ts` & `calculationTaskDate.spec.ts`:
  - `formatCalculationDateTime` và `formatShortDateTime` định dạng ngày giờ chuẩn xác theo timezone `Asia/Ho_Chi_Minh` cho task và audit trail.

### 1.2. API Services & Unit Tests
- Tạo `FE/src/services/calculationTaskApi.ts`:
  - `fetchCalculationTasks`: `GET /api/v2/scorebooks/calculation-tasks` với query params phân trang và bộ lọc.
  - `fetchFailedCalculationTasks`: `GET /api/v2/scorebooks/calculation-tasks/failed` (canonical endpoint cho task lỗi).
  - `retryCalculationTask`: `POST /api/v2/scorebooks/calculation-tasks/{taskId}/retry`.
  - `retryAllFailedCalculationTasks`: `POST /api/v2/scorebooks/calculation-tasks/retry-all-failed`.
  - `recalculateTranscriptByCode`: `POST /api/v2/students/{studentCode}/transcripts/recalculate?academicYearId=...`.
  - `recalculateTranscriptById`: `POST /api/v2/students/{studentId}/transcripts/recalculate?academicYearId=...`.
  - Viết unit test `FE/src/services/calculationTaskApi.spec.ts` (5/5 tests passed).
- Tạo `FE/src/services/scoreAuditApi.ts`:
  - `fetchScoreAuditLogs`: `GET /api/v2/scorebooks/audit-logs` với query params phân trang và filter `occurredAt DESC`.
  - Viết unit test `FE/src/services/scoreAuditApi.spec.ts` (1/1 test passed).
- Cập nhật `FE/src/services/transcriptApi.ts`:
  - Bổ sung `fetchStudentTermStatus` và `fetchStudentAnnualStatus` cho vai trò staff truy vấn calculation status theo `studentId`.
  - Cập nhật unit test `FE/src/services/transcriptApi.spec.ts` (3/3 tests passed).

### 1.3. UI Components, Tests & Storybook Stories
- `CalculationTaskTable.vue` (+ `spec.ts`, `stories.ts`):
  - Bảng danh sách calculation task responsive, mặc định lọc `FAILED`.
  - Cột: Task ID (`#CT-...`), Học sinh (`studentCode`), Loại task (`STUDENT_YEAR_RECALC`), Trạng thái (Tag severity tương ứng: `FAILED` - danger, `PENDING` - warn, `RUNNING` - info, `SUCCEEDED` - success), Số lần thử (`attemptCount / maxAttempts`), Lỗi gần nhất, Thời gian tạo/hoàn thành, Thao tác (Chi tiết, Retry đơn).
  - Nút "Retry tất cả failed" tự động disable khi không có task FAILED hoặc đang chạy mutation, chống double click.
  - Tích hợp `ServerPagination` và `EmptyState`.
- `CalculationTaskDetailModal.vue` (+ `spec.ts`, `stories.ts`):
  - Modal hiển thị chi tiết task, thông số version protection (`requestedVersion`), worker ID, các mốc timestamps (`createdAt`, `startedAt`, `completedAt`, `availableAt`).
  - Hộp thông báo lỗi nổi bật (`lastError`) và nút retry trực tiếp nếu task đang `FAILED`.
- `RetryConfirmationModal.vue` (+ `spec.ts`, `stories.ts`):
  - Modal xác nhận trước khi thực hiện mutation retry (cả single task và bulk failed tasks).
  - Cảnh báo rõ việc đưa task về `PENDING`, worker xử lý bất đồng bộ, tính idempotent và xử lý xung đột `409 Conflict`.
- `TranscriptStatusCard.vue` (+ `spec.ts`, `stories.ts`):
  - Thẻ hiển thị trạng thái tính transcript của học sinh: `IN_PROGRESS` (thanh tiến trình động + cảnh báo chưa phải kết quả chính thức), `FINISH · up-to-date` (xanh lá), `FINISH · out-of-date` (vàng cam).
  - So sánh `sourceVersion` và `calculatedVersion`, hiển thị thời điểm tính và lỗi nếu có.
  - Disclaimers read-only GET status không kích hoạt calculation ngầm.
- `ScoreAuditLogTable.vue` (+ `spec.ts`, `stories.ts`):
  - Bảng nhật ký audit điểm read-only, sắp xếp `occurredAt DESC`.
  - Hiển thị actor username, action, entity type & ID, request ID, và khung xem dữ liệu JSON before/after an toàn (untrusted display data).
  - Hoàn toàn không có affordance chỉnh sửa, xóa hay retry trên dòng audit.

### 1.4. View Orchestration, Routing & Shell Integration
- `CalculationOperationsView.vue` (+ `spec.ts`):
  - Màn hình vận hành tập trung tại `/v2/scorebooks/operations`.
  - Context bar chọn năm học và tra cứu nhanh mã học sinh.
  - 4 Thẻ thống kê tổng quan: FAILED tasks, RUNNING tasks, IN_PROGRESS transcripts, Audit events.
  - 3 Tab điều hướng độc lập: `Calculation tasks`, `Transcript status`, `Score audit log`. Thất bại ở một panel không gây sập panel khác.
  - Xử lý các trạng thái HTTP: `401` theo policy session, `403` giữ nguyên phiên và hiển thị banner giải thích thiếu quyền/scope, `404` không tìm thấy task/transcript, `409` hiển thị thông báo xung đột state đã đổi, tự động refresh và yêu cầu người dùng xác nhận lại.
- `FE/src/router/index.ts`:
  - Đăng ký route con `scorebooks/operations` dẫn đến `CalculationOperationsView.vue`.
- `FE/src/views/AuthenticatedV2ShellView.vue` (+ `spec.ts`):
  - Bổ sung navigation item `Vận hành tính điểm` (`pi pi-cog`) cho vai trò `ADMIN` và `ACADEMIC_OFFICE`.
  - Cập nhật test cases kiểm tra hiển thị và ẩn menu theo role tương ứng.

## 2. Danh sách file tạo mới và thay đổi

- `FE/src/types/calculationTask.ts` (NEW)
- `FE/src/types/scoreAudit.ts` (NEW)
- `FE/src/types/transcript.ts` (MODIFY)
- `FE/src/utils/calculationTaskDate.ts` (NEW)
- `FE/src/utils/calculationTaskDate.spec.ts` (NEW)
- `FE/src/services/calculationTaskApi.ts` (NEW)
- `FE/src/services/calculationTaskApi.spec.ts` (NEW)
- `FE/src/services/scoreAuditApi.ts` (NEW)
- `FE/src/services/scoreAuditApi.spec.ts` (NEW)
- `FE/src/services/transcriptApi.ts` (MODIFY)
- `FE/src/services/transcriptApi.spec.ts` (MODIFY)
- `FE/src/components/CalculationTaskTable.vue` (NEW)
- `FE/src/components/CalculationTaskTable.spec.ts` (NEW)
- `FE/src/components/CalculationTaskTable.stories.ts` (NEW)
- `FE/src/components/CalculationTaskDetailModal.vue` (NEW)
- `FE/src/components/CalculationTaskDetailModal.spec.ts` (NEW)
- `FE/src/components/CalculationTaskDetailModal.stories.ts` (NEW)
- `FE/src/components/RetryConfirmationModal.vue` (NEW)
- `FE/src/components/RetryConfirmationModal.spec.ts` (NEW)
- `FE/src/components/RetryConfirmationModal.stories.ts` (NEW)
- `FE/src/components/TranscriptStatusCard.vue` (NEW)
- `FE/src/components/TranscriptStatusCard.spec.ts` (NEW)
- `FE/src/components/TranscriptStatusCard.stories.ts` (NEW)
- `FE/src/components/ScoreAuditLogTable.vue` (NEW)
- `FE/src/components/ScoreAuditLogTable.spec.ts` (NEW)
- `FE/src/components/ScoreAuditLogTable.stories.ts` (NEW)
- `FE/src/views/CalculationOperationsView.vue` (NEW)
- `FE/src/views/CalculationOperationsView.spec.ts` (NEW)
- `FE/src/router/index.ts` (MODIFY)
- `FE/src/views/AuthenticatedV2ShellView.vue` (MODIFY)
- `FE/src/views/AuthenticatedV2ShellView.spec.ts` (MODIFY)

## 3. Quyết định kỹ thuật quan trọng

1. **Tuân thủ phân định trách nhiệm tính điểm (No Client-side Recalculation)**:
   - FE tuyệt đối không tự tính official score, average hay round điểm. Màn hình status chỉ đọc và phản ánh `IN_PROGRESS` / `FINISH` cùng số phiên bản từ backend.
2. **Đảm bảo tính độc lập giữa các panel**:
   - Quá trình tải danh sách task, truy vấn trạng thái transcript và nạp audit log diễn ra bất đồng bộ và độc lập. Một lỗi API 403 hoặc 404 ở tab task không làm mất dữ liệu audit log hay làm vỡ giao diện chung.
3. **Quản lý phân quyền & Bảo toàn phiên đăng nhập**:
   - Vai trò `ADMIN` và `ACADEMIC_OFFICE` được cấp quyền vận hành và retry task.
   - Khi một người dùng không có quyền quản lý task (ví dụ: `TEACHER`), hệ thống phản hồi 403 nhưng phiên làm việc (`authenticated session`) được giữ nguyên, ẩn hoặc vô hiệu hóa các nút mutation và hiển thị banner giải thích rõ ràng.
4. **Cơ chế xác nhận Retry và xử lý 409 Conflict**:
   - Tất cả các thao tác retry (đơn hoặc bulk) bắt buộc phải qua confirmation modal, giải thích rõ đây là mutation đưa task về PENDING cho worker bất đồng bộ.
   - Khi backend phản hồi 409 do task không còn ở trạng thái FAILED (đã được retry hoặc cập nhật), modal đóng lại, banner cảnh báo 409 hiển thị và task list tự động làm mới để người dùng nắm bắt dữ liệu mới nhất.
5. **Safe JSON Rendering cho Audit Trail**:
   - Dữ liệu `beforeData` và `afterData` được coi là untrusted data và render an toàn dưới dạng formatted JSON text trong khung monospace có scroll, chống nguy cơ injection và không bind vào editable form fields.

## 4. Kết quả Validation Quality Gates

- `npm run lint`: **PASS** (0 errors, 0 warnings).
- `npm run test`: **PASS** (73 test files passed, 310 tests passed).
- `npm run test:coverage`: **PASS** (84.32% statement coverage toàn codebase).
- `npm run build`: **PASS** (`vue-tsc --noEmit && vite build` hoàn thành không lỗi).
- `npm run build-storybook`: **PASS** (`storybook build` sinh tĩnh thành công tại `storybook-static` với đầy đủ stories cho tất cả 5 component mới).
