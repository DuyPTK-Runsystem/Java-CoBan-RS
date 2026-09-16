# Plan 076 BE — Targeted Notification v3

## 1. Trạng thái, nguồn và mục tiêu

- Application-document version: **v3**.
- Status: **IMPLEMENTED — validation incomplete; not COMPLETED**.
- Nguồn: [ApplicationContext](../../../application-doc/v3/ApplicationContext.md),
  [RequirementBaseline](../../../application-doc/v3/RequirementBaseline.md),
  [CR-V3-001](../../../application-doc/v3/change-request/CR-V3-001-academic-operations-and-targeted-communication.md),
  [Module 03](../../../application-doc/v3/modules/03-NotificationAndAudience.md),
  [data-model boundary](../../../application-doc/v3/data-model/README.md),
  [FE API boundary](../../../application-doc/v3/frontend-api/README.md),
  [Master Plan](../../summary/MASTER_PLAN_V3-2026-09-09.md).
- Requirement: `FR-V3-NOTI-001..004`, `BR-V3-NOTI-001`, `NFR-V3-001..002`, `AC-V3-001..002`.

Mục tiêu là tạo module notification v3 theo audience `INDIVIDUAL`, `CLASS`, `SCHOOL`,
kiểm soát scope ở backend, lưu receipt/read state, audit và chống duplicate bằng
idempotency. Implementation hiện tại đã được ghi nhận; các gate chưa đạt hoặc chưa chạy
không được suy diễn thành hoàn tất.

## 2. Baseline và ranh giới

v2 hiện có notification email phục vụ semester completeness tại
`academic/controller/SemesterCompletenessController` và các service/entity liên quan.
Plan 076 không xóa, đổi semantics hoặc di chuyển ngầm luồng v2 này. Notification v3 là
module mới, kênh mặc định đề xuất là `IN_APP`; email/push vẫn thuộc `TBD-004`.

### In-scope

- Entity/aggregate notification và receipt.
- Audience validation/resolution, school scope và recipient access guard.
- Lifecycle `DRAFT -> PUBLISHED -> EXPIRED/CANCELLED`; publishAt trong tương lai bị từ chối
  (slice này chỉ hỗ trợ immediate publish, không có scheduler).
- API tạo, publish, cancel, inbox, detail và mark-read theo contract được duyệt.
- Audit actor/time/audience/publish state, correlation và idempotency.
- Paged list dùng `ResultPaginationDTO`, page zero-based.
- BE contract/unit/integration tests và migration sau khi schema được chốt.

### Out-of-scope

- Email/push delivery, SMTP, template engine và retry email v2.
- Scheduler/background job nếu chưa có quyết định D09.
- Notification vượt school scope, broadcast không có audience kiểm chứng.
- Xóa hoặc thay đổi các bảng/migration `semester_completeness_notification` v2.

## 3. Open decisions phải chốt trước implementation

| Mã | Quyết định | Đề xuất để review |
|---|---|---|
| D01 | Quyền tạo/xuất bản | `ADMIN`, `ACADEMIC_OFFICE`; `TEACHER` chỉ đọc ở slice đầu |
| D02 | Resolution `CLASS`/`SCHOOL` | **Đã triển khai**: snapshot recipient tại thời điểm publish; không tự thêm membership về sau |
| D03 | Channel | **Đã chốt**: chỉ `IN_APP` cho v3; email/push để phase riêng |
| D04 | Lifecycle | **Đã triển khai theo slice immediate**: draft/published/expired/cancelled; future publishAt trả lỗi |
| D05 | School scope | **Đã chốt**: FE gửi `DEFAULT_SCHOOL`; BE validate đúng hằng số này vì app chỉ phục vụ một school |
| D06 | Content policy | Chốt giới hạn title/body, plain text hay cho phép link/HTML |
| D07 | Privacy/retention | **Đã chốt option A**: recipient không thấy `targetReference`/internal IDs; chỉ `ADMIN`/`ACADEMIC_OFFICE` được xem targeting details. Retention còn mở |
| D08 | Recipient identity | Chuẩn hóa receipt về `app_user`; student/teacher là scope/metadata |
| D09 | Scheduling | **Đã chốt**: không có scheduled flow trong slice này; chỉ immediate publish |

