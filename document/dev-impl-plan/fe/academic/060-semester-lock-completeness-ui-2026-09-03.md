# Developer Plan 060: Semester Lock & Completeness UI

## 1. Trạng thái và approval gate

- Status: `Approved/Implemented` — user đã phê duyệt Plan 060 và wireframe qua agent message.
- Ngày lập: `2026-09-03`.
- Application-document version: `v2`.
- Module: FE `academic`, phối hợp contract với backend semester/completeness/notification.
- Wireframe bắt buộc để duyệt: [`060 wireframe`](../../../../wireframes/fe/academic/060-semester-lock-completeness-ui/README.md).
- Implementation đã hoàn tất theo approval; checkpoint chỉ còn là dữ liệu nội bộ backend và không xuất hiện trên UI.

## 2. Mục tiêu

Tạo workspace cho `ADMIN`/`ACADEMIC_OFFICE` theo luồng `completeness report → missing-data details → email notification → lock/reopen lifecycle`, đồng thời hiển thị lifecycle/status học kỳ rõ ràng.

Completeness là warning không tự động chặn lock theo `BR-SEM-007`; trạng thái authorization, lỗi backend và trạng thái calculation phải được hiển thị riêng.

## 3. Evidence hiện có và khoảng thiếu

### 3.1. Backend/API đã có evidence

Theo `FrontendApiGuide.md`, `frontend-api/02-academic-structure.md` và controller hiện tại:

| Method | Endpoint | Mục đích | Quyền |
|---|---|---|---|
| `GET` | `/api/v2/semesters?academicYearId=...` | lấy danh sách học kỳ | `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`, `STUDENT` |
| `GET` | `/api/v2/semesters/{semesterId}/completeness-report?checkpointCode?` | latest completeness report | `ADMIN`, `ACADEMIC_OFFICE` |
| `GET` | `/api/v2/semesters/{semesterId}/completeness-decision?checkpointDate=yyyy-MM-dd` | internal mapping nếu backend bắt buộc; không tạo UI checkpoint | `ADMIN`, `ACADEMIC_OFFICE` |
| `GET` | `/api/v2/semesters/{semesterId}/notifications` | notification history/list | `ADMIN`, `ACADEMIC_OFFICE` |
| `POST` | `/api/v2/semesters/{semesterId}/notifications/dispatch?checkpointCode?` | dispatch email thủ công; query chỉ giữ ở internal mapping nếu backend bắt buộc | `ADMIN`, `ACADEMIC_OFFICE` |
| `POST` | `/api/v2/semesters/{semesterId}/notifications/retry-failed` | retry notification failed | `ADMIN`, `ACADEMIC_OFFICE` |
| `POST` | `/api/v2/semesters/{semesterId}/lock` | lock học kỳ | `ADMIN`, `ACADEMIC_OFFICE` |
| `POST` | `/api/v2/semesters/{semesterId}/reopen` với `{ reason }` | reopen học kỳ | `ADMIN`, `ACADEMIC_OFFICE` |

Response fields phải bám DTO hiện hành: report có `reportStatus`, summary counts/details, failure/correlation và các checkpoint fields chỉ ở internal mapping nếu backend bắt buộc; decision không được đưa lên UI; notification FE chỉ dùng luồng email với recipient email đã mask, trạng thái gửi, attempt/error/timestamps; semester có `DRAFT | ACTIVE | LOCKED | CLOSED` và lock/reopen metadata.

### 3.2. FE gap cần triển khai

- `FE/src/services/academicApi.ts` hiện đã có report/lock/reopen nhưng chưa có typed functions cho decision và notification list/dispatch/retry.
- `FE/src/types/academic.ts` chưa có type cho decision, notification channel/status và notification DTO.
- `SemesterStatusDialog` hiện hiển thị summary/details dạng `string[]`, chưa có missing-data detail table/filter và chưa hiển thị notification operations.
- Chưa có notification list email, dispatch confirmation và retry failed flow.
- Chưa có evidence UI cho capability của role; backend vẫn là authority.

## 4. Requirement và business rules

- `FR-SEM-004`/`005`: xác nhận lock thủ công và trạng thái automatic lock.
- `FR-SEM-006`/`009`: completeness report, checkpoint và cảnh báo dữ liệu thiếu.
- `FR-SEM-007`/`008`: reopen có reason, actor/time và không xóa lịch sử.
- `BR-SEM-004` đến `BR-SEM-009`: lifecycle, lock guard, dữ liệu thiếu không rollback lock, reopen chỉ từ `LOCKED`.
- `CR-SEM-001`: checkpoint và decision chỉ là backend/internal concern của FE plan; notification FE chỉ bao phủ email, idempotency, bounded retry và lỗi dispatch không làm UI giả định lock thất bại.
- `NFR-SECURITY-004`/`005`: `401` chuyển Login và clear session; `403` giữ session, hiển thị access denied.
- `AC-14`: automatic lock tạo completeness report và ngăn teacher sửa điểm trực tiếp.

