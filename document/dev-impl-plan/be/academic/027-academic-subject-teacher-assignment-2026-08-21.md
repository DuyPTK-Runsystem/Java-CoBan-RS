# Developer Plan 027: Academic Subject & Teacher Assignment

## 1. Trạng thái và phiên bản áp dụng

- Status: `Draft - awaiting user approval`.
- Application-document version: `v2`.
- Ngày lập plan: `2026-08-21`.
- Phụ thuộc: Plan 025 (contract/migration/scope freeze) và Plan 026 (academic year,
  grade, school class, student enrollment và audit log).
- Không thay đổi API legacy `/api/v1/**` và không sửa `Student.java`.
- Chỉ bắt đầu implementation sau khi user phê duyệt plan và chốt các decision gate ở mục 9.

## 2. Mục tiêu

Xây dựng backend foundation cho môn học, học kỳ, hồ sơ giáo viên, cấu hình môn của lớp
và hai loại phân công:

1. `homeroom_assignment`: xác định GVCN theo lịch sử, không lưu GVCN trong `school_class`.
2. `subject_teaching_assignment`: xác định GVBM có quyền thao tác trên một
   `class_subject` cụ thể.

Kết quả phải bảo đảm:

- một giáo viên có thể đồng thời là GVCN và GVBM, kể cả trên cùng một lớp;
- một bộ `{môn học, lớp học, học kỳ}` chỉ có một giáo viên `ACTIVE` tại một thời điểm;
- thay giáo viên kết thúc assignment cũ và tạo assignment mới trong cùng transaction;
- điểm đã có không bị thay đổi khi assignment được thay thế;
- các module điểm sau này có thể tái sử dụng service kiểm tra quyền từ
  `subject_teaching_assignment`, không suy quyền từ role `TEACHER`, GVCN hoặc môn chuyên môn.

## 3. Requirement liên quan

### 3.1. Functional requirements

- `FR-SEM-001`–`FR-SEM-008`: tạo, cập nhật, kích hoạt, khóa/mở khóa và xem học kỳ.
- `FR-TEACHER-001`–`FR-TEACHER-006`: hồ sơ giáo viên, tài khoản liên kết, trạng thái và
  lịch sử phân công; hồ sơ gồm identity/contact fields và không quản lý danh sách môn
  chuyên môn.
- `FR-ASSIGN-001`–`FR-ASSIGN-007`: phân công GVCN/GVBM, thay đổi, truy vấn theo giáo viên/lớp,
  kết thúc và kiểm tra trùng.
- `FR-SUBJECT-001`–`FR-SUBJECT-005`: CRUD môn, loại môn, phạm vi áp dụng và danh sách môn.

### 3.2. Business rules và non-functional requirements

- `BR-CLASS-001`: GVCN không phải là field cố định của `school_class`.
- `BR-SEM-001`–`BR-SEM-003`: học kỳ thuộc đúng năm học và nằm trong thời gian năm học.
- `BR-TEACHER-001`–`BR-TEACHER-006`: teacher code duy nhất, identity/contact fields,
  lifecycle `ACTIVE | ON_LEAVE | INACTIVE`, không mất lịch sử và quyền giảng dạy được
  xác định từ assignment.
- `BR-ASSIGN-001`: một lớp chỉ có một GVCN `ACTIVE` tại một thời điểm.
- `BR-ASSIGN-002`: một `{subject, class, semester}` chỉ có một GVBM `ACTIVE`.
- `BR-ASSIGN-003`: quyền nhập điểm tương lai dựa trên assignment active tương ứng.
- `BR-ASSIGN-005`–`BR-ASSIGN-007`: thay assignment không mất dữ liệu, không chồng thời gian,
  và nằm trong năm học/học kỳ tương ứng.
- `BR-SUBJECT-001`–`BR-SUBJECT-004`: loại môn, kỳ áp dụng và kiểm tra môn phù hợp với khối.
- `BR-COMMON-001`–`BR-COMMON-004`: giữ lịch sử, không xóa cứng dữ liệu đã phát sinh.
- `BR-AUTH-003`–`BR-AUTH-006`, `NFR-SECURITY-003`: backend kiểm tra phạm vi giáo viên từ
  phân công thực tế.
