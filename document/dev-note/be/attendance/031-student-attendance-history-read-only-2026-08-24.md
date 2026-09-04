# Dev Note 031: Student Attendance History Read-Only

## 1. Developer Plan liên quan và approval

- Developer Plan: `document/dev-impl-plan/BE/attendance/031-student-attendance-history-read-only-2026-08-23.md`.
- Approval: user approved Plan 031 bằng tin nhắn xác nhận trong phiên agent vào ngày `2026-08-24` (phê duyệt foundation liên kết `Student.userId` với `app_user.user_id` qua Flyway `V8`).
- Application-document version: `v2`.

## 2. Phạm vi đã triển khai

- Bổ sung endpoint read-only `GET /api/v2/attendance/students/me/history` cho role `STUDENT`.
- Bổ sung query DTO `ReqAttendanceHistoryQuery` hỗ trợ filter theo `academicYearId`, `semesterId`, `from`, `to`, `page`, `size`.
- Bổ sung response DTO `ResStudentAttendanceHistoryDTO` bao gồm danh sách items (ngày, buổi `MORNING`/`AFTERNOON`, lớp học, trạng thái chuyên cần, exceptionStatus, note), thống kê `summary` (`validSessionCount`, `presentCount`, `excusedAbsenceCount`, `unexcusedAbsenceCount`, `lateCount`, `earlyLeaveCount`) và metadata phân trang.
- Bổ sung cơ chế lookup ownership học sinh thông qua `Student.userId` và migration `V8__link_student_to_app_user.sql` (cột `user_id` nullable, unique, foreign key tới `app_user(user_id)`).
- Truy vấn session điểm danh và lịch học calendar hợp lệ (`CalendarSessionStatus.SCHEDULED`) trong khoảng thời gian phân lớp (`StudentYearEnrollment`).
- Suy diễn tự động trạng thái `PRESENT` cho các buổi học hợp lệ không có exception record mà không tạo record vật lý trong database.
- Tách kiến trúc backend thành các component chuyên trách:
  - `AttendanceHistoryController`: REST controller endpoint phân quyền `@PreAuthorize("hasRole('STUDENT')")`.
  - `AttendanceHistoryService`: Orchestration service kiểm tra context, ownership, date range và tổng hợp kết quả.
  - `AttendanceHistoryItemCollector`: Thu thập danh sách buổi học theo enrollment và calendar.
  - `AttendanceHistoryCalendarReader`: Đọc dữ liệu lịch học và thông tin lớp học.
  - `AttendanceHistorySessionReader`: Đọc dữ liệu session điểm danh và record ngoại lệ.
  - `AttendanceHistoryDataReader`: Facade reader hợp nhất data access.
  - `AttendanceHistoryResponseMapper`: Mapping item, summary counts và page DTO.
- Bổ sung unit tests cho service, collector và integration test phân quyền `AttendanceHistoryAuthorizationIntegrationTest`.
- Cập nhật Postman Collection `document/postman/Java-CoBan.postman_collection.json` bổ sung folder `Calendar` và `Attendance` (bao gồm endpoint `GET /api/v2/attendance/students/me/history`) kèm test scripts tự động và runner script `scripts/run-postman-tests.ps1`.

## 3. Files thay đổi theo nhóm

### Documentation

- `document/dev-impl-plan/BE/attendance/031-student-attendance-history-read-only-2026-08-23.md`: hoàn thiện plan và chuyển trạng thái sang `Approved`.
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md` và `document/dev-impl-plan/BE/BE_DEV_PLAN_SUMMARY.md`: cập nhật Plan 031 `Approved`.
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md` và `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`: ghi nhận Dev Note 031 `Completed`.
- `document/dev-note/be/attendance/031-student-attendance-history-read-only-2026-08-24.md`: tài liệu Dev Note chi tiết.
- `document/postman/Java-CoBan.postman_collection.json`: cập nhật collection 8 folders với test script tự động.
- `scripts/run-postman-tests.ps1`: script runner kiểm thử toàn bộ hệ thống bằng Newman.

### Database Migration và Domain Entity

