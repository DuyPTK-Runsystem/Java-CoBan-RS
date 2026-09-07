# Dev Note 033: Class Attendance Summary

## 1. Developer Plan liên quan và approval

- Developer Plan: `document/dev-impl-plan/be/attendance/033-class-attendance-summary-2026-08-24.md`.
- Approval: user approved Plan 033 qua tin nhắn agent vào ngày `2026-08-24`.
- Application-document version: `v2`.

## 2. Phạm vi đã triển khai

- Bổ sung endpoint read-only `GET /api/v2/attendance/classes/{classId}/summary` cho role `TEACHER` (GVCN).
- Bổ sung query DTO `ReqClassAttendanceSummaryQuery` hỗ trợ query parameters: `semesterId`, `from`, `to`, `page`, `size`.
- Bổ sung response DTO `ResClassAttendanceSummaryDTO` với các cấu trúc:
  - `ClassInfo`: `id`, `name`, `gradeLevelName`, `academicYearName`.
  - `Summary`: tổng hợp toàn lớp (`presentCount`, `excusedAbsenceCount`, `unexcusedAbsenceCount`, `lateCount`, `earlyLeaveCount`).
  - `StudentSummary`: chi tiết từng học sinh (`studentId`, `studentCode`, `studentName`, `validSessionCount`, `presentCount`, `excusedAbsenceCount`, `unexcusedAbsenceCount`, `lateCount`, `earlyLeaveCount`, `attendanceRate`).
  - Phân trang học sinh: `page`, `size`, `totalElements`, `totalPages`.
- Suy diễn tự động `PRESENT` cho các buổi học hợp lệ (`SCHEDULED`) nằm trong khoảng thời gian học sinh đang học tại lớp (`StudentYearEnrollment`) mà không có record ngoại lệ.
- Không tạo bản ghi vật lý nào trong cơ sở dữ liệu (read-only query).
- Kiểm tra phân quyền GVCN chặt chẽ: giáo viên hiện tại (`AuditContext.currentUserId()`) phải có phân công chủ nhiệm hoạt động (`HomeroomAssignment`) đối với lớp trong khoảng thời gian truy vấn (`from` đến `to`).
- Kiểm tra ràng buộc học kỳ: lớp và học kỳ phải thuộc cùng một năm học.
- Tách kiến trúc backend theo SRP và tuân thủ PMD Coupling <= 20:
  - `ClassAttendanceSummaryController`: REST controller endpoint phân quyền `@PreAuthorize("hasRole('TEACHER')")`.
  - `ClassAttendanceSummaryService`: Service điều phối xác thực guard, date validation, collector và mapper.
  - `ClassAttendanceSummaryCalendarReader`: Đọc danh sách buổi học hợp lệ từ calendar slots (`CalendarDay`, `CalendarSession`).
  - `ClassAttendanceSummarySessionReader`: Đọc attendance sessions và records ngoại lệ, trả Map status tra cứu nhanh.
  - `ClassAttendanceSummaryCollector`: Thu thập danh sách học sinh theo enrollment boundary, tính toán thống kê cá nhân và tổng hợp lớp.
  - `ClassAttendanceSummaryResponseMapper`: Phân trang in-memory và mapping sang `ResClassAttendanceSummaryDTO`.
  - Mở rộng repository: `HomeroomAssignmentRepository.existsActiveHomeroomBetween`, `AttendanceEnrollmentRepository.findActiveEnrollmentsInClassAt`, `AttendanceRecordRepository.findAllBySessionIdIn`, `CalendarSessionRepository.findAllByCalendarDayIdIn`.
- Bổ sung unit tests và integration tests:
  - `ClassAttendanceSummaryServiceTest`: 5 unit tests kiểm tra service orchestration, validation lỗi, forbidden, not found, conflict.
  - `ClassAttendanceSummaryCollectorTest`: 3 unit tests kiểm tra aggregate summary, enrollment date boundaries, empty students.
  - `ClassAttendanceSummaryAuthorizationIntegrationTest`: 3 integration tests kiểm tra phân quyền (403 cho STUDENT, ACADEMIC_OFFICE; 401 cho Anonymous).
  - `AttendanceGuardTest`: 2 unit tests kiểm tra phân quyền GVCN.

## 3. Files thay đổi theo nhóm

### Documentation

- `document/dev-impl-plan/be/attendance/033-class-attendance-summary-2026-08-24.md`: chuyển trạng thái `Approved`.
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`: cập nhật Plan 033 `Approved`.
- `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`: cập nhật Plan 033 `Approved`.
- `document/dev-note/be/attendance/033-class-attendance-summary-2026-08-24.md`: tạo Dev Note chi tiết.
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`: cập nhật Dev Note 033 `Completed`.
- `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`: cập nhật Dev Note 033 `Completed`.

