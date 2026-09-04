# Wireframe Plan 062 — Retake Result UI

## Mục đích

Wireframe tĩnh, deterministic để review cùng Developer Plan 062 trước khi triển khai Vue production. Wireframe không gọi backend, không đại diện cho contract mới và không có nghĩa Plan 062 đã được approved.

## Cách xem

Mở [`index.html`](index.html) bằng trình duyệt. Dùng dropdown **Trạng thái demo** để xem:

- `Normal`: danh sách retake với filter, before/after score, status và calculation notice.
- `Empty`: không có record phù hợp bộ lọc.
- `Validation`: dialog nhập điểm có lỗi range/scale và note quá dài.
- `Forbidden`: `403`, giữ session và hiển thị access denied.
- `Not found`: `404` khi mở detail đã mất.
- `Conflict`: `409`, nêu duplicate/lifecycle conflict và yêu cầu refresh.
- `In progress`: calculation đang `IN_PROGRESS`, after-score chưa được coi là official mới nhất.

Các nút trong normal state mở dialog tạo record, nhập/sửa score và hủy record. Dialog score hiển thị `preRetakeScore` trước thi lại và `retakeScore` sau thi lại; official after-score/calculation fields được đánh dấu là dữ liệu đọc từ Transcript API, không phải Retake API response.

## Điểm cần duyệt

- Filter theo student/year/subject/status và phân trang server-side 10 dòng/trang.
- Bảng phân biệt `PLANNED`, `SCORED`, `CANCELLED`; điểm `0.0` vẫn là điểm hợp lệ.
- Dialog create có thể tạo `PLANNED`; create `SCORED`, update và cancel chỉ enable theo capability/backend policy.
- Before/after score và cảnh báo calculation không tự tính ở FE.
- Empty, validation, `401`/`403`/`404`/`409` và `IN_PROGRESS` phải là state có thiết kế rõ.
- Responsive: bảng cuộn ngang, dialog không mất footer action trên viewport hẹp.

## Contract blockers được phản ánh

- Backend DTO hiện chỉ có IDs cho student/year/subject, nên tên trong fixture chỉ là mock display data; không coi là API field.
- Retake response chưa có `officialDtbmhCn`, `calculationSource`, `calculationStatus` hoặc `lastCalculationTaskId`; phần này phải lấy từ Transcript API sau khi có context contract.
- Backend Plan 045 ghi role `ADMIN`/`ACADEMIC_OFFICE`, nhưng FE docs đang có drift về việc JWT đã expose role hay chưa. Không dùng wireframe để chốt role discovery.
- Wire values dùng `PLANNED | SCORED | CANCELLED`, score `0.0..10.0`, max 1 decimal, date-only `yyyy-MM-dd`.

## Liên kết

- Developer Plan: [`062-retake-result-ui-2026-09-03.md`](../../../../dev-impl-plan/FE/scorebook/062-retake-result-ui-2026-09-03.md)
- Backend Plan 045: [`045-retake-foundation-and-calculation-integration-2026-08-26.md`](../../../../dev-impl-plan/BE/scorebook/045-retake-foundation-and-calculation-integration-2026-08-26.md)