Không tạo endpoint/schema production từ các đề xuất trên nếu contract checkpoint chưa được
người dùng duyệt.

## 4. Domain model và invariant dự kiến

### `Notification`

Gồm `id`, school scope, creator, audience type/reference, title/body, channel, status,
`publishAt`, `expiresAt`, idempotency key, correlation, version và created/updated audit.

### `NotificationReceipt`

Gồm notification, recipient `app_user`, resolution time, access scope, `readAt`, version
và audit cần thiết. Unique `(notification_id, recipient_user_id)` bảo vệ duplicate.

Invariant:

- Recipient không được vượt audience/school scope.
- Individual chỉ nhận user hợp lệ sau backend authorization.
- Class/school snapshot không thay đổi âm thầm sau publish.
- Create idempotency dùng fingerprint SHA-256 của full payload + actor; cùng key/cùng payload/actor
  trả lại kết quả cũ, khác fingerprint hoặc actor trả `409`; race trên unique key cũng trả `409`.
- Publish/cancel/read mutation phải idempotent hoặc optimistic-lock phù hợp.
- Notification hết hạn không được trả như active; receipt vẫn tuân retention policy.
- Audit không nhận entity từ HTTP; DTO là boundary, entity chỉ ở persistence layer.

Migration/table/index/FK cụ thể chỉ được chốt sau khi D01–D09 và schema review hoàn tất;
không tự đặt version migration chỉ từ số thứ tự dự kiến.

## 5. API contract checkpoint đề xuất

Các URI dưới đây là candidate để review, chưa phải API đã được phê duyệt:

| Method | Candidate URI | Mục đích |
|---|---|---|
| `POST` | `/api/v3/notifications` | Tạo draft notification |
| `GET` | `/api/v3/notifications/inbox?page=0&size=20` | Inbox theo actor, paged |
| `GET` | `/api/v3/notifications/manage?page=0&size=20` | Danh sách quản lý theo scope, paged |
| `GET` | `/api/v3/notifications/{id}` | Chi tiết sau khi kiểm tra access |
| `POST` | `/api/v3/notifications/{id}/publish` | Xuất bản/schedule theo lifecycle |
| `POST` | `/api/v3/notifications/{id}/cancel` | Hủy notification chưa hết lifecycle |
| `POST` | `/api/v3/notifications/{id}/read` | Đánh dấu receipt đã đọc, idempotent |

Request/response phải dùng `Req...DTO`/`Res...DTO`, giữ `RestResponse`, không lộ entity.
Inbox và management list dùng `ResultPaginationDTO`; mutation trả resource/version mới.
Recipient response không lộ `targetReference` hoặc internal IDs; targeting details chỉ dành cho
`ADMIN`/`ACADEMIC_OFFICE` sau khi backend authorize.

HTTP contract tối thiểu:

- `400/422`: payload, enum, length, lifecycle hoặc audience không hợp lệ.
- `401`: xử lý session theo `apiClient` hiện tại.
- `403`: sai role hoặc ngoài scope, không xóa session.
- `404`: không tồn tại sau scope check phù hợp.
- `409`: duplicate idempotency, stale version hoặc lifecycle race; không retry mù.

## 6. Package/file scope dự kiến

```text
BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/notification/
  controller/NotificationController.java
  service/NotificationService.java
  service/NotificationAudienceService.java
  service/NotificationAuditService.java
  repository/NotificationRepository.java
  repository/NotificationReceiptRepository.java
  domain/entity/Notification.java
  domain/entity/NotificationReceipt.java
  domain/entity/NotificationAudienceType.java
  domain/entity/NotificationStatus.java
  domain/entity/NotificationChannel.java
  domain/DTOs/requests/ReqCreateNotificationDTO.java
  domain/DTOs/requests/ReqPublishNotificationDTO.java
  domain/DTOs/requests/ReqMarkNotificationReadDTO.java
  domain/DTOs/response/ResNotificationDTO.java
  domain/DTOs/response/ResNotificationReceiptDTO.java
```

