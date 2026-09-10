# Contract Checkpoint 073 — v3 Foundation

## Trạng thái

- Application version: `v3`
- Plan: `073`
- Approval: người dùng phê duyệt Plan 073 và CR/baseline v3 qua agent ngày `2026-09-10`.
- Wireframe: vẫn `review-only`; chưa được coi là production UI.

## Ranh giới tương thích

- Không đổi ngầm endpoint, DTO hoặc error envelope v2.
- `POST /api/v3/students` có trước CR-V3-001 và không phải contract capability của Plans
  074–079.
- Endpoint cụ thể của placement, timetable, notification, query, score import và lesson
  log tiếp tục là `TBD` cho đến plan capability tương ứng.

## Envelope, pagination và lỗi

Endpoint v3 mới giữ `RestResponse` hiện có. Khi trả danh sách phân trang, `data` dùng
`V3PageResponse<T>` với các field `items`, `page` (zero-based), `pageSize`, `total` và
`appliedFilters`. Request list dùng `search`, `filter.<name>`, `sort`, `page`, `pageSize`.
Mỗi plan capability phải công khai filter allow-list, sort allow-list, page-size mặc định và
giới hạn trước khi tạo endpoint; FE không được tự lọc một page đã tải.

Plan 073 không thêm error code hay error payload riêng cho v3. Endpoint capability sau này tiếp
tục dùng HTTP status và envelope `RestResponse` hiện có; nếu cần semantics chi tiết hơn, plan
capability phải đề xuất và được duyệt cùng API contract của chính endpoint đó.

## Mutation, audit và authorization

- Mutation trên aggregate versioned phải nhận `expectedVersion`; mismatch trả `409` và FE
  giữ input để người dùng tải lại/review.
- Mutation retryable chỉ dùng idempotency khi plan capability xác định key, scope và thời
  gian giữ key; không mặc định áp dụng cho mọi POST.
- Audit tối thiểu lưu actor, thời điểm, correlation/request id, resource identity và
  before/after hoặc decision phù hợp với capability.
- Backend là nguồn quyền cuối cùng. `TBD-003` chưa có role matrix nghiệp vụ, nên không có
  endpoint capability nào được phép suy diễn `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`,
  `STUDENT`, GVCN hoặc GVBM từ checkpoint này.

| Capability | Quyền mutation/read | Trạng thái |
| --- | --- | --- |
| Placement (074) | `TBD-003` | Chờ role, criteria, tie-breaker và capacity policy. |
| Timetable (075) | `TBD-003` | Chờ publish permission và policy metadata. |
| Notification (076) | `TBD-003`, `TBD-004` | Chờ quyền gửi, retention và channel. |
| Query (077) | Theo endpoint nguồn | Chỉ mở sau allow-list filter/sort. |
| Score import (078) | `TBD-003` | Chờ role, template và partial-commit policy. |
| Lesson log (079) | `TBD-003` | Chờ edit/review policy. |

## Fixture và review boundary

| Artifact | Mục đích |
| --- | --- |
| `FE/src/types/v3Foundation.ts` | Type chung cho page, query, lỗi và review state. |
| `FE/src/utils/v3QueryState.ts` | Đọc/ghi URL query deterministic, không gọi HTTP. |
| `FE/src/fixtures/v3FoundationFixture.ts` | Fixture ổn định cho review, test và Storybook. |
| `V3ContractReviewPanel` | Loading, empty, 401, 403, 404, 409 và ready state; chưa gắn route/API. `401` thật vẫn do `apiClient` xóa phiên và chuyển hướng đăng nhập. |
| `common.contract.v3` | DTO boundary BE và test invariant, chưa expose endpoint. |

## Entry criteria cho Plans 074–079

Mỗi plan phải chọn capability cụ thể, cập nhật requirement/API/data decision, chốt role
matrix phần liên quan, bổ sung fixture contract và chạy BE/FE integration gate. Plan 073 không
triển khai placement engine, timetable, notification delivery, import commit hoặc lesson-log
CRUD.
