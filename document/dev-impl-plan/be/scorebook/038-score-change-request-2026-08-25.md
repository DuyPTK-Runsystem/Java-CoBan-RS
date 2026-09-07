# Developer Plan 038: Score Change Request

## 1. Trạng thái và phiên bản áp dụng

- Status: `Approved`.
- Application-document version: `v2`.
- Ngày lập plan: `2026-08-25`.
- Phê duyệt: user approved qua agent ngày `2026-08-25`.
- Module: Backend `scorebook` — Score Change Request.
- Phụ thuộc: Plan `036` (Scorebook Foundation), Plan `037` (Student Score Entry), Plan `026` (Enrollment), Plan `027` (Class Subject & Teaching Assignment).

## 2. Mục tiêu

Xây dựng toàn bộ luồng quản lý yêu cầu sửa điểm (`Score Change Request`) theo đặc tả nghiệp vụ v2, bao gồm:

- Cho phép Giáo viên bộ môn (GVBM) tạo yêu cầu sửa điểm khi quá hạn sửa trực tiếp (10 ngày) hoặc học kỳ đã khóa, hoặc khi cần quy trình phê duyệt chính thức.
- Tự động chụp snapshot trạng thái và giá trị điểm trước khi sửa (`before_status`, `before_value`, `student_score_id`) để bảo đảm tính toàn vẹn dữ liệu.
- Cung cấp API tra cứu, lọc và xem chi tiết danh sách yêu cầu sửa điểm có phân quyền (GVBM xem các yêu cầu của mình; Giáo vụ / Admin xem toàn trường hoặc lọc theo tiêu chí).
- Cho phép Giáo vụ / Admin phê duyệt yêu cầu (`approve`): tự động kiểm tra xung đột dữ liệu với snapshot ban đầu, cập nhật/tạo mới `student_score`, chuyển trạng thái `APPLIED`, tăng `source_version`, chuyển trạng thái bảng điểm tổng kết sang `IN_PROGRESS`, tạo/gộp `calculation_task` và ghi audit log trong cùng một transaction.
- Cho phép Giáo vụ / Admin từ chối yêu cầu (`reject`) kèm lý do từ chối.
- Cho phép Giáo viên hủy (`cancel`) yêu cầu đang ở trạng thái `PENDING` do mình tạo.

## 3. Requirement liên quan

### Functional Requirements

- `FR-SCORECHANGE-001`: Giáo viên tạo request sửa điểm.
- `FR-SCORECHANGE-002`: Giáo viên xem trạng thái các request của mình.
- `FR-SCORECHANGE-003`: Giáo vụ xem, duyệt hoặc từ chối request.
- `FR-SCORECHANGE-004`: Hệ thống tự động áp dụng điểm mới sau khi request được duyệt hợp lệ.
- `FR-SCORECHANGE-005`: Hệ thống lưu và cho phép người có quyền xem lịch sử request.

### Business Rules & NFR

- `BR-SCORECHANGE-001` đến `BR-SCORECHANGE-003`: Sửa trực tiếp chỉ áp dụng trong 10 ngày từ `entered_at` và học kỳ chưa khóa; quá 10 ngày hoặc học kỳ đã khóa bắt buộc phải tạo request sửa điểm.
- `BR-SCORECHANGE-004`: Một request chỉ cần một giáo vụ duyệt.
- `BR-SCORECHANGE-005`: Người duyệt không được đồng thời là người yêu cầu (`reviewed_by != requested_by`).
- `BR-SCORECHANGE-006`: Mỗi ô điểm (cặp `assessment_column_id` và `student_id`) chỉ có tối đa một request ở trạng thái `PENDING`.
- `BR-SCORECHANGE-007`: Khi duyệt, điểm hiện tại phải còn khớp snapshot `before_*` trong request.
- `BR-SCORECHANGE-008`: Nếu dữ liệu điểm đã thay đổi so với snapshot `before_*`, request bị từ chối áp dụng (báo lỗi xung đột dữ liệu).
- `BR-SCORECHANGE-009`: Điểm đề xuất mới (`proposed_status`, `proposed_value`) phải tuân thủ đúng quy tắc điểm 0.0 - 10.0, scale <= 1 đối với `SCORED` và null đối với `ABSENT`, `EXEMPTED`, `CANCELLED`.
- `BR-SCORECHANGE-010`: Phê duyệt cập nhật điểm tự động và tạo `calculation_task`.
- `BR-SCORECHANGE-011`: Phê duyệt không yêu cầu mở khóa toàn bộ học kỳ (vẫn duyệt và áp dụng điểm ngay cả khi học kỳ đã `LOCKED`).
- `BR-SCORECHANGE-012`: Mọi trạng thái và thay đổi phải có audit log (`AuditLog`).
- `BR-SCORECHANGE-013`: Vòng đời trạng thái:
  ```text
  PENDING → APPROVED → APPLIED (tự động chuyển sang APPLIED khi duyệt thành công)
          ↘ REJECTED
          ↘ CANCELLED
  ```
