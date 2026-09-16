# Dev Note 076 — Ghi nhận Developer Plan Targeted Notification

- Ngày: **2026-09-15**; application-document version: **v3**.
- Related plans:
  - [Plan BE](../../dev-impl-plan/be/notification/076-targeted-notification-2026-09-15.md)
  - [Plan FE](../../dev-impl-plan/fe/notification/076-targeted-notification-ui-2026-09-15.md)
- Approval status: Plan đã được dùng làm contract cho implementation slice; các quyết định đã
  chốt gồm audience, single-school scope, channel, immediate publish, idempotency và privacy option A.
  Full completion vẫn **CHƯA ĐẠT** do các gate ghi dưới đây.

## Phạm vi đã hoàn thành

- Ghi Developer Plan BE cho module notification v3.
- Ghi Developer Plan FE cho inbox/composer/audience UI v3.
- Ghi rõ baseline notification email v2 không bị thay đổi.
- Ghi requirement, role/scope boundary, candidate API, data invariant, file scope, test plan
  và acceptance criteria.
- Ghi nhận implementation boundary: `INDIVIDUAL`/`CLASS`/`SCHOOL`, `DEFAULT_SCHOOL` do FE gửi và
  BE validate, `IN_APP` only, immediate publish và full-payload + actor idempotency fingerprint.
- Ghi nhận privacy option A: recipient không thấy `targetReference`/internal IDs; chỉ
  `ADMIN`/`ACADEMIC_OFFICE` xem targeting details. Retention/content policy còn mở.
- Ghi nhận expired-detail privacy fix, audience/school-scope validation, FE nullable privacy
  fields và stable idempotency key.
- Ghi nhận V25 restore byte-for-byte theo Git blob/checksum đã apply; V26 chỉ chứa school-scope
  constraint để tránh duplicate column/channel constraint.

## Files changed

- `document/dev-impl-plan/be/notification/076-targeted-notification-2026-09-15.md`
- `document/dev-impl-plan/fe/notification/076-targeted-notification-ui-2026-09-15.md`
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`
- `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`
- `document/dev-impl-plan/fe/FE_DEV_PLAN_SUMMARY.md`
- `document/dev-impl-plan/summary/MASTER_PLAN_V3-2026-09-09.md`
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`

## Validation Result

| Nhóm | Kết quả | Ghi chú |
|---|---|---|
| Markdown/link/scope review | **PASS** | Đã đối chiếu v3 source, Plan 073, Plan 075/079 và convention summary |
| Backend focused tests | **PASS** | 61 notification tests passed |
| Backend full test | **FAIL / ABORTED** | Lượt chạy bị dừng sau khi ghi nhận `OutOfMemoryError` ở integration test hiện hữu; chưa có full-suite PASS |
| Backend `checkstyleMain` | **PASS** | Chạy với `--no-daemon --max-workers=1 --console=plain` |
| Backend `pmdMain` | **FAIL** | Notification 0 violations; repository-level PMD còn 9 LessonLog baseline violations |
| Backend `build` | **FAIL** | Dừng tại `pmdMain` |
| Backend full JaCoCo | **NOT RUN / insufficient** | JaCoCo chỉ là finalizer của focused test |
| Frontend focused tests/lint/build | **PASS** | 6 files/29 tests; lint và build PASS |
| Frontend full test | **PASS** | `npm test -- --run`: 109 test files, 571 tests |
| Frontend coverage | **NOT RUN** | Chưa chạy full coverage gate |
| Storybook build | **NOT RUN** | Chưa chạy Storybook build gate |
| Flyway/H2 migration chain | **PASS** | NotificationMigrationTest 1 test; Flyway chain tới V26 tổng 5 tests |
| MySQL/runtime migration | **NOT RUN** | Chưa chạy trên MySQL/runtime database |
| Browser/live | **NOT RUN / BLOCKED** | Chrome session redirect về `/login`; chưa có authenticated runtime evidence |

## Deviation, blocker và next step

- Không tạo wireframe HTML hoặc code vì yêu cầu hiện tại là ghi plan; wireframe/fixture chi
  tiết sẽ theo contract approval.
- Worktree có thay đổi Plan 079 từ trước; không chỉnh sửa, reset hoặc stage các thay đổi đó.
- Blocker hiện tại: backend full test và repository PMD/build; MySQL/runtime, browser/live và
  coverage chưa chạy.
- Không đánh dấu Plan 076 `COMPLETED`; source privacy enforcement đã được Terra triển khai, nhưng
  full test/baseline và các gate còn lại vẫn chưa đủ.
