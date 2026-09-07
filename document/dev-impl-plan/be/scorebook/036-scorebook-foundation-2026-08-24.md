# Developer Plan 036: Scorebook Foundation

## 1. Trạng thái và phiên bản áp dụng

- Status: `Approved`.
- Application-document version: `v2`.
- Ngày lập plan: `2026-08-24`.
- Module: Backend `scorebook`.
- Phụ thuộc: Plan `025` (contract/migration/scope freeze), Plan `026` (academic year,
  class và enrollment), Plan `027` (semester, subject, class-subject, teacher và
  `SubjectTeachingAssignmentAccessService`).
- Phê duyệt: user phê duyệt triển khai qua tin nhắn agent ngày `2026-08-24`.

## 2. Mục tiêu

Xây dựng foundation cho sổ điểm theo một bộ `{class_subject}` trong học kỳ, gồm lifecycle
của sổ điểm, cấu hình các cột đánh giá và cấu hình trọng số môn kỹ năng.

Kết quả mong muốn:

- Mỗi `class_subject` có tối đa một `scorebook`.
- Người có quyền có thể tạo/lấy sổ điểm, mở sổ, cấu hình cột và công bố sổ theo trạng thái
  hợp lệ.
- Cột `KTTT`, `KTĐK`, `KTCK` được kiểm tra theo loại môn và không bị trùng số thứ tự
  trong cùng sổ điểm.
- Môn kỹ năng có thể lưu một bộ trọng số hợp lệ, với ràng buộc tổng 100 và `KTCK` không
  nhỏ hơn hai trọng số còn lại.
- Quyền thao tác của GVBM dựa trên assignment thực tế; role `TEACHER` đơn lẻ không đủ
  để sửa sổ điểm của môn/lớp khác.
- Foundation này cung cấp parent/configuration contract cho các plan nhập điểm và
  calculation tiếp theo, không thực hiện phép tính trong HTTP request.

## 3. Requirement liên quan

### 3.1. Functional requirements

- `FR-SCORE-001`: GVBM mở sổ điểm của môn/lớp/học kỳ được phân công.
- `FR-SCORE-002`: Tạo cột điểm phù hợp với loại môn.
- `FR-SCORE-006`: Công bố điểm — plan này chỉ chuẩn bị lifecycle publish của scorebook;
  dữ liệu điểm học sinh và màn hình công bố chi tiết thuộc plan sau.
- `FR-SKILL-001`: Người có quyền cấu hình ba cột điểm môn kỹ năng.
- `FR-SKILL-002`: Người có quyền cấu hình trọng số KTTT, KTĐK và KTCK.
- `FR-SKILL-003`: Kiểm tra tổng trọng số và ràng buộc trọng số KTCK.

### 3.2. Business rules và NFR

- `BR-SCORE-007`: `KTTT` có thể có từ 0 cột trở lên.
- `BR-SCORE-008`: `KTĐK` phải có tối thiểu một cột trước khi publish/đóng sổ.
- `BR-SCORE-009`: `KTCK` phải có đúng một cột trước khi publish/đóng sổ.
- `BR-SKILL-001`–`BR-SKILL-003`: Môn kỹ năng có đúng một cột cho mỗi loại đánh giá.
- `BR-SKILL-004`: Tổng ba trọng số bằng 100 và trọng số `KTCK` lớn hơn hoặc bằng
  `KTTT`, `KTĐK`.
- `BR-SCORE-010`: Số lượng cột là cấu hình của sổ điểm; học sinh không bắt buộc có dữ
  liệu ở mọi cột.
- `NFR-SECURITY-003`: Quyền giáo viên dựa trên phân công thực tế.
- `NFR-AUDITABILITY-003`: Cấu hình/công bố dữ liệu điểm phải có audit log; foundation
  audit các mutation của scorebook, column và skill weight.
- `NFR-CALC-004`: Không tính điểm trung bình trong HTTP request.

## 4. Phạm vi

### 4.1. In-scope

- Tạo module backend `scorebook` theo cấu trúc feature-first.
- JPA entity và enum cho:
  - `Scorebook`: `DRAFT | OPEN | PUBLISHED | CLOSED`;
  - `AssessmentColumn`: `KTTT | KTĐK | KTCK`, `ACTIVE | INACTIVE`;
  - `SkillWeightConfig`.
