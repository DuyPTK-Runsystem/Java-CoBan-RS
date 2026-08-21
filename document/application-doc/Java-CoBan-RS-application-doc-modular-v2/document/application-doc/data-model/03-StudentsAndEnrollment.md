# Students and Enrollment

## 6. Học sinh và xếp lớp

### 6.1. student

~~~text
student
-------
PK  student_id       BIGINT UNSIGNED
UQ/FK user_id        BIGINT UNSIGNED NULL
UQ  student_code     VARCHAR(30) NOT NULL
    student_name     VARCHAR(150) NOT NULL
    status            ACTIVE | INACTIVE | GRADUATED
    created_at        DATETIME NOT NULL
    updated_at        DATETIME NOT NULL
~~~

user_id NULL cho phép tạo hồ sơ học sinh trước khi cấp tài khoản đăng nhập. student_code phải duy nhất trên toàn hệ thống.

### 6.2. student_info

~~~text
student_info
------------
PK  info_id          BIGINT UNSIGNED
UQ/FK student_id     BIGINT UNSIGNED NOT NULL
    address          VARCHAR(255) NULL
    date_of_birth    DATE NULL
    gender           VARCHAR(20) NULL
    created_at        DATETIME NOT NULL
    updated_at        DATETIME NOT NULL
~~~

Quan hệ:

~~~text
student 1 ───── 0..1 student_info
~~~

student_info.student_id là NOT NULL và UNIQUE. info_id là khóa chính duy nhất; không dùng student_id đồng thời làm một phần của khóa chính.

average_score không thuộc schema mục tiêu. Nếu cần giữ tạm trong migration để tương thích dữ liệu cũ, phải đánh dấu deprecated và không được dùng làm dữ liệu chính thức.

### 6.3. student_year_enrollment

~~~text
student_year_enrollment
-----------------------
PK  enrollment_id       BIGINT UNSIGNED
FK  student_id          BIGINT UNSIGNED NOT NULL
FK  academic_year_id    BIGINT UNSIGNED NOT NULL
FK  current_class_id    BIGINT UNSIGNED NOT NULL
    status              ACTIVE | COMPLETED | WITHDRAWN
    enrolled_at         DATETIME NOT NULL
    completed_at        DATETIME NULL
    created_at          DATETIME NOT NULL
    updated_at          DATETIME NOT NULL
UQ  student_id + academic_year_id
~~~

Mỗi học sinh có tối đa một hồ sơ học tập trong một năm học. current_class_id là lớp hiện tại; lịch sử trước đó nằm trong class_transfer_history.

Trong Plan 026, chuyển lớp nội bộ giữ `status = ACTIVE` và chỉ cập nhật `current_class_id`; `COMPLETED` dành cho kết năm và `WITHDRAWN` dành cho thôi học ở change request riêng.

### 6.4. class_transfer_history

~~~text
class_transfer_history
----------------------
PK  transfer_id       BIGINT UNSIGNED
FK  enrollment_id     BIGINT UNSIGNED NOT NULL
FK  from_class_id     BIGINT UNSIGNED NULL
FK  to_class_id       BIGINT UNSIGNED NOT NULL
    effective_at      DATETIME NOT NULL
    reason            VARCHAR(500) NULL
FK  approved_by       BIGINT UNSIGNED NULL
    created_at        DATETIME NOT NULL
~~~

Thay đổi lớp phải thực hiện trong một transaction:

1. Cập nhật current_class_id;
2. Thêm class_transfer_history;
3. Ghi audit_log;
4. Đánh dấu các tổng kết bị ảnh hưởng là IN_PROGRESS nếu cần.

Không xóa lịch sử chuyển lớp.

---
