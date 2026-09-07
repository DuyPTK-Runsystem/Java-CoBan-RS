# Developer Plan 039: Semester Lock

## 1. Trạng thái và phiên bản áp dụng

- Status: `Approved`.
- Application-document version: `v2`.
- Ngày lập plan: `2026-08-25`.
- Phê duyệt: user approved qua agent ngày `2026-08-25`.
- Module: Backend `academic` + integration với `scorebook`/calculation.
- Dependencies: Plan `027` (semester foundation), Plan `036` (scorebook foundation),
  Plan `037` (student score entry), Plan `038` (score change request).
- Requirement gate: áp dụng các mốc notification/checkpoint theo `CR-SEM-001`; email,
  SMTP và notification delivery vẫn để scope riêng.

### 1.1. Quyết định đã chốt cho Plan 039

- `automaticLockAt` mặc định là `endDate + 45 ngày`; nếu có input thì không được vượt quá
  mốc này và không sớm hơn `endDate`.
- `lockedAt` lưu thời điểm lock thực tế; không dùng nó để ghi đè thời điểm automatic lock
  dự kiến.
- Reopen chuyển semester về `ACTIVE` trong tối đa 3 ngày; lưu mốc `reopenUntil = reopenedAt
  + 3 ngày`. Scheduler sẽ lock lại khi mốc này tới hạn và không ghi đè `automaticLockAt` ban đầu.
- Dùng đúng 11 checkpoint trong `CR-SEM-001`:
  `t-45d`, `t-30d`, `t-14d`, `t-7d`, `t-3d`, `t-1d`, `t`, `t+1d`, `t+3d`, `t+7d`, `t+14d`.
- Report theo cách A: mỗi semester được xử lý trong một lần chạy tạo một report riêng.
- Scheduler chỉ trigger Spring Batch lúc `02:00` mỗi ngày theo timezone `Asia/Ho_Chi_Minh`;
  Spring Batch quản lý Job/Step execution, restart và trạng thái failure.
- Manual lock và automatic lock đi qua cùng một lock service, cùng pessimistic lock trên
  row `semester`, cùng audit flow và cùng calculation-task queue.
- Calculation task áp dụng cho toàn bộ học sinh `ACTIVE` thuộc semester, không chỉ học sinh
  đã có transcript.

## 2. Mục tiêu

Hoàn thiện lifecycle khóa học kỳ theo v2, gồm:

- Khóa thủ công bởi `ADMIN`/`ACADEMIC_OFFICE` với cùng một flow nghiệp vụ dùng cho khóa tự động.
- Tự động khóa học kỳ `ACTIVE` khi tới thời điểm khóa hiệu lực, có scheduler chạy theo
  `Asia/Ho_Chi_Minh` và có tính idempotent.
- Tạo báo cáo dữ liệu điểm chưa hoàn chỉnh sau quyết định khóa; dữ liệu thiếu không chặn
  hoặc rollback việc chuyển sang `LOCKED`.
- Mỗi semester được xử lý trong mỗi batch run có một report riêng; restart cùng job execution
  phải resume report item hiện tại, còn daily run mới tạo report attempt mới.
- Tạo/gộp calculation task và đánh dấu transcript bị ảnh hưởng là `IN_PROGRESS` trong
  transaction của thao tác khóa/mở khóa.
- Giữ đúng guard hiện có: giáo viên không được nhập/sửa điểm trực tiếp khi học kỳ là
  `LOCKED` hoặc `CLOSED`; sửa sau khóa đi qua Score Change Request.

## 3. Requirement liên quan

### Functional requirements

- `FR-SEM-004`: Giáo vụ xác nhận khóa học kỳ.
- `FR-SEM-005`: Hệ thống tự động khóa học kỳ.
- `FR-SEM-006`: Xem mức độ hoàn thành nhập điểm.
- `FR-SEM-007`: Người có thẩm quyền mở lại học kỳ.
- `FR-SEM-008`: Lưu lịch sử khóa và mở khóa.
- `FR-SEM-009`: Hiển thị báo cáo completeness trước khi xác nhận khóa.
- `FR-CALC-001`, `FR-CALC-002`: Đánh dấu tổng kết và tạo calculation task khi khóa/mở khóa.