- `NFR-RELIABILITY-005`: thay phân công phải atomic.
- `NFR-AUDITABILITY-002`: mọi thay đổi phân công phải có audit log.

## 4. Phạm vi

### 4.1. In-scope

- Thêm `semester` vào academic foundation hiện có.
- Tạo/cập nhật/list/đổi trạng thái hồ sơ `teacher`, liên kết tùy chọn với `app_user`,
  gồm ngày sinh, giới tính, điện thoại, email và ngày vào trường.
- Tạo/cập nhật/list/đổi trạng thái `subject`.
- Cấu hình `class_subject` cho một lớp, môn và học kỳ; validate lớp, khối, môn và học kỳ
  tương thích.
- Tạo, xem, thay thế và kết thúc `homeroom_assignment`.
- Tạo, xem, thay thế và kết thúc `subject_teaching_assignment`.
- Query assignment theo teacher, class, class-subject, semester và status; list môn trong
  phạm vi của giáo viên/lớp/học sinh theo contract v2 đã chốt.
- Service dùng chung để kiểm tra `teacher` đang có assignment `ACTIVE` cho một
  `classSubjectId` tại ngày nghiệp vụ. Service này là extension point bắt buộc cho module
  score, nhưng Plan 027 không triển khai API nhập điểm.
- Audit các thao tác tạo, thay thế, kết thúc assignment và thay đổi dữ liệu catalog có yêu
  cầu audit trong cùng transaction.
- Flyway migration, JPA mapping, unit test, integration test, authorization test và
  migration constraint test.

### 4.2. Out-of-scope

- Scorebook, assessment column, student score, sửa điểm, calculation task và transcript.
- API nhập điểm hoặc bất kỳ phép tính điểm trung bình nào.
- Lịch học, attendance, teaching session và schedule.
- Frontend, Storybook và Postman collection.
- Tự động tạo class-subject hàng loạt theo toàn bộ trường nếu chưa có contract riêng.
- Xóa cứng teacher, subject, class-subject hoặc assignment đã có lịch sử.
- Dùng role `GVCN`/`GVBM`; đây là vai trò nghiệp vụ từ hai bảng assignment, không phải role
  trong `role`/`user_role`.
- Không tạo hoặc quản lý `teacher_subject_specialty`; danh sách môn chuyên môn của giáo viên
  bị loại khỏi Plan 027. Quyền giảng dạy chỉ dựa trên assignment.
- Gửi email, cấu hình SMTP/email provider, email template, notification log, idempotency,
  retry/delivery workflow và persistence notification; các phần này được ghi chú để quay lại
  trong Implementation Note 27.1, không triển khai trong Plan 027. Plan 027 chỉ kiểm tra
  completeness tại các checkpoint G3 và trả output `NEEDS_NOTIFICATION` hoặc `NO_NOTIFICATION`.

### 4.3. Implementation Note 27.1

Trong Plan 027, G3/`CR-SEM-001` chỉ implement kiểm tra completeness tại các checkpoint sau và
trả output quyết định có cần gửi thông báo hay không:

```text
  t-20d, t-10d, t-5d, t-3d, t-2d, t-1d,
  t,     t+1d,  t+3d,  t+5d,  t+7d,  t+14d
```

Các phần code tương lai ngoài Plan 027 sẽ được xử lý trong một plan/delta riêng, tối thiểu gồm:

- scheduler đầy đủ và persistence/idempotency log cho từng `{semester, checkpoint}`;
- email delivery, email configuration, template và retry/outbox policy;
- test scheduler, notification retry, duplicate prevention và email failure.

Plan 027 không gửi thông báo và không tạo persistence, idempotency, retry/delivery component.

## 5. Kiến trúc và luồng hiện tại

Backend là Spring Boot modular monolith, dùng Spring Data JPA, Flyway, JWT và
`@PreAuthorize`. Plan 026 đã có:

- package `academic` với `AcademicYear`, `GradeLevel`, `SchoolClass` và repository/service/controller;
- package `enrollment` với student enrollment, class transfer và `EnrollmentAuditService`;
- `AuditLog`/`AuditContext` dùng chung;
- API mới dưới `/api/v2/**`, role `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`, `STUDENT`.

Plan 027 mở rộng theo dependency:

