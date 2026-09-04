# Developer Plan 042: Subject Calculation Engine & Calculation Worker Lifecycle

## 1. Trạng thái và thông tin chung

- **Status**: `Approved`.
- **Application-document version**: `v2`.
- **Ngày lập plan**: `2026-08-25`.
- **Phê duyệt**: User yêu cầu triển khai 42A qua agent ngày `2026-08-25`; full test được defer sang 42B.
- **Module**: Backend `scorebook` / Calculation domain.
- **Tài liệu tham chiếu**:
  - `document/application-doc/v2/modules/04-AssessmentAndScoringModule.md` (Mục 14, 15, 16, 17: Điểm số, Đtbmh, Điểm môn kỹ năng, Các loại điểm trung bình).
  - `document/application-doc/v2/modules/05-ScoreChangeAndCalculationModule.md` (Mục 20: Background calculation, Transaction, Phiên bản, Retry & Failure).
  - `document/application-doc/v2/data-model/07-ResultsAndCalculation.md` (Mục 12, 13, 15: Mô hình kết quả, Quy tắc tính điểm, Background calculation).
- **Dependencies**: Plan 041 (`V16__create_transcript_subject_result_tables.sql`, Entities & Repositories cho transcript và subject results).

---

## 2. Quyết định kỹ thuật & Nghiệp vụ đã thống nhất

1. **Gộp Plan 042 & Plan 043**:
   - **Part A (Engine)**: Engine thuần nghiệp vụ tính toán điểm môn thường (`Đtbmh`), môn kỹ năng (`skill_score`), điểm trung bình học kỳ (`Đtbhk`), điểm trung bình môn cả năm (`ĐtbmhCN`) và điểm trung bình cả năm (`Đtbcn`).
   - **Part B (Worker Lifecycle)**: Cơ chế worker chạy nền claim task, quản lý lifecycle (`PENDING` -> `RUNNING` -> `SUCCEEDED` / `FAILED`), retry với backoff, giới hạn `max_attempts`, bảo vệ `source_version`, idempotency và API hỗ trợ Giáo vụ yêu cầu chạy lại task lỗi.
2. **Quy tắc tính điểm thuần túy**:
   - Thang điểm 10, làm tròn `HALF_UP` đến 0.1 (`BigDecimal.setScale(1, RoundingMode.HALF_UP)`).
   - Điểm `0.0` là dữ liệu hợp lệ và phải tham gia tính toán.
   - Chỉ ô điểm có `scoreStatus == SCORED` và `scoreValue != null` mới tham gia tử số và mẫu số.
   - Bỏ qua hoàn toàn các ô `NOT_ENTERED`, `ABSENT`, `EXEMPTED`, `CANCELLED` (không tính là điểm 0).
   - Dữ liệu chưa đủ tất cả các cột cấu hình vẫn tính `Đtbmh` từ các cột hợp lệ đã có điểm. Nếu không có ô điểm nào hợp lệ -> trả về `null` (hiển thị "Chưa có dữ liệu").
   - Môn kỹ năng: áp dụng 3 trọng số cấu hình `W_KTTT + W_KTĐK + W_KTCK = 100`.
   - Môn kỹ năng (`SKILL`) không tham gia vào `Đtbhk` và `Đtbcn`.
3. **Nguyên tắc Background Worker & Version Protection**:
   - Tuyệt đối không chạy tính toán điểm trong HTTP request (`NFR-CALC-004`).
   - Worker claim task bằng pessimistic lock / atomic status transition an toàn.
   - Bảng điểm tổng kết (`StudentAnnualTranscript`, `StudentTermTranscript`) chỉ được chuyển sang `FINISH` khi `task.requestedVersion == transcript.sourceVersion`.
   - Nếu dữ liệu nguồn thay đổi trong khi worker đang chạy (`transcript.sourceVersion > task.requestedVersion`), worker ghi nhận kết quả tạm thời nhưng giữ transcript ở trạng thái `IN_PROGRESS`.
   - Task lỗi: ghi `lastError`, tăng `attemptCount`, retry tối đa `maxAttempts = 3` (hoặc theo cấu hình). Khi vượt quá `maxAttempts` -> chuyển `FAILED`. Transcript giữ `IN_PROGRESS`.

