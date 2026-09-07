# Developer Plan 040: Xem xét Plan 27.1 & Thực thi CR-SEM-001 (Incomplete Score Data Notifications)

## 1. Trạng thái và phiên bản áp dụng

- **Status**: `Approved`.
- **Application-document version**: `v2`.
- **Ngày lập plan**: `2026-08-25`.
- **Phê duyệt**: User approved qua agent ngày `2026-08-25`.
- **Module**: Backend `academic` (Semester Notification Flow) + tích hợp `scorebook`, `teacher`, `batch`.
- **Dependencies**:
  - Plan `027` / Dev Note `027.1` (Semester foundation, lifecycle chuẩn hóa `DRAFT -> ACTIVE -> LOCKED -> CLOSED`, khởi tạo `CR-SEM-001`).
  - Plan `036` (Scorebook foundation), Plan `037` (Student score entry), Plan `038` (Score change request).
  - Plan `039` (Semester lock lifecycle, `SemesterCompletenessService`, `SemesterLockRun`, `SemesterLockReport`, daily batch auto-lock).
- **Requirement gate**: Phê duyệt chính thức `CR-SEM-001`, triển khai notification persistence, recipient resolution, mail/in-app dispatch, retry/error isolation và tích hợp scheduler 11 checkpoint.

---

## 2. Bối cảnh & Xem xét Plan 27.1

### 2.1. Đánh giá lại Plan 27.1 (Implementation Note 27.1)
- Trong **Plan 027** (2026-08-21) và **Dev Note 027.1** (2026-08-22):
  - Chuẩn hóa lifecycle 4 trạng thái của học kỳ: `DRAFT -> ACTIVE -> LOCKED -> CLOSED`.
  - Khởi tạo đề xuất thay đổi `CR-SEM-001` (Incomplete Score Data Notifications) mở rộng `BR-SEM-006` thành `BR-SEM-006-01` đến `BR-SEM-006-08`.
  - Tạm hoãn (defer) phần code email delivery, notification persistence, outbox retry, scheduler gửi thông báo sang **Implementation Note 27.1** để chờ chốt module điểm (`scorebook`).
- Trong **Plan 039** (2026-08-25):
  - Đã triển khai thành công logic khóa học kỳ (`SemesterLockService`), lưu trữ lịch sử chạy batch (`SemesterLockRun`), bảng lưu báo cáo completeness (`SemesterLockReport`), cùng engine đánh giá dữ liệu điểm thực tế (`SemesterCompletenessService`) dựa trên sổ điểm, cột điểm, điểm học sinh và đơn sửa điểm pending.
  - Đã cài đặt phương thức đánh giá 11 mốc checkpoint (`-45, -30, -14, -7, -3, -1, 0, 1, 3, 7, 14`).
- **Nhu cầu của Plan 040**:
  - Xem xét và chính thức phê duyệt `CR-SEM-001` (chuyển trạng thái từ `Draft` sang `Approved`, đồng bộ vào `01-AcademicStructureModule.md`).
  - Thực thi toàn diện các yêu cầu còn lại của `CR-SEM-001` và Plan 27.1: lưu trữ notification log/outbox, phân giải người nhận (Giáo vụ, GVCN, GVBM), gửi thông báo (Email / In-app log), xử lý retry, bảo đảm tính idempotent, cách ly lỗi và bảo vệ quyền riêng tư dữ liệu.

---

## 3. Mục tiêu của Plan 040

1. **Phê duyệt và cập nhật tài liệu CR-SEM-001**:
   - Chuyển trạng thái `document/application-doc/v2/change-request/CR-SEM-001-incomplete-score-data-notifications.md` thành `Approved`.
   - Cập nhật tài liệu kiến trúc `document/application-doc/v2/modules/01-AcademicStructureModule.md` để ghi nhận các rule `BR-SEM-006-01` đến `BR-SEM-006-08` đã được thực thi.
2. **Persistence Layer cho Notifications**:
   - Migration Flyway `V15__create_semester_completeness_notification.sql`:
     - Bảng `semester_completeness_notification`: lưu thông báo đã phát sinh/gửi, liên kết với `semester_lock_report`, lưu `checkpoint_code`, `recipient_email`, `recipient_role`, `recipient_teacher_id`, `notification_channel` (`EMAIL`, `IN_APP`), `status` (`PENDING`, `SENT`, `FAILED`), `sent_at`, `error_message`, `attempt_count`.
     - Unique constraint `uk_sem_notif_chk_recip`: bảo đảm tính idempotent per `(semester_id, checkpoint_code, recipient_email, notification_channel)` trong cùng một kỳ đánh giá checkpoint.
