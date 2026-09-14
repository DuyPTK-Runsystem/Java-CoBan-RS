# Dev Note 079 — FE sổ đầu bài

## Phạm vi

Đã triển khai FE lesson-log theo Plan 079 v3 và D01–D10 đã được duyệt: lịch cá nhân, sổ tuần, form ghi/nộp, duyệt/điều chỉnh, ký tuần, policy và lịch sử. Phạm vi lần này chỉ gồm `FE/src` và test/story liên quan; không sửa BE.

## Đồng bộ contract runtime

- Giữ prefix `/api/v3/lesson-logs` và các route hiện có trong controller BE.
- Chuẩn hóa lookup lớp từ `classId/className` sang model FE `{id,name}`.
- Chuẩn hóa response tuần tối giản của BE bằng cách tạo calendar bảy ngày, summary từ `items`, và chuyển `blockedReason`/`signedSnapshotJson` sang model FE.
- Chuẩn hóa audit `actorId`, `beforeStateJson`, `afterStateJson` và `ResultPaginationDTO` với metadata phân trang zero-based.
- Payload amend gửi đúng `expectedVersion` và `reason` mà controller hiện tại nhận; các field nội dung không được gửi vào endpoint transition chưa hỗ trợ chúng.
- Rubric policy JSON được parse an toàn; lỗi parse trả danh sách rỗng để UI không crash.

## Test/story và recovery

Đã bổ sung service tests cho lookup lớp, weekly response, audit JSON/pagination, signed review và payload amend. Các test component hiện có bao phủ draft thiếu dữ liệu, sĩ số snapshot, weekly stale bắt buộc lý do và ma trận 2×4.

## Validation

| Gate | Kết quả |
|---|---|
| `npm run lint` | **PASS** |
| `npm run test` | **PASS** — 103 files, 541 tests |
| `npm run test:coverage` | **PASS** — overall 85.21% statements/lines, 73.29% branches, 69.23% functions; lesson-log chưa nằm trong `vite.config.ts` coverage include |
| `npm run build` | **PASS** |
| `npm run build-storybook` | **PASS** — Storybook cảnh báo eval/chunk size và không tìm thấy package.json PrimeVue nhưng build hoàn tất |
| Browser/live API E2E | **NOT RUN** — chưa có backend runtime/session để kiểm tra luồng thật |

Coverage là gate toàn FE theo include hiện tại; không dùng nó để khẳng định coverage riêng lesson-log. Browser/live và dữ liệu BE vẫn cần kiểm chứng ở môi trường chạy thật.
