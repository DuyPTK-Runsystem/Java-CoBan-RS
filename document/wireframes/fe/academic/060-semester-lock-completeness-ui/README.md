# Wireframe 060 — Semester Lock & Completeness UI

Status: `Draft — chờ user approval cùng Developer Plan 060`  
Ngày: `2026-09-03`  
Prototype tĩnh: [`index.html`](./index.html)

## Mục đích

Wireframe mô phỏng deterministic workspace cho `ADMIN`/`ACADEMIC_OFFICE`. Không có backend, không gọi API và không đại diện cho production styling. Dùng để duyệt cấu trúc, nội dung, state và dialog trước implementation.

## Cách xem

Mở `index.html` bằng browser. Dùng các nút state ở đầu trang để xem:

- `Active · Incomplete`: report thiếu dữ liệu, missing-data details và email notification có `FAILED`.
- `Active · Complete`: report đầy đủ, không có missing details.
- `Locked · Reopen`: lifecycle locked và dialog reopen với reason.
- `Loading`, `Empty`, `Forbidden`, `Report failed`, `Conflict 409`: các state bắt buộc.

Các nút `Xem chi tiết`, `Khóa học kỳ`, `Mở lại`, `Dispatch`, `Retry failed` chỉ mô phỏng dialog/feedback phía client; không tạo request thật.

## Nội dung cần duyệt

- Semester selector và lifecycle timeline.
- Completeness report và missing-data details.
- Email notification list với recipient email đã mask, trạng thái gửi `PENDING`, `SENT`, `FAILED`, attempt count, timestamps và error.
- Confirmation dialog lock/reopen, dispatch email và retry failed.
- Loading/empty/error/forbidden/conflict presentation.
- Phân tách warning completeness với lỗi authorization/lifecycle.

## Excluded from UI

Checkpoint, `checkpointCode` và `checkpointDate` không xuất hiện trong wireframe dưới dạng label, filter, panel, metadata hoặc CTA. Nếu backend bắt buộc các field này, chúng chỉ tồn tại ở API contract/internal mapping của implementation.

## Contract notes

Các endpoint và DTO tham chiếu trong Plan 060. Wireframe không chốt thêm field ngoài contract hiện có. Student/class-level detail và role-discovery vẫn là `TBD/BLOCKED` nếu backend/auth contract chưa xác nhận.
