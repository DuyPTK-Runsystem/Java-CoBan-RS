# Developer Plan 098 — Loan / Return / Renewal / Lost

## 1. Trạng thái, mục tiêu và dependency

- **Ngày lập:** 2026-10-09.
- **Application baseline:** Library Management v5 (0.1-draft), trên nền Java-CoBan-RS hiện hữu.
- **Trạng thái:** **APPROVED — implementation được duyệt; production target DB evidence chưa xác minh**.
- **Amendment 2026-10-09:** các con số chính sách circulation/fine là giá trị mặc định có thể cấu hình, không phải hằng số nghiệp vụ cố định.
- **Kế hoạch phụ thuộc:** Plan 095 (foundation/contract gates), Plan 096 (catalog/book copy), Plan 097 (patron/library card).
- **Kế hoạch sử dụng kết quả:** các plan batch/notification và FE hardening tiếp theo, nếu còn phần tách riêng sau khi slice này được nghiệm thu.
- Không mặc định coi migration V31, card/patron service hoặc catalog validation đã chứng minh DB runtime, MySQL version, concurrency hay API contract production.

### Approval and gate amendment — 2026-10-09

- Người dùng đã phê duyệt Plan 098, baseline Library v5 và bắt đầu implementation; mặc định borrow nhiều copy là all-or-nothing. Các numeric policy đã liệt kê phải cấu hình được, version/audit và snapshot theo giao dịch. Approval này thay cho yêu cầu duyệt ở §12; các DDL/DB claim vẫn phải dựa trên evidence thật.
- Người dùng cho phép skip **full test** và **PMD Test**. **PMD Main vẫn bắt buộc**; các gate khác tiếp tục theo phân công validation, ghi rõ PASS/FAIL/SKIPPED/NOT RUN.
- F8 được đóng đủ cho development migration/concurrency proof trên disposable MySQL Community 8.0.46: migration kế tiếp theo source là V32, và disposable target must be initialized separately dưới `/tmp`. Đây không xác nhận production/local target DB; exact target version và applied Flyway head vẫn `UNKNOWN`, không target write. Production-target compatibility/concurrency vẫn `NOT VERIFIED`.
- F9 đã được đóng cho routes/DTO/error/audit/policy authorization trong §7 và Amendment §12.1 dưới đây, sau source route scan không thấy exact method/path collision. Policy writer là `ADMIN` và `LIBRARIAN`. Notification contract chốt IN_APP notification + after-commit SSE và idempotency key; email delivery/retry không được hứa trong slice này vì đường email hiện tại gửi trước commit và chưa có retry contract.
- **Cleanup amendment — 2026-10-09:** User authorized removing excess one-off/generated runtime probe artifacts. Scope is the standalone Plan 097 MySQL acceptance runner and its VS Code launch entry/generated class files, plus generated Plan 098 probe `.class`/`.args` files under `/tmp/plan098-runtime-resume` after confirming no active process uses them. Preserve Plan 098 Java probe sources, logs and MySQL data directory; preserve Plan 097 implementation, tests and historical acceptance results. Do not run additional tests for this cleanup.

Mục tiêu của Plan 098 là đưa các nghiệp vụ lưu thông cốt lõi vào cùng một transaction boundary có thể kiểm chứng:

1. Tạo loan cho một hoặc nhiều bản sao bằng patron lookup/card và barcode.
2. Bảo vệ copy locking và giới hạn tối đa năm loan đang hoạt động cho mỗi patron.
3. Tính và snapshot due date/policy version theo Asia/Ho_Chi_Minh.
4. Nhận trả, gia hạn, đánh dấu mất sách và giữ lịch sử không bị xóa.
5. Gắn reservation FIFO vào borrow/return/renew khi reservation làm thay đổi eligibility.
6. Tạo/cập nhật fine state cần thiết khi trả hoặc mất; hỗ trợ contract cho overdue batch và offline payment/waive nếu được duyệt trong cùng slice.
7. Cung cấp API, FE workflow, audit và stable error code mà không thay đổi ngầm contract /api/v1, /api/v2, /api/v3 hiện hữu.

## 2. Quyết định kế thừa và các điểm còn lại từ bản DRAFT

Các quyết định sau đã được ghi nhận ở Plan 095 và Plan 097, không hỏi lại:

- app_user là identity; borrower là patron hợp lệ, ngoại trừ user mang role ADMIN hoặc LIBRARIAN.
- BORROWING_SUSPENDED chặn borrow, renewal và reservation mới; vẫn cho đọc, trả và hủy reservation. Không lock app_user.
- Một book_copy chỉ có một active loan, kể cả concurrent requests. Giới hạn active loans/patron là policy cấu hình; mặc định hiện tại là 5.
- Due, renewal limit và renewal duration đều là policy cấu hình có version/audit; mặc định hiện tại lần lượt là 14 ngày, 2 lần và +7 ngày. Ngày hạn còn hiệu lực hết ngày, overdue bắt đầu ngày kế tiếp.
- LOST kết thúc active loan; overdue tính đến ngày mất; tạo LOST_ITEM = book.listPrice + 50,000 VND; không cộng thêm sau ngày mất.
- Reservation FIFO theo reservedAt và tie-breaker ổn định; khi copy được cấp thì READY, pickup mặc định 3 ngày lịch; hết hạn từ đầu ngày kế tiếp theo Asia/Ho_Chi_Minh.
- Không cho cấp role LIBRARIAN nếu còn reservation WAITING/READY chưa được ADMIN giải quyết.
- Fine active overdue là provisional; chỉ pay/waive sau RETURNED hoặc LOST khi amount đã chốt. Payment offline toàn phần; ADMIN/LIBRARIAN pay, ADMIN waive; không partial/refund/receipt PDF.
- Mọi mutation circulation/fine/reservation phải audit actor, thời điểm, target, reason/reference phù hợp.

