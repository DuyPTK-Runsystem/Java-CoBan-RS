# Identity and Access

## 4. Tài khoản và phân quyền

### 4.1. app_user

~~~text
app_user
--------
PK  user_id              BIGINT UNSIGNED
UQ  user_name            VARCHAR(100) NOT NULL
    password_hash        VARCHAR(255) NOT NULL
    status               ACTIVE | LOCKED | DISABLED
    created_at            DATETIME NOT NULL
    updated_at            DATETIME NOT NULL
    created_by            BIGINT UNSIGNED NULL
    updated_by            BIGINT UNSIGNED NULL
~~~

Ràng buộc:

- user_name là duy nhất, không phân biệt hoa thường theo collation được chọn.
- Không trả password_hash trong API response.
- Khi tài khoản bị khóa hoặc vô hiệu hóa, không cho đăng nhập.

DDL mục tiêu:

~~~sql
CREATE TABLE app_user (
    user_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_app_user_user_name UNIQUE (user_name),
    CONSTRAINT ck_app_user_status
        CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED'))
);
~~~

### 4.2. role

~~~text
role
----
PK  role_id       BIGINT UNSIGNED
UQ  code          VARCHAR(50) NOT NULL
    name          VARCHAR(100) NOT NULL
    description   VARCHAR(255) NULL
~~~

Các role nền tảng:

~~~text
ADMIN
ACADEMIC_OFFICE
TEACHER
STUDENT
~~~

GVCN và GVBM không tạo thành role độc lập. Đây là vai trò nghiệp vụ được xác định từ bảng phân công.

### 4.3. user_role

~~~text
user_role
---------
PK/FK user_id    BIGINT UNSIGNED
PK/FK role_id    BIGINT UNSIGNED
    assigned_at  DATETIME NOT NULL
    assigned_by  BIGINT UNSIGNED NULL
~~~

Khóa chính gồm user_id và role_id. Một user có thể có nhiều role.

### 4.4. teacher

~~~text
teacher
-------
PK  teacher_id       BIGINT UNSIGNED
UQ/FK user_id        BIGINT UNSIGNED NULL
UQ  teacher_code     VARCHAR(50) NOT NULL
    teacher_name     VARCHAR(150) NOT NULL
    date_of_birth    DATE NULL
    gender           VARCHAR(20) NULL
    phone            VARCHAR(30) NULL
    email            VARCHAR(150) NULL
    join_date        DATE NULL
    status            ACTIVE | ON_LEAVE | INACTIVE
    created_at       DATETIME NOT NULL
    updated_at       DATETIME NOT NULL
~~~

user_id có thể NULL nếu hồ sơ giáo viên được tạo trước tài khoản đăng nhập. Khi được gán tài khoản, một tài khoản chỉ được liên kết với tối đa một hồ sơ giáo viên.
Hồ sơ giáo viên không lưu danh sách môn chuyên môn; quyền giảng dạy được xác định từ
`subject_teaching_assignment`.

---
