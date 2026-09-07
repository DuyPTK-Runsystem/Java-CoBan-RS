# Dev Note 028: Attendance Session Foundation

## 1. Developer Plan liên quan và approval

- Developer Plan: `document/dev-impl-plan/be/attendance/028-attendance-session-foundation-2026-08-22.md`.
- Approval: user approved Plan 028 bằng tin nhắn `approve` trong phiên agent.
- Application-document version: `v2`.
- Trước khi code đã cập nhật:
  - `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`;
  - `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`.

## 2. Phạm vi đã triển khai

- Tạo module backend `attendance`.
- Tạo `attendance_session` theo lớp, học kỳ, ngày và buổi.
- Hỗ trợ hai buổi `MORNING` và `AFTERNOON`.
- Validate ngày điểm danh nằm trong học kỳ và lớp/học kỳ thuộc cùng năm học.
- Validate GVCN từ `Teacher.userId` và `homeroom_assignment` active theo ngày điểm danh.
- Xem danh sách học sinh trong buổi với trạng thái mặc định `PRESENT` khi không có exception.
- Tạo/cập nhật/xóa ngoại lệ điểm danh:
  - `ABSENT`;
  - `EXCUSED`;
  - `LATE`;
  - `EARLY_LEAVE`.
- Enforce mỗi học sinh tối đa một exception trong một buổi bằng unique constraint
  `attendance_record(session_id, student_id)`.
- Ghi audit log cho create/update/delete exception.
- Thêm unit test service/guard cho session, default `PRESENT`, upsert/delete exception,
  ngày ngoài học kỳ và quyền GVCN.

## 3. Files thay đổi theo nhóm

### Developer Plan documentation

- `document/dev-impl-plan/be/attendance/028-attendance-session-foundation-2026-08-22.md`:
  Developer Plan 028 đã được ghi ra file.
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`: thêm dòng Plan 028.
- `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`: thêm dòng Plan 028 và module folder `attendance/`.

### Backend migration

- `BE/BaiTap-RS/src/main/resources/db/migration/V6__create_attendance_session_and_record.sql`:
  tạo `attendance_session`, `attendance_record`, FK, check constraint, index và unique constraint.

### Backend source

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/controller/AttendanceController.java`:
  REST API `/api/v2/attendance-sessions`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceService.java`:
  orchestration transaction cho session, list students và exception mutation.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceGuard.java`:
  lookup và business guard cho class, semester, date, enrollment và GVCN scope.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceAuditService.java`:
  ghi audit `ATTENDANCE_EXCEPTION_CREATED`, `ATTENDANCE_EXCEPTION_UPDATED`,
  `ATTENDANCE_EXCEPTION_DELETED`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceMapper.java`:
  map entity sang response DTO, gồm default `PRESENT`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/repository/AttendanceSessionRepository.java`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/repository/AttendanceRecordRepository.java`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/repository/AttendanceEnrollmentRepository.java`:
  query riêng của attendance để tránh làm `StudentYearEnrollmentRepository` vượt PMD method-count.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/entity/AttendanceSession.java`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/entity/AttendanceRecord.java`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/entity/AttendanceSessionPeriod.java`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/entity/AttendanceExceptionStatus.java`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/DTOs/requests/ReqCreateAttendanceSessionDTO.java`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/DTOs/requests/ReqUpsertAttendanceExceptionDTO.java`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/DTOs/response/ResAttendanceSessionDTO.java`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/DTOs/response/ResAttendanceStudentDTO.java`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/domain/DTOs/response/ResAttendanceExceptionDTO.java`.

### Existing backend files

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/assignment/repository/HomeroomAssignmentRepository.java`:
  thêm query `existsActiveHomeroomAt(...)` cho GVCN scope theo ngày.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/enrollment/repository/StudentYearEnrollmentRepository.java`:
  ban đầu có thêm query attendance, sau validation đã rút lại và chuyển sang
  `AttendanceEnrollmentRepository` để giữ PMD pass.

### Tests

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceServiceTest.java`.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceGuardTest.java`.

## 4. Quyết định triển khai quan trọng

- Trạng thái exception dùng đúng yêu cầu Plan 028: `ABSENT`, `EXCUSED`, `LATE`, `EARLY_LEAVE`.
- `PRESENT` chỉ là trạng thái suy ra trong response, không lưu vào `attendance_record`.
- `AttendanceRecord` delete row khi xóa exception; audit giữ before-data immutable.
- Chỉ `TEACHER` gọi API; service vẫn kiểm tra GVCN thật bằng `Teacher.userId` và assignment.
- Không mở quyền `ADMIN`/`ACADEMIC_OFFICE` cho mutation trong Plan 028 vì yêu cầu chốt
  "GVCN chỉ thao tác lớp được phân công".
