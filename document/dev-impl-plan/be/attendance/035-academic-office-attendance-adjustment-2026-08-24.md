# Developer Plan 035: Academic Office Attendance Adjustment

## 1. Trạng thái và phiên bản áp dụng

- Status: `Approved`.
- Application-document version: `v2`.
- Ngày lập plan: `2026-08-24`.
- Module: Backend `attendance`.
- Phụ thuộc: Plan `026`, `027`, `028`, `030`, `031`, `033`.
- Phê duyệt: user phê duyệt triển khai qua tin nhắn agent ngày `2026-08-24`.

## 2. Mục tiêu

Bổ sung API cho phép giáo vụ (`ACADEMIC_OFFICE`) điều chỉnh ngoại lệ điểm danh trên bất kỳ lớp
nào, không bị giới hạn bởi homeroom assignment như GVCN.

Kết quả mong muốn:

- Giáo vụ tạo hoặc lấy buổi điểm danh (session) cho bất kỳ lớp nào thuộc bất kỳ học kỳ hợp lệ nào.
- Giáo vụ upsert hoặc xóa ngoại lệ điểm danh của học sinh trong session.
- Giáo vụ xem danh sách điểm danh theo session.
- Mọi thay đổi của giáo vụ được audit đầy đủ như GVCN (`NFR-AUDITABILITY-007`).
- GVCN vẫn hoạt động đúng qua API cũ — không thay đổi hành vi hiện tại.

## 3. Requirement liên quan

- `FR-ATTENDANCE-005`: Giáo vụ điều chỉnh dữ liệu khi cần.
- `BR-ATTENDANCE-009`: GVCN chỉ quản lý điểm danh lớp mình (giáo vụ không bị giới hạn này).
- `BR-AUTH-005`: Giáo vụ có quyền quản lý dữ liệu nền, khóa học kỳ và duyệt request sửa điểm.
- `NFR-SECURITY-003`: Quyền giáo viên dựa trên phân công thực tế (không áp dụng cho giáo vụ).
- `NFR-SECURITY-004`: `401` cho chưa xác thực.
- `NFR-SECURITY-005`: `403` cho đã xác thực nhưng không đủ quyền.
- `NFR-AUDITABILITY-007`: Điều chỉnh ngoại lệ điểm danh phải được audit.

Ma trận quyền (từ `07-AccessQualityAndAcceptanceModule.md`):

| Chức năng |    Giáo vụ     |     GVCN      |
| --------- | :------------: | :-----------: |
| Điểm danh | **Toàn quyền** | Nhập lớp mình |

## 4. Phạm vi

### 4.1. In-scope

- API POST tạo hoặc lấy session điểm danh — giáo vụ, không kiểm tra homeroom.
- API GET lấy danh sách học sinh trong session — giáo vụ.
- API PUT upsert ngoại lệ điểm danh — giáo vụ.
- API DELETE xóa ngoại lệ điểm danh — giáo vụ.
- Service `AcademicOfficeAttendanceService` — bỏ homeroom check, giữ toàn bộ validation còn lại.
- Audit đầy đủ với `AttendanceAuditService` — dùng chung.
- Validation: class/semester hợp lệ, cùng academic year, calendar validity, học sinh thuộc lớp.
- Unit test service, guard và integration test API + authorization.
- Backend validation theo workflow hiện hành.

### 4.2. Out-of-scope

- Frontend Vue/PrimeVue, Storybook và export CSV/PDF.
- Đơn xin nghỉ, workflow duyệt phép và parent access.
- Thay đổi API GVCN hiện tại (`/api/v2/attendance-sessions/**`).
- Thay đổi API student history (`/api/v2/attendance/students/me/history`).
- Báo cáo tổng hợp (`/api/v2/attendance/classes/{classId}/summary`) — đã có Plan 033.
- Thêm quyền cho `STUDENT` hoặc mở rộng sang role khác ngoài `ACADEMIC_OFFICE`.
- Migration/schema nếu bảng hiện tại đã đủ.

## 5. Kiến trúc và flow hiện tại

Backend là Spring Boot modular monolith. Attendance hiện có:

- `AttendanceService`: xử lý upsert/delete exception, kiểm tra homeroom assignment.
- `AttendanceGuard`: validate class, semester, calendar, homeroom assignment, học sinh trong lớp.
- `AttendanceAuditService`: ghi audit log cho create/update/delete record.
- `AttendanceController`: endpoint TEACHER, role `TEACHER`.

