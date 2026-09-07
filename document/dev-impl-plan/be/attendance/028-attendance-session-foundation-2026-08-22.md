# Developer Plan 028: Attendance Session Foundation

## 1. Trạng thái và phiên bản áp dụng

- Status: `Draft - awaiting user approval`.
- Application-document version: `v2`.
- Ngày lập plan: `2026-08-22`.
- Phụ thuộc: Plan 026 (academic year, school class, student enrollment và audit log)
  và Plan 027 (teacher, homeroom assignment).
- Chỉ bắt đầu implementation sau khi user phê duyệt plan bằng tin nhắn qua agent.

## 2. Mục tiêu

Xây dựng backend foundation cho điểm danh theo buổi, theo cơ chế lưu ngoại lệ thay vì lưu
`PRESENT` cho mọi học sinh.

Kết quả mong muốn:

- tạo hoặc lấy `attendance_session` theo lớp, học kỳ, ngày học và buổi;
- hỗ trợ hai buổi `MORNING` và `AFTERNOON`;
- chỉ cho tạo session khi ngày điểm danh hợp lệ trong học kỳ và lớp thuộc cùng năm học;
- GVCN chỉ thao tác lớp được phân công;
- xem danh sách học sinh của buổi với trạng thái mặc định `PRESENT`;
- tạo, cập nhật và xóa ngoại lệ điểm danh;
- mỗi học sinh có tối đa một ngoại lệ trong một buổi;
- mọi thay đổi ngoại lệ điểm danh được audit.

## 3. Requirement liên quan

### 3.1. Functional requirements

- `FR-ATTENDANCE-001`: GVCN xem danh sách điểm danh theo ngày và buổi.
- `FR-ATTENDANCE-002`: Hệ thống mặc định học sinh có mặt.
- `FR-ATTENDANCE-003`: GVCN nhập các trường hợp ngoại lệ.
- `FR-ATTENDANCE-004`: GVCN cập nhật hoặc xóa ngoại lệ trong phạm vi quyền.
- `FR-CALENDAR-004`: Hệ thống cung cấp lịch buổi học cho nghiệp vụ điểm danh và báo cáo
  chuyên cần.

### 3.2. Business rules và constraint

- `BR-ATTENDANCE-001`: Điểm danh theo buổi.
- `BR-ATTENDANCE-002`: Một ngày có hai buổi sáng và chiều.
- `BR-ATTENDANCE-003`: Học sinh không tự điểm danh.
- `BR-ATTENDANCE-004`: Hệ thống lưu ngoại lệ thay vì tạo một record `PRESENT` cho mọi học sinh.
- `BR-ATTENDANCE-005`: Không có ngoại lệ và buổi học hợp lệ được suy ra là `PRESENT`.
- `BR-ATTENDANCE-006`: Không có dữ liệu chỉ được hiểu là `PRESENT` đối với buổi học hợp lệ.
- `BR-ATTENDANCE-007`: Không tính có mặt trước ngày học sinh vào lớp hoặc sau ngày rời lớp.
- `BR-ATTENDANCE-008`: Mỗi học sinh có tối đa một ngoại lệ chính trong một buổi.
- `BR-ATTENDANCE-009`: GVCN chỉ quản lý điểm danh lớp mình.
- `BR-CALENDAR-001`: Mỗi ngày có tối đa hai buổi: `MORNING` và `AFTERNOON`.
- `BR-CALENDAR-002`: Một buổi phải hợp lệ trước khi dùng trong chuyên cần.
- `BR-CALENDAR-003`: Ngày nghỉ, ngày lễ và buổi không học không được tính là có mặt.
- `BR-CALENDAR-004`: Lịch học thuộc năm học và nằm trong học kỳ phù hợp.
- `BR-COMMON-003`: Thay đổi quan trọng phải lưu người thực hiện, thời gian, before/after
  và reason nếu có.
- `BR-AUTH-003`, `BR-AUTH-006`: Backend kiểm tra phạm vi thao tác từ phân công thực tế.

### 3.3. Decision ghi nhận trong Plan 028

Tài liệu v2 hiện có lệch tên trạng thái:

- requirement module ghi `EXCUSED_ABSENCE`, `UNEXCUSED_ABSENCE`, `LATE`, `EARLY_LEAVE`;
- data model ghi `PRESENT`, `ABSENT`, `LATE`, `EXCUSED`;
- yêu cầu Plan 028 chốt `ABSENT`, `EXCUSED`, `LATE`, `EARLY_LEAVE`.

