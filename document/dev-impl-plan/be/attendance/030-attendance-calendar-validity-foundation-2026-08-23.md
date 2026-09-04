# Developer Plan 030: Attendance Calendar Validity Foundation

## 1. Trạng thái và phiên bản áp dụng

- Status: `Approved`.
- Application-document version: `v2`.
- Ngày lập plan: `2026-08-23`.
- Task sequence: bỏ qua Plan 029 theo chỉ dẫn của user.
- Phụ thuộc: Plan 026 (academic year, school class, enrollment), Plan 027 (semester,
  assignment) và Plan 028 (attendance session/exception foundation).
- Chỉ bắt đầu implementation sau khi user phê duyệt plan bằng tin nhắn qua agent.

## 2. Mục tiêu

Hoàn thiện nguồn dữ liệu lịch học tối thiểu để attendance phân biệt buổi học hợp lệ với
ngày nghỉ, ngày lễ và buổi không học. Sau thay đổi, `PRESENT` chỉ được suy ra khi session
điểm danh tương ứng có calendar entry hợp lệ; ngày nằm trong học kỳ nhưng không có lịch
không còn được xem là buổi học hợp lệ.

Kết quả mong muốn:

- Có schema/JPA foundation cho ngày học và hai buổi `MORNING`, `AFTERNOON`.
- Giáo vụ có thể cấu hình hoặc điều chỉnh trạng thái lịch trong phạm vi API v2.
- Giáo viên/học sinh có thể đọc lịch thuộc phạm vi được phép.
- Attendance session chỉ được tạo/lấy khi ngày và buổi đã được đánh dấu học.
- Dữ liệu lịch thuộc đúng năm học và học kỳ, có unique constraint và audit metadata.

## 3. Requirement liên quan

### 3.1. Functional requirements

- `FR-CALENDAR-001`: Giáo vụ cấu hình ngày và buổi học hợp lệ.
- `FR-CALENDAR-002`: Giáo vụ đánh dấu ngày nghỉ, ngày lễ hoặc buổi không học.
- `FR-CALENDAR-003`: Giáo viên và học sinh xem lịch học thuộc phạm vi của mình.
- `FR-CALENDAR-004`: Hệ thống cung cấp lịch buổi học cho attendance và báo cáo.
- `FR-ATTENDANCE-001`, `FR-ATTENDANCE-002`: xem attendance và chỉ suy ra mặc định
  `PRESENT` cho buổi học hợp lệ.

### 3.2. Business rules và constraint

- `BR-CALENDAR-001`: mỗi ngày tối đa hai buổi `MORNING` và `AFTERNOON`.
- `BR-CALENDAR-002`: buổi phải được đánh dấu có học trước khi dùng trong attendance/report.
- `BR-CALENDAR-003`: ngày nghỉ, ngày lễ và buổi không học không được tính là có mặt.
- `BR-CALENDAR-004`: lịch thuộc năm học và nằm trong khoảng học kỳ phù hợp.
- `BR-ATTENDANCE-005`, `BR-ATTENDANCE-006`: không có exception chỉ suy ra `PRESENT` nếu
  session là buổi học hợp lệ.
- `BR-COMMON-003`: thay đổi cấu hình quan trọng phải có người thực hiện, thời gian,
  before/after và reason nếu có.

## 4. Phạm vi

### 4.1. In-scope

- Tạo module backend `calendar` hoặc package lịch tương thích convention feature-first hiện tại.
- Tạo mô hình ngày lịch và buổi lịch tối thiểu, dùng scalar `Long` cho FK và enum dạng chuỗi.
- Tạo Flyway migration với FK tới `academic_year`, `semester` và user cấu hình.
- Enforce unique cho `{academic_year_id, calendar_date}` và
  `{calendar_day_id, session_period}`.
- Validate ngày thuộc năm học, thuộc khoảng học kỳ và không có hai semester chồng lấn không
  phù hợp với bản ghi lịch.
- API v2 tối thiểu cho office upsert/điều chỉnh lịch và đọc lịch theo khoảng ngày.
- Phân quyền: `ADMIN`/`ACADEMIC_OFFICE` mutation; `TEACHER`/`STUDENT` read theo contract
  hiện có và không expose dữ liệu ngoài phạm vi cần thiết.
- Tạo `CalendarValidityService` hoặc abstraction tương đương để attendance gọi kiểm tra
  `{semesterId, attendanceDate, sessionPeriod}`.
- Sửa `AttendanceGuard`/flow Plan 028 để reject session không có buổi học hợp lệ.
- Audit create/update trạng thái ngày hoặc buổi bằng `audit_log`; không tạo bảng audit riêng.
- Unit test service/guard và integration test cho migration/constraint/API nếu pattern hiện
  tại hỗ trợ.
- Chạy validation backend bắt buộc sau implementation.

### 4.2. Out-of-scope