---

## 3. Mục tiêu

1. Xây dựng component thuần nghiệp vụ `SubjectScoreCalculator` / `TranscriptCalculationEngine`.
2. Xây dựng dịch vụ điều phối tính toán `TranscriptRecalculationService` thực hiện chuỗi tính toán phụ thuộc cho một học sinh trong một năm học:
   `StudentScore` -> `StudentSubjectTermResult` -> `StudentTermTranscript` (`dtbhk`) -> `StudentSubjectAnnualResult` (`regular_dtbmh_cn`) -> `StudentAnnualTranscript` (`regular_dtbcn`, `final_dtbcn`).
3. Triển khai `CalculationTaskWorker` (chạy định kỳ background) với đầy đủ cơ chế claim, execute, retry, idempotency, version protection.
4. Cung cấp API và Controller cho `ACADEMIC_OFFICE` / `ADMIN`:
   - Xem danh sách calculation task lỗi (`FAILED`).
   - Yêu cầu thử lại (retry) task lỗi.
   - Kích hoạt tính lại thủ công cho một học sinh / năm học.

---

## 4. Phạm vi

### 4.1. In-scope

#### Part A: Pure Calculation Engine
- **`SubjectScoreCalculator`**:
  - `calculateNormalSubjectTermScore(List<AssessmentColumn> columns, List<StudentScore> scores)`:
    - `KTTT`: hệ số 1.
    - `KTĐK`: hệ số 2.
    - `KTCK`: hệ số 3.
    - Công thức: `ROUND(SUM(scoreValue * weight) / SUM(weight), 1)`.
  - `calculateSkillSubjectTermScore(SkillWeightConfig config, List<AssessmentColumn> columns, List<StudentScore> scores)`:
    - Công thức: `ROUND((KTTT * W_KTTT + KTĐK * W_KTĐK + KTCK * W_KTCK) / 100, 1)`.
  - `calculateTermAverage(List<BigDecimal> normalSubjectScores)`:
    - Công thức: `ROUND(SUM(normalSubjectScores) / COUNT(normalSubjectScores), 1)`.
  - `calculateAnnualSubjectScore(BigDecimal hk1Score, BigDecimal hk2Score, boolean isFullYear)`:
    - Môn 2 học kỳ: `ROUND((hk1Score + 2 * hk2Score) / 3, 1)` (nếu cả 2 HK đều có điểm; nếu thiếu 1 HK -> `null`).
    - Môn 1 học kỳ: lấy điểm của học kỳ có giảng dạy.
  - `calculateAnnualAverage(List<BigDecimal> normalAnnualSubjectScores)`:
    - Công thức: `ROUND(SUM(normalAnnualSubjectScores) / COUNT(normalAnnualSubjectScores), 1)`.

#### Part B: Orchestration & Worker Lifecycle
- **`TranscriptRecalculationService`**:
  - Nhận `(studentId, academicYearId, requestedVersion)`.
  - Truy vấn cấu trúc học tập của học sinh: lớp học sinh theo học (`StudentYearEnrollment`), danh sách học kỳ (`Semester`), danh sách môn (`ClassSubject`), sổ điểm (`Scorebook`), cột đánh giá (`AssessmentColumn`), ô điểm (`StudentScore`), cấu hình trọng số (`SkillWeightConfig`).
  - Thực hiện tính toán và upsert bản ghi:
    - `StudentSubjectTermResult`
    - `StudentTermTranscript` (`dtbhk`)
    - `StudentSubjectAnnualResult` (`regular_dtbmh_cn`, `official_dtbmh_cn`, `calculation_source = REGULAR`)
    - `StudentAnnualTranscript` (`regular_dtbcn`, `final_dtbcn`, `result_source = REGULAR`)
  - Thực hiện bảo vệ `source_version` trước khi commit:
    - So sánh `sourceVersion` hiện hành với `requestedVersion`.
    - Cập nhật `calculationStatus = FINISH` nếu khớp; giữ `IN_PROGRESS` nếu có version mới hơn.