- `NFR-CALC-004` đến `NFR-CALC-008`: Không tính điểm đồng bộ trong request; thực hiện cập nhật điểm, tăng version, set `IN_PROGRESS` và tạo `calculation_task` trong cùng một transaction.
- `NFR-AUDITABILITY-003`: Toàn bộ các thao tác tạo request, duyệt/áp dụng, từ chối, hủy đều phải lưu audit log.

### 3.1. Nguyên tắc triển khai: Nói KHÔNG với HARDCODE

- **Không hardcode Magic Numbers**: Mọi hằng số số học (khoảng điểm `0.0..10.0`, giới hạn `10` ngày sửa trực tiếp, độ dài tối đa chuỗi `1000`, scale thập phân `1`, v.v.) phải được khai báo dạng hằng số `private static final` có định danh và ngữ nghĩa rõ ràng.
- **Không hardcode Chuỗi trạng thái (Status Strings)**: Toàn bộ trạng thái nghiệp vụ bắt buộc map qua Enum có kiểu an toàn (`ScoreChangeRequestStatus`, `ScoreStatus`, `SemesterStatus`), tuyệt đối không so sánh chuỗi thô rải rác trong code.
- **Không hardcode Quyền hạn & Action Audit**: Định nghĩa tập trung các hằng số role (`ROLE_ADMIN`, `ROLE_ACADEMIC_OFFICE`, `ROLE_TEACHER`), endpoint path parameters và tên hành động audit (`CREATE_SCORE_CHANGE_REQUEST`, `APPROVE_AND_APPLY_SCORE_CHANGE_REQUEST`, `REJECT_SCORE_CHANGE_REQUEST`, `CANCEL_SCORE_CHANGE_REQUEST`).
- **Không hardcode User ID / Actor**: Tuyệt đối không giả lập hoặc fix cứng `userId` trong business logic; mọi thông tin người dùng, quyền hạn phải được trích xuất động qua `AuditContext` và `SecurityContextHolder`.
- **Không hardcode Query / Nối chuỗi SQL**: Sử dụng Spring Data JPA Repository, `JpaSpecificationExecutor` với Criteria Builder an toàn, tránh hoàn toàn rủi ro SQL Injection hoặc query tĩnh không linh hoạt.
- **Không hardcode Timezone**: Sử dụng hằng số múi giờ chuẩn (`ZoneId.of("Asia/Ho_Chi_Minh")`) hoặc cấu hình thời gian tập trung của dự án.

## 4. Phạm vi

### In-scope

- Migration DB `V13__create_score_change_request.sql` tạo bảng `score_change_request` và các foreign keys/indexes cần thiết.
- Entity `ScoreChangeRequest`, Enum `ScoreChangeRequestStatus` (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `APPLIED`).
- Repository `ScoreChangeRequestRepository` với các method truy vấn, kiểm tra unique pending request và phân trang lọc dữ liệu.
- DTOs cho request/response: `ReqCreateScoreChangeRequestDTO`, `ReqRejectScoreChangeRequestDTO`, `ResScoreChangeRequestDTO`, `ResScoreChangeRequestDetailDTO`.
- Validation component `ScoreChangeRequestValidator` kiểm tra ràng buộc nghiệp vụ (không tự duyệt, không trùng pending, kiểm tra snapshot, validate điểm đề xuất, kiểm tra quyền hủy).
- Service `ScoreChangeRequestService` điều phối tạo yêu cầu, tra cứu danh sách/chi tiết, duyệt & áp dụng điểm tự động, từ chối và hủy yêu cầu.
- Controller `ScoreChangeRequestController` cung cấp đầy đủ REST endpoints theo chuẩn `/api/v2/score-change-requests`.
- Unit test suite toàn diện và Integration test xác thực luồng tạo, duyệt (kèm cập nhật `student_score`, `transcript` version, `calculation_task`), từ chối và hủy request.
- Checkstyle, PMD và build validation đầy đủ.

