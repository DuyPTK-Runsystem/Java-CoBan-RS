# Developer Plan 068: Chuyển đổi Phân hệ Quản lý Học sinh sang V2 (/v2/students)

## Trạng thái
- Status: `IN-PROGRESS / APPROVED VIA SESSION CONTINUATION`.
- Application version: `v2`.
- Parent context: Kế hoạch tổng thể tại `PROJECT.md`, `ORIGINAL_REQUEST.md`, `document/dev-impl-plan/FE/student/015-...`, `016-...`, `021-...` và `CR-STUDENT-001`.
- Triển khai tiếp nối phiên làm việc lúc 5:00 PM (commit `9e5ba7b` hoàn thành Milestone 1 & M-TEST).

---

## 1. Bối cảnh & Phân tích Chuyển đổi từ /student sang /v2/student

Dựa trên các Dev Plan nền tảng ban đầu:
- **FE Plan 015, 016, 021**: Quản lý học sinh cơ bản, định dạng ngày `dd-mm-yyyy`, xuất CSV, phân trang server-side 10/20/50.
- **BE Plan 005, 015.1, 042.1, 043**: Aggregate `Student` + `StudentInfo`, định danh nghiệp vụ `studentCode` (CR-STUDENT-001 Change A), API tạo học sinh kèm cấp tài khoản `POST /api/v3/students` (Change B).
- **Tài liệu v1 (`v1/modules/StudentModule.md`)**: Mô tả luồng CRUD đơn lập, xóa cứng `DELETE /api/v1/students/{id}`, phụ thuộc trường cũ `averageScore`.
- **Tài liệu v2 (`v2/modules/StudentModule.md`, `RequirementBaseline.md`, `03-StudentsAndEnrollment.md`)**: Phân hệ học vụ tích hợp nằm trong `AuthenticatedV2ShellView`, định danh `studentCode`, quản lý tài khoản `app_user`, liên kết 4 phân hệ (Xếp lớp, Điểm danh, Sổ điểm, Bảng điểm) và chính sách xóa an toàn R5 bảo vệ toàn vẹn lịch sử.

---

## 2. Mục tiêu & Các Milestone

1. **Milestone 1: Navigation, Routing & Shell V2 Integration** (Trạng thái: **DONE** - commit `9e5ba7b`)
   - Chuyển hướng login fallback về `/v2`.
   - Router guard `guestOnly` chuyển hướng người dùng đã xác thực về `/v2`.
   - Đăng ký subtree `/v2/students*` trong children của `/v2`.
   - Sidebar V2 thêm menu "Hồ sơ học sinh" (`pi pi-user`), phân quyền cho `ADMIN, ACADEMIC_OFFICE, TEACHER`, đồng bộ active state.
2. **Milestone 2: Student List, Multi-dimensional Search & Safe Lifecycle (R2 & R5)** (Trạng thái: **IN-PROGRESS**)
   - Nâng cấp `StudentTable.vue`: thêm cột `gender`, `status`, `currentClassCode`, link drill-down vào `studentCode` và `studentName`.
   - Nâng cấp `StudentSearchForm.vue`: thêm bộ lọc `status` và `classId`.
   - Nâng cấp `StudentListView.vue`: layout con V2 sạch sẽ, hỗ trợ cảnh báo xóa an toàn R5.
3. **Milestone 3: Student Creation & Account Provisioning V3 (R3)** (Trạng thái: **IN-PROGRESS**)
   - Nâng cấp `StudentForm.vue` & `StudentFormView.vue`: thêm tùy chọn cấp tài khoản V3 (`POST /api/v3/students`) cho `ADMIN`/`ACADEMIC_OFFICE`.
   - Tự động sinh username, password mặc định an toàn, bảo vệ không lộ credentials.
   - Bắt và hiển thị lỗi 409 Conflict rõ ràng (trùng mã / trùng username).
4. **Milestone 4: Student Detail 4-Tab Workspace (R4)** (Trạng thái: **IN-PROGRESS**)
   - Xây dựng `StudentDetailView.vue` với 4 Tabs:
     - Tab 1: Hồ sơ cá nhân & Tài khoản người dùng.
     - Tab 2: Xếp lớp & Lịch sử chuyển lớp (`/api/v2/students/{id}/enrollments`).
     - Tab 3: Chuyên cần & Lịch sử điểm danh (`/api/v2/attendance/students/{id}/history`).
     - Tab 4: Bảng điểm & Học bạ (`/api/v2/transcripts/students/{id}/...`) + Nút Yêu cầu tính lại điểm (`/recalculate`).
   - Cập nhật route `/v2/students/:studentId` trỏ vào view chính thức.
5. **Milestone 5: Verification & Dev Note (Acceptance Criteria)** (Trạng thái: **PLANNED**)
   - Toàn bộ 61 test cases 4 Tiers trong `FE/src/tests/e2e/` pass 100%.
   - Toàn bộ unit tests FE pass 100%.
   - `npm --prefix FE run build` pass không có lỗi TypeScript.
   - Xuất bản Dev Note `068-student-v2-migration-workspace-2026-09-05.md`.
