# DISPATCH: End-to-End Integration & Test Architecture Survey

## Objective
Thực hiện khảo sát toàn diện về tích hợp End-to-End giữa Frontend và Backend, đồng thời nghiên cứu kiến trúc kiểm thử E2E / Integration tests theo yêu cầu trong ORIGINAL_REQUEST.md.

## Scope Boundaries
- Chỉ khảo sát và phân tích mã nguồn, mock APIs, fixtures, test runner (Read-only).
- Không sửa đổi mã nguồn.

## Input Information
- User request: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/ORIGINAL_REQUEST.md` (BẮT BUỘC đọc file này trước tiên)
- Toàn bộ workspace: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan`
- Working directory: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/explorer_e2e_0`

## Output Requirements
Viết báo cáo phân tích chi tiết vào `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/explorer_e2e_0/report.md` và `handoff.md`.
Báo cáo cần làm rõ:
1. Luồng tích hợp xuyên suốt từ FE sang BE:
   - Đăng nhập -> Redirect sang `/v2` -> Shell v2 -> Vào `/v2/students`.
   - Danh sách học sinh -> Tìm kiếm / Phân trang / Sắp xếp -> Drill-down vào Chi tiết học sinh (4 tabs: Hồ sơ cá nhân, Phân lớp, Điểm danh, Bảng điểm).
   - Thêm học sinh có cấp tài khoản (`POST /api/v3/students`) và không cấp tài khoản (`POST /api/v1/students`).
   - Xóa an toàn / Chuyển trạng thái học sinh (INACTIVE/GRADUATED) thay vì xóa cứng gây lỗi khóa ngoại.
2. Ma trận phân quyền (Role Matrix) cho từng hành động trên UI và API (ADMIN, ACADEMIC_OFFICE, TEACHER, STUDENT).
3. Đề xuất bộ kiểm thử E2E đa tầng (4 Tiers theo chuẩn Project Pattern):
   - Tier 1: Feature Coverage (mỗi feature >= 5 test cases)
   - Tier 2: Boundary & Corner cases (lỗi 409 duplicate username/studentCode, dữ liệu trống, phân trang ngoài giới hạn,...)
   - Tier 3: Cross-Feature Combinations (tạo học sinh -> phân lớp -> điểm danh -> bảng điểm)
   - Tier 4: Real-World Application Scenarios (kịch bản học vụ thực tế hoàn chỉnh)
4. Phương thức chạy test E2E / component test trong project (Vitest, Mock Service Worker / MSW, Spring Boot tests).

## Completion Criteria
Báo cáo đầy đủ, phân loại 4 tiers rõ ràng, ánh xạ đầy đủ từng yêu cầu R1-R5 và yêu cầu chuyển hướng login redirect `/v2`.