Các điểm chưa được chọn và phải đóng trước code/migration:

- F8 target evidence: production/local target exact MySQL version và applied Flyway head chưa biết. Đã được duyệt dùng disposable MySQL 8.0.46 cho migration/concurrency evidence; không target write.
- F9 API/error/audit/policy-editor role: đã được đóng trong amendment §12.1; email delivery/retry vẫn không thuộc contract slice.
- Chiến lược multi-copy borrow atomicity: đã được duyệt all-or-nothing.
- Cách biểu diễn LOST/RETURNED trong loan và fine component được chốt trong implementation; correction/reversal workflow vẫn deferred ngoài slice.
- Policy editor/versioning: role ADMIN/LIBRARIAN đã được duyệt; timezone theo Asia/Ho_Chi_Minh và policy edit không rewrite due date cũ.

### 2.1. Amendment về policy cấu hình

Plan 098 ghi nhận quyết định mới của người dùng: các giá trị số sau phải chỉnh được bởi role quản trị chính sách đã được phê duyệt, có effective time, version, actor và audit:

- maxActiveLoans (mặc định 5).
- loanDurationDays (mặc định 14).
- maxRenewals (mặc định 2) và renewalDurationDays (mặc định 7).
- reservationPickupDays (mặc định 3).
- Fine tiers có số lượng không cố định: cấu hình danh sách bậc gồm mốc ngày kết thúc và đơn giá/ngày, cùng fineCapPerLoan (mặc định wireframe minh họa 5.000 / 10.000 / 20.000 VND và trần 500.000 VND).
- fineSuspensionThreshold (mặc định 500.000 VND).

Mỗi loan, renewal, reservation và fine phải snapshot policyVersion và các giá trị đã áp dụng. Thay đổi policy chỉ ảnh hưởng giao dịch mới hoặc lần tính batch được chỉ định; không rewrite due date, fine đã chốt, PAID/WAIVED hoặc lịch sử cũ. Authorization của policy editor là ADMIN/LIBRARIAN theo approval amendment.

## 3. Nguồn đối chiếu và truy vết requirement

Đọc theo source order v5:

1. document/application-doc/v5/README.md, ApplicationContext.md, RequirementBaseline.md.
2. change-request/CR-V5-001-library-management.md và FoundationAndContractFreeze.md.
3. modules/03-CirculationAndReservation.md, modules/04-FineAndBatch.md, modules/05-BarcodeQrAndScanning.md.
4. data-model/README.md, data-model/MigrationAndConcurrency.md.
5. contract/Authorization.md, contract/ErrorContract.md, contract/IntegrationBoundaries.md, frontend-api/README.md.
6. Plan 095–097 và code hiện tại để kiểm tra compatibility.

| Requirement                | Phạm vi Plan 098                                                                     | Bằng chứng nghiệm thu                                         |
| -------------------------- | ------------------------------------------------------------------------------------ | ------------------------------------------------------------- |
| FR-V5-LIB-CIRC-001..004    | Borrow, return, renew, history/read model                                            | API/service/DB tests, FE states, audit                        |
| FR-V5-LIB-CIRC-005         | Reservation create/cancel, FIFO, allocation/expiry                                   | Queue/locking/notification tests                              |
| FR-V5-LIB-CIRC-006         | Lost flow và fine linkage                                                            | Transaction/idempotency/audit tests                           |
| BR-V5-LIB-CIRC-001..007    | Policy max active loans (default five), dates, renew limit, copy uniqueness, eligibility, LOST formula, audit | MySQL concurrency + focused tests |
| FR/BR-V5-LIB-FINE-001..006 | Fine calculator/state, overdue job contract, suspension sync, payment/waive boundary | Parameterized, batch restart/idempotency, authorization tests |
| NFR-V5-LIB-001..007        | Backend authority, MySQL, stable errors, security, FE conventions                    | Contract/security/regression gates                            |
| AC-V5-LIB-001..007         | Coverage/concurrency/batch/FE/browser evidence theo scope được duyệt                 | Validation report; chưa chạy ở giai đoạn lập plan             |

## 4. Phạm vi

### 4.1. In scope

**Backend và dữ liệu**

