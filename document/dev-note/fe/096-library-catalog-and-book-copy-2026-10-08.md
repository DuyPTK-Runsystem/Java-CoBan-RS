# Dev Note 096 — Library Catalog & Book Copy frontend

## Plan và approval

[Plan096](../../dev-impl-plan/summary/096-library-catalog-and-book-copy-2026-10-08.md) COMPLETED `2026-10-08`; baseline v5, UI phải đồng nhất hiện hữu. Wireframe là reference, API production theo Plan section12.
- **Amendment Plan 96.1 (`2026-10-08`)**: Người dùng phê duyệt tính năng in mã vạch hàng loạt (**Bulk Barcode Printing**) trên Frontend với khổ A4 dọc (50 nhãn/trang, 5 × 10), render CODE128 SVG qua thư viện `JsBarcode`, hỗ trợ print preview và in qua `window.print()`.
- **Amendment Plan 96.2 (`2026-10-08`)**: Người dùng phê duyệt bổ sung thông tin **Tên sách** và **Vị trí kệ** dưới mã vạch, thu nhỏ kích thước mã vạch ~10% (tăng padding lề ngang lên 2.5 mm, giảm `max-width` SVG xuống 32 mm, `JsBarcode width: 1.0, height: 25`) để bảo đảm khoảng trống an toàn 2 lề nhãn in.
- **Amendment Plan 96.3 (`2026-10-08`)**: Người dùng phê duyệt tối ưu hiển thị Tên sách khi quá dài: hỗ trợ xuống dòng tối đa 2 dòng (`-webkit-line-clamp: 2`, `word-break: break-word`) và tự động scale cỡ chữ linh hoạt theo độ dài (`title-normal` 6.8pt ≤25 ký tự, `title-medium` 6.2pt 26–45 ký tự, `title-compact` 5.5pt >45 ký tự). Chiều cao barcode SVG khống chế tối đa `13.5mm` bảo đảm trọn vẹn trong tem 27.7mm.
- **Amendment Plan 96.4 (`2026-10-08`)**: Triển khai luồng in qua isolated `<iframe>` độc lập trong `handlePrint()`. Loại bỏ hoàn toàn 100% giao diện web (khung modal PrimeVue, nút đóng, thanh cuộn, app shell) khỏi bản in của trình duyệt/PDF. Đồng thời bổ sung khối `<style>` un-scoped `@media print` ẩn dialog header và mask khi in dự phòng (Ctrl+P).

## Thực tế triển khai

- `FE/src/{types,services,components,views}/library/`: typed API contract, list/detail/create/edit, BookForm, BookCopyTable, add-copy/metadata/barcode dialogs, component stories và component `BarcodePrint.vue`.
- Router authenticated shell `/v2/library/books` cùng aliases `/library/books`/new/id/edit; existing school workspace restrictions giữ nguyên, authenticated readers được đọc Catalog. ADMIN/LIBRARIAN manager actions, quyền thật ở BE.
- UserRole thêm LIBRARIAN từ actual session role codes; pure LIBRARIAN menu Catalog+notifications. STUDENT/TEACHER + LIBRARIAN giữ giới hạn workspace học vụ cũ và thêm quyền Catalog; ADMIN + LIBRARIAN giữ quyền admin. Không role mock/demo controls trong production.
- ApiError/code và fieldErrors passthrough ở `types/api.ts`/`services/apiClient.ts`, legacy responses vẫn được hỗ trợ. Barcode lookup wire `{copy,book}` được typed-map về flattened UI copy.
- Independent QA owns specs cho API/form/list/detail/copy-table/router/shell/barcode-print. Root integration bổ sung ConfirmDialog thật và label AVAILABLE “Có sẵn” để không gọi referenceOnly là borrowable.

## Quyết định / defects đã xử lý