```text
academic_year -> semester
academic_year + grade_level + school_class -> class_subject <- subject
app_user -> teacher
school_class + teacher -> homeroom_assignment
class_subject + teacher -> subject_teaching_assignment
```

Giữ convention hiện tại: entity dùng scalar `Long` cho foreign-key id, DTO không lộ entity,
controller mỏng, service giữ business rule/transaction, repository chỉ truy vấn persistence.
Không tạo quan hệ JPA hai chiều nếu API không cần navigation.

## 6. Phương án triển khai

### 6.1. Domain và entity

Tạo các entity/enums sau, sử dụng `@Enumerated(EnumType.STRING)`, `Long`, `LocalDate` và
`LocalDateTime`:

- `academic`: `Semester`, `SemesterStatus`, `Subject`, `SubjectType`, `SubjectStatus`,
  `ClassSubject`, `ClassSubjectStatus`.
- `teacher`: `Teacher`, `TeacherStatus`.
- `assignment`: `HomeroomAssignment`, `SubjectTeachingAssignment`, `AssignmentStatus`.

Các entity có `createdAt`/`updatedAt`; `Teacher` có `dateOfBirth`, `gender`, `phone`, `email`,
`joinDate`, `status=ACTIVE|ON_LEAVE|INACTIVE`; assignment có `validFrom`, `validTo`,
`status` và `assignedBy`. `school_class` không thêm `homeroomTeacherId` hoặc field tương đương.

### 6.2. Catalog validation

- `Semester`: `academicYearId` tồn tại; ngày nằm trong khoảng năm học; code duy nhất trong
  năm học; không cho chồng lấn học kỳ trong cùng năm học.
- `Subject`: code duy nhất; tên bắt buộc; chỉ cho trạng thái inactive khi không tạo dữ liệu
  mới; không xóa record đã được tham chiếu.
- Việc môn có tham gia tính điểm trung bình học kỳ/năm hay không được suy ra từ
  `subject_type`; không tạo field/config boolean `counts_in_average` trong Plan 027.
- `ClassSubject`: class thuộc academic year của semester; subject active; class/semester
  ở trạng thái cho phép; unique `(class_id, subject_id, semester_id)`.
- `ClassSubject`: chỉ tạo khi có một `subject_applicability` ACTIVE khớp với subject,
  semester và target của class; không để assignment tự bỏ qua kiểm tra này.
- Môn `SKILL` chỉ được cấu hình/tổng kết trong một học kỳ; rule này được validate ở catalog,
  không đợi tới score module.

`subject_type` và `application_scope` là hai khái niệm độc lập:

- `subject_type = ACADEMIC | SKILL` mô tả cách môn tham gia nghiệp vụ điểm;
- `application_scope = GRADE | CLASS` mô tả target dùng khi cấu hình môn.

Vì vậy code không được hardcode quan hệ `ACADEMIC -> GRADE` hoặc `SKILL -> CLASS`.
Mỗi subject lưu scope của chính nó; dữ liệu seed/API có thể đặt mặc định hiện tại là
`ACADEMIC/GRADE` và `SKILL/CLASS`, nhưng validator chỉ đọc `application_scope`.

`subject_applicability` dùng một mô hình target chung:

```text
subject + semester + scope_type + grade_level_id(nullable) + class_id(nullable)
```

Với `scope_type=GRADE`, chỉ `grade_level_id` được phép có giá trị. Với `scope_type=CLASS`,
chỉ `class_id` được phép có giá trị. Database dùng hai foreign key nullable và CHECK để
giữ tính nhất quán; service kiểm tra `scope_type` khớp `subject.application_scope`.
`class_subject` vẫn là offering thực tế của một lớp trong một học kỳ và là parent của
`subject_teaching_assignment`.

### 6.3. Assignment transaction và concurrency

- Tạo assignment mới chỉ thành công khi không có assignment `ACTIVE` tương ứng.
- Endpoint thay thế là thao tác explicit: khóa parent row (`school_class` cho homeroom,
  `class_subject` cho GVBM), đọc assignment active, kết thúc record cũ và tạo record mới
  trong một `@Transactional` transaction.
- `validFrom`/`validTo` phải hợp lệ; assignment GVCN nằm trong academic year, assignment
  GVBM nằm trong semester và không được chồng khoảng hiệu lực.
