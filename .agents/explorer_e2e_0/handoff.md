# E2E Integration Explorer Handoff Report

## 1. Observation
1. **Login Redirect Hiện tại**:
   - Trong `FE/src/views/LoginView.vue#L17`: `const successRedirect = ref('/students')`.
   - Trong `FE/src/views/LoginView.vue#L23`: Hàm `safeRedirect()` fallback về `'/students'`.
   - Trong `FE/src/router/index.ts#L159-L161`:
     ```ts
     if (to.meta.guestOnly && authenticated) {
       return { name: 'students' }
     }
     ```
   - Trong `FE/src/views/LoginView.spec.ts#L57, L80`: Test case assert chuyển hướng về `/students/new`.

2. **Cấu trúc Tuyến đường & Shell v2**:
   - Tuyến đường `/students`, `/students/new`, `/students/:studentId/edit` đang khai báo ở root level độc lập (`FE/src/router/index.ts#L35-L51`).
   - `FE/src/views/AuthenticatedV2ShellView.vue#L13-L23`: Sidebar navigation list hiện chưa có mục menu "Hồ sơ học sinh" (`/v2/students`). Menu "Bảng điểm" (`/v2/transcripts`) hiển thị cho `STUDENT`, trong khi "Bảng điểm theo lớp" (`/v2/class-transcripts`) hiển thị cho `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER` (`AuthenticatedV2ShellView.vue#L27-L43`).

3. **Backend API Endpoints (Student v1, v2, v3)**:
   - **v1**: `BE/BaiTap-RS/.../StudentController.java#L32-L33`: `@RequestMapping("/api/v1/students")`, `@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')")`. Cung cấp `GET /api/v1/students`, `GET /api/v1/students/{id}`, `GET /api/v1/students/code/{code}`, `POST`, `PUT`, `DELETE /api/v1/students/{id}` (xóa cứng tại `StudentService.java#L77-L80`), `POST /api/v1/students/code`, `GET /api/v1/students/export`.
   - **v2**:
     - Phân lớp: `GET /api/v2/students/{studentId}/enrollments` và `GET /api/v2/students/by-code/{studentCode}/enrollments` tại `EnrollmentController.java#L101-L119`.
     - Chuyên cần: `GET /api/v2/attendance/students/{studentId}/history` tại `AttendanceHistoryController.java#L40-L50` (`hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')`) và `GET /api/v2/attendance/students/me/history` (`hasRole('STUDENT')`).
     - Bảng điểm: `GET /api/v2/transcripts/students/{studentId}/semesters/{semesterId}` và `academic-years/{academicYearId}` tại `TranscriptQueryController.java#L78-L100` (`TranscriptAccessGuard`).
     - Tính lại bảng điểm: `POST /api/v2/students/{studentCode}/transcripts/recalculate?academicYearId=...` tại `CalculationTaskController.java#L83-L93` (`hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')`).
   - **v3**: `POST /api/v3/students` tại `StudentV3Controller.java#L28-L38`: `@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")`. `StudentAccountService.createStudentWithAccount` chạy `@Transactional`, tự sinh username (tên không dấu + 7 số cuối mã HS, fallback <= 20 ký tự), mật khẩu mặc định `12345678` mã hóa BCrypt, gán role `STUDENT`, bắt lỗi `409 Conflict` khi duplicate mã HS hoặc username.

4. **Trạng thái Tests**:
   - Frontend: Lệnh `npm --prefix FE run test` trả về `75 passed (75)`, `324 passed (324)`.
   - Backend: Gradle build với `./gradlew test` (JUnit 5 + MockMvc), Checkstyle và PMD ruleset cấu hình nghiêm ngặt (`isIgnoreFailures = false`).

---

## 2. Logic Chain
1. **Từ Observation 1 đến Chuyển đổi Điều hướng**:
   - Vì `LoginView.vue` và `router/index.ts` đang hardcode redirect về `/students`, người dùng sau khi đăng nhập thành công sẽ bị rơi vào giao diện legacy v1.
   - Để thỏa mãn yêu cầu bổ sung của người dùng (`ORIGINAL_REQUEST.md#L55`), cả 3 điểm (`LoginView.vue`, `router/index.ts`, `LoginView.spec.ts`) phải đồng thời đổi target fallback từ `/students` sang `/v2`.

2. **Từ Observation 2 đến R1 (Shell v2 & Sidebar)**:
   - Vì các route quản lý học sinh đang nằm ngoài `/v2`, chúng không được bao bọc bởi layout và navigation của `AuthenticatedV2ShellView`.
   - Cần cấu hình các route con `/v2/students`, `/v2/students/new`, `/v2/students/:id`, `/v2/students/:id/edit` bên dưới route cha `/v2`, đồng thời thêm item "Hồ sơ học sinh" (`pi pi-user`) vào navigation array trong `AuthenticatedV2ShellView.vue` với điều kiện `isNonStudent`. Khi role là `STUDENT`, mục này bị ẩn để tránh lỗi 403 Forbidden.

3. **Từ Observation 3 đến R2 & R3 (Danh sách & Cấp tài khoản V3)**:
   - Backend đã có sẵn `POST /api/v3/students` với logic nghiệp vụ hoàn chỉnh, nhưng FE `StudentFormView.vue` chỉ đang gọi `POST /api/v1/students`.
   - FE cần bổ sung toggle "Cấp tài khoản đăng nhập cho học sinh" gọi API v3 khi người dùng là `ADMIN` hoặc `ACADEMIC_OFFICE`, tự động xử lý kết quả trả về `ResStudentWithAccountDTO` mà không để lộ mật khẩu, và bắt lỗi 409 Conflict khi trùng lặp.
   - Bảng danh sách cần bổ sung cột trạng thái `status` (ACTIVE/INACTIVE/GRADUATED) và mã `studentCode`.

