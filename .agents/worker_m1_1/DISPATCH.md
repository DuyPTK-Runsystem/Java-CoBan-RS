# DISPATCH: Worker - Milestone 1 (Navigation, Routing & Shell V2 Integration)

## Objective
Thực hiện toàn bộ thay đổi cho Milestone 1 theo PROJECT.md và ORIGINAL_REQUEST.md:
1. Chuyển hướng sau login thành công về `/v2`:
   - Sửa `FE/src/views/LoginView.vue`: fallback trong `safeRedirect()` chuyển sang `'/v2'`, `successRedirect = ref('/v2')`.
   - Sửa `FE/src/router/index.ts`: guard `guestOnly` chuyển hướng người dùng đã login sang `'/v2'`.
2. Định tuyến Shell v2 cho phân hệ Học sinh:
   - Trong `FE/src/router/index.ts`, thêm các route con dưới `/v2` (trong children của `AuthenticatedV2ShellView`):
     - `students` (name: `'v2-students'`) -> `StudentListView.vue`
     - `students/new` (name: `'v2-student-create'`) -> `StudentFormView.vue`
     - `students/:studentId` (name: `'v2-student-detail'`) -> `StudentDetailView.vue` (nếu chưa có thì trỏ view tạm hoặc component placeholder)
     - `students/:studentId/edit` (name: `'v2-student-edit'`) -> `StudentFormView.vue`
     - Thêm redirect từ `/students` cũ sang `/v2/students`.
3. Tích hợp Sidebar v2:
   - Trong `FE/src/views/AuthenticatedV2ShellView.vue`: thêm menu item "Hồ sơ học sinh" với icon `pi pi-user`, `to: '/v2/students'`.
   - Chỉ hiển thị cho các vai trò `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER` (`isNonStudent`). Ẩn hoàn toàn với vai trò `STUDENT`.
   - Cấu hình trạng thái active khi route là `/v2/students` hoặc bất kỳ route con nào của nó.
4. Logo/Brand link trong `FE/src/components/AuthenticatedLayout.vue`: trỏ về `/v2`.
5. Cập nhật và bổ sung Unit Tests:
   - `FE/src/views/LoginView.spec.ts`: cập nhật các test case redirect về `/v2`.
   - `FE/src/router/index.spec.ts`: kiểm tra route `/v2/students` và guard redirect.
   - `FE/src/views/AuthenticatedV2ShellView.spec.ts`: kiểm tra hiển thị menu "Hồ sơ học sinh", icon `pi pi-user`, vai trò hiển thị/ẩn, active state.
6. Chạy và kiểm tra:
   - `npm --prefix FE run test -- --run`
   - `npm --prefix FE run build`

## Write Ownership (File Boundaries)
Worker M1 sở hữu độc quyền các file sau:
- `FE/src/views/LoginView.vue`
- `FE/src/views/LoginView.spec.ts`
- `FE/src/router/index.ts`
- `FE/src/router/index.spec.ts`
- `FE/src/views/AuthenticatedV2ShellView.vue`
- `FE/src/views/AuthenticatedV2ShellView.spec.ts`
- `FE/src/components/AuthenticatedLayout.vue`

## MANDATORY INTEGRITY WARNING
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Input Information
- ORIGINAL_REQUEST.md: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/ORIGINAL_REQUEST.md` (BẮT BUỘC đọc file này trước tiên)
- PROJECT.md: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/PROJECT.md`
- Survey Reports:
  - `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/spec_miner_fe_0/report.md`
  - `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/explorer_e2e_0/report.md`
- Working Directory: `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/worker_m1_1`

## Output Requirements
Viết báo cáo kết quả và lệnh kiểm thử thực tế vào `/home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/worker_m1_1/handoff.md`.
Dùng send_message gửi thông báo hoàn thành về cho Orchestrator (parent).

