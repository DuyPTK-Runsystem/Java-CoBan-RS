# Wireframe 075 — Thời khóa biểu và định mức tiết dạy

- Version v3 · 2026-09-11 · **DRAFT / REVIEW PENDING**.
- [Mở wireframe HTML](index.html).
- [Plan BE](../../../../dev-impl-plan/be/timetable/075-timetable-teacher-load-2026-09-11.md) · [Plan FE](../../../../dev-impl-plan/fe/timetable/075-timetable-teacher-load-ui-2026-09-11.md).
- HTML độc lập, không dependency/network/API, không lưu dữ liệu thật. Mở trực tiếp bằng trình duyệt. Dữ liệu, tên người và lịch là minh họa.

## Luồng review

1. Mặc định W02/W03: xem lịch tuần có trùng giáo viên; `Đến tiết học` mở editor. Đổi Toán Thứ hai tiết 1 sang ô trống rồi `Kiểm tra lịch` để mô phỏng hết lỗi.
2. Click ô trống/card để thêm/sửa; có xóa và xác nhận. `Lưu nháp` phản hồi mẫu; không ghi hệ thống.
3. Grid có Sáng 1–4, Chiều 1–4. Đổi chế độ lớp/GV/phòng chức năng, lọc buổi/tiết; đổi tuần giữ mẫu lịch lặp và có thông báo giới hạn.
4. W04: tab `Định mức tiết dạy` có bốn ví dụ 19/15/16/12 và cảnh báo vượt một tiết.
5. W05: chọn `Đã kiểm tra, đủ điều kiện` trên thanh review → `Công bố` → dialog xác nhận → read-only → `Tạo bản điều chỉnh`.
6. W06: tab `Cấu hình` có 2 buổi × 4 tiết/ngày, nguồn policy và xác nhận giảm tiết; Giáo vụ/Admin có cùng quyền. Nút kích hoạt yêu cầu điền ba metadata; chỉ là validation form mẫu, không xác nhận nguồn thật.
7. W01: `Danh sách lịch` → `Tạo thời khóa biểu` → draft trống, thiếu policy. Tạo lịch thực và pagination nằm trong plan production, không mô phỏng persistence.
8. Thanh review cho phép xem thiếu policy, stale-version/tải lại, loading, empty, forbidden, network error, published và archived. Mobile chuyển bảng sang danh sách theo tiết/ngày.

9. W07: `Phòng chức năng` → thêm/sửa/tìm/xóa phòng chưa có tham chiếu. `Môn học` → chọn môn → gán phòng chức năng. Editor Toán mặc định không hiện picker phòng, Tin học chọn phòng Tin đã gán. Đây là đề xuất nhiều phòng/môn; D10 chưa chốt tính bắt buộc.
10. W08: chọn vai trò `Giáo viên` → `Lịch bận` → `Đăng ký lịch bận`, chọn ngày/buổi/tiết hoặc cả buổi (4 tiết) → gửi `Chờ duyệt`. Chuyển `Giáo vụ` hoặc `Admin` → duyệt/từ chối; chỉ `Đã duyệt` mới chặn. Teacher có sửa đăng ký chờ duyệt và rút theo đề xuất D09. Hai vai trò quản lý có cùng control.
11. Khi duyệt đăng ký trùng tiết published trong tuần mẫu: hiển thị cần điều chỉnh lịch trước, giữ chờ duyệt. Đây là cách xử lý đề xuất, chờ chốt phần còn lại D09.

## Giới hạn review

- Rule mẫu nhận diện tình huống Toán Thứ hai Sáng tiết 1 và trùng lịch bận đã duyệt trong tuần đang xem; không phải engine kiểm tra toàn học kỳ hoặc mọi trùng lớp/GV/phòng chức năng.
- Bảng định mức là fixture độc lập, không cập nhật theo editor. Policy ready là giả định review, không policy active chính thức.
- Form calendar/policy/eligibility chỉ phản hồi mẫu; module phòng, mapping và lịch bận lưu state trong bộ nhớ trang, mất khi reload. Bộ chọn vai trò là công cụ review, không phải authorization thật.
- Trình bày W01/W06/W07/W08 nằm trong dialog/tab để review cùng file; production có route riêng theo plan FE.
- D02 đã chốt quyền; D04 đã chốt 2 × 4 và phòng chức năng; D09 đã chốt cần duyệt trước. Chi tiết còn mở tại D01/D03–D10 của plan BE. HTML không khóa quyết định nghiệp vụ, không chứng minh backend hoặc browser/live integration.

## Validation

- Inline JavaScript syntax: PASS.
- DOM interaction smoke bằng jsdom: PASS — grid 2 × 4, room theo môn, quyền Admin/Giáo vụ tương đương, teacher gửi pending, chọn cả buổi, duyệt/từ chối, tạo phòng và gán môn, các trạng thái publish. Xem Dev Note 075.
- Kiểm tra render bằng trình duyệt/screenshot: NOT RUN.
- Backend, frontend production, API/live, Storybook: NOT RUN vì task chỉ soạn plan/wireframe.