### Business rules và acceptance

- `BR-SEM-004` đến `BR-SEM-009`: điều kiện khóa, khóa nền, dữ liệu thiếu không chặn,
  không sửa trực tiếp sau khóa và audit khi mở lại.
- `BR-SEM-006-01` đến `BR-SEM-006-08` trong `CR-SEM-001`: evaluation theo checkpoint,
  kết quả rỗng hợp lệ, idempotency, failure isolation, audit metadata và authorization scope.
- `BR-SEM-010` đến `BR-SEM-015`: report không chặn khóa, task cuối kỳ, transcript
  `IN_PROGRESS`, request sửa điểm sau khóa và chỉ worker mới chuyển sang `FINISH`.
- `BR-CALC-010`: khóa hoặc mở học kỳ kích hoạt tính lại.
- `AC-08`: sửa điểm sau khi khóa phải qua request.
- `AC-14`: đã qua 45 ngày từ ngày kết thúc, scheduler khóa học kỳ và tạo report.
- `NFR-AUDITABILITY-*`, `NFR-CALC-005` đến `NFR-CALC-008` và `NFR-CALC-014` đến
  `NFR-CALC-016`.

## 4. Hiện trạng và khoảng thiếu

- `Semester` đã có `automaticLockAt`, `status`, `lockedAt`, `lockedBy`, `lockReason`;
  `SemesterService.lockSemester()` hiện chỉ khóa thủ công và ghi audit.
- `SemesterController` đã có `POST /api/v2/semesters/{semesterId}/lock`, `reopen` và
  checkpoint decision; chưa có endpoint report completeness thực tế.
- `evaluateCompletenessCheckpoint()` hiện chỉ tính checkpoint notification decision,
  chưa truy vấn cột điểm/ô điểm/request để tạo báo cáo.
- Chưa có scheduler tự động tìm các học kỳ đến hạn và thực hiện lock idempotent.
- `CalculationTaskService` hiện đã gộp task theo học sinh/năm học; cần thêm orchestration
  theo tập học sinh của học kỳ khi lock/reopen.
- `ScoreEntryContext` và `ScoreEntryValidator` đã chặn nhập/sửa trực tiếp với `LOCKED`/
  `CLOSED`; Plan 039 chỉ bổ sung test/integration guard nếu không cần đổi behavior.
- `CR-SEM-001` yêu cầu persistence/idempotency/retry contract cho evaluation/notification,
  nhưng Plan 027 mới chỉ triển khai decision output và không có scheduler/query.

## 5. Phạm vi

### 5.1. In-scope

#### A. Lock orchestration

- Tách một service/use case dùng chung cho manual lock và automatic lock.
- Validate lifecycle: chỉ `ACTIVE` được chuyển sang `LOCKED`; thao tác lặp phải an toàn,
  không ghi audit/task/report trùng.
- Ghi `lockedAt`, `lockedBy` cho manual lock; automatic lock dùng actor hệ thống/null actor
  theo audit convention hiện có và ghi rõ source `MANUAL`/`AUTOMATIC` trong audit payload.
- Không để completeness report chặn hoặc rollback lifecycle transition.
- Giữ `CLOSED` là trạng thái chỉ đọc; không thêm use case mở lại `CLOSED`.
- Reopen chỉ áp dụng cho `LOCKED`, bắt buộc reason, actor, timestamp và audit; không làm mất
  lịch sử lock trước đó. Reopen tạo `reopenUntil` sau 3 ngày để scheduler có thể lock lại.

#### B. Effective automatic-lock time và scheduler

- Chuẩn hóa cách tính thời điểm khóa hiệu lực:
  - ưu tiên `automaticLockAt` nếu đã cấu hình;
  - nếu không có, dùng mốc 45 ngày dương lịch sau `endDate` theo timezone nghiệp vụ;
  - nếu semester đang ở cửa sổ reopen, ưu tiên `reopenUntil = reopenedAt + 3 ngày`.
