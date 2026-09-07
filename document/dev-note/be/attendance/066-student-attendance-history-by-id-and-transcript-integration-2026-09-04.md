# Dev Note 066: Student Attendance History by Student ID and Transcript Integration

## 1. Related Developer Plan & Approval Status

- **Developer Plan:** [`document/dev-impl-plan/be/attendance/066-student-attendance-history-by-id-and-transcript-integration-2026-09-04.md`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/document/dev-impl-plan/be/attendance/066-student-attendance-history-by-id-and-transcript-integration-2026-09-04.md).
- **Approval status:** Approved bởi người dùng qua agent message vào ngày 2026-09-04 ("sửa thành plan 66 nhé").
- **Application-document version:** `v2`.

## 2. Actual Scope Completed

Khắc phục lỗi gọi nhầm endpoint 403 Forbidden của Giáo vụ khi xem bảng điểm chi tiết của học sinh và bổ sung API tra cứu chuyên cần học sinh theo mã định danh:
1. **Backend APIs & Authorization:**
   - Cập nhật base request mapping của [`AttendanceHistoryController.java`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/controller/AttendanceHistoryController.java) thành `/api/v2/attendance/students`.
   - Giữ nguyên endpoint `/api/v2/attendance/students/me/history` cho vai trò `STUDENT`.
   - Bổ sung endpoint `GET /api/v2/attendance/students/{studentId}/history` cho các vai trò `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER` (`@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')")`).
   - Cập nhật [`AttendanceHistoryService.java`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceHistoryService.java): bổ sung `getStudentHistory(Long studentId, ReqAttendanceHistoryQuery query)`, kiểm tra tồn tại học sinh (trả `404 NOT_FOUND` nếu không thấy), tái sử dụng hàm helper phân trang và tổng hợp số buổi vắng `collectAndPage`.
2. **Frontend Services & Views:**
   - Trong [`FE/src/services/attendanceApi.ts`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/FE/src/services/attendanceApi.ts): export hàm `fetchStudentAttendanceHistoryById(token, studentId, query)` gọi đến `GET /api/v2/attendance/students/{studentId}/history`.
   - Trong [`FE/src/views/TranscriptViewerView.vue`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/FE/src/views/TranscriptViewerView.vue): rẽ nhánh gọi API trong `loadTranscript()`:
     - Khi có `studentIdParam` (vai trò Giáo vụ, Giáo viên, Quản trị viên): gọi `fetchStudentAttendanceHistoryById(token, studentIdParam, query)`.
     - Khi không có `studentIdParam` (Học sinh tự tra cứu): gọi `fetchStudentAttendanceHistory(token, query)`.
     - Hiển thị chính xác số buổi vắng có phép và không phép của học sinh mục tiêu tại bảng tóm tắt thay vì hiển thị dấu gạch ngang do lỗi 403.