- Flyway migration mới theo version thực tế của repository: `V10__create_scorebook_and_assessment.sql`.
  Migration tạo bảng `scorebook`, `assessment_column`, `skill_weight_config`, FK tới
  `class_subject`/`app_user`, unique key và CHECK constraint cần thiết.
- API v2 cho tạo/lấy sổ điểm, mở sổ, cấu hình cột, vô hiệu hóa cột, lưu/cập nhật trọng số
  môn kỹ năng và publish sổ sau khi cấu hình hợp lệ.
- Validate parent `class_subject` tồn tại, `ACTIVE`, thuộc semester còn cho phép cấu hình;
  subject type và cấu hình cột/weight phải nhất quán.
- Authorization:
  - `ADMIN`, `ACADEMIC_OFFICE` có quyền kiểm soát cấu hình/publish toàn hệ thống;
  - `TEACHER` chỉ được thao tác scorebook của `class_subject` có
    `subject_teaching_assignment` `ACTIVE` tại ngày nghiệp vụ;
  - `STUDENT` chỉ đọc metadata được expose ở plan sau, không mutation.
- Audit mutation bằng `AuditLog`/`AuditContext`, tối thiểu cho create/open, column
  create/update/deactivate, weight upsert và publish.
- Unit test service/validator/repository rule và integration test API, authorization,
  migration constraint.

### 4.2. Out-of-scope

- `student_score`, nhập điểm từng học sinh hoặc nhập điểm hàng loạt.
- Tính `Đtbmh`, điểm kỹ năng, `Đtbhk`, `ĐtbmhCN`, `Đtbcn` hoặc bất kỳ calculation worker/task nào.
- `score_change_request`, workflow duyệt sửa điểm, retake và transcript.
- Báo cáo completeness/notification của `CR-SEM-001`.
- Frontend Vue/PrimeVue, Storybook và Postman collection.
- Thay đổi API legacy `/api/v1/**` hoặc thay đổi hành vi các module academic,
  assignment, attendance hiện tại.
- Cho phép xóa cứng scorebook/column/weight đã phát sinh lịch sử; cột chỉ chuyển
  `INACTIVE`.
- Tự suy diễn công thức mới, thay đổi hệ số chuẩn `KTTT=1`, `KTĐK=2`, `KTCK=3`, hoặc
  quyết định thêm loại đánh giá chưa có trong v2.

## 5. Kiến trúc và flow hiện tại

Backend là Spring Boot modular monolith dùng Spring Data JPA, Flyway, JWT,
`@PreAuthorize`, scalar `Long` cho FK, DTO riêng và service transaction boundary.

Các dependency đã có:

```text
academic.class_subject + subject + semester
        |
        v
scorebook controller
        |
        v
ScorebookService / ScorebookGuard
        +--> ClassSubjectRepository, SubjectRepository, SemesterRepository
        +--> SubjectTeachingAssignmentAccessService
        +--> Scorebook/AssessmentColumn/SkillWeightConfig repositories
        +--> AuditLogRepository + AuditContext
```

Không tạo quan hệ JPA hai chiều hoặc trả entity trực tiếp từ controller. Service chịu
trách nhiệm kiểm tra lifecycle và cấu hình; repository chỉ giữ truy vấn persistence.

## 6. Phương án triển khai

### 6.1. Domain model

Các entity dùng `Long` identity, `LocalDateTime` cho audit timestamps, enum
`@Enumerated(EnumType.STRING)` và `BigDecimal` cho `weight_factor`/trọng số.

- `Scorebook`: `id`, `classSubjectId`, `status`, `publishedAt`, `publishedBy`,
  `closedAt`, `createdAt`, `updatedAt`.
- `AssessmentColumn`: `id`, `scorebookId`, `assessmentType`, `columnNo`, `columnName`,
  `weightFactor`, `isRequired`, `status`, timestamps.
- `SkillWeightConfig`: `id`, `scorebookId`, ba phần trăm trọng số, `configuredBy`,
  `configuredAt`, `lockedBy`, `lockedAt`.

`weightFactor` của cột môn thông thường được khởi tạo/kiểm tra theo loại (`1`, `2`, `3`)
và không được dùng để tính điểm trong plan này. Với môn kỹ năng, cấu hình ba cột và
`SkillWeightConfig` phải cùng tồn tại trước publish.

### 6.2. Lifecycle và validation