- Loan aggregate, active predicate rõ ràng, policy snapshot, renewal count và version.
- Borrow nhiều copy với deterministic lock order, patron eligibility/card validation, effective maxActiveLoans và all-or-nothing transaction.
- Return theo barcode không cần quét lại patron; resolve active loan, set returned state, cập nhật copy và fine as-of return.
- Renewal có giới hạn, reservation priority check và cộng duration vào due date hiện tại theo policy version mới.
- Reservation theo bookId, queue FIFO, WAITING/READY/FULFILLED/CANCELLED/EXPIRED, copy allocation và pickup expiry.
- Fine calculator dùng BigDecimal/DECIMAL, phân biệt OVERDUE và LOST_ITEM, unique loan/type, provisional/fixed lifecycle.
- Policy editor cho các numeric limits/rates nói trên; Java calculator nhận policy snapshot, không hardcode mốc phạt trong controller/FE.
- Overdue batch logical contract libraryOverdueFineJob: runDate, paging/chunk, restartable, idempotent, bounded skip, job history/metrics, manual launch authorization. Notification dùng infrastructure hiện hữu; không tự tạo mail/outbox mới nếu chưa chứng minh gap.
- Offline full pay/waive và suspension recompute theo approved policy; API/audit/authorization contract is frozen by §12.1.
- MySQL migration/index/FK/unique/locking strategy và dữ liệu lịch sử không cascade-delete.
- Scoped library authorization, stable machine error code, audit và mapping legacy envelope.

**Frontend**

- Route/workspace /library/circulation ưu tiên keyboard/scanner flow: patron lookup/card, copy barcode list, summary, confirm, per-copy result.
- Màn hình return/renew/lost/reservation/fine dùng component/service/types hiện hữu; luôn có manual input fallback.
- Upload-image scanner demo cho cả QR user/thẻ và barcode sách: FE nhận file ảnh, decode QR/Code128 thành chuỗi, đổ chuỗi vào patron/barcode input rồi mới gửi request. Không cần camera hoặc thiết bị scan thật; backend chỉ nhận decoded text. Live camera không nằm trong slice này.
- Loan history, reservation queue/status, fine state/payment/waive theo capability; loading/empty/error/forbidden/conflict/success states và Storybook/tests.

### 4.2. Out of scope

- Library AI, provider integration, recommendation, policy Q&A và prompt retention.
- Online payment gateway, partial payment, refund, receipt PDF, correction/reversal workflow.
- Card PDF/storage subsystem, cover upload/storage, analytics/reporting ngoài circulation history.
- Thay đổi identity/authentication, tạo role MEMBER, tạo app/database/deployment riêng.
- Hard delete loan/copy/history; generic copy status setter; tự động sửa các due date cũ khi policy đổi.
- Production deploy, remote push, target DB write hoặc cấp quyền LIBRARIAN ngoài workflow đã được phê duyệt.
- Tích hợp thiết bị scanner/camera thật, camera permission flow hoặc upload ảnh lên backend trong circulation hot path.

## 5. Hiện trạng source đã xác minh

- Đã có catalog/patron/card packages dưới BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/library/ và FE types/services/views tương ứng.
- Migration mới nhất trong source là V31__create_library_patron_and_card.sql; exact applied head và MySQL version UNKNOWN.
- library_card đã dùng generated active_patron_id/unique trong source migration, nhưng đây chỉ là precedent; không mặc định chọn cùng kỹ thuật cho active loan trước khi kiểm MySQL target.
- Chưa có source loan, reservation, fine hoặc circulation controller/service/entity trong inventory ngày 2026-10-09.
- Có AuditContext, AuditLog, LibraryOperationAuditService, Spring Security/JWT và notification infrastructure để tái sử dụng; cần kiểm field contract thực tế trước code.
- FE đã có catalog/patron/card components nhưng chưa có circulation workspace, loan/reservation/fine API types hay route.
- Module 03/04 và FoundationAndContractFreeze reflect Plan 095 inventory-time gates (F8 BLOCKED, F9 DEFERRED); Plan 098 approval/amendment supersedes them for this slice where explicitly stated.

## 6. Thiết kế đề xuất

### 6.1. Domain ownership

```
library
├── circulation   # loan, borrow, return, renewal, lost, policy snapshot
├── reservation    # book queue, allocation, pickup expiry
└── fine           # calculator, lifecycle, batch adapter, payment/waive
```

catalog sở hữu Book/BookCopy metadata và barcode; patron/card sở hữu identity/card validity; circulation sở hữu loan/copy transition; reservation sở hữu queue/allocation; fine sở hữu amounts/status. Không dùng JPA entity làm API response và không để controller quyết định eligibility/concurrency.

### 6.2. Loan transaction

1. Resolve authenticated actor và patron theo app_user; validate role, patron state, valid card, reference-only/withdrawn/lost state.
2. Normalize/deduplicate copy barcodes; lock patron trước, sau đó lock copies theo ascending ID để tránh deadlock.
3. Kiểm active loan count + request count không vượt effective maxActiveLoans (mặc định 5); kiểm reservation priority; kiểm active-copy invariant bằng DB/transaction strategy đã được F8 duyệt.
4. Snapshot current policy/version; create all loan rows và copy state cùng transaction; audit sau khi state transition hợp lệ.
5. Rollback toàn bộ khi một copy lỗi, unique conflict, audit failure hoặc notification event contract không đạt; map race thành 409 COPY_ALREADY_ON_LOAN.

Đề xuất mặc định all-or-nothing cho batch borrow để tránh trạng thái người dùng không biết copy nào thành công. Nếu PO chọn partial result, phải đổi contract/error/FE trước implementation.

### 6.3. Return, renewal và lost

