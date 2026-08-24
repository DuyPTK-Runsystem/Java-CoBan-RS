# Developer Plan 033: Class Attendance Summary

## 1. Trạng thái và phiên bản áp dụng

- Status: `Draft - awaiting user approval`.
- Status: `Approved`.
- Application-document version: `v2`.
- Ngày lập plan: `2026-08-24`.
- Module: Backend `attendance`.
- Phụ thuộc: Plan `026`, `027`, `028`, `030` và `031`.
- Chỉ bắt đầu implementation sau khi user phê duyệt plan bằng tin nhắn qua agent.
- Phê duyệt: user phê duyệt triển khai qua tin nhắn agent ngày `2026-08-24`.

## 2. Mục tiêu

Bổ sung API read-only để GVCN xem báo cáo chuyên cần của một lớp theo khoảng thời gian.

Kết quả mong muốn:

- Trả về thông tin lớp, khoảng thời gian và số buổi học hợp lệ.
- Trả về tổng hợp toàn lớp theo `PRESENT`, vắng có phép, vắng không phép, `LATE` và
  `EARLY_LEAVE`.
- Trả về thống kê từng học sinh trong lớp, có thể phân trang.
- Suy ra `PRESENT` từ buổi học `SCHEDULED` không có exception; không tạo record vật lý mới.
- Không tính buổi trước ngày học sinh vào lớp hoặc sau ngày rời lớp.
- GVCN chỉ đọc được lớp mình được phân công.

## 3. Requirement liên quan

- `FR-ATTENDANCE-007`: Thống kê số buổi vắng, đi trễ và về sớm.
- `FR-ATTENDANCE-008`: Báo cáo chuyên cần theo học sinh, lớp, khối và thời gian.
- `BR-ATTENDANCE-005`, `BR-ATTENDANCE-006`: Chỉ suy ra `PRESENT` cho buổi học hợp lệ.
- `BR-ATTENDANCE-007`: Không tính điểm danh ngoài thời gian enrollment.
- `BR-ATTENDANCE-009`: GVCN chỉ quản lý dữ liệu lớp mình.
- `BR-ATTENDANCE-011`: Báo cáo dùng số buổi học hợp lệ làm mẫu số.
- `BR-CALENDAR-002`, `BR-CALENDAR-003`: Chỉ dùng buổi `SCHEDULED`; ngày nghỉ/buổi không học
  không được tính.
- `NFR-SECURITY-005`: User đã xác thực nhưng không đủ quyền nhận `403`.

## 4. Phạm vi

### 4.1. In-scope

- API GET báo cáo chuyên cần của một `classId` theo `semesterId` và khoảng ngày.
- Kiểm tra class/semester cùng academic year và xác nhận current user là GVCN của lớp.
- Tính class summary và student summaries từ calendar, enrollment, attendance session và
  attendance exception.
- Phân trang danh sách học sinh, với thứ tự ổn định theo `studentCode`.
- Trả tỷ lệ có mặt ở dạng số buổi `PRESENT` trên số buổi hợp lệ mà học sinh được tính.
- Unit test service/readers/mapper và integration test API, authorization, query và boundary.
- Validation backend theo workflow hiện hành.

### 4.2. Out-of-scope

- Frontend Vue/PrimeVue, Storybook và export CSV/PDF.
- Báo cáo theo khối, toàn trường hoặc nhiều lớp trong một request.
- Dashboard realtime, cache hoặc materialized summary table.
- Giáo vụ/admin xem lớp chưa được phân công; quyền mở rộng này cần plan/approval riêng.
- Thay đổi flow tạo session, upsert/delete exception hoặc API student history của Plan 031.
- Đơn xin nghỉ, workflow duyệt phép và parent access.
- Thay đổi API legacy `/api/v1/**` hoặc migration nếu query/index hiện tại đáp ứng.

## 5. Kiến trúc và flow hiện tại

Backend là Spring Boot modular monolith. Attendance hiện có:

- `AttendanceSession`: session theo lớp, ngày và `MORNING`/`AFTERNOON`.
- `AttendanceRecord`: chỉ lưu exception, không lưu `PRESENT` cho mọi học sinh.
- `AttendanceGuard`: kiểm tra class, semester, enrollment, homeroom assignment và calendar validity.
- `CalendarValidityService`: xác định buổi `SCHEDULED`.
- `AttendanceHistory*`: các reader/mapper đã tách riêng cho read-only student history của Plan 031.

Flow dự kiến:

```text
Authenticated TEACHER
        |
        v
ClassAttendanceSummaryController
        |
        v
ClassAttendanceSummaryService
        |
        +--> validate class/semester/date range và homeroom ownership
        +--> lấy calendar sessions SCHEDULED trong khoảng thời gian
        +--> lấy enrollment hợp lệ của từng học sinh theo ngày session
        +--> lấy attendance exceptions theo class/session/student
        +--> suy ra PRESENT hoặc exception
        +--> aggregate class summary và student summaries
        +--> paginate student rows
        |
        v
Class attendance summary response
```

