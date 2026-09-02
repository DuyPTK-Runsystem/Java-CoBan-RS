# Developer Plan 059: Score Change Request UI

## Trạng thái phê duyệt

- Trạng thái: `Approved/Implemented` — user đã phê duyệt Plan 059 và wireframe qua tin nhắn agent.
- Ngày lập: `2026-09-03`.
- Application-document version: `v2`.
- Wireframe bắt buộc review trước implementation: [`README.md`](../../../../wireframes/fe/score-change-request/059-score-change-request-ui/README.md) · [`index.html`](../../../../wireframes/fe/score-change-request/059-score-change-request-ui/index.html).
- Phạm vi đã triển khai: Developer Plan, wireframe, FE production UI, typed API service và Storybook fixtures.

## 1. Mục tiêu

Thiết kế FE cho vòng đời yêu cầu sửa điểm: giáo viên tạo yêu cầu và theo dõi yêu cầu của mình; `ADMIN`/`ACADEMIC_OFFICE` xem, xem chi tiết, duyệt hoặc từ chối; người yêu cầu có thể hủy yêu cầu `PENDING` theo contract backend. UI phải làm rõ snapshot trước sửa, giá trị đề xuất, lý do, trạng thái và người/thời điểm xử lý.

## 2. Nguồn đối chiếu

- [BE Plan 038](../../../BE/scorebook/038-score-change-request-2026-08-25.md) — business flow, status, authorization và endpoint.
- [BE Dev Note 038](../../../dev-note/be/scorebook/038-score-change-request-2026-08-25.md) — DTO/controller đã triển khai và known risks.
- [Frontend API v2 — Scorebook, Score Change and Audit](../../../application-doc/v2/frontend-api/05-scorebook-change-audit.md).
- [Score Change and Calculation Module](../../../application-doc/v2/modules/05-ScoreChangeAndCalculationModule.md).
- [FE remaining plans review](../../summary/FE_REMAINING_PLANS-2026-09-02.md).
- FE rules: `FE/AGENTS.override.md`, `FE/agent-rules/00-foundation.md`, `02-domain-rules.md`, `03-api-data-boundaries.md`, `04-quality-documentation.md`.

## 3. Contract FE được phép sử dụng

Base path: `/api/v2/score-change-requests`.

| Method | Path | Quyền theo controller/backend | Mục đích |
|---|---|---|---|
| `POST` | `/` | `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER` | Tạo request |
| `GET` | `/` | `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER` | Danh sách có filter và server pagination |
| `GET` | `/{requestId}` | staff như trên | Chi tiết |
| `POST` | `/{requestId}/approve` | `ADMIN`, `ACADEMIC_OFFICE` | Duyệt và apply điểm |
| `POST` | `/{requestId}/reject` | `ADMIN`, `ACADEMIC_OFFICE` | Từ chối với `rejectionReason` |
| `POST` | `/{requestId}/cancel` | `ADMIN`, `TEACHER`; backend còn kiểm tra requester hoặc Admin | Hủy request |

Create payload gồm `assessmentColumnId`, một trong `studentId`/`studentCode`, `proposedStatus`, `proposedValue` và `reason`. `proposedValue` phải tuân theo `ScoreStatus`; `SCORED` cho phép `0`, các trạng thái không có điểm dùng `null` theo backend validation.

List response (`ResScoreChangeRequestDTO`) chỉ có request/column/student, proposed value/status, requester/reviewer, timestamp và status. Detail (`ResScoreChangeRequestDetailDTO`) mới có `studentScoreId`, `beforeStatus`, `beforeValue`, `reason`, `rejectionReason`, `appliedAt`.

## 4. Phạm vi in-scope

### 4.1 Teacher view

