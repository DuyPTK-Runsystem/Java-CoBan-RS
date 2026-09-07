# Dev Note 037: Student Score Entry

## 1. Developer Plan và trạng thái

- Related Developer Plan: `document/dev-impl-plan/be/scorebook/037-student-score-entry-2026-08-24.md`.
- Plan status: `Approved` ngày `2026-08-24`.
- Implementation status: `Implemented; Full backend validation PASS`.
- Application-document version: `v2`.

## 2. Phạm vi đã thực hiện

- Triển khai toàn bộ tính năng nhập điểm học sinh theo Plan 037:
  - Migration V11: Tạo bảng `student_score` với các ràng buộc CHECK trạng thái (`SCORED`, `ABSENT`, `EXEMPTED`, `CANCELLED`), giá trị điểm (0.0 – 10.0 khi SCORED, NULL khi non-SCORED), unique `(assessment_column_id, student_id)`, FK và indexes.
  - Migration V12: Tạo các bảng `student_annual_transcript`, `student_term_transcript` và `calculation_task` phục vụ quản lý version và kích hoạt tính toán không đồng bộ.
  - Các Entity, Enum, Repository và DTO (request/response Java records) tương ứng.
  - API v2 Single Score Upsert: `PUT /api/v2/assessment-columns/{columnId}/students/{studentId}/score`.
  - API v2 Bulk Score Upsert: `POST /api/v2/assessment-columns/{columnId}/scores/bulk` (xử lý all-or-nothing, chặn trùng studentId).
  - API v2 Score Grid Read: `GET /api/v2/scorebooks/{scorebookId}/score-entries` (phân trang roster học sinh theo lớp, ánh xạ điểm theo từng cột điểm).
  - Ràng buộc nghiệp vụ: Quy tắc thang điểm 10, làm tròn 1 chữ số thập phân, điểm 0.0 hợp lệ khác NOT_ENTERED (null), kiểm tra điều kiện sửa trực tiếp trong vòng 10 ngày kể từ `enteredAt`, kiểm tra học kỳ chưa khóa (`LOCKED`/`CLOSED`), optimistic locking qua `expectedVersion`.
  - Toàn vẹn giao dịch (NFR-CALC-007): Trong cùng 1 DB transaction lưu `student_score`, tăng `source_version` trên transcript, chuyển trạng thái `IN_PROGRESS`, tạo/gộp `calculation_task` theo `idempotency_key`, ghi `audit_log`.
  - Không mở rộng scope ra worker tính toán nền, quy trình xin sửa điểm (score-change request), thi lại (retake), frontend hay API legacy.

## 3. Files đã thay đổi

### Database Migrations

- `BE/BaiTap-RS/src/main/resources/db/migration/V11__create_student_score.sql`: Tạo bảng `student_score`, ràng buộc và indexes.
- `BE/BaiTap-RS/src/main/resources/db/migration/V12__create_transcript_calculation_state.sql`: Tạo bảng `student_annual_transcript`, `student_term_transcript`, `calculation_task`.

### Domain Entities & Enums

- `.../scorebook/domain/entity/ScoreStatus.java`: Enum trạng thái điểm (`SCORED`, `ABSENT`, `EXEMPTED`, `CANCELLED`).
- `.../scorebook/domain/entity/CalculationStatus.java`: Enum trạng thái tính toán transcript (`IN_PROGRESS`, `FINISH`).
- `.../scorebook/domain/entity/CalculationTaskStatus.java`: Enum trạng thái task (`PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`).
- `.../scorebook/domain/entity/CalculationTaskType.java`: Enum loại task (`STUDENT_YEAR_RECALC`).
- `.../scorebook/domain/entity/StudentScore.java`: JPA Entity bảng `student_score` với `@Version` và các method nghiệp vụ.
- `.../scorebook/domain/entity/StudentAnnualTranscript.java`: JPA Entity bảng `student_annual_transcript`.
- `.../scorebook/domain/entity/StudentTermTranscript.java`: JPA Entity bảng `student_term_transcript`.
- `.../scorebook/domain/entity/CalculationTask.java`: JPA Entity bảng `calculation_task`.

### Repositories

- `.../scorebook/repository/StudentScoreRepository.java`: Truy vấn điểm theo column, student, bulk columns.
- `.../scorebook/repository/StudentAnnualTranscriptRepository.java`: Tìm transcript năm theo student/year kèm pessimistic lock.
- `.../scorebook/repository/StudentTermTranscriptRepository.java`: Tìm transcript kỳ theo annual_transcript/semester.
- `.../scorebook/repository/CalculationTaskRepository.java`: Tìm task theo idempotency key.

### DTOs

- `.../scorebook/domain/DTOs/requests/ReqUpsertStudentScoreDTO.java`: Request nhập/sửa điểm đơn lẻ.
- `.../scorebook/domain/DTOs/requests/ReqBulkScoreItemDTO.java`: Item trong danh sách nhập điểm hàng loạt.
- `.../scorebook/domain/DTOs/requests/ReqBulkUpsertStudentScoreDTO.java`: Request nhập điểm hàng loạt.
- `.../scorebook/domain/DTOs/response/ResStudentScoreDTO.java`: Response chi tiết điểm học sinh.
- `.../scorebook/domain/DTOs/response/ResScoreGridColumnDTO.java`: Thông tin cột trong bảng điểm.
- `.../scorebook/domain/DTOs/response/ResScoreGridStudentRowDTO.java`: Dòng học sinh trong bảng điểm.
- `.../scorebook/domain/DTOs/response/ResStudentScoreGridDTO.java`: Response phân trang toàn bộ bảng điểm.

