# Plan 091 — Autofill số tiết yêu cầu mỗi tuần

- Tài liệu: application-doc v3, module Timetable and Teacher Load; giữ nguyên API hiện hữu.
- Approval: người dùng trả lời “ukm” sau khi duyệt phương án và v3 trong chat ngày 2026-10-02.
- Mục tiêu: tự điền số tiết cho phân công có dữ liệu trong TKB đang mở; thiếu lịch để trống; cho phép chỉnh sửa và giữ giá trị người dùng.
- Hiện trạng: TimetableWorkspaceView tải toàn bộ entries của revision; TimetableAgentWorkspace truyền entries vào TimetableAgentPanel, demands đang khởi tạo rỗng.
- Phạm vi: TimetableAgentPanel, regression tests, Dev Note. Không đổi backend/API/schema.
- Triển khai: khớp assignmentId/classId/revisionId; lọc khoảng ngày giao nhau; đếm slot ngày/buổi/tiết ở thời điểm đầu tiên có lịch trong khoảng đã chọn, không cộng các giai đoạn kế tiếp. Bảo vệ giá trị nhập tay kể cả khi xóa ô; reset khi chuyển revision. Giữ bước xác nhận demands.
- Validation: focused panel/workspace tests, lint, full test, coverage, production build, Storybook build.
- Giới hạn: autofill là mẫu lịch đầu khoảng áp dụng; không suy ra định mức từ policy hoặc tự tạo lịch.
