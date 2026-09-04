# Developer Plan 066: Student Attendance History by Student ID and Transcript Integration

## Trạng thái

- Application-document version: `v2`.
- Parent plan: `document/dev-impl-plan/be/attendance/031-student-attendance-history-read-only-2026-08-23.md`, `document/dev-impl-plan/fe/scorebook/061-transcript-viewer-ui-2026-09-03.md`.
- User approval: user phê duyệt và chốt số plan 066 qua agent message ngày `2026-09-04`.
- Implementation status: `In Progress`.

## Mục tiêu và phạm vi

1. **Backend**: Bổ sung endpoint `GET /api/v2/attendance/students/{studentId}/history` cho phép các vai trò quản trị/nhân sự (`ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`) tra cứu lịch sử và thống kê chuyên cần (buổi có mặt, vắng có phép, vắng không phép, muộn, về sớm) của một học sinh cụ thể theo `studentId`.
2. **Authorization**:
   - `GET /api/v2/attendance/students/me/history`: giữ nguyên chỉ dành cho `STUDENT` (`@PreAuthorize("hasRole('STUDENT')")`).
   - `GET /api/v2/attendance/students/{studentId}/history`: phân quyền cho `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER` (`@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')")`).
   - Request có token nhưng thiếu quyền trả `403 Forbidden`; request không có xác thực trả `401 Unauthorized`.
3. **Frontend**:
   - Bổ sung hàm API service `fetchStudentAttendanceHistoryById(token, studentId, query)` trong `FE/src/services/attendanceApi.ts`.
   - Cập nhật `TranscriptViewerView.vue` để khi xem bảng điểm học sinh có `studentIdParam` (vai trò Giáo vụ, Giáo viên, Quản trị viên), gọi `fetchStudentAttendanceHistoryById` thay vì gọi nhầm `/students/me/history`.
   - Giữ nguyên luồng cho Học sinh tự tra cứu bảng điểm gọi `fetchStudentAttendanceHistory`.

## Ranh giới

- Không thay đổi cấu trúc bảng cơ sở dữ liệu hoặc entity JPA.
- Không sửa đổi logic tổng hợp chuyên cần `itemCollector` và `responseMapper`.
- Giữ nguyên endpoint `/api/v2/attendance/students/me/history` cho vai trò học sinh.

## Files dự kiến

- `BE/.../AttendanceHistoryController.java`: mở rộng base path `/api/v2/attendance/students` và thêm endpoint `/{studentId}/history`.
- `BE/.../AttendanceHistoryService.java`: thêm `getStudentHistory(studentId, query)` và refactor logic phân trang dùng chung.
- `BE/.../AttendanceHistoryServiceTest.java`: unit tests cho `getStudentHistory`.
- `BE/.../AttendanceHistoryAuthorizationIntegrationTest.java`: kiểm thử phân quyền cho endpoint mới `/{studentId}/history`.
- `FE/src/services/attendanceApi.ts` & `attendanceApi.spec.ts`: export `fetchStudentAttendanceHistoryById` và unit test.
- `FE/src/views/TranscriptViewerView.vue` & `TranscriptViewerView.spec.ts`: tích hợp gọi API chuyên cần theo `studentIdParam` và test.
- `document/application-doc/v2/frontend-api/04-calendar-attendance.md`: đặc tả API mới.
- `document/application-doc/v2/ActualPermissionMatrix.md`: cập nhật ma trận quyền thực tế.
- Plan summaries và Dev Note 066 tương ứng sau khi triển khai.

## Unit test plan

- Backend Service:
  - `getStudentHistory` với học sinh hợp lệ $\rightarrow$ trả về danh sách session và summary (số buổi vắng có phép/không phép) đầy đủ.
  - `getStudentHistory` với học sinh không tồn tại $\rightarrow$ ném `AppException(HttpStatus.NOT_FOUND)`.
  - `getStudentHistory` với khoảng ngày không hợp lệ (`from > to`) $\rightarrow$ ném `AppException(HttpStatus.BAD_REQUEST)`.
- Backend Authorization:
  - `ACADEMIC_OFFICE`, `TEACHER`, `ADMIN` gọi `GET /api/v2/attendance/students/{studentId}/history` $\rightarrow$ thành công (không bị 403).
  - `STUDENT` gọi `GET /api/v2/attendance/students/{studentId}/history` $\rightarrow$ trả `403 Forbidden`.
  - Anonymous gọi `GET /api/v2/attendance/students/{studentId}/history` $\rightarrow$ trả `401 Unauthorized`.
- Frontend:
  - `attendanceApi.spec.ts`: gọi `fetchStudentAttendanceHistoryById` kiểm tra đúng URL và URLSearchParams.
  - `TranscriptViewerView.spec.ts`: khi có `routeQuery.studentId`, component gọi `fetchStudentAttendanceHistoryById` với đúng `studentId`; khi không có `routeQuery.studentId`, component gọi `fetchStudentAttendanceHistory`.

## Validation

- Backend: `test`, `checkstyleMain`, `pmdMain`, `build`.
- Frontend: `lint`, `test`, `build`.

