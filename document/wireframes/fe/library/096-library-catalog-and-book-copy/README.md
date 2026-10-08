# Plan 096 — Danh mục sách và bản sao

Đây là wireframe tương tác bằng HTML/CSS/JavaScript thuần, dùng dữ liệu minh họa và chạy trực tiếp khi mở `index.html`. Thanh màu xanh nhạt phía trên màn hình là điều khiển xem trước; nó nằm ngoài shell sản phẩm. Wireframe không kết nối backend, không chứng minh quyền truy cập và không phê duyệt quy tắc nghiệp vụ. Trang yêu cầu Roboto như FE hiện tại; nếu máy không có font này thì dùng font hệ thống thay thế để vẫn chạy offline. Không có font hoặc thư viện ngoài cần tải.

## Cách xem

- Chọn **Bạn đọc đã đăng nhập**, **Thủ thư** hoặc **Quản trị viên** để xem các thao tác hiện theo vai trò giả lập. Đây là lựa chọn trình bày, không phải bằng chứng về role/session thật.
- Chọn trạng thái để xem tải danh sách, danh sách trống, lỗi có thể thử lại, từ chối truy cập hoặc thông tin đã thay đổi. Trạng thái được điều khiển từ thanh xem trước.
- Tìm kiếm theo tên sách/tác giả/ISBN; lọc thể loại/năm/khả năng mượn; đổi sắp xếp và phân trang. Chọn **Xem chi tiết** để mở metadata, danh sách bản sao có phân trang, tra cứu chuỗi mã và các thao tác quản lý minh họa.
- Thủ thư/quản trị viên có thể mở biểu mẫu tạo/sửa, thêm nhiều bản sao, xem xác nhận lưu trữ/rút bản sao và chuyển trạng thái giữa **Có thể mượn** với **Hư hỏng**.
- Để xem thông báo xung đột phiên bản, chọn trạng thái **Thông tin vừa thay đổi**, mở một biểu mẫu sửa và chọn **Lưu thay đổi**.
- Mã vạch hiển thị các vạch trang trí và có ghi rõ là **minh họa, chưa phải ảnh dùng để quét**. Chuỗi có thể sao chép hoặc nhập tra cứu thủ công.
- Để xem bố cục di động, thu nhỏ cửa sổ xuống dưới 760 px; ở màn hình nhỏ hơn, biểu mẫu và bộ lọc chuyển thành một cột, bảng giữ cuộn ngang.

## Căn cứ hình ảnh từ frontend hiện tại

Wireframe mô phỏng shell thật hiện có, đồng thời thêm mục **Danh mục sách** như một mục đề xuất cho module v5. Tên thương hiệu, lời chào, nút đăng xuất, nhãn điều hướng học vụ và thứ tự nhóm mục được đối chiếu với nguồn hiện tại.

| Chi tiết | Mapping vào mã hiện có |
|---|---|
| Typography Roboto và token `#191c1e`, nền `#f7f9fb`, surface trắng, secondary `#f2f4f6`, outline `#c7c4d8`, primary `#4f46e5`, primary đậm `#3525ce`, secondary `#505f76`, error `#ba1a1a`, success `#047857` | `FE/src/styles/foundation.css` — `:root`, `body`, `.app-header`, `.sidebar`, `.page-content`, `.content-surface` |
| Header 64 px, thương hiệu Academic Core, lời chào, đăng xuất | `FE/src/components/common/AuthenticatedLayout.vue` — `.app-shell`, `.app-header`, `.brand`, `.header-actions`, `.welcome` |
| Sidebar 260 px, nền trắng, active `#eef2ff`, nhãn học vụ và điều hướng theo user | `FE/src/components/common/AuthenticatedLayout.vue` — `.sidebar`, `nav`, `.router-link-active`; `FE/src/views/shell/AuthenticatedV2ShellView.vue` — `navigation` |
| Khu vực nội dung tối đa 1280 px, padding 32 px; tiêu đề 28/36; surface bo góc 12 px, viền `#e2e8f0`, padding 24 px | `FE/src/styles/foundation.css` — `.page-content`, `.page-heading h1`, `.content-surface` |
| Bộ lọc theo hàng/cột, nút tìm kiếm, bảng sọc, tag và hành động trên dòng | `FE/src/views/functional-room/FunctionalRoomListView.vue`; `FE/src/views/academic/SubjectListView.vue`; `FE/src/components/academic/SubjectTable.vue` |
| Form dialog khoảng 600 px, lưới hai cột, mô tả, lỗi và nhóm nút hành động | `FE/src/components/academic/SubjectDialog.vue`; `FE/src/components/functional-room/FunctionalRoomDialog.vue` |
| Loading, empty, lỗi có retry, forbidden | `FE/src/components/common/PageState.vue`; `FE/src/components/common/EmptyState.vue`; `FE/src/components/common/FormAlert.vue` |
| Trang chi tiết: vùng nội dung chuẩn, dữ liệu dạng lưới; không tạo dashboard hoặc theme riêng | `FE/src/styles/foundation.css` — `.detail-grid`, `.detail-item`, `.section-heading`; cách bố trí từ các màn hình v2 hiện có |

## Ánh xạ sang cấu trúc dự kiến

Tên file dưới đây là mapping review theo Plan 096, chưa phải component đã tồn tại hoặc contract đã chốt.

