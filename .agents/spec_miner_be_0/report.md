# Báo Cáo Khảo Sát Mã Nguồn Backend (BE Specification Mining Report)

**Dự án**: Java-CoBan-RS  
**Thư mục làm việc**: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/spec_miner_be_0`  
**Ngày thực hiện**: 2026-09-04  
**Người thực hiện**: BE Spec Miner  
**Tài liệu tham chiếu chính**:
- `ORIGINAL_REQUEST.md`
- `DISPATCH.md`
- `document/application-doc/v2/change-request/CR-STUDENT-001-student-identifier-and-account-provisioning.md`
- `document/application-doc/v2/ActualPermissionMatrix.md`
- `document/application-doc/v2/data-model/03-StudentsAndEnrollment.md`
- `document/dev-note/be/student/043-student-create-with-account-2026-08-26.md`
- `document/dev-note/be/attendance/066-student-attendance-history-by-id-and-transcript-integration-2026-09-04.md`
- Mã nguồn BE tại `BE/BaiTap-RS/src`

---

## I. Tổng Quan Kiến Trúc Backend & Phân Hệ Quản Lý Học Sinh

Hệ thống Backend được phát triển trên nền tảng **Spring Boot 4.0.7**, **Java 21**, quản lý build bằng **Gradle 9.5.1**, cơ sở dữ liệu **MySQL** với migration versioning qua **Flyway** (V1 đến V19).

Phân hệ quản lý học sinh và các phân hệ học vụ tích hợp bao gồm 3 thế hệ API:
1. **Student v1 (`/api/v1/students`)**: Các API CRUD quản lý hồ sơ học sinh cơ bản (legacy), xuất CSV, sinh mã học sinh tự động.
2. **Học vụ v2 (`/api/v2/*`)**: Các API học vụ chuyên sâu tích hợp liên phân hệ:
   - Phân lớp & Chuyển lớp (`/api/v2/students/{id}/enrollments`, `/api/v2/students/by-code/{studentCode}/enrollments`).
   - Chuyên cần & Điểm danh (`/api/v2/attendance/students/{id}/history`, `/api/v2/attendance/students/me/history`).
   - Bảng điểm & Học bạ (`/api/v2/transcripts/students/{id}/...`, `/api/v2/students/{id}/transcripts/recalculate`).
3. **Student v3 (`/api/v3/students`)**: API tạo mới học sinh tích hợp cấp tài khoản đăng nhập (`User` + role `STUDENT` + `Student` + `StudentInfo`) trong 1 transaction duy nhất tuân thủ đặc tả **CR-STUDENT-001** (Plan 043).

---

## Features Discovered

| #   | Category      | Feature                                                   | Description                                                                                     | Inputs                                                                                                                                        | Outputs                                                                                                         | Error Behavior                                                                                                        | Discovered Via                                                                      |
| --- | ------------- | --------------------------------------------------------- | ----------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| 1   | Student v1    | Phân trang & tra cứu danh sách học sinh                   | Lấy danh sách học sinh có phân trang, tìm kiếm theo tên, mã, ngày sinh                          | Query params: `page`, `size`, `studentCode`, `studentName`, `birthday`, `sortField`, `sortDirection`                                          | `200 OK`: `ResStudentPageDTO` (`content: List<ResStudentDTO>`, `page`, `size`, `totalElements`, `totalPages`)   | `400` nếu page/size < 0; `401` nếu chưa đăng nhập; `403` nếu role là `STUDENT`                                        | `StudentController.java#L45-L52`, `StudentSpecifications.java#L14-L36`              |
| 2   | Student v1    | Xem chi tiết học sinh theo ID                             | Lấy thông tin chi tiết một học sinh theo ID kỹ thuật (`studentId`)                              | Path variable: `studentId` (`Long > 0`)                                                                                                       | `200 OK`: `ResStudentDTO` (`studentId`, `studentCode`, `studentName`, `dateOfBirth`, `address`, `averageScore`) | `404` "Không tìm thấy sinh viên"; `400` nếu ID <= 0; `401`, `403`                                                     | `StudentController.java#L54-L62`, `StudentServiceSupport.java#L34-L37`              |
| 3   | Student v1    | Tra cứu học sinh theo mã (CR-STUDENT-001)                 | Lấy thông tin học sinh theo mã định danh nghiệp vụ `studentCode`                                | Path variable: `studentCode` (`String`)                                                                                                       | `200 OK`: `ResStudentDTO`                                                                                       | `404` "Không tìm thấy sinh viên"; `401`, `403`                                                                        | `StudentController.java#L64-L71`, `StudentLookupService.java#L75-L78`               |
| 4   | Student v1    | Tạo hồ sơ học sinh đơn thuần (Legacy)                     | Tạo học sinh mới độc lập không kèm tài khoản User                                               | Body JSON `ReqCreateStudentDTO`: `studentCode` (STU+7 số), `studentName` (max 35), `dateOfBirth`, `address`, `averageScore`                   | `201 Created`: `ResStudentDTO`                                                                                  | `409` "Mã sinh viên đã tồn tại"; `400` validation error; `401`, `403`                                                 | `StudentController.java#L73-L81`, `StudentService.java#L62-L68`                     |
| 5   | Student v1    | Cập nhật thông tin học sinh                               | Cập nhật các trường mutable: tên, ngày sinh, địa chỉ, điểm trung bình; giữ nguyên `studentCode` | Path variable `studentId`, Body JSON `ReqUpdateStudentDTO`                                                                                    | `200 OK`: `ResStudentDTO`                                                                                       | `404` "Không tìm thấy sinh viên"; `400` validation error; `401`, `403`                                                | `StudentController.java#L83-L92`, `StudentServiceSupport.java#L57-L69`              |
| 6   | Student v1    | Xóa học sinh (Hard delete)                                | Xóa cứng bản ghi trong bảng `student` và cascade xóa `student_info`                             | Path variable: `studentId`                                                                                                                    | `204 No Content`                                                                                                | `404` nếu không tìm thấy; `500 DataIntegrityViolationException` nếu đã có dữ liệu phân lớp/điểm/điểm danh             | `StudentController.java#L94-L103`, `StudentService.java#L77-L80`                    |
| 7   | Student v1    | Tự động sinh mã học sinh                                  | Sinh mã học sinh khả dụng có tiền tố `STU` và 7 chữ số ngẫu nhiên                               | Không có input                                                                                                                                | `200 OK`: `ResStudentCodeDTO` (`studentCode: "STUxxxxxxx"`)                                                     | `409` "Không thể tạo mã sinh viên duy nhất" nếu thử 5 batch mà trùng hết; `401`, `403`                                | `StudentController.java#L105-L112`, `StudentCodeGenerator.java`                     |
| 8   | Student v1    | Xuất danh sách học sinh ra CSV                            | Xuất toàn bộ danh sách học sinh ra định dạng file CSV                                           | Không có input                                                                                                                                | `200 OK`: `byte[]` (`text/csv; charset=UTF-8`, filename `students.csv`)                                         | `401`, `403`                                                                                                          | `StudentController.java#L114-L123`, `StudentCsvExportService.java`                  |
| 9   | Student v3    | Tạo học sinh kèm cấp tài khoản đăng nhập (CR-STUDENT-001) | Tạo đồng thời User, Role STUDENT, Student, StudentInfo trong 1 Transaction                      | Body JSON `ReqCreateStudentV3DTO`: `studentCode`, `studentName`, `dateOfBirth`, `address`, `averageScore`, `username` (opt), `password` (opt) | `201 Created`: `ResStudentWithAccountDTO` (thông tin HS + `account: { userId, username, role: "STUDENT" }`)     | `409` "Mã sinh viên đã tồn tại" hoặc "Tên đăng nhập đã tồn tại"; `500` nếu thiếu role; `400` validation; `401`, `403` | `StudentV3Controller.java#L28-L38`, `StudentAccountService.java#L42-L70`            |
| 10  | Enrollment v2 | Xem lịch sử phân lớp & chuyển lớp theo ID                 | Xem thông tin lớp học hiện tại và toàn bộ lịch sử chuyển lớp theo `studentId`                   | Path variable: `studentId`                                                                                                                    | `200 OK`: `List<ResStudentEnrollmentHistoryDTO>` (enrollment + list transfers)                                  | `400` nếu ID <= 0; `401`, `403` (chỉ ADMIN, ACADEMIC_OFFICE, TEACHER)                                                 | `EnrollmentController.java#L101-L109`, `EnrollmentQueryService.java`                |
| 11  | Enrollment v2 | Xem lịch sử phân lớp & chuyển lớp theo mã                 | Xem thông tin lớp và lịch sử chuyển lớp theo `studentCode`                                      | Path variable: `studentCode`                                                                                                                  | `200 OK`: `List<ResStudentEnrollmentHistoryDTO>`                                                                | `404` nếu không tìm thấy HS; `401`, `403`                                                                             | `EnrollmentController.java#L111-L119`                                               |
| 12  | Attendance v2 | Xem lịch sử chuyên cần theo ID học sinh (Dev Note 066)    | Tra cứu thống kê chuyên cần, tổng số buổi hợp lệ, có mặt, vắng có/không phép của học sinh       | Path variable: `studentId`, Query params: `academicYearId`, `semesterId`, `from`, `to`, `page`, `size`                                        | `200 OK`: `ResStudentAttendanceHistoryDTO` (`items`, `summary`, phân trang)                                     | `404` nếu studentId không tồn tại; `400` date range sai; `401`, `403` (ADMIN, ACADEMIC_OFFICE, TEACHER)               | `AttendanceHistoryController.java#L40-L50`, `AttendanceHistoryService.java#L42-L58` |
| 13  | Attendance v2 | Học sinh tự xem lịch sử chuyên cần                        | Học sinh đăng nhập tự xem chuyên cần của chính mình                                             | Query params: `ReqAttendanceHistoryQuery`                                                                                                     | `200 OK`: `ResStudentAttendanceHistoryDTO`                                                                      | `401`, `403` (chỉ role `STUDENT`)                                                                                     | `AttendanceHistoryController.java#L30-L38`                                          |
| 14  | Transcript v2 | Tra cứu bảng điểm học kỳ theo ID học sinh                 | Xem chi tiết điểm các môn học trong một học kỳ của học sinh                                     | Path variables: `studentId`, `semesterId`                                                                                                     | `200 OK`: `ResStudentTermTranscriptDTO`                                                                         | `404` không tìm thấy bảng điểm; `401`, `403` (ADMIN, ACADEMIC_OFFICE; TEACHER phải có phân công lớp/môn)              | `TranscriptQueryController.java#L78-L88`, `TranscriptAccessGuard.java`              |
| 15  | Transcript v2 | Tra cứu bảng điểm cả năm theo ID học sinh                 | Xem kết quả tổng kết cả năm học của học sinh                                                    | Path variables: `studentId`, `academicYearId`                                                                                                 | `200 OK`: `ResStudentAnnualTranscriptDTO`                                                                       | `404`; `401`, `403`                                                                                                   | `TranscriptQueryController.java#L90-L100`                                           |
| 16  | Transcript v2 | Xem trạng thái tính toán bảng điểm                        | Kiểm tra tiến trình tính bảng điểm (PENDING/CALCULATED/STALE/FAILED)                            | Path variables: `studentId`, `semesterId` / `academicYearId`                                                                                  | `200 OK`: `ResTranscriptCalculationStatusDTO`                                                                   | `404`; `401`, `403`                                                                                                   | `TranscriptQueryController.java#L102-L124`                                          |
| 17  | Transcript v2 | Kích hoạt tính lại bảng điểm học sinh                     | Gửi yêu cầu tính toán lại bảng điểm học sinh bất đồng bộ qua CalculationTask                    | Path variable `studentCode` hoặc `studentId`, Query param: `academicYearId`                                                                   | `202 Accepted`: `ResCalculationTaskDTO`                                                                         | `404` không tìm thấy HS; `401`, `403` (chỉ ADMIN, ACADEMIC_OFFICE)                                                    | `CalculationTaskController.java#L83-L105`                                           |

---

## Edge Cases

| #   | Feature                                        | Input                                                                | Observed Behavior                                                                                                                                                                                                                                                                    |
| --- | ---------------------------------------------- | -------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 1   | `StudentV3Controller.createStudentWithAccount` | `username == null`                                                   | Hệ thống tự sinh username theo chuẩn: chuyển `studentName` về không dấu, lowercase, bỏ khoảng trắng + 7 ký tự cuối của `studentCode`. Ví dụ: "Khánh Duy" + "STU1234567" -> `khanhduy1234567` (`StudentUsernameGenerator.java#L16-L27`).                                              |
| 2   | `StudentV3Controller.createStudentWithAccount` | `studentName` dài vượt 20 ký tự khi sinh username                    | Fallback lấy các chữ cái đầu (initials) viết tắt + 7 ký tự cuối của `studentCode`. Ví dụ: "Phạm Trần Khánh Duy" + "STU1234567" -> `ptkd1234567`. Nếu vẫn vượt 20 ký tự thì cắt ngắn phần chữ cái đầu để tổng username không vượt 20 ký tự (`StudentUsernameGenerator.java#L29-L38`). |
| 3   | `StudentV3Controller.createStudentWithAccount` | `password == null`                                                   | Hệ thống tự điền mật khẩu mặc định `"12345678"` và mã hóa qua `PasswordEncoder.encode(...)` trước khi persist vào DB (`StudentAccountService.java#L60-L61`).                                                                                                                         |
| 4   | `StudentV3Controller.createStudentWithAccount` | Trùng `studentCode` hoặc trùng `username`                            | Ném `AppException(HttpStatus.CONFLICT, "Mã sinh viên đã tồn tại")` hoặc `"Tên đăng nhập đã tồn tại"`. Transaction rollback toàn bộ, không tạo User hoặc Student mồ côi (`StudentAccountService.java#L47-L55`).                                                                       |
| 5   | `StudentV3Controller.createStudentWithAccount` | Hệ thống chưa seed role `STUDENT`                                    | Ném `AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tìm thấy role STUDENT trong hệ thống")` trước khi persist User (`StudentAccountService.java#L56-L59`).                                                                                                                    |
| 6   | `StudentV3Controller.createStudentWithAccount` | Password hoặc password hash trong response                           | `ResStudentWithAccountDTO` chỉ chứa `Account(userId, username, role)`, TUYỆT ĐỐI không chứa `password` hay `passwordHash` (`ResStudentWithAccountDTO.java#L14`).                                                                                                                     |
| 7   | `StudentController.deleteStudent`              | Xóa học sinh đã có Enrollment/Score/Transcript                       | Ném ngoại lệ `DataIntegrityViolationException` từ tầng JPA/Hibernate do các bảng liên quan có khóa ngoại `ON DELETE RESTRICT`. API trả về lỗi 500 thay vì xử lý an toàn (`StudentService.java#L79`).                                                                                 |
| 8   | `StudentLookupService.resolveStudent`          | Cung cấp cả `studentId` và `studentCode` nhưng không cùng 1 học sinh | Ném `AppException(HttpStatus.BAD_REQUEST, "studentId và studentCode không khớp với cùng một học sinh")` (`StudentLookupService.java#L44-L46`).                                                                                                                                       |
| 9   | `StudentLookupService.resolveStudent`          | Không cung cấp cả `studentId` lẫn `studentCode`                      | Ném `AppException(HttpStatus.BAD_REQUEST, "Phải cung cấp studentId hoặc studentCode")` (`StudentLookupService.java#L38-L40`).                                                                                                                                                        |
| 10  | `StudentService.generateStudentCode`           | DB đã tồn tại toàn bộ mã ngẫu nhiên sinh ra                          | Thử lại tối đa 5 lượt, mỗi lượt 20 mã. Nếu sau 5 lượt vẫn trùng thì ném `AppException(HttpStatus.CONFLICT, "Không thể tạo mã sinh viên duy nhất")` (`StudentService.java#L86-L96`).                                                                                                  |
| 11  | `StudentController.fetchStudents`              | `page < 0` hoặc `size < 0`                                           | Ném `AppException(HttpStatus.BAD_REQUEST, "Trang không được nhỏ hơn 0")` hoặc `"Kích thước trang không được nhỏ hơn 0"` (`StudentServiceSupport.java#L20-L32`). Nếu `size == 0` thì fallback về mặc định là 10.                                                                      |

---

## II. Chi Tiết Khảo Sát Từng Thành Phần Yêu Cầu

### 1. Hiện Trạng Các API Student v1, v2, v3
- **v1 API (`StudentController.java`)**:
  - Đã có đầy đủ các endpoint CRUD cơ bản: `GET /api/v1/students`, `GET /api/v1/students/{id}`, `GET /api/v1/students/code/{code}`, `POST /api/v1/students`, `PUT /api/v1/students/{id}`, `DELETE /api/v1/students/{id}`, `POST /api/v1/students/code`, `GET /api/v1/students/export`.
  - **Hạn chế v1**:
    1. Cơ chế xóa là **xóa cứng (hard delete)**, gây crash 500 khi có ràng buộc khóa ngoại học vụ.
    2. Response `ResStudentDTO` chưa có trường `status` (ACTIVE/INACTIVE/GRADUATED) và chưa có thông tin tài khoản đăng nhập `userId`/`username`.
    3. Bộ lọc `ReqFetchStudentDTO` chỉ hỗ trợ `studentCode`, `studentName`, `birthday`; chưa hỗ trợ lọc theo `status` hay `classId`.
- **v2 Học vụ**:
  - `GET /api/v2/students/{id}/enrollments` & `/api/v2/students/by-code/{code}/enrollments`: Đã sẵn sàng trong `EnrollmentController.java`.
  - `GET /api/v2/attendance/students/{id}/history`: Đã sẵn sàng trong `AttendanceHistoryController.java` (được bổ sung và kiểm thử hoàn chỉnh trong Dev Note 066 ngày 2026-09-04).
  - `GET /api/v2/transcripts/students/{id}/semesters/{semesterId}` & `GET /api/v2/transcripts/students/{id}/academic-years/{academicYearId}`: Đã sẵn sàng trong `TranscriptQueryController.java`.
  - `POST /api/v2/students/{id}/transcripts/recalculate` & `/by-code`: Đã sẵn sàng trong `CalculationTaskController.java`.
- **v3 API (`StudentV3Controller.java`)**:
  - **Trạng thái**: **ĐÃ ĐƯỢC TRIỂN KHAI HOÀN TẤT TRONG BE** (Plan 043 / Dev Note 043).
  - Endpoint: `POST /api/v3/students`
  - Đã đóng gói atomic `@Transactional` tạo đồng thời `User` + role `STUDENT` + `Student` + `StudentInfo`.
  - Đã có `StudentUsernameGenerator` sinh username tự động với fallback initials.
  - Đã có mã hóa mật khẩu mặc định `"12345678"` qua BCrypt (`PasswordEncoder`).
  - Đã có Unit Test kiểm thử logic nghiệp vụ `StudentServiceAccountTest.java`.
  - **Điểm còn thiếu**: Chưa có Controller WebMvc Integration Test cho `StudentV3Controller`.

### 2. Data Model & Database Schema
- **Bảng `student` (`Student.java`)**:
  - Cột: `student_id` (PK), `student_name` (VARCHAR 35), `student_code` (VARCHAR 10, UNIQUE), `status` (VARCHAR 20, default `'ACTIVE'`), `user_id` (BIGINT, UNIQUE, FK -> `app_user.user_id`).
  - Trạng thái `status`: Enum `StudentStatus` gồm 3 giá trị: `ACTIVE`, `INACTIVE`, `GRADUATED`.
- **Bảng `student_info` (`StudentInfo.java`)**:
  - Cột: `info_id` (PK), `student_id` (UQ/FK), `address` (VARCHAR 255), `average_score` (DOUBLE, nullable), `date_of_birth` (DATE, nullable).
  - **ĐIỂM LƯU Ý QUAN TRỌNG VỀ GENDER**: Trong database và Entity `StudentInfo` hiện tại **CHƯA CÓ trường `gender`** (trường `gender` chỉ có ở bảng `teacher`). Schema mục tiêu trong `03-StudentsAndEnrollment.md` có ghi nhận `gender VARCHAR(20)`, nhưng migration thực tế chưa thêm cột này vào `student_info`.
  - **ĐIỂM LƯU Ý VỀ AVERAGE_SCORE**: Trường `average_score` vẫn tồn tại trong entity và DB migration V1, nhưng đã được đánh dấu là deprecated trong kiến trúc v2 (điểm học tập chuyển sang phân hệ Scorebook và Transcript).
- **Ràng buộc khóa ngoại & Vấn đề Xóa an toàn (Safe Delete vs Hard Delete)**:
  - Các bảng sau đều thiết lập FK tham chiếu đến `student(student_id)` với hành vi `RESTRICT`:
    - `student_year_enrollment` (fk_enrollment_student)
    - `student_score` (fk_student_score_student)
    - `student_annual_transcript` (fk_annual_transcript_student)
    - `student_term_transcript` (fk_term_transcript_student)
    - `score_change_request` (fk_score_change_student)
    - `calculation_task` (fk_calculation_task_student)
  - `DELETE /api/v1/students/{id}` hiện tại gọi `studentRepository.delete(student)`. Nếu học sinh đã có dữ liệu học vụ ở bất kỳ bảng nào trên, lệnh xóa sẽ bắn ngoại lệ SQL foreign key violation.
  - Nghiệp vụ v2 yêu cầu: Phải chuyển sang cơ chế quản lý vòng đời hồ sơ (cập nhật `status = INACTIVE` hoặc `GRADUATED`) hoặc chặn xóa kèm thông báo chi tiết khi đã phát sinh dữ liệu học vụ.

### 3. Phân Quyền (Security & Role Matrix)

| Endpoint URI                                             | HTTP Method | Annotation / Controller                         |     ADMIN      | ACADEMIC_OFFICE |       TEACHER        |     STUDENT      | Ghi chú kiểm soát nghiệp vụ              |
| -------------------------------------------------------- | :---------: | ----------------------------------------------- | :------------: | :-------------: | :------------------: | :--------------: | ---------------------------------------- |
| `/api/v1/students`                                       |     GET     | `StudentController` (Class-level)               |    Cho phép    |    Cho phép     |       Cho phép       |  **Chặn (403)**  | Phân trang danh sách HS                  |
| `/api/v1/students/{id}`                                  |     GET     | `StudentController` (Class-level)               |    Cho phép    |    Cho phép     |       Cho phép       |  **Chặn (403)**  | Xem chi tiết HS                          |
| `/api/v1/students/code/{code}`                           |     GET     | `StudentController` (Class-level)               |    Cho phép    |    Cho phép     |       Cho phép       |  **Chặn (403)**  | Tra cứu theo mã STU...                   |
| `/api/v1/students`                                       |    POST     | `StudentController` (Class-level)               |    Cho phép    |    Cho phép     | **Cho phép (BE hở)** |  **Chặn (403)**  | Tạo HS legacy                            |
| `/api/v1/students/{id}`                                  |     PUT     | `StudentController` (Class-level)               |    Cho phép    |    Cho phép     | **Cho phép (BE hở)** |  **Chặn (403)**  | Sửa HS legacy                            |
| `/api/v1/students/{id}`                                  |   DELETE    | `StudentController` (Class-level)               |    Cho phép    |    Cho phép     | **Cho phép (BE hở)** |  **Chặn (403)**  | Xóa HS legacy                            |
| `/api/v3/students`                                       |    POST     | `StudentV3Controller` (`@PreAuthorize`)         |  **Cho phép**  |  **Cho phép**   |    **Chặn (403)**    |  **Chặn (403)**  | Tạo HS kèm tài khoản User                |
| `/api/v2/students/{id}/enrollments`                      |     GET     | `EnrollmentController` (Class-level)            |    Cho phép    |    Cho phép     |       Cho phép       |  **Chặn (403)**  | Lịch sử phân lớp HS                      |
| `/api/v2/students/by-code/{code}/enrollments`            |     GET     | `EnrollmentController` (Class-level)            |    Cho phép    |    Cho phép     |       Cho phép       |  **Chặn (403)**  | Lịch sử phân lớp theo mã                 |
| `/api/v2/attendance/students/{id}/history`               |     GET     | `AttendanceHistoryController` (`@PreAuthorize`) |  **Cho phép**  |  **Cho phép**   |     **Cho phép**     |  **Chặn (403)**  | Chuyên cần theo studentId (Dev Note 066) |
| `/api/v2/attendance/students/me/history`                 |     GET     | `AttendanceHistoryController` (`@PreAuthorize`) | **Chặn (403)** | **Chặn (403)**  |    **Chặn (403)**    | **Chỉ bản thân** | HS tự xem chuyên cần của mình            |
| `/api/v2/transcripts/students/{id}/semesters/{sId}`      |     GET     | `TranscriptQueryController` (`@PreAuthorize`)   |    Cho phép    |    Cho phép     |   Theo lớp dạy/CN    |  **Chặn (403)**  | Bảng điểm kỳ của HS                      |
| `/api/v2/transcripts/students/{id}/academic-years/{yId}` |     GET     | `TranscriptQueryController` (`@PreAuthorize`)   |    Cho phép    |    Cho phép     |   Theo lớp dạy/CN    |  **Chặn (403)**  | Bảng điểm năm của HS                     |
| `/api/v2/transcripts/students/me/semesters/{sId}`        |     GET     | `TranscriptQueryController` (`@PreAuthorize`)   | **Chặn (403)** | **Chặn (403)**  |    **Chặn (403)**    | **Chỉ bản thân** | HS tự xem bảng điểm kỳ                   |
| `/api/v2/students/{id}/transcripts/recalculate`          |    POST     | `CalculationTaskController` (`@PreAuthorize`)   |  **Cho phép**  |  **Cho phép**   |    **Chặn (403)**    |  **Chặn (403)**  | Yêu cầu tính lại điểm                    |

*Lưu ý bảo mật*: `StudentController.java` áp dụng `@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')")` ở mức class mà không ghi đè phương thức cho `POST`, `PUT`, `DELETE`. Điều này khiến vai trò `TEACHER` về mặt kỹ thuật có thể gọi được các lệnh sửa/xóa học sinh legacy nếu không bị FE chặn.

### 4. Hiện Trạng Quality Tooling & Backend Tests
- **Hệ thống build & kiểm tra chất lượng**:
  - Gradle 9.5.1, Spring Boot 4.0.7, Spring Security 6, Spring Data JPA.
  - Checkstyle 10.26.1 (`config/checkstyle/checkstyle.xml`): Max line length 120, quy tắc import chặt chẽ (`AvoidStarImport`, `CustomImportOrder`), fail-on-error (`isIgnoreFailures = false`).
  - PMD 7.16.0 (`config/pmd/ruleset.xml`): Quy tắc clean code, loại trừ một số quy tắc đặc thù (LocalVariableCouldBeFinal, SignatureDeclareThrowsException), fail-on-error (`isIgnoreFailures = false`).
  - JaCoCo 0.8.13: Tự động chạy sau task `test` để sinh báo cáo coverage XML và HTML.
- **Lưu ý đặc thù về môi trường Gradle**:
  - Thư mục cache toàn cục `/home/duyptk/.gradle/caches/journal-1/journal-1.lock` có thể bị chiếm giữ bởi process khác trên host (PID 185267).
  - Khuyến nghị khi chạy các lệnh Gradle trong dự án: Dùng wrapper distribution đã giải nén tại `/home/duyptk/.gradle/wrapper/dists/gradle-9.5.1-bin/iq79hdu3mqx29lgffhp8bfmx/gradle-9.5.1/bin/gradle` kết hợp cờ `--offline` và chỉ định `GRADLE_USER_HOME` cục bộ (như `.gradle-user-home` hoặc `/tmp/java_coban_gradle`).
- **Danh mục Test hiện có liên quan đến Student**:
  - `StudentServiceMutationTest`: Test tạo, cập nhật, xóa học sinh ở tầng Service.
  - `StudentServiceFetchTest`: Test phân trang, tìm kiếm và sắp xếp danh sách học sinh.
  - `StudentServiceCodeTest`: Test logic sinh mã học sinh tự động.
  - `StudentServiceAccountTest`: Test tạo học sinh kèm tài khoản User v3, kiểm tra mật khẩu mặc định, sinh username, gán role STUDENT, kiểm tra conflict username/code.
  - `StudentUsernameGeneratorTest`: Test chuẩn hóa tên tiếng Việt và fallback chữ cái đầu (initials).
  - `StudentLookupServiceTest`: Test resolve học sinh qua ID hoặc Code.
  - `StudentControllerIntegrationTest`, `StudentDetailControllerIntegrationTest`, `StudentAuthorizationIntegrationTest`, `StudentValidationControllerIntegrationTest`, `StudentCsvExportControllerIntegrationTest`: Các bài test tích hợp MockMvc cho API v1.
- **Khoảng trống Test (Test Gaps) cần bổ sung khi triển khai**:
  1. Thiếu WebMvc Integration Test cho `StudentV3Controller`: Cần viết test kiểm thử `@WithMockUser` xác nhận chỉ `ADMIN` và `ACADEMIC_OFFICE` được gọi `POST /api/v3/students`, còn `TEACHER` và `STUDENT` bị `403 Forbidden`.
  2. Thiếu test kiểm tra Transaction Rollback của flow v3 khi lưu `Student` thất bại sau khi đã lưu `User`.
  3. Thiếu test kiểm tra hành vi xung đột khóa ngoại khi xóa học sinh đã có dữ liệu học vụ v2.
  4. Thiếu bộ lọc `status` trong `ReqFetchStudentDTO` và `StudentSpecifications` nếu muốn màn hình v2 lọc học sinh theo trạng thái (ACTIVE/INACTIVE/GRADUATED).

---

## III. Kết Luận & Khuyến Nghị Cho Các Phân Hệ Sau

1. **Về API `POST /api/v3/students`**: Backend ĐÃ CÓ VÀ ĐÃ HOÀN THIỆN ĐẦY ĐỦ về mặt logic nghiệp vụ, transaction, bảo mật và unit test service. Frontend v2 có thể tích hợp trực tiếp endpoint này cho form thêm mới học sinh của vai trò ADMIN/ACADEMIC_OFFICE.
2. **Về API Học vụ v2 (Enrollments, Attendance, Transcripts)**: Tất cả các API cần thiết cho 4 tab chi tiết học sinh đều đã có sẵn trong BE (`/enrollments`, `/attendance/students/{id}/history`, `/transcripts/students/{id}/...`, `/recalculate`).
3. **Về trường `gender` và `status`**:
   - `status`: Entity `Student` đã có cột `status` và enum `StudentStatus`, nhưng DTO `ResStudentDTO` v1 chưa expose trường này ra ngoài API list/detail.
   - `gender`: BE hoàn toàn chưa có trường `gender` trong bảng `student_info` (chỉ có trong tài liệu mục tiêu). Nếu FE yêu cầu hiển thị/nhập trường này, cần phối hợp với BE để bổ sung migration và DTO nếu nằm trong scope phê duyệt.
4. **Về chính sách xóa an toàn (R5)**: Cần bổ sung cảnh báo ở FE hoặc nâng cấp BE endpoint để cập nhật `status = INACTIVE` thay vì cho phép gọi `DELETE /api/v1/students/{id}` trực tiếp gây vỡ dữ liệu lịch sử.

