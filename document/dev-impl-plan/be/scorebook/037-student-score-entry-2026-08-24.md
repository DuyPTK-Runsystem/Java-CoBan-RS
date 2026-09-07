# Developer Plan 037: Student Score Entry

## 1. Trạng thái và phiên bản áp dụng

- Status: `Approved`.
- Application-document version: `v2`.
- Ngày lập plan: `2026-08-24`.
- Phê duyệt: user approved qua agent ngày `2026-08-24`.
- Module: Backend `scorebook` — student score entry.
- Phụ thuộc: Plan `036` (scorebook foundation), Plan `026` (enrollment), Plan `027`
  (class-subject, semester và teaching assignment).

## 2. Mục tiêu

Xây dựng luồng nhập điểm nguồn cho một `scorebook`, gồm nhập từng học sinh, nhập hàng loạt
và đọc score grid để hiển thị rõ ô chưa nhập.

Kết quả mong muốn:

- Lưu được `SCORED`, `ABSENT`, `EXEMPTED`, `CANCELLED` theo đúng rule thang điểm 10.
- Phân biệt ô chưa nhập với điểm `0.0`.
- Teacher chỉ nhập điểm cho lớp/môn/học kỳ có assignment hợp lệ.
- Ghi audit, optimistic-lock version và tạo calculation task trong cùng transaction.
- HTTP request chỉ lưu source data và task, không tính điểm trung bình hoặc chờ worker.

## 3. Requirement liên quan

### Functional requirements

- `FR-SCORE-003`: Nhập điểm từng học sinh.
- `FR-SCORE-004`: Nhập điểm hàng loạt.
- `FR-SCORE-005`: Xem ô điểm chưa nhập.
- `FR-SCORE-007`: Audit log điểm.
- `FR-SCORE-008`: Xem trạng thái tính bảng điểm tổng kết ở mức state hiện có.

### Business rules và NFR

- `BR-SCORE-001` đến `BR-SCORE-005`: thang điểm 10, range `0.0..10.0`, `HALF_UP`,
  điểm 0 hợp lệ và không dùng 0 cho chưa nhập/vắng/miễn.
- `BR-SCORE-006`: chỉ `SCORED` có value mới tham gia calculation.
- `BR-SCORE-003`: kết quả tính toán không nằm trong HTTP request.
- `BR-ASSIGN-003` và `BR-AUTH-003`: quyền nhập điểm dựa trên subject teaching assignment.
- `NFR-CALC-004` đến `NFR-CALC-008`: source version, transcript `IN_PROGRESS`, task
  bền vững và transaction nhất quán.
- `NFR-AUDITABILITY-003`: nhập/sửa/hủy điểm phải có audit.

## 4. Phạm vi

### In-scope

- Entity, enum, repository, DTO, service và controller cho `student_score`.
- Single score upsert và bulk score upsert.
- Read score grid có phân trang server-side, mặc định `size=10`.
- Validate scorebook, assessment column, semester, enrollment, student và assignment.
- Optimistic locking bằng `version`.
- Tối thiểu `student_annual_transcript`, `student_term_transcript` và
  `calculation_task` để ghi nhận source version/state; chưa có worker.
- Audit từng score mutation bằng `AuditLog`/`AuditContext`.
- Unit test, integration test, migration constraint test và backend validation.

### Out-of-scope

- Calculation worker và công thức `Đtbmh`, `Đtbhk`, `ĐtbmhCN`, `Đtbcn`.
- `score_change_request`, workflow duyệt sửa điểm, retake và transcript result detail.
- Frontend Vue/PrimeVue, Storybook và Postman.
- API legacy `/api/v1/**` và refactor ngoài module scorebook.

## 5. Kiến trúc và flow

```text
ScoreEntryController
        |
        v
ScoreEntryService
        +--> ScorebookGuard / SubjectTeachingAssignmentAccessService
        +--> ScoreEntryContext
        +--> StudentScoreRepository
        +--> TranscriptStateService
        +--> CalculationTaskService
        +--> ScorebookAuditService
```

