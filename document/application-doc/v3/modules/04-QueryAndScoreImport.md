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
version. File không được chọn cột đích. Preview phải chỉ rõ dòng hợp lệ/lỗi; commit chỉ
ghi vào column đã chọn và phải atomic theo chính sách đã duyệt.

## Contract boundary

Plan 077 chốt query envelope/filter names. Plan 078 chốt file format, student identity,
rounding/status mapping, row error model, size limits và partial-commit policy. Endpoint,
DTO và enum chưa được chốt đều là `TBD`.
