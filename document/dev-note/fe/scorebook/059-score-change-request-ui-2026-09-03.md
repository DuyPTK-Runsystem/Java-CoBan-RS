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
- Cập nhật typography: FE ưu tiên `Roboto`, nạp webfont qua Google Fonts và giữ fallback hệ thống khi không có mạng; thay thế lựa chọn `Segoe UI` trước đó.
- UX amendment: từ `ScoreEntryDialog`, GV có thể mở yêu cầu sửa điểm với context ô điểm đã chọn; form hiển thị readonly học sinh/cột điểm/điểm hiện tại, còn sổ điểm read-only vẫn cho mở ô nhưng khóa lưu trực tiếp.
- UX amendment tiếp theo: bỏ input mã HS khỏi form tạo request; tại trang yêu cầu, GV chọn học sinh và cột điểm bằng dropdown từ score grid, rồi xem điểm hiện tại trước phần đề xuất.
- UI alignment amendment: tách các trường detail thành card riêng có label/value rõ ràng; nhóm action trong detail căn phải và responsive; filter bar giữ chung baseline.
- Date display amendment: thời gian gửi request ở list/detail được format thành `dd-mm-yyyy hh:mm:ss`; API value vẫn giữ nguyên.
- Table alignment amendment: hàng danh sách căn giữa theo chiều dọc; ô học sinh dùng
  hai block cố định cho mã và tên để các cột cùng hàng không bị lệch khi tên học sinh
  chiếm dòng thứ hai.

## Files thay đổi thuộc Plan 059

- `FE/src/services/scoreChangeRequestApi.ts`
- `FE/src/services/scoreChangeRequestApi.spec.ts`
- `FE/src/types/scoreChangeRequest.ts`
- `FE/src/views/ScoreChangeRequestView.vue`
- `FE/src/components/ScoreChangeRequestForm.vue`
- `FE/src/components/ScoreChangeRequestForm.spec.ts`
- `FE/src/components/ScoreChangeRequestDetail.vue`
- `FE/src/components/ScoreChangeRequestDetail.spec.ts`
- `FE/src/utils/scoreChangeRequestDate.ts`
- `FE/src/utils/scoreChangeRequestDate.spec.ts`
- `FE/src/components/ScoreChangeRequestForm.stories.ts`
- `FE/src/components/ScoreEntryDialog.vue`
- `FE/src/components/ScoreEntryDialog.spec.ts`
- `FE/src/components/ScoreEntryDialog.stories.ts`
- `FE/src/components/ScoreGrid.vue`
- `FE/src/views/ScorebookWorkspaceView.vue`
- `FE/src/views/ScorebookWorkspaceView.spec.ts`
- `FE/src/components/ScoreChangeRequestDetail.stories.ts`
- `FE/src/router/index.ts`
- `FE/src/views/AuthenticatedV2ShellView.vue`

## Validation

Final validation after table-alignment amendment: lint/build và browser visual QA sẽ được
ghi nhận theo kết quả thực tế bên dưới; các gate trước đó giữ nguyên lịch sử.

- `git diff --check`: PASS.
- `npm run lint`: PASS sau alignment.
- `npm run test -- --reporter=dot`: PASS sau date-format amendment (49 files/162 tests).
- `npm run test:coverage`: NOT RUN trong amendment date-format này.
- `npm run build`: PASS.
- `npm run build-storybook`: PASS.
- Browser visual QA: NOT RUN.

### Table-alignment amendment validation (2026-09-03)

- `npm run lint`: `PASS`.
- `npm run build`: `PASS`.
- `npm run test`: `PASS` — 50 files, 168 tests.
- Browser visual QA: `PASS` — tại `/v2/score-change-requests`, hai hàng dữ liệu
  có chiều cao `62px`; mọi ô trong mỗi hàng cùng `top`/`height` và computed
  `vertical-align: middle`.

## Contract blocker / missing API

- Backend đã có đủ list/detail/create/approve/reject/cancel cho Plan 059.
- Không có endpoint lookup context riêng; implementation dùng các API học vụ/sổ điểm hiện có.
- List DTO không có tên cột điểm, nên bảng hiển thị “Cột điểm đã chọn”; cần backend/context DTO nếu muốn hiển thị tên cột chính xác.
- Quyền cuối cùng và ownership vẫn do backend quyết định; FE chỉ dùng role trong session cho UX hint.

## Deviations và next steps

- Đã hoàn tất UX/alignment amendments: cả flow từ score cell và flow tạo từ trang lịch sử đều chọn học sinh bằng UI, hiển thị điểm hiện tại; detail dialog đã căn chỉnh card/action; browser visual QA chưa chạy vì chưa có browser session.
