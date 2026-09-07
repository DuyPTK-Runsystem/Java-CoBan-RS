# Academic Structure Module

## 4. Metadata Khối — Grade

### 4.1. Mục tiêu

Khối là metadata dùng chung, độc lập với năm học. Lớp học tham chiếu đến khối thay vì suy luận khối từ tên lớp.

### 4.2. Dữ liệu

| Trường         | Bắt buộc | Mô tả                                 |
| -------------- | -------: | ------------------------------------- |
| `gradeId`      |       Có | Khóa chính                            |
| `gradeCode`    |       Có | Mã duy nhất, ví dụ `GRADE_6`          |
| `gradeName`    |       Có | Tên hiển thị, ví dụ `Khối 6`          |
| `gradeLevel`   |       Có | Giá trị 6, 7, 8 hoặc 9                |
| `displayOrder` |       Có | Thứ tự hiển thị                       |
| `nextGradeId`  |    Không | Khối tiếp theo                        |
| `active`       |       Có | Có được sử dụng cho dữ liệu mới không |
| `description`  |    Không | Mô tả bổ sung                         |
| Audit metadata |       Có | Người và thời gian tạo/cập nhật       |

Dữ liệu mặc định:

| Khối   | Khối tiếp theo |
| ------ | -------------- |
| Khối 6 | Khối 7         |
| Khối 7 | Khối 8         |
| Khối 8 | Khối 9         |
| Khối 9 | Không có       |

### 4.3. Functional requirements

- `FR-GRADE-001`: Giáo vụ xem danh sách khối.
- `FR-GRADE-002`: Giáo vụ cập nhật tên, thứ tự và mô tả khối.
- `FR-GRADE-003`: Giáo vụ kích hoạt hoặc ngừng sử dụng khối.
- `FR-GRADE-004`: Hệ thống hiển thị số lớp và số học sinh của từng khối theo năm học.
- `FR-GRADE-005`: Hệ thống ngăn xóa khối đang được tham chiếu.

### 4.4. Business rules

- `BR-GRADE-001`: `gradeCode` và `gradeLevel` phải duy nhất.
- `BR-GRADE-002`: `nextGradeId` không được trỏ về chính khối hiện tại hoặc tạo chu trình.
- `BR-GRADE-003`: Không xác định khối bằng cách kiểm tra ký tự đầu của tên lớp.
- `BR-GRADE-004`: Không xóa khối đã có lớp, môn học hoặc lịch sử học sinh.
- `BR-GRADE-005`: Ngừng hoạt động một khối chỉ ngăn dữ liệu mới, không làm mất dữ liệu lịch sử.
- `BR-GRADE-006`: Khối 9 không có khối tiếp theo trong phạm vi trường THCS.

## 5. Năm học — Academic Year

### 5.1. Dữ liệu

- Mã và tên năm học.
- Ngày bắt đầu, ngày kết thúc.
- Trạng thái: `DRAFT`, `ACTIVE`, `CLOSED`.
- Ghi chú và audit metadata.

### 5.2. Functional requirements

- `FR-AY-001`: Giáo vụ tạo năm học.
- `FR-AY-002`: Xem, tìm kiếm và lọc năm học.
- `FR-AY-003`: Cập nhật năm học chưa đóng.
- `FR-AY-004`: Kích hoạt năm học.
- `FR-AY-005`: Đóng năm học.
- `FR-AY-006`: Xóa năm học chưa phát sinh dữ liệu.
- `FR-AY-007`: Xem thống kê số học kỳ, lớp và học sinh.

### 5.3. Business rules

- `BR-AY-001`: Mã năm học phải duy nhất.
- `BR-AY-002`: Ngày kết thúc phải sau ngày bắt đầu.
- `BR-AY-003`: Tại một thời điểm chỉ có tối đa một năm học `ACTIVE`.
- `BR-AY-004`: Không xóa năm học đã có học kỳ, lớp hoặc phân lớp.
- `BR-AY-005`: Đóng năm học không xóa dữ liệu lịch sử.
- `BR-AY-006`: Mở lại năm học đã đóng phải có quyền và audit log.

## 6. Học kỳ — Semester

### 6.1. Dữ liệu

- Năm học.
- Mã, tên và thứ tự học kỳ.
- Ngày bắt đầu, ngày kết thúc.
- Thời điểm tự động khóa.
- Trạng thái: `DRAFT`, `ACTIVE`, `LOCKED`, `CLOSED`.
- Người và thời điểm xác nhận khóa.
- Audit metadata.

### 6.2. Functional requirements