### Services & Controller

- `.../scorebook/controller/ScoreEntryController.java`: Thin REST controller cho score grid, single score upsert, bulk score upsert với `@PreAuthorize` và `@ApiMessage`.
- `.../scorebook/service/ScoreEntryService.java`: Orchestrator điều phối nghiệp vụ nhập/sửa điểm.
- `.../scorebook/service/ScoreGridService.java`: Service truy vấn bảng điểm phân trang.
- `.../scorebook/service/ScoreGridLoader.java`: Helper tải và ánh xạ bảng điểm, tách rời để giảm coupling.
- `.../scorebook/service/ScoreEntryContext.java`: Tra cứu và validate context (scorebook, column, class_subject, semester, student, enrollment).
- `.../scorebook/service/ScoreEntryValidator.java`: Bộ quy tắc kiểm tra tính hợp lệ của điểm, phiên bản, hạn 10 ngày và danh sách không trùng lặp.
- `.../scorebook/service/ScoreEntryWriter.java`: Helper thực hiện lưu DB, ghi audit log và ánh xạ response.
- `.../scorebook/service/TranscriptStateService.java`: Quản lý `source_version` và trạng thái `IN_PROGRESS` của annual/term transcript.
- `.../scorebook/service/CalculationTaskService.java`: Tạo và gộp task tính toán theo `idempotency_key`.
- `.../scorebook/service/ScoreResponseMapper.java`: Mapper Entity `StudentScore` sang `ResStudentScoreDTO`.
- `.../scorebook/service/ScoreAuditDataMapper.java`: Snapshot before/after cho audit log.
- `.../scorebook/service/EnrollmentRosterService.java`: Tải danh sách học sinh theo lớp phục vụ score grid.
- `.../scorebook/service/EnrollmentRosterRepository.java`: Repository phân trang active roster.

### Tests

- `.../scorebook/service/ScoreEntryTestFixtures.java`: Fixtures dùng chung cho test.
- `.../scorebook/service/ScoreEntryServiceTest.java`: Unit tests toàn diện (single upsert, bulk upsert, out of range, scale, null vs 0.0, 10-day limit, semester locked, version conflict, duplicate bulk students, score grid pagination).
- `.../scorebook/controller/ScoreEntryAuthorizationIntegrationTest.java`: Integration tests phân quyền API (401 anonymous, 403 student, 403 unassigned teacher, 404 office bypass).
- `.../config/ScorebookFlywayMigrationTest.java`: Kiểm tra Flyway migration các bảng và constraints của V10, V11, V12.

## 4. Quyết định implementation

- Tách kiến trúc: Tách `ScoreGridService`, `ScoreGridLoader`, `ScoreEntryValidator`, `ScoreEntryWriter` để tuân thủ nghiêm ngặt các quy tắc thiết kế Clean Code và vượt qua PMD (tránh CouplingBetweenObjects, TooManyMethods, AvoidInstantiatingObjectsInLoops, CyclomaticComplexity).
- Idempotency cho task tính toán: Dùng key định dạng `RECALC:{studentId}:{academicYearId}` để tự động gộp các lần nhập điểm liên tiếp của học sinh mà không sinh task trùng.
- Auditing: Ghi nhận sự kiện `STUDENT_SCORE_CREATED` và `STUDENT_SCORE_UPDATED` đầy đủ snapshot before/after vào bảng `audit_log` trong cùng transaction với thay đổi điểm.
- Unchanged data: Khi dữ liệu gửi lên không thay đổi so với hiện tại, hệ thống trả về bản ghi hiện tại mà không bump version, không ghi audit thừa và không sinh task tính toán lại.

## 5. Validation Result

### Backend validation chính thức

- `test`: `PASS` — Chạy toàn bộ 156 tests backend (gồm 13 unit tests ScoreEntry, 4 integration tests auth, migration tests và baseline tests) đều thành công (`156 tests completed, 0 failed`).
- `checkstyle`: `PASS` — Cả `checkstyleMain` và `checkstyleTest` đều đạt 0 lỗi, tuân thủ độ dài dòng <= 120 ký tự và thứ tự import chuẩn.
- `PMD`: `PASS` — Cả `pmdMain` và `pmdTest` đều đạt 0 lỗi với cấu hình strict của dự án.
- `build`: `PASS` — Toàn bộ quy trình `test checkstyleMain checkstyleTest pmdMain pmdTest build jacocoTestReport` thành công.

### JaCoCo Coverage

- Module `scorebook`: Đạt chỉ số coverage cao trên các service mới viết (`ScoreEntryService`, `ScoreGridService`, `ScoreEntryValidator`, `ScoreEntryWriter`, `TranscriptStateService`, `CalculationTaskService`, `ScoreEntryContext`).

## 6. Deviations và blocker

- Không có sai lệch nghiệp vụ so với Developer Plan 037.
- Các vấn đề phát sinh trong quá trình code (PMD coupling, Mockito strict stubbing, import format) đều đã được xử lý triệt để trong 3 vòng lặp `code -> test -> debug`.

## 7. Next steps

- Sẵn sàng cho Plan 038 tiếp theo (Background calculation worker / Score calculation engine).