Ưu tiên tái sử dụng query/reader và quy tắc suy diễn đã được kiểm chứng ở Plan 031; không
đưa logic aggregation vào controller hoặc entity.

## 6. Phương án triển khai

### 6.1. API đề xuất

```http
GET /api/v2/attendance/classes/{classId}/summary
```

Query parameters:

- `semesterId`: bắt buộc.
- `from`: bắt buộc, ISO-8601 date.
- `to`: bắt buộc, ISO-8601 date.
- `page`: optional, mặc định `0`.
- `size`: optional, mặc định `20`.

Quyền:

- `TEACHER`: chỉ đọc khi teacher hiện tại là GVCN của `classId` trong phạm vi ngày yêu cầu.
- User thiếu JWT: `401`.
- User không có role `TEACHER` hoặc không phải GVCN lớp: `403`.

Response dự kiến:

```json
{
  "class": { "id": 12, "name": "7A1", "gradeLevelId": 7 },
  "semesterId": 3,
  "from": "2026-08-01",
  "to": "2026-08-31",
  "validSessionCount": 20,
  "summary": {
    "presentCount": 410,
    "excusedAbsenceCount": 8,
    "unexcusedAbsenceCount": 5,
    "lateCount": 12,
    "earlyLeaveCount": 3
  },
  "students": [
    {
      "studentId": 101,
      "studentCode": "STU1234567",
      "fullName": "Nguyen Van A",
      "validSessionCount": 20,
      "presentCount": 18,
      "excusedAbsenceCount": 1,
      "unexcusedAbsenceCount": 0,
      "lateCount": 1,
      "earlyLeaveCount": 0,
      "attendanceRate": 0.9
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 25,
  "totalPages": 2
}
```

Tên wrapper response phải tuân theo response convention thực tế của project khi implementation.
`attendanceRate` chỉ là tỷ lệ `presentCount / validSessionCount`, không phải điểm hay xếp loại.

### 6.2. Quy tắc dữ liệu và aggregation

1. Validate `from <= to`; reject range không hợp lệ và filter ngoài phạm vi semester.
2. Chỉ lấy calendar session có status `SCHEDULED` thuộc semester, trong `[from, to]`.
3. Với mỗi học sinh, chỉ tính session nếu enrollment active tại ngày session.
4. Có exception thì đếm đúng loại exception; không có exception thì đếm `PRESENT` suy diễn.
5. `validSessionCount` của từng học sinh là mẫu số riêng sau khi áp dụng enrollment boundary;
   `class.validSessionCount` là số session `SCHEDULED` của lớp trong khoảng lọc.
6. Không tính ngày nghỉ, `NO_CLASS`, session không thuộc semester hoặc học sinh ngoài enrollment.
7. Không có học sinh hoặc không có session hợp lệ thì trả danh sách phù hợp và các count bằng `0`;
   không trả lỗi chỉ vì dữ liệu rỗng.
8. Kết quả phải có thứ tự xác định theo `studentCode`, sau đó `studentId` nếu cần tie-break.

### 6.3. Quyết định kỹ thuật

- Dùng endpoint theo `classId` vì đây là báo cáo lớp của GVCN; không nhận danh sách student id
  từ client để tránh mở rộng quyền đọc ngoài class scope.
- Tách `ClassAttendanceSummaryService` khỏi `AttendanceService` mutation và khỏi
  `AttendanceHistoryService` student self-service để giữ ranh giới use case.
- Tính on-demand từ dữ liệu chuẩn, không tạo bảng summary hay ghi lại `PRESENT`; trade-off là
  query/aggregation có thể nặng hơn khi range lớn, được giảm bằng filter date, batch query và
  pagination student rows.
- Nếu repository hiện tại không hỗ trợ batch query theo class/date, chỉ bổ sung query projection
  tối thiểu trong attendance/enrollment/calendar; không refactor repository diện rộng.

## 7. Phạm vi mã nguồn dự kiến

