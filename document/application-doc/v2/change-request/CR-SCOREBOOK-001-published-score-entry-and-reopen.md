# CR-SCOREBOOK-001: Nhập điểm sau công bố và mở lại sổ điểm

## Trạng thái

- Trạng thái: `APPROVED - IMPLEMENTED`.
- Ngày: `2026-09-03`.
- Phê duyệt: chủ dự án xác nhận qua agent message.

## Thay đổi nghiệp vụ

1. `PUBLISHED` là trạng thái đã công bố, không phải trạng thái khóa nhập điểm.
   Giáo viên bộ môn có phân công còn hiệu lực vẫn được nhập hoặc sửa điểm; backend
   tiếp tục là nơi kiểm tra quyền, thời hạn sửa trực tiếp, học kỳ và dữ liệu điểm.
2. `ADMIN`, `ACADEMIC_OFFICE` và giáo viên có quyền GVBM của sổ điểm được chuyển
   sổ từ `PUBLISHED` về `OPEN` qua endpoint mở sổ điểm hiện có.
3. `CLOSED` vẫn là trạng thái chỉ xem đối với điểm. Quy trình yêu cầu sửa điểm
   không bị thay thế: nó vẫn áp dụng khi backend không cho sửa trực tiếp.

## Ảnh hưởng contract

- Không thêm endpoint, DTO, enum hoặc migration.
- `POST /api/v2/scorebooks/{scorebookId}/open` nhận trạng thái nguồn `DRAFT`
  hoặc `PUBLISHED`; các trạng thái khác trả `409`.
- Score-entry API giữ backend authorization/validation hiện có ở `OPEN` và
  `PUBLISHED`.

## Ảnh hưởng FE

- Bảng điểm và dialog nhập điểm cho phép chỉnh sửa ở `OPEN` và `PUBLISHED`.
- Cấu hình cột/trọng số vẫn chỉ chỉnh sửa ở `OPEN`.
- Header hiển thị `Mở lại sổ` khi sổ đang `PUBLISHED`; `CLOSED` chỉ xem.