- Nếu có assignment active khác hoặc dữ liệu cạnh tranh, trả `409 CONFLICT`; không tự động
  overwrite assignment cũ từ endpoint create.
- Kết thúc assignment ghi `status=ENDED`, `validTo` và audit; schema không thêm trạng thái
  `CANCELLED` riêng khi baseline chưa định nghĩa dữ liệu đó.
- Teacher phải `ACTIVE` để nhận assignment mới. Teacher `INACTIVE` vẫn xem được lịch sử.
- Thay GVBM chỉ đổi assignment; không đụng tới score/scorebook của `class_subject`.

### 6.4. Authorization và future score access

- Mutation catalog/teacher/assignment: `ADMIN`, `ACADEMIC_OFFICE`.
- Read metadata/assignment: `ADMIN`, `ACADEMIC_OFFICE`; `TEACHER` chỉ xem phạm vi liên quan
  tới teacher account; `STUDENT` chỉ xem môn thuộc enrollment của chính mình khi endpoint
  student-scope được triển khai.
- `GVCN` không có quyền nhập điểm từ homeroom assignment. `TEACHER` không có quyền nhập
  điểm chỉ vì có role.
- Tạo `SubjectTeachingAssignmentAccessService` với contract nội bộ tương đương:
  `assertActiveAssignment(teacherId, classSubjectId, effectiveDate)` và
  `hasActiveAssignment(...)`. Score module sẽ gọi service này khi được triển khai.
- Mapping `app_user -> teacher` phải duy nhất nếu có; account chưa liên kết vẫn cho phép
  lưu hồ sơ teacher nhưng chưa có context để đăng nhập dưới role `TEACHER`.

### 6.5. Audit

Tái sử dụng `AuditLog` và `AuditContext`, không tạo bảng audit mới. Tối thiểu ghi:

- `TEACHER_CREATED`, `TEACHER_UPDATED`, `TEACHER_STATUS_CHANGED`;
- `SUBJECT_CREATED`, `SUBJECT_UPDATED`, `SUBJECT_STATUS_CHANGED`;
- `HOMEROOM_ASSIGNMENT_CREATED`, `HOMEROOM_ASSIGNMENT_REPLACED`,
  `HOMEROOM_ASSIGNMENT_ENDED`;
- `SUBJECT_TEACHING_ASSIGNMENT_CREATED`, `SUBJECT_TEACHING_ASSIGNMENT_REPLACED`,
  `SUBJECT_TEACHING_ASSIGNMENT_ENDED`.

Với thay thế assignment, `before_data` chứa assignment cũ và `after_data` chứa assignment
mới, actor/request id/IP lấy từ context. Audit và thay đổi assignment phải cùng commit/rollback.

## 7. API contract v2 dự kiến

Response tiếp tục dùng `RestResponse`/`@ApiMessage` hiện có; request/response dùng DTO riêng.
Các mutation trả `201 Created` khi tạo mới, `200 OK` khi update/replace/end và `409 Conflict`
khi vi phạm assignment active hoặc khoảng hiệu lực.

| Method | Path | Quyền | Mục đích |
|---|---|---|---|
| `GET/POST/PUT` | `/api/v2/semesters` | office; teacher/student GET theo scope | Quản lý học kỳ |
| `POST` | `/api/v2/semesters/{semesterId}/activate` | office | Kích hoạt học kỳ |
| `POST` | `/api/v2/semesters/{semesterId}/lock` | office | Khóa học kỳ, không tính điểm |
| `POST` | `/api/v2/semesters/{semesterId}/reopen` | office | Mở lại có lý do và audit |
| `GET/POST/PUT` | `/api/v2/subjects` | office mutation; authenticated GET | Quản lý môn |
| `POST/PUT` | `/api/v2/class-subjects` | office | Cấu hình môn cho lớp/học kỳ |
| `GET` | `/api/v2/classes/{classId}/subjects` | office/teacher/student theo scope | Môn của lớp |
| `GET/POST/PUT` | `/api/v2/teachers` | office mutation; authenticated GET | Hồ sơ giáo viên |
| `GET` | `/api/v2/assignments/teachers/{teacherId}` | office hoặc chính teacher | Lịch sử assignment theo teacher |
| `GET` | `/api/v2/assignments/classes/{classId}` | office/teacher trong scope | Assignment theo lớp |
| `POST` | `/api/v2/classes/{classId}/homeroom-assignments` | office | Tạo GVCN |
| `POST` | `/api/v2/homeroom-assignments/{assignmentId}/replace` | office | Thay GVCN atomic |
| `POST` | `/api/v2/homeroom-assignments/{assignmentId}/end` | office | Kết thúc GVCN |
| `POST` | `/api/v2/class-subjects/{classSubjectId}/teaching-assignments` | office | Tạo GVBM |
| `POST` | `/api/v2/subject-teaching-assignments/{assignmentId}/replace` | office | Thay GVBM atomic |
| `POST` | `/api/v2/subject-teaching-assignments/{assignmentId}/end` | office | Kết thúc GVBM |