Reuse AuthenticatedLayout/foundation.css/PrimeVue/PageState/FormAlert/table/dialog conventions hiện hữu. Metadata layout một cột nếu không có cover, tránh 180px co hẹp và clipping. Batch retry giữ cùng key/payload, version conflict yêu cầu reload/review. Đên request sequencing/context guards khi đổi book, lookup/barcode/copy actions và duplicate accept guards; independent regression tests đã PASS. Reload/version conflict dùng typed state theo code, không parse message.
- **Tối ưu kích thước dropdown Khả năng mượn (`LibraryBookListView.vue`)**: Thay thế phân bổ đều 5 cột cũ bằng tỉ lệ linh hoạt `minmax(180px, 1.35fr) minmax(130px, 1fr) minmax(110px, 0.75fr) minmax(240px, 1.55fr) minmax(170px, 1.1fr)`. Giảm không gian cột Năm xuất bản, tăng độ rộng tối thiểu cho Khả năng mượn lên 240px để hiển thị trọn vẹn nhãn "Chưa có sách có thể mượn", loại bỏ hiện tượng đè icon chevron và tràn viền sang cột Sắp xếp. Bổ sung các mốc responsive `< 1120px` (3 cột), `< 760px` (2 cột), `< 540px` (1 cột).
- **Thêm checkbox chọn bản sao (`BookCopyTable.vue`)**: Bổ sung cột `<Column selection-mode="multiple" header-style="width: 3rem" />` trước cột Mã vạch, hỗ trợ prop/emit `selection` đồng bộ danh sách bản sao được chọn hai chiều qua `v-model:selection`.
- **Thêm nút In mã vạch (N) (`LibraryBookDetailView.vue`)**: Thêm nút `[pi pi-print In mã vạch (N)]` kiểu outlined xanh cạnh nút `Thêm bản sao` trong phần tiêu đề Bản sao dành cho quản trị viên/thủ thư, tự động cập nhật số lượng `N` đang chọn, bị vô hiệu hóa khi `N = 0`. Mở modal xem trước bản in khi bấm.
- **Component in mã vạch hàng loạt (`BarcodePrint.vue`)**:
  - Dàn trang chuẩn khổ A4 dọc (210 × 297 mm, lề trang 10 mm).
  - Mỗi trang tối đa 50 mã vạch, bố trí lưới 5 cột × 10 dòng (kích thước mỗi tem ~38 mm × 27.7 mm).
  - Tự động chia trang khi số lượng > 50 (ví dụ 50 bản sao → 1 trang, 80 bản sao → 2 trang).
  - Render mã vạch chuẩn CODE128 dạng SVG bằng thư viện `JsBarcode`, hiển thị chuỗi barcode bên dưới các vạch.
  - Sử dụng `@page { size: A4 portrait; margin: 10mm; }` và `@media print` với ngắt trang `break-after: page; page-break-after: always;` (trang cuối không ngắt trang để tránh trang trắng thừa), ẩn toàn bộ giao diện điều hướng và dialog của web khi in.
  - Xử lý các trường hợp ngoại lệ: danh sách rỗng (thông báo + disable nút in), chuỗi barcode không hợp lệ.
  - **Cải tiến Plan 96.2 & Plan 96.3**:
    - Bổ sung hiển thị **Tên sách** (`.barcode-label-title`) và **Vị trí kệ** (`.barcode-label-shelf`, font 6.8pt, bold) ngay dưới chuỗi barcode. Fallback vị trí kệ là "Chưa xếp kệ" nếu giá trị null/trống.
    - Thu nhỏ mã vạch khoảng 10% và mở rộng lề: `JsBarcode width: 1.0` (giảm từ 1.15), `height: 25` (giảm từ 38), SVG `max-width: 32mm` (giảm từ 36mm) và `max-height: 13.5mm`. Padding ngang của tem tăng lên 2.5 mm, tạo lề ~3 mm ở cả hai bên, tránh sát đường cắt đứt và đạt chuẩn quiet zone quét mã.
    - Cơ chế hiển thị tựa sách dài: hỗ trợ xuống dòng tối đa 2 dòng (`display: -webkit-box; -webkit-line-clamp: 2; word-break: break-word`), kết hợp scale cỡ chữ tự động (`title-normal`: 6.8pt ≤25 ký tự; `title-medium`: 6.2pt 26–45 ký tự; `title-compact`: 5.5pt >45 ký tự) giúp các tiêu đề sách dài hiển thị trọn vẹn không bị cắt cụt sớm.
    - Màu chữ khi in (`@media print`) thiết lập `#000000 !important` để tối ưu độ tương phản cho máy in.

