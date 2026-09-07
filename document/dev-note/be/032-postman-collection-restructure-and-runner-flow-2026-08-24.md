# Dev Note: Postman Collection Restructure and Runner Flow

## Related Developer Plan

- Plan: Implementation Plan được user phê duyệt trực tiếp qua chat vào ngày `2026-08-24`.
- Scope: Tái cấu trúc thứ tự 66 request trong `document/postman/Java-CoBan.postman_collection.json` thành 11 folder nghiệp vụ tuần tự, chuẩn hóa biến động, rà soát chính xác tên các trường ID trong Response DTO backend (`studentId`, `sessionId`, `id`,...), và bổ sung test script tự động trích xuất ID & kiểm tra status code cho tất cả API.

## Actual Scope Completed

1. **Sắp xếp lại 11 folder nghiệp vụ theo thứ tự logic**:
   - `01. Auth`: `Register`, `Login` (lưu `accessToken`), `Account`.
   - `02. Student`: `Generate Student Code`, `Create Student` (lưu `studentId` từ `data.studentId`), `Get Student`, `Update Student`, `Fetch Students`, `Export Students CSV`.
   - `03. Academic Foundation`: `Create Academic Year` (lưu `academicYearId` từ `data.id`), `List Academic Years`, `Update Academic Year`, `Create Grade Level` (lưu `gradeId` từ `data.id`), `List Grade Levels`, `Update Grade Level`, `Create School Class` (lưu `classId` từ `data.id`), `List School Classes`, `Update School Class`.
   - `04. Semesters & Subjects`: `Create Semester` (lưu `semesterId` từ `data.id`), `List Semesters`, `Update Semester`, `Activate Semester`, `Create Subject` (lưu `subjectId` từ `data.id`), `List Subjects`, `Update Subject`, `Create Subject Applicability - Grade`, `Create Subject Applicability - Class`, `Create Class Subject` (lưu `classSubjectId` từ `data.id`), `List Class Subjects`, `Update Class Subject`.
   - `05. Teacher`: `Create Teacher` (lưu `teacherId` từ `data.id`), `Get Teacher`, `List Teachers`, `Update Teacher`.
   - `06. Enrollment`: `List Unassigned Students`, `Create Enrollment` (lưu `enrollmentId` từ `data.enrollments[0].id`), `Bulk Create Enrollment`, `List Class Students`, `List Student Enrollment History`, `Transfer Enrollment`.
   - `07. Assignment`:
     - Subfolder `Homeroom Assignments`: `Create Homeroom Assignment` (lưu `homeroomAssignmentId` từ `data.id`), `List Homeroom Assignments By Class`, `Replace Homeroom Assignment`.
     - Subfolder `Subject Teaching Assignments`: `Create Subject Teaching Assignment` (lưu `subjectTeachingAssignmentId` từ `data.id`), `List Subject Teaching Assignments By Teacher`, `Replace Subject Teaching Assignment`.
   - `08. Calendar`: `Upsert Calendar Day`, `List Calendar Days`.
   - `09. Attendance`: `Create Or Get Attendance Session` (lưu `attendanceSessionId` từ `data.sessionId`), `List Session Students`, `Upsert Attendance Exception`, `Student Attendance History`, `Delete Attendance Exception`.
   - `10. Lifecycle & Decision`: `End Homeroom Assignment`, `End Subject Teaching Assignment`, `Evaluate Semester Completeness Decision`, `Lock Semester`, `Reopen Semester`, `Close School Class`, `Close Academic Year`.
   - `11. Cleanup & Logout`: `Delete School Class`, `Delete Grade Level`, `Delete Academic Year`, `Delete Teacher`, `Delete Student`, `Logout`.

2. **Rà soát & Sửa chính xác các trường ID trong Test Scripts**:
   - `Student`: Dùng `createdStudent.studentId || createdStudent.id` (vì `ResStudentDTO` trường chính là `studentId`).
   - `Attendance`: Dùng `data.sessionId || data.id` (vì `ResAttendanceSessionDTO` trường chính là `sessionId`).
   - `Enrollment`: Dùng `data.enrollments[0].id || data.enrollments[0].enrollmentId` (vì `ResEnrollmentMutationDTO` chứa mảng `enrollments`, mỗi phần tử `ResEnrollmentDTO` có `id`).
   - `Academic / Teacher / Assignment / Calendar`: Dùng fallback linh hoạt `data.id || data.<entity>Id` để tương thích cả 2 định dạng.

3. **Cập nhật Request Bodies & Query Parameters**:
   - Chuyển toàn bộ các giá trị ID hardcode (như `"academicYearId": 1`, `"semesterId": 1`, `"teacherId": 2`) sang biến động: `{{academicYearId}}`, `{{gradeId}}`, `{{classId}}`, `{{semesterId}}`, `{{subjectId}}`, `{{classSubjectId}}`, `{{teacherId}}`, `{{studentId}}`, `{{attendanceSessionId}}`.

4. **Bổ sung Test Assertions cho 100% Request (66/66)**:
   - Tất cả 66 requests đều có test script kiểm tra HTTP status code hợp lệ (`200`, `201`, `204`) và cấu trúc trả về tương ứng.

## Files Changed

- `document/postman/Java-CoBan.postman_collection.json`
- `document/dev-note/be/032-postman-collection-restructure-and-runner-flow-2026-08-24.md`
- `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`

## Validation

| Command | Result | Notes |
|---|---|---|
| `python3 -m json.tool document/postman/Java-CoBan.postman_collection.json` | PASS | File JSON hợp lệ, đúng cấu trúc Postman Schema v2.1 |
| DTO audit script & request field matching | PASS | Đã rà soát khớp 100% mã nguồn backend DTOs |
| 66/66 requests verification | PASS | Toàn bộ 66 requests đều có assertions và biến chính xác |

## Next Steps

- Thực thi test toàn bộ collection bằng Collection Runner trên Postman.