## 5. Scope FE in-scope

### 5.1. Workspace và lifecycle/status

- Chọn academic year/semester theo flow academic hiện có.
- Hiển thị `DRAFT`, `ACTIVE`, `LOCKED`, `CLOSED`, `automaticLockAt`, `lockedAt`, `lockedBy`, `lockReason`, `reopenUntil`.
- Timeline lifecycle `DRAFT → ACTIVE → LOCKED → CLOSED`; `CLOSED` chỉ đọc và không hiện reopen.
- `ACTIVE`: cho xem report, decision và lock action.
- `LOCKED`: cho xem report/notification và reopen action.
- Lock/reopen có confirmation dialog; reopen bắt buộc reason không rỗng; reload dữ liệu sau mutation.
- Completeness `INCOMPLETE` chỉ là warning; không tự disable lock nếu backend cho phép.

### 5.2. Completeness report và missing-data details

- Hiển thị latest completeness report của học kỳ; không hiển thị checkpoint label, filter, panel, metadata, date hoặc CTA.
- Luồng UX chính là: report cho biết dữ liệu thiếu → mở missing-data details → gửi email nhắc → review lock/reopen lifecycle.
- Summary cards: thiếu `KTĐK`, KTCK không hợp lệ, thiếu skill columns, ô điểm chưa nhập, học sinh chưa có score data, sổ điểm chưa công bố, pending score-change request.
- Hiển thị `COMPLETE`, `INCOMPLETE`, `FAILED`; `FAILED` có failure reason/correlation id và nút retry tải report nếu được contract phê duyệt.
- Detail view có danh sách missing-data hiện có; không tự parse hoặc suy diễn student/class fields ngoài DTO.

### 5.3. Notification list/dispatch/retry

- Notification chỉ có luồng email trong FE scope.
- Bảng email notification: recipient email đã mask, trạng thái gửi `PENDING | SENT | FAILED`, attempt count, sent/updated time và error; không hiển thị channel hoặc checkpoint.
- Cho dispatch email thủ công; confirmation nêu rõ thao tác gửi email và idempotency, không yêu cầu người dùng chọn checkpoint.
- Nút retry chỉ xuất hiện khi có `FAILED`; hỗ trợ retry toàn bộ failed theo endpoint hiện có, không tự retry vô hạn ở client.
- Sau dispatch/retry: refresh report/notification state, thông báo success/warning/error theo response.
- Không hiển thị nội dung notification nhạy cảm rộng hơn scope DTO/actor.

### 5.4. Permissions và transport errors

- Chỉ render workspace/action office khi role/capability contract đã được xác nhận; không coi `TEACHER`/`STUDENT` là được phép chỉ vì xem được semester list.
- Backend vẫn authoritative; UI ẩn action không thay thế server authorization.
- `401`: clear auth session, điều hướng Login.
- `403`: access-denied state, giữ auth session, không retry vô hạn.
- `404`: semester/report không tồn tại state.
- `409`: conflict lifecycle hoặc business constraint, giữ dữ liệu hiện tại và yêu cầu reload/review; không báo mutation thành công.
- `400`: validation (đặc biệt reopen reason) hiển thị gần form/dialog.

### 5.5. Loading, empty, error và accessibility

- Loading riêng cho semester/report/notifications và mutation; không khóa toàn trang không cần thiết.
- Empty: chưa có report, chưa có notification, không có failed notification, report complete không có details.
- Error có retry phù hợp và correlation id nếu có.
- Keyboard focus trong dialog, label cho input, `aria-live` cho status/error, màu status đi kèm text/icon; responsive cho desktop/mobile.

## 6. API/type boundary dự kiến

Chỉ mở rộng typed boundary trong `academicApi.ts`/`academic.ts` sau approval. Dự kiến type tối thiểu:

- `SemesterCompletenessDecision` chỉ là internal/API mapping nếu backend bắt buộc; không expose ra UI.
- `SemesterNotificationStatus = 'PENDING' | 'SENT' | 'FAILED'` cho luồng email.
- `SemesterNotification` khớp đủ fields của `ResSemesterNotificationDTO`.
- Không tạo UI/service action để người dùng chọn hoặc xem checkpoint; nếu endpoint decision bắt buộc cho report flow, mapping phải giữ nội bộ.
- `fetchSemesterNotifications(token, semesterId)`.
- `dispatchSemesterNotifications(token, semesterId)`; chỉ truyền query kỹ thuật nếu backend bắt buộc và không bind vào UI.
- `retryFailedSemesterNotifications(token, semesterId)`.

