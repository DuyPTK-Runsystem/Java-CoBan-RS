# Assessment and Scoring Module

## 14. Điểm số — Score Management

### 14.1. Thang điểm và làm tròn

- `BR-SCORE-001`: Mọi điểm số sử dụng thang điểm 10.
- `BR-SCORE-002`: Điểm hợp lệ nằm trong đoạn từ 0 đến 10.
- `BR-SCORE-003`: Các kết quả tính toán được làm tròn đến 0.1 bằng `HALF_UP`.
- `BR-SCORE-004`: Điểm 0 là dữ liệu hợp lệ.
- `BR-SCORE-005`: Không dùng điểm 0 để biểu diễn chưa nhập, vắng hoặc miễn đánh giá.

### 14.2. Trạng thái dữ liệu điểm

| Trạng thái | Ý nghĩa |
|---|---|
| `NOT_ENTERED` | Chưa nhập |
| `SCORED` | Có điểm hợp lệ |
| `ABSENT` | Vắng đánh giá |
| `EXEMPTED` | Được miễn |
| `CANCELLED` | Kết quả bị hủy |

`BR-SCORE-006`: Chỉ ô điểm có `scoreStatus = SCORED` và `scoreValue != null` được đưa vào công thức.

### 14.3. Functional requirements

- `FR-SCORE-001`: GVBM mở sổ điểm của môn/lớp/học kỳ được phân công.
- `FR-SCORE-002`: Tạo cột điểm phù hợp với loại môn.
- `FR-SCORE-003`: Nhập điểm từng học sinh.
- `FR-SCORE-004`: Nhập điểm hàng loạt.
- `FR-SCORE-005`: Xem ô điểm chưa nhập.
- `FR-SCORE-006`: Công bố điểm cho học sinh.
- `FR-SCORE-007`: Xem audit log điểm.
- `FR-SCORE-008`: Xem trạng thái tính bảng điểm tổng kết.

## 15. Điểm môn học thông thường

### 15.1. Loại và cấu trúc cột

| BR ID | Loại | Mã | Hệ số | Số cột cấu hình |
|---|---|---|---:|---|
| `BR-SCORE-007` | Kiểm tra thường xuyên | `KTTT` | 1 | Từ 0, không giới hạn |
| `BR-SCORE-008` | Kiểm tra định kỳ | `KTĐK` | 2 | Ít nhất 1, không giới hạn |
| `BR-SCORE-009` | Kiểm tra cuối kỳ | `KTCK` | 3 | Bắt buộc đúng 1 |

`BR-SCORE-010`: Số lượng cột là quy tắc cấu trúc sổ điểm. Học sinh không bắt buộc có dữ liệu ở tất cả các cột thì hệ thống mới tính `Đtbmh`.

### 15.2. Điểm trung bình môn học kỳ

`BR-SCORE-011`: `Đtbmh` được tính theo công thức:

```text
Đtbmh = ROUND(
    Tổng(Điểm đã có dữ liệu × Hệ số)
    / Tổng hệ số của các ô đã có dữ liệu,
    1
)
```

Quy tắc:

- `BR-SCORE-012`: Chỉ tính các ô điểm có dữ liệu hợp lệ của chính học sinh đó.
- `BR-SCORE-013`: Ô chưa nhập, vắng, miễn hoặc bị hủy không tham gia tử số và mẫu số.
- `BR-SCORE-014`: Điểm 0 vẫn tham gia công thức.
- `BR-SCORE-015`: Nếu chưa có ô điểm hợp lệ nào thì `Đtbmh = null` và hiển thị “Chưa có điểm”.
- `BR-SCORE-016`: `Đtbmh` có thể thay đổi khi dữ liệu mới được nhập hoặc sửa.
- `NFR-CALC-001`: Việc tính `Đtbmh` phải chạy nền, không tính trong HTTP request.

Ví dụ:

```text
KTTT = 8, KTĐK = 7, KTCK chưa nhập
Đtbmh = ROUND((8×1 + 7×2) / (1+2), 1) = 7.3
```

## 16. Điểm môn kỹ năng

### 16.1. Cấu trúc

Mỗi môn kỹ năng có đúng ba cột:

| BR ID | Loại | Số cột |
|---|---|---:|
| `BR-SKILL-001` | KTTT | 1 |
| `BR-SKILL-002` | KTĐK | 1 |
| `BR-SKILL-003` | KTCK | 1 |

### 16.2. Trọng số