- Return: lock active loan/copy; resolve bằng barcode; set RETURNED state/time; calculate overdue through return date; persist fine state; copy về AVAILABLE hoặc cấp cho reservation đầu queue theo transaction/event policy; audit.
- Renewal: lock patron → loan → copy/book; reject returned/lost, suspended patron, invalid card, effective max renewal hoặc reservation có priority; create renewal audit/transaction using current policy version and add effective renewalDurationDays (default +7) to current due date; không rewrite original policy snapshot.
- Lost: idempotently reject already closed/lost; lock loan/copy/book; close loan with LOST, copy LOST, compute overdue through lost date and one LOST_ITEM amount listPrice + 50,000; no post-lost accrual; audit actor/reason.

### 6.4. Reservation and notification

- Reservation gắn bookId, patron và reservedAt; không giữ cố định copy trước allocation.
- Queue order (reserved_at, reservation_id); allocate only eligible AVAILABLE/non-reference copy.
- Transition WAITING → READY atomically with copy allocation; calculate pickupDueAt = readyDate + effective reservationPickupDays (default 3 calendar days); expire at start of following day in Asia/Ho_Chi_Minh.
- Return/lost/renew must consult queue priority. Cancellation/expiry releases reservation without deleting history.
- READY notification persists an IN_APP event in the same DB transaction as the reservation transition; SSE refresh occurs after commit. A unique event identity prevents duplicate notifications. Email and automatic retry are excluded because existing email delivery runs before commit and has no retry contract.

### 6.5. Fine and batch

- Fine formula dùng policy snapshot. Giá trị mặc định theo Module 04 là 5,000/day for days 1–7, 10,000/day for days 8–30, 20,000/day after day 30, cap 500,000 per loan; mọi mốc/đơn giá/trần đều có thể cấu hình.
- OVERDUE and LOST_ITEM are explicit types; unique (loan_id, fine_type); UNPAID/PAID/WAIVED lifecycle.
- Active-loan overdue is provisional/read-only for payment; closing transition fixes calculatedThrough. Paid/waived rows cannot be overwritten by an older batch run.
- libraryOverdueFineJob recalculates total as-of runDate; same date rerun is idempotent; restart does not duplicate fine/notification; unpaid total above effective fineSuspensionThreshold (default 500,000) suspends patron unless another state reason governs.
- Payment/waive endpoints require actor, amount/reference/audit and idempotency decision; no gateway or partial settlement.

## 7. API contract đề xuất để review

Document v5 dùng /api/v2; phải scan method-level collision trước code. Các path dưới đây là **proposed**, chưa phải approved contract:

| Method/path                                  | Mục đích                                   | Quyền                         |
| -------------------------------------------- | ------------------------------------------ | ----------------------------- |
| POST /api/v2/loans                           | Tạo một/multi-copy loan                    | ADMIN, LIBRARIAN              |
| GET /api/v2/loans                            | History/filter/page; self hoặc staff scope | owner; ADMIN/LIBRARIAN        |
| POST /api/v2/loans/{loanId}/renew            | Gia hạn một loan                           | owner khi đủ điều kiện; staff |
| POST /api/v2/returns                         | Trả theo barcode/list barcode              | ADMIN, LIBRARIAN              |
| POST /api/v2/reservations                    | Tạo reservation theo book                  | eligible patron self          |
| POST /api/v2/reservations/{id}/cancel        | Hủy reservation                            | owner; staff policy           |
| GET /api/v2/reservations                     | Queue/history/status                       | owner; staff                  |
| POST /api/v2/book-copies/{barcode}/lost      | Đánh dấu mất                               | ADMIN, LIBRARIAN              |
| GET /api/v2/fines/{id}                       | Xem fine/detail state                      | owner; staff                  |
| POST /api/v2/fines/{id}/pay                  | Ghi nhận offline full payment              | ADMIN, LIBRARIAN              |
| POST /api/v2/fines/{id}/waive                | Waive toàn phần                            | ADMIN                         |
| GET /api/v2/library/batch-jobs               | Xem job history                            | ADMIN, LIBRARIAN              |
| POST /api/v2/library/batch-jobs/overdue-fine | Chạy tay với runDate                       | ADMIN, LIBRARIAN              |

Error codes tối thiểu cần giữ: COPY_ALREADY_ON_LOAN, MAX_ACTIVE_LOANS, REFERENCE_ONLY, PATRON_BORROWING_SUSPENDED, CARD_EXPIRED, CARD_REVOKED, ACTIVE_LOAN_NOT_FOUND, RENEW_LIMIT_REACHED, COPY_RESERVED, COPY_NOT_FOUND, INVALID_FINE_AMOUNT, LIBRARY_RESOURCE_FORBIDDEN. Scoped Library error vẫn giữ statusCode/error/message/data và bổ sung code/fieldErrors nếu F9 chốt; không lộ SQL/constraint/stack trace.

## 8. Database/migration đề xuất