- Một trigger chạy lúc `02:00` mỗi ngày với cron `0 0 2 * * *`, timezone
  `Asia/Ho_Chi_Minh`, chỉ có nhiệm vụ launch Spring Batch job.
- Spring Batch `JobRepository` quản lý `JobExecution`/`StepExecution`, restart và failure;
  business report/run state được lưu riêng để retry các item lỗi vào ngày kế tiếp.
- Query lấy semester có checkpoint của CR đến hạn trong ngày hoặc report item `FAILED` đủ
  điều kiện retry; từ checkpoint `t` trở đi, semester `LOCKED` vẫn được evaluate các mốc còn lại.
- Tại checkpoint `t`, nếu semester còn `ACTIVE` thì batch gọi lock service; nếu đã `LOCKED`
  thì chỉ evaluate report. Mỗi `{batch_run, semester, checkpoint}` tạo một report riêng.
- Retry cùng job execution không tạo report item thứ hai, nhưng daily run mới tạo report attempt
  mới cho checkpoint được xử lý.

#### C. Completeness report/evaluation

- Tạo `SemesterCompletenessService` thuộc boundary Assessment/Scoring để làm source-of-truth
  query cho một học kỳ.
- Report tối thiểu bao phủ các nhóm đã nêu trong v2:
  - thiếu cấu hình cột `KTĐK`;
  - không có đúng một cột `KTCK`;
  - môn kỹ năng thiếu đủ ba cột;
  - học sinh có ô điểm chưa nhập;
  - học sinh chưa có dữ liệu điểm;
  - sổ điểm chưa công bố;
  - score-change request đang `PENDING`.
- Kết quả phải có semester, checkpoint, thời điểm đánh giá, scope, completeness status,
  summary counts và danh sách chi tiết phù hợp với quyền người xem.
- Kết quả rỗng vẫn được ghi nhận là evaluation thành công/complete; không tạo false
  `NEEDS_NOTIFICATION`.
- Manual lock có endpoint preview để Giáo vụ xem report trước khi xác nhận; sau lock tạo một
  event report riêng cho thao tác manual lock, không thay thế các checkpoint notification của CR.
- Evaluation được chạy sau lock decision. Nếu query/persistence report lỗi, lưu failure
  context khi có thể và giữ nguyên `LOCKED`.
- Implement persistence cho batch run và report attempt:
  - run lưu business date, start/end time, Spring Batch execution id và status;
  - report lưu `run_id`, `semester_id`, checkpoint, status `COMPLETE`/`INCOMPLETE`/`FAILED`,
    evaluated time, summary, failure context và correlation/audit metadata;
  - unique `{run_id, semester_id, checkpoint_code}` để restart không tạo duplicate item;
  - một logical latest-evaluation lookup theo `{semester_id, checkpoint_code}` để giữ
    idempotency/tra cứu report hiện hành theo CR, trong khi vẫn giữ lịch sử report mỗi run.

#### D. Calculation integration

- Trong transaction lock/reopen, xác định toàn bộ học sinh `ACTIVE` thuộc enrollment/lớp
  của học kỳ và gọi `TranscriptStateService` để tạo/tăng `source_version`, đặt transcript
  `IN_PROGRESS`.
- Bổ sung API/service bulk cho `CalculationTaskService` để ensure/gộp task theo cơ chế
  idempotency hiện có; không tính điểm trong HTTP request hoặc scheduler.
- Không triển khai calculation worker/công thức `Đtbmh`, `Đtbhk`, `Đtbcn` trong Plan 039.

#### E. API và audit

- Giữ backward-compatible endpoint lock/reopen hiện có dưới `/api/v2/semesters/**`.
- Thêm endpoint office-scope để preview/latest completeness report theo semester và
  checkpoint; response không vượt quá phạm vi dữ liệu của actor.
- Chuẩn hóa audit action cho manual lock, automatic lock, reopen và completeness evaluation;
  audit chứa before/after, source, checkpoint, actor/system, timestamp và correlation id nếu có.