`BR-SKILL-004`: Người dùng có quyền cấu hình xác định ba trọng số `W_KTTT`, `W_KTĐK`, `W_KTCK` sao cho:

```text
W_KTTT + W_KTĐK + W_KTCK = 100
W_KTCK >= max(W_KTTT, W_KTĐK)
```

### 16.3. Công thức

`BR-SKILL-005`: Điểm môn kỹ năng được tính theo công thức:

```text
ĐiểmKỹNăng = ROUND(
    (KTTT×W_KTTT + KTĐK×W_KTĐK + KTCK×W_KTCK) / 100,
    1
)
```

### 16.4. Business rules

- `BR-SKILL-006`: Trọng số phải được cấu hình trước khi tính kết quả.
- `BR-SKILL-007`: Không thay đổi trọng số sau khi điểm đã công bố nếu không có quyền đặc biệt và audit log.
- `BR-SKILL-008`: Môn kỹ năng chỉ tổng kết trong một học kỳ duy nhất.
- `BR-SKILL-009`: Môn kỹ năng không tính vào `Đtbhk`, `Đtbcn`, xếp hạng hoặc xếp loại.
- `BR-SKILL-010`: Kết quả môn kỹ năng hiển thị riêng trên bảng điểm.
- `NFR-CALC-002`: Việc tính điểm môn kỹ năng phải chạy nền.

### 16.5. Functional requirements

- `FR-SKILL-001`: Người có quyền cấu hình ba cột điểm môn kỹ năng.
- `FR-SKILL-002`: Người có quyền cấu hình trọng số KTTT, KTĐK và KTCK.
- `FR-SKILL-003`: Hệ thống kiểm tra tổng trọng số và ràng buộc trọng số KTCK.
- `FR-SKILL-004`: Giáo viên và học sinh xem kết quả môn kỹ năng theo quyền.

## 17. Các loại điểm trung bình

### 17.1. Điểm trung bình môn học cả năm

`BR-AVERAGE-001`: Nếu môn có `Đtbmh` ở cả hai học kỳ thì:

```text
ĐtbmhCN = ROUND((Đtbmh_HK1 + 2×Đtbmh_HK2) / 3, 1)
```

`BR-AVERAGE-002`: Nếu môn chỉ được cấu hình học trong một học kỳ thì:

```text
ĐtbmhCN = Đtbmh của học kỳ có giảng dạy
```

`BR-AVERAGE-003`: Nếu môn được cấu hình học ở cả hai học kỳ nhưng một học kỳ chưa có `Đtbmh`, `ĐtbmhCN` tạm thời chưa được tính.

### 17.2. Điểm trung bình học kỳ

`BR-AVERAGE-004`: `Đtbhk` được tính theo công thức:

```text
Đtbhk = ROUND(Tổng các Đtbmh hợp lệ / Số môn có Đtbmh, 1)
```

Quy tắc:

- `BR-AVERAGE-005`: Mỗi môn thông thường có hệ số 1 trong `Đtbhk`.
- `BR-AVERAGE-006`: Môn chưa có `Đtbmh` không tham gia công thức.
- `BR-AVERAGE-007`: Môn kỹ năng không tham gia công thức `Đtbhk`.
- `BR-AVERAGE-008`: Nếu chưa có môn nào phát sinh `Đtbmh` thì `Đtbhk = null`.

### 17.3. Điểm trung bình cả năm

`BR-AVERAGE-009`: `Đtbcn` được tính theo công thức:

```text
Đtbcn = ROUND(Tổng các ĐtbmhCN hợp lệ / Số môn có ĐtbmhCN, 1)
```

Quy tắc:

- `BR-AVERAGE-010`: Mỗi môn thông thường có hệ số 1 trong `Đtbcn`.
- `BR-AVERAGE-011`: Môn chưa có `ĐtbmhCN` không tham gia công thức.
- `BR-AVERAGE-012`: Môn kỹ năng không tham gia công thức `Đtbcn`.
- `BR-AVERAGE-013`: Nếu chưa có môn nào phát sinh `ĐtbmhCN` thì `Đtbcn = null`.
- `BR-AVERAGE-014`: Sau khi có điểm thi lại, `Đtbcn` phải được tính lại từ các `ĐtbmhCN` chính thức sau thi lại.

`NFR-CALC-003`: Tất cả `Đtbmh`, điểm kỹ năng, `Đtbhk`, `ĐtbmhCN` và `Đtbcn` phải được tính bằng background process.