### DTOs và Repositories

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/DTOs/requests/ReqClassAttendanceSummaryQuery.java`: Query DTO với validation.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/DTOs/response/ResClassAttendanceSummaryDTO.java`: Response DTO phân trang kèm summary.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/assignment/repository/HomeroomAssignmentRepository.java`: thêm `existsActiveHomeroomBetween`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/repository/AttendanceEnrollmentRepository.java`: thêm `findActiveEnrollmentsInClassAt`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/repository/AttendanceRecordRepository.java`: thêm `findAllBySessionIdIn`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/calendar/repository/CalendarSessionRepository.java`: thêm `findAllByCalendarDayIdIn`.

### Service, Controller và Guard

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/controller/ClassAttendanceSummaryController.java`: Controller endpoint.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/ClassAttendanceSummaryService.java`: Service chính.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/ClassAttendanceSummaryCalendarReader.java`: Reader đọc lịch học.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/ClassAttendanceSummarySessionReader.java`: Reader đọc session & exception record.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/ClassAttendanceSummaryCollector.java`: Collector tổng hợp số liệu.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/ClassAttendanceSummaryResponseMapper.java`: Mapper và pagination.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceGuard.java`: bổ sung `validateClassAndSemester` và `validateCurrentUserHomeroomInRange`.

### Tests

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/attendance/controller/ClassAttendanceSummaryAuthorizationIntegrationTest.java`: Integration tests phân quyền.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/attendance/service/ClassAttendanceSummaryServiceTest.java`: Unit tests cho Service.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/attendance/service/ClassAttendanceSummaryCollectorTest.java`: Unit tests cho Collector.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceGuardTest.java`: Cập nhật Unit tests cho Guard.

## 4. Quyết định triển khai quan trọng

- **Read-Only Inferred Attendance:** Không tạo bản ghi `PRESENT` vật lý vào database. Trạng thái `PRESENT` được suy ra bằng cách so khớp giữa các buổi học hợp lệ (`SCHEDULED`) trong lịch học với các bản ghi ngoại lệ (`EXCUSED`, `ABSENT`, `LATE`, `EARLY_LEAVE`) ghi nhận trong `attendance_record`.
- **Enrollment Date Boundary:** Mỗi học sinh chỉ được tính số buổi học hợp lệ (`validSessionCount`) trong phạm vi ngày học sinh thực sự theo học tại lớp (`enrolledAt` đến `completedAt` nếu có).
- **Homeroom Date-Range Authorization:** Giáo viên truy vấn phải có phân công chủ nhiệm lớp (`HomeroomAssignment`) hoạt động và giao thoa với khoảng ngày truy vấn (`from` đến `to`).
- **Phân tách Reader, Collector, Mapper:** Giữ các class nhỏ gọn, single responsibility, giảm thiểu class coupling <= 20 và số method <= 10 để tuân thủ nghiêm ngặt PMD ruleset (`CouplingBetweenObjects`, `TooManyMethods`).

## 5. Validation thực tế

| Lệnh/kiểm tra              | Kết quả  | Ghi chú                                                    |
| -------------------------- | -------- | ---------------------------------------------------------- |
| `./gradlew test`           | PASS     | 114/114 unit và integration tests pass (100% success rate) |
| `./gradlew checkstyleMain` | PASS     | 0 vi phạm checkstyle trên main code                        |
| `./gradlew checkstyleTest` | PASS     | 0 vi phạm checkstyle trên test code                        |
| `./gradlew pmdMain`        | PASS     | 0 vi phạm PMD trên main code                               |
| `./gradlew pmdTest`        | PASS     | 0 vi phạm PMD trên test code                               |
| `./gradlew build`          | PASS     | BUILD SUCCESSFUL                                           |
| `jacocoTestReport`         | EXECUTED | Báo cáo test coverage được sinh thành công                 |

Số vòng `code -> validation -> debug` đã thực hiện: `4`.

## 6. Sai lệch so với Developer Plan

- Không có sai lệch về API contract hoặc logic nghiệp vụ.
- Về mặt cấu trúc, tách thêm các component hỗ trợ (`ClassAttendanceSummaryCalendarReader`, `ClassAttendanceSummarySessionReader`, `ClassAttendanceSummaryResponseMapper`) để tuân thủ triệt để quy chuẩn Clean Code và PMD metrics.

## 7. Blocker và rủi ro còn lại

- Không còn blocker nào. Toàn bộ các tiêu chuẩn kiểm thử và phân tích tĩnh (Unit test, Integration test, Checkstyle, PMD, Build) đều đã đạt 100% PASS.

## 8. Next steps

1. Triển khai giao diện Frontend cho GVCN xem báo cáo chuyên cần lớp học theo khoảng ngày và học kỳ (khi có FE plan tương ứng).
2. Bổ sung các test collection Newman/Postman mở rộng nếu cần thiết.