- Tạo scorebook chỉ khi `class_subject` tồn tại, `ACTIVE`, và chưa có scorebook; trạng thái
  ban đầu `DRAFT`.
- `DRAFT -> OPEN` khi mở sổ; chỉ `OPEN` mới cho phép thêm/sửa/deactivate column và upsert
  skill weight.
- `OPEN -> PUBLISHED` chỉ khi cấu hình hợp lệ:
  - môn thường: `KTĐK >= 1`, `KTCK = 1` active;
  - môn kỹ năng: đúng một `KTTT`, `KTĐK`, `KTCK` active và weight config hợp lệ;
  - mọi cột active có `columnNo` dương, unique theo `(scorebook, assessmentType, columnNo)`.
- `PUBLISHED` chỉ đọc cấu hình trong plan này; mọi thay đổi sau publish cần plan sửa điểm/
  mở khóa được phê duyệt.
- `CLOSED` được mapping trong entity/schema nhưng transition do semester-lock/score-change
  plan sở hữu; không expose endpoint đóng sổ trong Plan 036.
- `column` đã được dùng hoặc đã audit không xóa vật lý; deactivate chỉ được phép trước
  publish trong Plan 036.
- `SKILL` chỉ được cấu hình ở một học kỳ theo rule của Plan 027; service kiểm tra không
  tạo thêm scorebook trái với catalog hiện tại.

### 6.3. API contract dự kiến

Contract dùng response wrapper/`@ApiMessage` hiện có. DTO cụ thể cần chốt khi implementation
đối chiếu convention, không expose entity/audit internals.

| Method | Path | Quyền | Mục đích |
|---|---|---|---|
| `POST` | `/api/v2/scorebooks` | office | Tạo scorebook cho `classSubjectId` |
| `GET` | `/api/v2/scorebooks/{scorebookId}` | authenticated theo scope | Lấy scorebook, columns và weight config |
| `POST` | `/api/v2/scorebooks/{scorebookId}/open` | office hoặc GVBM được phân công | Mở sổ |
| `POST` | `/api/v2/scorebooks/{scorebookId}/columns` | office hoặc GVBM được phân công | Thêm cột |
| `PUT` | `/api/v2/assessment-columns/{columnId}` | office hoặc GVBM được phân công | Sửa metadata cột |
| `DELETE` | `/api/v2/assessment-columns/{columnId}` | office hoặc GVBM được phân công | Deactivate cột, không xóa vật lý |
| `PUT` | `/api/v2/scorebooks/{scorebookId}/skill-weight` | office hoặc GVBM được phân công | Upsert trọng số môn kỹ năng |
| `POST` | `/api/v2/scorebooks/{scorebookId}/publish` | office hoặc GVBM được phân công | Validate và publish sổ |

Decision gate khi implementation: nếu codebase yêu cầu PATCH thay DELETE cho deactivate
hoặc endpoint nested khác, phải cập nhật plan trước khi code.

### 6.4. Authorization và audit

- `ScorebookGuard` lấy `classSubjectId` từ scorebook, sau đó gọi
  `SubjectTeachingAssignmentAccessService.assertActiveAssignment(...)` cho teacher.
- `ACADEMIC_OFFICE` là quyền kiểm soát cấu hình; `ADMIN` giữ override theo convention
  hiện tại. Không cấp quyền dựa trên homeroom assignment hoặc teacher role đơn thuần.
- Audit action dự kiến: `SCOREBOOK_CREATED`, `SCOREBOOK_OPENED`,
  `ASSESSMENT_COLUMN_CREATED`, `ASSESSMENT_COLUMN_UPDATED`, `ASSESSMENT_COLUMN_DEACTIVATED`,
  `SKILL_WEIGHT_CONFIGURED`, `SCOREBOOK_PUBLISHED`.
- `before_data`/`after_data` chỉ chứa field nghiệp vụ cần thiết; không ghi password,
  token hoặc toàn bộ entity không liên quan.

## 7. Phạm vi mã nguồn dự kiến

### 7.1. Tạo mới

- `BE/BaiTap-RS/src/main/resources/db/migration/V10__create_scorebook_and_assessment.sql`:
  schema/constraint/index cho ba bảng foundation.
- `.../scorebook/controller/ScorebookController.java`: REST endpoints và authorization.
- `.../scorebook/domain/entity/Scorebook.java`, `ScorebookStatus.java`.
- `.../scorebook/domain/entity/AssessmentColumn.java`, `AssessmentType.java`,
  `AssessmentColumnStatus.java`.