API chính thức phải chốt tên path/response shape trước khi coding; bảng trên là contract đề
xuất để review, không phải quyền tự mở rộng scope.

## 8. Database và migration dự kiến

Tạo migration kế tiếp sau V4 hiện có:

`BE/BaiTap-RS/src/main/resources/db/migration/V5__create_semester_subject_teacher_assignment.sql`

Các bảng chính:

- `semester`: FK `academic_year_id`, code, dates, status, lock metadata và audit timestamps;
- `teacher`: FK nullable unique tới `app_user`, `teacher_code` unique, name, date of birth,
  gender, phone, email, join date, `ACTIVE|ON_LEAVE|INACTIVE` status và timestamps;
- `subject`: code/name/type/application_scope/status/timestamps;
- `subject_applicability`: FK `subject_id`, `semester_id`, `scope_type`, nullable FK
  `grade_level_id`, nullable FK `class_id`, status/timestamps; CHECK chỉ cho phép đúng một
  target FK theo scope type;
- `class_subject`: FK `class_id`, `subject_id`, `semester_id`, status/timestamps, unique
  `(class_id, subject_id, semester_id)`;
- `homeroom_assignment`: FK class/teacher/assigned_by, valid dates, status/timestamps;
- `subject_teaching_assignment`: FK class_subject/teacher/assigned_by, valid dates,
  status/timestamps.

Constraints/index bắt buộc:

- FK phải dùng `BIGINT` nhất quán với V4 hiện tại và không cascade dữ liệu lịch sử.
- Unique `semester(academic_year_id, code)`, `teacher(teacher_code)`, nullable unique
  `teacher(user_id)`, `subject(code)`, `class_subject(class_id, subject_id, semester_id)`.
- Unique applicability theo target: `(subject_id, semester_id, scope_type, grade_level_id,
  class_id)`; migration phải xử lý NULL/unique theo MySQL và H2 compatibility.
- Index `homeroom_assignment(class_id, status)`, `homeroom_assignment(teacher_id, status)`,
  `subject_teaching_assignment(class_subject_id, status)`,
  `subject_teaching_assignment(teacher_id, status)`,
  `subject_applicability(subject_id, semester_id, scope_type, status)`,
  `subject_applicability(grade_level_id, semester_id, status)`,
  `subject_applicability(class_id, semester_id, status)`.
- Check status/date ở database trong giới hạn MySQL/H2 compatibility; quy tắc active duy nhất
  và không chồng lấn vẫn do service transaction + lock parent bảo đảm.
- Không dùng unique `(class_subject_id, status)` vì sẽ làm mất khả năng lưu lịch sử nhiều
  assignment `ENDED`.

Phương án G2 hiện tại đề xuất migration thêm `subject_applicability` thay vì một bảng
riêng chỉ cho grade. Bảng này cho phép cùng một validator xử lý cả:

```text
ACADEMIC hiện tại: subject + grade + semester
SKILL hiện tại:    subject + class + semester
```

và vẫn mở rộng được scope mới bằng dữ liệu/configuration, không phải thêm nhánh theo từng
loại môn trong service. Phương án này vẫn cần user xác nhận trước khi implementation.

## 9. Decision gates cần user xác nhận

