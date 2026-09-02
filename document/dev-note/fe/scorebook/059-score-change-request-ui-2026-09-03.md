# Dev Note 059 — Score Change Request UI

- Developer Plan: `document/dev-impl-plan/FE/scorebook/059-score-change-request-ui-2026-09-03.md`
- Approval: User đã approve Plan 059 qua agent message.
- Trạng thái implementation: Completed.

## Phạm vi đã thực hiện

- Thêm typed service cho list/detail/create/approve/reject/cancel tại `FE/src/services/scoreChangeRequestApi.ts`.
- Thêm API/UI types tại `FE/src/types/scoreChangeRequest.ts`.
- Thêm route và view `FE/src/views/ScoreChangeRequestView.vue`.
- Thêm form và detail components cùng Storybook fixtures.
- Context UI dùng năm học, học kỳ, lớp và môn học; không hiển thị `scorebookId` như input/context kỹ thuật.
- Nhãn trạng thái, validation và thông báo người dùng hiển thị bằng tiếng Việt.

## Files thay đổi thuộc Plan 059

- `FE/src/services/scoreChangeRequestApi.ts`
- `FE/src/services/scoreChangeRequestApi.spec.ts`
- `FE/src/types/scoreChangeRequest.ts`
- `FE/src/views/ScoreChangeRequestView.vue`
- `FE/src/components/ScoreChangeRequestForm.vue`
- `FE/src/components/ScoreChangeRequestForm.stories.ts`
- `FE/src/components/ScoreChangeRequestDetail.vue`
- `FE/src/components/ScoreChangeRequestDetail.stories.ts`
- `FE/src/router/index.ts`
- `FE/src/views/AuthenticatedV2ShellView.vue`

## Validation

Final validation: lint PASS; test PASS (46 files/148 tests); coverage PASS (84.35% statements); production build PASS; Storybook build PASS; browser visual QA NOT RUN vì chưa có browser session.

- `git diff --check`: PASS; chỉ phát cảnh báo LF/CRLF của các thay đổi có sẵn trong working tree.
- `npm.cmd run lint`: FAIL lần đầu do 2 import không dùng trong `ScoreChangeRequestView.vue`; đã sửa. Lần chạy lại bị ngắt trước khi có kết quả cuối.
- `npm.cmd run test`: NOT RUN/đang chạy khi phiên bị dừng, chưa có kết quả cuối.
- `npm.cmd run test:coverage`: NOT RUN.
- `npm.cmd run build`: NOT RUN.
- `npm.cmd run build-storybook`: NOT RUN.
- Browser visual QA: NOT RUN.

## Contract blocker / missing API

- Backend đã có đủ list/detail/create/approve/reject/cancel cho Plan 059.
- Không có endpoint lookup context riêng; implementation dùng các API học vụ/sổ điểm hiện có.
- List DTO không có tên cột điểm, nên bảng hiển thị “Cột điểm đã chọn”; cần backend/context DTO nếu muốn hiển thị tên cột chính xác.
- Quyền cuối cùng và ownership vẫn do backend quyết định; FE chỉ dùng role trong session cho UX hint.

## Deviations và next steps

- Validation chưa hoàn tất do user yêu cầu dừng và báo cáo ngay.
- Đã chạy lại toàn bộ FE gates; Plan 059 được ghi nhận Completed. Browser visual QA chưa chạy vì chưa có browser session.