### 5.2. Out-of-scope

- Frontend Vue/PrimeVue, Storybook và Postman collection.
- Email provider, SMTP configuration, template, recipient delivery, outbox delivery worker,
  retry/backoff/dead-letter notification workflow. Đây là scope notification riêng sau khi
  các open decisions của `CR-SEM-001` được phê duyệt.
- Background calculation worker và toàn bộ công thức điểm trung bình.
- Tự động chuyển `LOCKED` sang `CLOSED`.
- Mở lại `CLOSED`, thay đổi role/JWT contract hoặc mở rộng authorization ngoài ma trận v2.
- Retake, transcript business rules chưa có CR riêng.
- Refactor diện rộng các module scorebook/academic không cần cho lock flow.

## 6. Thiết kế kỹ thuật dự kiến

### 6.1. Luồng manual lock

1. Controller kiểm tra `ADMIN`/`ACADEMIC_OFFICE` và nhận `semesterId`.
2. Gọi chung `SemesterLockService` với source `MANUAL`.
3. Service lấy row `semester` bằng pessimistic lock, đọc lại status và kiểm tra `ACTIVE`.
4. Chuyển semester sang `LOCKED`, ghi metadata và audit source `MANUAL`.
5. Xác định học sinh bị ảnh hưởng, touch transcript và enqueue calculation tasks qua queue
   dùng chung với automatic lock.
6. Commit lifecycle/audit/transcript/task cùng transaction.
7. Sau quyết định lock, evaluation service tạo report item cho checkpoint lock.
   Failure của bước report không mở khóa hoặc rollback semester.

### 6.2. Luồng automatic lock

1. Trigger `02:00` launch Spring Batch job.
2. Batch reader lấy các semester có checkpoint CR đến hạn, các semester `ACTIVE` tới mốc `t`,
   và report item cần retry.
3. Với checkpoint `t`, gọi cùng `SemesterLockService` với source `AUTOMATIC` và system actor.
4. Service dùng cùng pessimistic lock và calculation-task queue với manual lock.
5. Evaluate checkpoint tương ứng và tạo report item có key của batch run; checkpoint `t` chỉ
   lock khi row semester sau pessimistic lock vẫn là `ACTIVE` và đã tới hạn.
6. Ghi failure vào batch/report state và tiếp tục semester/checkpoint kế tiếp nếu một item
   độc lập lỗi.

### 6.3. Luồng reopen

1. Chỉ actor có quyền office và semester `LOCKED` được reopen.
2. Validate reason không rỗng; chuyển về `ACTIVE`, đặt `reopenUntil = now + 3 ngày`, giữ
   lịch sử lock trong audit và không thay đổi `automaticLockAt` ban đầu.
3. Touch transcript của toàn bộ học sinh `ACTIVE` thuộc semester và ensure calculation task
   trong cùng transaction.
4. Ghi audit `SEMESTER_REOPENED`; không xóa report/evaluation đã có.

### 6.4. Persistence đề xuất

Tạo migration kế tiếp `V14__create_semester_lock_run_and_report.sql` với bảng mới,
không sửa dữ liệu lịch sử của `semester`:

```text
semester_lock_run
-----------------
run_id             BIGINT PK
business_date      DATE NOT NULL
batch_execution_id BIGINT NULL
status             RUNNING | SUCCEEDED | FAILED
started_at         DATETIME NOT NULL
finished_at        DATETIME NULL
last_error         VARCHAR(2000) NULL

semester_lock_report
--------------------
report_id           BIGINT PK
run_id              BIGINT FK NOT NULL
semester_id        BIGINT FK NOT NULL
checkpoint_code     VARCHAR(30) NOT NULL
report_status       COMPLETE | INCOMPLETE | FAILED
evaluated_at        DATETIME NOT NULL
scope_type          VARCHAR(30) NOT NULL
summary_payload     JSON/TEXT NOT NULL
failure_reason      VARCHAR(2000) NULL
correlation_id      VARCHAR(100) NULL
created_at          DATETIME NOT NULL
updated_at          DATETIME NOT NULL
UQ run_id + semester_id + checkpoint_code
```