| Gate | Nội dung chưa nhất quán | Đề xuất để review |
|---|---|---|
| G1 | `SubjectType` trong module là `ACADEMIC/SKILL`, còn data model là `NORMAL/SKILL`. | **Đã chốt theo user:** dùng canonical value `ACADEMIC/SKILL`; migration, entity và DTO phải dùng đúng hai giá trị này. |
| G2 | FR-SUBJECT-003 yêu cầu hai kiểu target: môn học theo khối/học kỳ và môn học theo lớp/học kỳ. | **Đã chốt theo user ngày 2026-08-22:** tách `subject_type` khỏi `application_scope`, dùng `subject_applicability` với `scope_type=GRADE/CLASS`; validator đọc scope từ subject/data, không hardcode `ACADEMIC` hay `SKILL`. |
| G3 | Semester schema ghi `OPEN/LOCKED`, module requirement ghi `DRAFT/ACTIVE/LOCKED/CLOSED`. | **Đã chốt theo user ngày 2026-08-22:** dùng lifecycle `DRAFT -> ACTIVE -> LOCKED -> CLOSED`; mở rộng `BR-SEM-006` được ghi trong `CR-SEM-001`, còn email/config email để Implementation Note 27.1. |
| G4 | Teacher requirement cũ có “danh sách môn chuyên môn”. | **Đã chốt theo user:** bỏ danh sách môn chuyên môn và không tạo `teacher_subject_specialty`; giữ teacher identity/contact fields và status `ACTIVE|ON_LEAVE|INACTIVE`; quyền giảng dạy chỉ từ assignment. |
| G5 | `FR-TEACHER-005` cho phép ngừng công tác nhưng assignment đang active sẽ xử lý thế nào. | Khuyến nghị không tự ENDED assignment trong cùng request; từ chối assignment mới và yêu cầu workflow replace/end explicit, giữ lịch sử. |
| G6 | Baseline nói “kết thúc hoặc hủy assignment” nhưng schema chỉ có `ACTIVE/ENDED`. | Khuyến nghị dùng `ENDED` cho kết thúc/hủy và bắt buộc `validTo`; thêm `reason` chỉ khi user yêu cầu contract riêng. |

G1-G4 đã được user chốt. G5-G6 vẫn là các decision kỹ thuật cần xác nhận trước khi hoàn thiện
assignment replacement/cancel contract; không tạo migration hoặc API contract chính thức cho
phần bị ảnh hưởng nếu hai gate này thay đổi.

## 10. Phạm vi mã nguồn dự kiến sau approval

### Tạo mới

- `academic/domain/entity/Semester.java`, `SemesterStatus.java`;
- `academic/domain/entity/Subject.java`, `SubjectType.java`, `SubjectStatus.java`;
- `academic/domain/entity/ClassSubject.java`, `ClassSubjectStatus.java`;
- `academic/repository/SemesterRepository.java`, `SubjectRepository.java`,
  `ClassSubjectRepository.java`;
- `academic/service/SemesterService.java`, `SubjectService.java`, `ClassSubjectService.java`
  và validator/lookup tương ứng;
- DTO/controller cho semester, subject và class-subject;
- `teacher/**` entity, repository, DTO, service, controller và test;
- `assignment/**` entity, repository, DTO, service, access service, audit service,
  controller và test;
- migration V5 và test schema/migration;
- `document/dev-note/be/academic/027-academic-subject-teacher-assignment-2026-08-21.md`
  sau implementation.
- `document/application-doc/v2/change-request/CR-SEM-001-incomplete-score-data-notifications.md`
  là CR tài liệu; không triển khai email hoặc email configuration trong Plan 027.

### Chỉnh sửa có điều kiện

- `SchoolClass` chỉ chỉnh khi cần thêm query/lookup; không thêm field GVCN.
- `EnrollmentLookupService` chỉ chỉnh nếu cần query môn theo enrollment của học sinh.
- security chỉ chỉnh khi cần map teacher account hiện hành; không đổi JWT contract.
- application data-model docs chỉ cập nhật sau khi các gate/schema decision được user chốt.
- CR-SEM-001 chỉ được code qua Implementation Note 27.1 sau khi CR và các open decisions
  của CR được phê duyệt riêng.

## 11. Unit test và validation plan

### 11.1. Unit test service

- Semester: tạo thành công; ngày ngoài academic year; ngày đảo; code trùng; học kỳ chồng lấn;
  khóa/mở lại sai trạng thái; null/empty input và id không tồn tại.
