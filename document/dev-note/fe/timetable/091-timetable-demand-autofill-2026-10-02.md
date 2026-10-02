# Dev Note 091 — Autofill số tiết yêu cầu mỗi tuần

- Plan: [091](../../../dev-impl-plan/summary/091-timetable-demand-autofill-2026-10-02.md), approved trong chat ngày 2026-10-02 (“ukm”), application-doc v3.
- Đã triển khai: khởi tạo demands từ entries của revision đang mở theo assignmentId và classId; thiếu lịch để trống; giữ chỉnh sửa tay kể cả giá trị null khi tải lại/đổi ngày/chọn lại lớp; chuyển revision xóa dữ liệu nhập cũ. Giữ checkbox xác nhận và payload API hiện tại.
- Cách đếm: lọc entries giao khoảng áp dụng; lấy thời điểm đầu tiên có lịch trong khoảng của từng phân công; đếm slot duy nhất theo weekday/session/periodIndex, không cộng các giai đoạn kế tiếp hoặc bản ghi trùng.
- Files: `FE/src/components/timetable/TimetableAgentPanel.vue`, `TimetableAgentPanel.spec.ts`; Plan 091 và hai Dev Note summary.
- Regression: dữ liệu tải muộn, khác assignment/class/revision, lịch kế tiếp, slot trùng, đổi ngày, giữ sửa tay/xóa ô, chuyển revision.

## Validation

- PASS: focused panel/workspace tests, 14 tests.
- PASS: `npm run lint`.
- PASS: `npm run test`, 124 files / 665 tests.
- PASS: `npm run build` (vue-tsc + Vite).
- PASS: `npm run build-storybook` (cảnh báo thư viện eval/chunk size/primevue package metadata).
- FAIL lần đầu: `npm run test:coverage`, 665 tests pass nhưng unhandled rejection `history is not defined` từ `EnrollmentListView.spec.ts` sau teardown; không sửa file ngoài scope.
- PASS coverage rerun: `npm run test:coverage` chạy riêng exit 0; 124 files / 665 tests. Lỗi teardown ở lần đầu không tái hiện trong lần chạy lại.
- NOT RUN: browser/live API/DB, backend validation (không thay backend).
- Deviation: không có. Không đổi API/schema hoặc các thay đổi có sẵn trong worktree.
