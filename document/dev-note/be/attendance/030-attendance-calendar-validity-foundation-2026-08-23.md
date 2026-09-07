# Dev Note 030: Attendance Calendar Validity Foundation

## 1. Developer Plan liên quan và approval

- Developer Plan: `document/dev-impl-plan/BE/attendance/030-attendance-calendar-validity-foundation-2026-08-23.md`.
- Approval: user approved Plan 030 bằng tin nhắn `approve` trong phiên agent.
- Application-document version: `v2`.

## 2. Phạm vi đã triển khai

- Tạo calendar module backend cho ngày lịch và session lịch.
- Hỗ trợ `SCHOOL_DAY`, `WEEKEND`, `HOLIDAY`, `NO_CLASS`.
- Hỗ trợ hai period `MORNING`, `AFTERNOON` và trạng thái session `SCHEDULED`, `NO_CLASS`.
- Validate calendar thuộc academic year/semester và ngày nằm trong cả hai khoảng thời gian.
- Cấu hình và đọc calendar qua `/api/v2/calendar`.
- Office mutation bằng `ADMIN`/`ACADEMIC_OFFICE`; read mở cho `ADMIN`, `ACADEMIC_OFFICE`,
  `TEACHER`, `STUDENT`.
- Attendance chỉ tạo session khi calendar session là `SCHEDULED` trên `SCHOOL_DAY`.
- Ghi audit before/after cho calendar day và calendar session.
- Bổ sung unit test calendar validity/upsert và cập nhật attendance guard test.

## 3. Files thay đổi theo nhóm

### Documentation

- `document/dev-impl-plan/BE/attendance/030-attendance-calendar-validity-foundation-2026-08-23.md`:
  chuyển trạng thái plan sang `Approved`.
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md` và
  `document/dev-impl-plan/BE/BE_DEV_PLAN_SUMMARY.md`: ghi Plan 030.
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md` và
  `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`: ghi Dev Note 030.

### Migration và domain

- `BE/BaiTap-RS/src/main/resources/db/migration/V7__create_calendar_day_and_session.sql`:
  tạo `calendar_day`, `calendar_session`, FK, check, index và unique constraint.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/calendar/domain/`:
  entity, enum và request/response DTO cho calendar.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/calendar/repository/`:
  repository lookup validity, upsert và range query.

### Service và API

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/calendar/service/CalendarService.java`:
  transaction, range read, upsert và validity lookup.
- `.../calendar/service/CalendarSessionService.java` và `CalendarValidator.java`:
  tách session orchestration và business validation để đáp ứng PMD.
- `.../calendar/service/CalendarAuditService.java` và `CalendarMapper.java`:
  audit before/after và response mapping.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/calendar/controller/CalendarController.java`:
  API cấu hình/đọc calendar.
- `attendance/service/AttendanceGuard.java` và `AttendanceService.java`:
  tích hợp calendar validity vào flow tạo attendance session.

### Tests

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/calendar/service/CalendarServiceTest.java`.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceGuardTest.java`.

## 4. Quyết định triển khai quan trọng

- Calendar được mô hình hóa ở cấp academic year/semester, chưa gắn theo từng lớp.
- Không có calendar entry hoặc session không phải `SCHEDULED` được xem là invalid; không tự
  suy ra `SCHOOL_DAY` mặc định.
- Upsert không xóa session cũ; period bị bỏ khỏi request được chuyển thành `NO_CLASS`.
- Giữ attendance history khi calendar thay đổi; validity guard chặn việc suy ra `PRESENT` cho
  buổi không học.
- Dùng `CalendarSessionPeriod` riêng trong calendar module; chỉ chuyển từ period attendance
  ở boundary validity service.

## 5. Validation thực tế

| Lệnh/kiểm tra | Kết quả |
|---|---|
| `./gradlew test` | PASS |
| `./gradlew checkstyleMain` | PASS |
| `./gradlew checkstyleTest` | PASS |
| `./gradlew pmdMain` | PASS |
| `./gradlew pmdTest` | PASS |
| `./gradlew build` | PASS |

Validation cuối chạy tuần tự trong môi trường `BE/BaiTap-RS`; JaCoCo report được tạo qua
`test` task. PMD console vẫn thông báo rule `LoosePackageCoupling` bị loại vì cấu hình không
có package/class, đây là behavior cấu hình hiện tại và không phải lỗi mới của task.

Số vòng `code -> validation -> debug` có sửa code/test: `4`.

## 6. Sai lệch so với Developer Plan

- Tách thêm `CalendarValidator` và `CalendarSessionService` để giữ PMD pass.
- Dùng migration V7 kế tiếp migration V6 hiện có thay vì tên migration trong schema skeleton
  tài liệu v2.
- API đọc calendar dùng query bắt buộc `academicYearId`, `semesterId`, `from`, `to`; chưa có
  API riêng theo lớp vì assumption cấp academic year/semester đã được chốt trong plan.

## 7. Blocker và rủi ro còn lại

- Không còn blocker validation trong môi trường hiện tại.
- Chưa có controller integration test riêng cho calendar; hiện có service test và full Spring
  test suite.
- Chưa có dữ liệu seed calendar production hoặc import lịch tự động.
- Nếu nghiệp vụ yêu cầu lịch khác nhau theo lớp, cần plan cập nhật schema/API trước khi mở rộng.

## 8. Next steps

1. Bổ sung Postman collection chỉ khi user yêu cầu.
2. Mở rộng calendar theo lớp hoặc import/auto-generate lịch bằng plan riêng nếu nghiệp vụ cần.
3. Xây dựng attendance report trên calendar validity ở task tiếp theo.
