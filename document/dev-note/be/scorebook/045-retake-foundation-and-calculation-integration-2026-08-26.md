# Dev Note: Kế hoạch 045 - Retake Foundation and Calculation Integration

- **Kế hoạch liên quan**: [`document/dev-impl-plan/be/scorebook/045-retake-foundation-and-calculation-integration-2026-08-26.md`](../../../../dev-impl-plan/be/scorebook/045-retake-foundation-and-calculation-integration-2026-08-26.md)
- **Trạng thái phê duyệt**: `Approved` (2026-08-26)
- **Trạng thái triển khai**: `Completed`
- **Application-document version**: `v2`
- **Ngày ghi nhận**: 2026-08-26

---

## 1. Phạm vi thực tế hoàn thành

1. **Database Migration**:
   - Tạo Flyway migration `V17__create_retake_exam.sql`:
     - Tạo bảng `retake_exam` với khóa chính `retake_id`, foreign keys (`student_id`, `academic_year_id`, `subject_id`), các cột snapshot `pre_retake_score`, `retake_score`, `exam_date`, `status`, `note`, `created_by`, `updated_by`, timestamps.
     - Tạo Unique Constraint `uk_retake_student_year_subject` trên `(student_id, academic_year_id, subject_id)`.
     - Tạo check constraints cho thang điểm [0.0, 10.0] và status (`PLANNED`, `SCORED`, `CANCELLED`).
     - Tạo indexes `idx_retake_exam_student_year` và `idx_retake_exam_status`.
     - Thêm Foreign Key `fk_subject_annual_result_retake` từ `student_subject_annual_result.retake_id` sang `retake_exam.retake_id`.

2. **Backend Domain & Persistence**:
   - Enum `RetakeExamStatus` (`PLANNED`, `SCORED`, `CANCELLED`).
   - Entity `RetakeExam` theo quy chuẩn JPA và Lombok.
   - Repository `RetakeExamRepository` kế thừa `JpaRepository` và `JpaSpecificationExecutor`.
   - Specifications helper `RetakeExamSpecifications` hỗ trợ lọc động.

3. **DTOs & Validation**:
   - Request DTOs: `ReqCreateRetakeExamDTO`, `ReqUpdateRetakeScoreDTO`, `ReqFilterRetakeExamDTO`.
   - Response DTO: `ResRetakeExamDTO`.
   - Bean validation chặt chẽ: `@NotNull`, `@Positive`, `@DecimalMin`, `@DecimalMax`, `@Digits(integer = 2, fraction = 1)`, `@Size(max = 1000)`.

4. **Service & Business Rules**:
   - `RetakeExamService`:
     - Snapshot `pre_retake_score` từ `StudentSubjectAnnualResult.regular_dtbmh_cn` (chặn tạo nếu chưa có điểm thường hợp lệ).
     - Ràng buộc duy nhất 1 bản ghi thi lại trên mỗi `{student, academicYear, subject}`.
     - Hỗ trợ tạo trạng thái `PLANNED` hoặc `SCORED`.
     - Cập nhật điểm thi lại và chuyển trạng thái sang `SCORED`.
     - Hủy kỳ thi lại với trạng thái `CANCELLED` (bảo toàn lịch sử audit).
     - Tích hợp phát audit event cho tất cả hành động tạo, sửa điểm, hủy.
     - Tích hợp `TranscriptStateService.touchAnnualTranscript` và `CalculationTaskService.ensureRecalcTask` khi điểm thi lại được nhập, cập nhật hoặc khi hủy bản ghi đã từng có điểm.

5. **Calculation & Worker Recalculation Integration**:
   - Cập nhật `TranscriptRecalculationService.calculateAnnualResults`:
     - Tải danh sách `RetakeExam` của học sinh trong năm học.
     - Với môn có bản ghi `RetakeExam` hợp lệ (`SCORED`, điểm không null): gán `official_dtbmh_cn = retake_score`, `calculation_source = RETAKE`, `retake_id = retake.getId()`.
     - Với môn không có thi lại `SCORED`: giữ nguyên `official_dtbmh_cn = regular_dtbmh_cn`, `calculation_source = REGULAR`, `retake_id = null`.
     - Giữ nguyên `regular_dtbmh_cn` và `regular_dtbcn`.
     - Tính `final_dtbcn` từ toàn bộ điểm `official_dtbmh_cn` của các môn học thuật (`ACADEMIC`).
     - Tự động gán `result_source = RETAKE` cho `StudentAnnualTranscript` nếu có ít nhất một môn dùng điểm thi lại, ngược lại giữ `REGULAR`.

