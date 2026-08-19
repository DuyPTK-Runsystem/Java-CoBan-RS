# Dev Note: Student CSV Download và Pagination Options

## Related plan and approval

- Plan: `document/dev-impl-plan/fe/student/021-student-csv-download-pagination-options-go-to-page-2026-08-19.md`.
- Approved by user on 2026-08-19.

## Actual scope completed

- Thêm download toàn bộ Student CSV từ `GET /api/v1/students/export`.
- Thêm page size `10`, `20`, `50` cho server-side pagination.
- Thêm `Go to page` one-based với validation và chuyển đổi sang API page zero-based.
- Amendment theo yêu cầu user: bỏ sort Birthday và Address ở Student List.

## Files changed

- API: `FE/src/services/studentApi.ts`, `FE/src/services/studentApi.spec.ts`.
- Student List UI: `FE/src/views/StudentListView.vue`, `FE/src/components/StudentTable.vue`, `FE/src/styles.css`.
- Test/Storybook: `FE/src/components/StudentTable.spec.ts`, `FE/src/components/StudentTable.stories.ts`.
- Documentation: related Developer Plan, FE/central plan summaries, Dev Note và FE/central Dev Note summaries.

## Implementation decisions

- Export response được xử lý thành `Blob`; frontend tạo download `students.csv` và không parse CSV thành JSON.
- `401` khi download tuân thủ session policy hiện có: clear session rồi điều hướng Login.
- Khi Paginator đổi page size, view gửi `page = 0` cùng `size` mới; `No` dùng page size hiện tại.
- `Go to page` chỉ emit request với số nguyên từ `1..totalPages`; empty result disable control.
- Student List chỉ gửi server-side sort cho Code, Name và Score; Birthday/Address là cột hiển thị.

## Validation

| Command | Result |
|---|---|
| `npm run lint` | PASS |
| `npm run test` | PASS — 12 files, 37 tests |
| `npm run test:coverage` | PASS — statements 94.64%, lines 94.64% trong scope coverage hiện cấu hình |
| `npm run build` | PASS |
| `npm run build-storybook` | PASS — có warning PrimeVue package discovery, `eval` runtime và large chunk đã tồn tại; command exit 0 |

## Deviations and remaining risks

- Amendment bỏ sort Birthday/Address được thực hiện theo yêu cầu user, không tạo plan riêng.
- Export batch không có progress/cancel; nút chỉ ngăn request download trùng lặp trong lúc đang chạy.

## Next step

- Manual UI verification với backend đang chạy, đặc biệt download CSV trên browser mục tiêu.
