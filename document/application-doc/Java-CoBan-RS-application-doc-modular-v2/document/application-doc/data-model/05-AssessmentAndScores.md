# Assessment and Scores

## 8. Sổ điểm và cấu hình cột điểm

### 8.1. scorebook

~~~text
scorebook
---------
PK  scorebook_id       BIGINT UNSIGNED
UQ/FK class_subject_id BIGINT UNSIGNED NOT NULL
    status              DRAFT | OPEN | PUBLISHED | CLOSED
    published_at        DATETIME NULL
FK  published_by        BIGINT UNSIGNED NULL
    closed_at            DATETIME NULL
    created_at           DATETIME NOT NULL
    updated_at           DATETIME NOT NULL
~~~

Mỗi class_subject có tối đa một scorebook.

### 8.2. assessment_column

~~~text
assessment_column
-----------------
PK  assessment_column_id  BIGINT UNSIGNED
FK  scorebook_id           BIGINT UNSIGNED NOT NULL
    assessment_type        KTTT | KTĐK | KTCK
    column_no               INT UNSIGNED NOT NULL
    column_name             VARCHAR(100) NULL
    weight_factor            DECIMAL(5,2) NULL
    is_required              BOOLEAN NOT NULL
    status                  ACTIVE | INACTIVE
    created_at              DATETIME NOT NULL
    updated_at              DATETIME NOT NULL
UQ  scorebook_id + assessment_type + column_no
~~~

Ràng buộc cấu hình:

- KTTT có thể có nhiều cột tùy môn.
- KTĐK phải có tối thiểu một cột.
- KTCK phải có đúng một cột.
- Môn kỹ năng phải có đủ ba cột cần thiết theo requirement.
- Cột bị vô hiệu hóa không nhận dữ liệu điểm mới.
- weight_factor phải phù hợp với công thức đã được phê duyệt; worker là nơi thực hiện tính toán.

Các điều kiện “tối thiểu một”, “đúng một” và “đủ ba cột” cần được kiểm tra khi publish hoặc khóa sổ, không chỉ khi tạo từng dòng.

### 8.3. skill_weight_config

~~~text
skill_weight_config
-------------------
PK  skill_weight_config_id  BIGINT UNSIGNED
UQ/FK scorebook_id           BIGINT UNSIGNED NOT NULL
    kttt_weight_percent      DECIMAL(5,2) NOT NULL
    ktdk_weight_percent      DECIMAL(5,2) NOT NULL
    ktck_weight_percent      DECIMAL(5,2) NOT NULL
FK  configured_by             BIGINT UNSIGNED NOT NULL
    configured_at              DATETIME NOT NULL
FK  locked_by                 BIGINT UNSIGNED NULL
    locked_at                 DATETIME NULL
~~~

Ràng buộc:

~~~text
kttt_weight_percent
+ ktdk_weight_percent
+ ktck_weight_percent = 100

ktck_weight_percent >= kttt_weight_percent
ktck_weight_percent >= ktdk_weight_percent
~~~

Sau khi khóa cấu hình, thay đổi trọng số phải đi qua quyền nghiệp vụ phù hợp và tạo calculation task.

---

## 9. Điểm học sinh

### 9.1. student_score

~~~text
student_score
-------------
PK  score_id              BIGINT UNSIGNED
FK  assessment_column_id  BIGINT UNSIGNED NOT NULL
FK  student_id            BIGINT UNSIGNED NOT NULL
    score_status          SCORED | ABSENT | EXEMPTED | CANCELLED
    score_value           DECIMAL(3,1) NULL
    note                  VARCHAR(500) NULL
FK  entered_by            BIGINT UNSIGNED NOT NULL
    entered_at            DATETIME NOT NULL
FK  updated_by            BIGINT UNSIGNED NULL
    updated_at            DATETIME NULL
    version               BIGINT UNSIGNED NOT NULL
UQ  assessment_column_id + student_id
~~~

Quy tắc:

- Điểm 0.0 là một giá trị hợp lệ.
- score_value phải nằm trong khoảng 0.0 đến 10.0 khi score_status là SCORED.
- ABSENT, EXEMPTED và CANCELLED không được coi là điểm 0.
- Ô chưa nhập không bắt buộc phải tạo student_score row.
- Ô có row nhưng status không tham gia công thức phải được báo cáo riêng.
- version dùng cho optimistic locking và kiểm tra xung đột khi sửa điểm.

DDL constraint đề xuất:

~~~sql
CHECK (
    (score_status = 'SCORED' AND score_value IS NOT NULL
        AND score_value >= 0.0 AND score_value <= 10.0)
    OR
    (score_status <> 'SCORED' AND score_value IS NULL)
)
~~~

### 9.2. Nguyên tắc kiểm tra quyền nhập điểm

Để tạo hoặc cập nhật student_score, hệ thống phải kiểm tra:

1. Người dùng có role TEACHER;
2. Người dùng liên kết với teacher;
3. teacher có subject_teaching_assignment ACTIVE;
4. assignment trỏ tới class_subject tương ứng;
5. học sinh thuộc lớp của class_subject trong năm học;
6. học kỳ và scorebook còn cho phép nhập;
7. nếu đã khóa sổ, phải tạo score_change_request thay vì sửa trực tiếp.

GVCN chỉ có quyền nghiệp vụ lớp theo assignment chủ nhiệm. Quyền nhập điểm vẫn phụ thuộc assignment giảng dạy môn học.

---