- loan: loan_id, patron_id, copy_id, card_id/card snapshot reference, borrowed_at, due_at, returned_at, explicit state, renew_count, policy_version, close reason, audit/version fields.
- reservation: reservation_id, book_id, patron_id, status, reserved_at, ready_at, pickup_due_at, allocated_copy_id nullable, expiry/cancel/fulfill fields, policy/version/audit.
- fine: fine_id, loan_id, fine_type, status, amount, calculated_through, closed/payment/waive actor/reference/time, unique (loan_id, fine_type).
- Optional versioned library_policy/effective-date table only after policy editor fields and ownership are approved; do not hardcode mutable values into existing rows.
- Active-copy strategy must be selected after exact MySQL verification: generated nullable active key + unique, explicit allocation/current-loan table, or equivalent lock strategy. Service-only precheck is rejected.
- Effective maxActiveLoans requires patron-row serialization and a distinct-copy concurrency test; a single-copy unique key alone is insufficient.
- FK/history rules must not cascade delete loans, reservations or fines when book/patron/card changes. Index active loan lookup by copy/patron/due date and reservation queue by book/status/order.
- Migration number là số kế tiếp sau V31 theo head thực tế; không giả định V32 hoặc sửa migration đã áp dụng. Chạy schema/data-repair checks trước khi thêm constraint.

## 9. Phạm vi file dự kiến

| Khu vực                                                                                 | Action                        | Mục đích                                                                           |
| --------------------------------------------------------------------------------------- | ----------------------------- | ---------------------------------------------------------------------------------- |
| BE/.../library/circulation/                                                             | New                           | controller, service, repository, entity, DTO, policy snapshot, state/error mapping |
| BE/.../library/reservation/                                                             | New                           | queue/allocation/lifecycle/notification adapter                                    |
| BE/.../library/fine/                                                                    | New                           | calculator, entity/service, batch reader/processor/writer, payment/waive           |
| BE/.../library/security/LibraryAccessPolicy.java                                        | Edit if needed                | staff/self/borrower capability, no hardcoded role strings                          |
| BE/.../library/common và common/audit                                                   | Edit only if gap proven       | error/audit/event integration; preserve legacy callers                             |
| BE/.../src/main/resources/db/migration/\__create_library_circulation.sql                | New                           | loan/reservation/fine/policy tables and indexes in migration V32, with disposable MySQL proof |
| BE/.../src/test/java/.../library/{circulation,reservation,fine}/                        | New                           | unit, API/security, MySQL/concurrency/batch tests                                  |
| FE/src/services/library/{libraryCirculationApi,libraryReservationApi,libraryFineApi}.ts | New                           | typed API/error/query contracts                                                    |
| FE/src/types/library/{circulation,reservation,fine}.ts                                  | New                           | enums, DTOs, state and pagination types                                            |
| FE/src/views/library/LibraryCirculationView.vue and history views                       | New                           | borrow/return/renew/lost/reservation/fine orchestration                            |
| FE/src/components/library/                                                              | New/Edit                      | barcode workflow, summaries, reservation/fine dialogs, state components            |
| FE/src/components/library/ScannerUploadDemo.vue                                         | New                           | upload ảnh QR/barcode, decode ở FE, hiển thị chuỗi và lỗi không đọc được            |
| document/wireframes/fe/library/098-library-circulation/                                 | New                           | circulation, reservation, fine và policy editor; self-contained/offline            |
| FE/src/router/index.ts, shell/menu and role types                                       | Edit                          | route/capability only after verified backend role contract                         |
| document/dev-note/\*\* and summaries                                                    | New/Edit after implementation | actual files, gates, deviations, blockers; not created by this draft               |

Exact class/method names remain subject to repository pattern review after approval. No generic refactor or new dependency is authorized by this draft.

## 10. Validation plan

### Backend/unit/API

- Borrow: eligible/ineligible role, missing/expired/revoked card, suspended/closed patron, reference-only/withdrawn/lost copy, duplicate barcode, effective maxActiveLoans boundary, multi-copy atomic rollback.
- Concurrency: 10 attempts for one copy → exactly one success; parallel different copies for one patron cannot exceed effective maxActiveLoans (default five); deadlock/unique conflict maps to stable codes.
- Return: active resolve, repeated return, overdue amount as-of date, reservation allocation, audit failure rollback.
- Renewal: two successes then RENEW_LIMIT_REACHED, reservation priority, due-date snapshot, policy edit does not rewrite old loan.
- Lost: idempotent repeat, list price + 50,000, overdue cutoff, no later accrual, copy/loan/fine atomicity.
- Reservation: FIFO/tie-break, READY/pickup expiry, cancel/expire/fulfill, duplicate active request, suspension block, notification retry/idempotency.
- Fine/batch: parameterized formula, same runDate three times, old date no decrease, PAID/WAIVED preservation, restart/skip metrics, suspension source and un-suspension.
- Policy configuration: validate positive/range/boundary values, version/audit, effective date, concurrent edit conflict, snapshot isolation for existing loans, and fine preview matching backend calculator.
- Authorization/error/audit: role matrix, ownership, actor/time/reference, legacy envelope plus stable code, no sensitive payload/secret leakage.

### MySQL/integration

- Exact target major version and applied Flyway head must be captured before migration test.
- Verify generated-column/unique/lock syntax on MySQL target; H2 is allowed only for unit/slice tests and cannot prove concurrency or dialect behavior.
- Run clean migration and upgrade migration, FK/index/unique conflict, rollback/repair checks, 10-request concurrency and batch restart tests.
- Confirm no catalog/patron/card regression and no change to legacy API routes.

### Frontend/browser

