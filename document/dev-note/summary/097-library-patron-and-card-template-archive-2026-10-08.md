# Dev Note 097 — Wireframe quản lý cấp phát, thu hồi thẻ thư viện và mẫu thẻ CR-80

## Kế hoạch liên quan và trạng thái phê duyệt

- Kế hoạch: [`document/dev-impl-plan/summary/097-library-patron-and-card-2026-10-08.md`](../../dev-impl-plan/summary/097-library-patron-and-card-2026-10-08.md).
- Trạng thái kế hoạch 097: `CHƯA PHÊ DUYỆT — Chỉ lập kế hoạch, chưa triển khai`.
- Phạm vi hoàn thành trong phiên này: Thực hiện theo yêu cầu trực tiếp của người dùng — Kiểm tra hiện trạng wireframe tính năng quản lý cấp phát/thu hồi thẻ thư viện; xác định chưa có hệ thống quản lý hoàn chỉnh; triển khai wireframe tương tác độc lập (self-contained HTML5/CSS/JS) chuẩn Academic Core; tích hợp mẫu thẻ chuẩn CR-80 và cập nhật đầy đủ tài liệu README. Không can thiệp mã nguồn backend hoặc cơ sở dữ liệu.

## Các nội dung đã thực hiện

1. **Kiểm tra hiện trạng wireframe:**
   - Xác nhận trước đó thư mục `document/wireframes/fe/library/097-library-patron-and-card/` mới chỉ có bản dựng preview đơn lẻ mẫu thẻ cá nhân và ảnh `sample-library-card.jpg`, **chưa có** wireframe quản lý cấp phát/thu hồi thẻ thư viện hoàn chỉnh cho Thủ thư/Admin.
2. **Triển khai wireframe tương tác độc lập chất lượng cao (`index.html`):**
   - **Shell Academic Core:** Tích hợp đồng nhất với giao diện chuẩn (header 64px, brand Academic Core, thanh bên sidebar 260px với menu học vụ và mục *Bạn đọc & Thẻ thư viện*, typography Roboto, palette token `#4f46e5`, `#191c1e`, `#f7f9fb`).
   - **Thanh điều khiển xem trước (Demo Rail):**
     - Đổi vai trò xem trước: `Thủ thư (LIBRARIAN)`, `Quản trị viên (ADMIN)`, `Bạn đọc đã đăng nhập (STUDENT / TEACHER)`.
     - Đổi trạng thái hiển thị: `Bình thường (Normal)`, `Đang tải (Loading)`, `Không có kết quả (Empty)`, `Lỗi kết nối (Error 500)`, `Không có quyền (Forbidden 403)`, `Xung đột phiên bản (Conflict 409)`.
     - Đổi tab chế độ xem nhanh: *Quản lý thẻ & bạn đọc*, *Thẻ thư viện của tôi*, *Đối chiếu mẫu gốc (CR-80)*.
   - **Giao diện quản lý bạn đọc & thẻ (Thủ thư / Admin):**
     - Hàng thẻ KPI thống kê nhanh: Tổng bạn đọc, Thẻ đang hoạt động, Chưa cấp thẻ/Hết hạn, Thẻ đã thu hồi.
     - Bộ lọc đa tiêu chí: Tìm kiếm theo từ khóa (tên, mã thẻ, mã độc giả, mã HS/GV), lọc theo đối tượng (Học sinh/Giáo viên), trạng thái bạn đọc (`ACTIVE`, `BORROWING_SUSPENDED`, `CLOSED`), trạng thái thẻ (`ACTIVE`, `EXPIRED`, `REVOKED`, Chưa cấp thẻ).
     - Quick filter tabs: *Tất cả*, *Có thẻ hiệu lực*, *Chưa cấp thẻ*, *Đã thu hồi / Hết hạn*, *Bị đình chỉ mượn*.
     - Chọn dòng bảng đơn lẻ và hàng loạt (Bulk bar) để cấp thẻ hoặc in thẻ hàng loạt.
     - Bảng dữ liệu phân trang chuẩn `ResultPaginationDTO`.
   - **Các modal nghiệp vụ hoàn chỉnh:**
     - `issueCardModal`: Phát hành thẻ mới, chọn chính sách thời hạn (12 tháng mặc định, hết năm học, 24 tháng, tùy chọn), tự động sinh mã thẻ duy nhất `LIB-2026-000...`, kiểm tra và chặn lỗi `CARD_ALREADY_ACTIVE` nếu bạn đọc đã có thẻ hoạt động.
     - `revokeCardModal`: Thu hồi thẻ kèm lý do bắt buộc (Mất thẻ, Thẻ hỏng, Chuyển trường, Vi phạm quy chế, Cấp đổi...), hiển thị cảnh báo vô hiệu hóa vĩnh viễn và đóng dấu ấn `ĐÃ THU HỒI` màu đỏ.
     - `reissueCardModal`: Quy trình cấp lại thẻ — thu hồi an toàn thẻ cũ và sinh thẻ mới có mã và chữ ký mới.
     - `suspendPatronModal`: Tạm đình chỉ hoặc gỡ đình chỉ quyền mượn sách kèm lý do; thể hiện rõ nguyên tắc không khóa tài khoản trường học `app_user` hay ảnh hưởng nghiệp vụ học vụ.
     - `qrDetailModal`: Xem mã QR phóng to, chi tiết chuỗi payload chữ ký HMAC-SHA256 (`v1|{cardNo}|{patronId}|{expEpochDay}|{signature}`), nút sao chép mã.
     - `verifyModal`: Giả lập quét và xác thực QR 6 bước theo đặc tả `POST /api/v2/library-cards/verify` (Phân tích cấu trúc -> Kiểm tra chữ ký HMAC -> Kiểm tra hạn dùng -> Tra cứu DB -> Kiểm tra thẻ ACTIVE -> Kiểm tra điều kiện mượn của bạn đọc).
     - `printBatchModal`: Xem trước lưới in phôi thẻ trên giấy A4 hoặc in thẻ nhựa CR-80; tích hợp CSS `@media print` chuyên dụng tự động ẩn shell và căn chỉnh lề in chuẩn.
     - `activatePatronModal`: Kích hoạt hồ sơ bạn đọc mới cho tài khoản người dùng chưa có hồ sơ.
   - **Màn hình Bạn đọc tự xem thẻ (`LibraryMyCardView`):**
     - Tự động hiển thị khi chọn vai trò Bạn đọc: hiển thị thẻ số cá nhân với mã QR động, thông báo điều kiện mượn sách, hướng dẫn sử dụng và nút báo mất thẻ.
   - **Đối chiếu mẫu thẻ gốc:**
     - Giữ nguyên chế độ so sánh song song giữa bản dựng HTML5/CSS3 và tệp ảnh mẫu gốc `sample-library-card.jpg`.