3. **Recipient Resolution & Content Generation Engine**:
   - Dịch vụ phân giải người nhận dựa trên phạm vi dữ liệu thiếu:
     - **Giáo vụ (Academic Office / Admin)**: nhận báo cáo tổng thể toàn trường cho học kỳ.
     - **Giáo viên bộ môn (Subject Teacher)**: nhận thông báo về các môn học / lớp học do mình phụ trách chưa hoàn thành nhập điểm hoặc còn đơn sửa điểm pending.
     - **Giáo viên chủ nhiệm (Homeroom Teacher)**: nhận thông báo tổng hợp về tình trạng hoàn thiện điểm của các môn trong lớp mình chủ nhiệm.
   - Bảo đảm `BR-SEM-006-08`: Không để lộ dữ liệu điểm hay danh sách học sinh của lớp/môn ngoài phạm vi phân công của giáo viên.
4. **Notification Dispatcher, Email Service & Outbox Retry**:
   - Dịch vụ gửi email / thông báo `SemesterNotificationDispatchService`:
     - Tích hợp gửi email qua template chuẩn (HTML/Plain text thân thiện, rõ ràng danh sách môn/lớp còn thiếu).
     - Hỗ trợ chế độ Mock/Log khi không cấu hình SMTP và cấu hình gửi thật khi có `JavaMailSender`.
     - Cách ly lỗi (`BR-SEM-006-06`): Lỗi gửi mail không được làm rollback transaction hoặc dừng luồng batch/lock học kỳ.
     - Cơ chế retry (`BR-SEM-006-05`): Lưu lại trạng thái `FAILED` kèm lý do lỗi, hỗ trợ retry có giới hạn (bounded retry) qua API hoặc scheduler.
5. **Tích hợp Checkpoint Evaluation & Scheduler**:
   - Cập nhật Daily Scheduler (`SemesterLockScheduler` & Tasklet) để hàng ngày quét các học kỳ `ACTIVE`:
     - Kiểm tra nếu hôm nay rơi vào bất kỳ mốc nào trong 11 checkpoint (`t-45d`, `t-30d`, `t-14d`, `t-7d`, `t-3d`, `t-1d`, `t`, `t+1d`, `t+3d`, `t+7d`, `t+14d`).
     - Tự động chạy đánh giá completeness -> sinh report -> phân giải người nhận -> gửi thông báo nếu có dữ liệu chưa hoàn chỉnh (`NEEDS_NOTIFICATION`).
     - Bỏ qua gửi thông báo nếu dữ liệu đầy đủ (`NO_NOTIFICATION`) hoặc checkpoint đó đã được thông báo thành công trước đó (idempotent).
6. **REST API Endpoints**:
   - `GET /api/v2/semesters/{semesterId}/notifications`: Xem lịch sử và danh sách thông báo đã gửi của học kỳ (dành cho `ADMIN`, `ACADEMIC_OFFICE`).
   - `POST /api/v2/semesters/{semesterId}/notifications/dispatch`: Kích hoạt gửi thông báo thủ công cho một checkpoint cụ thể (dành cho `ADMIN`, `ACADEMIC_OFFICE`).
   - `POST /api/v2/semesters/{semesterId}/notifications/retry-failed`: Thử gửi lại các thông báo bị `FAILED`.

---

## 4. Chi tiết thay đổi dự kiến

