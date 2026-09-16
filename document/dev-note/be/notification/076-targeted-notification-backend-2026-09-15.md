# Dev Note — Plan 076 Backend: Targeted Notification v3

## 1. Thông tin và approval

- Kế hoạch liên quan: [Plan 076 BE](../../../dev-impl-plan/be/notification/076-targeted-notification-2026-09-15.md).
- Application-document version: **v3**.
- Approval: implementation slice đã được user yêu cầu sửa theo scope, channel, immediate publish
  và idempotency; privacy option A nay đã được user chốt và Terra đã triển khai enforcement.
- Trạng thái: **IMPLEMENTED SLICE — validation incomplete; không phải COMPLETED**.

## 2. Phạm vi thực tế

- Audience: `INDIVIDUAL`, `CLASS`, `SCHOOL`; class/school resolve thành recipient snapshot khi publish.
- Single-school boundary: FE gửi `DEFAULT_SCHOOL`; BE chỉ chấp nhận đúng giá trị này.
- Channel: chỉ `IN_APP`.
- Publish: chỉ immediate publish; `publishAt` trong tương lai bị từ chối, không có scheduler.
- Idempotency: fingerprint SHA-256 của full payload + actor; cùng key và cùng fingerprint trả lại
  kết quả cũ, khác actor/payload trả `409`; unique-key race cũng được map thành `409`.
- Inbox/detail: chỉ trả notification đã `PUBLISHED`, đã đến `publishAt` và chưa quá `expiresAt`;
  receipt/read state và access scope được kiểm tra backend.
- Privacy option A: recipient không được thấy `targetReference` hoặc internal IDs; chỉ
  `ADMIN`/`ACADEMIC_OFFICE` được xem targeting details. Retention vẫn còn mở.
- Expired-detail privacy fix: recipient detail áp dụng visibility guard, không trả title/body sau
  khi notification hết hạn.
- Audience/school-scope validation: validate chéo field theo audience, chặn dữ liệu malformed
  khi create/publish và chặn notification ngoài `DEFAULT_SCHOOL`.
- Không thay đổi notification email v2 hoặc bảng `semester_completeness_notification`.

## 3. Files changed trong slice BE

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/notification/` — entity, DTO, repository,
  audience/audit/service/controller.
- `BE/BaiTap-RS/src/main/resources/db/migration/V25__create_notification_and_receipt.sql`.
- `BE/BaiTap-RS/src/main/resources/db/migration/V26__harden_notification_v3_constraints.sql`.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/notification/` — focused regression tests.

Migration checksum repair (without Flyway repair):

- V25 được khôi phục byte-for-byte từ Git blob `fd6dd5a8dea73f00419c8e11694d7af7bd81fafa`;
  checksum theo thuật toán Flyway là `-164191837`, khớp record đã apply.
- V25 chịu trách nhiệm tạo `idempotency_fingerprint` và constraint channel `IN_APP`.
- V26 chỉ thêm `ck_notification_school_scope`; đã loại bỏ các thao tác duplicate column và
  drop/add channel constraint.
- MySQL read-only verification sau startup: V25 checksum `-164191837`, `success=1`; V26 checksum
  `-880875250`, `success=1`; cả hai constraint `ck_notification_channel` và
  `ck_notification_school_scope` tồn tại; `idempotency_fingerprint` tồn tại.
- Không chạy Flyway `repair`, `clean`, `drop` hoặc sửa DB trực tiếp.

## 4. Validation Result

| Gate | Kết quả | Evidence/giới hạn |
|---|---|---|
| Notification focused tests | **PASS** | Focused notification suite: 62 tests passed, including the legacy embedded-idempotency regression |
| Backend full test | **FAIL / ABORTED** | Lượt chạy bị dừng sau khi ghi nhận `OutOfMemoryError` ở integration test hiện hữu; chưa có full-suite PASS |
| `checkstyleMain` | **PASS** | `./gradlew checkstyleMain --no-daemon --max-workers=1 --console=plain` |
| `pmdMain` | **FAIL** | `./gradlew pmdMain --no-daemon --max-workers=1 --console=plain`; repository-level: 9 LessonLog baseline violations; notification production package: 0 violations |
| `build` | **FAIL** | `./gradlew build --no-daemon --max-workers=1 --console=plain`; dừng tại `pmdMain` |
| Full JaCoCo | **NOT RUN / insufficient** | JaCoCo chỉ chạy như finalizer của focused test, chưa phải full JaCoCo gate |
| Flyway/H2 migration chain | **PASS** | `./gradlew test --no-daemon --max-workers=1 --console=plain --rerun-tasks --tests 'com.JavaTraining.BaiTap_RS.config.*FlywayMigrationTest' --tests 'com.JavaTraining.BaiTap_RS.notification.NotificationMigrationTest'` |
| V25 checksum/blob verification | **PASS** | Current V25 byte-identical với Git blob `fd6dd5a8...`; resolved checksum `-164191837` |
| MySQL/runtime migration | **PASS** | Startup validate 26 migrations và apply V26 thành công; app/JPA khởi động trên port 8081; read-only history/metadata xác nhận V25/V26 và constraints |
| `git diff --check` | **PASS** | Không có whitespace error trong diff |
| Browser/live integration | **PASS (targeted smoke)** | Sau khi restart backend và đăng nhập mới bằng `academic.office`, Chrome mở `/v2/notifications/manage`, hiển thị heading “Quản lý thông báo” và danh sách; không redirect `/login` |

## 4.1. Amendment 076.2 — legacy notification thiếu embedded idempotency

- Root cause của việc giáo vụ bị đá khỏi màn hình quản lí: một notification row cũ có cả
  `idempotency_key` và `idempotency_fingerprint` là `NULL`. Hibernate materialize embedded
  `NotificationIdempotency` thành `null`; mapper gọi getter trực tiếp, ném
  `NullPointerException`. Error dispatch làm response bề ngoài thành `401`, khiến `apiClient`
  xoá session và điều hướng về `/login`. Đây không phải lỗi quyền `ACADEMIC_OFFICE`.
- `Notification` nay đọc idempotency null-safe và tự khởi tạo embedded object khi setter được
  gọi, nên vẫn tương thích dữ liệu legacy mà không thay đổi schema.
- Thêm `NotificationResponseMapperTest.mapsLegacyNotificationWithoutEmbeddedIdempotency()` để
  khóa hành vi tương thích này.
- Live evidence: authenticated `GET /api/v3/notifications/manage?page=0&size=20` trả `200`;
  Chrome walkthrough tới `/v2/notifications/manage` trả đúng màn hình quản lí.

## 5. Deviations, blockers và next steps

- Scheduling được giới hạn thành immediate publish theo quyết định người dùng; `SCHEDULED` không
  phải flow được chứng minh trong slice này.
- V25 đã được restore chính xác theo checksum đã apply; hardening còn thiếu duy nhất được giữ ở V26.
  Không chạy Flyway `repair`, `clean`, drop hoặc reset.
- Plan không được đánh dấu `COMPLETED` khi full test, repository PMD và build còn FAIL.
- PMD notification: 0 violation; repository còn 9 LessonLog baseline violations, nên `pmdMain`
  và `build` vẫn FAIL tại baseline đó.
- Còn rủi ro retention, full backend quality gates và kiểm thử authorization/integration nghiệp vụ
  trên database thật; full coverage là NOT RUN. Targeted browser/live đã PASS sau khi restart
  backend, nhưng toàn bộ notification feature chưa production-safe do PMD/build baseline còn FAIL.