Implementation Plan 028 sẽ dùng đúng chỉ dẫn mới nhất của user:

```text
ABSENT, EXCUSED, LATE, EARLY_LEAVE
```

`PRESENT` chỉ là trạng thái suy ra trong response, không lưu thành ngoại lệ.

## 4. Phạm vi

### 4.1. In-scope

- Tạo module backend `attendance`.
- Tạo JPA mapping cho `attendance_session` và `attendance_record`.
- Tạo DTO request/response cho session, danh sách học sinh và ngoại lệ.
- Tạo API:
  - tạo hoặc lấy session theo lớp, học kỳ, ngày và buổi;
  - xem danh sách học sinh trong session với default `PRESENT`;
  - upsert ngoại lệ cho một học sinh;
  - xóa ngoại lệ để học sinh quay về `PRESENT`.
- Validate lớp, học kỳ, ngày học, buổi học, enrollment và quyền GVCN.
- Enforce unique:
  - `attendance_session(class_id, attendance_date, session_period)`;
  - `attendance_record(session_id, student_id)`.
- Audit khi tạo/cập nhật/xóa ngoại lệ.
- Unit test service chính và validation quan trọng.
- Chạy validation backend bắt buộc trước khi báo hoàn tất.

### 4.2. Out-of-scope

- Frontend.
- Postman collection, trừ khi user yêu cầu riêng sau khi duyệt.
- Báo cáo thống kê chuyên cần.
- API học sinh xem lịch sử chuyên cần.
- Đơn xin nghỉ, quyền phụ huynh hoặc workflow duyệt phép.
- Bảng calendar/schedule production đầy đủ cho ngày nghỉ, ngày lễ và cấu hình buổi không học.
- Migration production tool nếu project chưa chốt Flyway/Liquibase.
- Không đổi các API legacy `/api/v1/**`.
- Không sửa các module điểm, transcript hoặc calculation task.

### 4.3. Giới hạn về "ngày học hợp lệ"

Trong tài liệu hiện có chưa thấy implementation bảng calendar riêng. Vì vậy Plan 028 sẽ
validate ngày học hợp lệ ở mức foundation:

- `attendance_date` phải nằm trong `semester.startDate` và `semester.endDate`;
- `semester` phải thuộc cùng `academicYearId` với `school_class`;
- `session_period` chỉ nhận `MORNING` hoặc `AFTERNOON`.

Nếu cần chặn ngày nghỉ/lễ hoặc buổi không học theo calendar table riêng, cần mở một plan/delta
riêng vì hiện chưa có schema/API tương ứng trong codebase.

## 5. Kiến trúc và luồng hiện tại

Backend là Spring Boot modular monolith, dùng Spring Data JPA, JWT, `@PreAuthorize`,
`AuditContext`, `AuditLog` và `AppException`.

Các module đã có liên quan:

- `academic`: `AcademicYear`, `Semester`, `SchoolClass` và repository/service/controller.
- `enrollment`: `StudentYearEnrollment` dùng để xác định học sinh đang thuộc lớp.
- `assignment`: `HomeroomAssignment` dùng để xác định GVCN theo lịch sử, không lưu trực tiếp
  trong `school_class`.
- `teacher`: `Teacher` có liên kết `userId`, dùng để map user đăng nhập sang teacher.
- `common.audit`: `AuditLog`, `AuditLogRepository`, `AuditContext`.

Plan 028 thêm dependency một chiều:

```text
attendance controller
  -> attendance service/guard
    -> attendance repositories
    -> academic repositories
    -> enrollment repository
    -> assignment repository
    -> teacher repository
    -> audit_log
```

Giữ convention hiện tại: entity dùng scalar `Long` cho FK, DTO không expose JPA entity,
controller mỏng, service giữ business rule và transaction.

## 6. Phương án triển khai

### 6.1. Domain model

Tạo:

- `AttendanceSession`
  - `id`;
  - `classId`;
  - `semesterId`;
  - `attendanceDate`;
  - `sessionPeriod`;
  - `createdBy`;
  - `createdAt`.