- **`CalculationTaskWorker`**:
  - `@Scheduled(fixedDelayString = "${app.calculation.worker-interval-ms:5000}")` hoặc trigger claim task.
  - Claim task theo batch (ưu tiên task có `status = PENDING`, `availableAt <= now()`, sắp xếp `availableAt ASC`).
  - Pessimistic locking / Atomic transition: `status: PENDING -> RUNNING`, `workerId`, `startedAt`, `attemptCount++`.
  - Bọc execution trong khối try-catch an toàn:
    - Thành công -> `status = SUCCEEDED`, `completedAt = now()`.
    - Lỗi -> `lastError`, nếu `attemptCount < maxAttempts` -> `status = PENDING`, `availableAt = now() + backoff`; nếu `attemptCount >= maxAttempts` -> `status = FAILED`.
- **API & Controller cho Giáo vụ**:
   - `GET /api/v2/scorebooks/calculation-tasks?status=FAILED` (phân trang, lọc theo status, studentId, academicYearId).
   - `POST /api/v2/scorebooks/calculation-tasks/{taskId}/retry` (chuyển task `FAILED` về `PENDING`, `attemptCount = 0`, `availableAt = now()`).
   - `POST /api/v2/students/{studentId}/transcripts/recalculate?academicYearId=...` (touch transcript và enqueue recalc task).

#### User-friendly identifier
- API user-facing, filter và response ưu tiên `studentCode`; `studentId` tiếp tục được giữ trong persistence/internal flow.
- Route numeric theo `studentId` vẫn được hỗ trợ để bảo toàn compatibility cho client cũ.

### 4.2. Out-of-scope

- Xử lý điểm thi lại `retake_exam` (thuộc plan Retake Exam riêng; hiện tại `official_dtbmh_cn = regular_dtbmh_cn`, `calculation_source = REGULAR`).
- Giao diện Frontend Vue 3 / Storybook.
- Postman Collection (chỉ tạo khi có yêu cầu).

---

## 5. Thiết kế kỹ thuật chi tiết

### 5.1. Danh sách file thay đổi & tạo mới

```text
BE/BaiTap-RS/
├── src/main/java/com/JavaTraining/BaiTap_RS/scorebook/
│   ├── controller/
│   │   └── [NEW] CalculationTaskController.java
│   ├── domain/DTOs/
│   │   ├── requests/
│   │   │   └── [NEW] ReqFilterCalculationTaskDTO.java
│   │   └── response/
│   │       └── [NEW] ResCalculationTaskDTO.java
│   ├── repository/
│   │   └── [MODIFY] CalculationTaskRepository.java (thêm query findAvailableTasks, findByStatus, findForUpdate)
│   ├── service/
│   │   ├── [NEW] SubjectScoreCalculator.java (Pure business calculation engine)
│   │   ├── [NEW] TranscriptRecalculationService.java (Recalculation orchestration)
│   │   ├── [NEW] CalculationTaskWorker.java (Background task poller & executor)
│   │   ├── [MODIFY] CalculationTaskService.java (Bổ sung claimTask, retryTask, markFailed, markSucceeded)
│   │   └── [MODIFY] TranscriptStateService.java (Bổ sung markTranscriptsFinishIfMatched)
└── src/test/java/com/JavaTraining/BaiTap_RS/scorebook/
    ├── service/
    │   ├── [NEW] SubjectScoreCalculatorTest.java (Unit test các công thức điểm)
    │   ├── [NEW] TranscriptRecalculationServiceTest.java (Unit test orchestration & calculation flow)
    │   └── [NEW] CalculationTaskWorkerTest.java (Unit test worker lifecycle, claim, retry, version protection)
    └── controller/
        └── [NEW] CalculationTaskControllerTest.java (Integration test API retry & query)
```

---

## 6. Kế hoạch Unit Test & Validation

### 6.1. Unit Tests theo `unit-test-plan`

