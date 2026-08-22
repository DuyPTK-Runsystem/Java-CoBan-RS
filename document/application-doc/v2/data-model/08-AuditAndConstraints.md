# Audit and Constraints

## 16. audit_log

~~~text
audit_log
---------
PK  audit_log_id      BIGINT UNSIGNED
FK  actor_user_id     BIGINT UNSIGNED NULL
    action            VARCHAR(100) NOT NULL
    entity_type       VARCHAR(100) NOT NULL
    entity_id         VARCHAR(100) NOT NULL
    before_data       JSON NULL
    after_data        JSON NULL
    request_id        VARCHAR(100) NULL
    ip_address        VARCHAR(45) NULL
    occurred_at       DATETIME NOT NULL
~~~

Phải ghi audit cho tối thiểu:

- Tạo, khóa, mở hoặc thay đổi tài khoản;
- Thay đổi role;
- Tạo hoặc sửa học sinh;
- Chuyển lớp;
- Thay đổi phân công GVCN/GVBM;
- Tạo, sửa, hủy điểm;
- Duyệt hoặc từ chối request sửa điểm;
- Publish hoặc close scorebook;
- Khóa hoặc mở học kỳ;
- Điều chỉnh điểm danh;
- Nhập hoặc sửa điểm thi lại;
- Chạy lại calculation task hoặc sửa kết quả tính toán bằng thao tác quản trị.

audit_log là bảng lịch sử, không xóa vật lý trong hoạt động thông thường.

---

## 17. Quan hệ tổng quát

~~~mermaid
flowchart TD
    AU["app_user"] --> UR["user_role"]
    AU --> ST["student"]
    AU --> TE["teacher"]
    R["role"] --> UR

    AY["academic_year"] --> SE["semester"]
    AY --> CL["school_class"]
    AY --> EN["student_year_enrollment"]
    GL["grade_level"] --> CL
    ST --> EN
    CL --> EN
    EN --> TR["class_transfer_history"]

    CL --> CS["class_subject"]
    SU["subject"] --> CS
    SE --> CS
    CS --> SB["scorebook"]
    SB --> AC["assessment_column"]
    AC --> SC["student_score"]
    ST --> SC
    CS --> TA["subject_teaching_assignment"]
    TE --> TA
    CL --> HA["homeroom_assignment"]
    TE --> HA

    ST --> AT["student_annual_transcript"]
    AY --> AT
    AT --> TT["student_term_transcript"]
    TT --> TSR["student_subject_term_result"]
    AT --> ASR["student_subject_annual_result"]
    SU --> ASR
    ASR --> RE["retake_exam"]
    AT --> CT["calculation_task"]
~~~

Các bảng audit có quan hệ đa hình tới entity nghiệp vụ thông qua entity_type và entity_id; không tạo một foreign key duy nhất tới nhiều loại bảng.

---

## 18. Foreign key và delete strategy

### 18.1. Cho phép cascade

Có thể dùng ON DELETE CASCADE cho:

- user_role khi app_user hoặc role bị xóa trước khi phát sinh lịch sử;
- student_info khi student bị xóa trong giai đoạn chưa có dữ liệu học tập.

### 18.2. Không cascade dữ liệu học tập

Không dùng ON DELETE CASCADE cho các quan hệ có lịch sử hoặc điểm:

- student_year_enrollment;
- class_transfer_history;
- student_score;
- score_change_request;
- attendance_record;
- transcript;
- retake_exam;
- audit_log.

Sau khi phát sinh dữ liệu học tập, student, teacher, subject, class và academic_year nên chuyển trạng thái thay vì xóa vật lý.

### 18.3. Quy tắc khi tài khoản bị xóa

Không xóa app_user nếu user đã xuất hiện trong audit hoặc đã thao tác nghiệp vụ. Chuyển status thành DISABLED và giữ dữ liệu lịch sử.

---

## 19. Index và unique constraint

### 19.1. Unique constraint

~~~text
app_user(user_name)
role(code)
teacher(user_id)
teacher(teacher_code)
student(user_id)
student(student_code)
student_info(student_id)
academic_year(code)
semester(academic_year_id, code)
school_class(academic_year_id, class_code)
class_subject(class_id, subject_id, semester_id)
student_year_enrollment(student_id, academic_year_id)
scorebook(class_subject_id)
assessment_column(scorebook_id, assessment_type, column_no)
student_score(assessment_column_id, student_id)
student_term_transcript(annual_transcript_id, semester_id)
student_subject_term_result(term_transcript_id, subject_id)
student_subject_annual_result(annual_transcript_id, subject_id)
retake_exam(student_id, academic_year_id, subject_id)
calculation_task(idempotency_key)
~~~

### 19.2. Index truy vấn

~~~text
student(student_name)
student(status)
teacher(status)
school_class(academic_year_id, grade_level_id)
class_subject(class_id, semester_id)
class_subject(subject_id, semester_id)
subject_teaching_assignment(class_subject_id, status)
subject_teaching_assignment(teacher_id, status)
homeroom_assignment(class_id, status)
homeroom_assignment(teacher_id, status)
student_score(student_id, assessment_column_id)
score_change_request(student_id, status)
attendance_session(class_id, semester_id, attendance_date)
attendance_record(session_id, student_id)
student_annual_transcript(student_id, academic_year_id)
student_term_transcript(annual_transcript_id, semester_id)
calculation_task(status, available_at)
calculation_task(student_id, academic_year_id, status)
audit_log(entity_type, entity_id, occurred_at)
~~~

Không tạo index trùng với index tự sinh từ UNIQUE constraint.

Plan 026 ghi audit event `STUDENT_ENROLLMENT_TRANSFER` trong cùng transaction với
`student_year_enrollment` và `class_transfer_history`. `before_data`/`after_data` chứa lớp
nguồn/đích, trạng thái và transfer id; `actor_user_id`, `request_id` và `ip_address` giữ context
thực hiện thao tác. Enrollment và transfer history không dùng cascade delete.

---