- `AttendanceRecord`
  - `id`;
  - `sessionId`;
  - `studentId`;
  - `status`;
  - `note`;
  - `recordedBy`;
  - `recordedAt`;
  - `updatedBy`;
  - `updatedAt`.
- `AttendanceSessionPeriod`
  - `MORNING`;
  - `AFTERNOON`.
- `AttendanceExceptionStatus`
  - `ABSENT`;
  - `EXCUSED`;
  - `LATE`;
  - `EARLY_LEAVE`.

`AttendanceRecord` chỉ lưu exception. Không tạo record `PRESENT`.

### 6.2. Session flow

`createOrGetSession(request)`:

1. Kiểm tra class tồn tại.
2. Kiểm tra semester tồn tại.
3. Kiểm tra class và semester cùng academic year.
4. Kiểm tra `attendanceDate` nằm trong khoảng học kỳ.
5. Kiểm tra `sessionPeriod` hợp lệ.
6. Kiểm tra user hiện tại là GVCN active của lớp tại ngày điểm danh.
7. Nếu session đã tồn tại theo unique key thì trả session đó.
8. Nếu chưa có thì tạo mới với `createdBy = AuditContext.currentUserId()`.

### 6.3. List students flow

`listSessionStudents(sessionId)`:

1. Kiểm tra session tồn tại.
2. Kiểm tra user hiện tại là GVCN active của lớp trong ngày session.
3. Lấy học sinh đang thuộc lớp tại ngày session từ `student_year_enrollment`.
4. Lấy toàn bộ exception của session.
5. Map response:
   - có exception: trả status exception và note/record metadata;
   - không có exception: trả status `PRESENT`, không có `attendanceRecordId`.

### 6.4. Upsert exception flow

`upsertException(sessionId, studentId, request)`:

1. Kiểm tra session tồn tại.
2. Kiểm tra user hiện tại là GVCN active của lớp.
3. Kiểm tra học sinh thuộc lớp tại ngày session.
4. Kiểm tra status thuộc `ABSENT`, `EXCUSED`, `LATE`, `EARLY_LEAVE`.
5. Nếu chưa có record thì tạo mới.
6. Nếu đã có record thì cập nhật status/note và `updatedBy`, `updatedAt`.
7. Ghi audit trong cùng transaction:
   - create: `ATTENDANCE_EXCEPTION_CREATED`;
   - update: `ATTENDANCE_EXCEPTION_UPDATED`.

### 6.5. Delete exception flow

`deleteException(sessionId, studentId)`:

1. Kiểm tra session tồn tại.
2. Kiểm tra user hiện tại là GVCN active của lớp.
3. Kiểm tra record tồn tại.
4. Xóa row exception để trạng thái response quay về `PRESENT`.
5. Ghi audit `ATTENDANCE_EXCEPTION_DELETED` với `before_data` có record cũ và `after_data = null`.

Trade-off: data model có ghi chú "Không xóa record đã dùng trong báo cáo; nếu cần hủy,
sử dụng trạng thái hoặc audit correction". Tuy nhiên Plan 028 yêu cầu "xóa ngoại lệ" và
không định nghĩa status hủy. Vì foundation chưa có báo cáo chuyên cần, phương án hiện tại
xóa row exception nhưng vẫn giữ audit immutable. Nếu về sau báo cáo cần immutable correction,
cần thêm plan soft-delete/correction status.

### 6.6. Authorization

- Controller dùng `@PreAuthorize("hasRole('TEACHER')")` cho thao tác GVCN trong Plan 028.
- Service vẫn kiểm tra quyền thật bằng `Teacher.userId -> HomeroomAssignment`.
- Nếu user không có teacher profile hoặc không có assignment active cho lớp tại ngày session,
  trả `403 FORBIDDEN`.
- `ADMIN`/`ACADEMIC_OFFICE` override không nằm trong yêu cầu "GVCN chỉ thao tác lớp được
  phân công", nên Plan 028 không mở quyền mutation cho office. Nếu cần office điều chỉnh theo
  `FR-ATTENDANCE-005`, sẽ làm trong plan riêng.

### 6.7. Audit

Tạo `AttendanceAuditService`, tái sử dụng `AuditLogRepository`, `ObjectMapper` và `AuditContext`.

Audit payload tối thiểu:

- `attendanceRecordId`;
- `sessionId`;
- `classId`;
- `semesterId`;
- `attendanceDate`;
- `sessionPeriod`;
- `studentId`;
- `status`;
- `note`;
- `recordedBy`;
- `recordedAt`;
- `updatedBy`;
- `updatedAt`.

Không tạo bảng audit mới.

## 7. API contract v2 dự kiến

Response tiếp tục dùng `RestResponse`/`@ApiMessage` hiện có.

| Method | Path | Quyền | Mục đích |
|---|---|---|---|
| `POST` | `/api/v2/attendance-sessions` | GVCN của lớp | Tạo hoặc lấy session |
| `GET` | `/api/v2/attendance-sessions/{sessionId}/students` | GVCN của lớp | Xem điểm danh buổi |
| `PUT` | `/api/v2/attendance-sessions/{sessionId}/exceptions/{studentId}` | GVCN của lớp | Tạo/cập nhật ngoại lệ |
| `DELETE` | `/api/v2/attendance-sessions/{sessionId}/exceptions/{studentId}` | GVCN của lớp | Xóa ngoại lệ |

### 7.1. Request DTO

`ReqCreateAttendanceSessionDTO`:

```text
classId: Long
semesterId: Long
attendanceDate: LocalDate
sessionPeriod: MORNING | AFTERNOON
```

`ReqUpsertAttendanceExceptionDTO`:

```text
status: ABSENT | EXCUSED | LATE | EARLY_LEAVE
note: String nullable, max 500
```

### 7.2. Response DTO

`ResAttendanceSessionDTO`:

```text
sessionId
classId
semesterId
attendanceDate
sessionPeriod
createdBy
createdAt
```

`ResAttendanceStudentDTO`:

```text
studentId
studentCode
studentName
attendanceRecordId nullable
status: PRESENT | ABSENT | EXCUSED | LATE | EARLY_LEAVE
note nullable
recordedBy nullable
recordedAt nullable
updatedBy nullable
updatedAt nullable
```

`ResAttendanceExceptionDTO`:

```text
attendanceRecordId
sessionId
studentId
status
note
recordedBy
recordedAt
updatedBy
updatedAt
```

## 8. Phạm vi mã nguồn dự kiến

### 8.1. Tạo mới

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/controller/AttendanceController.java`
  - REST endpoints cho session, list students và exception CRUD.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceService.java`
  - transaction orchestration, mapping response và business flow.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceGuard.java`
  - lookup và validation class/semester/date/student/GVCN.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceAuditService.java`
  - ghi audit cho exception mutation.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/repository/AttendanceSessionRepository.java`
  - query session by unique key.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/repository/AttendanceRecordRepository.java`
  - query/upsert/delete exception by `sessionId + studentId`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/entity/AttendanceSession.java`
  - JPA entity `attendance_session`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/entity/AttendanceRecord.java`
  - JPA entity `attendance_record`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/entity/AttendanceSessionPeriod.java`
  - enum `MORNING`, `AFTERNOON`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/entity/AttendanceExceptionStatus.java`
  - enum `ABSENT`, `EXCUSED`, `LATE`, `EARLY_LEAVE`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/DTOs/requests/ReqCreateAttendanceSessionDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/DTOs/requests/ReqUpsertAttendanceExceptionDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/DTOs/response/ResAttendanceSessionDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/DTOs/response/ResAttendanceStudentDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/DTOs/response/ResAttendanceExceptionDTO.java`

### 8.2. Chỉnh sửa có thể cần

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/enrollment/repository/StudentYearEnrollmentRepository.java`
  - thêm query lấy học sinh active trong lớp tại ngày điểm danh;
  - thêm exists để kiểm tra một học sinh thuộc lớp tại ngày điểm danh.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/assignment/repository/HomeroomAssignmentRepository.java`
  - thêm query kiểm tra GVCN active theo `classId`, `teacherId`, `effectiveDate`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/teacher/repository/TeacherRepository.java`
  - thêm lookup theo `userId` nếu chưa có.

