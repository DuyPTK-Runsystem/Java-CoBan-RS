# BÁO CÁO KHẢO SÁT & ĐẶC TẢ FRONTEND (FE SPEC MINING)
**Dự án**: Java-CoBan (Hệ thống Quản lý Học sinh & Học vụ v2)  
**Tài liệu tham chiếu**: `ORIGINAL_REQUEST.md`, `FE/src/*`, `BE/BaiTap-RS/src/*`  
**Ngày thực hiện**: 2026-09-04  
**Trạng thái**: Hoàn tất khảo sát (Read-only Spec Mining)

---

## 1. TỔNG QUAN HIỆN TRẠNG VÀ PHẠM VI CHUYỂN ĐỔI (GAP ANALYSIS)

Hệ thống Frontend hiện tại đang tồn tại ở trạng thái lai ghép (hybrid):
- **Phân hệ Học vụ V2**: Đã vận hành trong Shell v2 (`AuthenticatedV2ShellView.vue` tại route `/v2/...`) gồm Năm học, Khối, Lớp, Môn học, Xếp lớp (`/v2/enrollments`), Giáo viên, Phân công giảng dạy, Điểm danh (`/v2/attendance`), Sổ điểm (`/v2/scorebooks`), Bảng điểm (`/v2/transcripts`), Thi lại (`/v2/retake-exams`), Vận hành tính điểm (`/v2/scorebooks/operations`).
- **Phân hệ Học sinh (Legacy V1)**: Đang chạy ở các tuyến đường độc lập cấp cao nhất:
  - `/students`: `StudentListView.vue` (sử dụng trực tiếp `AuthenticatedLayout.vue`, gọi API `GET /api/v1/students`).
  - `/students/new`: `StudentFormView.vue` (gọi API `POST /api/v1/students`).
  - `/students/:studentId/edit`: `StudentFormView.vue` (gọi API `PUT /api/v1/students/{id}`).
  - Xóa sinh viên: gọi `DELETE /api/v1/students/{id}` (xóa cứng - vi phạm R5 làm vỡ khóa ngoại phân lớp/điểm danh).
  - Chưa có màn hình Chi tiết học sinh (Student Detail) dạng Tabbed Workspace.
- **Điều hướng và xác thực**:
  - `LoginView.vue`: Sau khi đăng nhập thành công, `safeRedirect` fallback về `/students` (thay vì `/v2`).
  - `router/index.ts`: Guard `guestOnly` chuyển hướng người dùng đã xác thực về route có name `students` (tức `/students`).
  - `AuthenticatedV2ShellView.vue`: Sidebar chưa có mục "Hồ sơ học sinh" (`/v2/students`) với icon `pi pi-user` dành cho các vai trò `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`.

---

## 2. KHẢO SÁT CHI TIẾT THEO TỪNG HẠNG MỤC

### 2.1. Routing & Shell v2 (`router/index.ts`, `LoginView.vue`, `AuthenticatedV2ShellView.vue`)