- Typed request/response and error code mapping; no message parsing.
- Search/filters/page reset, stale response suppression, double-submit prevention, keyboard scanner/manual input, upload fallback, permission denied, conflict/retry/success states.
- Upload demo: accept image file, FE decode QR/barcode for user and book, decoded string populates the right input, malformed/unsupported/no-result image stays client-side and shows retry/manual fallback; no image is sent to BE.
- Storybook states for borrow, return, renew, lost, reservation, provisional/fixed fine and forbidden/error.
- Browser evidence for staff circulation and borrower self-service at desktop/mobile sizes; camera/live DB evidence only when actually run.

Commands are finalized from BE/BaiTap-RS/build.gradle.kts, FE/package.json and the approved gate policy. At minimum, focused BE tests, Checkstyle/PMD/build as approved, FE test/lint/build and any MySQL/browser profile must be reported separately as PASS, FAIL, NOT RUN or BLOCKED. This planning turn runs none of them.

## 11. Gates, risks and expected output

| Gate                            | Owner           | Current state (2026-10-09) | Required evidence                                             |
| ------------------------------- | --------------- | --------------------- | ------------------------------------------------------------- |
| F1 scope/compatibility          | User/PO         | APPROVED              | Baseline, scope, configurable policy and all-or-nothing borrow approved |
| F8 disposable MySQL concurrency | BE/QA           | APPROVED TO RUN       | Disposable MySQL 8.0.46 evidence; no target DB writes         |
| F8 production target proof      | User/DB + BE    | NOT VERIFIED          | Exact target version/head and target-compatible evidence      |
| F9 API/error/audit/notification | BE/FE           | FROZEN FOR SLICE      | Contract in §12.1; IN_APP/SSE only, email retry excluded      |
| Policy editor authorization     | User/PO         | APPROVED              | ADMIN + LIBRARIAN numeric policy read/update                   |
| BE/FE implementation approval   | User            | APPROVED              | User message dated 2026-10-09                                   |
| Validation                      | BE/FE/QA        | IMPLEMENTED — VALIDATION PARTIAL | Approved scoped gates PASS. Full BE test, `pmdTest`, and scanner processing are user-SKIPPED; target DB and hard-crash recovery remain NOT VERIFIED. See §13. |

Key risks: race conditions, deadlocks, partial multi-copy results, stale policy dates, fine idempotency, notification duplication, role/ownership leakage, and migration conflicts with an unknown target head. Mitigation is deterministic lock order, DB-level invariant, policy snapshot, idempotent keys, scoped errors, explicit audit and target-DB tests.

The approved implementation now provides a reviewable circulation workspace and API whose loan/copy/patron/fine/reservation transitions have focused unit and disposable-MySQL evidence. Target-environment compatibility and the explicitly skipped/unverified gates remain outside that evidence.

Wireframe review artifact: document/wireframes/fe/library/098-library-circulation/README.md. It is a static/offline prototype only and does not prove API, DB, browser or runtime authorization.

## 12. Approval request

> **Superseded by the approval amendment above:** the user has approved the baseline, scope, all-or-nothing borrow default, and implementation start. The remaining questions below are scoped implementation gates and do not revoke that approval.

Đề nghị người dùng xác nhận:

1. Application baseline v5 và phạm vi Plan 098 ở trên.
2. Borrow multi-copy mặc định all-or-nothing.
3. Cho phép policy editor thay đổi toàn bộ numeric limits/rates nêu tại §2.1, với snapshot/version/audit.
4. Cho phép bắt đầu implementation chỉ sau khi F8/F9 được đóng ở mức cần thiết và plan được phê duyệt rõ ràng.

## 12.1. Approved implementation contract amendment — 2026-10-09

This amendment records the implementable contract choices for the approved slice. It does not claim target MySQL compatibility or runtime validation.

### Routes and payloads

Use the proposed `/api/v2` paths in §7; source scan found no exact method/path collision. Do not move the existing catalog, patron or card endpoints. Success remains inside the existing `RestResponse` envelope (`statusCode`, `error`, `message`, `data`). List routes use the existing `/api/v2` `ResultPaginationDTO` (`meta: {page, pageSize, totalPages, totalItems}`, `result`), not the v3 page DTO.

| Operation | Request | Response payload |
|---|---|---|
| `POST /api/v2/loans` | `{patronId, cardNo, copyBarcodes[]}` | `{items: LoanDTO[]}`; all-or-nothing |
| `POST /api/v2/loans/{loanId}/renew` | `{}` | `LoanDTO` |
| `POST /api/v2/returns` | `{copyBarcodes[]}` | `{items: [{loan: LoanDTO, fine: FineDTO|null}]}` |
| `POST /api/v2/reservations` | `{bookId}` | `ReservationDTO` |
| `POST /api/v2/reservations/{reservationId}/cancel` | `{}` | `ReservationDTO` |
| `POST /api/v2/book-copies/{barcode}/lost` | `{reason}` | `{loan: LoanDTO, fine: FineDTO}` |
| `POST /api/v2/fines/{fineId}/pay` | `{reference}` | `FineDTO`; full payment only |
| `POST /api/v2/fines/{fineId}/waive` | `{reason}` | `FineDTO`; full waive only |
| `GET /api/v2/fines` | query `patronId?, status?, page?, pageSize?` | paged `FineDTO[]`; owner or staff scope |
| `GET /api/v2/library/policies/circulation` | — | `PolicyDTO`; ADMIN/LIBRARIAN |
| `PUT /api/v2/library/policies/circulation` | policy fields below + `{expectedVersion, effectiveAt}` | `PolicyDTO` |
| `POST /api/v2/library/batch-jobs/overdue-fine` | `{runDate: YYYY-MM-DD}` | `{jobExecutionId, status, runDate}` |