- Frontend và Storybook.
- Báo cáo/thống kê chuyên cần, mẫu số báo cáo hoặc API history của học sinh.
- Tự động sinh lịch theo thứ trong tuần, lịch năm học, lịch quốc gia hoặc import file.
- Workflow duyệt ngày nghỉ, đơn xin nghỉ, phụ huynh và office correction nâng cao.
- Lịch riêng theo từng lớp/môn nếu chưa có requirement/schema được phê duyệt.
- Thay đổi các API legacy `/api/v1/**`, score, transcript hoặc calculation.
- Postman collection, trừ khi user yêu cầu riêng.

### 4.3. Assumption cần user xác nhận khi approval

Plan dùng mô hình lịch cấp năm học/học kỳ, không gắn trực tiếp với từng lớp. Mỗi ngày có một
calendar day và tối đa hai calendar session; session không tồn tại hoặc có trạng thái không học
đều không hợp lệ cho attendance. Nếu nghiệp vụ yêu cầu lịch khác nhau theo lớp, plan này cần
được cập nhật trước implementation vì khóa unique và API contract sẽ thay đổi.

## 5. Kiến trúc và luồng hiện tại

Backend là Spring Boot modular monolith dùng Spring Data JPA, Flyway, JWT,
`@PreAuthorize`, `AuditContext`, `AuditLog` và `AppException`.

Plan 028 hiện validate attendance date chỉ bằng khoảng ngày của semester và session period
bằng enum trong `AttendanceGuard`. Attendance session/record đã tồn tại ở migration V6.

Dependency sau thay đổi:

```text
calendar controller
  -> calendar service/guard
    -> calendar repositories
    -> academic year/semester repositories
    -> audit_log

attendance guard
  -> calendar validity service
```

Calendar service là nguồn kiểm tra validity dùng chung; không copy query calendar vào
`AttendanceService`.

## 6. Phương án triển khai

### 6.1. Domain model đề xuất

Tạo `CalendarDay`:

- `id`;
- `academicYearId`;
- `semesterId`;
- `calendarDate`;
- `dayType`: `SCHOOL_DAY`, `WEEKEND`, `HOLIDAY`, `NO_CLASS`;
- `reason` nullable, tối đa 500 ký tự;
- `configuredBy`, `configuredAt`, `updatedBy`, `updatedAt`.

Tạo `CalendarSession`:

- `id`;
- `calendarDayId`;
- `sessionPeriod`: `MORNING`, `AFTERNOON`;
- `sessionStatus`: `SCHEDULED`, `NO_CLASS`;
- `reason` nullable;
- metadata cấu hình và cập nhật.

`SCHEDULED` chỉ hợp lệ khi `CalendarDay.dayType = SCHOOL_DAY`. Với `HOLIDAY`, `WEEKEND`
hoặc `NO_CLASS`, các session tương ứng không hợp lệ. Không tạo record calendar mặc định
ngầm trong attendance vì sẽ làm mất khả năng phân biệt ngày chưa cấu hình với ngày học.

### 6.2. Calendar write flow

1. Load academic year và semester.
2. Kiểm tra semester thuộc academic year và ngày nằm trong khoảng semester.
3. Tạo hoặc cập nhật `CalendarDay` theo academic year/date.
4. Validate `dayType` và reason.
5. Upsert tối đa hai `CalendarSession` theo period.
6. Không cho trạng thái `SCHEDULED` nếu ngày là `HOLIDAY`, `WEEKEND` hoặc `NO_CLASS`.
7. Ghi audit before/after trong cùng transaction.

### 6.3. Attendance validity flow

`AttendanceGuard.validateClassSemesterAndDate(...)` giữ các kiểm tra class/semester/date hiện
có, sau đó gọi `CalendarValidityService.assertScheduled(semesterId, attendanceDate,
sessionPeriod)`. Nếu không có session `SCHEDULED`, trả `409 CONFLICT` theo convention hiện tại
và không tạo session attendance.

Việc đọc danh sách học sinh của một attendance session đã tồn tại cũng kiểm tra validity để
không suy ra `PRESENT` cho một buổi đã bị đổi thành `NO_CLASS`. Cách xử lý session attendance
đã tồn tại khi calendar bị đổi trạng thái cần giữ dữ liệu exception/audit; không xóa cascade.

### 6.4. API contract v2 dự kiến

| Method | Path | Quyền | Mục đích |
|---|---|---|---|
| `PUT` | `/api/v2/calendar/days/{calendarDate}` | Office | Upsert ngày và các buổi của ngày |
| `GET` | `/api/v2/calendar/days` | Teacher/Student/Office | Đọc lịch theo `academicYearId`, `semesterId`, `from`, `to` |

Request/response phải dùng DTO, không expose entity. Contract chi tiết gồm `dayType`, danh sách
session period/status, reason và audit metadata phù hợp; tên DTO cụ thể sẽ bám convention
module academic sau khi plan được duyệt.

