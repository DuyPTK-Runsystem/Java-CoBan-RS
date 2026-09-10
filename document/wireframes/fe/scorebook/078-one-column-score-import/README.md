# Wireframe Plan 078 — Bổ sung import file cho Nhập điểm hàng loạt v2

## Mục đích

Wireframe tĩnh để review Plan 078 theo hướng mở rộng trực tiếp
`BulkScoreEntryDialog` của v2. Không tạo workspace import riêng.

Luồng chính:

1. Người dùng đang ở dialog `Nhập điểm hàng loạt` của một cột điểm.
2. Chuyển sang chế độ `Dùng file mẫu`.
3. Tải file mẫu có sẵn toàn bộ danh sách học sinh của lớp.
4. Điền `scoreStatus`, `scoreValue`, `note` rồi upload lại.
5. Xem preview và bấm `Lưu hàng loạt` bằng cùng bulk add/update flow v2.

Wireframe dùng fixture tại chỗ, không gọi backend, không thay thế Vue/PrimeVue
production code và chưa có nghĩa Plan 078 đã được phê duyệt.

## Cách xem

Mở [`index.html`](index.html) bằng trình duyệt. Dùng `Trạng thái xem thử` để xem:

- `Bình thường`: template đã tải, preview hợp lệ và có thể lưu.
- `Chưa tải file mẫu`: upload bị khóa cho tới khi tải mẫu.
- `Có lỗi theo dòng`: nút `Lưu hàng loạt` bị khóa.
- `Xung đột dữ liệu`: giữ preview và yêu cầu tải lại.
- `Không có quyền`: giữ dialog/context, không xóa phiên đăng nhập.

Tab `Nhập trực tiếp` mô phỏng phần bulk v2 hiện có; tab `Dùng file mẫu` là phần
bổ sung của Plan 078.

## Điểm cần review

- Vị trí mode switch trong `BulkScoreEntryDialog`.
- Nút tải mẫu có đủ nổi bật và có nói rõ số học sinh hay chưa.
- Upload có bị khóa đúng trước khi file mẫu được tải hay chưa.
- File mẫu có nên hiển thị current status/current score như cột tham chiếu hay không.
- Cách phân biệt vùng tham chiếu với vùng người dùng điền.
- Preview có đủ thông tin để review tạo mới/cập nhật/status hay chưa.
- Nút lưu có giữ đúng ngữ nghĩa `Lưu hàng loạt` của v2 hay không.
- Khả năng đọc dialog và bảng trên viewport hẹp.

## Template được thể hiện

```text
studentCode | studentName | currentStatus | currentScore | scoreStatus | scoreValue | note
```

`studentCode`, `studentName`, `currentStatus`, `currentScore` là thông tin roster/tham
chiếu; người dùng điền ba cột còn lại. Template chứa toàn bộ roster của lớp, không chỉ
những học sinh đang nằm trên page hiện tại.

## Contract được thể hiện

- Một lần bulk chỉ gắn với một `assessmentColumnId` đã chọn trong v2.
- File không được chọn cột điểm đích.
- Preview bắt buộc trước `Lưu hàng loạt`.
- Commit dùng lại bulk add/update contract v2.
- `0`, `ABSENT`, `EXEMPTED`, `CANCELLED` giữ semantics v2.
- Dòng không thay đổi được bỏ qua; lỗi theo dòng chặn toàn bộ thao tác lưu.

## Liên kết

- [Plan 078 BE](../../../../dev-impl-plan/be/scorebook/078-one-column-score-import-2026-09-10.md)
- [Plan 078 FE](../../../../dev-impl-plan/fe/scorebook/078-one-column-score-import-ui-2026-09-10.md)