Migration cũng bổ sung `semester.reopen_until TIMESTAMP NULL` để biểu diễn cửa sổ `ACTIVE`
ba ngày sau reopen. Mốc này không thay thế `automatic_lock_at`; scheduler dùng deadline
automatic ban đầu khi chưa reopen và dùng `reopen_until` khi semester đã được reopen.

Tên cột/status cuối cùng phải phù hợp MySQL/H2 compatibility và convention migration hiện có.
Student-level detail chỉ được trả qua service/controller đã kiểm tra quyền; summary persistence
không được trở thành kênh vượt authorization.

### 6.5. API contract dự kiến

| Method | Endpoint                                             | Quyền                      | Mục đích                                           |
| ------ | ---------------------------------------------------- | -------------------------- | -------------------------------------------------- |
| `GET`  | `/api/v2/semesters/{semesterId}/completeness-report` | `ADMIN`, `ACADEMIC_OFFICE` | Preview/latest report theo checkpoint              |
| `POST` | `/api/v2/semesters/{semesterId}/lock`                | `ADMIN`, `ACADEMIC_OFFICE` | Xác nhận lock thủ công, giữ response compatibility |
| `POST` | `/api/v2/semesters/{semesterId}/reopen`              | `ADMIN`, `ACADEMIC_OFFICE` | Mở lại `LOCKED` với reason                         |

Query parameter `checkpointCode` và response shape phải được chốt cùng `CR-SEM-001` trước
khi code; không expose report detail cho `TEACHER`/`STUDENT` nếu chưa có scope contract.

## 7. File/khu vực dự kiến thay đổi

### Backend source

- `[MODIFY]` `academic/service/SemesterService.java` — delegate lock/reopen về orchestration,
  effective lock time và lifecycle/audit consistency.
- `[MODIFY]` `academic/controller/SemesterController.java` — report endpoint và contract lock.
- `[MODIFY]` `academic/repository/SemesterRepository.java` — query semester đến hạn, nếu cần.
- `[NEW/MODIFY]` `academic/service/SemesterLockService.java` — manual/automatic lock use case,
  transaction và concurrency boundary.
- `[NEW]` `academic/service/SemesterLockScheduler.java` — scheduled automatic lock.
- `[NEW]` `academic/service/SemesterCompletenessService.java` — report orchestration.
- `[NEW/MODIFY]` `academic/domain/entity/SemesterCompletenessEvaluation*.java` — entity/status.
- `[NEW]` DTO request/response cho report/lock result nếu contract được chốt.
- `[NEW/MODIFY]` repository/query classes trong `scorebook` cho authoritative completeness
  queries; chỉ thêm query cần thiết, không chuyển logic tính điểm vào HTTP.
- `[MODIFY]` `scorebook/service/CalculationTaskService.java` và có thể
  `TranscriptStateService.java` — bulk ensure/touch cho semester lock/reopen.
- `[MODIFY]` audit mapper/service nếu cần thêm action/payload chuẩn hóa.

### Database và tài liệu

- `[NEW]` `BE/BaiTap-RS/src/main/resources/db/migration/V14__create_semester_lock_run_and_report.sql`
  và bổ sung column `semester.reopen_until`.
- `[NEW]` unit/integration tests cho academic lock, scheduler, report, migration và score-entry guard.
- `[UPDATE]` `document/application-doc/v2/...` chỉ khi CR-SEM-001 được approve và contract/schema
  chính thức thay đổi; không tự cập nhật baseline đang có TBD.
- `[NEW AFTER IMPLEMENTATION]` `document/dev-note/be/academic/039-semester-lock-2026-08-25.md`
  ghi nhận implementation thực tế, validation và known issues.

## 8. Kế hoạch test và validation

### Unit test