#### 2.1.1. Hiện trạng Router (`FE/src/router/index.ts`)
- **Đường dẫn**: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/FE/src/router/index.ts`
- **Cấu hình route hiện tại**:
  - Dòng 35-51: Đăng ký các route độc lập:
    - `/students` -> `StudentListView.vue` (name: `'students'`)
    - `/students/new` -> `StudentFormView.vue` (name: `'student-create'`)
    - `/students/:studentId/edit` -> `StudentFormView.vue` (name: `'student-edit'`)
  - Dòng 53-149: Đăng ký route cha `/v2` bọc bởi `AuthenticatedV2ShellView.vue` với các route con (`academic-years`, `academic-catalog/*`, `enrollments`, `teachers`, `teaching-assignments`, `attendance`, `scorebooks`, `score-change-requests`, `transcripts`, `class-transcripts`, `retake-exams`, `scorebooks/operations`).
  - **Thiếu hụt**: Hoàn toàn chưa có route con `students` bên dưới `/v2`.
  - Dòng 154-163: Guard `router.beforeEach`:
    ```typescript
    if (to.meta.guestOnly && authenticated) {
      return { name: 'students' } // Cần sửa thành redirect về '/v2' hoặc { path: '/v2' }
    }
    ```
- **Đặc tả chuyển đổi**:
  - Thêm các route con bên dưới `/v2` trong children của `/v2`:
    - `students` (name: `'v2-students'`): `StudentListView.vue`
    - `students/new` (name: `'v2-student-create'`): `StudentFormView.vue`
    - `students/:studentId` (name: `'v2-student-detail'`): `StudentDetailView.vue` (mới)
    - `students/:studentId/edit` (name: `'v2-student-edit'`): `StudentFormView.vue`
  - Cập nhật guard `guestOnly`: đổi fallback từ `{ name: 'students' }` sang `'/v2'`.
  - Giữ lại các alias hoặc redirect từ `/students` cũ sang `/v2/students` để tương thích ngược.

#### 2.1.2. Chuyển hướng sau đăng nhập (`FE/src/views/LoginView.vue`)
- **Đường dẫn**: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/FE/src/views/LoginView.vue`
- **Hiện trạng code**:
  - Dòng 17: `const successRedirect = ref('/students')`
  - Dòng 19-24:
    ```typescript
    function safeRedirect(): string {
      const redirect = router.currentRoute.value.query.redirect
      return typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')
        ? redirect
        : '/students' // Cần chuyển fallback về '/v2'
    }
    ```
  - Dòng 51: `await router.replace(successRedirect.value)` sau khi người dùng bấm "Close" trên dialog thành công.
- **Đặc tả chuyển đổi**:
  - Đổi giá trị khởi tạo: `const successRedirect = ref('/v2')`.
  - Đổi fallback trong `safeRedirect()` thành `'/v2'`.
  - Kiểm tra unit test `LoginView.spec.ts` (dòng 57, 80) để cập nhật các case test redirect.

#### 2.1.3. Sidebar v2 & Authenticated Layout (`AuthenticatedV2ShellView.vue` & `AuthenticatedLayout.vue`)
- **Đường dẫn**:
  - `FE/src/views/AuthenticatedV2ShellView.vue` (dòng 12-49)
  - `FE/src/components/AuthenticatedLayout.vue` (dòng 18-21, 27)
- **Hiện trạng menu Sidebar**:
  - Menu cơ sở gồm 9 mục: Năm học & học kỳ, Khối, Lớp, Môn học, Quản lí môn học các lớp, Xếp lớp, Hồ sơ giáo viên, Phân công giảng dạy, Điểm danh.
  - Phân quyền theo vai trò:
    - `STUDENT`: thêm menu "Bảng điểm" (`/v2/transcripts`, icon `pi pi-table`).
    - `isNonStudent` (`ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`): thêm "Bảng điểm theo lớp" (`/v2/class-transcripts`, icon `pi pi-list`), "Sổ điểm" (`/v2/scorebooks`, icon `pi pi-book`), "Yêu cầu sửa điểm" (`/v2/score-change-requests`, icon `pi pi-file-edit`).
    - `ADMIN` & `ACADEMIC_OFFICE`: thêm "Kết quả thi lại" (`/v2/retake-exams`, icon `pi pi-check-square`), "Vận hành tính điểm" (`/v2/scorebooks/operations`, icon `pi pi-cog`).
- **Đặc tả yêu cầu R1 cho Sidebar**:
  - Thêm menu item **"Hồ sơ học sinh"** vào danh sách điều hướng.
  - Cấu hình item:
    - `label`: `'Hồ sơ học sinh'`
    - `to`: `'/v2/students'`
    - `icon`: `'pi pi-user'` (đúng chuẩn yêu cầu R1 và Acceptance Criteria)
    - Vị trí: Hiển thị ngay sau hoặc gần mục "Xếp lớp" (`/v2/enrollments`).
    - Quyền truy cập: Chỉ hiển thị cho các vai trò `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER` (tức `isNonStudent`).
    - Trạng thái Active: Kích hoạt class `router-link-active` khi route hiện tại là `/v2/students` hoặc các route con `/v2/students/:id`, `/v2/students/new`, `/v2/students/:id/edit`. Có thể gán thuộc tính `active: Boolean(route?.path?.startsWith('/v2/students'))` trong item để bảo đảm đồng bộ.
  - Sửa link thương hiệu logo trong `AuthenticatedLayout.vue` (dòng 27): đổi `to="/students"` sang `to="/v2"` (hoặc prop động).

---

### 2.2. Danh sách & tra cứu học sinh v2 (`StudentListView.vue`, `StudentTable.vue`, `StudentSearchForm.vue`)

#### 2.2.1. Hiện trạng các component
1. **`FE/src/views/StudentListView.vue`**:
   - Sử dụng layout độc lập `<AuthenticatedLayout>`, không nằm trong Shell v2 RouterView.
   - Gọi API `fetchStudents(accessToken, query)` -> `GET /api/v1/students`.
   - Query chỉ gửi: `page`, `pageSize`, `sortField`, `sortDirection`, `studentCode`, `studentName`, `birthday`.
   - Nút "Add student" điều hướng sang `/students/new`.
   - Xóa học sinh: Gọi trực tiếp `deleteStudent` (`DELETE /api/v1/students/{id}`) bằng popup xác nhận cơ bản.
2. **`FE/src/components/StudentTable.vue`**:
   - Các cột hiển thị:
     - `No` (số thứ tự)
     - `Code` (`studentCode`, sortable)
     - `Name` (`studentName`, sortable)
     - `Birthday` (`dateOfBirth`)
     - `Address` (`address`)
     - `Score` (`averageScore`, sortable) -> **Deprecated**
     - `Actions` (nút Edit bút chì, nút Delete thùng rác)
   - **Thiếu sót so với R2**:
     - Chưa có cột Giới tính (`gender`).
     - Chưa có cột Trạng thái học vụ (`status`: `ACTIVE`, `INACTIVE`, `GRADUATED`) với StatusTag.
     - Chưa có cột Lớp hiện tại (`currentClassName` / `currentClassCode`).
     - Chưa có hành động Drill-down xem chi tiết học sinh (`/v2/students/:studentId`).
3. **`FE/src/components/StudentSearchForm.vue`**:
   - Form tìm kiếm chỉ có: Mã học sinh (`studentCode`), Tên học sinh (`studentName`), Ngày sinh (`dateOfBirth`).
   - **Thiếu sót so với R2**:
     - Thiếu bộ lọc Lớp học (`classId` hoặc `className`).
     - Thiếu bộ lọc Trạng thái (`status`: `ALL`, `ACTIVE`, `INACTIVE`, `GRADUATED`).

#### 2.2.2. Đặc tả nâng cấp V2
- **Cấu trúc dữ liệu `types/student.ts`**:
  ```typescript
  export type StudentGender = 'MALE' | 'FEMALE' | 'OTHER'
  export type StudentAcademicStatus = 'ACTIVE' | 'INACTIVE' | 'GRADUATED'

  export interface Student {
    studentId: number
    studentCode: string
    studentName: string
    dateOfBirth: string
    gender?: string | null
    address: string
    status: StudentAcademicStatus
    currentClassCode?: string | null
    currentClassId?: number | null
    userId?: number | null
    averageScore?: number | null // deprecated
  }

  export interface StudentSearchValues {
    studentCode: string
    studentName: string
    dateOfBirth: Date | null
    status?: StudentAcademicStatus | ''
    classId?: number | null
  }
  ```
- **Bảng dữ liệu `StudentTable.vue`**:
  - Cột 1: STT (`No`).
  - Cột 2: Mã học sinh (`studentCode`) - hiển thị dạng link drill-down sang chi tiết `/v2/students/${student.studentId}`.
  - Cột 3: Họ tên (`studentName`) - link drill-down.
  - Cột 4: Ngày sinh (`dateOfBirth`) format `dd/MM/yyyy`.
  - Cột 5: Giới tính (`gender`): Nam / Nữ / Khác.
  - Cột 6: Lớp hiện tại (`currentClassCode`): Nếu chưa xếp lớp, hiển thị tag hoặc text "Chưa xếp lớp".
  - Cột 7: Trạng thái (`status`): Tag màu (ACTIVE: xanh lá `success`, INACTIVE: xám `secondary`, GRADUATED: xanh dương `info`).
  - Cột 8: Thao tác: Nút Xem chi tiết (icon `pi pi-eye`), Nút Sửa (icon `pi pi-pencil`), Nút Đổi trạng thái/Ngừng hoạt động (thay thế xóa cứng theo R5).
- **Cơ chế tải dữ liệu tích hợp**:
  - Vì `GET /api/v1/students` trả về danh sách học sinh cơ bản, màn hình V2 có thể kết hợp dữ liệu enrollment qua `GET /api/v2/classes/{classId}/students` hoặc gọi `GET /api/v2/students/{id}/enrollments` để hiển thị lớp học hiện tại.

---

### 2.3. Tạo học sinh kèm cấp tài khoản - Student V3 (`StudentFormView.vue`, `StudentForm.vue`, `StudentV3Controller`)

#### 2.3.1. Đặc tả Backend API V3
- **Controller**: `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/controller/StudentV3Controller.java`
- **Endpoint**: `POST /api/v3/students`
- **Phân quyền**: `@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")`
- **Request Body (`ReqCreateStudentV3DTO`)**:
  - `studentCode` (bắt buộc, regex `STU[0-9]{7}`)
  - `studentName` (bắt buộc, max 35 chars)
  - `dateOfBirth` (bắt buộc/past or present)
  - `address` (max 255 chars)
  - `averageScore` (tùy chọn, 0.0 - 10.0, legacy)
  - `username` (tùy chọn, max 20 chars, ASCII, tự sinh nếu để trống qua `StudentUsernameGenerator`)
  - `password` (tùy chọn, 6-15 chars, ASCII, mặc định "12345678" nếu để trống)
- **Response Body (`ResStudentWithAccountDTO`)**:
  - `studentId`, `studentCode`, `studentName`, `dateOfBirth`, `address`, `averageScore`
  - `account`: `{ userId: Long, username: String, role: "STUDENT" }`
  - **Lưu ý an ninh tối quan trọng**: Không trả về plaintext password và không trả về password hash.
- **Xử lý mã lỗi HTTP 409 Conflict**:
  - Trùng `studentCode`: `"Mã sinh viên đã tồn tại"`
  - Trùng `username`: `"Tên đăng nhập đã tồn tại"`

#### 2.3.2. Form tạo mới học sinh trên Frontend
- **Hai chế độ tạo (Toggle / Radio Selection)**:
  1. **Chế độ V3 (Mặc định - Cấp tài khoản tự động)**:
     - Dành cho `ADMIN` / `ACADEMIC_OFFICE`.
     - Tùy chọn sinh username tự động theo chuẩn quy định hoặc cho phép nhập username thủ công.
     - Tùy chọn mật khẩu mặc định an toàn (`12345678`) hoặc cho phép nhập mật khẩu khởi tạo (ẩn mật khẩu qua InputPassword).
     - Gọi `POST /api/v3/students`.
     - Sau khi tạo thành công: Hiển thị thông báo thành công kèm thông tin tài khoản đã tạo (`Username: ...`, `Role: STUDENT`). Tuyệt đối không lưu trữ hay hiển thị password sau khi submit.
  2. **Chế độ V1 (Tương thích - Chỉ tạo hồ sơ học sinh)**:
     - Gọi `POST /api/v1/students` với payload truyền thống `StudentPayload`.
     - Không tạo `User` và `Role`.
- **Xử lý lỗi**:
  - Bắt lỗi HTTP 409 từ `apiClient` và hiển thị cảnh báo cụ thể:
    - Nếu thông báo chứa "Mã sinh viên": focus và báo lỗi trường `studentCode`.
    - Nếu thông báo chứa "Tên đăng nhập": focus và báo lỗi trường `username`.

---

### 2.4. Màn hình chi tiết học sinh v2 - 4 Tab Workspace (`StudentDetailView.vue`)

Màn hình chi tiết học sinh mới (`StudentDetailView.vue` đặt tại `/v2/students/:studentId`) được thiết kế dạng Tabbed Workspace đồng bộ phong cách với `AttendanceWorkspaceView` và `ScorebookWorkspaceView`.

#### Tab 1: Hồ sơ cá nhân (Student Profile & Account)
- **Dữ liệu nguồn**: `GET /api/v1/students/{id}` kết hợp thông tin User account.
- **Thông tin hiển thị**:
  - **Nhân khẩu học**: Mã học sinh (`studentCode`), Họ và tên (`studentName`), Ngày sinh (`dateOfBirth`), Giới tính (`gender`), Địa chỉ (`address`), Trạng thái học tập (`status`: `ACTIVE`/`INACTIVE`/`GRADUATED`).
  - **Loại bỏ**: Trường điểm trung bình cũ `averageScore` (không hiển thị ở tab hồ sơ cá nhân).
  - **Tài khoản liên kết**: Nếu có `userId`: hiển thị ID tài khoản, Username, Vai trò (`STUDENT`). Nếu chưa có tài khoản: hiển thị nút "Cấp tài khoản đăng nhập" (dành cho Admin).
- **Hành động**:
  - Nút "Chỉnh sửa hồ sơ" -> chuyển sang màn hình edit hoặc mở modal chỉnh sửa.
  - Nút "Chuyển trạng thái" (Nghỉ học, Tốt nghiệp) theo chính sách xóa an toàn R5.

#### Tab 2: Phân lớp & Lịch sử chuyển lớp (Enrollment History)
- **API Backend**: `GET /api/v2/students/{studentId}/enrollments`
- **Quyền gọi API**: `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`
- **Dữ liệu trả về**: `List<ResStudentEnrollmentHistoryDTO>`:
  - `enrollment`: `{ id, studentId, studentCode, studentName, academicYearId, currentClassId, currentClassCode, status, enrolledAt, completedAt }`
  - `transfers`: danh sách `ResTransferHistoryDTO`: `{ transferId, fromClassId, toClassId, effectiveAt, reason }`
- **Giao diện**:
  - Thẻ thông tin Lớp hiện tại: Mã lớp (`currentClassCode`), Năm học, Ngày vào lớp, Trạng thái enrollment (`Đang học` / `Đã hoàn thành` / `Đã thôi học`).
  - Bảng timeline lịch sử chuyển lớp: Từ lớp -> Đến lớp, Ngày hiệu lực, Lý do chuyển lớp.
  - Tái sử dụng template và logic trình bày từ `StudentEnrollmentHistoryDialog.vue`.

#### Tab 3: Chuyên cần & Điểm danh (Attendance History)
- **API Backend**: `GET /api/v2/attendance/students/{studentId}/history`
- **Service Frontend đã có sẵn**: `fetchStudentAttendanceHistoryById(token, studentId, query)` trong `FE/src/services/attendanceApi.ts` (dòng 81-92).
- **Quyền gọi API**: `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER` (riêng học sinh gọi qua endpoint `/me/history`).
- **Giao diện**:
  - Bộ lọc: Chọn Năm học, Học kỳ, Từ ngày, Đến ngày.
  - Khối thẻ thống kê tổng quan (`AttendanceSummaryCards`):
    - Tổng số buổi học hợp lệ
    - Số buổi có mặt (tỷ lệ %)
    - Vắng có phép (`excusedAbsenceCount`)
    - Vắng không phép (`unexcusedAbsenceCount`)
    - Đi trễ (`lateCount`), Về sớm (`earlyLeaveCount`)
  - Bảng chi tiết từng buổi điểm danh: Ngày, Buổi (Sáng/Chiều), Lớp, Trạng thái điểm danh (Tag), Ghi chú ngoại lệ, Phân trang server-side (`ServerPagination`).
  - Tái sử dụng các thẻ và components từ `AttendanceHistoryPanel.vue`.

#### Tab 4: Bảng điểm & Học bạ (Transcripts & Recalculation)
- **API Backend**:
  - Xem bảng điểm học kỳ: `GET /api/v2/transcripts/students/{studentId}/semesters/{semesterId}`
  - Xem bảng điểm năm học: `GET /api/v2/transcripts/students/{studentId}/academic-years/{academicYearId}`
  - Trạng thái tính điểm: `GET /api/v2/transcripts/students/{studentId}/semesters/{semesterId}/status`
  - Yêu cầu tính lại điểm: `POST /api/v2/students/{studentId}/transcripts/recalculate?academicYearId={academicYearId}` (hoặc theo mã sinh viên).
- **Service Frontend đã có sẵn**:
  - `fetchStudentTermTranscript`, `fetchStudentAnnualTranscript`, `fetchStudentTermStatus`, `fetchStudentAnnualStatus` trong `FE/src/services/transcriptApi.ts`.
  - `recalculateTranscriptById(token, studentId, academicYearId)` trong `FE/src/services/calculationTaskApi.ts` (dòng 64-75).
- **Giao diện**:
  - Bộ lọc: Chọn Năm học, Học kỳ.
  - Hiển thị bảng điểm học kỳ (`TranscriptTermTable.vue`) hoặc bảng điểm cả năm (`TranscriptAnnualTable.vue`).
  - Thẻ thông tin trạng thái tính điểm (`TranscriptStatusCard.vue`): Trạng thái (`CALCULATED`, `PENDING`, `STALE`), Phiên bản (`calculatedVersion`), Thời điểm tính gần nhất.
  - **Nút "Yêu cầu tính lại điểm" (Request Recalculation)**:
    - Quyền hạn: Chỉ hiển thị/kích hoạt cho vai trò `ADMIN` và `ACADEMIC_OFFICE` (quy định tại `CalculationTaskController.java` `OFFICE_ROLES`).
    - Khi click: Gọi `recalculateTranscriptById`, hiển thị thông báo tiến trình đã gửi (`202 Accepted`) và làm mới trạng thái bảng điểm.

---

### 2.5. Vòng đời học sinh và chính sách xóa an toàn (Safe Lifecycle & Inactivation - R5)

- **Vấn đề của phương pháp cũ**:
  - `StudentListView.vue` hiện tại gọi `deleteStudent(token, id)` (`DELETE /api/v1/students/{id}`).
  - Trong cơ sở dữ liệu V2, sinh viên đã có quan hệ ràng buộc khóa ngoại (foreign key) với bảng `class_enrollment`, `attendance_record`, `student_score`, `retake_exam`. Việc xóa cứng sẽ gây lỗi 500 vi phạm Foreign Key Constraint hoặc làm vỡ tính toàn vẹn dữ liệu lịch sử.
- **Giải pháp Frontend**:
  1. Thay thế nút "Delete" cứng bằng nút "Ngừng hoạt động / Cập nhật trạng thái" (Deactivate / Change Status).
  2. Hiển thị hộp thoại cảnh báo: "Không thể xóa vĩnh viễn học sinh đã có dữ liệu học vụ (xếp lớp, điểm danh, sổ điểm). Bạn có muốn chuyển trạng thái học sinh sang INACTIVE hoặc GRADUATED không?".
  3. Bắt lỗi HTTP 400/409/500 nếu backend từ chối xóa và hiển thị hướng dẫn người dùng chuyển trạng thái hồ sơ thay vì xóa.

---

## 3. HIỆN TRẠNG BUILD TOOLING & TEST RUNNER

### 3.1. Kết quả kiểm tra công cụ kiểm thử (Test Runner)
- Lệnh chạy: `npm --prefix FE run test -- --run`
- **Kết quả ghi nhận**:
  - **Test Files**: 75 passed / 75 total (100% PASS).
  - **Tests**: 324 passed / 324 total (100% PASS).
  - **Thời gian thực thi**: ~22.12 giây.
  - Không có test nào bị fail hoặc bị skip.

### 3.2. Kết quả kiểm tra Build TypeScript & Vite
- Lệnh chạy: `npm --prefix FE run build`
- **Kết quả ghi nhận**:
  - Chạy `vue-tsc --noEmit` kiểm tra kiểu tĩnh: Thành công 100%, **0 lỗi TypeScript**.
  - Vite build bundle: Sinh bundle đầy đủ trong `FE/dist/` trong 4.51s, không có cảnh báo nghiêm trọng.

### 3.3. Các file test cần cập nhật và thêm mới khi triển khai

| STT | File Test | Loại | Mục đích & Nội dung cần cập nhật / bổ sung |
|-----|-----------|------|---------------------------------------------|
| 1 | `FE/src/router/index.spec.ts` | Cập nhật | Kiểm tra route `/v2/students`, `/v2/students/new`, `/v2/students/:id`; kiểm tra guard `guestOnly` chuyển hướng về `/v2`. |
| 2 | `FE/src/views/LoginView.spec.ts` | Cập nhật | Cập nhật assertion kiểm tra `safeRedirect` fallback về `/v2` thay vì `/students`. |
| 3 | `FE/src/views/AuthenticatedV2ShellView.spec.ts` | Cập nhật | Thêm test case xác thực hiển thị menu "Hồ sơ học sinh" với icon `pi pi-user` cho các vai trò `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`, ẩn với `STUDENT`, và kiểm tra active state. |
| 4 | `FE/src/views/StudentListView.spec.ts` | Thêm mới | Unit test cho view danh sách học sinh V2: render bảng dữ liệu, lọc theo trạng thái/lớp, phân trang, điều hướng drill-down chi tiết. |
| 5 | `FE/src/views/StudentDetailView.spec.ts` | Thêm mới | Unit test cho view chi tiết 4 tab: chuyển tab, nạp enrollment history, nạp attendance history, nạp transcript, bấm tính lại điểm. |
| 6 | `FE/src/components/StudentTable.spec.ts` | Cập nhật | Thêm assertion kiểm tra hiển thị cột `gender`, `status` badge, `currentClassCode`, click drill-down. |
| 7 | `FE/src/components/StudentSearchForm.spec.ts` | Cập nhật | Kiểm tra emit bộ lọc đa chiều gồm mã, tên, ngày sinh, trạng thái, lớp học. |
| 8 | `FE/src/components/StudentForm.spec.ts` | Cập nhật | Kiểm tra form tạo học sinh V3 (tự sinh username, mật khẩu an toàn, không lộ password hash, xử lý lỗi 409 Conflict). |
| 9 | `FE/src/services/studentApi.spec.ts` | Cập nhật | Bổ sung mock test cho API V3 `createStudentWithAccount` (`POST /api/v3/students`). |

---

## 4. BẢNG TỔNG HỢP CÁC TÍNH NĂNG ĐƯỢC KHẢO SÁT (FEATURES DISCOVERED)

| # | Phân nhóm | Tính năng | Mô tả | Đầu vào (Inputs) | Đầu ra (Outputs) | Hành vi lỗi (Error Behavior) | Nguồn khám phá |
|---|-----------|-----------|-------|------------------|------------------|------------------------------|----------------|
| 1 | Routing & Shell | V2 Shell Root & Nesting | Route `/v2` bọc bởi `AuthenticatedV2ShellView.vue` cung cấp layout và sidebar | URL path `/v2/*`, token hợp lệ | Giao diện Shell v2 với Header và Sidebar | Chưa login redirect `/login?redirect=...` | `FE/src/router/index.ts:53-149` |
| 2 | Routing & Shell | GuestOnly Auth Guard | Chặn người dùng đã đăng nhập truy cập lại trang guest (`/login`, `/register`) | Session token lưu trong `localStorage` | Chuyển hướng về `/v2` (hiện tại legacy về `/students`) | N/A | `FE/src/router/index.ts:159-162` |
| 3 | Routing & Shell | Login Safe Redirect | Chuyển hướng an toàn sau đăng nhập thành công | Query `redirect` trên URL | Điều hướng tới URL redirect hợp lệ hoặc fallback về `/v2` (hiện tại legacy về `/students`) | Query URL độc hại (bắt đầu bằng `//`) bị bỏ qua, fallback về mặc định | `FE/src/views/LoginView.vue:19-24` |
| 4 | Routing & Shell | V2 Sidebar Navigation | Menu bên trái theo vai trò người dùng trong Shell v2 | Session vai trò: `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`, `STUDENT` | Mảng `NavigationItem[]` hiển thị trên thanh sidebar | Nếu session rỗng thì chuyển về login | `FE/src/views/AuthenticatedV2ShellView.vue:12-49` |
| 5 | Routing & Shell | Student V2 Route Subtree | Các tuyến đường con của học sinh nằm trong Shell v2 | Tuyến đường `/v2/students`, `/v2/students/new`, `/v2/students/:id`, `/v2/students/:id/edit` | Màn hình tương ứng render trong `RouterView` của Shell v2 | Route không khớp fallback về neutral router view | `ORIGINAL_REQUEST.md:12-14` |
| 6 | Quản lý học sinh | Tra cứu & Lọc học sinh | Tìm kiếm danh sách học sinh theo nhiều tiêu chí đa chiều | `studentCode`, `studentName`, `dateOfBirth`, `status`, `classId`, `page`, `pageSize`, `sortField`, `sortDirection` | Danh sách phân trang `StudentPage` | 400 Bad Request nếu định dạng sai; 401 Unauthorized | `FE/src/services/studentApi.ts:12-19`, `BE StudentController.java:47-52` |
| 7 | Quản lý học sinh | Bảng học sinh v2 | Hiển thị thông tin học sinh kết hợp học vụ | Danh sách học sinh: mã, tên, ngày sinh, giới tính, trạng thái (`ACTIVE`/`INACTIVE`/`GRADUATED`), lớp hiện tại | Bảng DataTable PrimeVue với tag trạng thái và nút drill-down | Hiển thị Empty State nếu không có kết quả | `FE/src/components/StudentTable.vue`, `ORIGINAL_REQUEST.md:15-17` |
| 8 | Quản lý học sinh | Drill-down Chi tiết | Chuyển sang màn hình chi tiết học sinh khi click mã hoặc hàng | Click event kèm `studentId` | Điều hướng sang `/v2/students/:studentId` | 404 Not Found nếu học sinh không tồn tại | `ORIGINAL_REQUEST.md:16-17` |
| 9 | Quản lý học sinh | Xuất CSV Học sinh | Tải danh sách học sinh dạng tệp CSV UTF-8 | Token xác thực | File blob `students.csv` | 401 Unauthorized | `FE/src/services/studentApi.ts:21-23`, `BE StudentController.java:114-123` |
| 10 | Tạo học sinh V3 | Cấp tài khoản tự động | Tạo đồng thời hồ sơ học sinh, tài khoản người dùng và gán quyền `STUDENT` trong một transaction | `ReqCreateStudentV3DTO` (`studentCode`, `studentName`, `dateOfBirth`, `address`, `username?`, `password?`) | `ResStudentWithAccountDTO` (`studentId`, `studentCode`, `account`: `{userId, username, role}`) | 409 Conflict nếu trùng mã sinh viên hoặc trùng tên đăng nhập | `BE StudentV3Controller.java:28-38`, `StudentAccountService.java:43-70` |
| 11 | Tạo học sinh V3 | Tự sinh Username chuẩn | Backend tự động sinh username theo tên và mã học sinh nếu không cung cấp | `studentName`, `studentCode` | Tên đăng nhập chuẩn hóa ASCII không dấu | 400 Bad Request nếu tên sinh viên không hợp lệ | `BE StudentUsernameGenerator.java:16-27` |
| 12 | Tạo học sinh V3 | Bảo mật mật khẩu | Thiết lập mật khẩu mặc định hoặc an toàn mà không để lộ trên FE | Password string (6-15 ký tự ASCII) hoặc null | Password được mã hóa BCrypt lưu trong database; response KHÔNG chứa password | Password không đạt chuẩn độ dài/ký tự báo lỗi validation | `BE StudentAccountService.java:60-61`, `ResStudentWithAccountDTO.java:14` |
| 13 | Tạo học sinh V1 | Tạo hồ sơ đơn thuần | Tạo hồ sơ học sinh không kèm tài khoản để tương thích ngược | `StudentPayload` (`studentCode`, `studentName`, `dateOfBirth`, `address`) | `ResStudentDTO` | 400 Bad Request, 409 Conflict | `FE/src/services/studentApi.ts:27`, `BE StudentController.java:75-81` |
| 14 | Chi tiết học sinh | Tab 1 - Hồ sơ cá nhân | Xem thông tin nhân khẩu học và tài khoản đăng nhập của học sinh | `studentId` | Thông tin chi tiết cá nhân (ẩn trường `averageScore` cũ, thêm `gender`, `status`, `userId`, `username`) | 404 Not Found nếu ID không tồn tại | `ORIGINAL_REQUEST.md:23` |
| 15 | Chi tiết học sinh | Tab 2 - Xếp lớp & Lịch sử | Xem lớp học hiện tại và toàn bộ lịch sử chuyển lớp | `studentId` | `List<ResStudentEnrollmentHistoryDTO>` gồm lớp hiện tại, năm học, ngày vào lớp, danh sách chuyển lớp | 403 Forbidden nếu không có quyền; 404 Not Found | `FE/src/services/enrollmentApi.ts:37-39`, `BE EnrollmentController.java:101-109` |
| 16 | Chi tiết học sinh | Tab 3 - Chuyên cần | Xem thống kê số buổi có mặt, vắng phép, không phép và lịch sử chi tiết | `studentId`, `academicYearId?`, `semesterId?`, `from?`, `to?`, `page`, `size` | `StudentAttendanceHistoryResponse` gồm thẻ tổng hợp (`summary`) và danh sách bản ghi | 403 Forbidden đối với học sinh khác; 404 Not Found | `FE/src/services/attendanceApi.ts:81-92`, `BE AttendanceHistoryController.java:40-50` |
| 17 | Chi tiết học sinh | Tab 4 - Bảng điểm học sinh | Xem bảng điểm học kỳ và cả năm kèm trạng thái tính toán | `studentId`, `semesterId`, `academicYearId` | `ResStudentTermTranscriptDTO`, `ResStudentAnnualTranscriptDTO` | 403 Forbidden; 404 Not Found | `FE/src/services/transcriptApi.ts:50-70`, `BE TranscriptQueryController.java:78-100` |
| 18 | Chi tiết học sinh | Nút yêu cầu tính lại điểm | Gửi tác vụ nền tính toán lại toàn bộ bảng điểm cho học sinh | `studentId` (hoặc `studentCode`), `academicYearId` | Trả về `202 Accepted` với `ResCalculationTaskDTO` | 403 Forbidden nếu vai trò không phải `ADMIN`/`ACADEMIC_OFFICE` | `FE/src/services/calculationTaskApi.ts:64-75`, `BE CalculationTaskController.java:83-105` |
| 19 | Vòng đời & Xóa | Chính sách xóa an toàn (R5) | Cảnh báo ràng buộc toàn vẹn dữ liệu, chuyển trạng thái hồ sơ thay vì xóa cứng | Thao tác xóa hoặc ngừng hoạt động kèm `studentId` | Cập nhật `status = INACTIVE/GRADUATED` hoặc hộp thoại cảnh báo ràng buộc | Chặn xóa cứng khi có bản ghi enrollment/attendance/transcript | `ORIGINAL_REQUEST.md:28-29` |

---

## 5. BẢNG CÁC TRƯỜNG HỢP BIÊN & NGOẠI LỆ (EDGE CASES)

| # | Tính năng | Đầu vào (Input) | Hành vi quan sát được / Đặc tả xử lý |
|---|-----------|-----------------|--------------------------------------|
| 1 | Login Safe Redirect | Query `redirect` chứa link độc hại: `//attacker.com` hoặc `https://evil.com` | Hàm `safeRedirect()` phát hiện chuỗi bắt đầu bằng `//` hoặc không bắt đầu bằng `/`, tự động hủy và trả về fallback `/v2`. |
| 2 | Login Safe Redirect | Đăng nhập thành công khi không có query `redirect` | Hàm `safeRedirect()` trả về giá trị fallback `/v2` (hiện tại trong code cũ đang trả về `/students`). |
| 3 | GuestOnly Guard | Người dùng đã đăng nhập truy cập trực tiếp URL `/login` hoặc `/register` | Guard `router.beforeEach` phát hiện session hợp lệ và điều hướng về `/v2` (hiện tại trong code cũ đang về `/students`). |
| 4 | Sidebar Active State | Người dùng đang ở route con `/v2/students/123` hoặc `/v2/students/new` | Menu "Hồ sơ học sinh" (`/v2/students`) vẫn giữ trạng thái active (`router-link-active`) chính xác. |
| 5 | Role-based Sidebar | Đăng nhập với tài khoản vai trò `STUDENT` | Menu "Hồ sơ học sinh" (`/v2/students`) bị ẩn hoàn toàn khỏi thanh sidebar; nếu cố tình gõ URL thì bị guard/view chặn 403. |
| 6 | Tạo sinh viên V3 | Trùng `studentCode` đã có trong hệ thống (vd: `STU0000001`) | Backend ném `AppException(HttpStatus.CONFLICT, "Mã sinh viên đã tồn tại")` (HTTP 409); FE bắt lỗi và hiển thị thông báo đỏ ngay dưới trường Student code. |
| 7 | Tạo sinh viên V3 | Trùng `username` đã có trong hệ thống (vd: `admin`) | Backend ném `AppException(HttpStatus.CONFLICT, "Tên đăng nhập đã tồn tại")` (HTTP 409); FE bắt lỗi và hiển thị thông báo đỏ dưới trường Username. |
| 8 | Tạo sinh viên V3 | Không nhập `username` và không nhập `password` | Backend tự động sinh username qua `StudentUsernameGenerator` và gán password mặc định `12345678`. FE hiển thị thông báo tài khoản đã tạo thành công mà không để lộ password. |
| 9 | Tạo sinh viên V3 | Tên sinh viên dài có nhiều từ (vd: "Nguyen Thi Hoang Yen Phuong") | `StudentUsernameGenerator` tự động rút gọn chữ cái đầu ("nthyphuong" + suffix 7 số) để bảo đảm username không vượt quá 20 ký tự. |
| 10 | Danh sách học sinh | Học sinh chưa được xếp vào lớp nào | Cột "Lớp hiện tại" hiển thị "Chưa xếp lớp" (hoặc tag xám) thay vì hiển thị giá trị null/rỗng hoặc gây lỗi vỡ UI. |
| 11 | Chi tiết học sinh Tab 2 | Học sinh mới tạo chưa có bất kỳ bản ghi xếp lớp nào | Tab 2 hiển thị Empty State: "Chưa có lịch sử enrollment - Học sinh chưa có bản ghi xếp lớp nào" (giống `StudentEnrollmentHistoryDialog.vue`). |
| 12 | Chi tiết học sinh Tab 3 | Học sinh chưa có dữ liệu điểm danh nào trong học kỳ được chọn | Thẻ thống kê hiển thị 0; bảng DataTable hiển thị Empty State: "Chưa có lịch sử chuyên cần - Không có bản ghi phù hợp với bộ lọc hiện tại". |
| 13 | Chi tiết học sinh Tab 4 | Bảng điểm học kỳ chưa được tính toán (`PENDING` hoặc `NOT_CALCULATED`) | Cột điểm hiển thị ô trống hoặc dấu gạch ngang; thẻ trạng thái hiển thị cảnh báo `PENDING`; nút "Yêu cầu tính lại điểm" cho phép kích hoạt tiến trình tính toán. |
| 14 | Chi tiết học sinh Tab 4 | Giáo viên (`TEACHER`) bấm xem bảng điểm | Nút "Yêu cầu tính lại điểm" bị ẩn hoặc disabled vì backend chỉ cho phép `ADMIN` và `ACADEMIC_OFFICE` gọi endpoint `/recalculate`. |
| 15 | Xóa học sinh (R5) | Bấm xóa học sinh đã có dữ liệu xếp lớp và điểm thi | Hiển thị thông báo ràng buộc dữ liệu: Không cho phép xóa cứng để bảo toàn dữ liệu lịch sử; gợi ý chuyển trạng thái sang `INACTIVE` hoặc `GRADUATED`. |