- Trang danh sách “Yêu cầu sửa điểm của tôi”, lọc trạng thái/mã học sinh/bối cảnh môn học/cột điểm và phân trang phía máy chủ.
- Người dùng chọn năm học → học kỳ → lớp → môn/lớp học phần hoặc bối cảnh thân thiện; FE tự lookup dữ liệu request/scorebook theo bối cảnh. Không yêu cầu người dùng nhập hoặc nhớ `scorebookId`.
- Tạo yêu cầu từ bối cảnh đã lookup; chọn học sinh bằng `studentCode` khi contract hỗ trợ, giữ `studentId` là technical identifier nội bộ.
- Form trạng thái/giá trị đề xuất/lý do; validation trước submit; không tự tính điểm trung bình.
- Detail drawer/modal hiển thị snapshot `beforeStatus`/`beforeValue`, proposed status/value, reason, status timeline, requester/reviewer và thời điểm.
- Nút `Hủy` chỉ hiện cho request `PENDING` do requester tạo; xác nhận trước mutation; sau thành công refresh detail/list.

### 4.2 Office/Admin view

- Danh sách toàn phạm vi backend cho phép, lọc trạng thái và bối cảnh thân thiện; các filter kỹ thuật `scorebookId`, `columnId`, `studentId`/`studentCode`, `requestedBy`, page/size chỉ được adapter tự sinh sau lookup, không lộ thành input chính cho người dùng.
- Detail review với comparison “Trước sửa” / “Đề xuất”, cảnh báo snapshot và action approve/reject.
- Reject dialog bắt buộc `rejectionReason`, tối đa 1000 ký tự theo DTO.
- Approve confirmation nêu rõ backend sẽ apply điểm, chuyển calculation state/task trong transaction; UI không chờ hoặc tự chạy calculation.
- Sau approve/reject/cancel, dùng response detail làm trạng thái mới và invalidate/refetch list.

### 4.3 Shared states và permission

- Loading cho list/detail/mutation; empty list và empty filtered result.
- `401`: yêu cầu đăng nhập lại/đi tới auth flow hiện có.
- `403`: hiển thị không có quyền; không suy diễn role từ UI.
- `404`: request/context không tồn tại, cho phép quay về list và refresh.
- `409`: snapshot conflict, duplicate pending request, invalid lifecycle hoặc backend conflict; giữ form khi phù hợp và yêu cầu reload detail/list.
- Network/5xx: lỗi có thể retry, không hiển thị stack trace.
- Lookup context: đang tải, không tìm thấy bối cảnh/request, lỗi lookup và có thể thử lại; không gọi danh sách request khi context chưa được resolve (trừ màn hình lịch sử đã có phạm vi rõ ràng).
- Capability hiển thị chỉ là UX hint; backend authorization là authoritative. `TEACHER` không mặc định có quyền với mọi scorebook.

## 5. Trạng thái và quy tắc hiển thị

