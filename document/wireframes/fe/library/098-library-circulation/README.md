# Plan 098 — Wireframe Lưu thông thư viện

Wireframe này minh họa circulation core của Plan 098 trên cùng visual language với các wireframe Library trước đó. Tệp HTML tự chứa CSS và JavaScript, chạy offline bằng cách mở trực tiếp index.html.

## Phạm vi wireframe

- Quầy lưu thông: nhận diện patron/card, thêm barcode, kiểm tra eligibility và xác nhận borrow.
- Demo upload ảnh: chọn ảnh QR user/thẻ hoặc barcode sách, FE decode thành chuỗi rồi đổ vào ô tra cứu tương ứng. Không cần camera hay thiết bị scan thật.
- Các thao tác return, renewal và lost được mô phỏng từ cùng workspace.
- Lịch sử loan: due date, renewal count, overdue/fine state.
- Hàng đợi reservation: FIFO, WAITING/READY, allocation và pickup due.
- Policy editor: max active loans, loan duration, renewal limit/duration, pickup window, số lượng fine tiers tùy ý, fine cap và suspension threshold.
- Preview state: loading, empty, error, forbidden và conflict.
- Role switcher: LIBRARIAN, ADMIN và STUDENT để xem khác biệt capability.

## Cách trải nghiệm

1. Mở index.html trong trình duyệt, không cần dev server hoặc dependency mạng.
2. Ở thanh điều khiển trên cùng, đổi role hoặc trạng thái dữ liệu.
3. Ở sidebar, chuyển giữa Quầy lưu thông, Lịch sử loan, Hàng đợi reservation và Chính sách.
4. Tại quầy, bấm Tra cứu, thêm barcode rồi thử các nút Tạo loan, Xác nhận trả, Gia hạn loan đang chọn và Đánh dấu mất.
5. Ở khối Demo FE, chọn ảnh QR hoặc barcode. Prototype đọc file ở trình duyệt và đổ chuỗi mẫu đã decode vào input; implementation thật sẽ thay adapter bằng decoder QR/Code128 ở FE.
6. Trong Chính sách, sửa các con số, bấm Thêm bậc hoặc Xóa bậc để thay đổi số lượng fine tiers, rồi bấm Lưu version mới. Quay lại quầy để thấy due date, giới hạn loan và renewal thay đổi.
7. Bấm Cấp copy tiếp theo để mô phỏng chuyển phần tử đầu FIFO từ WAITING sang READY.

## Ánh xạ với sản phẩm dự kiến

| Wireframe | Component/view dự kiến |
| --- | --- |
| Quầy lưu thông | LibraryCirculationView.vue |
| Patron lookup và barcode workflow | PatronLookupPanel.vue, CirculationBarcodeInput.vue |
| Upload scan demo | ScannerUploadDemo.vue |
| Tóm tắt phiên và mutation actions | CirculationSummaryPanel.vue |
| Lịch sử loan | LibraryLoanHistoryView.vue |
| Reservation queue | LibraryReservationQueueView.vue |
| Policy editor | LibraryPolicyView.vue, LibraryPolicyForm.vue |
| Typed transport | libraryCirculationApi.ts, libraryReservationApi.ts, libraryFineApi.ts |

## Policy amendment

Plan 098 ghi nhận các con số hiện tại là default policy, không phải hằng số:

- max active loans: 5
- loan duration: 14 ngày
- max renewals: 2
- renewal duration: 7 ngày
- reservation pickup: 3 ngày
- fine tiers: số lượng bậc không cố định; mặc định wireframe minh họa 5.000 / 10.000 / 20.000 VND theo các bậc ngày
- fine cap: 500.000 VND
- suspension threshold: 500.000 VND

Mỗi lần lưu trong wireframe tăng policy version và mô phỏng audit. Đây là prototype tài liệu; nó không gọi API, không ghi DB và không chứng minh authorization hoặc concurrency.

Ảnh upload chỉ dùng ở phía FE demo. Chuỗi sau decode mới là dữ liệu được đưa vào flow; ảnh không được gửi lên backend.

## Quy tắc visual

Wireframe giữ các convention của Library shell:

- Header 64px, sidebar 246px, nền #f7f9fb, surface trắng, border #e2e8f0.
- Typography sans-serif hệ thống, màu primary indigo hiện có, trạng thái success/warning/error phân biệt bằng cả màu và nhãn.
- Bố cục ưu tiên scanning tại quầy, bảng có overflow ngang trên màn hình nhỏ, summary panel sticky trên desktop.
- Các nút mutation có trạng thái disabled cho role STUDENT; backend vẫn là authority ở implementation.
