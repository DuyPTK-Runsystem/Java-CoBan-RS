# Wireframe Plan 074 — Xếp lớp theo quy tắc

## Mục đích

Wireframe tĩnh cho **bước review kết quả** của vertical slice Plan 074 v3: xem kết quả/giải thích/cảnh báo rồi xác nhận. Đây là prototype review, không gọi backend và không phải màn hình production. Màn cấu hình/tạo phiên production được mô tả chi tiết trong Plan 074 FE; file HTML này không phải bằng chứng rằng route/view tạo phiên đã được triển khai.

Mở [`index.html`](index.html) để xem. Chọn **Trạng thái xem thử**:

- `Sẵn sàng xác nhận`: preview minh họa không có lỗi chặn.
- `Có dữ liệu thiếu`: đánh dấu học sinh `Cần xếp thủ công`; vẫn cho xác nhận phần kết quả tự động.
- `Xung đột phiên bản`: giữ review state, yêu cầu tải lại; không retry mù.
- `Không có quyền`: giữ context/phiên đăng nhập; action bị khóa.
- `Đã xác nhận`: kết quả read-only.

## Điều wireframe thể hiện

- Context scope, lớp đích/capacity, profile `Nâng cao`/`Hỗ trợ`/`Thường` và tỷ lệ giới tính mục tiêu theo khối.
- Preview theo lớp, allocation rationale từng học sinh, `Đã xếp tự động`, `Cần xếp thủ công` và lỗi chặn capacity.
- Confirm chỉ xuất hiện khi session ở `READY_FOR_CONFIRM`; lifecycle/audit/version do backend quyết định.
- Capacity vượt limit là lỗi chặn **chỉ** cho auto-placement Plan 074. Giáo vụ vẫn dùng xếp/chuyển thủ công v2 theo semantics warning hiện có.

## Không được suy diễn từ mẫu

Điểm minh họa, capacity `35` và học sinh trong mẫu chỉ phục vụ layout. `SCORE` và `GENDER` là catalog đã được duyệt; `SCORE` phải kèm nguồn snapshot, không dùng trường trung bình cố định trên Student. Candidate có thể là lên lớp, nhập học mới (kể cả L8) hoặc lưu ban. Auto-placement vượt capacity mới là lỗi chặn; thiếu dữ liệu hoặc bằng điểm ở ngưỡng capacity là `Cần xếp thủ công`.

## Điểm cần user review

1. Luồng draft → mô phỏng → review → xác nhận phần tự động có rõ không?
2. Profile lớp, tỷ lệ giới tính mục tiêu và drawer chi tiết có đủ giúp hiểu lý do xếp không?
3. Có phân biệt rõ `Cần xếp thủ công` với lỗi chặn do vượt capacity không?
4. P4 đã chốt `ADMIN` và `ACADEMIC_OFFICE` có toàn bộ thao tác Plan 074; state `Không có quyền` cho `TEACHER`/`STUDENT` có rõ không?

## Liên kết

- [Plan 074 BE](../../../../dev-impl-plan/be/enrollment/074-rule-based-class-placement-2026-09-10.md)
- [Plan 074 FE](../../../../dev-impl-plan/fe/enrollment/074-rule-based-class-placement-ui-2026-09-10.md)
- [Module Placement v3](../../../../application-doc/v3/modules/01-PlacementAndEnrollment.md)
