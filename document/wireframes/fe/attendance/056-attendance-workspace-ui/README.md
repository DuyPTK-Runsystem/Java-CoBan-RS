# Wireframe Plan 056 — Attendance Workspace UI

## Mục đích

Bộ wireframe tĩnh để review trước khi phê duyệt và triển khai Developer Plan
056. Wireframe gộp ba hạng mục:

- `Attendance Session UI`: chọn context, ngày/buổi và mở buổi điểm danh hợp lệ.
- `Attendance Exception Entry UI`: ghi nhận, sửa và xóa exception.
- `Attendance History & Summary UI`: lịch sử cá nhân và báo cáo tổng hợp theo lớp/thời gian.

Wireframe không gọi backend, không thay thế mã nguồn Vue/PrimeVue production và
không có nghĩa là plan đã được phê duyệt.

## Cách xem

Mở [`index.html`](index.html) bằng trình duyệt. Các tab trong page cho phép xem:

1. Điểm danh theo buổi với trạng thái `SCHEDULED`/`NO_CLASS` và danh sách học sinh.
2. Dialog ghi nhận exception `ABSENT`, `EXCUSED`, `LATE`, `EARLY_LEAVE`.
3. Lịch sử chuyên cần của học sinh hiện tại, ở chế độ read-only.
4. Báo cáo lớp với summary và bảng thống kê từng học sinh.

## Điểm cần duyệt

- Một context chung cho năm học, học kỳ, lớp, ngày và buổi sáng/chiều.
- Tab và thứ tự thao tác từ mở session → nhập exception → xem history/summary.
- Cách biểu diễn `PRESENT` dẫn xuất khi không có exception; nút xóa trả row về mặc định có mặt.
- Cách phân biệt `SCHEDULED`/`NO_CLASS`, warning, validation error, `403` và empty state.
- Bảng có overflow cục bộ trên mobile; dialog có footer thao tác được khi content dài.
- Summary lớp dùng mẫu số `validSessionCount` và tỷ lệ backend trả về, không tự tính điểm.

## Giới hạn contract được thể hiện

- Wire enum hiện tại là `ABSENT | EXCUSED | LATE | EARLY_LEAVE`; không dùng
  `EXCUSED_ABSENCE`/`UNEXCUSED_ABSENCE` trong request FE.
- Auth/JWT chưa expose role/capability cho FE. Navigation trong wireframe là
  tĩnh và phần quyền vẫn do backend quyết định; không có control giả để chọn role.
- Office adjustment dùng contract alias riêng; endpoint selection cần capability
  adapter đã được phê duyệt, không suy role từ token.
- Dữ liệu trong bảng là fixture minh họa để review bố cục và interaction.

## Liên kết

- Developer Plan:
  [`056-attendance-workspace-ui-2026-08-28.md`](../../../../dev-impl-plan/fe/attendance/056-attendance-workspace-ui-2026-08-28.md)
