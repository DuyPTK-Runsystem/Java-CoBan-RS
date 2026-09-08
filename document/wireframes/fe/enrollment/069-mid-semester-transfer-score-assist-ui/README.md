# Wireframe Plan 069 — Hỗ trợ chuyển điểm khi chuyển lớp giữa học kỳ

## Mục đích

Wireframe tĩnh để review Developer Plan 069. Kịch bản chính là học sinh `HS011 - Nguyễn An` có điểm trong `HK1`, đang chuyển từ lớp `6A1` sang `6A2`.

Preview mô phỏng:

- thông tin transfer và lớp nguồn/lớp đích;
- popup evidence điểm cũ cạnh cột điểm lớp mới;
- source score read-only, target score editable;
- mapping đã gợi ý, cột chưa ghép, ô chưa nhập và điểm `0.0`;
- validation, `409 Conflict`, không có điểm và loading.

## Cách xem

Mở [`index.html`](index.html) bằng trình duyệt. Chọn trạng thái ở thanh trên; bấm `Gợi ý từ điểm cũ` để xem draft target được điền; bấm `Xác nhận chuyển lớp` để xem màn hình xác nhận.

Wireframe chỉ dùng fixture tại chỗ, không gọi backend và không có nghĩa production implementation đã hoàn tất.

## Điểm cần review

- Popup nên mở ngay sau khi chọn lớp đích hay sau nút `Tiếp tục` trong transfer form.
- Có nên cho phép gợi ý/copy theo từng ô hay thêm `Gợi ý tất cả ô đã ghép`.
- Mapping theo môn + loại cột + số thứ tự có đủ rõ không; cột khác mapping phải được xử lý thế nào.
- Nút cuối nên là `Xác nhận chuyển lớp và lưu điểm mới` với semantics nguyên tử phía backend.
- Bố cục hai bảng trên desktop và overflow ngang trên viewport hẹp.

## Giới hạn contract được thể hiện

- Đây là đề xuất UX; assist endpoint, mapping DTO và mutation nguyên tử chưa tồn tại trong contract hiện tại.
- Điểm cũ chỉ là bằng chứng read-only; không tự động coi là điểm lớp mới.
- Missing score là `Chưa nhập`, khác với score `0.0`.
- Wireframe không tính `Đtbmh`/`Đtbhk` và không suy diễn authorization.

Liên kết: [Developer Plan 069](../../../../dev-impl-plan/FE/enrollment/069-mid-semester-transfer-score-assist-ui-2026-09-06.md).