- `BE/BaiTap-RS/src/main/resources/db/migration/V8__link_student_to_app_user.sql`: migration thêm `user_id BIGINT NULL`, `uk_student_user`, `fk_student_user` tới `app_user(user_id)`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/entity/Student.java`: bổ sung trường `userId`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/repository/StudentRepository.java`: bổ sung phương thức `findByUserId(Long userId)`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/DTOs/requests/ReqAttendanceHistoryQuery.java`: DTO query filter và pagination.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/DTOs/response/ResStudentAttendanceHistoryDTO.java`: DTO response lịch sử và summary.

### Service, Controller và Repositories

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/controller/AttendanceHistoryController.java`: controller endpoint `GET /api/v2/attendance/students/me/history`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceHistoryService.java`: xử lý nghiệp vụ đọc lịch sử.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceHistoryItemCollector.java`: thu thập item lịch sử.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceHistoryCalendarReader.java`: đọc calendar và classes.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceHistorySessionReader.java`: đọc attendance sessions và records.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceHistoryDataReader.java`: facade reader dữ liệu.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceHistoryResponseMapper.java`: mapping item, summary counts và page DTO.

### Tests

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceHistoryServiceTest.java`: unit test cho service.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceHistoryItemCollectorTest.java`: unit test cho collector.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/attendance/controller/AttendanceHistoryAuthorizationIntegrationTest.java`: integration test phân quyền (403 cho teacher/office, 401 cho anonymous).
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/config/FlywayMigrationTest.java`: kiểm tra Flyway migration V8.

## 4. Quyết định triển khai quan trọng

- **Identity Mapping:** Khảo sát và bổ sung liên kết `Student.userId` với `app_user.user_id` (nullable unique FK) qua migration V8 để phục vụ ownership lookup an toàn thông qua `AuditContext.currentUserId()`, ngăn chặn việc giả mạo `studentId` trên URL.
- **Read-Only / Inferred Status:** Chỉ suy diễn trạng thái `PRESENT` trong response DTO cho các buổi học hợp lệ không có exception record, tuyệt đối không tạo bản ghi vật lý `AttendanceRecord` trong DB.
- **Tách biệt Controller & Service:** Tạo riêng `AttendanceHistoryController` và `AttendanceHistoryService` độc lập với luồng mutation của GVCN (`AttendanceController`/`AttendanceService`) để đảm bảo single responsibility và phân quyền rõ ràng.
- **Phân tách Reader & Mapper:** Tạo các component chuyên biệt `AttendanceHistoryCalendarReader`, `AttendanceHistorySessionReader`, `AttendanceHistoryItemCollector`, `AttendanceHistoryResponseMapper` giúp giảm coupling object <= 20 và tuân thủ PMD ruleset.

## 5. Validation thực tế

| Lệnh/kiểm tra | Kết quả | Ghi chú |
|---|---|---|
| `./gradlew test` | PASS | 95/95 unit và integration tests pass |
| `./gradlew checkstyleMain` | PASS | 0 vi phạm checkstyle trên main code |
| `./gradlew checkstyleTest` | PASS | 0 vi phạm checkstyle trên test code |
| `./gradlew pmdMain` | PASS | 0 vi phạm PMD trên main code |
| `./gradlew pmdTest` | PASS | 0 vi phạm PMD trên test code |
| `./gradlew build` | PASS | BUILD SUCCESSFUL |
| `jacocoTestReport` | EXECUTED | Báo cáo test coverage được sinh thành công trong task test |

Số vòng `code -> validation -> debug` đã thực hiện: `3`.

## 6. Sai lệch so với Developer Plan

- Đã bổ sung migration `V8__link_student_to_app_user.sql` sau khi được approval để giải quyết triệt để vấn đề identity mapping giữa `Student` và `app_user`.
- Tách thêm component `AttendanceHistoryResponseMapper`, `AttendanceHistoryItemCollector`, `AttendanceHistoryCalendarReader`, `AttendanceHistorySessionReader` để tối ưu hóa coupling và tuân thủ PMD.

## 7. Blocker và rủi ro còn lại

- Không còn blocker nào. Toàn bộ các tiêu chuẩn chất lượng (Unit test, Integration test, Checkstyle, PMD, Build) đều đã đạt 100% PASS.

## 8. Next steps

1. Triển khai giao diện Frontend cho học sinh xem lịch sử chuyên cần (khi có FE plan tương ứng).
2. Tích hợp chạy CI/CD pipeline và Newman automated testing qua script `scripts/run-postman-tests.ps1`.