### Out-of-scope

- Background Calculation Worker xử lý các công thức điểm trung bình (thuộc phạm vi calculation worker riêng).
- Giao diện người dùng Frontend Vue 3 / PrimeVue và Storybook.
- Quản lý kỳ thi lại (`retake_exam`) và bảng điểm tốt nghiệp/khen thưởng.

## 5. Thiết kế kỹ thuật & Luồng xử lý

### 5.1. Database Schema (`score_change_request`)

```sql
CREATE TABLE score_change_request (
    request_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    assessment_column_id BIGINT UNSIGNED NOT NULL,
    student_id BIGINT UNSIGNED NOT NULL,
    student_score_id BIGINT UNSIGNED NULL,
    before_status VARCHAR(20) NOT NULL,
    before_value DECIMAL(3,1) NULL,
    proposed_status VARCHAR(20) NOT NULL,
    proposed_value DECIMAL(3,1) NULL,
    reason VARCHAR(1000) NOT NULL,
    requested_by BIGINT UNSIGNED NOT NULL,
    requested_at DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    reviewed_by BIGINT UNSIGNED NULL,
    reviewed_at DATETIME NULL,
    rejection_reason VARCHAR(1000) NULL,
    applied_at DATETIME NULL,
    CONSTRAINT fk_scr_column FOREIGN KEY (assessment_column_id) REFERENCES assessment_column(column_id),
    CONSTRAINT fk_scr_student FOREIGN KEY (student_id) REFERENCES student(student_id),
    CONSTRAINT fk_scr_score FOREIGN KEY (student_score_id) REFERENCES student_score(score_id),
    CONSTRAINT fk_scr_requested_by FOREIGN KEY (requested_by) REFERENCES app_user(user_id),
    CONSTRAINT fk_scr_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES app_user(user_id)
);

CREATE INDEX idx_scr_column_student ON score_change_request(assessment_column_id, student_id);
CREATE INDEX idx_scr_requested_by ON score_change_request(requested_by);
CREATE INDEX idx_scr_status ON score_change_request(status);
```

### 5.2. Luồng tạo yêu cầu (`createRequest`)

1. Lấy thông tin người dùng hiện tại (`requested_by` từ `AuditContext`).
2. Kiểm tra quyền GVBM phụ trách môn/lớp thông qua `ScorebookGuard` (hoặc Giáo vụ/Admin).
3. Kiểm tra tính hợp lệ của học sinh (`ACTIVE`, phân lớp khớp) và cột điểm (`ACTIVE`).
4. Kiểm tra ô điểm `(assessment_column_id, student_id)` chưa có request nào đang ở trạng thái `PENDING`.
5. Đọc bản ghi `student_score` hiện tại (nếu có):
   - Nếu đã có: Chụp `before_status = existing.scoreStatus`, `before_value = existing.scoreValue`, `student_score_id = existing.id`.
   - Nếu chưa có: Ghi nhận `before_status = 'UNSCORED'`, `before_value = null`, `student_score_id = null`.
6. Validate điểm đề xuất (`proposed_status`, `proposed_value`):
   - Phải hợp lệ theo rule điểm.
   - Phải khác với trạng thái/giá trị hiện tại (không tạo request vô nghĩa).
7. Lưu bản ghi `ScoreChangeRequest` với trạng thái `PENDING`, `requested_at = now()`.
8. Ghi Audit log hành động `CREATE_SCORE_CHANGE_REQUEST`.

### 5.3. Luồng phê duyệt và áp dụng (`approveAndApplyRequest`)

1. Lấy thông tin người duyệt (`reviewed_by` từ `AuditContext`).
2. Kiểm tra quyền: Phải là `ROLE_ADMIN` hoặc `ROLE_ACADEMIC_OFFICE`.
3. Kiểm tra người duyệt không phải người tạo (`reviewed_by != requested_by`).
4. Request phải đang ở trạng thái `PENDING`.
5. Kiểm tra snapshot:
   - Đọc `student_score` hiện tại của `(assessment_column_id, student_id)`.
   - Nếu request ghi `student_score_id` có giá trị: `student_score` phải tồn tại, `scoreStatus` phải bằng `before_status`, `scoreValue` phải bằng `before_value`.
   - Nếu request ghi `student_score_id` là null: `student_score` hiện tại vẫn phải chưa tồn tại.
   - Nếu không khớp -> Báo lỗi `409 Conflict` (dữ liệu điểm đã bị thay đổi).
