# Score Change and Calculation Module

## 18. Quy trình sửa điểm

### 18.1. Sửa trực tiếp

`BR-SCORECHANGE-001`: GVBM được sửa trực tiếp khi đồng thời thỏa mãn:

1. Giáo viên đang được phân công cho môn/lớp/học kỳ.
2. Chưa quá 10 ngày kể từ thời điểm ô điểm được nhập lần đầu.
3. Học kỳ chưa bị khóa.

`BR-SCORECHANGE-002`: Sửa trực tiếp không cần request nhưng vẫn phải có audit log. Sau khi lưu, hệ thống đánh dấu bảng điểm tổng kết liên quan là `IN_PROGRESS` và tạo calculation task.

### 18.2. Request sửa điểm

`BR-SCORECHANGE-003`: GVBM phải tạo request nếu đã quá 10 ngày hoặc học kỳ đã khóa.

Request gồm:

- Cột điểm cần sửa.
- Học sinh cần sửa.
- Lý do.
- Điểm trước khi sửa.
- Điểm đề xuất sau khi sửa.
- Người và thời điểm yêu cầu.

`BR-SCORECHANGE-013`: Request sửa điểm phải tuân theo các trạng thái và hướng chuyển sau:

```text
PENDING → APPROVED → APPLIED
        ↘ REJECTED
        ↘ CANCELLED
```

### 18.3. Functional requirements

- `FR-SCORECHANGE-001`: Giáo viên tạo request sửa điểm.
- `FR-SCORECHANGE-002`: Giáo viên xem trạng thái các request của mình.
- `FR-SCORECHANGE-003`: Giáo vụ xem, duyệt hoặc từ chối request.
- `FR-SCORECHANGE-004`: Hệ thống tự động áp dụng điểm mới sau khi request được duyệt hợp lệ.
- `FR-SCORECHANGE-005`: Hệ thống lưu và cho phép người có quyền xem lịch sử request.

### 18.4. Business rules

- `BR-SCORECHANGE-004`: Một request chỉ cần một giáo vụ duyệt.
- `BR-SCORECHANGE-005`: Người duyệt không được đồng thời là người yêu cầu.
- `BR-SCORECHANGE-006`: Mỗi ô điểm chỉ có tối đa một request `PENDING`.
- `BR-SCORECHANGE-007`: Khi duyệt, điểm hiện tại phải còn bằng điểm trước sửa trong request.
- `BR-SCORECHANGE-008`: Nếu dữ liệu đã thay đổi, request không được áp dụng.
- `BR-SCORECHANGE-009`: Điểm mới phải nằm trong đoạn 0 đến 10.
- `BR-SCORECHANGE-010`: Phê duyệt cập nhật điểm tự động và tạo calculation task.
- `BR-SCORECHANGE-011`: Phê duyệt không yêu cầu mở khóa toàn bộ học kỳ.
- `BR-SCORECHANGE-012`: Mọi trạng thái và thay đổi phải có audit log.

## 19. Khóa học kỳ

`FR-SEM-009`: Trước khi giáo vụ xác nhận khóa, hệ thống hiển thị báo cáo:

- Môn chưa cấu hình đủ cột KTĐK.
- Môn chưa có đúng một cột KTCK.
- Môn kỹ năng chưa có đủ ba cột.
- Học sinh có ô điểm chưa nhập.
- Học sinh chưa có dữ liệu điểm.
- Sổ điểm chưa công bố.
- Request sửa điểm đang chờ duyệt.

`BR-SEM-010`: Dữ liệu chưa đầy đủ không ngăn tính `Đtbmh` và không ngăn khóa học kỳ.

Khi khóa:

- `BR-SEM-011`: Hệ thống tạo calculation task cuối kỳ.
- `BR-SEM-012`: Bảng điểm tổng kết chuyển sang `IN_PROGRESS`.
- `BR-SEM-013`: Giáo viên không được sửa trực tiếp.
- `BR-SEM-014`: Thay đổi tiếp theo phải qua request.
- `BR-SEM-015`: Chỉ khi background calculation hoàn tất thì bảng điểm mới chuyển sang `FINISH`.

## 20. Background calculation

### 20.1. Nguyên tắc bắt buộc

`NFR-CALC-004`: Tuyệt đối không tính điểm trung bình trong một HTTP request, bao gồm cả request ghi dữ liệu và request đọc dữ liệu.

`NFR-CALC-005`: HTTP request thay đổi điểm chỉ được:

1. Kiểm tra quyền và validate dữ liệu.
2. Lưu dữ liệu điểm gốc.
3. Đánh dấu bảng điểm tổng kết bị ảnh hưởng là `IN_PROGRESS`.
4. Tạo calculation task có khả năng lưu bền vững.
5. Trả response mà không chờ phép tính hoàn tất.

