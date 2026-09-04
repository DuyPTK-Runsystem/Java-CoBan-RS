# Developer Plan 063: Calculation Task & Audit UI

## 1. Trạng thái

- **Approval gate:** `Approved qua agent message và đã hoàn thành implementation (Dev Note 063, validation PASS)`.
- **Ngày lập:** `2026-09-03`.
- **Ngày hoàn thành:** `2026-09-04`.
- **Module:** FE `scorebook` / `calculation operations` / `audit`.
- **Wireframe bắt buộc:** [063 Calculation Task & Audit UI](../../../wireframes/fe/scorebook/063-calculation-task-audit-ui/README.md).
- **Trạng thái:** Đã hoàn thành toàn bộ implementation, tests, Storybook và Dev Note; validation PASS.

## 2. Mục tiêu

Tạo màn hình vận hành cho người có quyền theo dõi calculation task, điều tra task lỗi,
yêu cầu retry an toàn và xem score audit log. Màn hình hiển thị trạng thái tính transcript
nhưng không tự tính điểm và không biến audit log thành thao tác chỉnh sửa.

## 3. Nguồn và contract áp dụng

- [BE Plan 042](../../../dev-impl-plan/BE/scorebook/042-subject-calculation-engine-and-worker-lifecycle-2026-08-25.md): worker lifecycle, retry, version protection, idempotency.
- [BE Plan 047](../../../dev-impl-plan/BE/scorebook/047-be-operational-closure-2026-08-26.md): operational APIs, audit, permission matrix và status query.
- [Scorebook change/audit API](../../../application-doc/v2/frontend-api/05-scorebook-change-audit.md).
- [Transcript, retake and calculation API](../../../application-doc/v2/frontend-api/06-transcript-retake-calculation.md).
- [Score Change and Calculation module](../../../application-doc/v2/modules/05-ScoreChangeAndCalculationModule.md).
- [Retake and Transcript module](../../../application-doc/v2/modules/06-RetakeAndTranscriptModule.md).
- [FE domain rules](../../../../FE/agent-rules/02-domain-rules.md), [API boundaries](../../../../FE/agent-rules/03-api-data-boundaries.md) và [quality/documentation](../../../../FE/agent-rules/04-quality-documentation.md).

Contract hiện hành phải được kiểm tra lại từ controller/DTO trước implementation nếu `TBD`
chưa được giải quyết. Không suy diễn quyền từ UI: backend vẫn authoritative về authorization
và assignment/data scope.

## 4. In-scope

### 4.1. Calculation task operations

- Danh sách task, mặc định ưu tiên/lọc `FAILED`, server-side pagination `page`/`size`.
- Filter theo `status`, `studentId` hoặc `studentCode`, `academicYearId`; ưu tiên hiển thị `studentCode`.
- Cột tối thiểu: task id, student, academic year, task type, status, `attemptCount`, `maxAttempts` nếu response có, `lastError`, `createdAt`, `updatedAt`/`completedAt` nếu response có.
- Detail drawer/modal: lỗi gần nhất, số lần thử, timestamps, source/requested version và transcript liên quan khi contract trả về.
- Retry một task `FAILED` và retry hàng loạt failed.
- Confirmation trước retry; nêu rõ đây là mutation, task sẽ được đưa về `PENDING`, có thể tạo audit event và worker xử lý bất đồng bộ.
- Sau success: refresh task list/detail và transcript status; không tự đánh dấu `FINISH`.
- Nếu retry bị lặp hoặc task đã đổi trạng thái, hiển thị `409` và tải lại state mới; hành vi phải idempotent theo backend contract.

### 4.2. Transcript calculation status

- Hiển thị `calculationStatus`, `sourceVersion`, `calculatedVersion`, `isUpToDate`, `calculatedAt`, `lastError`.
- `IN_PROGRESS`: hiển thị “Đang cập nhật”, có thể refresh/poll status; không coi dữ liệu cũ là kết quả chính thức mới nhất.
- `FINISH` và `isUpToDate=true`: hiển thị “Đã cập nhật”/current result.
- Chỉ đọc status GET; không gọi endpoint recalculate ngầm từ màn hình status.
- Endpoint scope (staff theo student và `/me` nếu màn hình được tái sử dụng) là `TBD` theo role/use case được duyệt.

### 4.3. Score audit log

- Bảng audit read-only, server-side pagination, sort `occurredAt DESC`.
- Filter theo `entityType`, `entityId`, `studentId`/`studentCode`, `action`, `actorUserId`, `fromOccurredAt`, `toOccurredAt`.
- Hiển thị actor, action, entity, timestamp, request id và before/after data dạng safe JSON preview.
- Có detail expansion để xem before/after; coi JSON là untrusted display data, không bind thành form editable.
- Không có nút sửa/xóa audit log và không dùng audit row để retry trực tiếp.

### 4.4. States và permission

- Loading, empty, success, validation/filter error, retrying, partial refresh và network error.
- `401`: xử lý theo session policy hiện có.
- `403`: giữ authenticated session, ẩn/disable mutation phù hợp và giải thích thiếu quyền/scope.
- `404`: detail/task/transcript không tồn tại; cho phép quay lại danh sách.
- `409`: stale task/state hoặc retry task không còn `FAILED`; refresh và yêu cầu người dùng xác nhận lại.
- Capability hiển thị theo role đã có trong auth contract: `ADMIN`, `ACADEMIC_OFFICE` vận hành task; `TEACHER` chỉ xem audit trong assignment scope nếu backend cho phép; `STUDENT` không xem operations/audit. Không hard-code quyền nếu source hiện tại chưa expose đủ capability.

