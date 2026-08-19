# Developer Plan: Student CSV Download và Pagination Options

## Mục tiêu

- Thêm nút download toàn bộ Student cạnh `Add student`.
- Thêm page size `20` và `50` bên cạnh mặc định `10`.
- Thêm `Go to page [textbox] / {totalPages}`.

## Phạm vi

### In-scope

- Frontend Student List, Student API service và test liên quan.
- Gọi API CSV export hiện có: `GET /api/v1/students/export`.
- Pagination server-side với page size `10`, `20`, `50`.

### Out-of-scope

- Không sửa backend, database, batch job, API contract, Postman collection hoặc dependency.
- Download luôn export toàn bộ data theo endpoint; không gửi search, sort hay page hiện tại.

## Thiết kế

1. Thêm `downloadStudentsCsv(token)` trong `studentApi.ts`:
   - Gửi Bearer token và nhận `Blob` CSV, không parse JSON.
   - Nếu response lỗi, đọc JSON error và dùng `ApiError` như Student API hiện có.
2. `StudentListView.vue` thêm nút `Download all students` cạnh `Add student`:
   - Tạo object URL, kích hoạt browser download `students.csv`, sau đó revoke URL.
   - Dùng trạng thái download riêng; khóa nút trong lúc tải và hiện lỗi/401 theo policy hiện có.
3. `StudentTable.vue` đổi `rows-per-page-options` thành `[10, 20, 50]` và emit page size mới:
   - Khi đổi size: đặt `page = 0`, gọi lại list với `size` mới.
   - Cột `No` tiếp tục tính theo page và page size hiện tại.
4. Thêm `Go to page` bằng PrimeVue `InputNumber` và nút Go:
   - UI one-based, API zero-based.
   - Go/Enter chỉ chấp nhận số nguyên trong khoảng `1..totalPages`.
   - Input invalid không gọi API và hiện validation ngắn.
   - Khi `totalPages = 0`, disable textbox/nút; đồng bộ input khi query/page response thay đổi.

## File dự kiến

| Path | Thao tác | Mục đích |
|---|---|---|
| `FE/src/services/studentApi.ts` | Sửa | Thêm CSV blob request và error handling. |
| `FE/src/services/studentApi.spec.ts` | Sửa | Test export URL, bearer header, Blob và error. |
| `FE/src/views/StudentListView.vue` | Sửa | Điều phối download/loading/error và page-size query. |
| `FE/src/components/StudentTable.vue` | Sửa | Page-size options, Go to page UI và emits. |
| `FE/src/types/student.ts` | Có thể sửa nhỏ | Bổ sung type emit/query nếu cần. |
| `FE/src/**/*.spec.ts` phù hợp | Tạo/sửa | Test pagination và Go to page. |
| `document/dev-note/fe/...` | Tạo sau implementation | Ghi nhận thay đổi và validation thực tế. |

## Test và validation

- Export: request/header đúng, download Blob, error và `401`.
- Page size: đổi 10/20/50 gửi `size` đúng và reset trang 0.
- Go to page: Go và Enter hợp lệ; input rỗng, ngoài khoảng, `totalPages = 0` không gọi API.
- Chạy: `npm run lint`, `npm run test`, `npm run test:coverage`, `npm run build`.

## Rủi ro

- Export batch có thể chậm khi dữ liệu lớn; UI chỉ chặn download trùng lặp. Không có progress/cancel vì API hiện tại không hỗ trợ.

## Approval status

- Approved by user on 2026-08-19.
