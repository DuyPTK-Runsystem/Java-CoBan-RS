# Developer Plan 031: Student Attendance History, Read-Only

## 1. Trạng thái và phiên bản áp dụng

- Status: `Approved`.
- Application-document version: `v2`.
- Ngày lập plan: `2026-08-23`.
- Module: Backend `attendance`.
- Phụ thuộc: Plan `026`, `027`, `028`, `030`.
- Chỉ bắt đầu implementation sau khi user phê duyệt plan bằng tin nhắn qua agent.

Approval scope confirmed by user on `2026-08-24`: bổ sung foundation liên kết `Student.userId` với
`app_user.user_id` để phục vụ ownership lookup cho attendance history.

## 2. Mục tiêu

Bổ sung API read-only để học sinh xem lịch sử chuyên cần của chính mình.

Kết quả mong muốn:

- Hiển thị các buổi học hợp lệ theo khoảng thời gian.
- Suy ra `PRESENT` khi học sinh thuộc lớp tại thời điểm điểm danh, buổi học hợp lệ và không có exception.
- Hiển thị các exception `EXCUSED_ABSENCE`, `UNEXCUSED_ABSENCE`, `LATE` và `EARLY_LEAVE`.
- Không cho học sinh tạo, sửa hoặc xóa dữ liệu điểm danh.
- Không thay đổi API ghi điểm danh hiện tại của GVCN.

## 3. Requirement liên quan

- `FR-ATTENDANCE-006`: Học sinh xem lịch sử chuyên cần của mình.
- `FR-ATTENDANCE-007`: Thống kê số buổi vắng, đi trễ và về sớm.
- `BR-ATTENDANCE-005`, `BR-ATTENDANCE-006`: Chỉ suy ra `PRESENT` cho buổi học hợp lệ.
- `BR-ATTENDANCE-007`: Không tính điểm danh trước ngày học sinh vào lớp hoặc sau ngày rời lớp.
- `BR-ATTENDANCE-010`: Học sinh chỉ được xem dữ liệu của bản thân.
- `BR-ATTENDANCE-011`: Dùng số buổi học hợp lệ làm mẫu số báo cáo.
- `NFR-SECURITY-005`: User đã xác thực nhưng không đủ quyền phải nhận `403`.
- `NFR-USABILITY-005`: Dữ liệu lịch sử phải truy cập được nhưng không gây nhầm với dữ liệu hiện hành.

## 4. Phạm vi

### 4.1. In-scope

- API read-only lấy lịch sử chuyên cần của học sinh đang đăng nhập.
- Filter theo `academicYearId`, `semesterId`, `from`, `to`.
- Trả về từng ngày/buổi, trạng thái chuyên cần và exception nếu có.
- Trả về summary gồm tổng buổi hợp lệ, `PRESENT`, vắng có phép, vắng không phép, đi trễ và về sớm.
- Kiểm tra ownership từ authenticated user.
- Unit test service/guard và integration test cho API, authorization, query và trạng thái suy diễn.

### 4.2. Out-of-scope

- Frontend Vue/PrimeVue và Storybook.
- API báo cáo toàn trường, theo lớp, theo khối hoặc nhiều học sinh.
- Export CSV/PDF.
- Đơn xin nghỉ và quyền phụ huynh.
- Học sinh chỉnh sửa exception.
- Thay đổi `/api/v2/attendance-sessions/**` hoặc API legacy `/api/v1/**`.
- Thay đổi schema nếu các bảng hiện tại đã đủ để truy vấn.

## 5. Kiến trúc và flow hiện tại

Attendance hiện có:

- `AttendanceSession`: buổi điểm danh theo lớp, ngày và `MORNING`/`AFTERNOON`.
- `AttendanceRecord`: chỉ lưu exception, không lưu record `PRESENT` cho mọi học sinh.
- `AttendanceService`: phục vụ GVCN xem danh sách buổi và mutation exception.
- `AttendanceGuard`: kiểm tra lớp, kỳ học, enrollment tại thời điểm điểm danh và calendar validity.
- `CalendarValidityService`: xác định buổi có phải buổi học hợp lệ hay không.

Flow dự kiến:

```text
Authenticated STUDENT
        |
        v
AttendanceHistoryController
        |
        v
AttendanceHistoryService
        |
        +--> xác định Student tương ứng với current user
        +--> lấy enrollment history theo khoảng thời gian
        +--> lấy calendar sessions hợp lệ
        +--> lấy attendance sessions/records của student
        +--> suy ra PRESENT hoặc exception
        +--> tính summary
        |
        v
Read-only response
```

## 6. Phương án triển khai

### 6.1. API đề xuất

```http
GET /api/v2/attendance/students/me/history
```

Query parameters:

- `academicYearId`: optional.
- `semesterId`: optional.
- `from`: optional, ISO-8601 date.
- `to`: optional, ISO-8601 date.
- `page`: optional.
- `size`: optional, mặc định `10`.

Quyền dự kiến:

- `STUDENT`: chỉ đọc lịch sử của chính mình.
- `TEACHER`, `ACADEMIC_OFFICE`, `ADMIN`: chưa mở trong Plan 31.
- User không phải `STUDENT`: trả `403` khi gọi endpoint student-only.

Response dự kiến:

```json
{
  "items": [
    {
      "attendanceDate": "2026-08-24",
      "sessionPeriod": "MORNING",
      "classId": 12,
      "className": "7A1",
      "status": "PRESENT",
      "attendanceRecordId": null,
      "exceptionStatus": null,
      "note": null
    }
  ],
  "summary": {
    "validSessionCount": 20,
    "presentCount": 17,
    "excusedAbsenceCount": 1,
    "unexcusedAbsenceCount": 1,
    "lateCount": 1,
    "earlyLeaveCount": 0
  },
  "page": 0,
  "size": 10,
  "totalElements": 20,
  "totalPages": 2
}
```

### 6.2. Quy tắc trạng thái

1. Chỉ lấy calendar session có trạng thái `SCHEDULED`.
2. Chỉ tính học sinh nếu enrollment active tại ngày của session.
3. Nếu có `AttendanceRecord`, trả trạng thái exception tương ứng.
4. Nếu không có `AttendanceRecord`, suy ra `PRESENT`.
5. Không tạo bản ghi vật lý cho `PRESENT`.
6. Không nhận `studentId` tùy ý để tránh đọc dữ liệu học sinh khác.
7. Không có dữ liệu thì trả danh sách rỗng và summary bằng `0`.

### 6.3. Quyết định kỹ thuật

Dùng endpoint `/me` và lấy student identity từ authenticated user để đáp ứng trực tiếp
`BR-ATTENDANCE-010`. Cách này tránh việc thay `studentId` trên URL để đọc dữ liệu người khác.

Tách read service riêng thay vì mở rộng `AttendanceService` mutation hiện tại, giữ rõ ranh giới giữa
GVCN quản lý điểm danh theo buổi và học sinh đọc lịch sử cá nhân.

## 7. Phạm vi mã nguồn dự kiến