3. **Cập nhật tài liệu `README.md` của wireframe:**
   - Hoàn thiện tài liệu [`document/wireframes/fe/library/097-library-patron-and-card/README.md`](../../wireframes/fe/library/097-library-patron-and-card/README.md) phản ánh đầy đủ kiến trúc, cách xem và trải nghiệm, quy cách thẻ CR-80, ánh xạ component Vue 3 TypeScript và các quy tắc nghiệp vụ cốt lõi.

## Danh sách tệp liên quan

| Tệp | Thay đổi |
|:---|:---|
| `document/wireframes/fe/library/097-library-patron-and-card/index.html` | Bản wireframe tương tác hoàn chỉnh quản lý cấp phát, thu hồi, in ấn và xác thực thẻ |
| `document/wireframes/fe/library/097-library-patron-and-card/README.md` | Tài liệu đặc tả wireframe, quy cách CR-80 và ánh xạ thành phần Vue 3 |
| `document/wireframes/fe/library/097-library-patron-and-card/sample-library-card.jpg` | Ảnh mẫu thẻ thư viện gốc phục vụ đối chiếu |
| `document/dev-note/summary/097-library-patron-and-card-template-archive-2026-10-08.md` | Dev Note cập nhật kết quả triển khai |

## Kiểm tra và xác nhận (Validation)

- **Cú pháp JavaScript:** Kiểm tra bằng Node.js `new Function(code)`: `PASS`, không có lỗi cú pháp.
- **Tính duy nhất của HTML ID:** Quét toàn bộ 140 phần tử `id`: `PASS`, không có ID trùng lặp.
- **Kiểm tra truy xuất DOM:** Quét toàn bộ 154 lệnh `$('...')`: `PASS`, 100% phần tử tồn tại trong DOM.
- **Tính độc lập (Self-contained):** Chạy offline hoàn toàn, không phụ thuộc font mạng hay CDN bên ngoài.
- **Hỗ trợ in ấn:** CSS `@media print` căn chỉnh chuẩn kích thước ISO CR-80 (85.6mm × 54mm) và khổ A4.
- `git diff --check`: PASS, không có lỗi định dạng hay khoảng trắng thừa.