6. **REST API Controller**:
   - `RetakeExamController` tại `/api/v2/retake-exams` với phân quyền `@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")`:
     - `POST /api/v2/retake-exams` (201 Created)
     - `PUT /api/v2/retake-exams/{retakeId}/score` (200 OK)
     - `POST /api/v2/retake-exams/{retakeId}/cancel` (200 OK)
     - `GET /api/v2/retake-exams/{retakeId}` (200 OK)
     - `GET /api/v2/retake-exams` (200 OK, Pageable)

7. **Unit & Integration Tests**:
   - `RetakeExamServiceTest`: bao phủ các ca tạo planned/scored, cập nhật điểm, hủy planned/scored, kiểm tra lỗi missing transcript, missing regular score, duplicate tuple, cancelled update, tra cứu phân trang.
   - `RetakeExamControllerTest`: bao phủ gọi qua service và format response.
   - `RetakeExamAuthorizationIntegrationTest`: kiểm tra phân quyền anonymous (401), STUDENT/TEACHER (403), ACADEMIC_OFFICE (200).
   - `TranscriptRecalculationServiceTest`: bổ sung test thay thế điểm thi lại, tính `final_dtbcn`, giữ nguyên regular score, loại bỏ thi lại CANCELLED.
   - `ScorebookFlywayMigrationTest`: kiểm tra schema table, constraints, indexes của V17.

## 2. File thay đổi

### Database Migration
- `BE/BaiTap-RS/src/main/resources/db/migration/V17__create_retake_exam.sql`

### Backend Production Code
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/entity/RetakeExamStatus.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/entity/RetakeExam.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/repository/RetakeExamRepository.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/DTOs/requests/ReqCreateRetakeExamDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/DTOs/requests/ReqUpdateRetakeScoreDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/DTOs/requests/ReqFilterRetakeExamDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/DTOs/response/ResRetakeExamDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/RetakeExamSpecifications.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/RetakeExamService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/TranscriptRecalculationService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/controller/RetakeExamController.java`

### Backend Tests
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/service/RetakeExamServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/controller/RetakeExamControllerTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/controller/RetakeExamAuthorizationIntegrationTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/service/TranscriptRecalculationServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/config/ScorebookFlywayMigrationTest.java`

### Dev Note
- `document/dev-note/be/scorebook/045-retake-foundation-and-calculation-integration-2026-08-26.md`
- `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`

## 3. Quyết định kỹ thuật và tính nhất quán

- **Schema decision**: Giữ nguyên `result_source` và `calculation_source` có 2 giá trị `REGULAR` và `RETAKE` theo quyết định của Plan 041.
- **Authorization**: Sử dụng `hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')` cho toàn bộ các thao tác quản lý thi lại.
- **Audit**: Mọi thay đổi dữ liệu thi lại (`RETAKE_EXAM_CREATED`, `RETAKE_EXAM_SCORE_UPDATED`, `RETAKE_EXAM_CANCELLED`) đều được ghi nhận đầy đủ vào audit log qua `ScorebookAuditService`.
- **Durable recalculation**: Khi điểm thi lại thay đổi hoặc bị hủy, `StudentAnnualTranscript.source_version` tăng lên và calculation task được tạo/merge thông qua `CalculationTaskService.ensureRecalcTask`.

## 4. Validation Result

| Kiểm tra       | Lệnh                                                  | Trạng thái | Ghi chú                                                 |
| -------------- | ----------------------------------------------------- | ---------- | ------------------------------------------------------- |
| **test**       | `./gradlew --no-daemon test`                          | **PASS**   | Toàn bộ 48+ test classes và test cases mới/cũ đều PASS. |
| **checkstyle** | `./gradlew --no-daemon checkstyleMain checkstyleTest` | **PASS**   | 0 checkstyle error.                                     |
| **PMD**        | `./gradlew --no-daemon pmdMain pmdTest`               | **PASS**   | 0 PMD violation.                                        |
| **build**      | `./gradlew --no-daemon build`                         | **PASS**   | Build và bootJar thành công 100%.                       |

- JaCoCo Report: `BE/BaiTap-RS/build/reports/jacoco/test/html/index.html`
  - `RetakeExamService`: 95% instruction coverage, 83% branch coverage.
  - Toàn bộ các nhánh core logic thi lại, kiểm tra điều kiện, chuyển trạng thái và tính điểm đều được bao phủ.

## 5. Số vòng debug (code → test → debug)

- **Số vòng**: 2 vòng (điều chỉnh Checkstyle line length và PMD lint rules trong test files).

## 6. Blockers và rủi ro còn lại

- **Blocker**: Không có.
- **Rủi ro còn lại**: Không có.