### 7.1. Tạo mới

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/controller/AttendanceHistoryController.java`:
  endpoint `GET /api/v2/attendance/students/me/history`.
- `.../attendance/service/AttendanceHistoryService.java`:
  xác định student, truy vấn dữ liệu, suy diễn trạng thái và tính summary.
- `.../attendance/domain/DTOs/request/ReqAttendanceHistoryQuery.java`:
  filter ngày, kỳ học, năm học và phân trang.
- `.../attendance/domain/DTOs/response/ResStudentAttendanceHistoryDTO.java`:
  response item, summary và page metadata.
- Repository/query methods cần thiết cho student identity, enrollment, calendar session,
  attendance session và attendance record.

### 7.2. Chỉnh sửa

- Student identity repository/entity nếu cần để xác định quan hệ `User -> Student`.
- Attendance repositories nếu query hiện tại chưa đủ.
- Security/authorization convention chỉ khi cần bổ sung `STUDENT` endpoint.
- Không chỉnh sửa flow mutation trong `AttendanceService`, trừ phần dùng chung thật sự cần thiết.

### 7.3. Database

- Ưu tiên không tạo migration mới.
- Chỉ tạo index/migration nếu query history thiếu index cần thiết và thay đổi đó được approval bổ sung.
- Không thay đổi dữ liệu lịch sử hoặc xóa record hiện tại.

## 8. Điểm cần xác nhận khi implementation

Code hiện đã có liên kết `Teacher.userId`, nhưng khảo sát hiện tại chưa xác nhận được liên kết tương đương
rõ ràng từ `Student` tới `User`.

Implementation phải xác minh một trong hai contract:

1. Có mapping `User -> Student` ở module/repository khác và reuse mapping đó.
2. Chưa có mapping học sinh-tài khoản; khi đó phần xác định `/me` là blocker contract và cần approval
   cho migration/identity design riêng.

Plan 31 không tự tạo liên kết identity mới nếu repository chưa có contract tương ứng.

## 9. Test và validation dự kiến

### 9.1. Unit test

- Student đọc lịch sử của chính mình thành công.
- User không phải `STUDENT` nhận `403`.
- Không đọc được dữ liệu của student khác.
- Không có exception và calendar session `SCHEDULED` thì status là `PRESENT`.
- Có exception thì trả đúng status và note.
- Không tính session trước ngày enrollment hoặc sau ngày enrollment kết thúc.
- Không tính ngày nghỉ hoặc calendar session `NO_CLASS`.
- Filter `academicYearId`, `semesterId`, `from`, `to`.
- Khoảng ngày không hợp lệ bị từ chối.
- Summary tính đúng từng loại trạng thái.
- Không có dữ liệu trả empty items và zero summary.
- Pagination có thứ tự kết quả xác định.

### 9.2. Integration test

- `GET /api/v2/attendance/students/me/history` với JWT học sinh.
- `401` khi thiếu hoặc sai token.
- `403` khi role không phù hợp theo contract.
- Không lộ dữ liệu student khác.
- Response không expose entity hoặc field audit không cần thiết.
- Kết hợp calendar/session/record đúng.

### 9.3. Backend validation

Chạy từ `BE/BaiTap-RS` theo cấu hình thực tế:

```text
./gradlew.bat test
./gradlew.bat checkstyleMain
./gradlew.bat checkstyleTest
./gradlew.bat pmdMain
./gradlew.bat pmdTest
./gradlew.bat build
```

Kiểm tra `./gradlew.bat tasks --all` trước khi kết luận về JaCoCo. Nếu `jacocoTestReport` không tồn tại,
báo `NOT RUN`, không coi là PASS.

## 10. Rủi ro và giảm thiểu

- **Chưa có mapping user-student rõ ràng:** dừng tại identity và báo cần quyết định; không dùng `studentId`
  từ request để bypass ownership.
- **Query nhiều bảng có thể chậm:** dùng query theo date range, server-side pagination và index hiện có.
- **Calendar/enrollment thay đổi sau attendance:** tính theo enrollment tại ngày session và chỉ dùng calendar
  `SCHEDULED`.
- **Nhầm `PRESENT` với record vật lý:** chỉ suy diễn `PRESENT`, không tạo `AttendanceRecord`.
- **Lộ audit/mutation metadata:** response chỉ trả field phục vụ lịch sử và không nhận mutation body.
- **Contract quyền chưa mở rộng cho teacher/office:** giữ Plan 31 ở student self-service.

Sau approval, rủi ro mapping identity được xử lý trong phạm vi foundation:

- `student.user_id` nullable để cho phép hồ sơ chưa được cấp tài khoản.
- Database enforce unique và FK tới `app_user(user_id)`.
- `StudentRepository.findByUserId(...)` phục vụ bước lookup ownership.
- Việc mở field `userId` vào request/response CRUD hoặc workflow cấp tài khoản vẫn nằm ngoài Plan 031.

## 11. Output dự kiến

- API v2 read-only cho học sinh xem lịch sử chuyên cần cá nhân.
- Trạng thái `PRESENT` đúng theo calendar và enrollment.
- Exception hiển thị đúng.
- Summary chuyên cần đúng theo khoảng thời gian.
- Không đọc được dữ liệu học sinh khác.
- Không ảnh hưởng API ghi điểm danh của GVCN.
- Có unit/integration test và báo cáo validation thực tế.
- Có Dev Note trong `document/dev-note/` sau implementation.

## 12. Approval gate

Plan này chỉ được coi là approved sau khi user xác nhận rõ ràng bằng tin nhắn qua agent.
Trước approval, không sửa production code, test code, migration hoặc API contract.
