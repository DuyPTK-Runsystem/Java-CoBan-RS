# DISPATCH: E2E Test Writer (Parallel Testing Track)

## Objective
Xây dựng cơ sở hạ tầng kiểm thử E2E và thiết kế toàn diện bộ kiểm thử 4 Tiers (Tiers 1-4) cho hệ thống Quản lý học sinh V2 theo phương pháp Category-Partition, Boundary Value Analysis, Pairwise Combinations, và Real-World Scenarios theo quy định của Project Pattern.

## Scope Boundaries
- Chỉ tạo và bổ sung các test suite E2E / component integration tests và tài liệu kiểm thử.
- TUYỆT ĐỐI KHÔNG sửa đổi mã nguồn ứng dụng (views, components chính).
- Xuất bản `TEST_INFRA.md` tại thư mục gốc dự án `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/TEST_INFRA.md`.
- Xuất bản `TEST_READY.md` tại thư mục gốc dự án `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/TEST_READY.md` khi hoàn thành.

## 4 Tiers Test Requirements
- **Tier 1 - Feature Coverage**: Ít nhất 5 test cases cho mỗi tính năng trong Feature Inventory (>= 5 x N).
- **Tier 2 - Boundary & Corner Cases**: Ít nhất 5 test cases biên cho mỗi tính năng có giá trị biên (>= 5 x N).
- **Tier 3 - Cross-Feature Combinations**: Kiểm thử tương tác giữa các cặp tính năng (tạo sinh viên -> phân lớp -> điểm danh -> bảng điểm -> tính lại điểm).
- **Tier 4 - Real-World Application Scenarios**: Ít nhất 5 kịch bản học vụ thực tế hoàn chỉnh từ đầu đến cuối.

## File Ownership
- `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/TEST_INFRA.md`
- `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/TEST_READY.md`
- Các file test E2E / integration test mới dưới `FE/src/tests/` hoặc `FE/src/views/` dành riêng cho E2E integration test suite.

## MANDATORY INTEGRITY WARNING
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Input Information
- ORIGINAL_REQUEST.md: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/ORIGINAL_REQUEST.md` (BẮT BUỘC đọc file này trước tiên)
- PROJECT.md: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/PROJECT.md`
- Báo cáo E2E Explorer: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/explorer_e2e_0/report.md`
- Working Directory: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/test_writer_e2e_1`

## Output Requirements
1. Hoàn thành viết các bộ kiểm thử E2E tích hợp cho FE/BE.
2. Viết file `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/TEST_INFRA.md`.
3. Viết file `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/TEST_READY.md`.
4. Viết báo cáo bàn giao vào `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/test_writer_e2e_1/handoff.md`.
Dùng send_message gửi thông báo hoàn thành về cho Orchestrator (parent).