- Tái sử dụng `ScorebookGuard` để office/admin bypass và teacher kiểm tra assignment.
- Student phải `ACTIVE`, enrollment phải `ACTIVE` và `current_class_id` phải trùng lớp
  của `class_subject`.
- Column phải `ACTIVE`; scorebook phải `OPEN` hoặc `PUBLISHED`; semester không được
  `LOCKED`/`CLOSED`.
- Tạo mới không yêu cầu `expectedVersion`; cập nhật phải có version khớp hiện tại.
- Update trực tiếp chỉ hợp lệ trong 10 ngày từ `enteredAt` và khi semester chưa khóa.
  Ngoài điều kiện này trả `409`, không tự tạo score-change request trong Plan 037.
- Bulk là all-or-nothing; duplicate student trong cùng request bị từ chối.
- Request không làm thay đổi dữ liệu trả dữ liệu hiện tại, không tăng version, không audit
  và không tạo calculation task mới.

## 6. API contract

| Method | Path | Quyền | Mục đích |
|---|---|---|---|
| `GET` | `/api/v2/scorebooks/{scorebookId}/score-entries?page=0&size=10` | office hoặc GVBM đúng assignment | Đọc columns active, roster và score grid |
| `PUT` | `/api/v2/assessment-columns/{columnId}/students/{studentId}/score` | office hoặc GVBM đúng assignment | Upsert một score |
| `POST` | `/api/v2/assessment-columns/{columnId}/scores/bulk` | office hoặc GVBM đúng assignment | Upsert nhiều score trong một transaction |

### Request/response chính

- `ReqUpsertStudentScoreDTO`: `scoreStatus`, `scoreValue`, `note`, `expectedVersion`.
- `ReqBulkUpsertStudentScoreDTO`: danh sách item gồm `studentId`, `scoreStatus`,
  `scoreValue`, `note`, `expectedVersion`; danh sách không được rỗng hoặc trùng student.
- `ResStudentScoreDTO`: `scoreId`, `assessmentColumnId`, `studentId`, status/value/note,
  entered/updated metadata và `version`.
- `ResStudentScoreGridDTO`: scorebook metadata, active columns, phân trang roster và
  score map theo student/column; ô chưa nhập có score `null`, điểm 0 giữ nguyên `0.0`.

### HTTP/error behavior

- `400`: request shape, status/value không hợp lệ, range/scale sai hoặc bulk duplicate.
- `401`: chưa xác thực.
- `403`: không có role hoặc không có assignment tương ứng.
- `404`: scorebook, column hoặc student không tồn tại.
- `409`: lifecycle/semester/enrollment không phù hợp, version conflict, quá hạn sửa,
  column inactive hoặc constraint conflict.

## 7. Database và transaction

### Migration

- `V11__create_student_score.sql`:
  - `student_score(score_id, assessment_column_id, student_id, score_status,
    score_value, note, entered_by, entered_at, updated_by, updated_at, version)`;
  - unique `(assessment_column_id, student_id)`;
  - CHECK `SCORED` phải có value `0..10`, status khác `SCORED` phải có value `NULL`;
  - FK không cascade delete dữ liệu điểm.
- `V12__create_transcript_calculation_state.sql`:
  - annual/term transcript state với `IN_PROGRESS|FINISH`, `source_version`,
    `calculated_version` và unique key phù hợp;
  - `calculation_task` với `STUDENT_YEAR_RECALC`, `PENDING|RUNNING|SUCCEEDED|FAILED`,
    retry metadata và unique `idempotency_key`.

### Transaction khi ghi điểm

1. Validate authorization và dữ liệu.
2. Insert/update `student_score` với optimistic locking.
3. Tạo hoặc khóa transcript state tương ứng; tăng `source_version`.
4. Đặt transcript thành `IN_PROGRESS`.
5. Tạo hoặc gộp task theo khóa ổn định `{studentId, academicYearId}` và version mới nhất.
6. Ghi audit cho từng row thay đổi.
7. Commit rồi trả response; không chạy calculation.

## 8. Phạm vi mã nguồn dự kiến

### Tạo mới