Các status wire là `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `APPLIED` (chỉ giữ trong contract/type). UI phải dùng nhãn tiếng Việt tương ứng: `PENDING` = “Chờ duyệt”, `APPROVED` = “Đã duyệt”, `REJECTED` = “Bị từ chối”, `CANCELLED` = “Đã hủy”, `APPLIED` = “Đã áp dụng”. Flow backend ghi nhận là `PENDING → APPROVED → APPLIED`, hoặc kết thúc `REJECTED`/`CANCELLED`; UI phải hiển thị status response thực tế, không tự chuyển trạng thái.

Snapshot status dùng `UNSCORED`, `SCORED`, `ABSENT`, `EXEMPTED`, `CANCELLED`. `UNSCORED` và `beforeValue = null` không được hiển thị như điểm `0`. JSON/timestamp chỉ format ở boundary; không dùng `Date.toISOString()` cho date-only nếu phát sinh filter ngày.

## 6. Phương án FE dự kiến sau approval

- `FE/src/services/scoreChangeRequestApi.ts`: typed list/detail/create/approve/reject/cancel service, query mapping, normalized error.
- `FE/src/types/scoreChangeRequest.ts`: wire DTO/status unions và tách editable form state khỏi response DTO.
- `FE/src/components/scorebook/ScoreChangeRequestForm.vue`, `ScoreChangeRequestTable.vue`, `ScoreChangeRequestDetail.vue`, `ScoreChangeRequestReviewDialog.vue` nếu các abstraction này phù hợp pattern hiện tại.
- `FE/src/views/scorebook/ScoreChangeRequestView.vue`: route orchestration, lookup bối cảnh thân thiện, filters, pagination và role/capability hints.
- Router/menu chỉ bổ sung sau khi role source và route convention được xác nhận; không tạo navigation authoritative từ guessed role.

## 7. Test, Storybook và validation dự kiến

- Service tests: request mapping, filter mapping, response/detail mapping và mỗi endpoint error (`401/403/404/409`).
- Component/view tests: teacher create, office approve/reject, cancel ownership, status rendering, snapshot null/zero distinction, loading/empty/error và pagination.
- Storybook deterministic fixtures, không gọi backend: teacher pending, office review, applied, rejected, cancelled, empty, loading, forbidden, not-found và conflict.
- Chạy đúng scripts hiện có trong `FE/package.json`: lint, test, coverage, build; Storybook build nếu cấu hình/ảnh hưởng. Documentation-only plan/wireframe hiện tại không claim các gate này đã PASS.
- Browser walkthrough/live backend chỉ thực hiện sau implementation và phải ghi `NOT RUN` nếu không có browser hoặc backend seed phù hợp.

## 8. Out-of-scope

- Backend/schema/migration, sửa Plan 038, full persistence integration test hoặc MySQL preflight.
- Audit log screen độc lập; UI chỉ hiển thị metadata/status có trong score-change detail. Audit API cần plan riêng nếu mở rộng.
- Calculation worker, công thức điểm, transcript calculation UI, retake UI hoặc semester lock.
- Sửa điểm trực tiếp trong scorebook; request flow không thay thế score-entry flow.
- Tự động approve, tự mở khóa học kỳ, polling calculation hoặc optimistic local status transition.

## 9. Unresolved contract/TBD cần chốt trước hoặc trong approval

- DTO hiện chỉ trả numeric IDs cho `assessmentColumnId`, `requestedBy`, `reviewedBy`; tên môn/lớp/học kỳ/cột điểm phải lấy từ context/API khác. Mã tham chiếu kỹ thuật nếu buộc phải hiển thị phải có nhãn tiếng Việt, không dùng làm điều hướng/input chính.
- Endpoint và identifier để lookup `năm học → học kỳ → lớp → môn/lớp học phần → scorebook/column` chưa được chốt trong contract FE hiện tại. Đây là contract gate/TBD bắt buộc trước implementation; không tự invent endpoint hoặc dùng `scorebookId` làm UX context.
- List/detail envelope thực tế và shape lỗi chuẩn cần xác nhận từ FE service convention/current controller response.
- `ACADEMIC_OFFICE` có được tạo request và hủy request của người khác hay không: controller role cho phép nhưng service ownership/role semantics phải là nguồn quyết định.
- `ADMIN` có được cancel request của requester khác: controller cho phép, BE note nói service cho phép Admin; FE chỉ hiển thị theo capability/response, không coi action visibility là authorization.
- `APPROVED` có tồn tại như trạng thái quan sát được trước `APPLIED` hay backend luôn trả `APPLIED` sau approve; wireframe thể hiện cả canonical lifecycle nhưng implementation phải dùng response thực tế.
- Cách khởi tạo scorebook context khi mở form từ score grid và cách resolve `studentId` từ `studentCode` cần dùng API hiện có, không thêm endpoint mới trong FE plan này.
- Cancel endpoint body là rỗng; reject body chính xác là `{ rejectionReason }`.

## 10. Approval gate

Chỉ bắt đầu production implementation khi user xác nhận bằng agent message rằng:

1. wireframe Plan 059 đã được duyệt;
2. teacher create/list/detail/cancel và office list/detail/approve/reject là scope chính;
3. các TBD về envelope, role/ownership và display context được chấp nhận hoặc đã được chốt;
4. Storybook là checkpoint review trước khi hoàn tất production UI.