### 8.3. Test dự kiến

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceGuardTest.java`

## 9. Test plan

### 9.1. Unit test service

`AttendanceService.createOrGetSession`:

- success khi lớp/học kỳ hợp lệ, ngày nằm trong học kỳ và user là GVCN lớp;
- idempotent khi session đã tồn tại;
- lỗi `404` khi class không tồn tại;
- lỗi `404` khi semester không tồn tại;
- lỗi `409` khi class và semester khác năm học;
- lỗi `409` khi ngày ngoài học kỳ;
- lỗi `403` khi user không phải GVCN lớp.

`AttendanceService.listSessionStudents`:

- học sinh active trong lớp trả `PRESENT` nếu không có exception;
- học sinh có exception trả đúng status/note/metadata;
- không trả học sinh chưa vào lớp hoặc đã rời lớp;
- lỗi `403` khi user không phải GVCN lớp.

`AttendanceService.upsertException`:

- tạo mới exception `ABSENT`;
- tạo mới exception `EXCUSED`;
- tạo mới exception `LATE`;
- tạo mới exception `EARLY_LEAVE`;
- update exception cùng `sessionId + studentId`;
- reject student không thuộc lớp tại ngày session;
- ghi audit create/update.

`AttendanceService.deleteException`:

- xóa exception hiện có;
- sau khi xóa, list students suy ra `PRESENT`;
- lỗi khi exception không tồn tại theo convention repository/service hiện có;
- ghi audit delete.

### 9.2. Boundary và validation

- `sessionPeriod` null bị Bean Validation chặn.
- `attendanceDate` null bị Bean Validation chặn.
- `note` quá 500 ký tự bị Bean Validation chặn.
- `studentId`, `sessionId`, `classId`, `semesterId` phải positive.
- Duplicate exception được xử lý bằng update, không tạo row thứ hai.

### 9.3. Mock và assertion

- Mock repositories cho unit test service/guard.
- Mock `AuditContext` nếu code hiện tại cho phép; nếu không, assertion audit qua
  `AuditLogRepository.save(...)` interaction.
- Assertion output:
  - DTO đúng id/status/note;
  - `PRESENT` chỉ xuất hiện ở response;
  - audit action đúng;
  - exception HTTP status/message đúng theo `AppException`.

### 9.4. Validation commands

Sau implementation chạy các kiểm tra thực tế theo cấu hình `BE/BaiTap-RS`:

```bash
./mvnw test
./mvnw checkstyle:check
./mvnw pmd:check
./mvnw verify
```

Nếu project không có wrapper hoặc command khác trong `pom.xml`, dùng lệnh Maven tương ứng
thực tế và báo rõ.

JaCoCo: đọc report generated của Maven nếu project đang cấu hình JaCoCo; không tự đặt threshold
mới ngoài cấu hình hiện có.

## 10. Rủi ro và giảm thiểu

- Lệch contract status giữa tài liệu và yêu cầu Plan 028: ghi nhận trong plan và dùng yêu cầu
  mới nhất của user.
- Chưa có calendar table đầy đủ: giới hạn validation ngày hợp lệ theo học kỳ và session enum;
  không tự dựng module calendar ngoài scope.
- Xóa ngoại lệ bằng delete row có thể chưa phù hợp khi báo cáo chuyên cần immutable ra đời:
  giảm thiểu bằng audit before/after và ghi rõ cần plan correction nếu báo cáo yêu cầu.
- Authorization GVCN phụ thuộc liên kết `Teacher.userId`: guard sẽ trả `403` nếu user đăng nhập
  chưa có teacher profile hoặc chưa có homeroom assignment active.
- Data integrity với concurrent upsert: dùng unique constraint và repository lookup theo
  `sessionId + studentId`; nếu cần lock chi tiết hơn sau validation sẽ thêm trong phạm vi nhỏ.

## 11. Output dự kiến

Sau khi hoàn thành và validation pass:

- Backend có module `attendance` hoạt động dưới `/api/v2/attendance-sessions`.
- GVCN tạo/lấy session cho lớp mình, theo buổi sáng/chiều và ngày trong học kỳ.
- Danh sách học sinh trong session trả `PRESENT` mặc định.
- GVCN tạo/cập nhật/xóa exception `ABSENT`, `EXCUSED`, `LATE`, `EARLY_LEAVE`.
- Mỗi học sinh chỉ có một exception trong một session.
- Audit log ghi lại create/update/delete exception.
- Có unit test và báo cáo validation thực tế.

## 12. Approval gate

Plan này đang ở trạng thái draft. Không sửa production code, test code hoặc API contract cho tới khi
user phê duyệt rõ ràng bằng tin nhắn qua agent.