- `BE/BaiTap-RS/src/main/resources/db/migration/V11__create_student_score.sql`.
- `BE/BaiTap-RS/src/main/resources/db/migration/V12__create_transcript_calculation_state.sql`.
- `.../scorebook/domain/entity/StudentScore.java`, `ScoreStatus.java`, transcript state
  entities và calculation task entities/enums.
- `.../scorebook/domain/DTOs/requests/*StudentScore*` và response DTO tương ứng.
- `.../scorebook/repository/StudentScoreRepository.java`, transcript repositories và
  `CalculationTaskRepository.java`.
- `.../scorebook/controller/ScoreEntryController.java`.
- `.../scorebook/service/ScoreEntryService.java`, context/validation, transcript-state,
  calculation-task và audit boundary.
- Unit/integration/migration tests trong package `scorebook`.

### Chỉnh sửa tối thiểu

- `ScorebookContext` hoặc repository liên quan nếu cần truy vấn column/class-subject.
- `StudentYearEnrollmentRepository` nếu cần query roster/active enrollment tối ưu.
- Không thay đổi public contract của academic, enrollment hoặc assignment nếu không bắt buộc.

## 9. Test và validation

### Unit test

- Create/update single score với `0.0`, biên `0.0/10.0`, sai scale và sai status/value.
- `ABSENT`, `EXEMPTED`, `CANCELLED` không có score value.
- Student không thuộc lớp, enrollment inactive, column inactive, scorebook/semester sai state.
- Teacher đúng assignment được phép; teacher sai assignment, student và anonymous bị từ chối.
- Version thiếu/sai, update quá 10 ngày và update khi semester locked.
- Bulk thành công, duplicate student, item lỗi và bảo đảm all-or-nothing.
- Grid trả đúng roster, active columns, null cho chưa nhập và 0 cho điểm 0.
- Transcript version tăng, state thành `IN_PROGRESS`, task được tạo/gộp và audit interaction.

### Integration/constraint test

- API `401`, `403`, office bypass và teacher assignment.
- V11/V12 kiểm tra FK, unique, CHECK và optimistic-lock behavior.
- Sau HTTP write có source score, transcript state và calculation task; không có calculation
  đồng bộ trong request.
- Regression toàn bộ test academic, enrollment, assignment và scorebook hiện có.

### Backend validation

Sau implementation gọi skill `backend-validation` theo workflow project: test, Checkstyle,
PMD và build. Ghi `Validation Result` thực tế, sau đó tạo Dev Note #37 theo skill `dev-note`.

## 10. Rủi ro và quyết định

- Dùng V11/V12 vì repository thực tế đã dùng V6–V10 cho attendance, calendar, user linkage
  và scorebook foundation.
- Không tạo result-detail tables hoặc worker trong plan này; calculation plan sau sẽ mở rộng
  state tables và sử dụng task đã tạo.
- Không dùng điểm 0 để biểu diễn chưa nhập; row không tồn tại hoặc score value `NULL` là
  dữ liệu chưa nhập.
- Không xóa vật lý score row; hủy điểm dùng `CANCELLED` và giữ audit/history.
- Plan đã được người dùng phê duyệt; implementation chỉ được thực hiện trong đúng scope
  và các decision của tài liệu này.

## 11. Output dự kiến

- Backend có API nhập từng điểm, nhập hàng loạt và đọc score grid.
- Score source được lưu đúng constraint, có audit và optimistic locking.
- Mỗi thay đổi điểm tạo state `IN_PROGRESS` và calculation task bền vững trong cùng transaction.
- Không có phép tính điểm trung bình trong HTTP request và không ảnh hưởng API legacy.

## 12. Implementation handoff tối giản

Session implementation đọc Plan 037 này và Plan 036 trước khi code; áp dụng application
docs `v2`. Implement backend scorebook student-score entry với single/bulk/grid, migration
V11/V12, assignment authorization, optimistic locking, audit và calculation task state;
không triển khai worker, score-change request, retake, frontend hoặc API legacy. Sau code
phải chạy unit/backend validation, cập nhật Dev Note #37 và báo cáo `Validation Result`.