6. Áp dụng điểm vào `student_score`:
   - Nếu đã có: Cập nhật `scoreStatus = proposed_status`, `scoreValue = proposed_value`, `updatedBy = reviewed_by`, `updatedAt = now()`.
   - Nếu chưa có: Tạo mới bản ghi `StudentScore` với `enteredBy = requested_by` (hoặc `reviewed_by`), `enteredAt = now()`.
7. Cập nhật `ScoreChangeRequest`:
   - `status = APPLIED` (hoặc chuyển `APPROVED` rồi `APPLIED`).
   - `reviewed_by = currentUserId`.
   - `reviewed_at = now()`.
   - `applied_at = now()`.
8. Đồng bộ trạng thái tính toán:
   - Gọi `TranscriptStateService.touchTranscripts(studentId, academicYearId, semesterId)` để tăng `source_version` và đặt trạng thái bảng điểm thành `IN_PROGRESS`.
   - Gọi `CalculationTaskService.ensureRecalcTask(studentId, academicYearId, newVersion)`.
9. Ghi Audit log hành động `APPROVE_AND_APPLY_SCORE_CHANGE_REQUEST`.

### 5.4. Luồng từ chối (`rejectRequest`)

1. Kiểm tra quyền `ROLE_ADMIN` hoặc `ROLE_ACADEMIC_OFFICE`.
2. Kiểm tra `reviewed_by != requested_by`.
3. Request phải đang ở trạng thái `PENDING`.
4. Validate `rejection_reason` không được trống.
5. Cập nhật `status = REJECTED`, `reviewed_by = currentUserId`, `reviewed_at = now()`, `rejection_reason = reason`.
6. Ghi Audit log hành động `REJECT_SCORE_CHANGE_REQUEST`.

### 5.5. Luồng hủy yêu cầu (`cancelRequest`)

1. Lấy `currentUserId`.
2. Request phải đang ở trạng thái `PENDING`.
3. Chỉ người tạo (`requested_by == currentUserId`) hoặc Admin mới có quyền hủy.
4. Cập nhật `status = CANCELLED`.
5. Ghi Audit log hành động `CANCEL_SCORE_CHANGE_REQUEST`.

## 6. API Contract

| Method | Endpoint                                            | Quyền                                                       | Mục đích                                                                                                                       |
| ------ | --------------------------------------------------- | ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| `POST` | `/api/v2/score-change-requests`                     | GVBM đúng phân công, Admin, Giáo vụ                         | Tạo yêu cầu sửa điểm                                                                                                           |
| `GET`  | `/api/v2/score-change-requests`                     | Teacher (xem request của mình), Admin, Giáo vụ (xem tất cả) | Tìm kiếm & lọc danh sách request (hỗ trợ phân trang, lọc theo `status`, `scorebookId`, `columnId`, `studentId`, `requestedBy`) |
| `GET`  | `/api/v2/score-change-requests/{requestId}`         | Người tạo request, Teacher phân công, Admin, Giáo vụ        | Xem chi tiết 1 yêu cầu sửa điểm                                                                                                |
| `POST` | `/api/v2/score-change-requests/{requestId}/approve` | Admin, Giáo vụ (`reviewed_by != requested_by`)              | Phê duyệt và tự động áp dụng điểm                                                                                              |
| `POST` | `/api/v2/score-change-requests/{requestId}/reject`  | Admin, Giáo vụ (`reviewed_by != requested_by`)              | Từ chối yêu cầu sửa điểm                                                                                                       |
| `POST` | `/api/v2/score-change-requests/{requestId}/cancel`  | Người tạo request (khi đang PENDING)                        | Hủy yêu cầu sửa điểm                                                                                                           |

## 7. Cấu trúc file dự kiến

### Database Migration

- `[NEW]` `BE/BaiTap-RS/src/main/resources/db/migration/V13__create_score_change_request.sql`

### Domain & DTOs

