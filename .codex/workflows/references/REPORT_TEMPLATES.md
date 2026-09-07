# REPORT_TEMPLATES.md

## 1. Báo cáo trước khi triển khai

```text
## Báo cáo trước khi triển khai

### Cấu trúc hiện tại
- ...

### Feature/module sẽ thực hiện
- ...

### Phạm vi ảnh hưởng
- Module/component: ...
- File/class/method: ...
- API/database/integration: ...

### Output dự kiến
- ...

### Rủi ro
- ...

### Validation dự kiến
- Checkstyle: ...
- PMD: ...
- Build: ...
- Test: ...

### Trạng thái
- Chờ người dùng phê duyệt bằng tin nhắn qua agent trước khi code.
```

## 2. Báo cáo cuối

```text
## Kết quả triển khai

### Tóm tắt
- Requirement đã thực hiện: ...
- Output đạt được: ...

### Thay đổi mã nguồn
- `path/to/file`: ...

### Validation
- Checkstyle: PASS / FAIL / NOT RUN
- PMD: PASS / FAIL / NOT RUN
- Build: PASS / FAIL / NOT RUN
- Test: PASS / FAIL / NOT RUN

### So với Developer Plan
- Đúng plan / Sai lệch: ...

### Vấn đề còn lại
- Không có / ...

### Bước tiếp theo
- ...
```

## 3. Báo cáo khi dừng sau giới hạn debug

```text
## Dừng sau giới hạn debug

- Số vòng đã dùng: 10/10
- Lỗi hiện tại: ...
- Nguyên nhân đã xác định: ...
- Phương án đã thử: ...
- Kết quả quan trọng: ...
- Thay đổi đã thực hiện: ...
- Phần chưa hoàn thành: ...
- Thông tin còn thiếu: ...
- Bước tiếp theo đề xuất: ...
```

## 4. Quy tắc trạng thái
Chỉ dùng:
- `PASS`: đã chạy và thành công.
- `FAIL`: đã chạy và thất bại.
- `NOT RUN`: chưa chạy hoặc không thể chạy.

Không dùng từ ngữ mơ hồ như “có vẻ ổn”, “chắc là pass” hoặc “đã xử lý” nếu chưa có bằng chứng validation.