4. **Từ Observation 3 đến R4 (Màn hình Chi tiết 4 Tabs)**:
   - Cả 4 phân hệ backend (`StudentController`, `EnrollmentController`, `AttendanceHistoryController`, `TranscriptQueryController`) đều đã có sẵn API hỗ trợ tra cứu theo `studentId`/`studentCode`.
   - Do đó, việc xây dựng `StudentDetailView.vue` gồm 4 tabs (Tab 1: Hồ sơ & Account; Tab 2: Lớp học & Lịch sử chuyển lớp; Tab 3: Báo cáo chuyên cần; Tab 4: Bảng điểm & Tính lại điểm) là hoàn toàn khả thi và có đầy đủ API contract hậu thuẫn.

5. **Từ Observation 3 đến R5 (Xóa an toàn & Ràng buộc khóa ngoại)**:
   - Vì `StudentService.java#L77-L80` thực hiện hard-delete thẳng vào repository, việc xóa học sinh đã có dữ liệu học vụ (xếp lớp, điểm danh, điểm số) sẽ vi phạm khóa ngoại.
   - Do đó, chính sách an toàn bắt buộc phải áp dụng: FE chặn xác nhận xóa cứng đối với học sinh đã có dữ liệu học vụ, hướng dẫn người dùng chuyển trạng thái sang `INACTIVE` hoặc `GRADUATED`.

---

## 3. Caveats
- **Trường `status` và `gender` trong `ResStudentDTO`**: Hiện tại entity `Student` đã có cột `status` và bảng `student_info` có thiết kế `gender`, tuy nhiên `ResStudentDTO` trả về từ `GET /api/v1/students` chưa expose trực tiếp hai trường này ra ngoài. Đội ngũ backend cần mở rộng mapping DTO để FE hiển thị trọn vẹn mà không cần gọi API phụ.
- **Quyền hạn `TEACHER` trên `StudentController` v1**: Class-level của `StudentController` gắn `@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')")` và không override trên `POST`, `PUT`, `DELETE`. Mặc dù FE có thể ẩn các nút này với giáo viên, Backend vẫn cần siết chặt annotation ở cấp method để ngăn chặn bypass API.
- **Chưa có công cụ E2E Browser Test tự động (Playwright/Cypress)**: Hiện dự án chỉ chạy Unit/Component tests qua Vitest và Spring Boot Integration Tests. Bộ kiểm thử E2E 4 Tiers được thiết kế sẵn sàng để ánh xạ sang Vitest view-integration tests hoặc cấu hình Playwright khi môi trường sẵn sàng.

---

## 4. Conclusion
1. **Khảo sát hoàn tất 100%**: Đã làm rõ toàn bộ đường đi dữ liệu, API endpoints, entity schema, permission guards từ Frontend sang Backend.
2. **Kế hoạch chuyển đổi rõ ràng**:
   - Follow-up login redirect: Chuyển toàn bộ fallback về `/v2`.
   - R1: Chuyển route sang `/v2/students` và tích hợp Sidebar v2.
   - R2: Nâng cấp bảng tra cứu học sinh với mã STU và Tag trạng thái.
   - R3: Tích hợp form cấp tài khoản tự động v3 (`POST /api/v3/students`).
   - R4: Xây dựng giao diện chi tiết học sinh 4 Tabs Workspace kết nối Enrollment, Attendance, Transcript.
   - R5: Thay thế xóa cứng bằng cảnh báo ràng buộc và chuyển đổi trạng thái INACTIVE/GRADUATED.
3. **Bộ kiểm thử 4 Tiers & Ma trận phân quyền**: Đã được thiết kế chi tiết với đầy đủ test cases, boundary cases, tổ hợp nghiệp vụ chéo và kịch bản thực tế trong `report.md`.

---

## 5. Verification Method
Để độc lập xác minh các phát hiện và đánh giá trong báo cáo:

1. **Kiểm tra hiện trạng kiểm thử Frontend**:
   ```bash
   npm --prefix FE run test -- --run
   ```
   *Kỳ vọng*: 75 test files và 324 tests vượt qua (PASS).

2. **Kiểm tra kiểm thử Backend**:
   ```bash
   cd BE/BaiTap-RS
   ./gradlew test
   ```
   *Kỳ vọng*: Toàn bộ unit tests và integration tests (bao gồm `StudentServiceAccountTest`, `StudentAuthorizationIntegrationTest`) đều PASS.

3. **Kiểm tra các tệp tin mã nguồn chủ chốt**:
   - `FE/src/views/LoginView.vue`: Xác minh dòng 17 và 23 đang trỏ về `/students`.
   - `FE/src/router/index.ts`: Xác minh dòng 160 đang redirect về `students`.
   - `FE/src/views/AuthenticatedV2ShellView.vue`: Xác minh dòng 13-48 chưa có mục "Hồ sơ học sinh".
   - `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/controller/StudentV3Controller.java`: Xác minh endpoint `POST /api/v3/students` đã sẵn sàng.
   - `document/application-doc/v2/change-request/CR-STUDENT-001-student-identifier-and-account-provisioning.md`: Xác minh quy chuẩn sinh username và mã học sinh.

4. **Điều kiện vô hiệu hóa (Invalidation conditions)**:
   - Nếu Backend thay đổi cấu trúc `ReqCreateStudentV3DTO` hoặc đường dẫn `/api/v3/students`.
   - Nếu thiết kế Shell v2 thay đổi hoàn toàn hệ thống sidebar hoặc cấu trúc routing.