- Ngày học hợp lệ ở foundation được validate bằng khoảng ngày học kỳ và enum buổi; chưa tạo
  calendar table riêng cho ngày nghỉ/lễ.
- Tách `AttendanceMapper` và `AttendanceEnrollmentRepository` sau PMD để tránh suppress rule.

## 5. Validation thực tế

| Lệnh/kiểm tra | Kết quả | Bằng chứng |
|---|---|---|
| `./gradlew test --tests com.JavaTraining.BaiTap_RS.attendance.service.AttendanceServiceTest` | PASS | Focused test ban đầu pass và tạo `jacocoTestReport`. |
| `./gradlew test --tests com.JavaTraining.BaiTap_RS.attendance.service.*` | PASS | Focused attendance service/guard tests pass sau khi tách test. |
| `./gradlew checkstyleMain` | PASS | `BUILD SUCCESSFUL`; main Checkstyle pass. |
| `./gradlew checkstyleTest` | PASS | `BUILD SUCCESSFUL`; test Checkstyle pass. |
| `./gradlew pmdMain` | PASS | `BUILD SUCCESSFUL`; PMD main pass. |
| `./gradlew pmdTest` | PASS | `BUILD SUCCESSFUL`; PMD test pass. |
| `./gradlew test` | PASS | Chạy tuần tự sau `./gradlew --stop`; `BUILD SUCCESSFUL`, JaCoCo finalize chạy. |
| `./gradlew build` | PASS | Chạy tuần tự; `BUILD SUCCESSFUL`, 12 tasks up-to-date sau full test/check. |

### 5.1. JaCoCo snapshot cho attendance

- `AttendanceService`: line coverage `80.0%`, instruction coverage `80.4%`.
- `AttendanceMapper`: line/instruction coverage `100.0%`.
- `AttendanceSession`: line coverage `77.8%`.
- `AttendanceRecord`: line coverage `85.7%`.
- `AttendanceGuard`: line coverage `52.6%`.
- `AttendanceController`: line coverage `33.3%`; chưa có controller integration test trong Plan 028.
- `AttendanceAuditService`: line coverage `11.4%`; audit interaction được verify qua service mock,
  chưa test JSON serialization trực tiếp trong Plan 028.

Số vòng `code -> validation -> debug` có sửa code/test: `2`.

1. Vòng 1: Checkstyle/PMD fail do import thừa, service method count và repository method count;
   sửa bằng cách bỏ import, tách mapper và tạo attendance-specific repository.
2. Vòng 2: PMD test fail do test coupling/assertion/explicit type; tách test service/guard và
   chỉnh assertion theo rule.

Ghi chú validation: có hai lần `./gradlew test`/`build` fail do chạy song song gây lỗi file
`build/test-results/test/binary` (`EOFException`/`NoSuchFileException`). Sau khi `./gradlew --stop`
và chạy tuần tự, `test` và `build` đều pass.

## 6. Sai lệch so với Developer Plan

- Có thêm migration `V6__create_attendance_session_and_record.sql` vì codebase thực tế đang dùng Flyway.
- Có thêm `AttendanceMapper` và `AttendanceEnrollmentRepository` để đáp ứng PMD mà không suppress rule.
- `StudentYearEnrollmentRepository` cuối cùng không giữ query mới; query attendance được chuyển sang
  repository riêng trong module attendance.
- Chưa cập nhật Postman collection vì Plan 028 ghi out-of-scope và user chưa yêu cầu riêng.

## 7. Blocker và rủi ro còn lại

- Không còn blocker validation trong môi trường hiện tại.
- Rủi ro còn lại: chưa có calendar table production để loại ngày nghỉ/lễ hoặc buổi không học.
- Rủi ro còn lại: "xóa ngoại lệ" hiện là delete row có audit; nếu báo cáo chuyên cần sau này cần
  immutable correction record, cần plan soft-delete/correction riêng.
- Rủi ro còn lại: controller và audit JSON serialization chưa có test trực tiếp trong Plan 028.

## 8. Next steps

1. Nếu cần CRUD/report chuyên cần hoặc office adjustment (`FR-ATTENDANCE-005`), tạo plan tiếp theo.
2. Nếu cần ngày nghỉ/lễ chính xác, bổ sung calendar/school session table trước khi mở rộng báo cáo.
