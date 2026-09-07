# Wireframe Plan 057 — Scorebook & Assessment Column UI

## Mục đích

Bộ wireframe tĩnh để review Developer Plan 057. Nội dung
mô phỏng workspace scorebook gồm:

- context năm học, học kỳ, lớp và môn/lớp;
- lifecycle `DRAFT`, `OPEN`, `PUBLISHED`, `CLOSED`;
- score grid với assessment columns động;
- dialog nhập điểm đơn và bulk theo một cột;
- cấu hình create/edit/deactivate assessment column;
- state minh họa empty, forbidden và optimistic conflict.

Wireframe dùng fixture tại chỗ, không gọi backend, không thay thế Vue/PrimeVue
production code và không có nghĩa production implementation đã hoàn tất.

## Cách xem

Mở [`index.html`](index.html) bằng trình duyệt. Có thể:

1. Chuyển tab `Bảng điểm` và `Cấu hình cột`.
2. Bấm một score cell để xem dialog nhập điểm.
3. Bấm `Nhập hàng loạt` để xem bulk dialog.
4. Bấm `Thêm cột`, `Sửa` hoặc `Vô hiệu hóa` để review column interactions.
5. Dùng `Trạng thái demo` để chuyển giữa `Bình thường`, `Trống`, `403` và `409`.

## Điểm cần review

- Một workspace hay tách riêng score grid và assessment-column configuration.
- Thứ tự context: năm học → học kỳ → lớp → môn/lớp.
- Vị trí lifecycle actions `Mở sổ`/`Công bố`.
- Cách phân biệt “Chưa nhập”, điểm `0.0`, `ABSENT`, `EXEMPTED`, `CANCELLED`.
- Cell editor bằng dialog và bulk editor theo một assessment column.
- Cách nhóm `KTTT`, `KTĐK`, `KTCK` và cảnh báo cấu trúc trước publish.
- Responsive table scroll và dialog trên viewport nhỏ.
- Trạng thái `403`/`409` không làm mất authenticated session.

## Giới hạn contract được thể hiện

- Canonical wire `AssessmentType` là `KTTT | KTĐK | KTCK`.
- API `ScoreStatus` không có `NOT_ENTERED`; ô thiếu entry chỉ được trình bày là
  “Chưa nhập”.
- Điểm `0.0` là dữ liệu hợp lệ.
- `expectedVersion` được thể hiện trong dialog edit để review optimistic conflict.
- Navigation/action visibility là minh họa tĩnh; backend assignment scope vẫn là
  authorization boundary.
- Wireframe không tính bất kỳ official average nào.

## Liên kết

- Developer Plan draft:
  [`057-scorebook-assessment-column-ui-2026-09-01.md`](../../../../dev-impl-plan/fe/scorebook/057-scorebook-assessment-column-ui-2026-09-01.md)
