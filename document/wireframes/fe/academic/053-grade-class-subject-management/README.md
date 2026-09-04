# Wireframe Plan 053 — Grade, Class, Subject & Class-Subject UI

## Mục đích

Bộ wireframe tĩnh để duyệt trước khi phê duyệt và triển khai Plan 053. Nội
dung mô phỏng shell v2 và các màn hình hợp nhất từ:

- Grade & Class Management UI.
- Subject & Class Subject UI.

Wireframe không gọi backend và không phải mã nguồn FE production.

Wireframe này là checkpoint `Phase 0`. Sau khi duyệt, implementation sẽ được
tách thành các phase Storybook độc lập; wireframe không đồng nghĩa với việc
đã duyệt toàn bộ source FE.

## Cách xem

Mở [`index.html`](index.html) bằng trình duyệt. Dùng các tab và nút trong
wireframe để xem:

1. Khối & chỉ báo sĩ số.
2. Danh sách lớp theo năm học.
3. Môn học với loại `ACADEMIC`/`SKILL`.
4. Gán môn vào lớp theo học kỳ.
5. Dialog tạo/sửa khối, lớp, môn và cấu hình applicability.

## Điểm cần duyệt

- Navigation và bố cục 3 nhóm màn: `Khối`, `Lớp`, `Môn học & lớp-môn`.
- Cách chọn năm học/lớp/học kỳ trước khi xem class-subject.
- Vị trí warning sĩ số và cách phân biệt warning với lỗi chặn.
- Các field `applicationScope`, `scopeType`, khối/lớp và học kỳ.
- Action matrix cho `PLANNED`, `ACTIVE`, `CLOSED`, `INACTIVE`, `COMPLETED`.
- Dialog create/edit và confirmation.
- Thứ tự review theo phase: `Khối & lớp` → `Môn học` → `Lớp-môn` → API integration.

## Giới hạn contract được thể hiện trong wireframe

- Các số sĩ số/cảnh báo là dữ liệu minh họa cho review; API hiện tại chưa trả
  count/warning trong list Grade/Class.
- Applicability hiện có endpoint create nhưng chưa có endpoint list/update;
  wireframe đánh dấu trạng thái này là “chờ API đọc lại cấu hình”.
- Auth chưa expose role nên navigation trong wireframe là tĩnh; `403` vẫn do
  backend quyết định.

## Liên kết

- Developer Plan draft:
  [`053-grade-class-subject-management-ui-2026-08-27.md`](../../../../dev-impl-plan/fe/academic/053-grade-class-subject-management-ui-2026-08-27.md)
