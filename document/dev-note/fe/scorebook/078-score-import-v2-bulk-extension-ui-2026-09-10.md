# Dev Note 078 FE — UI file mẫu trong bulk add/update v2

## Trạng thái và approval

- Plan 078 FE và wireframe đã được người dùng approve qua agent message ngày 2026-09-10.
- Trạng thái: `IMPLEMENTED; FE validation PASS; browser/live NOT RUN`.

## Phạm vi thực tế đã triển khai

- Giữ nguyên mode `Nhập trực tiếp` của `BulkScoreEntryDialog`.
- Thêm mode `Dùng file mẫu` ngay trong dialog bulk v2: hai bước độc lập; chọn `.xlsx` hợp lệ sẽ tự tải preview rồi dùng cùng event `save` và `bulkUpsertStudentScores` hiện có.
- Bỏ hai dòng hướng dẫn dư thừa, bỏ nút `Xem trước file` và rút nhãn hành động từ `Lưu hàng loạt` thành `Lưu`.
- Đổi nhãn bước tải mẫu thành `Tải file mẫu có danh sách học sinh của lớp`.
- Preview hiển thị học sinh, current/new status và value, note, summary tạo mới/cập nhật/lỗi và lọc chỉ dòng lỗi; còn lỗi thì khóa `Lưu`.
- Thêm typed transport cho download/preview, multipart support trong `apiClient` để giữ shared auth/error handling và 401 redirect boundary.
- Thêm test component/service và story states cho manual mode, file mode trước download, template download, preview hợp lệ và preview có lỗi.

## Files chính

- `FE/src/components/scorebook/BulkScoreEntryDialog.vue`
- `FE/src/components/scorebook/BulkScoreEntryDialog.spec.ts`
- `FE/src/components/scorebook/BulkScoreEntryDialog.stories.ts`
- `FE/src/services/apiClient.ts`
- `FE/src/services/scorebookApi.ts`
- `FE/src/services/scorebookApi.spec.ts`
- `FE/src/types/scorebook.ts`

## Giới hạn

- Không tạo route/workspace import hoặc batch history riêng.
- Chưa có browser/live API walkthrough; static wireframe local không mở được do browser policy chặn `file://`, nên visual QA wireframe là `NOT RUN`.
- Storybook chưa mô phỏng toàn bộ các lỗi runtime 403/404/409/network; service giữ shared API error boundary để chờ backend runtime verification.

## Validation Result

- `npm run lint`: `PASS`.
- `npm run test`: `PASS` — 89 test files, 481 tests.
- Focused score import tests: `PASS` — 11 tests, gồm preview tự mở sau khi chọn file.
- `npm run build`: `PASS`.
- `npm run build-storybook`: `PASS`; chỉ có warning có sẵn từ Storybook/Vite (eval và chunk size).
- Browser/live API/upload thật: `NOT RUN`.
