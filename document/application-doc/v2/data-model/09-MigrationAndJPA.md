# Migration and JPA

## 20. DDL skeleton theo thứ tự migration

Thứ tự tạo bảng đề xuất:

~~~text
V2__rename_user_and_adjust_student_schema.sql
V3__create_role_and_user_role.sql
V4__create_teacher_and_academic_structure.sql
V5__create_student_enrollment_and_assignment.sql
V6__create_scorebook_and_assessment.sql
V7__create_student_score_and_score_change_request.sql
V8__create_attendance.sql
V9__create_transcript_and_retake.sql
V10__create_calculation_task.sql
V11__create_audit_log_and_indexes.sql
~~~

Thứ tự phụ thuộc:

~~~text
app_user
    ↓
role, user_role, teacher, student
    ↓
academic_year, semester, grade_level, school_class
    ↓
student_info, student_year_enrollment
    ↓
class_subject, assignments
    ↓
scorebook, assessment_column, skill_weight_config
    ↓
student_score, score_change_request
    ↓
attendance
    ↓
transcript, retake_exam
    ↓
calculation_task, audit_log
~~~

Không dùng ddl-auto=update làm chiến lược migration production. Entity, migration và database schema phải được kiểm tra đồng bộ.

---

## 21. Mapping JPA đề xuất

### 21.1. Kiểu dữ liệu

| Database | Java |
|---|---|
| BIGINT UNSIGNED | Long |
| VARCHAR | String |
| DECIMAL(3,1) | BigDecimal |
| DATE | LocalDate |
| DATETIME | LocalDateTime |
| JSON | JsonNode, Map hoặc String theo cách serialize |
| BOOLEAN | Boolean |

Không dùng double cho điểm chính thức vì điểm cần độ chính xác thập phân cố định. Dùng BigDecimal với precision và scale rõ ràng.

### 21.2. Enum

Các enum phải map bằng chuỗi:

~~~java
@Enumerated(EnumType.STRING)
~~~

Không lưu ordinal của enum vì việc thay đổi thứ tự enum có thể làm sai dữ liệu cũ.

### 21.3. Quan hệ

- Dùng @OneToOne cho student và student_info.
- Dùng @ManyToOne cho các quan hệ tới academic_year, semester, class, subject và user.
- Không dùng quan hệ JPA hai chiều nếu API không cần navigation hai chiều.
- Dùng LAZY cho collection.
- Không dùng @Data trên entity.
- Không trả entity trực tiếp từ controller; dùng DTO.
- Không đưa password_hash vào DTO response.

### 21.4. ID và audit

ID strategy phải thống nhất với schema. Nếu dùng database auto increment, mọi entity phải mapping đúng generated identity. created_at và updated_at nên dùng entity listener hoặc base auditable entity chung.

---

## 22. Quy tắc nhất quán dữ liệu

### 22.1. Consistency khi nhập điểm

Một transaction nhập điểm phải bảo đảm:

~~~text
student_score đã lưu
source_version đã tăng
transcript = IN_PROGRESS
calculation_task đã tạo hoặc được gộp
audit_log đã ghi
~~~

Không chấp nhận trạng thái chỉ lưu điểm nhưng không tạo task.

### 22.2. Consistency khi worker hoàn tất

Worker chỉ commit kết quả khi:

1. Tính xong toàn bộ dependency cần thiết;
2. Kết quả được lưu thành công;
3. calculated_version khớp source_version;
4. Không phát hiện phiên bản nguồn mới hơn;
5. Transcript chuyển FINISH trong cùng transaction với kết quả.

### 22.3. Dữ liệu thiếu

Dữ liệu thiếu không được biến thành 0. Hệ thống cần phân biệt:

~~~text
NULL / không có row  = chưa nhập hoặc chưa có dữ liệu
0.0                  = đã nhập điểm 0
ABSENT               = vắng
EXEMPTED             = miễn
CANCELLED            = hủy
~~~

Các trường hợp thiếu cấu hình hoặc thiếu điểm phải xuất hiện trong báo cáo ngoại lệ trước khi người dùng công bố bảng điểm.

---

## 23. Tóm tắt schema mục tiêu

~~~text
app_user
role
user_role
teacher

academic_year
semester
grade_level
school_class
subject
class_subject

student
student_info
student_year_enrollment
class_transfer_history

homeroom_assignment
subject_teaching_assignment

scorebook
assessment_column
skill_weight_config
student_score
score_change_request

attendance_session
attendance_record

student_annual_transcript
student_term_transcript
student_subject_term_result
student_subject_annual_result
retake_exam

calculation_task
audit_log
~~~

Luồng dữ liệu chính:

~~~text
Student
  ↓
Enrollment theo AcademicYear
  ↓
SchoolClass
  ↓
ClassSubject
  ↓
Scorebook
  ↓
AssessmentColumn
  ↓
StudentScore
  ↓
BackgroundCalculation
  ↓
TermTranscript / AnnualTranscript
  ↓
RetakeExam nếu có
  ↓
Official Annual Result
~~~

---

## 24. Các quyết định đã chốt trong v2

1. Schema mục tiêu dùng app_user thay cho user.
2. Mật khẩu lưu bằng password_hash VARCHAR(255), không lưu plaintext.
3. student_code là duy nhất.
4. student_info.student_id là foreign key và unique.
5. average_score không còn là nguồn điểm chính.
6. date_of_birth dùng DATE.
7. Một giáo viên có thể đồng thời là GVCN và GVBM.
8. GVCN không tự động có quyền nhập điểm mọi môn.
9. Quyền nhập điểm dựa trên subject_teaching_assignment.
10. Mỗi học sinh có tối đa một enrollment trong một năm học.
11. Mỗi học sinh và môn học trong một năm học có tối đa một retake_exam.
12. Đtlmh thay thế ĐtbmhCN của đúng môn thi lại.
13. Đtbcn được tính lại từ toàn bộ official_dtbmh_cn.
14. Tính điểm là background process, tuyệt đối không tính trong HTTP request.
15. Transcript dùng IN_PROGRESS và FINISH.
16. Calculation task có retry, idempotency và version protection.
17. Dữ liệu lịch sử điểm và audit không bị xóa dây chuyền.

---

## 25. Checklist trước khi triển khai

- [ ] Chốt việc đổi tên user thành app_user.
- [ ] Xác nhận migration password sang password_hash.
- [ ] Chuyển đồng nhất INT/BIGINT nếu database hiện tại đang dùng INT.
- [ ] Xác nhận danh sách role và quyền chi tiết.
- [ ] Xác nhận công thức Đtbmh và ĐtbmhCN trong Requirement Baseline.
- [ ] Xác nhận danh sách môn SKILL.
- [ ] Xác nhận số cột KTĐK tối thiểu của từng loại môn.
- [ ] Xác nhận trọng số môn kỹ năng.
- [ ] Xác nhận điều kiện học sinh phải thi lại.
- [ ] Xác nhận API đọc trạng thái IN_PROGRESS/FINISH.
- [ ] Tạo migration Flyway theo thứ tự phụ thuộc.
- [ ] Viết test migration và test constraint.
- [ ] Viết test worker cho retry, idempotency và source version.
- [ ] Kiểm tra không có HTTP path nào tính trực tiếp điểm tổng kết.