- `.../scorebook/domain/entity/SkillWeightConfig.java`.
- `.../scorebook/domain/DTOs/requests/*` và `.../response/*`: request/response typed DTO.
- `.../scorebook/repository/ScorebookRepository.java`,
  `AssessmentColumnRepository.java`, `SkillWeightConfigRepository.java`.
- `.../scorebook/service/ScorebookService.java`, `ScorebookGuard.java`,
  `ScorebookMapper.java`, `ScorebookAuditService.java`.
- Unit tests `.../scorebook/service/ScorebookServiceTest.java` và validator tests.
- Integration tests `.../scorebook/controller/ScorebookControllerIntegrationTest.java`
  và migration/constraint coverage nếu test harness hỗ trợ.

### 7.2. Chỉnh sửa tối thiểu

- `SubjectTeachingAssignmentAccessService` hoặc repository chỉ khi cần thêm query lấy
  assignment từ `scorebook -> class_subject`; giữ backward-compatible.
- `ClassSubjectRepository` chỉ khi cần query `findByIdAndStatus` hoặc lock parent để
  chống duplicate; không đổi contract public hiện tại nếu không cần.
- `document/application-doc/v2` chỉ cập nhật khi implementation phát hiện contract cần
  một Change Request; không sửa requirement trong plan này.

### 7.3. Không dự kiến chỉnh sửa

- `student`, `enrollment`, `attendance`, transcript và calculation worker.
- API legacy `/api/v1/**`.
- `AuditLog` schema dùng chung.

## 8. API / Database / Integration

### 8.1. Database

Migration `V10` tạo:

- `scorebook(scorebook_id, class_subject_id, status, published_at, published_by,
  closed_at, created_at, updated_at)` với unique `class_subject_id`.
- `assessment_column(assessment_column_id, scorebook_id, assessment_type, column_no,
  column_name, weight_factor, is_required, status, created_at, updated_at)` với unique
  `(scorebook_id, assessment_type, column_no)` và FK cascade policy được review theo
  lịch sử nghiệp vụ; không cascade delete score data vì score chưa nằm trong plan.
- `skill_weight_config(skill_weight_config_id, scorebook_id, ...weights, configured_by,
  configured_at, locked_by, locked_at)` với unique `scorebook_id` và CHECK weight.

CHECK constraints phải phản ánh enum hiện tại; service vẫn validate vì MySQL/version
khác nhau có thể xử lý CHECK khác nhau. Index phục vụ `scorebook(class_subject_id)` và
`assessment_column(scorebook_id, assessment_type, status)`.

### 8.2. Compatibility

- Migration chạy sau V9, không sử dụng version `V6` trong data-model v2 vì repository đã
  dùng V6-V9 cho attendance/calendar/user linkage.
- Không đụng schema legacy `average_score`; field deprecated không phải nguồn scorebook.
- API mới chỉ dưới `/api/v2/**`, không ảnh hưởng contract hiện tại.

## 9. Test và validation dự kiến

### 9.1. Unit test

- `ScorebookService.createScorebook(ReqCreateScorebookDTO)`: mock
  `ClassSubjectRepository`, `SubjectRepository`, `SemesterRepository`,
  `ScorebookRepository` và audit; assert default `DRAFT`, response mapping, duplicate
  conflict và parent not-found.
- `ScorebookService.openScorebook(Long)`: assert `DRAFT -> OPEN`, reject `OPEN`,
  `PUBLISHED` và `CLOSED`, đồng thời kiểm tra audit interaction.
- `ScorebookService.addColumn/updateColumn/deactivateColumn`: fixture scorebook,
  subject type và cột hiện tại; assert positive column number, duplicate key, lifecycle
  permission, chuẩn `weightFactor`, soft-deactivate và không gọi delete repository.
- `ScorebookService.upsertSkillWeight(...)`: assert chỉ `SKILL` được cấu hình, tổng
  `100.00`, boundary `KTCK == KTTT/KTĐK` hợp lệ, reject null/âm/quá 100/tổng sai.
- `ScorebookService.publishScorebook(Long)`: assert cấu hình môn thường và môn kỹ năng
  đạt điều kiện; reject thiếu `KTĐK`, thừa `KTCK`, thiếu cột skill hoặc thiếu weight;
  assert chuyển `PUBLISHED`, timestamps/user và audit.
