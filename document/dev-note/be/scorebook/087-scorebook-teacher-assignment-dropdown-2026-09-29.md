# Dev Note 087 — Giới hạn lựa chọn sổ điểm theo phân công giáo viên

## Liên kết và trạng thái

- Developer Plan: [087-scorebook-teacher-assignment-context-2026-09-29.md](../../../dev-impl-plan/summary/087-scorebook-teacher-assignment-context-2026-09-29.md)
- Approval: người dùng duyệt ngày `2026-09-29`; áp dụng tài liệu v3, scorebook kế thừa contract v2.
- Status: `IMPLEMENTED SLICE — VALIDATION INCOMPLETE`.

## Phạm vi đã thực hiện

- Thêm `GET /api/v2/assignments/me/scorebook-context`, chỉ cho `TEACHER`; backend xác định giáo viên từ principal đăng nhập.
- Lookup chỉ trả phân công GVBM `ACTIVE`, còn hiệu lực theo ngày nghiệp vụ HCM, kèm kỳ/năm học và context lớp-môn.
- FE workspace dùng lookup scoped cho giáo viên, giới hạn dropdown theo phân công, reset context phụ thuộc và hiển thị trạng thái chưa có phân công. Nhánh Admin/Phòng giáo vụ tiếp tục dùng lookup hiện có.
- Giữ nguyên `ScorebookGuard` và các authorization guard trên API sổ điểm.
- Cập nhật API guide v2 và bổ sung test service/repository/access/integration cùng FE service/workspace.
- Không thay đổi schema hoặc migration.

## File thay đổi

- Backend: assignment context controller/service/access/repository mới, `ResSubjectTeachingAssignmentDTO`, `ScorebookController`, `ScorebookLifecycleService`.
- Frontend: `FE/src/services/assignmentApi.ts`, `FE/src/types/assignment.ts`, `FE/src/views/scorebook/ScorebookWorkspaceView.vue` (teacher được tạo khi có lựa chọn lớp-môn hợp lệ; copy quyền đã cập nhật).
- Tests: assignment repository/service/access, scorebook authorization/guard/lifecycle (có positive assigned-teacher POST 201 + DRAFT, denial và office regression assertions) và FE assignment API/workspace specs. DEV chỉ sửa test source, không chạy; QA độc lập chạy sau bàn giao.
- Contract: `document/application-doc/v2/frontend-api/03-teacher-assignment-enrollment.md` và `05-scorebook-change-audit.md`.
- Kế hoạch: Plan 087.
- `application.properties` có thay đổi từ trước task và sau đó được ghi nhận trong commit mới `75ce6aec` (cùng thay đổi CI của người dùng); DEV/TEST không sửa file này.

## Validation Result

### Backend

- Focused Plan 087 tests: `PASS`, 36/36.
- `checkstyleMain`: `PASS`, 977 repository warnings; Plan 087 changed files were clean.
- `pmdMain`: `PASS`, 0 main-source violations.
- Full test suite: `NOT RUN` for this final verification because an earlier full-suite run hit known JVM/Spring-context OOM after 571 tests with 38 failures/errors.
- Backend build: `NOT RUN` for this final verification.

### Frontend

- Focused tests (`ScorebookWorkspaceView.spec.ts`, `assignmentApi.spec.ts`): `PASS`, 19/19 across 2 files.
- `npm run lint`: `PASS`.
- `npm run build`: `PASS`.
- The focused view coverage includes an assigned TEACHER seeing the create action and calling create then open, plus no create target when assignment context is empty.

### Khác

- `git diff --check`: `PASS`.
- Browser/live and verification with a real teacher account: `NOT RUN`.
- DEV did not run validation; TEST performed the independent checks.

## Sai lệch, blocker và rủi ro còn lại

- Full backend suite và build không chạy ở lần xác minh cuối; OOM đã biết khiến full suite chưa có bằng chứng PASS. Không coi backend validation toàn repo là PASS.
- Chưa có browser/live evidence xác nhận trực quan bằng tài khoản teacher.
- FE unit/component tests, lint và build đã PASS ở lần QA cuối; các API guards vẫn là quyền quyết định cuối cùng.