### 4.1. Database Migration (Flyway)
- **`V15__create_semester_completeness_notification.sql`**:
  ```sql
  CREATE TABLE semester_completeness_notification (
      id BIGINT AUTO_INCREMENT PRIMARY KEY,
      semester_id BIGINT NOT NULL,
      report_id BIGINT NULL,
      checkpoint_code VARCHAR(20) NOT NULL,
      recipient_email VARCHAR(150) NOT NULL,
      recipient_role VARCHAR(50) NOT NULL,
      recipient_teacher_id BIGINT NULL,
      notification_channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
      status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
      subject VARCHAR(255) NOT NULL,
      body_content TEXT NOT NULL,
      attempt_count INT NOT NULL DEFAULT 0,
      sent_at TIMESTAMP NULL,
      error_message TEXT NULL,
      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      CONSTRAINT fk_scn_semester FOREIGN KEY (semester_id) REFERENCES semester (id),
      CONSTRAINT fk_scn_report FOREIGN KEY (report_id) REFERENCES semester_lock_report (id),
      CONSTRAINT fk_scn_teacher FOREIGN KEY (recipient_teacher_id) REFERENCES teacher (id),
      CONSTRAINT uk_sem_notif_chk_recip UNIQUE (semester_id, checkpoint_code, recipient_email, notification_channel)
  );
  CREATE INDEX idx_scn_sem_chk ON semester_completeness_notification (semester_id, checkpoint_code);
  CREATE INDEX idx_scn_status ON semester_completeness_notification (status);
  ```

### 4.2. Backend Domain Entities & Enums
- **`NotificationChannel.java`**: `EMAIL`, `IN_APP`.
- **`NotificationStatus.java`**: `PENDING`, `SENT`, `FAILED`.
- **`SemesterCompletenessNotification.java`**: JPA entity tương ứng bảng migration V15.
- **DTOs**:
  - `ResSemesterNotificationDTO.java`: Trả về chi tiết thông báo cho quản trị viên.
  - `ReqDispatchNotificationDTO.java`: Dữ liệu yêu cầu trigger dispatch thủ công.

### 4.3. Repositories
- **`SemesterCompletenessNotificationRepository.java`**:
  - `findBySemesterIdOrderByCreatedAtDesc(...)`
  - `findBySemesterIdAndCheckpointCode(...)`
  - `findByStatusAndAttemptCountLessThan(...)`
  - `existsBySemesterIdAndCheckpointCodeAndRecipientEmailAndNotificationChannel(...)`

### 4.4. Service Layer
- **`SemesterRecipientResolverService.java`**: Phân giải danh sách người nhận và payload cá nhân hóa theo vai trò:
  - Lấy danh sách giáo vụ từ hệ thống `User` có role `ACADEMIC_OFFICE`/`ADMIN`.
  - Phân tích `SemesterCompletenessSummaryDTO` để tìm các lớp/môn thiếu điểm -> tìm `TeacherAssignment` tương ứng -> lấy email của GVBM và GVCN.
- **`SemesterNotificationTemplateService.java`**: Render nội dung thông báo/email theo format chuẩn, liệt kê rõ các đầu mục thiếu:
  - Tên học kỳ, mốc checkpoint.
  - Số lượng sổ điểm chưa nộp/chưa publish.
  - Danh sách môn học / lớp học cụ thể cần bổ sung điểm.
- **`SemesterNotificationDispatchService.java`**:
  - Điều phối gửi thông báo: kiểm tra tính idempotent -> lưu `PENDING` -> dispatch -> cập nhật `SENT` / `FAILED` và `attemptCount`.
  - Xử lý retry cho các bản ghi `FAILED`.
  - Cách ly lỗi (try-catch bao bọc, không throw phá vỡ luồng chính).
- **`SemesterCompletenessService.java`**:
  - Tích hợp gọi `SemesterNotificationDispatchService` sau khi đánh giá completeness và phát hiện `NEEDS_NOTIFICATION`.

### 4.5. Batch & Scheduler Layer
- **`SemesterLockTasklet.java`**:
  - Bổ sung bước kích hoạt thông báo sau khi lưu report completeness nếu checkpoint yêu cầu.
- **`SemesterLockScheduler.java`**:
  - Cập nhật scheduler quét 11 checkpoint cho toàn bộ các học kỳ đang `ACTIVE` để gửi thông báo kịp thời.

### 4.6. Controller & Security
- **`SemesterNotificationController.java`** (hoặc mở rộng `SemesterController.java`):
  - Endpoints phân quyền rõ ràng với `@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")`.

---

### 4.7. Đặc tả Checkpoint Code (Encoding & Decoding Specification)
- **Xác định Effective Lock Date ($T$)**:
  - Ưu tiên 1: `reopenUntil` (nếu học kỳ đang mở lại).
  - Ưu tiên 2: `automaticLockAt` (ngày hẹn khóa tự động đã cấu hình).
  - Ưu tiên 3: `endDate + 45 ngày` (fallback mặc định theo quy chế).