1. **`SubjectScoreCalculatorTest` (Pure Calculation Engine)**:
   - **Môn thông thường (`Đtbmh`)**:
     - Case đầy đủ các cột: 2 KTTT (hệ số 1), 1 KTĐK (hệ số 2), 1 KTCK (hệ số 3) -> kiểm tra công thức tính và làm tròn `HALF_UP` 0.1.
     - Case điểm 0.0 hợp lệ: kiểm tra điểm 0.0 tham gia đúng tử số và mẫu số.
     - Case dữ liệu chưa đủ: chỉ có 1 KTTT và 1 KTĐK, chưa có KTCK -> tính từ 2 cột đã có.
     - Case trạng thái đặc biệt: bỏ qua các ô có trạng thái `NOT_ENTERED`, `ABSENT`, `EXEMPTED`, `CANCELLED`.
     - Case không có ô điểm hợp lệ nào -> trả về `null`.
   - **Môn kỹ năng (`skill_score`)**:
     - Case có cấu hình trọng số (ví dụ: KTTT: 20%, KTĐK: 30%, KTCK: 50%) -> kiểm tra tính đúng và làm tròn 0.1.
     - Case thiếu trọng số cấu hình -> trả về `null` hoặc xử lý an toàn.
   - **Điểm trung bình học kỳ (`Đtbhk`)**:
     - Case nhiều môn ACADEMIC -> trung bình cộng đúng, làm tròn `HALF_UP` 0.1.
     - Case có môn SKILL -> xác nhận môn SKILL không tham gia tính `Đtbhk`.
     - Case không có môn nào có điểm -> trả về `null`.
   - **Điểm môn cả năm (`ĐtbmhCN`)**:
     - Môn học cả năm: có điểm HK1 và HK2 -> `ROUND((HK1 + 2*HK2)/3, 1)`.
     - Môn học cả năm: chỉ mới có điểm HK1 -> trả về `null`.
     - Môn chỉ học 1 học kỳ (HK1 hoặc HK2) -> lấy điểm đúng học kỳ đó.
   - **Điểm trung bình cả năm (`Đtbcn`)**:
     - Trung bình cộng các `regular_dtbmh_cn` hợp lệ, môn SKILL không tham gia.

2. **`TranscriptRecalculationServiceTest`**:
   - Test recalculate toàn bộ bảng điểm cho học sinh: cập nhật đúng `StudentSubjectTermResult`, `StudentTermTranscript`, `StudentSubjectAnnualResult`, `StudentAnnualTranscript`.
   - Test Version Protection:
     - Khớp version (`requestedVersion == sourceVersion`) -> transcript chuyển `FINISH`.
     - Lệch version (`sourceVersion > requestedVersion`) -> transcript giữ `IN_PROGRESS`.

3. **`CalculationTaskWorkerTest`**:
   - Test claim task: `PENDING` -> `RUNNING`, gán `workerId`, `startedAt`, `attemptCount`.
   - Test hoàn thành task: `RUNNING` -> `SUCCEEDED`, `completedAt`.
   - Test retry khi gặp lỗi: `attemptCount < maxAttempts` -> chuyển về `PENDING` với backoff.
   - Test max attempts: `attemptCount >= maxAttempts` -> chuyển `FAILED`, transcript giữ `IN_PROGRESS`.
   - Test idempotency: chạy lại task nhiều lần không làm trùng lặp dữ liệu hay sinh lỗi.

4. **`CalculationTaskControllerTest`**:
   - Test Giáo vụ xem danh sách task lỗi (`403` nếu không có quyền `ADMIN`/`ACADEMIC_OFFICE`).
   - Test retry task `FAILED` -> chuyển về `PENDING` thành công.
   - Test recalculate endpoint cho học sinh.

### 6.2. Backend Verification
- `./gradlew test`
- `./gradlew checkstyleMain checkstyleTest`
- `./gradlew pmdMain pmdTest`
- `./gradlew build -x test`
- Đánh giá JaCoCo coverage cho các service và engine mới.

---

## 7. Dev Note
Sau khi triển khai và kiểm thử hoàn tất, Dev Note sẽ được tạo tại:
`document/dev-note/be/scorebook/042-subject-calculation-engine-and-worker-lifecycle-2026-08-25.md`