`LoanDTO` fields: `loanId, patronId, copyId, copyBarcode, bookId, bookTitle, cardNo, status, borrowedAt, dueAt, returnedAt, lostAt, renewCount, policyVersion`. `FineDTO` fields: `fineId, loanId, type, status, amount, currency, calculatedThrough, policyVersion, provisional, paidAt, paymentReference, waivedAt, waiveReason`. `provisional=true` only for an active-loan overdue estimate; it becomes false when RETURNED/LOST fixes the amount, and FE disables pay/waive while true. `ReservationDTO` fields: `reservationId, bookId, bookTitle, patronId, status, reservedAt, readyAt, pickupDueAt, allocatedCopyBarcode, fulfilledAt, cancelledAt, policyVersion`. Timestamps use ISO-8601 with offset; VND amounts are represented as decimal strings at the FE boundary.

`GET /api/v2/loans` filters `patronId, status, dueBefore, page, pageSize`; `GET /api/v2/reservations` filters `bookId, status, patronId, page, pageSize`; `GET /api/v2/fines` filters `patronId, status, page, pageSize`. Patron self scope is derived from authentication; staff can filter by patron. `GET /api/v2/fines/{fineId}` returns `FineDTO`; `GET /api/v2/library/policies/circulation` returns `PolicyDTO`; `GET /api/v2/library/batch-jobs` returns a bounded latest-50 `List<LibraryBatchRunDTO>` with no pagination query.

**Batch history contract clarification — 2026-10-09:** source-to-FE contract review confirmed that batch history is a latest-50 list, not a paged response. This corrects the earlier paged-metadata wording in this amendment; it does not change the approval for the batch feature or add a page-query contract.

`PolicyDTO` contains `policyVersion, effectiveAt, maxActiveLoans, loanDurationDays, maxRenewals, renewalDurationDays, reservationPickupDays, fineTiers[{throughDay|null, dailyRate}], fineCapPerLoan, fineSuspensionThreshold`. Policy update also includes those configurable fields, `expectedVersion` for optimistic conflict, and `effectiveAt`; validate positive values, strictly increasing tier cutoffs/rates, and nonnegative cap/threshold. The API never rewrites old loan, reservation, or closed fine snapshots.

### Authorization, error, audit, and notifications

- Borrow/return/lost and staff reads: `ADMIN` or `LIBRARIAN`. Owner history/fine/reservation reads and own reservation create/cancel use server-side `authentication.userId -> library_patron.userId` ownership. Fine payment: `ADMIN` or `LIBRARIAN`; waive: `ADMIN`. No request-supplied actor ID.
- Error responses preserve the existing `RestResponse` fields and add a stable Library `code` through a scoped Library error adapter; do not change the global envelope for legacy callers. Keep existing statuses/codes in §7; FE uses `code`, never parses `message`. Map active-copy race to `409 COPY_ALREADY_ON_LOAN`, max-policy race to `409 MAX_ACTIVE_LOANS`, and do not expose SQLState/constraint/stack details.
- Reuse `LibraryOperationAuditService`/`AuditLog`: actor from `AuditContext.currentUserId()`, action, target type/id, before/after JSON, request ID and IP; retain domain transition timestamps. Payment stores full amount plus reference and actor/time; waive stores reason and actor/time.
- Notification events persist an IN_APP notification in the same database transaction as the circulation/fine transition; SSE inbox refresh is after commit. Persist a unique event identity in the circulation/fine schema (reservation transition or loan/runDate reminder key) so retries cannot duplicate the event. Email sending and automatic retry are outside this slice: the current email publish path runs before commit and exposes no retry contract. Do not add a second mail/outbox subsystem under this amendment.
- Numeric policy read/update is `ADMIN` and `LIBRARIAN`; the user explicitly resolved this policy-writer authorization on 2026-10-09. Policy updates carry `expectedVersion`, effective time, actor/time audit and immutable version history.

### F8 implementation boundary

The active-copy database invariant remains mandatory. Implement migration V32 from source V31 using a MySQL generated nullable active-copy key with a unique constraint, plus patron-row serialization and policy-version snapshot for the effective loan limit. Run migration and concurrency proof on a disposable MySQL Community 8.0.46 instance rooted under `/tmp`; do not write the configured target DB. This evidence may validate implementation DDL on the disposable version, but does not close production-target compatibility because target version/applied head remain unknown. Report disposable DB evidence separately from production target `NOT VERIFIED`.

### Approved validation amendment — 2026-10-09

The user approved 45 narrowly scoped PMD `ImmutableField` exceptions on JPA-mapped Plan 098 fields, because persistence-provider field access/population is incompatible with declaring those mapped fields `final`. Exceptions are field-local and documented only for `LibraryBatchRun` (3), `LibraryCirculationPolicy` (12), `LibraryFine` (4), `LibraryLoan` (9), `LibraryLoanRenewal` (8), `LibraryPolicyFineTier` (3), and `LibraryReservation` (6); the policy tier collection reference is provider-managed and mutated in place. This does not waive other PMD findings: `pmdMain` remains required and all non-approved violations must be fixed. QA must also verify through a policy update that an existing loan and renewal keep their persisted version/date/duration snapshots and that the new version becomes effective exactly at its configured boundary.