- **Tính toán Offset & Quy tắc Encode**:
  - $\Delta = \text{ChronoUnit.DAYS.between}(T, \text{checkpointDate})$.
  - $\Delta = 0 \implies \mathbf{"t"}$.
  - $\Delta < 0 \implies \mathbf{"t" + \Delta + "d"}$ (ví dụ: $-7 \rightarrow \mathbf{"t-7d"}$).
  - $\Delta > 0 \implies \mathbf{"t+" + \Delta + "d"}$ (ví dụ: $+3 \rightarrow \mathbf{"t+3d"}$).
- **Tập 11 Checkpoint Offsets chuẩn**: `{-45, -30, -14, -7, -3, -1, 0, 1, 3, 7, 14}`.
- **Quy tắc Decode**: Tách bỏ tiền tố `t` và hậu tố `d` để thu lại số ngày offset ($\Delta$).

---

## 5. Kế hoạch kiểm thử & Đảm bảo chất lượng (Verification Plan)

### 5.1. Unit Tests
- `SemesterRecipientResolverServiceTest`: Kiểm tra logic phân giải đúng GVBM, GVCN theo lớp/môn thiếu điểm, không rò rỉ dữ liệu ngoài scope (`BR-SEM-006-08`).
- `SemesterNotificationDispatchServiceTest`:
  - Kiểm tra idempotency (không gửi trùng lặp email khi chạy checkpoint lần 2).
  - Kiểm tra xử lý lỗi khi gửi email thất bại (lưu `FAILED`, tăng attempt count, không ném exception ra ngoài).
  - Kiểm tra retry bounded (tối đa 3 lần).
- `SemesterCompletenessNotificationIntegrationTest`: Kiểm tra toàn bộ luồng từ evaluate completeness -> dispatch -> lưu database.

### 5.2. Code Quality & Build Checks
- `./gradlew checkstyleMain checkstyleTest` (0 lỗi, chuẩn format import, javadoc, độ dài dòng).
- `./gradlew pmdMain pmdTest` (0 vi phạm PMD).
- `./gradlew test jacocoTestReport` (100% tests pass, line coverage >= 80% cho các class mới).
- `./gradlew build` (Build thành công toàn bộ backend).

---

## 6. Danh mục tài liệu cập nhật
- `document/application-doc/v2/change-request/CR-SEM-001-incomplete-score-data-notifications.md` (Chuyển status sang `Approved`).
- `document/application-doc/v2/modules/01-AcademicStructureModule.md` (Cập nhật `BR-SEM-006` và các rule `BR-SEM-006-01`..`08`).
- `document/dev-impl-plan/be/academic/040-semester-completeness-notification-cr-sem-001-2026-08-25.md` (Lưu file plan chính thức).
- `document/dev-note/be/academic/040-semester-completeness-notification-cr-sem-001-2026-08-25.md` (Tạo Dev Note sau khi triển khai).

---

## 7. Tóm tắt kết quả triển khai (Plan Summary)
- Đã xem xét và hoàn thành toàn diện các hạng mục trì hoãn từ **Plan 27.1**.
- Phê duyệt và chuyển đổi trạng thái Change Request **CR-SEM-001** sang `Approved`.
- Hoàn thành toàn bộ mã nguồn backend, migration Flyway V15, dịch vụ phân giải người nhận, dịch vụ gửi thông báo qua Spring Mail kèm cơ chế idempotent, retry và cách ly lỗi.
- Đảm bảo 100% kiểm tra chất lượng mã nguồn: Checkstyle PASS (0 lỗi), PMD PASS (0 vi phạm), Unit/Integration Tests PASS (200/200 tests), Coverage cao (> 90%), Build artifact PASS.

## 8. Amendment 040.1 — Email-only contract (2026-09-03)

Plan 040.1 đã được user phê duyệt để chuẩn hóa contract thành email-only. `IN_APP` không còn là channel hợp lệ; `checkpointCode` vẫn được giữ trong persistence để phục vụ idempotency nhưng không phải UI concern. Việc gửi chỉ được đánh dấu `SENT` khi `JavaMailSender` trả về thành công; không xác nhận việc người nhận đã nhận email. Xem [Developer Plan 040.1](040.1-email-only-notification-contract-2026-09-03.md).