- `[NEW]` `.../scorebook/domain/entity/ScoreChangeRequestStatus.java`
- `[NEW]` `.../scorebook/domain/entity/ScoreChangeRequest.java`
- `[NEW]` `.../scorebook/domain/DTOs/requests/ReqCreateScoreChangeRequestDTO.java`
- `[NEW]` `.../scorebook/domain/DTOs/requests/ReqRejectScoreChangeRequestDTO.java`
- `[NEW]` `.../scorebook/domain/DTOs/requests/ReqFilterScoreChangeRequestDTO.java`
- `[NEW]` `.../scorebook/domain/DTOs/response/ResScoreChangeRequestDTO.java`
- `[NEW]` `.../scorebook/domain/DTOs/response/ResScoreChangeRequestDetailDTO.java`

### Repository, Service & Controller

- `[NEW]` `.../scorebook/repository/ScoreChangeRequestRepository.java`
- `[NEW]` `.../scorebook/service/ScoreChangeRequestValidator.java`
- `[NEW]` `.../scorebook/service/ScoreChangeRequestService.java`
- `[NEW]` `.../scorebook/service/ScoreChangeRequestMapper.java`
- `[NEW]` `.../scorebook/controller/ScoreChangeRequestController.java`

### Tests

- `[NEW]` `.../scorebook/service/ScoreChangeRequestServiceTest.java`
- `[NEW]` `.../scorebook/service/ScoreChangeRequestValidatorTest.java`
- `[NEW]` `.../scorebook/controller/ScoreChangeRequestControllerTest.java`
- `[NEW]` `.../scorebook/ScoreChangeRequestIntegrationTest.java`

## 8. Kế hoạch Unit Test & Validation

1. **Unit Test - Validator**:
   - `testValidateProposedScore_Valid`: Điểm đề xuất hợp lệ (0.0..10.0, 1 chữ số thập phân).
   - `testValidateProposedScore_Invalid`: Điểm âm, > 10.0, scale > 1, SCORED null value, ABSENT có value.
   - `testValidatePendingConflict`: Ném `AppException(CONFLICT)` nếu đã có request PENDING cho cùng ô điểm.
   - `testValidateDifferentFromCurrent`: Ném `AppException(BAD_REQUEST)` nếu điểm đề xuất giống hệt điểm hiện tại.
   - `testValidateNotSelfReview`: Ném `AppException(FORBIDDEN/BAD_REQUEST)` khi `reviewed_by == requested_by`.
   - `testValidateSnapshotMatch`: Ném `AppException(CONFLICT)` khi `before_*` không khớp `student_score` thực tế lúc duyệt.

2. **Unit Test - Service**:
   - `testCreateRequest_Success`: Tạo request PENDING thành công, lưu snapshot, ghi audit log.
   - `testApproveAndApply_Success`: Duyệt request thành công -> cập nhật điểm, set status APPLIED, gọi `touchTranscripts` và `ensureRecalcTask`, ghi audit log.
   - `testApproveAndApply_NewScore_Success`: Duyệt request cho ô điểm trước đó chưa nhập (`UNSCORED`) -> tạo mới `student_score`.
   - `testApproveAndApply_Conflict_ThrowsConflict`: Snapshot bị lệch lúc duyệt -> ném lỗi và không áp dụng.
   - `testRejectRequest_Success`: Từ chối request thành công kèm lý do, ghi audit log.
   - `testCancelRequest_Success`: Người tạo hủy request PENDING thành công.
   - `testCancelRequest_NotRequester_ThrowsForbidden`: Người khác không có quyền hủy request.

3. **Integration Test**:
   - Khởi chạy full Spring Boot context với Flyway migration V13.
   - Luồng end-to-end: Teacher tạo request -> Admin duyệt -> `student_score` được cập nhật, `transcript` version tăng, task được tạo.
   - Luồng duyệt khi học kỳ bị khóa (`LOCKED`): Vẫn duyệt và áp dụng thành công (thỏa mãn `BR-SCORECHANGE-011`).

4. **Validation Suite**:
   - Chạy toàn bộ Unit/Integration tests: `./gradlew test`.
   - Kiểm tra Checkstyle: `./gradlew checkstyleMain checkstyleTest`.
   - Kiểm tra PMD: `./gradlew pmdMain pmdTest`.
   - Build hoàn chỉnh: `./gradlew build -x test`.