Tên file có thể điều chỉnh trong contract review nếu project convention hoặc identity
mapping hiện tại yêu cầu khác. Migration nằm trong
`BE/BaiTap-RS/src/main/resources/db/migration/` sau khi kiểm tra Flyway history thực tế.

## 7. Test, validation và acceptance

- Unit: audience scope, snapshot membership, lifecycle, idempotency, read state, expiry.
- Integration/MockMvc: quyền `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`, `STUDENT`; `400/401/403/404/409/422`;
  pagination zero-based và transaction rollback.
- Concurrency: hai publish/read/create cùng idempotency không tạo duplicate receipt.
- Migration/Flyway: schema thật, unique/index/FK, không ảnh hưởng notification v2.
- Gates sau implementation: backend test, Checkstyle, PMD, build, JaCoCo và MySQL/Flyway.
  Mỗi gate báo riêng `PASS/FAIL/NOT RUN`; browser/live không được suy ra từ unit test.

Acceptance: không có notification vượt audience, không duplicate khi retry cùng key, người
nhận chỉ đọc đúng scope, read state bền sau reload/re-query, audit đủ actor/time/state và
notification v2 vẫn hoạt động độc lập.

## 8. Approval và Dev Note

## 9. Evidence sau implementation

| Gate | Kết quả | Evidence/giới hạn |
|---|---|---|
| BE notification focused tests | **PASS** | `GRADLE_USER_HOME=.gradle-user-home ./gradlew test --tests 'com.JavaTraining.BaiTap_RS.notification.*'`: 49 tests passed |
| FE focused tests | **PASS** | 6 files, 29 tests passed |
| FE lint/build | **PASS** | `npm run lint && npm run build` |
| BE full test | **FAIL** | `./gradlew cleanTest test --no-daemon --max-workers=1 --console=plain`: 511 tests completed, 12 failed; XML evidence cho thấy `OutOfMemoryError` trong integration tests hiện hữu (Auth/Student/JWT) |
| BE PMD production | **PASS** | Notification production package còn 0 PMD violations; repository-level PMD vẫn FAIL do LessonLog baseline |
| BE `checkstyleMain` | **PASS** | `./gradlew checkstyleMain --no-daemon --max-workers=1 --console=plain` |
| BE `pmdMain` | **FAIL** | `./gradlew pmdMain --no-daemon --max-workers=1 --console=plain`; repository-level: 9 LessonLog baseline violations; notification package: 0 violations |
| BE `build` | **FAIL** | `./gradlew build --no-daemon --max-workers=1 --console=plain`; dừng tại `pmdMain`, không coi là build PASS |
| BE full JaCoCo | **NOT RUN / insufficient** | JaCoCo chỉ chạy như finalizer của focused test, chưa phải full JaCoCo gate |
| Flyway/H2 migration chain | **PASS** | `GRADLE_USER_HOME=.gradle-user-home ./gradlew test --no-daemon --max-workers=1 --console=plain --rerun-tasks --tests 'com.JavaTraining.BaiTap_RS.config.FlywayMigrationTest'`; 4 migration tests, chain tới V25 PASS |
| MySQL/runtime migration | **NOT RUN** | Chưa chạy trên MySQL/runtime database |
| Browser/live integration | **NOT RUN** | Chưa có authenticated runtime walkthrough |

Không đánh dấu Plan 076 là `COMPLETED` khi full test, repository PMD và build còn FAIL; các gate
migration/browser/live vẫn chưa chạy.
Notification email v2 (`semester_completeness_notification`) nằm ngoài scope và không bị thay đổi.