Các tên field/enum trên chỉ là mapping từ DTO/controller đã có; nếu controller/DTO hiện hành khác khi implementation bắt đầu thì dừng và cập nhật plan/API guide, không tự invent contract.

## 7. Component/view dự kiến

- Mở rộng `FE/src/views/SemesterListView.vue` để điều phối report, decision, notification và mutation state.
- Tách hoặc mở rộng component status hiện có (`SemesterStatusDialog.vue`) thành các presentation sections nhỏ nếu cần: lifecycle, completeness summary/detail, email notifications.
- Có thể tạo `SemesterNotificationTable.vue`, `SemesterLockConfirmDialog.vue`, `SemesterReopenDialog.vue` nếu reuse/độ phức tạp chứng minh cần thiết; không tạo `SemesterCheckpointPanel.vue`.
- Không thêm store hoặc generic API abstraction.
- Storybook deterministic, không gọi backend; mỗi mocked request dùng response mới.

## 8. Test và validation dự kiến

- Service tests: internal query mapping only when required, response mapping, email dispatch/retry/lock/reopen calls.
- Component/view tests: active incomplete, complete, failed report, no report, email notifications empty/failed/sent, lock/reopen success, validation, `401/403/404/409`.
- Storybook stories: `ActiveIncomplete`, `Complete`, `ReportFailed`, `EmailNotificationsFailed`, `LockedReopen`, `Forbidden`, `Empty`, `Loading`, `MutationConflict`.
- Validation theo scripts thực tế trong `FE/package.json`: lint, focused/full test, coverage, build, Storybook build nếu áp dụng.
- Browser walkthrough/live backend chỉ báo `PASS` khi thực sự chạy; nếu không có browser/backend thì `NOT RUN`/`BLOCKED`.

## 9. Out-of-scope / TBD / blockers

- Không sửa BE, migration, scheduler, email provider, calculation worker, transcript calculation hoặc teacher score-entry flow.
- Không mở rộng quyền cho `TEACHER`/`STUDENT`; không tự suy role/capability từ UI.
- Không phát triển audit-log viewer đầy đủ; chỉ hiển thị metadata đã có trong response.
- Không tự tạo endpoint history lock/reopen nếu backend chưa expose contract; lifecycle hiển thị từ `Semester` DTO hiện có.
- TBD: có endpoint/DTO riêng cho student/class-level missing-data detail hay chỉ dùng `summary.details: string[]`; plan này chỉ cam kết hiển thị dữ liệu backend trả về.
- TBD: semantics nội bộ của decision/checkpoint fields nếu backend bắt buộc; không được đưa chúng thành UX hoặc CTA.
- TBD: nếu backend bổ sung decision riêng cho reopen, FE chỉ xử lý ở internal mapping khi thật sự cần; lifecycle UI vẫn chỉ hiển thị lock/reopen status và action contract.
- Blocker trước implementation: xác nhận auth role/capability contract hiện hành (JWT/user summary có roles optional trong FE); backend authorization đã có nhưng FE không được tự coi role là authoritative.
- Blocker nếu xảy ra: controller/DTO/API guide drift, đặc biệt request semantics dispatch/retry hoặc pagination notification; phải cập nhật contract trước code.

## 10. Acceptance criteria cho Plan Draft

Plan chỉ chuyển `Approved` sau khi user xác nhận cả Plan và wireframe. Sau approval, implementation chỉ được coi là đạt khi:

1. Workspace hiển thị đúng lifecycle/status và không cho reopen `CLOSED`.
2. Report/decision/notification sử dụng typed API đúng endpoint và DTO.
3. UI không hiển thị checkpoint/checkpointCode/checkpointDate dưới bất kỳ dạng label, filter, panel, metadata hoặc CTA nào.
4. Luồng UI rõ ràng: completeness report (dữ liệu thiếu) → missing-data details → email notification → lock/reopen lifecycle.
5. Email notification chỉ hiển thị recipient email đã mask, trạng thái gửi, attempt/error/timestamps; không hiển thị channel khác.
6. Lock/reopen có confirmation, validation, refresh và xử lý `409`.
7. `401`/`403`/`404`/`400` được phân biệt đúng.
8. Storybook deterministic bao phủ các state trong wireframe.
9. Test/quality gates chạy theo cấu hình FE và báo cáo evidence thực tế.