`NFR-CALC-006`: HTTP GET chỉ đọc kết quả và trạng thái hiện có, không kích hoạt phép tính đồng bộ.

### 20.2. Trạng thái bảng điểm tổng kết

| BR ID | Trạng thái | Ý nghĩa |
|---|---|---|
| `BR-CALC-001` | `IN_PROGRESS` | Dữ liệu nguồn đã thay đổi, kết quả đang chờ tính hoặc đang được tính lại |
| `BR-CALC-002` | `FINISH` | Kết quả đã được tính thành công từ phiên bản dữ liệu nguồn mới nhất |

Khi trạng thái là `IN_PROGRESS`:

- `BR-SUMMARY-001`: UI hiển thị “Đang tính toán” hoặc “Đang cập nhật”.
- `BR-SUMMARY-002`: Kết quả cũ, nếu hiển thị, phải được đánh dấu chưa phải kết quả mới nhất.
- `BR-SUMMARY-003`: Không được dùng kết quả đang `IN_PROGRESS` cho quyết định tổng kết chính thức.
- `BR-SUMMARY-004`: Client có thể refresh hoặc polling để lấy trạng thái mới.

### 20.3. Thứ tự tính

`BR-CALC-003`: Background worker tính theo thứ tự phụ thuộc:

1. `Đtbmh` hoặc điểm môn kỹ năng.
2. `Đtbhk`.
3. `ĐtbmhCN`.
4. Áp dụng `Đtlmh` vào `ĐtbmhCN` nếu có thi lại.
5. `Đtbcn`.

### 20.4. Functional requirements

- `FR-CALC-001`: Mọi thay đổi điểm nguồn đánh dấu các tổng kết bị ảnh hưởng là `IN_PROGRESS`.
- `FR-CALC-002`: Hệ thống tạo calculation task khi dữ liệu nguồn thay đổi.
- `FR-CALC-003`: Worker lấy và xử lý task ở background.
- `FR-CALC-004`: Worker tính lại toàn bộ giá trị phụ thuộc theo đúng thứ tự.
- `FR-CALC-005`: Worker chuyển tổng kết sang `FINISH` sau khi tính thành công.
- `FR-CALC-006`: Hệ thống lưu phiên bản dữ liệu nguồn và thời điểm tính gần nhất.
- `FR-CALC-007`: Giáo vụ yêu cầu chạy lại task lỗi.

### 20.5. Tính nhất quán transaction

`NFR-CALC-007`: Các thao tác sau phải nằm trong cùng một transaction:

- Lưu điểm nguồn.
- Tăng phiên bản dữ liệu nguồn.
- Chuyển tổng kết sang `IN_PROGRESS`.
- Tạo calculation task.

`BR-CALC-004`: Một tổng kết chỉ được chuyển sang `FINISH` khi:

```text
calculatedVersion = sourceVersion
```

`NFR-CALC-008`: Nếu dữ liệu thay đổi trong lúc worker đang chạy, worker của phiên bản cũ không được chuyển trạng thái sang `FINISH`.

### 20.6. Xử lý lỗi

- `NFR-CALC-009`: Trạng thái nghiệp vụ của bảng tổng kết chỉ gồm `IN_PROGRESS` và `FINISH`.
- `NFR-CALC-010`: Nếu job lỗi, bảng tổng kết tiếp tục là `IN_PROGRESS`.
- `NFR-CALC-011`: Calculation task phải hỗ trợ trạng thái kỹ thuật `FAILED` và số lần retry.
- `NFR-CALC-012`: Hệ thống lưu lỗi gần nhất của calculation task.
- `NFR-CALC-013`: Không được đánh dấu `FINISH` khi tính toán thất bại.
- `NFR-CALC-014`: Calculation task phải lưu bền vững và hỗ trợ retry.
- `NFR-CALC-015`: Worker phải xử lý idempotent.
- `NFR-CALC-016`: Các task trùng phải được gộp hoặc bỏ qua an toàn.

### 20.7. Sự kiện kích hoạt tính lại

- `BR-CALC-005`: Nhập, sửa, hủy hoặc thay đổi trạng thái điểm kích hoạt tính lại.
- `BR-CALC-006`: Thêm hoặc xóa cột điểm kích hoạt tính lại.
- `BR-CALC-007`: Phê duyệt request sửa điểm kích hoạt tính lại.
- `BR-CALC-008`: Thay đổi trọng số môn kỹ năng kích hoạt tính lại.
- `BR-CALC-009`: Học sinh chuyển lớp kích hoạt tính lại.
- `BR-CALC-010`: Khóa hoặc mở học kỳ kích hoạt tính lại.
- `BR-CALC-011`: Nhập hoặc sửa điểm thi lại kích hoạt tính lại.

