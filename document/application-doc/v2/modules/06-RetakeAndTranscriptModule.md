# Retake and Transcript Module

## 21. Thi lại cuối năm

### 21.1. Phạm vi

`BR-RETAKE-001`: Sau khi kết thúc năm học, học sinh có thể phải thi lại một hoặc nhiều môn để đủ điều kiện lên lớp. Điều kiện xác định môn phải thi lại thuộc CR về xếp loại và lên lớp.

Với mỗi bộ:

```text
{Học sinh, Môn học, Năm học}
```

`BR-RETAKE-002`: Với mỗi bộ `{Học sinh, Môn học, Năm học}`, học sinh chỉ có tối đa một lần thi lại hợp lệ và một `Đtlmh`.

### 21.2. Dữ liệu thi lại

- Học sinh.
- Môn học.
- Năm học.
- Ngày thi.
- `ĐtbmhCN` trước thi lại.
- `Đtlmh`.
- `ĐtbmhCN` chính thức sau thi lại.
- Người nhập.
- Trạng thái kết quả.
- Ghi chú và audit metadata.

### 21.3. Functional requirements

- `FR-RETAKE-001`: Giáo vụ ghi nhận học sinh và môn được thi lại theo kết quả của CR.
- `FR-RETAKE-002`: Người có quyền nhập một `Đtlmh` cho học sinh/môn/năm học.
- `FR-RETAKE-003`: Hệ thống ngăn tạo lần thi hợp lệ thứ hai cho cùng bộ dữ liệu.
- `FR-RETAKE-004`: Nhập hoặc sửa `Đtlmh` tạo calculation task.
- `FR-RETAKE-005`: Background worker thay `ĐtbmhCN` của môn bằng `Đtlmh`.
- `FR-RETAKE-006`: Background worker tính lại `Đtbcn`.
- `FR-RETAKE-007`: Bảng điểm hiển thị kết quả trước và sau thi lại.
- `FR-RETAKE-008`: Hệ thống lưu audit toàn bộ thay đổi điểm thi lại.

### 21.4. Công thức và business rules

`BR-RETAKE-003`: Đối với môn có thi lại:

```text
ĐtbmhCN chính thức của môn = Đtlmh
```

`BR-RETAKE-004`: Sau khi thay thế các môn có thi lại:

```text
Đtbcn = ROUND(
    Tổng các ĐtbmhCN chính thức / Số môn có ĐtbmhCN chính thức,
    1
)
```

Quy tắc:

- `BR-RETAKE-005`: Mỗi môn trong một năm chỉ có một lần thi lại cho một học sinh.
- `BR-RETAKE-006`: Học sinh có thể thi lại nhiều môn; giới hạn một lần được áp dụng riêng cho từng môn.
- `BR-RETAKE-007`: `Đtlmh` nằm trong đoạn 0 đến 10 và làm tròn đến 0.1.
- `BR-RETAKE-008`: Không xóa hoặc cập nhật đè `Đtbmh` của HK1 và HK2.
- `BR-RETAKE-009`: Phải giữ `ĐtbmhCN` trước thi lại để phục vụ lịch sử.
- `BR-RETAKE-010`: Tổng kết môn sau thi lại có `calculationSource = RETAKE`.
- `BR-RETAKE-011`: Bảng điểm phải ghi rõ “ĐtbmhCN được thay thế bằng điểm thi lại”.
- `BR-RETAKE-012`: Nhập hoặc sửa `Đtlmh` chuyển các tổng kết liên quan sang `IN_PROGRESS`.
- `NFR-CALC-017`: Chỉ background worker được cập nhật `ĐtbmhCN` và `Đtbcn` chính thức.
- `BR-RETAKE-013`: Khi worker hoàn tất, các tổng kết chuyển sang `FINISH`.
- `BR-RETAKE-014`: Quy trình sửa `Đtlmh` phải có quyền phù hợp và audit log.
- `BR-RETAKE-015`: Nếu môn kỹ năng có thi lại theo CR, kết quả vẫn không tham gia `Đtbcn`.

### 21.5. Ví dụ

Trước thi lại:

| Môn | ĐtbmhCN |
|---|---:|
| Toán | 4.0 |
| Ngữ văn | 6.5 |
| Tiếng Anh | 7.0 |

Học sinh thi lại môn Toán và đạt:

```text
Đtlmh Toán = 6.0
```

Sau background calculation:

| Môn | Điểm trước thi lại | Đtlmh | ĐtbmhCN chính thức | Nguồn |
|---|---:|---:|---:|---|
| Toán | 4.0 | 6.0 | 6.0 | `RETAKE` |
| Ngữ văn | 6.5 | — | 6.5 | `REGULAR` |
| Tiếng Anh | 7.0 | — | 7.0 | `REGULAR` |

`Đtbcn` được tính lại từ 6.0, 6.5 và 7.0.

## 22. Bảng điểm tổng kết

### 22.1. Phạm vi dữ liệu

Bảng điểm tổng kết phải thể hiện:

- `FR-SUMMARY-001`: Các điểm thành phần.
- `FR-SUMMARY-002`: `Đtbmh` của từng học kỳ.
- `FR-SUMMARY-003`: `ĐtbmhCN` trước và sau thi lại nếu có.
- `FR-SUMMARY-004`: `Đtbhk`.
- `FR-SUMMARY-005`: `Đtbcn`.
- `FR-SUMMARY-006`: Điểm môn kỹ năng được báo cáo riêng.
- `FR-SUMMARY-007`: Trạng thái `IN_PROGRESS` hoặc `FINISH`.
- `FR-SUMMARY-008`: Thời điểm tính gần nhất.
- `FR-SUMMARY-009`: Nguồn kết quả `REGULAR` hoặc `RETAKE`.
- `FR-SUMMARY-010`: Ghi chú chuyển lớp.
- `FR-SUMMARY-011`: Ghi chú thi lại.

### 22.2. Dữ liệu trạng thái tính

| Trường | Mô tả |
|---|---|
| `calculationStatus` | `IN_PROGRESS` hoặc `FINISH` |
| `sourceVersion` | Phiên bản dữ liệu nguồn mới nhất |
| `calculatedVersion` | Phiên bản đã được tính |
| `calculatedAt` | Thời điểm tính thành công gần nhất |
| `calculationSource` | `REGULAR` hoặc `RETAKE` |
| `lastCalculationTaskId` | Task gần nhất |

### 22.3. Quy tắc hiển thị

- `BR-SUMMARY-005`: `IN_PROGRESS` hiển thị trạng thái đang cập nhật và không được coi là kết quả chính thức.
- `BR-SUMMARY-006`: `FINISH` cho phép hiển thị và sử dụng kết quả chính thức.
- `BR-SUMMARY-007`: Khi có thi lại, phải hiển thị đồng thời điểm trước thi lại, `Đtlmh` và điểm chính thức.
- `BR-SUMMARY-008`: Khi có chuyển lớp, phải hiển thị ghi chú lớp cũ, lớp mới và ngày chuyển.

