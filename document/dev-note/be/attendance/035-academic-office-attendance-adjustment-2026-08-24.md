# Dev Note 035: Academic Office Attendance Adjustment

## 1. Developer Plan liên quan và approval

- Developer Plan: `document/dev-impl-plan/be/attendance/035-academic-office-attendance-adjustment-2026-08-24.md`.
- Approval: user approved Plan 035 qua tin nhắn agent vào ngày `2026-08-24`.
- Application-document version: `v2`.

## 2. Phạm vi đã triển khai

- Bổ sung nhóm API dành riêng cho `ACADEMIC_OFFICE` và `ADMIN` để điều chỉnh ngoại lệ điểm danh trên bất kỳ lớp nào:
  - `POST /api/v2/office/attendance-sessions`: Tạo hoặc lấy buổi điểm danh.
  - `GET /api/v2/office/attendance-sessions/{sessionId}/students`: Lấy danh sách điểm danh của học sinh trong buổi học.
  - `PUT /api/v2/office/attendance-sessions/{sessionId}/exceptions/{studentId}`: Thêm mới hoặc cập nhật ngoại lệ điểm danh.
  - `DELETE /api/v2/office/attendance-sessions/{sessionId}/exceptions/{studentId}`: Xóa ngoại lệ điểm danh.
- Tách riêng `AcademicOfficeAttendanceService` và `AcademicOfficeAttendanceController`:
  - Bỏ qua kiểm tra homeroom assignment (`assertCurrentUserHomeroom`), cho phép giáo vụ thao tác toàn quyền trên mọi lớp học.
  - Vẫn bảo toàn toàn bộ quy tắc nghiệp vụ: kiểm tra lớp và học kỳ cùng năm học, kiểm tra ngày điểm danh nằm trong học kỳ, kiểm tra lịch học hợp lệ (`SCHEDULED` qua `calendarValidityService.assertScheduled`), kiểm tra học sinh đang active trong lớp tại ngày điểm danh.
- Ghi nhận Audit Log đầy đủ theo quy chuẩn:
  - `OFFICE_ATTENDANCE_EXCEPTION_CREATED`: Ghi nhận khi giáo vụ thêm ngoại lệ mới.
  - `OFFICE_ATTENDANCE_EXCEPTION_UPDATED`: Ghi nhận khi giáo vụ cập nhật ngoại lệ.
  - `OFFICE_ATTENDANCE_EXCEPTION_DELETED`: Ghi nhận khi giáo vụ xóa ngoại lệ.
- Tái sử dụng các DTO, Mapper, Repository và Guard hiện có mà không gây ảnh hưởng tới API GVCN (`/api/v2/attendance-sessions/**`).
- Bổ sung unit tests và integration tests:
  - `AcademicOfficeAttendanceServiceTest`: 4 unit tests kiểm tra orchestration, mapping, audit log và repository interactions.
  - `AcademicOfficeAttendanceAuthorizationIntegrationTest`: 7 integration tests kiểm tra phân quyền truy cập (cho phép `ACADEMIC_OFFICE`, `ADMIN`; từ chối `TEACHER` [403], `STUDENT` [403], Anonymous [401]).

## 3. Files thay đổi theo nhóm

### Documentation

- `document/dev-impl-plan/be/attendance/035-academic-office-attendance-adjustment-2026-08-24.md`: chuyển trạng thái `Approved`.
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`: cập nhật Plan 035 `Approved`.
- `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`: cập nhật Plan 035 `Approved`.
- `document/dev-note/be/attendance/035-academic-office-attendance-adjustment-2026-08-24.md`: tạo Dev Note chi tiết.
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`: cập nhật Dev Note 035 `Completed`.
- `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`: cập nhật Dev Note 035 `Completed`.

### Controller và Service

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/controller/AcademicOfficeAttendanceController.java`: Controller endpoint phân quyền `ACADEMIC_OFFICE` và `ADMIN`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/AcademicOfficeAttendanceService.java`: Service điều phối và ghi audit log cho giáo vụ.

### Tests

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/attendance/service/AcademicOfficeAttendanceServiceTest.java`: Unit tests cho Service.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/attendance/controller/AcademicOfficeAttendanceAuthorizationIntegrationTest.java`: Integration tests phân quyền.

## 4. Quyết định triển khai quan trọng

- **Tách riêng Service và Controller cho Giáo vụ:** Không chèn cờ điều kiện vào `AttendanceService` cũ của GVCN để tránh phá vỡ SRP và giữ code clean, dễ bảo trì.
- **Duy trì Calendar Validity:** Giáo vụ chỉ được ghi nhận ngoại lệ vào buổi học hợp lệ (`SCHEDULED`); không cho phép ghi exception vào ngày nghỉ hoặc buổi `NO_CLASS`.
- **Phân biệt Audit Action Name:** Sử dụng prefix `OFFICE_` (`OFFICE_ATTENDANCE_EXCEPTION_*`) để dễ dàng truy vết và phân biệt nguồn thay đổi giữa GVCN và Giáo vụ trong `audit_log`.

## 5. Validation thực tế

| Lệnh/kiểm tra              | Kết quả  | Ghi chú                                                    |
| -------------------------- | -------- | ---------------------------------------------------------- |
| `./gradlew test`           | PASS     | Tất cả unit và integration tests pass                      |
| `./gradlew checkstyleMain` | PASS     | 0 vi phạm checkstyle trên main code                        |
| `./gradlew checkstyleTest` | PASS     | 0 vi phạm checkstyle trên test code                        |
| `./gradlew pmdMain`        | PASS     | 0 vi phạm PMD trên main code                               |
| `./gradlew pmdTest`        | PASS     | 0 vi phạm PMD trên test code                               |
| `./gradlew build`          | PASS     | BUILD SUCCESSFUL                                           |
| `jacocoTestReport`         | EXECUTED | Báo cáo test coverage được sinh thành công                 |

Số vòng `code -> validation -> debug` đã thực hiện: `2`.

## 6. Sai lệch so với Developer Plan

- Không có sai lệch về API contract hoặc logic nghiệp vụ.

## 7. Blocker và rủi ro còn lại

- Không còn blocker nào. Toàn bộ các tiêu chuẩn kiểm thử và phân tích tĩnh (Unit test, Integration test, Checkstyle, PMD, Build) đều đã đạt 100% PASS.

## 8. Next steps

1. Bổ sung giao diện Frontend cho Giáo vụ điều chỉnh điểm danh (khi có FE plan tương ứng).
