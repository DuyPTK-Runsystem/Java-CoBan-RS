# Attendance and Subject Module

## 12. Điểm danh — Attendance

### 12.1. Phạm vi

- `BR-ATTENDANCE-001`: Điểm danh theo buổi.
- `BR-ATTENDANCE-002`: Một ngày có hai buổi: sáng và chiều.
- `BR-ATTENDANCE-003`: Học sinh không tự điểm danh.
- `BR-ATTENDANCE-004`: Hệ thống lưu ngoại lệ thay vì tạo một bản ghi `PRESENT` cho mọi học sinh.

### 12.2. Nguyên tắc mặc định

`BR-ATTENDANCE-005`: Nếu học sinh đang thuộc lớp, buổi đó là buổi học hợp lệ và không có bản ghi ngoại lệ thì trạng thái được suy ra là `PRESENT`.

```text
Không có dữ liệu ngoại lệ + buổi học hợp lệ = PRESENT
```

### 12.3. Trạng thái ngoại lệ

| Trạng thái | Ý nghĩa |
|---|---|
| `EXCUSED_ABSENCE` | Vắng có phép |
| `UNEXCUSED_ABSENCE` | Vắng không phép |
| `LATE` | Đi trễ |
| `EARLY_LEAVE` | Về sớm |

### 12.4. Dữ liệu ngoại lệ

- Học sinh và lớp tại thời điểm điểm danh.
- Ngày học và buổi học.
- Trạng thái ngoại lệ.
- Thời gian đến hoặc rời trường nếu có.
- Lý do, ghi chú.
- Người nhập và audit metadata.

### 12.5. Functional requirements

- `FR-ATTENDANCE-001`: GVCN xem danh sách điểm danh theo ngày và buổi.
- `FR-ATTENDANCE-002`: Hệ thống mặc định học sinh có mặt.
- `FR-ATTENDANCE-003`: GVCN nhập các trường hợp ngoại lệ.
- `FR-ATTENDANCE-004`: GVCN cập nhật hoặc xóa ngoại lệ trong phạm vi quyền.
- `FR-ATTENDANCE-005`: Giáo vụ điều chỉnh dữ liệu khi cần.
- `FR-ATTENDANCE-006`: Học sinh xem lịch sử chuyên cần của mình.
- `FR-ATTENDANCE-007`: Thống kê số buổi vắng, đi trễ và về sớm.
- `FR-ATTENDANCE-008`: Báo cáo chuyên cần theo học sinh, lớp, khối và thời gian.

### 12.6. Business rules

- `BR-ATTENDANCE-006`: Không có dữ liệu chỉ được hiểu là `PRESENT` đối với buổi học hợp lệ.
- `BR-ATTENDANCE-007`: Không tính có mặt trước ngày học sinh vào lớp hoặc sau ngày rời lớp.
- `BR-ATTENDANCE-008`: Mỗi học sinh có tối đa một ngoại lệ chính trong một buổi.
- `BR-ATTENDANCE-009`: GVCN chỉ quản lý điểm danh lớp mình.
- `BR-ATTENDANCE-010`: Học sinh chỉ được xem dữ liệu của bản thân.
- `BR-ATTENDANCE-011`: Báo cáo chuyên cần dùng số buổi học hợp lệ làm mẫu số.
- `BR-ATTENDANCE-012`: Đơn xin nghỉ và quyền phụ huynh thuộc giai đoạn sau.

## 13. Metadata môn học — Subject

### 13.1. Dữ liệu tối thiểu

- Mã và tên môn.
- Loại môn: `ACADEMIC` hoặc `SKILL`.
- Khối áp dụng.
- Học kỳ được giảng dạy.
- Trạng thái hoạt động.
- Có tính vào điểm trung bình hay không.

### 13.2. Functional requirements

- `FR-SUBJECT-001`: Giáo vụ tạo và cập nhật môn học.
- `FR-SUBJECT-002`: Giáo vụ cấu hình loại môn `ACADEMIC` hoặc `SKILL`.
- `FR-SUBJECT-003`: Giáo vụ cấu hình khối và học kỳ áp dụng.
- `FR-SUBJECT-004`: Giáo vụ kích hoạt hoặc ngừng sử dụng môn học.
- `FR-SUBJECT-005`: Giáo viên và học sinh xem danh sách môn thuộc phạm vi của mình.

### 13.3. Business rules

- `BR-SUBJECT-001`: Môn thông thường có thể học một hoặc hai học kỳ.
- `BR-SUBJECT-002`: Môn kỹ năng là optional và chỉ tổng kết trong một học kỳ duy nhất.
- `BR-SUBJECT-003`: Môn kỹ năng không được tính vào `Đtbhk`, `Đtbcn`, xếp hạng hoặc xếp loại.
- `BR-SUBJECT-004`: Chỉ được phân công môn đã được cấu hình cho khối của lớp.