- `ScorebookGuard`: mock `SubjectTeachingAssignmentAccessService` và
  `TeacherRepository`/`ScorebookRepository`; assert office/admin bypass, teacher có
  assignment được phép, teacher không có assignment nhận `403`, principal không phải
  teacher nhận `403`.
- Dùng record DTO fixtures và entity constructors, không phụ thuộc database trong unit
  test; verify không có calculation/task/repository interaction ngoài scope.
- Regression: chạy lại toàn bộ test academic, assignment, attendance và migration hiện có;
  không thay đổi assertion hoặc fixture của module cũ.

- Tạo scorebook thành công, default `DRAFT`.
- Từ chối scorebook thứ hai cho cùng `class_subject`.
- Từ chối class-subject không tồn tại/inactive hoặc semester `LOCKED/CLOSED`.
- `DRAFT -> OPEN` thành công; không cho mutation column khi `DRAFT`/`PUBLISHED` theo
  lifecycle đã chốt.
- Môn thường: chấp nhận nhiều `KTTT`, yêu cầu tối thiểu một `KTĐK`, đúng một `KTCK` khi
  publish; reject thiếu/thừa cấu hình.
- Môn kỹ năng: đúng ba cột và weight hợp lệ; reject tổng khác 100, `KTCK` nhỏ hơn
  `KTTT`/`KTĐK`, thiếu một cột hoặc duplicate column.
- Deactivate column không xóa vật lý và không cho deactivate sau publish.
- Teacher không có assignment tương ứng nhận `403`; assignment đúng class-subject được phép.
- Audit được ghi cùng transaction cho mọi mutation.

### 9.2. Integration/constraint test

- Office/teacher hợp lệ nhận status `201/200` đúng contract.
- Anonymous nhận `401`; role `STUDENT` hoặc teacher không được phân công nhận `403`.
- Publish thất bại trả `409` với lỗi cấu hình cụ thể.
- Unique/FK/CHECK của V10 được database enforce.
- Response không lộ entity JPA, `publishedBy` nội bộ ngoài contract, token hoặc secret.
- Existing academic/assignment/attendance tests tiếp tục pass.

### 9.3. Backend validation

Sau khi implementation được phê duyệt, chạy skill `backend-validation` từ `BE/BaiTap-RS`
theo workflow hiện hành: test, Checkstyle, PMD và build; ghi `PASS`, `FAIL` hoặc `NOT RUN`
theo Validation Result. Plan 036 chưa chạy validation vì chưa có code implementation.

## 10. Rủi ro, assumption và decision gate

- **Lifecycle publish:** chốt trong implementation theo `DRAFT -> OPEN -> PUBLISHED`; việc
  `CLOSED` giao cho plan semester-lock/score-change. Nếu user muốn đóng sổ độc lập, cần
  approval delta.
- **Quyền office/teacher:** plan áp dụng ma trận v2 (`ACADEMIC_OFFICE` kiểm soát, GVBM
  theo assignment). Chưa mở quyền cho GVCN chỉ vì là homeroom.
- **`weight_factor`:** dùng hệ số chuẩn theo loại đánh giá; chưa cho công thức tùy biến.
- **Skill scope:** chỉ cho một scorebook skill hợp lệ theo catalog; nếu dữ liệu hiện tại
  cho phép skill ở nhiều kỳ, dừng và đề xuất quyết định nghiệp vụ.
- **Deactivate endpoint:** đang đề xuất `DELETE` mang nghĩa soft-deactivate; nếu convention
  hiện tại không phù hợp, cập nhật plan trước implementation.
- **Migration/schema freeze:** không thêm `student_score` hoặc calculation tables vào V10;
  chúng thuộc plan kế tiếp để tránh coupling và migration rollback khó.

## 11. Output dự kiến

- Backend module `scorebook` có schema/JPA/API foundation cho sổ điểm và cấu hình cột.
- Authorization dựa trên `subject_teaching_assignment` và office control.
- Audit đầy đủ cho lifecycle/configuration mutations.
- Contract ổn định để Plan tiếp theo triển khai `student_score`, score entry và background
  calculation mà không phải đổi parent model.
- Không có thay đổi ngoài scope đối với attendance, enrollment, academic hoặc legacy API.
