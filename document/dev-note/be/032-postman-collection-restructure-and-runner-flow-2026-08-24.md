# Dev Note: Postman Collection Restructure and Runner Flow

## Related Developer Plan

- Plan: Implementation Plan được user phê duyệt trực tiếp qua chat vào ngày `2026-08-24`.
- Scope: Tái cấu trúc thứ tự 66 request trong `document/postman/Java-CoBan.postman_collection.json` thành 11 folder nghiệp vụ tuần tự, chuẩn hóa biến động và bổ sung test script tự động trích xuất ID.

## Actual Scope Completed

1. **Sắp xếp lại 11 folder nghiệp vụ theo thứ tự logic**:
   - `01. Auth`: `Register`, `Login` (lưu `accessToken`), `Account`.
   - `02. Student`: `Generate Student Code`, `Create Student` (lưu `studentId`), `Get Student`, `Update Student`, `Fetch Students`, `Export Students CSV`.
   - `03. Academic Foundation`: `Create Academic Year` (lưu `academicYearId`), `List Academic Years`, `Update Academic Year`, `Create Grade Level` (lưu `gradeId`), `List Grade Levels`, `Update Grade Level`, `Create School Class` (lưu `classId`), `List School Classes`, `Update School Class`.
   - `04. Semesters & Subjects`: `Create Semester` (lưu `semesterId`), `List Semesters`, `Update Semester`, `Activate Semester`, `Create Subject` (lưu `subjectId`), `List Subjects`, `Update Subject`, `Create Subject Applicability - Grade`, `Create Subject Applicability - Class`, `Create Class Subject` (lưu `classSubjectId`), `List Class Subjects`, `Update Class Subject`.
   - `05. Teacher`: `Create Teacher` (lưu `teacherId`), `Get Teacher`, `List Teachers`, `Update Teacher`.
   - `06. Enrollment`: `List Unassigned Students`, `Create Enrollment` (lưu `enrollmentId`), `Bulk Create Enrollment`, `List Class Students`, `List Student Enrollment History`, `Transfer Enrollment`.
   - `07. Assignment`:
     - Subfolder `Homeroom Assignments`: `Create Homeroom Assignment` (lưu `homeroomAssignmentId`), `List Homeroom Assignments By Class`, `Replace Homeroom Assignment`.
     - Subfolder `Subject Teaching Assignments`: `Create Subject Teaching Assignment` (lưu `subjectTeachingAssignmentId`), `List Subject Teaching Assignments By Teacher`, `Replace Subject Teaching Assignment`.
   - `08. Calendar`: `Upsert Calendar Day`, `List Calendar Days`.
   - `09. Attendance`: `Create Or Get Attendance Session` (lưu `attendanceSessionId`), `List Session Students`, `Upsert Attendance Exception`, `Student Attendance History`, `Delete Attendance Exception`.
   - `10. Lifecycle & Decision`: `End Homeroom Assignment`, `End Subject Teaching Assignment`, `Evaluate Semester Completeness Decision`, `Lock Semester`, `Reopen Semester`, `Close School Class`, `Close Academic Year`.
   - `11. Cleanup & Logout`: `Delete School Class`, `Delete Grade Level`, `Delete Academic Year`, `Delete Teacher`, `Delete Student`, `Logout`.

2. **Cập nhật Request Bodies & Query Parameters**:
   - Chuyển các giá trị ID hardcode sang biến động: `{{academicYearId}}`, `{{gradeId}}`, `{{classId}}`, `{{semesterId}}`, `{{subjectId}}`, `{{classSubjectId}}`, `{{teacherId}}`, `{{studentId}}`, `{{attendanceSessionId}}`.

3. **Bổ sung Test Scripts trích xuất ID động**:
   - Thêm `pm.collectionVariables.set(...)` cho tất cả các API tạo mới tài nguyên để tự động đồng bộ ID cho các request phụ thuộc tiếp theo.

## Files Changed

- `document/postman/Java-CoBan.postman_collection.json`
- `document/dev-note/be/032-postman-collection-restructure-and-runner-flow-2026-08-24.md`
- `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`

## Validation

| Command | Result | Notes |
|---|---|---|
| `python3 -m json.tool document/postman/Java-CoBan.postman_collection.json` | PASS | File JSON hợp lệ, đúng cấu trúc Postman Schema v2.1 |
| Verification script: 66/66 requests preserved across 11 folders | PASS | Không bị thất thoát request |
| Git status & diff inspection | PASS | File collection được cập nhật sạch sẽ |

## Next Steps

- Người dùng có thể import collection vào Postman và thực thi bằng Collection Runner hoặc từng API lẻ.