## 5. Out-of-scope

- Không tính hoặc làm tròn official score/average ở FE.
- Không chạy calculation đồng bộ trong HTTP hoặc biến GET status thành command.
- Không sửa, xóa, export hoặc append audit log từ FE.
- Không tạo endpoint/DTO/enum mới; không thay schema, worker, retry algorithm hoặc backend permission.
- Không làm Retake Result UI/Transcript Viewer đầy đủ ngoài phần status cần liên kết.
- Không quyết định contract còn `TBD` về response fields, bulk retry response, polling interval, idempotency key/header hoặc endpoint status thực tế.

## 6. Luồng UI đề xuất

1. Người dùng chọn context (nếu cần) và mở Operations.
2. FE tải task page, transcript status và audit page độc lập; một request lỗi không làm mất panel còn lại.
3. Người dùng lọc `FAILED`, mở detail để xem lỗi/attempt/timestamp.
4. Bấm retry một hoặc retry all → confirmation dialog → mutation.
5. Thành công → thông báo “đã xếp lại task”, refresh task/status/audit; hiển thị `PENDING`/`IN_PROGRESS` cho tới khi backend trả `FINISH` + up-to-date.
6. Chuyển tab Audit để tra cứu read-only; before/after chỉ hiển thị safely escaped.

## 7. Retry, confirmation và idempotency

- Retry là mutation riêng, không gộp với thao tác đọc audit/status.
- Nút retry chỉ active với task đang được render là `FAILED`; bulk retry chỉ active khi page/filter có task failed và quyền cho phép.
- Confirmation phải nêu phạm vi (task cụ thể hoặc tất cả failed theo filter), số task hiển thị nếu biết, và việc reset/đưa task về `PENDING` theo backend contract.
- Disable nút trong lúc request chạy, chống double-click, nhưng không coi đây là idempotency đầy đủ.
- FE dựa vào backend idempotency/version behavior; `idempotency key`, retry token, bulk response và duplicate semantics: **TBD — cần chốt trước coding**.
- `409` sau confirmation không tự retry lại; refresh và yêu cầu confirmation mới.

## 8. Kiến trúc FE dự kiến

- Route-level view điều phối context, tabs, permission/capability và load states.
- Typed service riêng cho calculation tasks, transcript status và audit logs; không gọi raw fetch trong component.
- Components dự kiến: task filter, task table/paginator, task detail drawer, retry confirmation, transcript status card, audit table/detail JSON viewer.
- Types dùng canonical enums: `CalculationTaskStatus = PENDING | RUNNING | SUCCEEDED | FAILED`, `CalculationTaskType = STUDENT_YEAR_RECALC`, `CalculationStatus = IN_PROGRESS | FINISH`.
- Dùng query params cho filters/pagination nếu route contract cho phép; date/time hiển thị rõ timezone/format từ API.

## 9. Test, Storybook và validation dự kiến

- Service tests: query filters/pagination, 401/403/404/409, single/bulk retry, fresh `Response` cho từng mock request.
- Component/view tests: loading, empty, failed list, detail, retry confirmation, retrying/duplicate click, success refresh, `IN_PROGRESS`, `FINISH`, forbidden, stale 409 và audit JSON safely rendered.
- Storybook deterministic, không live backend: `FailedTasks`, `TaskDetail`, `RetryConfirmation`, `TranscriptInProgress`, `TranscriptFinished`, `AuditReadOnly`, `Empty`, `Forbidden`, `Conflict`.
- Validation theo script thật trong `FE/package.json`: `npm run lint`, `npm run test`, `npm run test:coverage`, `npm run build`; chạy Storybook build nếu module có stories.
- Browser walkthrough/live backend và permission regression cho `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`, `STUDENT`: **TBD / NOT RUN until environment and approved implementation exist**.

## 10. Rủi ro và TBD cần approval

1. **TBD — backend source verification:** endpoint canonical giữa `/calculation-tasks?status=FAILED` và `/calculation-tasks/failed`, response DTO fields và bulk retry response.
2. **TBD — authorization contract:** role claim/capability mapping và teacher assignment scope tại thời điểm FE implementation.
3. **TBD — idempotency:** header/token/optimistic version và duplicate bulk retry semantics.
4. **TBD — status polling:** interval, stop condition, maximum duration và whether polling is permitted for this screen.
5. **TBD — transcript context:** screen nhận `studentId`, `studentCode`, semester/year từ route/context nào.
6. **TBD — audit JSON:** giới hạn kích thước, redaction/PII policy và format detail approved for display.

## 11. Deliverables sau khi được duyệt

- FE view/components/services/types/tests trong module scorebook operations.
- Storybook stories deterministic để review trước production implementation.
- Validation evidence tách riêng PASS/FAIL/BLOCKED/NOT RUN.
- Dev Note sau implementation; không cập nhật summary dùng chung trong phase draft này.

## 12. Approval gate

Plan này chỉ chuyển sang `Approved` sau khi User xác nhận qua agent message rằng:

1. phạm vi UI và wireframe đã được duyệt;
2. các TBD contract cần chốt đã có quyết định hoặc được chấp nhận là blocker;
3. cho phép bắt đầu phase Storybook/implementation theo checkpoint riêng.