- Subject: tạo/cập nhật thành công; code/name/type invalid; duplicate; inactive không được
  dùng cho class-subject; SKILL cấu hình quá một học kỳ bị từ chối.
- ClassSubject: class/semester khác academic year; subject không áp dụng grade; duplicate
  tuple; status không cho phép; not-found và input id `<= 0`.
- Teacher: teacher code trùng; user không tồn tại; user đã liên kết teacher khác; inactive
  không nhận assignment; update không làm mất history.
- Homeroom assignment: tạo thành công; cùng teacher vừa GVCN vừa GVBM; active duplicate;
  overlap; replace kết thúc cũ/tạo mới; end invalid; class/teacher not-found.
- Subject teaching assignment: tạo thành công; active duplicate trên cùng class-subject;
  teacher khác lớp/môn vẫn được assignment; replace atomic; ended không cấp access;
  GVCN không được suy ra access; `hasActiveAssignment` đúng/sai theo ngày hiệu lực.

Dependency cần mock: repository lookup, parent-row lock/query, `AuditLogRepository`,
`AuditContext`, clock/time provider nếu có và `ObjectMapper` cho before/after JSON.
Assertion phải kiểm tra exception/status, repository interaction, entity state, audit payload,
assignment cũ/mới và không có side effect khi validation fail.

### 11.2. Integration/migration test

- Flyway clean schema tạo đủ V5 tables, FK, indexes, unique constraints và check constraints.
- JPA context khởi động được với `ddl-auto=validate` trên schema tương ứng.
- MockMvc kiểm tra `401/403` và role mutation/read; teacher chỉ xem assignment của mình.
- Transaction replace rollback khi persist assignment mới hoặc audit thất bại.
- Concurrent/duplicate active assignment trả `409` và không tạo hai active record.
- Regression giữ toàn bộ test Plan 026 và API legacy hiện có.

### 11.3. Lệnh validation dự kiến

Chạy trong `BE/BaiTap-RS` sau implementation:

```text
./gradlew test
./gradlew checkstyleMain checkstyleTest
./gradlew pmdMain pmdTest
./gradlew build
./gradlew jacocoTestReport
```

Đọc XML/HTML report của Checkstyle, PMD và JaCoCo; không đặt coverage threshold mới nếu
project chưa chốt. Coverage phải được dùng để xác định nhánh business rule còn thiếu, không
được dùng làm lý do bỏ qua migration/authorization/integration test.

## 12. Rủi ro và giảm thiểu

- **Schema conflict:** G1-G4 chưa chốt có thể làm migration/API sai. Giảm thiểu bằng approval
  gate trước coding và không hard-code enum chưa được duyệt.
- **Race condition assignment:** application-level check đơn thuần không đủ. Giảm thiểu bằng
  lock parent row, transaction và test duplicate/concurrency; index chỉ hỗ trợ truy vấn.
- **Quyền giáo viên sai:** role `TEACHER` hoặc GVCN bị dùng như quyền nhập điểm. Giảm thiểu
  bằng access service chỉ nhận `subject_teaching_assignment`.
- **Mất lịch sử:** update/delete trực tiếp assignment cũ. Giảm thiểu bằng `ENDED`, valid dates,
  guarded delete và audit before/after.
- **Compatibility H2/MySQL:** check constraint và nullable unique có khác biệt. Giảm thiểu
  bằng Flyway schema test trên H2 mode MySQL và preflight MySQL khi môi trường cho phép.
- **Scope creep sang score:** không tạo score endpoint hoặc tính điểm trong plan này; chỉ tạo
  access contract và test regression boundary.

## 13. Output dự kiến

Sau khi plan được phê duyệt và triển khai, repository sẽ có:

- catalog học kỳ/môn/lớp-môn và teacher profile theo API v2;
- assignment GVCN/GVBM có lịch sử, transaction replacement, audit và authorization;
- không có field GVCN trong `school_class`;
- không có quyền nhập điểm dựa trên role/GVCN; score module tương lai có service contract để
  kiểm tra `subject_teaching_assignment`;
- migration/test/validation evidence và Dev Note ghi đúng trạng thái thực tế.

Plan 027 chưa được xem là approved cho đến khi user xác nhận rõ việc triển khai và các decision
gate cần thiết.