Điểm khác biệt chính: GVCN bị giới hạn qua `assertCurrentUserHomeroom()` trong `AttendanceGuard`.
Giáo vụ cần bỏ qua giới hạn đó nhưng vẫn phải validate class/semester/calendar/học sinh.

Flow dự kiến:

```text
Authenticated ACADEMIC_OFFICE
        |
        v
AcademicOfficeAttendanceController
        |
        v
AcademicOfficeAttendanceService
        |
        +---> validateClassSemesterAndDate (dùng lại AttendanceGuard, bỏ homeroom check)
        +---> assertStudentInClass nếu cần
        +---> tạo/lấy session, upsert/delete record
        +---> audit qua AttendanceAuditService (dùng chung)
        |
        v
Response
```

## 6. Phương án triển khai

### 6.1. API đề xuất

Nhóm dưới path riêng để phân biệt với GVCN endpoint:

```http
POST   /api/v2/office/attendance-sessions
GET    /api/v2/office/attendance-sessions/{sessionId}/students
PUT    /api/v2/office/attendance-sessions/{sessionId}/exceptions/{studentId}
DELETE /api/v2/office/attendance-sessions/{sessionId}/exceptions/{studentId}
```

Quyền:

- `ACADEMIC_OFFICE`: toàn quyền với mọi lớp, không bị giới hạn homeroom.
- Không có JWT: `401`.
- JWT nhưng không có role `ACADEMIC_OFFICE`: `403`.

Request/Response:

- Dùng lại `ReqCreateAttendanceSessionDTO`, `ReqUpsertAttendanceExceptionDTO`,
  `ResAttendanceSessionDTO`, `ResAttendanceStudentDTO`, `ResAttendanceExceptionDTO`
  đã có — không tạo DTO mới nếu contract đã phù hợp.
- Kiểm tra convention response wrapper tại implementation.

### 6.2. Quyết định kỹ thuật

- **Không tái sử dụng `AttendanceService`** trực tiếp vì service đó gọi `assertCurrentUserHomeroom()`;
  tách `AcademicOfficeAttendanceService` để giữ ranh giới rõ ràng và tránh conditional logic trong
  service GVCN.
- **Dùng lại `AttendanceGuard`** cho phần validate class/semester/calendar/học sinh (không gọi
  `assertCurrentUserHomeroom()`); không duplicate validation logic.
- **Dùng chung `AttendanceAuditService`** — action names thêm prefix `OFFICE_` để phân biệt trong
  audit log, ví dụ `OFFICE_ATTENDANCE_EXCEPTION_CREATED`.
- **Repository**: dùng lại `AttendanceSessionRepository` và `AttendanceRecordRepository` hiện có;
  không tạo repository riêng.
- **Validation calendar**: vẫn gọi `calendarValidityService.assertScheduled()` qua `AttendanceGuard`
  để đảm bảo giáo vụ không ghi exception vào buổi không học.
- **Học sinh trong lớp**: vẫn gọi `assertStudentInClass()` — giáo vụ không được ghi exception cho
  học sinh không thuộc lớp tại ngày điểm danh.

### 6.3. Quyết định đã chốt

- **[DECIDED-1]** Giáo vụ **chỉ được ghi exception vào buổi học hợp lệ** (`SCHEDULED`); buổi
  `NO_CLASS` hoặc ngày nghỉ không được ghi nhận. Giáo vụ vẫn phải đi qua `assertScheduled()`
  giống GVCN.
- **[DECIDED-2]** Audit action name dùng prefix `OFFICE_` để phân biệt nguồn ghi:
  `OFFICE_ATTENDANCE_EXCEPTION_CREATED`, `OFFICE_ATTENDANCE_EXCEPTION_UPDATED`,
  `OFFICE_ATTENDANCE_EXCEPTION_DELETED`.

## 7. Phạm vi mã nguồn dự kiến

### 7.1. Tạo mới

- `BE/.../attendance/controller/AcademicOfficeAttendanceController.java`:
  endpoint `ACADEMIC_OFFICE`, `@RequestMapping("/api/v2/office/attendance-sessions")`,
  `@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")`.
- `BE/.../attendance/service/AcademicOfficeAttendanceService.java`:
  orchestration không có homeroom check, delegate validation đến `AttendanceGuard`,
  delegate audit đến `AttendanceAuditService`, tái sử dụng `AttendanceMapper`.
- Unit test: `AcademicOfficeAttendanceServiceTest.java`.
- Integration test: `AcademicOfficeAttendanceControllerIT.java`.

