# Dev Note 038: Score Change Request

## 1. Developer Plan và trạng thái

- Related Developer Plan: `document/dev-impl-plan/be/scorebook/038-score-change-request-2026-08-25.md`.
- Plan status: `Approved` ngày `2026-08-25`.
- Implementation status: `Implemented; backend validation PASS`.
- Application-document version: `v2`.

## 2. Phạm vi đã thực hiện

- Thêm migration V13 cho `score_change_request`, FK tới assessment column, student, student score và app user, CHECK cho snapshot/proposed score, status, index và unique pending key.
- Thêm `ScoreChangeRequest`, `ScoreChangeRequestStatus` và `ScoreSnapshotStatus`; snapshot hỗ trợ `UNSCORED` khi ô điểm chưa có row.
- Thêm request/response DTO cho create, filter, reject, summary và detail.
- Thêm repository có kiểm tra pending conflict, query phân trang/specification và pessimistic lock khi duyệt/từ chối/hủy.
- Thêm context, validator, mapper và service cho:
  - tạo request với assignment authorization, enrollment validation và before snapshot;
  - teacher đọc request của mình, office/admin đọc và lọc toàn trường;
  - approve-and-apply có self-review guard, snapshot conflict check, score upsert, transcript state/task và audit trong transaction;
  - reject có lý do và cancel chỉ cho requester hoặc Admin.
- Thêm REST API `/api/v2/score-change-requests` với create, list, detail, approve, reject và cancel.
- Thêm unit tests cho validator/service và authorization integration tests cho anonymous, student và academic office.
- Flyway migration tests hiện có đã chạy qua V13 cùng toàn bộ test suite.

## 3. Files đã thay đổi

### Database

- `BE/BaiTap-RS/src/main/resources/db/migration/V13__create_score_change_request.sql`.

### Domain, DTO và repository

- `.../scorebook/domain/entity/ScoreChangeRequest.java`.
- `.../scorebook/domain/entity/ScoreChangeRequestStatus.java`.
- `.../scorebook/domain/entity/ScoreSnapshotStatus.java`.
- `.../scorebook/domain/DTOs/requests/ReqCreateScoreChangeRequestDTO.java`.
- `.../scorebook/domain/DTOs/requests/ReqFilterScoreChangeRequestDTO.java`.
- `.../scorebook/domain/DTOs/requests/ReqRejectScoreChangeRequestDTO.java`.
- `.../scorebook/domain/DTOs/response/ResScoreChangeRequestDTO.java`.
- `.../scorebook/domain/DTOs/response/ResScoreChangeRequestDetailDTO.java`.
- `.../scorebook/repository/ScoreChangeRequestRepository.java`.

### Service và controller

- `.../scorebook/service/ScoreChangeRequestContext.java`.
- `.../scorebook/service/ScoreChangeRequestValidator.java`.
- `.../scorebook/service/ScoreChangeRequestMapper.java`.
- `.../scorebook/service/ScoreChangeRequestSpecifications.java`.
- `.../scorebook/service/ScoreChangeRequestService.java`.
- `.../scorebook/controller/ScoreChangeRequestController.java`.

### Tests và validation hygiene

- `.../scorebook/service/ScoreChangeRequestTestFixtures.java`.
- `.../scorebook/service/ScoreChangeRequestValidatorTest.java`.
- `.../scorebook/service/ScoreChangeRequestServiceTest.java`.
- `.../scorebook/controller/ScoreChangeRequestAuthorizationIntegrationTest.java`.
- Xóa duplicate `Optional` import trong `CalculationTaskRepository.java` và `StudentTermTranscriptRepository.java` để PMD baseline chạy được.

## 4. Quyết định implementation và deviation

- `before_status` map bằng `ScoreSnapshotStatus` thay vì `ScoreStatus` để biểu diễn type-safe trạng thái `UNSCORED`.
- Các FK dùng `BIGINT` tương thích với schema hiện tại V10-V12; không đổi hàng loạt signed/unsigned của các migration trước.
- Pending uniqueness dùng cột nullable `pending_request_key` do entity quản lý cùng unique constraint. Cách này tương thích cả MySQL và H2; không dùng generated column vì H2 Flyway test không hỗ trợ cú pháp `STORED` đang dùng.
- Approve chuyển request trực tiếp sang `APPLIED` sau khi score và calculation state/task được cập nhật trong cùng transaction, đúng behavior API của plan.
- Không thêm worker calculation, công thức điểm, frontend, Postman hoặc API legacy.

## 5. Validation Result

- `test`: `PASS` — `GRADLE_USER_HOME=.gradle-user-home ./gradlew test`, toàn bộ `168 tests completed, 0 failed`.
- `checkstyle`: `PASS` — `GRADLE_USER_HOME=.gradle-user-home ./gradlew checkstyleMain checkstyleTest`.
- `PMD`: `PASS` — `GRADLE_USER_HOME=.gradle-user-home ./gradlew pmdMain pmdTest`.
- `build`: `PASS` — `GRADLE_USER_HOME=.gradle-user-home ./gradlew build`.
- JaCoCo: `PASS` — `GRADLE_USER_HOME=.gradle-user-home ./gradlew jacocoTestReport`; report tại `BE/BaiTap-RS/build/reports/jacoco/test/jacocoTestReport.xml`.
- Focused test: `ScoreChangeRequestValidatorTest`, `ScoreChangeRequestServiceTest`, `ScoreChangeRequestAuthorizationIntegrationTest`, `FlywayMigrationTest` và `ScorebookFlywayMigrationTest` đều PASS.

Coverage line tham khảo từ JaCoCo: `ScoreChangeRequestService` 103/127, `ScoreChangeRequestMapper` 34/45, `ScoreChangeRequest` 29/36, `ScoreChangeRequestValidator` 21/35.

## 6. Known blockers và remaining risks

- Chưa có full persistence integration test mô phỏng teacher create -> academic office approve với toàn bộ dữ liệu nghiệp vụ seed; service flow đã được unit test và migration/authorization đã được integration test.
- Chưa có test cạnh tranh hai transaction tạo cùng pending cell trên database thật; unique pending key và global `DataIntegrityViolationException` handler là lớp bảo vệ hiện tại.
- Chưa chạy preflight Docker MySQL trong turn này; validation dùng H2/Flyway test và Gradle local cache.

## 7. Next steps

- Bổ sung full end-to-end persistence test và MySQL preflight trước khi deploy production.
- Khi có Plan calculation worker, tái sử dụng `TranscriptStateService`/`CalculationTaskService` và bảo vệ `source_version` theo baseline v2.
