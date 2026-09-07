# Handoff Report: Backend Architecture & API Spec Mining

## 1. Observation
- **Student v1 Endpoints**:
  - `StudentController.java` (`BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/controller/StudentController.java`): Class-level `@RequestMapping("/api/v1/students")`, `@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')")`.
  - Các method: `fetchStudents` (line 47), `getStudent` (line 56), `getStudentByCode` (line 66), `createStudent` (line 75), `updateStudent` (line 85), `deleteStudent` (line 96), `generateStudentCode` (line 107), `exportStudents` (line 115).
- **Student v3 Account Provisioning Endpoint**:
  - `StudentV3Controller.java` (`BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/controller/StudentV3Controller.java`): `@RequestMapping("/api/v3/students")`, endpoint `POST /api/v3/students` có `@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")` (lines 28-38).
  - `StudentAccountService.java` (`BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/service/StudentAccountService.java`): Phương thức `createStudentWithAccount` có `@Transactional` (line 42). Tạo User (gán role STUDENT), Student, StudentInfo trong cùng một transaction. Sinh username qua `StudentUsernameGenerator` khi username null (lines 50-52); mã hóa mật khẩu mặc định `"12345678"` qua `PasswordEncoder` khi password null (lines 60-61). Trả về `ResStudentWithAccountDTO` không chứa mật khẩu.
- **Học vụ v2 Endpoints**:
  - `EnrollmentController.java` (`BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/enrollment/controller/EnrollmentController.java`): `GET /api/v2/students/{studentId}/enrollments` (line 101), `GET /api/v2/students/by-code/{studentCode}/enrollments` (line 111). Phân quyền `ADMIN, ACADEMIC_OFFICE, TEACHER`.
  - `AttendanceHistoryController.java` (`BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/controller/AttendanceHistoryController.java`): `GET /api/v2/attendance/students/{studentId}/history` có `@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')")` (lines 40-50); `GET /api/v2/attendance/students/me/history` có `@PreAuthorize("hasRole('STUDENT')")` (lines 30-38). (Vừa được hoàn thành trong Dev Note 066 ngày 2026-09-04).
  - `TranscriptQueryController.java` (`BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/controller/TranscriptQueryController.java`): `GET /api/v2/transcripts/students/{studentId}/semesters/{semesterId}` (line 78), `GET /api/v2/transcripts/students/{studentId}/academic-years/{academicYearId}` (line 90), `GET .../status` (lines 102, 114).
  - `CalculationTaskController.java` (`BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/controller/CalculationTaskController.java`): `POST /api/v2/students/{studentCode}/transcripts/recalculate` (line 83), `POST /api/v2/students/{studentId}/transcripts/recalculate` (line 95). Phân quyền `ADMIN, ACADEMIC_OFFICE`.
- **Database Schema & Entity Mapping**:
  - `Student.java` (`BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/entity/Student.java`): Có các trường `id`, `studentName` (35), `studentCode` (10, unique), `userId` (unique FK -> app_user), `status` (Enum `StudentStatus` [ACTIVE, INACTIVE, GRADUATED], default ACTIVE), `studentInfo` (OneToOne).
  - `StudentInfo.java` (`BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/entity/StudentInfo.java`): Có các trường `id`, `student` (OneToOne), `address`, `averageScore`, `dateOfBirth`. **Không có trường `gender`**.
  - `V4__create_academic_structure_enrollment_and_audit.sql`: Thêm `ALTER TABLE student ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'`.
  - `V8__link_student_to_app_user.sql`: Thêm `ALTER TABLE student ADD COLUMN user_id BIGINT NULL; ADD CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)`.
  - Khóa ngoại RESTRICT: `student_year_enrollment`, `student_score`, `student_annual_transcript`, `student_term_transcript`, `score_change_request`, `calculation_task` đều có FK tới `student(student_id)`.
  - `StudentService.java` line 77-80: `deleteStudent` thực hiện xóa cứng `studentRepository.delete(support.find(studentId))`.
- **Quality Tooling & Environment**:
  - `build.gradle.kts`: Spring Boot 4.0.7, Java 21, Checkstyle 10.26.1, PMD 7.16.0, JaCoCo 0.8.13.
  - Gradle binary: `/home/duyptk/.gradle/wrapper/dists/gradle-9.5.1-bin/iq79hdu3mqx29lgffhp8bfmx/gradle-9.5.1/bin/gradle`.
  - Global cache `/home/duyptk/.gradle/caches/journal-1/journal-1.lock` đang bị chiếm dụng bởi process host PID 185267 (`Timeout waiting to lock journal cache`). Do đó chạy build/test cần `--offline` và GRADLE_USER_HOME riêng.

