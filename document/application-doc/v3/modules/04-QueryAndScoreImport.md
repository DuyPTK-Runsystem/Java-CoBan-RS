# Module 04 — Search, Filter and One-column Score Import

## Query contract

Danh sách và bảng điểm dùng query có `search`, filter typed, sort allow-list, page/pageSize
và total. Filter phải chạy ở backend khi endpoint phân trang. FE giữ query state trong URL
chỉ khi contract của module cho phép.

`0` là giá trị điểm hợp lệ; missing score không được biến thành `0`.

## Score import lifecycle

```text
UPLOADED -> PARSED -> VALIDATED -> PREVIEWED -> COMMITTED
                         \-> REJECTED
```

Import context bắt buộc gồm `scorebookId`, `assessmentColumnId`, file metadata và expected
version. Phiên bản đầu nhận file `.xlsx`; file không được chọn cột đích và một lần upload
chỉ được xử lý một `assessmentColumnId`. Preview là bắt buộc trước khi xác nhận; preview
phải chỉ rõ dòng hợp lệ/lỗi, thông tin học sinh và giá trị cũ/mới khi ô đã có điểm. Commit
chỉ ghi vào column đã chọn, có thể update giá trị hiện có, và phải atomic theo chính sách
đã duyệt.

Quy ước nghiệp vụ đã cung cấp cho ô điểm là `0–10` cho `SCORED`, `11` cho `ABSENT` và
`12` cho `EXEMPTED`. Các giá trị ngoài quy ước không được âm thầm chuyển thành điểm.

## Contract boundary

Plan 077 chốt query envelope/filter names. Plan 078 chốt template/header mapping, student
identity, rounding/status mapping đầy đủ, row error model, size limits, blank-cell/
`CANCELLED` semantics và partial-commit policy. Endpoint, DTO và enum chưa được chốt đều
là `TBD`.
