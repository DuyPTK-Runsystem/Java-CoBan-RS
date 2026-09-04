# BRIEFING — 2026-09-04T09:45:30Z

## Mission
Khảo sát toàn diện tích hợp End-to-End giữa FE và BE, đối chiếu yêu cầu R1-R5 và login redirect '/v2', xây dựng ma trận phân quyền và thiết kế bộ kiểm thử E2E 4 Tiers.

## 🔒 My Identity
- Archetype: explorer
- Roles: explorer, synthesis
- Working directory: /home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/explorer_e2e_0
- Original parent: 275dfe1c-b9ce-4cb7-b542-547e4cb2fc91
- Milestone: E2E Integration Survey & 4-Tier Test Suite Design

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Khảo sát toàn diện tích hợp End-to-End giữa FE và BE, đối chiếu tất cả yêu cầu R1-R5 và login redirect '/v2'
- Đề xuất ma trận phân quyền (ADMIN, ACADEMIC_OFFICE, TEACHER, STUDENT)
- Thiết kế bộ kiểm thử E2E 4 Tiers (Feature Coverage >=5/feature, Boundary/Corner >=5/feature, Pairwise Combinations, Real-World Scenarios)
- Phản hồi và báo cáo bằng tiếng Việt

## Current Parent
- Conversation ID: 275dfe1c-b9ce-4cb7-b542-547e4cb2fc91
- Updated: not yet

## Investigation State
- **Explored paths**:
  - `ORIGINAL_REQUEST.md`, `HANDOFF.md`
  - FE: `FE/src/router/index.ts`, `FE/src/views/LoginView.vue`, `FE/src/views/AuthenticatedV2ShellView.vue`, `FE/src/views/StudentListView.vue`, `FE/src/views/StudentFormView.vue`, `FE/src/views/TranscriptViewerView.vue`, `FE/src/services/studentApi.ts`, `FE/src/services/calculationTaskApi.ts`
  - BE: `StudentController.java`, `StudentV3Controller.java`, `StudentAccountService.java`, `StudentService.java`, `EnrollmentController.java`, `AttendanceHistoryController.java`, `TranscriptQueryController.java`, `CalculationTaskController.java`
  - Docs: `CR-STUDENT-001-...md`, `ActualPermissionMatrix.md`, `01-auth-student.md`, `03-StudentsAndEnrollment.md`, `ContractMigrationScopeFreeze.md`
- **Key findings**:
  - Đã xác định toàn bộ các điểm cần sửa cho Login redirect '/v2' (LoginView, router, unit tests).
  - Đã làm rõ tuyến đường Shell v2 và phân quyền sidebar cho "Hồ sơ học sinh" (`isNonStudent`).
  - Backend API v3 (`POST /api/v3/students`) đã sẵn sàng, FE cần tích hợp form và bắt 409 Conflict.
  - Chi tiết học sinh 4 tabs được hậu thuẫn bởi 4 controller nghiệp vụ backend có sẵn.
  - Chính sách xóa an toàn cần chặn hard-delete khi đã phát sinh dữ liệu học vụ.
  - Ma trận phân quyền đầy đủ cho 4 vai trò.
  - Thiết kế bộ test 4 Tiers (Feature Coverage >=5, Boundary/Corner >=5, Pairwise Chains, Real-World Scenarios).
- **Unexplored areas**: Không có. Toàn bộ phạm vi yêu cầu đã được khảo sát triệt để.

## Key Decisions Made
- Hoàn thành báo cáo chi tiết tại `report.md` và biên soạn báo cáo bàn giao chuẩn 5 phần tại `handoff.md`.

## Artifact Index
- report.md — Báo cáo phân tích tích hợp E2E và thiết kế bộ test 4 Tiers
- handoff.md — Báo cáo bàn giao 5 phần theo quy thức
- progress.md — Liveness heartbeat và tiến trình công việc
- DISPATCH.md — Chỉ dẫn điều phối
