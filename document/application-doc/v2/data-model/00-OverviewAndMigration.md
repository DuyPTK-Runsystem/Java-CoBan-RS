# Overview and Migration

## 0. Thông tin tài liệu

| Thuộc tính | Giá trị |
|---|---|
| Tên tài liệu | Data Structure v2 |
| Phiên bản | 2.0 |
| Phạm vi | Cơ sở dữ liệu hệ thống quản lý học sinh THCS |
| Hệ quản trị cơ sở dữ liệu mục tiêu | MySQL 8.x |
| Mô hình khóa chính | BIGINT UNSIGNED, tự tăng |
| Quy ước tên | snake_case cho database |
| Trạng thái | Thiết kế mục tiêu |

Tài liệu này thay thế mô hình dữ liệu ba bảng ban đầu bằng mô hình phục vụ đầy đủ các nghiệp vụ:

- Tài khoản và phân quyền;
- Hồ sơ học sinh, giáo viên;
- Năm học, học kỳ, lớp và môn học;
- Xếp lớp và lịch sử chuyển lớp;
- Phân công GVCN/GVBM;
- Sổ điểm, cột điểm và điểm thành phần;
- Điểm danh;
- Request sửa điểm sau khi khóa sổ;
- Tính điểm nền;
- Bảng điểm học kỳ và cả năm;
- Thi lại cuối năm theo từng môn;
- Audit và theo dõi calculation task.

---

## 1. Nguyên tắc thiết kế

### 1.1. Khóa chính và khóa ngoại

- Tất cả khóa chính và khóa ngoại trong schema mục tiêu dùng cùng kiểu BIGINT UNSIGNED.
- Khóa chính dùng AUTO_INCREMENT nếu không có yêu cầu dùng UUID.
- Không trộn INT với BIGINT trong cùng một quan hệ.
- Mọi mã nghiệp vụ có tính duy nhất phải được bảo vệ bằng UNIQUE constraint ở database.
- Các quan hệ lịch sử quan trọng không được xóa dây chuyền theo thao tác xóa người dùng.

Nếu database hiện tại đang dùng INT, cần chuyển đổi cả khóa chính và khóa ngoại liên quan trong cùng một migration. Không được để bảng cũ dùng INT trong khi bảng mới tham chiếu bằng BIGINT.

### 1.2. Tên bảng user

Tên user được đổi thành app_user trong schema mục tiêu để tránh phụ thuộc vào từ khóa, view hệ thống hoặc quy ước đặc biệt của từng phiên bản MySQL.

### 1.3. Mật khẩu

Mật khẩu không được lưu dạng plaintext.

Schema mục tiêu dùng:

~~~sql
password_hash VARCHAR(255) NOT NULL
~~~

Giá trị lưu trong cột này là password hash do BCrypt hoặc thuật toán tương đương tạo ra. Không giữ cột password VARCHAR(15) của mô hình cũ làm nơi lưu mật khẩu thật.

### 1.4. Dữ liệu nguồn và dữ liệu tính toán

- Điểm thành phần là dữ liệu nguồn.
- Điểm trung bình là dữ liệu dẫn xuất.
- average_score không còn là nguồn dữ liệu chính trong student_info.
- Mọi điểm trung bình phải được tính bằng background worker.
- HTTP request chỉ lưu dữ liệu nguồn, đánh dấu trạng thái và tạo task.

### 1.5. Trạng thái tổng kết

Các bảng tổng kết dùng hai trạng thái nghiệp vụ:

| Trạng thái | Ý nghĩa |
|---|---|
| IN_PROGRESS | Dữ liệu nguồn đã thay đổi hoặc kết quả chưa tính xong |
| FINISH | Kết quả đã tính thành công từ phiên bản dữ liệu nguồn mới nhất |

FAILED chỉ là trạng thái kỹ thuật của calculation_task. Bảng tổng kết không được chuyển sang FINISH khi task thất bại.

### 1.6. Thời gian

- Ngày sinh dùng DATE.
- Ngày thi, ngày điểm danh và ngày hiệu lực dùng DATE hoặc DATETIME tùy nghiệp vụ.
- Thời điểm tạo, cập nhật, khóa sổ và tính toán dùng DATETIME hoặc TIMESTAMP thống nhất trong toàn hệ thống.
- Múi giờ nghiệp vụ mặc định là Asia/Ho_Chi_Minh.

---

## 2. Mô hình hiện tại và hướng chuyển đổi

### 2.1. Schema hiện tại

Database ban đầu gồm:

~~~text
user
student
student_info
~~~

Quan hệ hiện tại:

~~~text
student 1 ───── 0..1 student_info
                    |
                    └── student_info.student_id -> student.student_id
~~~

### 2.2. Các vấn đề của schema hiện tại

1. user là tên bảng không nên tiếp tục dùng trong schema mục tiêu.
2. password VARCHAR(15) không đủ để lưu password hash hiện đại.
3. average_score trong student_info là dữ liệu dẫn xuất, không nên dùng làm nguồn chính.
4. Chưa có năm học, học kỳ, lớp và môn học.
5. Chưa biểu diễn được một học sinh học ở lớp nào trong từng năm học.
6. Chưa biểu diễn được quan hệ GVCN và GVBM.
7. Chưa có sổ điểm, cột điểm và điểm thành phần.
8. Chưa có lịch sử sửa điểm, khóa học kỳ và audit.
9. Chưa có bảng kết quả tổng kết hoặc task tính nền.
10. Chưa biểu diễn được một lần thi lại duy nhất cho mỗi môn trong một năm học.

### 2.3. Mục tiêu migration

Migration phải bảo toàn dữ liệu hiện tại theo hướng:

1. Đổi user thành app_user hoặc tạo app_user rồi chuyển dữ liệu.
2. Đổi password thành password_hash và bắt buộc xử lý lại dữ liệu mật khẩu.
3. Giữ nguyên student_id và student_code nếu không có lý do nghiệp vụ phải đổi.
4. Giữ student_info.student_id là khóa ngoại duy nhất.
5. Đưa average_score vào trạng thái deprecated.
6. Tạo các bảng nghiệp vụ mới.
7. Không tự sinh điểm trung bình chính thức từ average_score cũ nếu không có nguồn điểm chi tiết.

---

## 3. Quy ước cột dùng chung

Các bảng nghiệp vụ có thể dùng nhóm cột audit sau:

| Cột | Kiểu | Ý nghĩa |
|---|---|---|
| created_at | DATETIME | Thời điểm tạo |
| updated_at | DATETIME | Thời điểm cập nhật gần nhất |
| created_by | BIGINT UNSIGNED NULL | Người tạo |
| updated_by | BIGINT UNSIGNED NULL | Người cập nhật gần nhất |

created_by và updated_by tham chiếu app_user khi cần truy vết người thao tác. Các bảng lịch sử, audit và kết quả tính toán có thể dùng thêm các cột chuyên biệt như entered_by, reviewed_by, calculated_at.

Các bảng dữ liệu chính không được xóa vật lý tùy tiện sau khi đã phát sinh dữ liệu học tập. Sử dụng status hoặc deleted_at theo chính sách của module.

---

