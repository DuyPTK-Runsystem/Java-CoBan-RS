# Enrollment and Teaching Module

## 8. Xếp lớp — Student Enrollment

### 8.1. Dữ liệu

- Học sinh, lớp và năm học.
- Ngày vào lớp, ngày rời lớp.
- Trạng thái: `ACTIVE`, `TRANSFERRED`, `COMPLETED`, `WITHDRAWN`.
- Lý do chuyển hoặc rời lớp.
- Người thực hiện và audit metadata.

### 8.2. Functional requirements

- `FR-ENROLL-001`: Xếp một học sinh vào lớp.
- `FR-ENROLL-002`: Xếp nhiều học sinh hàng loạt.
- `FR-ENROLL-003`: Chuyển học sinh sang lớp khác.
- `FR-ENROLL-004`: Xem học sinh chưa được xếp lớp.
- `FR-ENROLL-005`: Xem lịch sử lớp của học sinh.
- `FR-ENROLL-006`: Kết chuyển học sinh sang năm học mới.
- `FR-ENROLL-007`: Import kết quả phân lớp.
- `FR-ENROLL-008`: Ghi nhận lên lớp, lưu ban, thôi học hoặc hoàn thành THCS.

### 8.3. Business rules

- `BR-ENROLL-001`: Một học sinh chỉ thuộc tối đa một lớp `ACTIVE` trong một năm học.
- `BR-ENROLL-002`: Chuyển lớp không cập nhật đè bản ghi cũ.
- `BR-ENROLL-003`: Khi chuyển lớp, hệ thống phải:
  1. Kết thúc phân lớp cũ;
  2. Tạo phân lớp mới;
  3. Lưu ngày hiệu lực và lý do;
  4. Giữ toàn bộ điểm đã tích lũy;
  5. Tạo ghi chú trong bảng điểm tổng kết.
- `BR-ENROLL-004`: Ghi chú chuyển lớp thể hiện lớp cũ, lớp mới và ngày chuyển.
- `BR-ENROLL-005`: Giáo viên lớp mới được xem các điểm đã tích lũy của học sinh.
- `BR-ENROLL-006`: Không xóa phân lớp đã có điểm danh hoặc điểm.
- `BR-ENROLL-007`: Kết chuyển cuối năm tạo dữ liệu mới, không sửa lịch sử năm cũ.
- `BR-ENROLL-008`: Sau khi xếp hoặc chuyển lớp, hệ thống kiểm tra cảnh báo sĩ số.

## 9. Giáo viên — Teacher

### 9.1. Dữ liệu

- Mã giáo viên, họ tên.
- Ngày sinh, giới tính.
- Điện thoại, email.
- Tổ chuyên môn.
- Ngày vào trường.
- Trạng thái: `ACTIVE`, `ON_LEAVE`, `INACTIVE`.
- Tài khoản được liên kết.
- Audit metadata.

### 9.2. Functional requirements

- `FR-TEACHER-001`: Tạo và cập nhật hồ sơ giáo viên.
- `FR-TEACHER-002`: Tìm kiếm, lọc và phân trang giáo viên.
- `FR-TEACHER-003`: Liên kết giáo viên với tài khoản.
- `FR-TEACHER-004`: Quản lý tổ chuyên môn.
- `FR-TEACHER-005`: Ngừng hoặc kích hoạt công tác.
- `FR-TEACHER-006`: Xem lịch sử phân công.

### 9.3. Business rules

- `BR-TEACHER-001`: Mã giáo viên phải duy nhất.
- `BR-TEACHER-002`: Một giáo viên chỉ liên kết với một tài khoản.
- `BR-TEACHER-003`: Giáo viên ngừng công tác không được nhận phân công mới.
- `BR-TEACHER-004`: Ngừng công tác không làm mất lịch sử.
- `BR-TEACHER-005`: Không xóa giáo viên đã phát sinh phân công, điểm danh hoặc nhập điểm.
- `BR-TEACHER-006`: Tổ chuyên môn không tự động cấp quyền giảng dạy; quyền được xác định từ phân công.

## 10. Phân công giáo viên — Teaching Assignment

### 10.1. Loại phân công

Phân công chủ nhiệm:

```text
Giáo viên + Lớp + Năm học
```

Phân công giảng dạy:

```text
Giáo viên + Môn học + Lớp + Học kỳ
```

### 10.2. Functional requirements

- `FR-ASSIGN-001`: Phân công GVCN.
- `FR-ASSIGN-002`: Phân công GVBM.
- `FR-ASSIGN-003`: Thay giáo viên trong học kỳ.
- `FR-ASSIGN-004`: Xem phân công theo giáo viên.
- `FR-ASSIGN-005`: Xem phân công theo lớp.
- `FR-ASSIGN-006`: Kết thúc hoặc hủy phân công.
- `FR-ASSIGN-007`: Kiểm tra phân công trùng.

### 10.3. Business rules

- `BR-ASSIGN-001`: Một lớp chỉ có một GVCN active tại một thời điểm.
- `BR-ASSIGN-002`: Một bộ `{môn học, lớp học, học kỳ}` chỉ có một giáo viên giảng dạy.
- `BR-ASSIGN-003`: Giáo viên chỉ nhập điểm cho bộ môn/lớp/học kỳ đang được phân công.
- `BR-ASSIGN-004`: GVCN được xem điểm tổng hợp của lớp nhưng không được sửa điểm môn khác.
- `BR-ASSIGN-005`: Thay giáo viên phải kết thúc phân công cũ và tạo phân công mới.
- `BR-ASSIGN-006`: Thay giáo viên không làm mất điểm đã nhập.
- `BR-ASSIGN-007`: Phân công phải nằm trong thời gian năm học hoặc học kỳ tương ứng.

## 11. Lịch học và buổi học

### 11.1. Mục tiêu

Lịch học xác định những ngày và buổi được xem là buổi học hợp lệ. Đây là dữ liệu nền bắt buộc để áp dụng quy tắc “không có ngoại lệ nghĩa là có mặt”.

### 11.2. Functional requirements

- `FR-CALENDAR-001`: Giáo vụ cấu hình ngày và buổi học hợp lệ.
- `FR-CALENDAR-002`: Giáo vụ đánh dấu ngày nghỉ, ngày lễ hoặc buổi không học.
- `FR-CALENDAR-003`: Giáo viên và học sinh xem lịch học thuộc phạm vi của mình.
- `FR-CALENDAR-004`: Hệ thống cung cấp lịch buổi học cho nghiệp vụ điểm danh và báo cáo chuyên cần.

### 11.3. Business rules

- `BR-CALENDAR-001`: Mỗi ngày có tối đa hai buổi: `MORNING` và `AFTERNOON`.
- `BR-CALENDAR-002`: Một buổi phải được đánh dấu có học trước khi được dùng trong báo cáo chuyên cần.
- `BR-CALENDAR-003`: Ngày nghỉ, ngày lễ và buổi không có lịch học không được tính là học sinh có mặt.
- `BR-CALENDAR-004`: Lịch học phải thuộc năm học và nằm trong khoảng thời gian học kỳ phù hợp.
