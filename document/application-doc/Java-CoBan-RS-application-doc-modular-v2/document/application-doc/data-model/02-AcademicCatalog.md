# Academic Catalog

## 5. Năm học, học kỳ, khối, lớp và môn học

### 5.1. academic_year

~~~text
academic_year
-------------
PK  academic_year_id  BIGINT UNSIGNED
UQ  code               VARCHAR(20) NOT NULL
    start_date         DATE NOT NULL
    end_date           DATE NOT NULL
    status              DRAFT | ACTIVE | CLOSED
    created_at          DATETIME NOT NULL
    updated_at          DATETIME NOT NULL
~~~

Ví dụ code: 2025-2026.

Ràng buộc:

- code là duy nhất.
- end_date phải lớn hơn start_date.
- Chỉ có một năm học được ACTIVE nếu nghiệp vụ nhà trường yêu cầu.

### 5.2. semester

~~~text
semester
--------
PK  semester_id        BIGINT UNSIGNED
FK  academic_year_id   BIGINT UNSIGNED NOT NULL
    code               HK1 | HK2
    start_date         DATE NOT NULL
    end_date           DATE NOT NULL
    status              OPEN | LOCKED
    locked_at           DATETIME NULL
    locked_by           BIGINT UNSIGNED NULL
    created_at          DATETIME NOT NULL
    updated_at          DATETIME NOT NULL
UQ  academic_year_id + code
~~~

Học kỳ được khóa khi:

1. Giáo vụ xác nhận khóa; hoặc
2. Đã qua 45 ngày dương lịch kể từ ngày kết thúc học kỳ.

Việc còn thiếu một số ô điểm không tự động ngăn khóa học kỳ. Các thiếu sót phải được ghi nhận trong báo cáo ngoại lệ.

### 5.3. grade_level

~~~text
grade_level
-----------
PK  grade_level_id  BIGINT UNSIGNED
UQ  code             VARCHAR(10) NOT NULL
    name             VARCHAR(50) NOT NULL
    grade_level      INT NOT NULL
    display_order    INT NOT NULL
    next_grade_id    BIGINT UNSIGNED NULL
    active           BOOLEAN NOT NULL
    description      VARCHAR(255) NULL
    created_at       DATETIME NOT NULL
    updated_at       DATETIME NOT NULL
~~~

Ví dụ code: 6, 7, 8, 9.

### 5.4. school_class

~~~text
school_class
------------
PK  class_id          BIGINT UNSIGNED
FK  academic_year_id  BIGINT UNSIGNED NOT NULL
FK  grade_level_id    BIGINT UNSIGNED NOT NULL
UQ  class_code        VARCHAR(30) NOT NULL
    class_name        VARCHAR(100) NULL
    capacity          INT UNSIGNED NULL
    status             PLANNED | ACTIVE | CLOSED
    created_at         DATETIME NOT NULL
    updated_at         DATETIME NOT NULL
UQ  academic_year_id + class_code
~~~

class_code chỉ cần duy nhất trong phạm vi một năm học. Hai năm học khác nhau có thể có cùng mã lớp.

### 5.5. subject

~~~text
subject
-------
PK  subject_id    BIGINT UNSIGNED
UQ  code          VARCHAR(30) NOT NULL
    name          VARCHAR(150) NOT NULL
    subject_type  NORMAL | SKILL
    status        ACTIVE | INACTIVE
    created_at    DATETIME NOT NULL
    updated_at    DATETIME NOT NULL
~~~

Môn NORMAL được tham gia tính Đtbmh, Đtbhk, ĐtbmhCN và Đtbcn theo requirement. Môn SKILL có điểm môn kỹ năng riêng và không tham gia công thức điểm trung bình quy định cho môn thông thường.

### 5.6. class_subject

~~~text
class_subject
-------------
PK  class_subject_id  BIGINT UNSIGNED
FK  class_id          BIGINT UNSIGNED NOT NULL
FK  subject_id        BIGINT UNSIGNED NOT NULL
FK  semester_id       BIGINT UNSIGNED NOT NULL
    status             ACTIVE | INACTIVE | COMPLETED
    created_at         DATETIME NOT NULL
    updated_at         DATETIME NOT NULL
UQ  class_id + subject_id + semester_id
~~~

class_subject là đơn vị trung tâm để:

- Phân công giáo viên bộ môn;
- Tạo sổ điểm;
- Cấu hình cột điểm;
- Nhập điểm cho học sinh trong lớp;
- Kiểm tra quyền nhập điểm.

---
