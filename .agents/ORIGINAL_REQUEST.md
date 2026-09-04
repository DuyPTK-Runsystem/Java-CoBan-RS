# Original User Request

## Initial Request — 2026-09-04T09:31:00Z

Chuyển đổi phân hệ quản lý học sinh từ giao diện đơn lập v1 (/students) sang phân hệ học vụ tích hợp v2 (/v2/students) nằm trong AuthenticatedV2ShellView, kết nối dữ liệu hồ sơ cá nhân với tài khoản đăng nhập (v3 API) và chuỗi nghiệp vụ học vụ v2 (xếp lớp, điểm danh, sổ điểm, bảng điểm).

Working directory: /home/duyptk/Coding/HoiNhapJava/Java-CoBan
Integrity mode: development

## Requirements

### R1. Tích hợp tuyến đường và Shell v2 cho Học sinh (/v2/students)
Chuyển đổi tuyến đường frontend từ độc lập `/students` sang tuyến đường con `/v2/students` bên trong `AuthenticatedV2ShellView`, hiển thị mục "Hồ sơ học sinh" trên sidebar điều hướng theo vai trò (ADMIN, ACADEMIC_OFFICE, TEACHER) và đồng bộ breadcrumb/navigation.

### R2. Nâng cấp màn hình danh sách và tra cứu học sinh v2
Màn hình danh sách học sinh v2 hiển thị thông tin học sinh kết hợp trạng thái học vụ (Status: ACTIVE/INACTIVE/GRADUATED), định danh mã học sinh `studentCode` (theo CR-STUDENT-001) và lớp học hiện tại; hỗ trợ bộ lọc đa chiều (mã, tên, ngày sinh, lớp, trạng thái) và drill-down sang chi tiết học vụ.

### R3. Tích hợp tạo học sinh kèm cấp tài khoản đăng nhập (Student V3)
Giao diện thêm mới học sinh trong v2 cung cấp tùy chọn cấp tài khoản đăng nhập tự động qua `POST /api/v3/students` (dành cho ADMIN / ACADEMIC_OFFICE), tự sinh username theo chuẩn và mật khẩu khởi tạo an toàn, đồng thời giữ tùy chọn tạo hồ sơ đơn thuần qua `POST /api/v1/students` khi cần tương thích.

### R4. Nâng cấp màn hình chi tiết & chỉnh sửa học sinh tích hợp đa phân hệ học vụ
Màn hình chi tiết học sinh v2 được tổ chức dạng Tabbed Workspace hoặc Section:
- Tab 1 - Hồ sơ cá nhân: thông tin nhân khẩu học (loại bỏ trường deprecated `averageScore`, thêm `gender`, `status`), thông tin tài khoản đăng nhập (User ID, username, role).
- Tab 2 - Phân lớp & Lịch sử chuyển lớp: gọi `GET /api/v2/students/{id}/enrollments` hiển thị lớp học hiện tại và lịch sử chuyển lớp.
- Tab 3 - Chuyên cần / Điểm danh: gọi `GET /api/v2/attendance/students/{id}/history` hiển thị tỷ lệ có mặt, vắng mặt có phép/không phép.
- Tab 4 - Bảng điểm & Học bạ: tích hợp `TranscriptViewerView` hoặc gọi API bảng điểm v2 `GET /api/v2/transcripts/students/{id}/...`, cung cấp nút yêu cầu tính lại điểm khi có thẩm quyền.

### R5. Chuẩn hóa vòng đời học sinh và chính sách xóa an toàn
Thay thế cơ chế xóa cứng (`DELETE /api/v1/students/{id}`) gây vỡ dữ liệu lịch sử bằng cảnh báo ràng buộc khóa ngoại chặt chẽ hoặc chuyển đổi trạng thái hồ sơ (Status: INACTIVE/GRADUATED) để bảo toàn lịch sử phân lớp, điểm danh và sổ điểm theo quy định v2.

## Acceptance Criteria

### Điều hướng & Giao diện Shell
- [ ] Truy cập `/v2/students` hiển thị trọn vẹn trong Shell v2 với Header và Sidebar v2.
- [ ] Sidebar v2 hiển thị menu "Hồ sơ học sinh" với icon phù hợp (`pi pi-user`), kích hoạt trạng thái active đúng khi điều hướng.
- [ ] Người dùng chưa đăng nhập hoặc không đủ quyền bị chặn và điều hướng an toàn.

### Quản lý & Tra cứu học vụ
- [ ] Danh sách học sinh hiển thị đầy đủ mã học sinh `studentCode`, họ tên, ngày sinh, giới tính, trạng thái và lớp hiện tại.
- [ ] Tìm kiếm, phân trang và sắp xếp server-side hoạt động chính xác theo chuẩn API.
- [ ] Xem chi tiết học sinh hiển thị đầy đủ 4 tab dữ liệu (Hồ sơ, Xếp lớp, Điểm danh, Bảng điểm) mà không bị lỗi 403/500 đối với vai trò được phép.

### Tạo mới & Cấp tài khoản
- [ ] Thêm học sinh qua form v2 với quyền ADMIN/ACADEMIC_OFFICE gọi thành công `POST /api/v3/students`, tạo đồng thời User, Student, StudentInfo trong một transaction.
- [ ] Không để lộ plaintext password hoặc password hash trên frontend.
- [ ] Thông báo lỗi rõ ràng khi trùng mã học sinh hoặc trùng username (409 Conflict).

### Kiểm thử & Chất lượng
- [ ] Toàn bộ unit test và component test FE vượt qua: `npm --prefix FE run test -- --run`.
- [ ] Build frontend thành công không có lỗi TypeScript: `npm --prefix FE run build`.
- [ ] Backend test và kiểm tra chất lượng mã nguồn (Checkstyle/PMD) đạt chuẩn.

## Follow-up — 2026-09-04T09:32:31Z

Yêu cầu bổ sung từ người dùng: Cấu hình chuyển hướng (redirect) sau khi đăng nhập thành công vào thẳng '/v2' thay vì '/students' như trong hệ thống legacy. Vui lòng cập nhật các điểm liên quan: LoginView.vue (safeRedirect fallback về '/v2'), router/index.ts (guestOnly guard redirect về '/v2'), và các unit tests liên quan.
