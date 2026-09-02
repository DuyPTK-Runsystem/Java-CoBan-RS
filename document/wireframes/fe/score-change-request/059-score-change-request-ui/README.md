# Wireframe Plan 059 — Score Change Request UI

## Mục đích

Wireframe tĩnh để review Plan 059 trước production implementation. Fixture mô phỏng hai persona:

- Giáo viên: tạo yêu cầu, xem lịch sử của mình, mở chi tiết và hủy yêu cầu “Chờ duyệt” của mình.
- Giáo vụ/Quản trị viên: xem danh sách, mở chi tiết, duyệt hoặc từ chối yêu cầu.

Wireframe không gọi backend, không gửi dữ liệu thật và không thay thế Vue/PrimeVue production.

## Cách xem

Mở [`index.html`](index.html) bằng trình duyệt. Dùng bộ chọn “Trạng thái minh họa” để xem các vai trò và trạng thái đang tải, rỗng, không có quyền, không tìm thấy, xung đột, lỗi lookup; dùng tab để đổi danh sách/chi tiết và dùng các nút thao tác để mở hộp thoại.

## Các điểm cần duyệt

- Bố cục list + filter + pagination.
- Form tạo yêu cầu và phân biệt snapshot trước sửa với giá trị đề xuất.
- Chọn bối cảnh năm học → học kỳ → lớp → môn/lớp học phần; tự lookup, không nhập mã sổ điểm.
- Màn hình giáo vụ với duyệt/từ chối; từ chối yêu cầu lý do.
- Hủy chỉ xuất hiện cho yêu cầu “Chờ duyệt” phù hợp quyền sở hữu.
- Nhãn trạng thái tiếng Việt cho toàn bộ vòng đời.
- Đang tải/không có dữ liệu/401/403/404/409/lỗi lookup.
- Responsive layout và keyboard-visible controls.

## Giới hạn contract được thể hiện

- Snapshot/reason chỉ xuất hiện ở detail vì list DTO không có các field đó.
- Tên lớp/môn/cột điểm là fixture display; backend detail hiện documented chủ yếu trả mã số, không khẳng định đây là DTO field.
- Mã sổ điểm chỉ là định danh kỹ thuật ẩn sau lookup; không xuất hiện như ô nhập hoặc điều hướng chính.
- Actions chỉ là visual fixture; quyền và ownership cuối cùng do backend quyết định.
- Không mô phỏng calculation worker, audit screen độc lập hoặc official average.

## Liên kết

- [Developer Plan 059](../../../../dev-impl-plan/FE/scorebook/059-score-change-request-ui-2026-09-03.md)
- [BE Plan 038](../../../../dev-impl-plan/BE/scorebook/038-score-change-request-2026-08-25.md)
- [Frontend API v2](../../../../application-doc/v2/frontend-api/05-scorebook-change-audit.md)