| Phần wireframe | Thành phần dự kiến theo Plan 096 |
|---|---|
| Trang danh mục, lọc/sắp xếp/phân trang và các trạng thái | `FE/src/views/library/LibraryBookListView.vue` |
| Trang metadata và danh sách bản sao/tra cứu mã | `FE/src/views/library/LibraryBookDetailView.vue` |
| Form tạo/sửa đầu sách | `FE/src/views/library/LibraryBookFormView.vue` và/hoặc `FE/src/components/library/BookForm.vue` |
| Bảng bản sao và thao tác được phép | `FE/src/components/library/BookCopyTable.vue` |
| Sửa vị trí kệ/cờ chỉ đọc tại chỗ của bản sao | `FE/src/components/library/BookCopyTable.vue` hoặc dialog con được tách khi triển khai |
| Dialog thêm N bản sao | `FE/src/components/library/AddBookCopiesDialog.vue` |
| Xem chuỗi mã/ảnh mã vạch và nhập thủ công | `FE/src/components/library/BookBarcodeDialog.vue` |
| Dữ liệu và lỗi nếu contract được duyệt | `FE/src/types/library/catalog.ts`, `FE/src/services/library/libraryCatalogApi.ts` |
| Điều hướng shell khi role/session thực sự được contract cung cấp | `FE/src/router/index.ts`, `FE/src/views/shell/AuthenticatedV2ShellView.vue`, `FE/src/types/user.ts` |

Danh mục và bản sao trong wireframe lấy từ mock có sẵn trong trang. Bộ lọc được chạy cục bộ để người review thấy trạng thái và bố cục; nó **không** chứng minh truy vấn server, thứ tự sort backend, tổng kết quả hoặc response contract.

## Điểm còn là đề xuất cần review

Plan 096 đang ở trạng thái **DRAFT — READY FOR REVIEW; implementation NOT APPROVED**. Các con số, trường dữ liệu và hành vi sau chỉ phục vụ xem bố cục, không phải quyết định:

- Trường metadata trên form gồm ISBN, tên sách, tác giả, nhà xuất bản, năm xuất bản, thể loại, giá bìa và địa chỉ ảnh bìa; giới hạn/required/nullability, chuẩn hóa ISBN, năm hợp lệ, category taxonomy và giá trị tiền tệ chưa được chốt.
- Bộ lọc minh họa theo keyword/category/năm/availability; sort mặc định, danh sách sort cho phép, page index/page size, collation tìm kiếm và semantics tổng số lượng còn phải chốt theo API.
- Số bản sao trong form demo cho nhập 1–100 theo mức tối đa đang đề xuất trong Plan 096. Giới hạn lô được đề xuất chưa được chốt; xử lý atomic/idempotent, cách sinh barcode từ ID và giới hạn ID vẫn mở.
- Hành động trạng thái minh họa chỉ đổi `AVAILABLE` ↔ `DAMAGED`; form sửa bản sao có vị trí kệ và cờ chỉ đọc tại chỗ; rút kho/lưu trữ hiển thị bước xác nhận. Wireframe chặn lưu trữ khi còn copy `ON_LOAN` hoặc `RESERVED` như một guard cần review, không kết luận đây là policy cuối. Transition hợp lệ, guard lịch sử, restore/visibility và safe-delete policy cần review theo C4.
- Mã `LIB-...` chỉ là dữ liệu demo. Vạch barcode trang trí không theo Code128; cần renderer mã vạch thật và contract ảnh phù hợp trước khi coi là chức năng quét/in.
- Role xem trước `Bạn đọc`, `Thủ thư`, `Quản trị viên` là giả lập. Auth frontend hiện chưa có contract LIBRARIAN; ẩn nút không thay thế backend authorization. Không tự cấp role.
- Lỗi phiên bản thay đổi là bản xem trước thông báo. Cách gửi expected version, error code ổn định và tải lại/xử lý xung đột chưa được contract hóa.
- Ảnh bìa demo dùng khối màu hoặc fallback không gọi URL ngoài. Cover URL là amendment đã duyệt ở Plan 095; validation và cách trình bày khi tải lỗi cần được chốt trong contract triển khai.

Các gate F1/F7/F8/F9, C1–C5 và dependency role foundation được ghi trong [Developer Plan 096](../../../../dev-impl-plan/summary/096-library-catalog-and-book-copy-2026-10-08.md). Wireframe này không đóng gate nào.

## Phạm vi

Chỉ có hai artifact wireframe là `index.html` và `README.md`. Không thêm code ứng dụng, API, schema, migration, dependency, dữ liệu thật, quyền role hoặc quy tắc vận hành. Không có dịch vụ bên ngoài cần tải để xem trang.

## Kiểm tra bản wireframe

- JavaScript syntax, duplicate IDs, CSS parser và independent DOM interaction checks: PASS.
- Đã render bằng Chrome và xem ảnh desktop 1440×1100, mobile 390×1100, chi tiết đầu sách, dialog tạo sách và thêm bản sao: PASS trong các view này. State previews còn lại được kiểm trên DOM, không coi là browser automation/live API proof.
- Source mapping/style review dùng FE hiện tại; application đang chạy chưa được kiểm vì phiên này chỉ tạo prototype tài liệu. Khi triển khai cần reuse component/style thật và so sánh màn Library với các màn academic/functional-room trên cùng viewport trước UI sign-off.