- `FR-SEM-001`: Tạo học kỳ trong năm học.
- `FR-SEM-002`: Cập nhật thời gian học kỳ.
- `FR-SEM-003`: Kích hoạt học kỳ.
- `FR-SEM-004`: Giáo vụ xác nhận khóa học kỳ.
- `FR-SEM-005`: Hệ thống tự động khóa học kỳ.
- `FR-SEM-006`: Xem mức độ hoàn thành nhập điểm.
- `FR-SEM-007`: Người có thẩm quyền mở lại học kỳ.
- `FR-SEM-008`: Lưu lịch sử khóa và mở khóa.

### 6.3. Business rules

- `BR-SEM-001`: Học kỳ phải thuộc một năm học.
- `BR-SEM-002`: Thời gian học kỳ phải nằm trong thời gian năm học.
- `BR-SEM-003`: Các học kỳ trong cùng năm học không được chồng lấn.
- `BR-SEM-004`: Một học kỳ được khóa khi và chỉ khi:
  1. Giáo vụ xác nhận khóa; hoặc
  2. Đã qua 45 ngày kể từ ngày kết thúc học kỳ.
- `BR-SEM-005`: Tác vụ tự động khóa phải chạy nền theo lịch.
- `BR-SEM-006`: Khi tự động khóa, hệ thống tạo báo cáo dữ liệu điểm chưa hoàn chỉnh.
- `BR-SEM-006` được mở rộng bởi `CR-SEM-001` (đã phê duyệt). Các business rule phát sinh từ rule gốc
  dùng mã `BR-SEM-006-01` đến `BR-SEM-006-08`, quy định việc đánh giá 11 mốc checkpoint, tạo báo cáo
  và gửi thông báo (email/in-app) cho Giáo vụ, GVBM, GVCN (triển khai trong Plan 040).
- `BR-SEM-007`: Dữ liệu chưa hoàn chỉnh không ngăn việc khóa theo hai điều kiện trên.
- `BR-SEM-008`: Học kỳ đã khóa không cho giáo viên sửa điểm trực tiếp.
- `BR-SEM-009`: Mở lại học kỳ phải lưu người thực hiện, thời gian và lý do.

## 7. Lớp học — School Class

### 7.1. Dữ liệu

- Năm học và khối.
- Mã và tên lớp.
- Phòng học.
- Sĩ số dự kiến.
- Trạng thái: `PLANNED`, `ACTIVE`, `CLOSED`.
- Audit metadata.

`BR-CLASS-001`: GVCN không được lưu cố định trong lớp. GVCN được xác định từ phân công để bảo toàn lịch sử.

### 7.2. Functional requirements

- `FR-CLASS-001`: Tạo lớp theo năm học và khối.
- `FR-CLASS-002`: Xem danh sách lớp theo năm học, khối và trạng thái.
- `FR-CLASS-003`: Cập nhật thông tin lớp.
- `FR-CLASS-004`: Xem danh sách học sinh trong lớp.
- `FR-CLASS-005`: Xem GVCN và GVBM.
- `FR-CLASS-006`: Đóng lớp.
- `FR-CLASS-007`: Xóa lớp chưa phát sinh dữ liệu.
- `FR-CLASS-008`: Cảnh báo mất cân bằng sĩ số trong khối.

### 7.3. Cảnh báo sĩ số

Sĩ số trung bình khối:

```text
SĩSốTBKhối = Tổng HS active trong khối / Số lớp active trong khối
```

Hệ thống cảnh báo khi:

```text
SĩSốLớp < 0.8 × SĩSốTBKhối
hoặc
SĩSốLớp > 1.2 × SĩSốTBKhối
```

Quy tắc:

- `BR-CLASS-002`: Sĩ số trung bình được tính trong cùng năm học và cùng khối.
- `BR-CLASS-003`: Chỉ tính lớp và học sinh đang hoạt động.
- `BR-CLASS-004`: Kiểm tra sau khi xếp lớp hoặc chuyển lớp.
- `BR-CLASS-005`: Cảnh báo sĩ số không chặn thao tác.
- `BR-CLASS-006`: Không áp dụng cảnh báo khi khối chưa có lớp hoặc chưa có học sinh.

### 7.4. Business rules khác

- `BR-CLASS-007`: Tên lớp phải duy nhất trong cùng năm học.
- `BR-CLASS-008`: Một lớp thuộc đúng một khối.
- `BR-CLASS-009`: Không đổi khối sau khi lớp đã phát sinh dữ liệu.
- `BR-CLASS-010`: Không xóa lớp đã có học sinh, phân công, điểm danh hoặc điểm.
- `BR-CLASS-011`: Lớp đã đóng chỉ được truy cập ở chế độ xem.
