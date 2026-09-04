# Dev Note: Kế hoạch 040 - Triển khai Semester Completeness Notification (CR-SEM-001)

- **Kế hoạch liên quan**: [`document/dev-impl-plan/be/academic/040-semester-completeness-notification-cr-sem-001-2026-08-25.md`](../../../../dev-impl-plan/be/academic/040-semester-completeness-notification-cr-sem-001-2026-08-25.md)
- **Change Request liên quan**: [`document/application-doc/v2/change-request/CR-SEM-001-incomplete-score-data-notifications.md`](../../../../application-doc/v2/change-request/CR-SEM-001-incomplete-score-data-notifications.md)
- **Trạng thái phê duyệt**: `Approved` (2026-08-25)
- **Ngày thực hiện**: 2026-08-25

---

## 1. Phạm vi thực tế hoàn thành

1. **Cấu hình Dependencies & Spring Mail**:
   - Thêm `spring-boot-starter-mail` vào `BE/BaiTap-RS/build.gradle.kts`.
   - Bổ sung cấu hình Spring Mail trong `BE/BaiTap-RS/src/main/resources/application.properties` (sử dụng biến môi trường chuẩn `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, không lưu cứng bí mật trong code).
2. **Flyway Migration `V15__create_semester_completeness_notification.sql`**:
   - Tạo bảng `semester_completeness_notification` quản lý thông báo nhắc nhở dữ liệu điểm theo mốc checkpoint.
   - Khóa ngoại liên kết `semester (semester_id)`, `semester_lock_report (report_id)`, `teacher (teacher_id)`.
   - Ràng buộc kiểm tra `ck_scn_channel (EMAIL, IN_APP)`, `ck_scn_status (PENDING, SENT, FAILED)`.
   - Ràng buộc duy nhất `uk_sem_notif_chk_recip (semester_id, checkpoint_code, recipient_email, notification_channel)` đảm bảo tính idempotent.
   - Chỉ mục tối ưu `idx_scn_sem_chk` và `idx_scn_status`.
3. **Domain Model & Entities**:
   - Tạo enum `NotificationChannel` (`EMAIL`, `IN_APP`).
   - Tạo enum `NotificationStatus` (`PENDING`, `SENT`, `FAILED`).
   - Tạo entity JPA `SemesterCompletenessNotification`.
   - Tạo DTOs: `ReqDispatchNotificationDTO`, `ResSemesterNotificationDTO`, `SemesterRecipientInfo`, `ClassSubjectIncompleteDetail`.
4. **Repository Layer**:
   - Tạo `SemesterCompletenessNotificationRepository`: tìm kiếm theo semester, checkpoint, status, attemptCount, kiểm tra tồn tại và idempotent.
   - Cập nhật `UserRepository`: thêm `findAcademicOfficeAndAdminUsers()` để truy vấn người dùng vai trò Giáo vụ và Admin.
5. **Service Layer**:
   - `SemesterNotificationTemplateService`: sinh tiêu đề & nội dung email tùy biến chuyên biệt cho từng vai trò (Ban Giám hiệu/Phòng Giáo vụ, Giáo viên bộ môn, Giáo viên chủ nhiệm).
   - `SemesterRecipientResolverService`: giải thuật phân giải người nhận, phân quyền và cách ly scope cảnh báo đúng đối tượng theo `BR-SEM-006-08`.
   - `SemesterNotificationDispatchService`: điều phối gửi thông báo qua `JavaMailSender`, kiểm tra idempotent, xử lý bắt lỗi cách ly độc lập từng email theo `BR-SEM-006-06`, cập nhật trạng thái `SENT`/`FAILED`, đếm số lần attempt, hỗ trợ retry.
   - Cập nhật `SemesterCompletenessService`: trích xuất danh sách lỗi chi tiết theo lớp môn (`evaluateIncompleteClassSubjectDetails`), tự động kích hoạt dispatch notification khi phát hiện `INCOMPLETE` trong `evaluateAndSaveReport`, bổ sung các API nghiệp vụ `dispatchCheckpointNotifications`, `getNotificationsForSemester`, `retryFailedNotifications`.
6. **REST API Controller**:
   - Cập nhật `SemesterController`:
     - `GET /api/v2/semesters/{semesterId}/notifications`: Danh sách thông báo đã gửi/thất bại của học kỳ (bảo vệ quyền `ADMIN`, `ACADEMIC_OFFICE`).
     - `POST /api/v2/semesters/{semesterId}/notifications/dispatch`: Kích hoạt gửi thông báo thủ công theo checkpoint (bảo vệ quyền `ADMIN`, `ACADEMIC_OFFICE`).
     - `POST /api/v2/semesters/{semesterId}/notifications/retry-failed`: Thử gửi lại các thông báo thất bại (bảo vệ quyền `ADMIN`, `ACADEMIC_OFFICE`).
7. **Unit & Integration Tests**:
   - `SemesterCompletenessNotificationFlywayMigrationTest`: xác minh schema V15 trên cơ sở dữ liệu H2.
   - `SemesterNotificationTemplateServiceTest`: kiểm tra tạo template email.
   - `SemesterRecipientResolverServiceTest`: kiểm tra phân giải người nhận và cách ly dữ liệu.
   - `SemesterNotificationDispatchServiceTest`: kiểm tra gửi email, idempotency, retry và bắt lỗi cách ly.
   - `SemesterCompletenessServiceTest`: kiểm tra tích hợp tự động dispatch notification khi report incomplete.
   - `SemesterLockAuthorizationIntegrationTest`: kiểm tra phân quyền bảo vệ các endpoint notification.

---

## 2. Danh sách file thay đổi

### Flyway Migration
- `BE/BaiTap-RS/src/main/resources/db/migration/V15__create_semester_completeness_notification.sql`

### Build & Configuration
- `BE/BaiTap-RS/build.gradle.kts`
- `BE/BaiTap-RS/src/main/resources/application.properties`

### Domain, Enums & DTOs
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/entity/NotificationChannel.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/entity/NotificationStatus.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/entity/SemesterCompletenessNotification.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/DTOs/requests/ReqDispatchNotificationDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/DTOs/response/ResSemesterNotificationDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/DTOs/response/SemesterRecipientInfo.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/domain/DTOs/response/ClassSubjectIncompleteDetail.java`

### Repositories
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/repository/SemesterCompletenessNotificationRepository.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/repository/UserRepository.java`

### Services
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/service/SemesterNotificationTemplateService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/service/SemesterRecipientResolverService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/service/SemesterNotificationDispatchService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/service/SemesterCompletenessService.java`

### Controllers
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/academic/controller/SemesterController.java`

### Tests
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/config/SemesterCompletenessNotificationFlywayMigrationTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/academic/service/SemesterNotificationTemplateServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/academic/service/SemesterRecipientResolverServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/academic/service/SemesterNotificationDispatchServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/academic/service/SemesterCompletenessServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/academic/controller/SemesterLockAuthorizationIntegrationTest.java`

---

## 3. Kết quả Validation

| Công cụ / Lệnh                            | Trạng thái | Ghi chú                                                                       |
| ----------------------------------------- | ---------- | ----------------------------------------------------------------------------- |
| `./gradlew checkstyleMain checkstyleTest` | **PASS**   | 0 warnings, tuân thủ quy tắc checkstyle                                       |
| `./gradlew pmdMain pmdTest`               | **PASS**   | 0 warnings, tuân thủ các quy tắc PMD                                          |
| `./gradlew test jacocoTestReport`         | **PASS**   | 200/200 tests pass (100%), coverage cao trên toàn bộ các service notification |
| `./gradlew build`                         | **PASS**   | Build thành công toàn bộ backend artifact                                     |

---

## 4. Đặc tả Checkpoint Code & Cơ chế Encode/Decode

1. **Xác định ngày khóa hiệu lực ($T$ - Effective Lock Date)**:
   - Ưu tiên 1: `reopenUntil` (nếu học kỳ đang mở lại sau khóa).
   - Ưu tiên 2: `automaticLockAt` (ngày khóa tự động đã cấu hình).
   - Ưu tiên 3: `endDate + 45 ngày` (fallback mặc định theo quy chế học vụ).
2. **Quy tắc Encode chuỗi `checkpoint_code`**:
   - $\Delta = \text{ChronoUnit.DAYS.between}(T, \text{checkpointDate})$.
   - $\Delta = 0 \implies \mathbf{"t"}$ (chính ngày khóa).
   - $\Delta < 0 \implies \mathbf{"t" + \Delta + "d"}$ (ví dụ: $-7 \rightarrow \mathbf{"t-7d"}$, $-45 \rightarrow \mathbf{"t-45d"}$).
   - $\Delta > 0 \implies \mathbf{"t+" + \Delta + "d"}$ (ví dụ: $+3 \rightarrow \mathbf{"t+3d"}$, $+14 \rightarrow \mathbf{"t+14d"}$).
3. **Quy tắc Decode chuỗi `checkpoint_code`**:
   - Bỏ tiền tố `t` và hậu tố `d` để lấy lại giá trị số nguyên offset $\Delta$.
4. **11 Mốc Offset chuẩn kích hoạt thông báo**:
   - `{-45, -30, -14, -7, -3, -1, 0, 1, 3, 7, 14}`.
5. **Ứng dụng Idempotent & Audit**:
   - Bảng `semester_lock_report` và `semester_completeness_notification` dùng `checkpoint_code` làm thành phần khóa duy nhất chống chạy trùng và chống spam email khi batch chạy lại trong ngày.

---

## 5. Trạng thái sau triển khai

- Hệ thống thông báo tính đầy đủ của dữ liệu điểm học kỳ theo `CR-SEM-001` và Developer Plan 040 đã hoàn thành 100%.
- Cấu hình thông tin mail sử dụng environment variables (`${SPRING_MAIL_USERNAME}`, `${SPRING_MAIL_PASSWORD}`, v.v.), không có biến môi trường hoặc tài khoản nhạy cảm nào bị lưu cứng trong mã nguồn code Java.

---

## 6. Tổng kết (Dev Note Summary)

- **Mục tiêu đạt được**: Đã đóng toàn bộ các yêu cầu còn trì hoãn từ **Plan 27.1**, chính thức phê duyệt và đưa **CR-SEM-001** vào vận hành trong hệ thống.
- **Tính năng cốt lõi**:
  - Tự động đánh giá dữ liệu điểm học kỳ và gửi cảnh báo cá nhân hóa theo từng vai trò (Giáo vụ, GVBM, GVCN) với độ cách ly dữ liệu cao (`BR-SEM-006-08`).
  - Gửi mail qua Spring Mail với cơ chế chống gửi lặp (Idempotent), cách ly lỗi không ảnh hưởng transaction chính (`BR-SEM-006-06`) và hỗ trợ retry thông báo thất bại (`BR-SEM-006-05`).
  - Cung cấp 3 REST API quản lý và điều phối thông báo cho Giáo vụ/Admin.
- **Chất lượng kiểm định**:
  - Checkstyle: 0 lỗi.
  - PMD: 0 vi phạm.
  - Test Suite: 200/200 tests pass (100%).
  - JaCoCo Coverage: > 90% instruction coverage trên các module notification.
  - Build Artifact: Thành công.


