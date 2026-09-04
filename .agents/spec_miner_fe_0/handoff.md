# HANDOFF REPORT — FE Spec Miner

**Agent Folder**: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/spec_miner_fe_0`  
**Target Milestone**: Spec Mining & Architecture Analysis (Frontend)  
**Date**: 2026-09-04  
**Type**: Hard Handoff (Task Complete)

---

## 1. OBSERVATION

1. **Router (`FE/src/router/index.ts`)**:
   - Dòng 35-51: Routes `/students`, `/students/new`, `/students/:studentId/edit` đang là routes độc lập cấp cao nhất, không nằm trong Shell v2.
   - Dòng 53-149: Tuyến đường `/v2` bọc bởi `AuthenticatedV2ShellView.vue` với các route con học vụ nhưng **chưa có** route con `students`.
   - Dòng 159-162: Guard `guestOnly`:
     ```typescript
     if (to.meta.guestOnly && authenticated) {
       return { name: 'students' }
     }
     ```
     Đang chuyển hướng người dùng đã xác thực về route legacy `{ name: 'students' }` (tức `/students`) thay vì `/v2`.
2. **Login Redirect (`FE/src/views/LoginView.vue`)**:
   - Dòng 17: `const successRedirect = ref('/students')`
   - Dòng 21-24:
     ```typescript
     return typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')
       ? redirect
       : '/students'
     ```
     Fallback URL sau khi đăng nhập thành công đang trỏ cứng về `'/students'` thay vì `'/v2'`.
3. **Sidebar V2 (`FE/src/views/AuthenticatedV2ShellView.vue`)**:
   - Dòng 12-49: `navigation` computed property định nghĩa menu danh sách phân hệ học vụ gồm 9 mục cơ sở và các mục phân quyền theo vai trò (`STUDENT`, `isNonStudent`, `ADMIN`/`ACADEMIC_OFFICE`).
   - **Chưa có** menu "Hồ sơ học sinh" với icon `pi pi-user` và route `/v2/students`.
4. **Student List & Search Components**:
   - `FE/src/views/StudentListView.vue`: Dòng 28 dùng `<AuthenticatedLayout>` riêng lẻ, gọi `fetchStudents` (`/api/v1/students`), nút Add student dẫn tới `/students/new`, nút Delete gọi `deleteStudent` (`DELETE /api/v1/students/{id}`).
   - `FE/src/components/StudentTable.vue`: Dòng 95-104 hiển thị `studentCode`, `studentName`, `dateOfBirth`, `address`, `averageScore` (deprecated). Thiếu các trường `gender`, `status` (ACTIVE/INACTIVE/GRADUATED), `currentClassCode`, và drill-down sang chi tiết `/v2/students/:id`.
   - `FE/src/components/StudentSearchForm.vue`: Dòng 12-16 chỉ có 3 tiêu chí: `studentCode`, `studentName`, `dateOfBirth`. Thiếu lọc theo `status` và `classId`.
5. **Tạo học sinh V3 & Bảo mật**:
   - `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/controller/StudentV3Controller.java`: Cung cấp `POST /api/v3/students` (`ReqCreateStudentV3DTO` -> `ResStudentWithAccountDTO`). Tự động sinh `username` nếu null qua `StudentUsernameGenerator`, mặc định password `12345678` nếu null. Transactional tạo User, gán role STUDENT, tạo Student. Bắt lỗi 409 Conflict cho mã học sinh hoặc username trùng.
   - `ResStudentWithAccountDTO.java`: Response chỉ trả về `Account(userId, username, role)`. Không trả về plaintext password hay hash.
   - `FE/src/components/StudentForm.vue` và `FE/src/views/StudentFormView.vue`: Hiện tại chỉ hỗ trợ tạo V1 (`POST /api/v1/students`), chưa có tùy chọn tạo tài khoản tự động V3, chưa có xử lý lỗi 409 Conflict.
6. **Màn hình Chi tiết Học sinh (Student Detail)**:
   - Trong FE hiện tại chưa có `StudentDetailView.vue`.
   - Các API học vụ tương ứng cho 4 tab đã sẵn sàng:
     - Tab 1: `GET /api/v1/students/{id}` kết hợp thông tin User account.
     - Tab 2: `GET /api/v2/students/{id}/enrollments` (`fetchStudentEnrollmentHistory` trong `enrollmentApi.ts`). UI có thể kế thừa từ `StudentEnrollmentHistoryDialog.vue`.
     - Tab 3: `GET /api/v2/attendance/students/{id}/history` (`fetchStudentAttendanceHistoryById` trong `attendanceApi.ts`). UI có thể kế thừa từ `AttendanceHistoryPanel.vue`.
     - Tab 4: `GET /api/v2/transcripts/students/{id}/...` (`fetchStudentTermTranscript`, `fetchStudentAnnualTranscript` trong `transcriptApi.ts`) và `POST /api/v2/students/{id}/transcripts/recalculate?academicYearId=...` (`recalculateTranscriptById` trong `calculationTaskApi.ts`).
7. **Test Runner & Build Tooling Results**:
   - Lệnh `npm --prefix FE run test -- --run`:
     ```
     Test Files  75 passed (75)
          Tests  324 passed (324)
       Duration  22.12s
     ```
   - Lệnh `npm --prefix FE run build`:
     ```
     vue-tsc --noEmit && vite build
     built in 4.51s (0 TypeScript errors)
     ```

---

## 2. LOGIC CHAIN

1. Từ **Observation 1 & 2**: Yêu cầu người dùng trong `ORIGINAL_REQUEST.md` (Follow-up) đòi hỏi chuyển hướng sau đăng nhập thành công vào thẳng `/v2` thay vì `/students`. Do đó, `safeRedirect()` trong `LoginView.vue` và guard `guestOnly` trong `router/index.ts` cần đồng loạt đổi fallback từ `/students` thành `/v2`.
2. Từ **Observation 1 & 3**: Tuyến đường phân hệ học sinh cần chuyển đổi từ độc lập `/students` sang tuyến đường con `/v2/students` trong `AuthenticatedV2ShellView.vue`. Để người dùng điều hướng thuận tiện, sidebar v2 cần bổ sung menu item `{ label: 'Hồ sơ học sinh', to: '/v2/students', icon: 'pi pi-user' }` hiển thị cho các vai trò `isNonStudent` (`ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`) và active khi ở các route con `/v2/students/*`.
3. Từ **Observation 4**: Màn hình danh sách học sinh v2 cần nâng cấp `StudentTable.vue` và `StudentSearchForm.vue` để hỗ trợ hiển thị `gender`, `status` (`ACTIVE`/`INACTIVE`/`GRADUATED`), `currentClassCode`, loại bỏ trường `averageScore` cũ, và cung cấp link drill-down sang `/v2/students/:id`.
4. Từ **Observation 5**: Form tạo học sinh cần hỗ trợ 2 chế độ: Chế độ V3 gọi `POST /api/v3/students` để tự sinh tài khoản đăng nhập (với xử lý lỗi 409 Conflict cho mã học sinh hoặc username trùng, bảo mật không lộ password) và Chế độ V1 gọi `POST /api/v1/students` khi chỉ cần tạo hồ sơ đơn thuần.
5. Từ **Observation 6**: Cần tạo mới view `StudentDetailView.vue` tổ chức dạng Tabbed Workspace 4 tab (Hồ sơ cá nhân, Xếp lớp, Điểm danh, Bảng điểm) tích hợp các API v2/v3 đã có sẵn trên backend và frontend services.
6. Từ **Observation 4 & 6 (R5)**: Cơ chế xóa cứng `DELETE /api/v1/students/{id}` cần được thay thế bằng chính sách xóa an toàn hoặc vô hiệu hóa hồ sơ (`status = INACTIVE`) kèm cảnh báo ràng buộc dữ liệu học vụ.
7. Từ **Observation 7**: Hệ thống test runner (Vitest: 75/75 files pass) và TypeScript compiler (`vue-tsc`: 0 errors) đang ở trạng thái hoàn hảo 100%, bảo đảm nền tảng vững chắc cho giai đoạn triển khai code tiếp theo.

---

## 3. CAVEATS

1. **Dữ liệu Gender của Sinh viên trong Database**: Entity `Student` và bảng `student` hiện tại chưa có cột `gender` (chỉ có trong `Teacher`). Khi hiển thị trên FE, trường `gender` cần được thiết kế mềm dẻo (nullable) để tương thích nếu backend bổ sung migration sau này.
2. **Quyền truy cập Tab 4 Bảng điểm**: Đối với vai trò `TEACHER`, endpoint xem bảng điểm học sinh `GET /api/v2/transcripts/students/{studentId}/...` được phép gọi, nhưng endpoint tính lại điểm `POST /api/v2/students/{id}/transcripts/recalculate` bị cấm (`OFFICE_ROLES: ADMIN, ACADEMIC_OFFICE`). Nút tính lại điểm cần ẩn hoặc vô hiệu hóa đối với `TEACHER`.
3. **Phạm vi khảo sát**: Tuân thủ tuyệt đối quy tắc Read-Only của Spec Miner, không có bất kỳ file mã nguồn FE hay BE nào bị chỉnh sửa trong phiên làm việc này.

---

## 4. CONCLUSION

Quá trình khảo sát đặc tả Frontend cho phân hệ Quản lý Học sinh & Shell v2 đã hoàn thành toàn diện và chính xác:
- Toàn bộ 5 hạng mục trọng tâm được giao trong `DISPATCH.md` đã được đối chiếu chi tiết với mã nguồn thực tế và tài liệu `ORIGINAL_REQUEST.md`.
- Đã xác định đầy đủ các file cần sửa đổi (`router/index.ts`, `LoginView.vue`, `AuthenticatedV2ShellView.vue`, `StudentListView.vue`, `StudentTable.vue`, `StudentSearchForm.vue`, `StudentForm.vue`, `types/student.ts`) và file cần tạo mới (`StudentDetailView.vue`).
- Báo cáo đặc tả chi tiết đã được lưu tại:  
  `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/spec_miner_fe_0/report.md`

---

## 5. VERIFICATION METHOD

1. **Đọc báo cáo đặc tả chi tiết**:
   - File: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/spec_miner_fe_0/report.md`
2. **Xác minh trạng thái Test Suite hiện tại**:
   ```bash
   npm --prefix FE run test -- --run
   ```
   *Kết quả mong đợi*: 75 test files passed, 324 tests passed, 0 failures.
3. **Xác minh trạng thái Build & TypeScript hiện tại**:
   ```bash
   npm --prefix FE run build
   ```
   *Kết quả mong đợi*: `vue-tsc --noEmit` hoàn thành không có lỗi, Vite build bundle thành công (exit code 0).
