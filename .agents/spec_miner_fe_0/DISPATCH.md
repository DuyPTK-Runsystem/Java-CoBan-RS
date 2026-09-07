# DISPATCH: Frontend Architecture & UI Spec Mining

## Objective
Thực hiện khảo sát toàn diện mã nguồn Frontend (FE) của dự án liên quan đến quản lý học sinh và chuyển đổi sang Shell v2 theo yêu cầu trong ORIGINAL_REQUEST.md.

## Scope Boundaries
- Chỉ khảo sát và phân tích mã nguồn FE, tests, route, components hiện có (Read-only).
- TUYỆT ĐỐI KHÔNG sửa đổi mã nguồn frontend trong giai đoạn Survey này.

## Input Information
- User request: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/ORIGINAL_REQUEST.md` (BẮT BUỘC đọc file này trước tiên)
- Frontend code: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/FE`
- Working directory: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/spec_miner_fe_0`

## Output Requirements
Viết báo cáo phân tích chi tiết vào `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/spec_miner_fe_0/report.md` và `handoff.md`.
Báo cáo cần làm rõ:
1. Routing & Shell v2:
   - Cấu trúc router hiện tại (`FE/src/router/index.ts`), route `/v2` và `AuthenticatedV2ShellView`.
   - Chuyển hướng redirect sau đăng nhập thành công: `LoginView.vue` (safeRedirect fallback về `/v2`), router guard guestOnly.
   - Cấu hình Sidebar v2 (`SidebarV2` hoặc tương đương): danh sách menu, role-based menu (ADMIN, ACADEMIC_OFFICE, TEACHER), icon (`pi pi-user`), trạng thái active route.
   - Route `/v2/students` và các route con.
2. Danh sách & tra cứu học sinh v2:
   - Các component hiển thị danh sách học sinh hiện có (v1 vs v2).
   - Hiển thị studentCode (theo CR-STUDENT-001), họ tên, ngày sinh, giới tính, trạng thái (ACTIVE/INACTIVE/GRADUATED), lớp hiện tại.
   - Bộ lọc đa chiều, tìm kiếm, phân trang, sắp xếp server-side, drill-down sang chi tiết.
3. Tạo học sinh kèm cấp tài khoản (Student V3):
   - Form tạo học sinh v2, tùy chọn cấp tài khoản tự động qua `POST /api/v3/students` vs hồ sơ đơn thuần `POST /api/v1/students`.
   - Sinh username/mật khẩu an toàn, không để lộ plaintext password/hash trên FE, bắt lỗi 409 Conflict.
4. Màn hình chi tiết học sinh v2 (4 tab):
   - Tab 1: Hồ sơ cá nhân (không có averageScore deprecated, thêm gender, status, thông tin User).
   - Tab 2: Phân lớp & lịch sử chuyển lớp (`GET /api/v2/students/{id}/enrollments`).
   - Tab 3: Chuyên cần / Điểm danh (`GET /api/v2/attendance/students/{id}/history`).
   - Tab 4: Bảng điểm & Học bạ (`TranscriptViewerView` hoặc `/api/v2/transcripts/...`).
5. Hiện trạng FE tests & build tooling:
   - Test runner: `npm --prefix FE run test -- --run`
   - Build check: `npm --prefix FE run build`
   - Các unit test và component test hiện có và các file cần update/thêm mới.

## Completion Criteria
Báo cáo đầy đủ, rõ ràng, trích dẫn file code và component cụ thể trong FE.