- Manual lock thành công từ `ACTIVE`; reject `DRAFT`, `LOCKED`, `CLOSED`.
- Automatic lock đúng boundary `automaticLockAt`, fallback 45 ngày và timezone.
- Scheduler trigger đúng `02:00`, xử lý semester đến hạn, không xử lý `DRAFT`/`CLOSED`.
- Restart cùng Spring Batch execution không tạo duplicate report item; daily run mới tạo
  report attempt mới theo quyết định cách A.
- Manual và automatic lock dùng cùng audit/task behavior.
- Completeness report bao phủ từng nhóm thiếu dữ liệu và kết quả empty/complete.
- Evaluation `{semester, checkpoint}` idempotent; failure được lưu/không đổi lifecycle.
- Reopen cần reason, chỉ mở `LOCKED`, tạo cửa sổ 3 ngày, ghi audit và trigger recalculation
  cho toàn bộ học sinh `ACTIVE` thuộc semester.
- Bulk task gộp đúng idempotency key; không tính điểm đồng bộ.

### Integration/migration test

- Flyway V14 tạo đúng bảng run/report, FK, unique và tương thích H2/MySQL test setup.
- End-to-end manual lock: semester `LOCKED`, audit tồn tại, transcript `IN_PROGRESS`, task được tạo.
- End-to-end automatic lock sau 45 ngày: report evaluation được lưu, không tạo duplicate khi chạy lại.
- Dữ liệu thiếu không ngăn lock; report vẫn có summary/failure status phù hợp.
- Sau lock, score entry trả `409`; score-change request vẫn có thể đi qua flow đã có.
- Reopen `LOCKED` -> `ACTIVE` trong 3 ngày, audit/recalculation được tạo; scheduler lock lại
  khi `reopenUntil` tới hạn; không reopen `CLOSED`.
- Authorization: chỉ office lock/reopen/report; actor ngoài scope nhận `403`.

### Validation sau implementation

- `./gradlew test`
- `./gradlew checkstyleMain checkstyleTest`
- `./gradlew pmdMain pmdTest`
- `./gradlew build -x test`
- JaCoCo coverage cho service/report/scheduler branch quan trọng.
- `git diff --check` và migration/schema test.

Validation Result và Dev Note phải được ghi theo backend workflow sau khi implementation hoàn tất.

## 9. Rủi ro, assumption và gate cần xác nhận

- **CR gate:** `CR-SEM-001` đang Draft; Plan 039 áp dụng 11 checkpoint của CR, nhưng CR vẫn
  cần được formal approve trước khi code phần notification contract.
- **Thời điểm 45 ngày:** plan đề xuất `endDate.plusDays(45)` theo `Asia/Ho_Chi_Minh` khi
  `automaticLockAt` null; cần xác nhận cách quy đổi mốc ngày sang thời điểm scheduler.
- **Report persistence:** mỗi `{batch_run, semester, checkpoint}` là một report attempt; latest
  evaluation vẫn cần lookup logical theo `{semester, checkpoint}` để không mất idempotency.
- **Calculation scope:** plan enqueue task cho toàn bộ học sinh `ACTIVE` thuộc semester; worker/
  công thức vẫn là task riêng.
- **Concurrency:** manual lock và scheduler dùng chung `SemesterLockService`, pessimistic row lock
  trên `semester` và calculation-task queue; cần test lock-vs-lock, lock-vs-score-entry,
  lock-vs-reopen, reopen-expiry-vs-auto-lock và restart-vs-retry.
- **Notification:** không gửi email/SMTP trong Plan 039; notification delivery/outbox/retry
  cần plan được approve riêng.

## 10. Output mong đợi

- Manual và automatic semester lock có cùng lifecycle, audit, transcript/task behavior.
- Scheduler tự động khóa đúng hạn, idempotent và không khóa nhầm trạng thái.
- Giáo vụ xem được completeness report trước/sau lock; report không chặn lock.
- Giáo viên bị chặn sửa trực tiếp sau lock và vẫn phải dùng Score Change Request.
- Evaluation/report persistence, schema, API và test evidence đủ để làm nền cho notification
  workflow về sau.
