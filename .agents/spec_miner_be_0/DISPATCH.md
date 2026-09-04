# DISPATCH: Backend Architecture & API Spec Mining

## Objective
Thực hiện khảo sát toàn diện mã nguồn Backend (BE) của dự án liên quan đến quản lý học sinh và các phân hệ học vụ tích hợp theo yêu cầu trong ORIGINAL_REQUEST.md.

## Scope Boundaries
- Chỉ khảo sát và phân tích mã nguồn, tài liệu, tests hiện có (Read-only).
- TUYỆT ĐỐI KHÔNG sửa đổi, tạo mới mã nguồn backend hay chạy lệnh làm thay đổi code.

## Input Information
- User request: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/ORIGINAL_REQUEST.md` (BẮT BUỘC đọc file này trước tiên)
- Backend code: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/BE`
- Documentation: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/document`
- Working directory: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/spec_miner_be_0`

## Output Requirements
Viết báo cáo phân tích chi tiết vào `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/spec_miner_be_0/report.md` và `handoff.md`.
Báo cáo cần làm rõ:
1. Toàn bộ API hiện có liên quan đến Student:
   - v1: `POST /api/v1/students`, `DELETE /api/v1/students/{id}`, `GET`, `PUT`,...
   - v2: Học vụ (`/api/v2/students/{id}/enrollments`, `/api/v2/attendance/students/{id}/history`, `/api/v2/transcripts/students/{id}/...`,...)
   - v3: `POST /api/v3/students` (tạo đồng thời User, Student, StudentInfo trong 1 transaction, quy tắc sinh username/password, mã studentCode theo CR-STUDENT-001). Trạng thái hiện tại của API v3 trong BE đã có chưa hay cần bổ sung/hoàn thiện?
2. Data model & schema: Student, StudentInfo, User, Enrollment, Attendance, Transcript, Status enum (ACTIVE/INACTIVE/GRADUATED), ràng buộc khóa ngoại và chính sách xóa an toàn / soft delete / chuyển trạng thái.
3. Phân quyền (Security & Roles): ADMIN, ACADEMIC_OFFICE, TEACHER, STUDENT đối với từng endpoint.
4. Hiện trạng Backend tests & quality tooling: lệnh chạy test, Checkstyle, PMD, các test case đã có và những test cần viết thêm.

## Completion Criteria
Báo cáo đầy đủ, rõ ràng, trích dẫn file code và class/method cụ thể trong BE.

