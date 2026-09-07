# Data Structure

## Vai trò

Đây là file chính của tài liệu dữ liệu. Schema mục tiêu được phân rã theo nhóm bảng trong thư mục `data-model/`. Đọc file này để chọn đúng phần schema thay vì nạp toàn bộ Data Structure v2.

## Schema hiện tại

```text
user
student
student_info
```

Schema hiện tại phục vụ nền tảng CRUD/auth. Nó chưa đủ cho năm học, lớp, phân công, điểm danh, điểm số, tính toán và audit.

## Schema mục tiêu v2

```text
app_user ──< user_role >── role
    |
    └── teacher

academic_year ──< semester
       |
       └── school_class ──< student_year_enrollment >── student
                                  |
                                  └── class_transfer_history

school_class ──< class_subject >── subject
teacher ──< homeroom_assignment
teacher ──< subject_teaching_assignment >── class_subject

scorebook ──< assessment_column ──< student_score
student ──< attendance_record >── attendance_session
student ──< student_*_transcript / subject_result
```

## Quy ước thiết kế cốt lõi

- PK/FK mục tiêu dùng `BIGINT UNSIGNED` đồng nhất.
- Database dùng `snake_case`.
- Mật khẩu dùng `password_hash VARCHAR(255)`, không lưu plaintext.
- `average_score` cũ là dữ liệu deprecated, không phải nguồn điểm chính thức.
- Điểm thành phần là nguồn; điểm trung bình và transcript là dữ liệu dẫn xuất.
- Tổng kết dùng `IN_PROGRESS` và `FINISH`; `FAILED` chỉ thuộc calculation task.
- Ngày sinh dùng `DATE`; thời điểm audit/calculation dùng `DATETIME` hoặc `TIMESTAMP` thống nhất.
- Không cascade xóa dữ liệu lịch sử học tập.

## Data model chi tiết

- [`data-model/00-OverviewAndMigration.md`](data-model/00-OverviewAndMigration.md)
- [`data-model/01-IdentityAndAccess.md`](data-model/01-IdentityAndAccess.md)
- [`data-model/02-AcademicCatalog.md`](data-model/02-AcademicCatalog.md)
- [`data-model/03-StudentsAndEnrollment.md`](data-model/03-StudentsAndEnrollment.md)
- [`data-model/04-TeachingAssignments.md`](data-model/04-TeachingAssignments.md)
- [`data-model/05-AssessmentAndScores.md`](data-model/05-AssessmentAndScores.md)
- [`data-model/06-ChangesAndAttendance.md`](data-model/06-ChangesAndAttendance.md)
- [`data-model/07-ResultsAndCalculation.md`](data-model/07-ResultsAndCalculation.md)
- [`data-model/08-AuditAndConstraints.md`](data-model/08-AuditAndConstraints.md)
- [`data-model/09-MigrationAndJPA.md`](data-model/09-MigrationAndJPA.md)

## Quan hệ với requirement

| Nghiệp vụ | Data model |
|---|---|
| Auth/role/teacher | `01-IdentityAndAccess.md` |
| Khối, năm học, kỳ, lớp, môn | `02-AcademicCatalog.md` |
| Student, enrollment, transfer | `03-StudentsAndEnrollment.md` |
| Homeroom/subject teaching | `04-TeachingAssignments.md` |
| Scorebook, column, score | `05-AssessmentAndScores.md` |
| Score change, attendance | `06-ChangesAndAttendance.md` |
| Transcript, retake, calculation | `07-ResultsAndCalculation.md` |
| Audit, FK, indexes, uniqueness | `08-AuditAndConstraints.md` |
| Migration, DDL, JPA | `09-MigrationAndJPA.md` |

## Cảnh báo trước implementation

Các mục sau vẫn cần chốt trước migration production:

1. Công cụ migration chính thức.
2. Chiến lược đổi `user` thành `app_user`.
3. Quy tắc xử lý password hash của dữ liệu cũ.
4. Có giữ `average_score` để tương thích đọc tạm thời hay loại bỏ sau migration.
5. Chi tiết `DATE`/`DATETIME` ở các nghiệp vụ không phải ngày sinh.

