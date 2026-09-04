# Dev Note: Kế hoạch 042 - Subject Calculation Engine & Worker Lifecycle

- **Kế hoạch liên quan**: [`document/dev-impl-plan/be/scorebook/042-subject-calculation-engine-and-worker-lifecycle-2026-08-25.md`](../../../../dev-impl-plan/be/scorebook/042-subject-calculation-engine-and-worker-lifecycle-2026-08-25.md)
- **Trạng thái phê duyệt**: `Approved` (user yêu cầu triển khai 42A và tiếp tục 42B qua agent ngày 2026-08-25)
- **Trạng thái triển khai**: `42A + 42B implemented`
- **Application-document version**: `v2`
- **Ngày ghi nhận**: 2026-08-25

## 1. Phạm vi thực tế hoàn thành

- Tạo `SubjectScoreCalculator` cho `Đtbmh`, điểm kỹ năng, `Đtbhk`, `ĐtbmhCN` và `Đtbcn`; chỉ tính ô `SCORED` có giá trị, giữ điểm `0.0` hợp lệ và làm tròn `HALF_UP` đến 0.1.
- Tạo `TranscriptRecalculationService` để tính và upsert kết quả theo thứ tự môn → học kỳ → năm học, đồng thời bảo vệ `sourceVersion`.
- Tạo `CalculationTaskWorker` polling nền, claim bằng pessimistic lock, lifecycle `PENDING → RUNNING → SUCCEEDED/FAILED`, retry backoff và requeue khi phát hiện version mới.
- Mở rộng `CalculationTaskService` cho claim, retry, filter phân trang, mapping response và manual recalculation.
- Ghi audit cho thao tác retry và yêu cầu manual recalculation theo `NFR-AUDITABILITY-009`.
- Flow recalculation sắp xếp semester theo `endDate ASC`, sau đó `displayOrder` và `id` để tie-break; thứ tự hiển thị academic vẫn giữ `displayOrder`.
- Mở API Giáo vụ:
  - `GET /api/v2/scorebooks/calculation-tasks`
  - `POST /api/v2/scorebooks/calculation-tasks/{taskId}/retry`
  - `POST /api/v2/students/{studentCode}/transcripts/recalculate`
  - route numeric `studentId` được giữ ở `/api/v2/students/{studentId:\\d+}/transcripts/recalculate`.
- Kích hoạt scheduling để worker calculation chạy theo `app.calculation.worker-interval-ms`.

### Test và coverage của 42B

- `SubjectScoreCalculatorTest`: công thức môn thường, môn kỹ năng, điểm 0, trạng thái không tính, dữ liệu thiếu, làm tròn và điểm trung bình năm.
- `TranscriptRecalculationServiceTest`: upsert term/annual result, thứ tự semester theo `endDate`, version protection và enrollment không hợp lệ.
- `CalculationTaskServiceTest`: claim, retry backoff, max attempts, idempotency, source-version requeue và retry task FAILED.
- `CalculationTaskWorkerTest`: claim/execute thành công, failure handling, không có task và task không load được.
- `CalculationTaskControllerTest` và `CalculationTaskControllerIntegrationTest`: delegation endpoint, HTTP 202 và authorization `401/403/200`.

## 2. File thay đổi

### Backend production

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/BaiTapRsApplication.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/controller/CalculationTaskController.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/DTOs/requests/ReqFilterCalculationTaskDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/DTOs/response/ResCalculationTaskDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/domain/entity/CalculationTask.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/repository/CalculationTaskRepository.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/CalculationTaskService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/CalculationTaskSpecifications.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/CalculationTaskWorker.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/SubjectScoreCalculator.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/TranscriptRecalculationService.java`

### Developer Plan / Dev Note summaries

- `document/dev-impl-plan/be/scorebook/042-subject-calculation-engine-and-worker-lifecycle-2026-08-25.md`
- `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`
- `document/dev-note/be/scorebook/042-subject-calculation-engine-and-worker-lifecycle-2026-08-25.md`
- `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`

### Test files

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/service/SubjectScoreCalculatorTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/service/TranscriptRecalculationServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/service/CalculationTaskServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/service/CalculationTaskWorkerTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/controller/CalculationTaskControllerTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/controller/CalculationTaskControllerIntegrationTest.java`

## 3. Quyết định và sai lệch

- API/filter/response dùng `studentCode` ở boundary; `studentId` vẫn là khóa internal/persistence và có numeric compatibility route.
- Phần retake chưa triển khai; kết quả 42A dùng `REGULAR` theo plan.
- Test dùng Mockito cho orchestration/lifecycle và H2 `@SpringBootTest` cho authorization/controller integration; không thay đổi production schema.
- Không thêm migration/schema mới.

## 4. Validation

| Công cụ / Lệnh | Trạng thái | Ghi chú |
| --- | --- | --- |
| `./gradlew test` | **PASS** | 254 tests, 0 skipped, 0 failures, 0 errors; task đã finalize `jacocoTestReport`. |
| `./gradlew jacocoTestReport` | **PASS** | Report XML/HTML được tạo tại `BE/BaiTap-RS/build/reports/jacoco/test/`; không đặt threshold mới. |
| `./gradlew compileJava` | **PASS** | Biên dịch production code. |
| `./gradlew checkstyleMain checkstyleTest pmdMain pmdTest` | **PASS** | Quality checks cho main/test; PMD còn thông báo baseline `LoosePackageCoupling` misconfigured nhưng task PASS. |
| `./gradlew build -x test` | **PASS** | Build, test compilation, Checkstyle/PMD test tasks; không thực thi test. |
| `git diff --check` | **PASS** | Không có whitespace error. |

Coverage line theo JaCoCo cho class mới/chính:

- `SubjectScoreCalculator`: 94% line, 75% branch.
- `TranscriptRecalculationService`: 91% line, 61% branch.
- `CalculationTaskService`: 74% line, 54% branch.
- `CalculationTaskWorker`: 100% line, 100% branch.
- `CalculationTaskController`: 34% line; các route chính đã được kiểm tra qua unit/integration test.

## 5. Known risks và bước tiếp theo

- Chưa có kiểm thử đa tiến trình trên MySQL thật cho pessimistic claim; test hiện tại dùng Mockito/H2 và chưa mô phỏng contention production.
- Scheduler lifecycle được kiểm tra qua worker method và context integration, nhưng chưa có test thời gian thực phụ thuộc polling interval.
- Không đặt coverage threshold vì project chưa quy định threshold.
