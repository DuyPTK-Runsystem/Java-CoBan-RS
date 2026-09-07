# Wireframe Plan 052 — Academic Year & Semester UI

## Mục đích

Bộ wireframe dùng để duyệt luồng màn hình trước khi phê duyệt và triển khai
Developer Plan 052. Đây là tài liệu tĩnh, không gọi backend và không phải mã
nguồn frontend production.

## Cách xem

Mở [`index.html`](index.html) bằng trình duyệt. Thanh tab phía trên cho phép
chuyển giữa hai màn danh sách và ba trạng thái dialog:

1. Danh sách năm học.
2. Dialog tạo/sửa năm học trên danh sách năm học.
3. Danh sách học kỳ theo năm học.
4. Dialog tạo/sửa học kỳ trên danh sách học kỳ.
5. Dialog trạng thái học kỳ và completeness report trên danh sách học kỳ.

## Điểm cần duyệt

- Bố cục và luồng điều hướng giữa hai màn danh sách năm học với học kỳ.
- Các field của form tạo/sửa.
- Vị trí status tag và action theo lifecycle.
- Cách trình bày metadata khóa và cảnh báo dữ liệu điểm chưa hoàn chỉnh.
- Kích thước, backdrop, nút đóng/hủy và action footer của ba dialog.
- Các dialog xác nhận dự kiến: đóng năm học, kích hoạt/khóa học kỳ và mở lại
  học kỳ có lý do. Form và trạng thái học kỳ là dialog state, không tách thành
  route hoặc page-level view riêng.

## Giới hạn contract hiện tại

- API danh sách năm học trả toàn bộ danh sách, chưa có query search/filter/page;
  wireframe thể hiện tìm kiếm/lọc cục bộ.
- Auth response/JWT chưa expose role nên không thiết kế menu theo role suy đoán.
- Semester response chỉ trả `lockedBy` dạng ID, chưa trả `lockSource` hoặc user
  summary; màn trạng thái khóa không suy đoán tên người hay nguồn khóa.
- Backend chưa có endpoint đóng học kỳ riêng; `CLOSED` được xem là trạng thái
  chỉ đọc khi API trả về.

## Liên kết

- Developer Plan draft:
  [`052-academic-year-semester-ui-2026-08-27.md`](../../../../dev-impl-plan/fe/academic/052-academic-year-semester-ui-2026-08-27.md)