## Validation cuối — independent 6-Luna QA

| Command từ FE/ | Kết quả |
| --- | --- |
| `npm test` | PASS — 131 files / 720 tests |
| `npm run lint` | PASS |
| `npm run build` | PASS — vue-tsc + Vite production |
| `npm run build-storybook` | PASS |
| `npm run test:coverage` | PASS — baseline configured scope 86.94% lines/statements, 76.57% branches |

Coverage mặc định excludes Library; đo riêng với 8 spec files Library API/BookForm/BookCopyTable/list/form/detail/router/shell, CLI `--coverage.include` cho production library components/views/API + router/shell và `--coverage.reportsDirectory=/tmp/plan096-fe-library-focused-coverage-final`: PASS 131 tests. Components 100% lines /79.81% branches; API92.15/100; views80.44/71.98 (Detail73.50/58.82, Form91.30/81.13, List97.46/90); router98.79/96.07; shell88.81/93.02. Không dùng baseline aggregate làm coverage Library.

Sau fix role hỗn hợp, router/shell regression PASS 110 tests; scoped coverage tại `/tmp/plan096-fe-mixed-role-coverage`: router 98.79% lines /96.15% branches, studentNavigation 89.47/90.47, shell 88.75/93.33. Full test/coverage/lint/build đã chạy lại PASS; thay đổi này không ảnh hưởng stories nên dùng kết quả Storybook PASS trước đó.

Hai coverage lượt trung gian từng báo async `history is not defined` ở EnrollmentListView sau teardown. Final full coverage không tái hiện; Enrollment spec riêng PASS4/4,72.16% lines; không sửa Enrollment source/test và không tuyên bố đã fix lỗi có sẵn. Storybook vẫn có warnings metadata PrimeVue/eval trong Storybook runtime/large DocsRenderer chunk, command exit0.

Independent tests xác nhận actual wire query/version/idempotency header, nested copy/book mapping, PNG blob và code/fieldErrors legacy regression; filter/page reset, search lại cùng filter, stale requests, edit route race, pending intent reset khi đổi sách, double-submit/duplicate accept, real PrimeVue confirmation accept/reject, reader permissions và librarian menu/landing/aliases.

Actual Chrome FE build với API fixture: desktop list/detail/form, mobile list/detail/add-copy dialog, desktop add-copy/metadata/archive dialogs, reader/LIBRARIAN menu và màn Môn học trên cùng1440x1100 đã render/inspect. Shell/palette/table/button/form conventions đồng nhất actual academic screen (PrimeVue palette hiện hành). Root phát hiện và sửa metadata clipping/missing ConfirmDialog/mobile button squeeze, sau đó render lại.

Final visual evidence: `/tmp/plan096-ui-{library,academic,detail-fixed,form,mobile,reader,librarian,add,metadata,archive,mobile-final,add-mobile}.png`. Các ảnh detail/mobile cũ trướcfix không final proof. Browser dùng fixture/mock session để đánh giá layout; không chứng minh API/DB/runtime authorization hay full-stack target integration.

## Deviation / giới hạn

Shared optional code transport và role-aware navigation là narrow integration cần cho Catalog, không generic refactor. Vite coverage config hiện excludes library; QA dùng CLI scoped coverage riêng thay vì thay baseline config. Không deploy/push; full-stack target-browser proof chưa chạy. Các FE quality gates cuối ở trên đã PASS.
