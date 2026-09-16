# Dev Note Summary — Plan 076: Targeted Notification v3

## 1. Trạng thái

- Application-document version: **v3**.
- Trạng thái: **IMPLEMENTED SLICE — validation incomplete; không phải COMPLETED**.
- Plan BE: [076 BE](../../dev-impl-plan/be/notification/076-targeted-notification-2026-09-15.md).
- Plan FE: [076 FE](../../dev-impl-plan/fe/notification/076-targeted-notification-ui-2026-09-15.md).

## 2. Kết quả thực tế

- Audience v3: `INDIVIDUAL`, `CLASS`, `SCHOOL`.
- Single-school: FE gửi `DEFAULT_SCHOOL`, BE validate exact value.
- Channel: `IN_APP` only.
- Publish: immediate only; future `publishAt` bị reject.
- Idempotency: full-payload + actor fingerprint; mismatch/race trả `409`.
- Inbox: lọc `PUBLISHED`, publish đã đến và chưa hết hạn; receipt/read access được giữ.
- Privacy option A: recipient không thấy `targetReference`/internal IDs; chỉ
  `ADMIN`/`ACADEMIC_OFFICE` thấy targeting details; retention còn mở.
- Expired-detail privacy fix và audience/school-scope validation đã được bổ sung.
- FE privacy identity fields là nullable/optional; composer gửi stable `idempotencyKey`.
- V25 được khôi phục byte-for-byte từ Git blob `fd6dd5a8...`, checksum `-164191837` khớp DB;
  V26 chỉ thêm `ck_notification_school_scope`, không dùng `repair`.
- FE pagination: zero-based `page/pageSize/totalPages/totalItems`.
- v2 semester-completeness email nằm ngoài scope và không bị thay đổi.

## 3. Evidence

| Area | Kết quả |
|---|---|
| BE focused notification tests | **PASS** — 61 tests |
| FE focused notification tests | **PASS** — 6 files, 29 tests |
| FE full test | **PASS** — 109 test files, 571 tests |
| FE coverage | **NOT RUN** |
| Storybook build | **NOT RUN** |
| FE lint/build | **PASS** |
| BE full test | **FAIL / ABORTED** — lượt chạy bị dừng sau khi ghi nhận `OutOfMemoryError` ở integration test hiện hữu; chưa có full-suite PASS |
| BE `checkstyleMain` | **PASS** |
| BE `pmdMain` | **FAIL** — notification 0 violations; repository-level PMD có 9 LessonLog baseline violations |
| BE `build` | **FAIL** — dừng tại `pmdMain` |
| Full JaCoCo | **NOT RUN / insufficient** — chỉ chạy như finalizer của focused test |
| Flyway/H2 migration chain | **PASS** — focused Flyway migration command chạy thành công sau patch |
| V25 checksum/blob | **PASS** — exact blob match, resolved checksum `-164191837` |
| MySQL/runtime migration | **PASS** — validate 26 migrations, apply V26 thành công; history/metadata read-only xác nhận |
| `git diff --check` | **PASS** |
| Browser/live | **NOT RUN / BLOCKED** — Chrome redirect về `/login` |

## 4. Tài liệu chi tiết

- [BE Dev Note](../be/notification/076-targeted-notification-backend-2026-09-15.md)
- [FE Dev Note](../fe/notification/076-targeted-notification-frontend-2026-09-15.md)

Không mark `COMPLETED` cho Plan 076 cho đến khi các blocker/gate nêu trên được xử lý và xác minh.