3. **Documentation:**
   - Cập nhật đặc tả API `GET /api/v2/attendance/students/{studentId}/history` trong [`04-calendar-attendance.md`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/document/application-doc/v2/frontend-api/04-calendar-attendance.md).
   - Cập nhật dòng quyền 85.1 trong ma trận phân quyền [`ActualPermissionMatrix.md`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/document/application-doc/v2/ActualPermissionMatrix.md).
   - Đăng ký Plan 066 vào [`BE_DEV_PLAN_SUMMARY.md`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md) và [`DEV_PLAN_SUMMARY.md`](file:///home/duyptk/Coding/HoiNhapJava/Java-CoBan/document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md).

## 3. Files Changed Grouped by Purpose

### Backend Core & Security
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/controller/AttendanceHistoryController.java`: Mở rộng base path `/api/v2/attendance/students` và thêm endpoint `/{studentId}/history`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceHistoryService.java`: Thêm `getStudentHistory` và helper `collectAndPage`.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/attendance/service/AttendanceHistoryServiceTest.java`: Thêm 3 test case cho `getStudentHistory` (thành công, not found, invalid date range).
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/attendance/controller/AttendanceHistoryAuthorizationIntegrationTest.java`: Thêm 5 test case kiểm thử phân quyền cho `/{studentId}/history` (Office, Teacher, Admin, Student, Anonymous).

### Frontend Services & Views
- `FE/src/services/attendanceApi.ts`: Thêm và export hàm `fetchStudentAttendanceHistoryById`.
- `FE/src/services/attendanceApi.spec.ts`: Thêm test case serialize request `fetchStudentAttendanceHistoryById`.
- `FE/src/views/TranscriptViewerView.vue`: Tích hợp gọi `fetchStudentAttendanceHistoryById` khi có `studentIdParam`.
- `FE/src/views/TranscriptViewerView.spec.ts`: Cập nhật mock và bổ sung assertions kiểm tra rẽ nhánh API chuyên cần.

### Documentation & Summaries
- `document/application-doc/v2/frontend-api/04-calendar-attendance.md`: Thêm đặc tả endpoint tra cứu chuyên cần theo `studentId`.
- `document/application-doc/v2/ActualPermissionMatrix.md`: Thêm mục 85.1 vào bảng quyền.
- `document/dev-impl-plan/be/attendance/066-student-attendance-history-by-id-and-transcript-integration-2026-09-04.md`: Developer Plan 066.
- `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md` & `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`: Cập nhật bảng tổng hợp plan.
- `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md` & `document/dev-note/summary/DEV_NOTE_SUMMARY.md`: Cập nhật bảng tổng hợp Dev Note.

## 4. Validation Result

### Backend
- **Test AttendanceHistory:** `PASS` (5 unit tests + 8 authorization integration tests đều xanh):
  - `AttendanceHistoryServiceTest`: `shouldReturnSortedItemsAndSummary`, `shouldReturnEmptyResponseWhenNoEnrollments`, `shouldRejectInvalidDateRange`, `shouldThrowExceptionWhenStudentNotFound`, `shouldReturnStudentHistoryWhenStudentExists`, `shouldThrowExceptionWhenStudentIdNotFound`, `shouldRejectInvalidDateRangeForStudentHistory`.
  - `AttendanceHistoryAuthorizationIntegrationTest`: `teacherCannotAccessStudentHistory`, `academicOfficeCannotAccessStudentHistory`, `anonymousCannotAccessStudentHistory`, `academicOfficeCanAccessStudentIdHistory`, `teacherCanAccessStudentIdHistory`, `adminCanAccessStudentIdHistory`, `studentCannotAccessStudentIdHistory`, `anonymousCannotAccessStudentIdHistory`.
- **Checkstyle:** `PASS` — Không phát sinh cảnh báo mới trên các file code sửa đổi (`AttendanceHistoryController.java`, `AttendanceHistoryService.java`).
- **PMD:** `PASS` — `build/reports/pmd/main.html` và `build/reports/pmd/test.html` ghi nhận 0 vi phạm trên các file `AttendanceHistory`.
- **Package / BootJar:** `PASS` — `./gradlew assemble -x test --no-daemon --max-workers=1` thành công tạo `bootJar`.

### Frontend
- **Unit tests:** `PASS` — `npm --prefix FE run test -- --run src/services/attendanceApi.spec.ts src/views/TranscriptViewerView.spec.ts` (13/13 tests pass).
- **Lint:** `PASS` — `npm --prefix FE run lint` (0 error, 0 warning).
- **Build:** `PASS` — `npm --prefix FE run build` (built thành công trong ~4.93s).
- **Live/Browser QA:** `NOT RUN` — Môi trường terminal không khởi động web browser tự động.

## 5. Deviations and Remaining Risks

- Không có rủi ro hồi quy: endpoint `/api/v2/attendance/students/me/history` vẫn giữ nguyên 100% logic và hợp đồng cũ cho học sinh.
- Các vi phạm PMD baseline cũ ở các module khác không bị ảnh hưởng.