## 13. Validation Result — 2026-10-09 (resumed, final scoped snapshot)

Overall status: **IMPLEMENTED — approved scoped gates PASS; validation PARTIAL by explicit user waiver and environment boundary.** Fresh backend gates and disposable-MySQL probes used the final service/entity/repository source. Full BE test and `pmdTest` were not run by user instruction. The user also temporarily skipped QR/barcode feature processing tests; scanner processing is reported `SKIPPED (user)`. No result here proves the unknown target DB or abrupt process-death recovery.

| Evidence area | Result | Evidence and boundary |
|---|---|---|
| Focused backend unit tests | PASS | Fresh run: 26 tests in 14 Plan 098 test suites, 0 failures/errors/skips. JaCoCo report generated from this run: `BE/BaiTap-RS/build/reports/jacoco/test/jacocoTestReport.xml` and `html/index.html`. Tests were refreshed for the final borrow/return/renewal/reservation collaborator splits and include return orchestration. |
| `pmdMain` | PASS | Fresh round 14 completed with no findings. The exact 45 user-approved, field-local `PMD.ImmutableField` exceptions remain; no new suppression was added. Full `pmdTest` remains skipped below. |
| Checkstyle main/test | PASS with warnings | Fresh `checkstyleMain checkstyleTest` exited 0. Reports show 1,037 main warnings across 951 files (53 in 23 Plan 098 production files) and 415 test warnings across 183 files (37 in 13 Plan 098 test files). Warning severity is configured; warnings did not fail the tasks. |
| Backend build | PASS | `build -x test -x pmdTest` passed, including compile, assemble, `pmdMain`, Checkstyle, and `check`. Compile emitted 10 Spring Batch `JobLauncher` deprecation/removal warnings. |
| Disposable MySQL migrations and schema | PASS | MySQL Community 8.0.46 under `/tmp`: clean schema migrated V1–V32 and upgrade schema migrated V1–V31 then V32. `active_copy_id` generated column and its unique index each appear exactly once. Read-only audit confirmed each of the seven configurable policy columns occurs exactly once. Logs: `/tmp/plan098-runtime-resume/final-schema-audit.log`, `/tmp/plan098-runtime-resume/policy-columns-audit.log`. No target DB was contacted. |
| Hibernate bootstrap and repository metadata | PASS | Final compiled application context initialized against a fresh disposable schema with `ddl-auto=validate`; repository queries resolved and `BookCopyLockQueries` registered. Log: `/tmp/plan098-runtime-resume/bootstrap-final.log`. |
| Disposable MySQL service acceptance | PASS | Against final compiled classes: 10-way same-copy race yielded one success; near-cap race yielded one success and one `MAX_ACTIVE_LOANS`; concurrent returns allocated FIFO reservations to distinct copies; mixed valid/missing multi-copy borrow rolled back loan/copy/audit/notification; policy edit preserved persisted loan and renewal snapshots and switched at boundary−1µs/exact boundary; expired READY reservation became EXPIRED and copy AVAILABLE; notification rollback/retry remained idempotent; failed batch after a committed 100-item chunk and 26 injected notification failures restarted from checkpoint without duplicates, with 126 expected totals. `SkipLimitExceededException` was deliberately injected. Logs: `/tmp/plan098-runtime-resume/service-final.log`, `/tmp/plan098-runtime-resume/final-db-audit.log`. |
| Abrupt batch process-crash recovery | NOT VERIFIED | FAILED-job checkpoint restart passed. A hard process death leaving Spring Batch metadata `STARTED` and its recovery semantics were not exercised. |
| Full backend test and `pmdTest` | SKIPPED (user) | Explicitly waived for this resume; focused tests and `pmdMain` were run separately. |
| Frontend lint/build/Storybook | PASS | `npm run lint`, `npm run build`, and `npm run build-storybook` passed on unchanged FE source. Storybook reported existing PrimeVue package lookup and large-chunk warnings; command exited 0. Logs: `/tmp/plan098-fe-lint.log`, `/tmp/plan098-fe-build.log`, `/tmp/plan098-fe-storybook.log`. |
| Frontend focused tests | PASS, historical selection | The earlier refreshed selection passed 115 tests across 3 files, including scanner mock tests. It predates the user's later waiver and is not current scanner-processing evidence. Log: `/tmp/plan098-fe-focused-tests-all.log`. |
| QR/barcode feature processing and native decode | SKIPPED (user) | User temporarily skipped QR/barcode feature processing tests. No browser/native QR or Code128 decode is claimed. |
| Production target DB | NOT VERIFIED | Exact target DB version and applied Flyway head remain unknown. No target credentials or writes were used. |

The resumed validation ended after round 14. Historical PMD failures in rounds 11–13 were repaired; the final round passed. All QA Gradle and disposable MySQL processes have settled; no commit, push, or deployment was performed. Keep target DB compatibility and abrupt `STARTED` batch recovery open for future evidence; scanner processing remains skipped per user instruction.
