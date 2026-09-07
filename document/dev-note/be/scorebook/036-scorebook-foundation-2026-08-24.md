# Dev Note 036: Scorebook Foundation

## 1. Developer Plan và trạng thái

- Related Developer Plan: `document/dev-impl-plan/be/scorebook/036-scorebook-foundation-2026-08-24.md`.
- Plan status: `Approved` ngày `2026-08-24`.
- Implementation status: `Implemented; PMD and full backend validation PASS`.
- Application-document version: `v2`.

## 2. Phạm vi đã thực hiện

- Tạo scorebook foundation backend cho một `class_subject`:
  - lifecycle `DRAFT`, `OPEN`, `PUBLISHED`, `CLOSED`;
  - cột `KTTT`, `KTDK` (API code `KTĐK`), `KTCK`;
  - cấu hình trọng số môn `SKILL`.
- Thêm API v2 tạo/lấy/mở/publish scorebook, thêm/sửa/vô hiệu hóa assessment column và
  upsert skill weight.
- Enforce authorization qua `subject_teaching_assignment` cho teacher; office/admin có
  quyền kiểm soát theo plan.
- Ghi audit cho các mutation scorebook/column/weight.
- Không triển khai `student_score`, calculation worker, score-change request, transcript,
  frontend hoặc Postman.

## 3. Files đã thay đổi

### Migration và persistence

- `BE/BaiTap-RS/src/main/resources/db/migration/V10__create_scorebook_and_assessment.sql`:
  tạo `scorebook`, `assessment_column`, `skill_weight_config`, FK, unique, index và CHECK.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/entity/*`:
  entity/enum và mapping JPA.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/repository/*`:
  repository và truy vấn scorebook/column/weight.

### API và business service

- `.../scorebook/controller/ScorebookController.java`: endpoint v2 và `@PreAuthorize`.
- `.../scorebook/domain/DTOs/requests/*`, `.../response/*`: request/response DTO.
- `.../scorebook/service/ScorebookService.java`: façade giữ nguyên API và transaction boundary.
- `.../scorebook/service/ScorebookLifecycleService.java`: create/get/open/publish lifecycle.
- `.../scorebook/service/ScorebookColumnService.java`: assessment-column mutation.
- `.../scorebook/service/ScorebookSkillWeightService.java`: skill-weight upsert/locking.
- `.../scorebook/service/ScorebookContext.java`, `ScorebookConfigurationValidator.java`,
  `ScorebookResponseService.java`, `ScorebookAuditDataMapper.java`: lookup, validation,
  response và audit-data boundary tách khỏi God Class.
- `.../scorebook/domain/entity/AssessmentType.java`: đổi tên field nội bộ để loại PMD
  field/method name collision, không đổi API method.
- `.../scorebook/service/ScorebookGuard.java`: office/admin bypass và teacher assignment guard.
- `.../scorebook/service/ScorebookAuditService.java`: audit dùng `AuditLog`/`AuditContext`.
- `.../scorebook/service/ScorebookMapper.java`: entity-to-DTO mapping.

### Test

- `.../scorebook/service/ScorebookLifecycleServiceTest.java`: create, duplicate và publish.
- `.../scorebook/service/ScorebookColumnServiceTest.java`: duplicate column.
- `.../scorebook/service/ScorebookSkillWeightServiceTest.java`: invalid weight boundary.
- `.../scorebook/service/ScorebookTestFixtures.java`: shared entity fixtures.
- `.../scorebook/service/ScorebookGuardTest.java`: office bypass, teacher assignment và
  principal context.
- `.../scorebook/controller/ScorebookAuthorizationIntegrationTest.java`: `401`, `403`,
  office authorization và student/teacher denial.
- `.../config/ScorebookFlywayMigrationTest.java`: kiểm tra ba bảng V10 và constraint chính;
  tách khỏi test migration baseline để PMD không phình class cũ.
- Test fixture fixes trong lượt điều tra này:
  - chuyển unit test từ God-Class façade sang các operation service tương ứng, chỉ stub
    response repository ở test thực sự map response.
  - `ScorebookGuardTest`: dùng authenticated token cho case teacher thiếu hồ sơ.
  - `ScorebookAuthorizationIntegrationTest`: tạo scorebook fixture trước khi kiểm tra
    teacher bị từ chối, tránh nhầm `404` của resource không tồn tại với `403` authorization.

## 4. Quyết định implementation

- Migration dùng `V10` vì repository hiện đã dùng V6-V9 cho attendance/calendar/user linkage;
  không dùng số V6 theo skeleton tài liệu cũ.
- Enum Java dùng `KTDK` ASCII để qua Checkstyle; Jackson nhận và trả mã nghiệp vụ `KTĐK`,
  còn JPA/database lưu enum name `KTDK`.
- `CLOSED` được map trong entity/schema nhưng transition thuộc plan semester-lock/score-change;
  Plan 036 không expose endpoint đóng sổ.
- Deactivate assessment column là soft state `INACTIVE`, không xóa vật lý.
- Skill weight được khóa khi publish và không cho upsert lại sau khi đã khóa.

## 5. Validation Result

### Backend validation chính thức

- `test`: `PASS` — test riêng scorebook `12/12` và full backend `138/138` pass với Gradle
  `9.5.1`, `GRADLE_USER_HOME=/tmp/plan36-gradle-home`, shared read-only dependency cache,
  `--offline --no-daemon`.
- `checkstyle`: `PASS` — `checkstyleMain` và `checkstyleTest` chạy thành công, không còn
  warning sau khi format/import được chỉnh.
- `PMD`: `PASS` — `pmdMain` và `pmdTest` chạy thành công; không đổi rule, không suppress.
- `build`: `PASS` — compile, jar, Checkstyle, PMD, test và JaCoCo đều pass.

### JaCoCo

- Task `jacocoTestReport` tồn tại, xác nhận bằng `gradle tasks --all`.
- `jacocoTestReport`: `PASS` sau full test pass; aggregate package `scorebook` đạt
  instruction `949/1836` (`51.7%`), branch `37/112` (`33.0%`), line `233/442` (`52.7%`),
  method `64/114` (`56.1%`) và class `22/25` (`88.0%`).

### Kiểm tra bổ sung đã chạy

- `PASS`: `git diff --check`.
- `PASS`: compile source scorebook bằng `javac --release 21` với dependency cache.
- `PASS`: compile test source scorebook bằng `javac --release 21` với dependency cache;
  chỉ còn warning metadata JUnit do classpath cô lập.
- `PASS`: Checkstyle CLI trực tiếp trên source/test scorebook sau khi sắp xếp import; không
  thay thế task Gradle chính thức.

## 6. Deviations và blocker

- Không có deviation về scope nghiệp vụ so với Plan 036.
- API dùng `KTDK` nội bộ và Jackson code `KTĐK` để đáp ứng đồng thời Checkstyle và business code.
- Blocker file lock của Gradle wrapper đã được tránh bằng Gradle user home tạm và shared
  read-only dependency cache; không sửa wrapper hoặc build config.
- Runtime test và Flyway test pass; cả 5 failure ban đầu đã được khắc phục ở test fixture.
- PMD God Class/coupling/complexity đã được xử lý bằng refactor boundary nghiệp vụ; PMD
  test violations đã được xử lý bằng tách test/helper và explicit assertion types.

## 7. Next steps

- Giữ nguyên scope hiện tại; score entry và background calculation cần plan/approval riêng.