## 7. Phạm vi mã nguồn dự kiến

### 7.1. Tạo mới

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/calendar/controller/CalendarController.java`:
  endpoint cấu hình và đọc lịch.
- `.../calendar/service/CalendarService.java`: transaction, upsert, đọc và mapping.
- `.../calendar/service/CalendarValidityService.java`: kiểm tra buổi `SCHEDULED` cho attendance.
- `.../calendar/service/CalendarAuditService.java`: ghi audit qua hạ tầng dùng chung.
- `.../calendar/repository/CalendarDayRepository.java` và `CalendarSessionRepository.java`:
  lookup/upsert/range query.
- `.../calendar/domain/entity/CalendarDay.java`, `CalendarSession.java` và các enum trạng thái.
- DTO request/response của calendar.
- Migration kế tiếp sau V6 cho calendar tables.

### 7.2. Chỉnh sửa

- `attendance/service/AttendanceGuard.java`: gọi calendar validity khi tạo/đọc session.
- `attendance/service/AttendanceService.java`: truyền period khi cần và giữ không suy ra
  `PRESENT` cho session không hợp lệ.
- Test attendance hiện có: cập nhật fixture để tạo calendar entry `SCHEDULED`.

### 7.3. Test dự kiến

- `calendar/service/CalendarServiceTest.java`.
- `calendar/service/CalendarValidityServiceTest.java`.
- `attendance/service/AttendanceGuardTest.java` và `AttendanceServiceTest.java`.
- Integration test API/migration/constraint theo pattern test backend hiện tại.

## 8. Test và validation

### 8.1. Unit và integration cases

- Tạo ngày học và upsert hai session thành công.
- Reject period thứ ba hoặc duplicate period.
- Reject ngày ngoài academic year/semester hoặc semester khác academic year.
- Reject `SCHEDULED` trên ngày `HOLIDAY`, `WEEKEND` hoặc `NO_CLASS`.
- Đọc đúng calendar range và không trả dữ liệu ngoài semester/scope.
- Teacher/Student không được mutation; office mutation được phép.
- Attendance tạo session thành công khi calendar session `SCHEDULED`.
- Attendance reject ngày trong semester nhưng chưa có calendar session.
- Attendance reject session đã chuyển `NO_CLASS` và không suy ra `PRESENT`.
- Calendar update ghi audit before/after và không xóa attendance history.
- Migration enforce FK, unique và enum/check constraints.

### 8.2. Validation commands

Sau implementation chạy tuần tự theo cấu hình thực tế của `BE/BaiTap-RS`:

```bash
./gradlew test
./gradlew checkstyleMain
./gradlew checkstyleTest
./gradlew pmdMain
./gradlew pmdTest
./gradlew build
```

Đọc JaCoCo report nếu task tạo coverage report; không tự đặt threshold mới ngoài cấu hình
hiện có. Không chạy các lệnh Gradle song song để tránh lỗi file test artifact đã gặp ở Plan 028.

## 9. Rủi ro và giảm thiểu

- **Mô hình lịch cấp trường hay cấp lớp chưa được mô tả chi tiết**: dùng assumption cấp năm
  học/học kỳ trong plan; dừng và xin cập nhật nếu user yêu cầu lịch theo lớp.
- **Calendar update sau khi attendance đã tồn tại**: không xóa dữ liệu; chặn suy ra PRESENT,
  giữ exception và audit, đồng thời ghi rõ behavior trong test.
- **Dữ liệu lịch chưa cấu hình**: xem là invalid, không tự động coi là SCHOOL_DAY.
- **Tương thích Plan 028**: các fixture/test attendance phải được bổ sung calendar setup;
  API attendance giữ nguyên path và request contract.
- **Schema data model v2 đang dùng `session_no` nhưng code Plan 028 dùng `session_period`**:
  tiếp tục dùng `MORNING`/`AFTERNOON` theo contract mới nhất đã chốt ở Plan 028, không lưu
  ordinal.
- **Concurrency khi upsert**: unique constraint, transaction và lookup theo ngày/period;
  xử lý duplicate key theo convention hiện có nếu integration test phát hiện race.

## 10. Output dự kiến

- Có calendar foundation v2 lưu được ngày học, ngày nghỉ/ngày lễ và trạng thái từng buổi.
- Có API office cấu hình và API đọc lịch theo khoảng ngày.
- Attendance không tạo session và không suy ra `PRESENT` nếu buổi chưa được đánh dấu học.
- Có audit cho thay đổi calendar và giữ tương thích với attendance exception hiện tại.
- Unit/integration test và backend validation pass theo command thực tế.

## 11. Approval gate

Plan này đang ở trạng thái draft. Không sửa production code, test code, migration hoặc API
contract cho tới khi user phê duyệt rõ ràng bằng tin nhắn qua agent.