### 7.1. Tạo mới

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/controller/ClassAttendanceSummaryController.java`:
  endpoint và authorization.
- `.../attendance/service/ClassAttendanceSummaryService.java`:
  orchestration, validation và aggregation.
- `.../attendance/domain/DTOs/requests/ReqClassAttendanceSummaryQuery.java`:
  `semesterId`, `from`, `to`, `page`, `size`.
- `.../attendance/domain/DTOs/response/ResClassAttendanceSummaryDTO.java`:
  class metadata, class summary, student rows và page metadata.
- Các reader/mapper chuyên trách chỉ khi cần để giữ coupling thấp và tái sử dụng được query
  hiện có.
- Unit/integration test tương ứng dưới `BE/BaiTap-RS/src/test/java/.../attendance/`.

### 7.2. Chỉnh sửa

- `AttendanceGuard` hoặc assignment lookup chỉ khi cần expose validation dùng chung cho class
  summary; không thay đổi semantics của mutation API.
- `AttendanceSessionRepository`, `AttendanceRecordRepository`, calendar/enrollment repository
  để bổ sung batch query tối thiểu nếu query hiện tại chưa đủ.
- DTO/mapper dùng chung chỉ khi có backward-compatible reuse rõ ràng.

### 7.3. Không dự kiến chỉnh sửa

- Migration/schema production nếu không có bằng chứng về thiếu index hoặc thiếu cột.
- API `/api/v2/attendance-sessions/**` và `/api/v2/attendance/students/me/history`.
- Frontend, Postman collection và infrastructure.

## 8. API / Database / Integration

- Không đổi database contract trong baseline; `AttendanceSession`, `AttendanceRecord`, calendar
  và enrollment là nguồn dữ liệu duy nhất.
- Không expose audit fields, `recordedBy`, password/token hoặc entity JPA trong response.
- Nếu cần index cho truy vấn class/date hoặc session lookup, phải dừng và đề xuất delta riêng kèm
  bằng chứng query plan; không tự thêm migration ngoài approval.
- API mới không ảnh hưởng backward compatibility của các endpoint hiện tại.

## 9. Test và validation dự kiến

### 9.1. Unit test

- GVCN truy vấn lớp được phân công thành công.
- Teacher không phải GVCN nhận lỗi authorization.
- `from > to`, `semesterId` không tồn tại, class/semester khác academic year bị reject.
- Calendar chỉ có `SCHEDULED`; `NO_CLASS`/ngày nghỉ không được tính.
- Không có exception thì suy ra `PRESENT`, không gọi save/delete record.
- Mỗi exception được aggregate đúng bucket.
- Enrollment bắt đầu/kết thúc tại boundary ngày session được xử lý đúng.
- Học sinh không active hoặc ngoài class không xuất hiện trong summary.
- Class summary và student summary có mẫu số đúng; không chia cho zero.
- Khoảng không có session/học sinh trả zero summary và page rỗng.
- Pagination và sort `studentCode` ổn định.

### 9.2. Integration test

- `GET /api/v2/attendance/classes/{classId}/summary` với JWT GVCN trả `200` và đúng response.
- Anonymous trả `401`; role không phù hợp trả `403`.
- GVCN lớp A không đọc được lớp B.
- Kết hợp calendar, enrollment, session và exception trả aggregation đúng.
- Response không lộ entity/audit metadata và không tạo thêm `PRESENT` record.

### 9.3. Backend validation

Chạy từ `BE/BaiTap-RS` theo `backend-validation`:

```text
./gradlew.bat test
./gradlew.bat checkstyleMain
./gradlew.bat checkstyleTest
./gradlew.bat pmdMain
./gradlew.bat pmdTest
./gradlew.bat build
```

Kiểm tra task JaCoCo bằng `./gradlew.bat tasks --all`; nếu không có task report phù hợp thì ghi
`NOT RUN`, không tự đặt coverage threshold.

## 10. Rủi ro, assumption và điểm cần xác nhận

- **Phạm vi role:** Plan này giả định báo cáo lớp dành cho GVCN; nếu user muốn mở cho giáo vụ/admin
  hoặc giáo viên bộ môn, cần bổ sung contract authorization trước implementation.
- **Lớp có thay đổi GVCN trong khoảng ngày:** guard phải kiểm tra assignment theo ngày, không chỉ
  assignment hiện tại.
- **Enrollment khác ngày giữa học sinh:** dùng mẫu số riêng từng học sinh để tuân thủ `BR-ATTENDANCE-007`;
  class valid session count vẫn là count theo lớp.
- **Hiệu năng:** on-demand aggregation có thể chậm với range lớn; giới hạn page/size và batch query,
  chưa thêm cache/materialized table trong plan này.
- **Response naming:** tài liệu data model dùng `PRESENT/ABSENT/EXCUSED` còn code/Plan 028/031
  dùng `EXCUSED_ABSENCE/UNEXCUSED_ABSENCE/LATE/EARLY_LEAVE`; implementation phải giữ enum/contract
  đang chạy trong codebase và không tự đổi status cũ.
- **Endpoint path/response wrapper:** cần đối chiếu convention tại implementation; nếu khác đề xuất
  delta trước khi code.

## 11. Output dự kiến

- API v2 báo cáo chuyên cần read-only theo lớp và thời gian.
- Class summary và per-student summary đúng theo calendar/enrollment/exception.
- Authorization không cho đọc lớp ngoài phạm vi GVCN.
- Không ảnh hưởng flow mutation attendance hoặc student history.
- Có unit/integration tests và validation result thực tế.
- Có Dev Note trong `document/dev-note/be/attendance/` sau implementation.

## 12. Approval gate

Plan này chỉ được coi là approved sau khi user xác nhận rõ ràng bằng tin nhắn qua agent.
Trước approval, không sửa production code, test code, migration hoặc API contract.