## 2. Logic Chain
1. **Từ các quan sát trên `StudentV3Controller.java` và `StudentAccountService.java`**:
   - Ta thấy API `POST /api/v3/students` đã được implement đầy đủ logic nghiệp vụ theo đặc tả CR-STUDENT-001 (Plan 043): xử lý transaction nguyên tử, cấp tài khoản User, mã hóa mật khẩu mặc định, sinh username chuẩn hóa và fallback initials khi tên dài > 20 ký tự.
   - Do đó, kết luận: API v3 đã hoàn thiện trong BE và sẵn sàng để Frontend tích hợp.
2. **Từ các quan sát trên `EnrollmentController`, `AttendanceHistoryController`, `TranscriptQueryController`, `CalculationTaskController`**:
   - 4 tab dữ liệu học vụ của màn hình chi tiết học sinh v2 (Hồ sơ, Xếp lớp, Chuyên cần, Bảng điểm) đều có endpoint backend tương ứng hỗ trợ cả `studentId` và `studentCode`. Đặc biệt endpoint tra cứu chuyên cần theo `studentId` vừa được hoàn tất ngày hôm nay (Dev Note 066).
3. **Từ các quan sát trên `StudentInfo.java`, `V1__create_legacy_schema.sql` và `DataStructure.md`**:
   - Trường `gender` chưa được thêm vào bảng `student_info` hay entity `StudentInfo`. `average_score` vẫn còn trong DB và entity nhưng đã deprecated theo kiến trúc v2.
4. **Từ các quan sát trên `StudentService.deleteStudent` và các migration V4, V11, V12, V13**:
   - Khi xóa học sinh có dữ liệu học vụ, thao tác xóa cứng sẽ vi phạm ràng buộc khóa ngoại RESTRICT và gây lỗi 500. Vì vậy, yêu cầu R5 về chính sách xóa an toàn hoặc chuyển trạng thái `status = INACTIVE` là hoàn toàn chính xác và cấp thiết.
5. **Từ các quan sát trên `StudentController.java`**:
   - `@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')")` ở class-level không có ghi đè ở `createStudent`, `updateStudent`, `deleteStudent`, dẫn đến vai trò `TEACHER` về mặt kỹ thuật vẫn được phép gọi các API sửa/xóa v1 nếu không được FE kiểm soát.

## 3. Caveats
- Khảo sát mã nguồn ở chế độ Read-only, không thực hiện thay đổi mã nguồn backend hay migration database.
- Không thể chạy full test suite `./gradlew test` trực tiếp trong phiên do lock cache toàn cục của Gradle trên máy host (`PID 185267`); thông tin kiểm thử được đối chiếu từ test source code thực tế và evidence trong Dev Notes (043, 066).
- Về trường `gender`: Chưa xác nhận liệu có yêu cầu tạo migration DB mới để thêm cột `gender` vào `student_info` trong scope hiện tại hay không, vì CR-STUDENT-001 trước đó quy định giữ nguyên schema.

## 4. Conclusion
- Backend của hệ thống đã sẵn sàng phần lớn các API học vụ và tài khoản cần thiết cho việc nâng cấp phân hệ học sinh v2 theo `ORIGINAL_REQUEST.md`.
- `POST /api/v3/students` đã sẵn sàng và hoạt động chính xác theo chuẩn bảo mật.
- Cần lưu ý việc hiển thị trường `status` và `gender` trên FE: `status` đã có trong entity `Student` nhưng chưa có trong `ResStudentDTO`; `gender` chưa có trong BE `StudentInfo`.
- Báo cáo chi tiết đã được lập tại `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/spec_miner_be_0/report.md`.

## 5. Verification Method
- Kiểm tra mã nguồn các controller:
  - `StudentV3Controller.java`: `view_file` tại `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/controller/StudentV3Controller.java`.
  - `StudentAccountService.java`: `view_file` tại `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/service/StudentAccountService.java`.
  - `AttendanceHistoryController.java`: `view_file` tại `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/controller/AttendanceHistoryController.java`.
- Kiểm tra báo cáo đầy đủ:
  - Xem nội dung file `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/spec_miner_be_0/report.md`.