### 7.2. Chỉnh sửa

- `AttendanceGuard`: chỉ chỉnh sửa nếu cần expose thêm method tiện ích dùng chung mà không phá
  vỡ contract hiện tại. Không thay đổi `assertCurrentUserHomeroom()`.
- `AttendanceRecordRepository`, `AttendanceSessionRepository`: bổ sung query tối thiểu nếu cần
  (backward-compatible).

### 7.3. Không dự kiến chỉnh sửa

- `AttendanceService`, `AttendanceController`, `AttendanceHistoryService`,
  `ClassAttendanceSummaryService`, `AttendanceAuditService`.
- Entity `AttendanceSession`, `AttendanceRecord`, `AttendanceExceptionStatus`.
- Migration/schema.
- API `/api/v2/attendance-sessions/**` và API student history.
- Frontend, Postman collection và infrastructure.

## 8. API / Database / Integration

- Không thay đổi database contract; `AttendanceSession` và `AttendanceRecord` là bảng dùng chung
  cho cả GVCN và giáo vụ.
- Không expose audit fields, `recordedBy`, password/token hoặc entity JPA trong response.
- API mới không ảnh hưởng backward compatibility của các endpoint hiện tại.
- Nếu cần index bổ sung, dừng và đề xuất delta riêng kèm bằng chứng; không tự thêm migration.

## 9. Test và validation dự kiến

### 9.1. Unit test

- Giáo vụ tạo session thành công cho lớp bất kỳ (không cần homeroom).
- Giáo vụ upsert exception thành công.
- Giáo vụ xóa exception thành công.
- Giáo vụ xem danh sách học sinh theo session.
- Class/semester khác academic year bị reject.
- Calendar `NO_CLASS` hoặc ngày nghỉ bị reject.
- Học sinh không thuộc lớp tại ngày điểm danh bị reject.
- Audit log được ghi sau create/update/delete.
- Session đã tồn tại thì trả về session cũ, không tạo mới.
- `assertCurrentUserHomeroom()` không được gọi trong service mới.

### 9.2. Integration test

- `POST /api/v2/office/attendance-sessions` với JWT `ACADEMIC_OFFICE` trả `201`.
- Anonymous trả `401`; role `TEACHER`, `STUDENT` trả `403`.
- Giáo vụ upsert exception cho học sinh thuộc lớp trả đúng response.
- Giáo vụ xóa exception trả `204`.
- Giáo vụ ghi exception lên lớp không phải homeroom của bất kỳ teacher nào — vẫn được chấp nhận.
- Học sinh không thuộc lớp tại ngày đó bị reject.
- Response không lộ entity/audit metadata.

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

- **Calendar guard cho giáo vụ**: đã chốt — giáo vụ phải tuân thủ `assertScheduled()`, không được
  ghi exception vào buổi `NO_CLASS` hoặc ngày nghỉ.
- **Audit action name**: đã chốt — dùng prefix `OFFICE_` (`OFFICE_ATTENDANCE_EXCEPTION_CREATED`,
  `OFFICE_ATTENDANCE_EXCEPTION_UPDATED`, `OFFICE_ATTENDANCE_EXCEPTION_DELETED`).
- **Role name**: codebase hiện dùng `ACADEMIC_OFFICE` (xem `AcademicYearController`,
  `GradeLevelController`); plan này giữ nguyên tên đó và thêm `ADMIN` theo pattern hiện hành.
- **DTO tái sử dụng**: nếu contract DTO hiện tại không phù hợp, sẽ tạo DTO riêng — dừng và đề xuất
  delta trước khi code.
- **Hiệu năng**: không có logic aggregation hay pagination phức tạp trong plan này;
  danh sách học sinh theo session đã có sẵn logic từ `AttendanceGuard.findActiveClassStudents()`.

## 11. Output dự kiến

- API v2 điều chỉnh điểm danh dành cho `ACADEMIC_OFFICE` — tách biệt với GVCN.
- Không thay đổi hành vi API GVCN hiện tại.
- Audit log đầy đủ cho mọi thao tác giáo vụ.
- Unit/integration tests và validation result thực tế.
- Dev Note trong `document/dev-note/be/attendance/` sau implementation.

## 12. Approval gate

Plan này chỉ được coi là approved sau khi user xác nhận rõ ràng bằng tin nhắn qua agent.
Trước approval, không sửa production code, test code, migration hoặc API contract.
