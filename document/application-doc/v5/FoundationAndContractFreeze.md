# Foundation and Contract Freeze — Plan 095

## Trạng thái

- Artifact: `INVENTORY_COMPLETE`; `FROZEN_FOR_SLICE` chưa đạt.
- Ngày khảo sát: 2026-10-08.
- Branch: `training/duyptk/student-management`.
- Commit khảo sát: `2170898356ed5035eaafa67c3dd2d6dc9a75dac6`.
- Plan: [`095-v5-foundation-contract-freeze-2026-10-08.md`](../../dev-impl-plan/summary/095-v5-foundation-contract-freeze-2026-10-08.md), `COMPLETE — inventory/documentation`; it does not indicate a completed contract freeze or implementation approval.
- Baseline nguồn: v5 `0.1-draft`, implementation approval `PENDING`.
- Source adaptation: [`RequirementBaseline.md`](RequirementBaseline.md), [`standalone-library-to-school-platform.md`](requirement-adaptation/standalone-library-to-school-platform.md), dựa trên bản training độc lập `/home/duyptk/Downloads/library_training_spec.html`. Bản HTML chỉ là nguồn adaptation, không phải policy override của v5.
- Không có API implementation, migration, DB/runtime write hoặc provider call trong Plan 095.

## Quy tắc đọc kết quả

Evidence được gắn loại: `DOC` = tài liệu; `CODE` = source/config trên commit nêu trên; `TEST` = kết quả test đã chạy; `RUNTIME/DB` = quan sát target runtime/database; `UNKNOWN` = chưa có evidence. Source code không chứng minh runtime state. Các quyết định người dùng dưới đây được phê duyệt cho phạm vi core slice và có precedence so với clause v5 mâu thuẫn khi lập slice. Chúng không phê duyệt toàn bộ v5 hoặc implementation; đồng bộ baseline/CR sau là công việc tài liệu, không phải yêu cầu xin duyệt lại các quyết định này.

## Inventory F7–F9

### F7 — routes, migration, database và platform integration

| Fact | Evidence | Trạng thái và ý nghĩa |
|---|---|---|
| API hiện hữu dùng đồng thời `/api/v1`, `/api/v2`, `/api/v3`; có nhiều controller `/api/v2` như academic, student, teacher, calendar, enrollment và scorebook. | `CODE`: [StudentV2Controller.java](../../../BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/controller/StudentV2Controller.java#L30), [EnrollmentController.java](../../../BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/enrollment/controller/EnrollmentController.java#L36), [ScorebookController.java](../../../BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/controller/ScorebookController.java#L29). | Xác nhận API v2 là namespace dùng chung, không thể suy collision chỉ từ prefix. |
| Targeted controller mapping search cho các resource path v5 bên dưới không trả về match exact trên commit khảo sát. | `CODE`: path-string search trong `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/**Controller.java` cho listed resource bases. | Đây là source search theo resource base, không phải inventory đầy đủ method/path sau class-level mapping. Method-level collision map phải lập lại trên implementation target trước mỗi API slice. Giữ rule baseline: chỉ namespace group bị collision/ambiguous. |
| V5 path map gồm các top-level path và một số path Library namespace. | `DOC`: [v5 README API map](README.md#api-versioning-rule), [frontend API map](frontend-api/README.md). | Proposed allocation, chưa phải deployed contract. Không đổi đồng loạt sang `/api/v2/library/**`. |
| Flyway source có migration `V1` tới `V29`; source head hiện tại là `V29__create_timetable_agent.sql`. | `CODE`: [V29 migration](../../../BE/BaiTap-RS/src/main/resources/db/migration/V29__create_timetable_agent.sql#L1); migration directory `BE/BaiTap-RS/src/main/resources/db/migration/`. | Source head xác nhận `V29`. Applied DB migration head là `UNKNOWN`. Allocate next migration only after target branch/head scan. |
| Datasource default là MySQL; Flyway locations dùng migration classpath hiện hữu. | `CODE`: [application.properties](../../../BE/BaiTap-RS/src/main/resources/application.properties#L7), [Flyway config](../../../BE/BaiTap-RS/src/main/resources/application.properties#L16). Gradle includes Flyway MySQL and MySQL Connector/J at [build.gradle.kts](../../../BE/BaiTap-RS/build.gradle.kts#L86). | MySQL family confirmed in source. Exact MySQL/Azure MySQL major/minor and runtime target are `UNKNOWN`; no DB/cloud query was made. |
| Database target name | `USER-REPORTED`: `java_coban` is the current local-only target; it has not been pushed to the existing production database. The source default is also `jdbc:mysql://localhost:3306/java_coban` ([application.properties](../../../BE/BaiTap-RS/src/main/resources/application.properties#L7)). | Target name is recorded, not a live DB connection result. Exact server MySQL version and applied Flyway head remain `UNKNOWN`; no production DB was queried or changed. |
| Security allows selected auth/docs/actuator paths anonymously; all other requests require authentication. | `CODE`: [SecurityConfiguration.java](../../../BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/config/SecurityConfiguration.java#L65). | New route still needs method/ownership authorization. Role grant/revoke flow is user decision, but role source/runtime assignment not inventoried as live state. |
| Shared notification and audit integration exist in source. | `CODE`: notification package and `common/util/AuditUtil.java`; `DOC`: [IntegrationBoundaries.md](contract/IntegrationBoundaries.md#3-notificationmail), [Authorization.md](contract/Authorization.md#audit). | Reuse is baseline. Atomic notification/outbox gap and sufficient audit fields require implementation-plan evidence. |

#### Proposed REST route allocation to compare during slice planning

| Methods/resources in v5 | Proposed base path | Collision scan on surveyed source |
|---|---|---|
| Catalog reads/mutations | `/api/v2/books` | Proposed baseline resource path; no exact controller match in targeted source search |
| Copy create/update and barcode | `/api/v2/book-copies` | Proposed baseline resource path; no exact controller match in targeted source search |
| Patron administration and self-service | `/api/v2/library-patrons` | Proposed baseline resource path; no exact controller match in targeted source search |
| Card issue/revoke/verify/render | `/api/v2/library-cards` | Proposed baseline resource path; no exact controller match in targeted source search |
| Loan create/read/renew | `/api/v2/loans` | Proposed baseline resource path; no exact controller match in targeted source search |
| Return | `/api/v2/returns` | Proposed baseline resource path; no exact controller match in targeted source search |
| Reservation create/cancel/read | `/api/v2/reservations` | Proposed baseline resource path; no exact controller match in targeted source search |
| Fine read/pay/waive | `/api/v2/fines` | Proposed baseline resource path; no exact controller match in targeted source search |
| Library batch jobs | `/api/v2/library/batch-jobs` | Proposed baseline resource path; no exact controller match in targeted source search |
| AI endpoints (deferred) | `/api/v2/library/ai/*` | Proposed baseline resource path; no exact controller match in targeted source search |

The table is only the proposed v5 resource allocation, not the concrete route/request/response contract. Method-level route inventory and collision decisions are deferred with API contract planning; repeat the scan on the target commit before implementation. If a collision or semantic ambiguity exists, namespace only that resource group under `/api/v2/library/{resource}` per v5 rule.

### F8 — database-level uniqueness and concurrency

| Invariant | Current design evidence | Gate status |
|---|---|---|
| A copy has at most one active loan. | User-approved invariant; [`MigrationAndConcurrency.md`](data-model/MigrationAndConcurrency.md#1-active-loan-invariant) proposes generated nullable active-copy key + unique constraint as design direction. | Invariant `APPROVED`; concrete DDL/strategy `BLOCKED` until exact target MySQL version is verified. Prior option is a proposal, not a selected DDL. |
| A patron has at most one ACTIVE card. | User-approved invariant and canonical table name `library_card`; [`MigrationAndConcurrency.md`](data-model/MigrationAndConcurrency.md#5-card-active-uniqueness) lists generated nullable unique key, patron-row lock, or current-card pointer as alternatives. | Invariant/name `APPROVED`; no exact DDL alternative is approved. Select and verify on exact MySQL target before implementation. |
| Patron has at most five active loans, including simultaneous borrowing of different copies. | User-approved invariant; `BR-V5-LIB-CIRC-001`; transaction flow in [`MigrationAndConcurrency.md`](data-model/MigrationAndConcurrency.md#3-transaction-flow). | Invariant `APPROVED`; patron-row serialization is design direction, not a frozen implementation. Test parallel attempts across distinct copies; one-copy race test alone is insufficient. |
| LOST ends active loan. | User-approved invariant; current source spec describes lost/return state in [CirculationAndReservation.md](modules/03-CirculationAndReservation.md#lost). | `APPROVED`; implementation needs an explicit active-loan predicate/status; do not use `returned_at IS NULL` alone as active predicate. |
| DB evidence | `AC-V5-LIB-002` requires 10 simultaneous requests against one copy: exactly one succeeds; [`MigrationAndConcurrency.md`](data-model/MigrationAndConcurrency.md#6-test-database) excludes H2 as proof for MySQL generated uniqueness, lock/upsert/concurrency. | Required test plan: exact production-major-compatible MySQL container/image; card issue race for same patron; max-five race across distinct copies; verify conflict codes and final rows. Tests were not run in Plan 095. |

### F9 — module ownership, errors, audit and notification

| Concern | Existing convention/evidence | Decision needed before slice |
|---|---|---|
| Ownership and access | `SecurityConfiguration` requires authentication; method security is used in controllers. V5 resolves `authenticated userId → library_patron.userId` server-side ([Authorization.md](contract/Authorization.md#ownership)). | Apply owner resolution server-side; UI visibility is not authority. Verify effective role/user records on target at implementation time. |
| Error envelope | `RestResponse` fields are `statusCode`, `error`, `message`, `data`; `error` is a title and `message` is human-readable (`BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/dto/RestResponse.java:3-23`). `GlobalExceptionHandler` maps exceptions to status/title/message (`BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/error/GlobalExceptionHandler.java:15-53`). | Stable Library code required by `NFR-V5-LIB-005`. Proposal: additive `code` field, preserve `error`/`message`, typed domain exception/code mapping; do not overload legacy `error`. |
| FE error handling | `FE/src/services/apiClient.ts:21-28,71-90,140-150` parses human messages but not machine code; `FE/src/types/api.ts:25-50` has no code member. | Proposed FE adapter preserves `ApiError.code`, branches on code, maps user-facing text locally; legacy message parsing unchanged. Regression coverage must prove compatibility and safe 5xx handling. This is design only, not implementation approval. |
| Audit | V5 requires actor/time on circulation mutations and sensitive mutations; repo has `AuditUtil` and domain records with `createdBy` patterns. | Define fields for actor, event, target, timestamp, reason/reference, and before/after or state transition as needed. Confirm whether generic audit metadata suffices; don't add domain log until gap is shown. |
| Notifications | Reuse notification/email infrastructure; existing v5 boundary says new outbox only after atomicity gap analysis ([IntegrationBoundaries.md](contract/IntegrationBoundaries.md#3-notificationmail)). | Reservation READY notice is core. Decide transactional event/outbox behavior and fallback/retry/idempotency. Overdue reminders remain per mandatory fine/batch baseline; optional expiry reminders may be deferred. |
| FE routing/role view | v5 proposes `/library/*`; no existing Library route found in FE route source scan on this commit. Backend `UserService` exposes roles (`user/service/UserService.java:85-88`); FE `UserSummary.roles` exists (`FE/src/types/user.ts:15-29`) but its `UserRole` union does not include `LIBRARIAN`. | Extend the FE role type only under approved contract. Menu visibility remains UX; backend authorization remains authoritative. Re-scan route tree before UI slice. |

## Approved user decisions and amendment precedence

The following decisions were supplied explicitly during Plan 095 coordination. They are authoritative for the approved core-first direction and supersede conflicting v5 draft clauses for slice planning. This artifact records the amendments without silently editing `RequirementBaseline.md`; baseline/CR synchronization is still required for discoverability and does not reopen the approved policy choices.

| Decision | Recorded rule | Baseline relationship / unresolved details |
|---|---|---|
| Delivery order | Core first: catalog, patron/card, circulation/reservation and fine. | AI-001/002 MUST plus AI SHOULD/BONUS are deferred to a later plan/release; keep all baseline IDs and security/retention guardrails. |
| Role management | ADMIN only can grant/revoke `LIBRARIAN`; audit each change. | Resolves F2/TBD-001 for this slice. No self-service role assignment. |
| Borrower eligibility (Plan 097 approved clarification) | Any user with eligible patron may borrow except users whose single role is `ADMIN` or `LIBRARIAN`. Reuse role enum/shared constants; no hardcoded role strings. | Approved user decision supersedes the earlier Student/Teacher restriction and multi-role interpretation for Plan 097. Role cardinality/runtime assignment remains to be verified separately; do not infer DB state from source. Plan 098 owns borrow/renew/reservation enforcement. |
| Suspension | Blocks new borrow, renewal and new reservation; permits read, return and reservation cancellation. | Record as explicit Library policy. Do not disable/lock `app_user`. |
| Reservation | FIFO queue, allocate an eligible copy, move to READY, notify, then expire and advance to next eligible patron. | Pickup deadline = READY date + configured duration (default 3 calendar days); expiry at start of following day in `Asia/Ho_Chi_Minh`. Cutoff is configurable/audited/versioned for new transactions. |
| Fine payment, waiver and display | Fine shown for an active overdue loan is provisional; payment/waiver becomes available only after RETURNED or LOST, when its amount is fixed through the closing date. Offline full payment only; ADMIN/LIBRARIAN pay; ADMIN waives; audit actor/time/reference. No partial payment, refund or receipt PDF. | Approved for this slice. Fine date cutoff is resolved below. No correction/reversal workflow is included; correction policy remains OPEN for a later dedicated plan. Online gateway is excluded. |
| LOST handling | Close active loan; overdue fine accrues through lost date; create LOST_ITEM charge equal to book list price + 50,000 VND; no accrual after lost date. | Fine component representation and idempotency remain implementation-plan choices. |
| Catalog/card artifact | Store cover URL only. Generate barcode and QR/PNG on demand; no persisted cover upload/card PDF in core. | Canonical table/entity name `library_card` approved; `modules/02-LibraryPatronAndCard.md` uses conflicting `membership_card` and requires documentation alignment before implementation. |
| Scanner demo | Upload an image for scan/decode in core demo instead of live camera; preserve manual-code fallback. Decode in browser; server receives decoded text, not camera/image frames. | Approved replacement for mandatory `FR-V5-LIB-CODE-004` live-camera behavior. Barcode/QR generation and manual fallback remain core. |
| Date policy | ADMIN/LIBRARIAN may change audited/versioned policy for borrow duration, renewal duration, card validity, reservation pickup and expiry cutoffs. Store applied policy/version with each new transaction; edits do not rewrite existing due dates/accrual. Renewal is a new transaction using current policy version and adds configured duration to current due date. | Defaults: loan 14 days, renewal +7 days, card 12 months, pickup 3 calendar days; timezone `Asia/Ho_Chi_Minh`. Due date/card validity include deadline day; overdue starts next day; reservation expires at start of next day after pickup deadline. Max five loans, max two renewals and fine rates are not configurable under this decision. |

### Baseline synchronization and unresolved decisions

Plan 097 is approved with these clarifications: any user except ADMIN/LIBRARIAN may be eligible for borrowing, each user has exactly one role, role enum/shared constants must be reused, FE may calculate expiry from a month duration or accept a direct expiry date, and both modes send the expiry date to BE. Plan 098 owns all Loan/Return/Renewal/Lost and circulation transaction/copy-lock/due-date work. This is documentation synchronization; do not ask to reapprove decisions already recorded. Other unrelated baseline decisions remain untouched.

## Requirement traceability

Every baseline requirement ID in `RequirementBaseline.md` is mapped to its canonical contract/module and to an acceptance or decision path below. Range notation `001..005` expands to each sequential ID in that range. `Core` means approved first-slice direction, subject to the separate implementation approval gate; `Later` means user-directed deferral; `Open` means an additional business/technical decision is required. This is traceability, not evidence of implementation.

| ID | Owner / data / API | Auth / error / consumer | Validation / status |
|---|---|---|---|
| FR-V5-LIB-CAT-001..005 | `modules/01-LibraryCatalog.md`; `book`, `book_copy`; `/api/v2/books`, `/api/v2/book-copies` | LIBRARIAN/ADMIN mutations; stable domain codes; catalog FE | Core; catalog CRUD/search/pagination, barcode uniqueness, safe-delete and URL-only cover checks planned |
| BR-V5-LIB-CAT-001..004 | `data-model/README.md`; ISBN/barcode uniqueness, reference-only, history preservation | Backend database constraints; conflict code | Core; MySQL migration/integration plan required, F8 target OPEN |
| FR-V5-LIB-PATRON-001..002 | `modules/02-LibraryPatronAndCard.md`; `library_patron` → `app_user`; `/api/v2/library-patrons` | ADMIN/LIBRARIAN management; owner mapping for self; Patron FE | Core; role/patron ownership matrix follows approved borrower eligibility |
| FR-V5-LIB-CARD-001..004 | `modules/02-LibraryPatronAndCard.md`, `modules/05-BarcodeQrAndScanning.md`; proposed `library_card`; `/api/v2/library-cards` | Backend HMAC/state check; stable card codes; card/QR FE | Core; HMAC/tamper/expiry/revoke and active-card race planned; F8 strategy OPEN |
| BR-V5-LIB-PATRON-001..003; BR-V5-LIB-CARD-001..002 | `data-model/README.md`; identity FK, patron lifecycle, card active invariant | Backend owns state; fine suspension; no account lock | Core; FK/history, state-transition and uniqueness tests planned; approved amendments take precedence in core planning |
| FR-V5-LIB-CIRC-001..006 | `modules/03-CirculationAndReservation.md`; `loan`, `reservation`, `book_copy`, `fine`; `/api/v2/loans`, `/returns`, `/reservations`, `/book-copies/{barcode}/lost` | Librarian circulation actions; self reservation; stable domain codes; circulation/reservation FE | Core; borrow/return/renew/lost/FIFO lifecycle tests; fine contract must ship with circulation |
| BR-V5-LIB-CIRC-001..007 | `RequirementBaseline.md` §3; max five, configurable 14-day default, two renewals, copy uniqueness, eligibility, LOST formula, audit | Patron lock/transaction, LIBRARIAN exclusion, actor audit; error codes | Core; active-loan overdue amount is provisional until RETURNED/LOST; distinct-copy max-five race, single-copy race, date-policy snapshot, LOST idempotency planned; F8 OPEN |
| FR-V5-LIB-FINE-001..005 | `modules/04-FineAndBatch.md`; `fine`, shared Batch/notification; `/api/v2/fines`, `/api/v2/library/batch-jobs` | ADMIN/LIBRARIAN pay; ADMIN waive; job authorization; stable codes | Core under core-first direction; active-loan amount is provisional and payment/waive only after RETURNED/LOST; correction/reversal is OPEN for later plan; schedule/restart/idempotency/notification/actor tests planned, not run |
| FR-V5-LIB-FINE-006..007 | `modules/04-FineAndBatch.md`; optional card reminder / AI batch | Optional service and later AI consumer | Later/SHOULD; no core implementation |
| BR-V5-LIB-FINE-001..006 | `modules/04-FineAndBatch.md`; `BigDecimal`/DECIMAL, typed fine, unique loan/type, suspension source | Backend calculates exact amounts; no AI financial authority; stable errors | Core; calculator boundary, same runDate x3, old date, PAID/WAIVED preservation, suspension-source tests planned. Fine rates/caps are baseline business values, not made configurable by date-policy decision. |
| FR-V5-LIB-CODE-001..002; FR-V5-LIB-CODE-005 | `modules/05-BarcodeQrAndScanning.md`; barcode PNG and signed card QR on demand | Backend signs/verifies; scanner/card FE | Core; generation, signature, tamper, expiry, revoke tests planned |
| FR-V5-LIB-CODE-003; FR-V5-LIB-CODE-006 | `RequirementBaseline.md` §5 | Batch label export / handheld scanner | SHOULD/BONUS; defer as optional |
| FR-V5-LIB-CODE-004; BR-V5-LIB-CODE-001..003 | `modules/05-BarcodeQrAndScanning.md`; image decoder and HMAC payload | Browser decodes uploaded image; backend validates decoded text and does not receive image/camera frames; scanner FE | Approved image-upload demo replaces live camera; manual fallback remains. QR payload/security checks planned. |
| FR-V5-LIB-AI-001..002 | `modules/06-LibraryAI.md`; image metadata prefill / natural-language filter; Spring AI | LIBRARIAN catalog or authenticated catalog search; validated DTO, never SQL | Baseline MUST but user-directed later release; no core implementation. Retain FR IDs and validation acceptance in later plan. |
| FR-V5-LIB-AI-003..006 | `modules/06-LibraryAI.md` | Optional AI consumers | SHOULD/BONUS, all deferred from core |
| BR-V5-LIB-AI-001..007 | `modules/06-LibraryAI.md`; Spring AI abstraction, feature flag, no repository tools, structured validation, PII minimization | Later AI module; provider mocked in tests | Deferred with AI scope; retention `TBD-V5-LIB-005` remains OPEN; no provider/test called in Plan 095 |
| SEC-V5-LIB-001..005 | `contract/Authorization.md`; reuse JWT/Spring Security; patron identity map; LIBRARIAN role | Backend method/ownership checks; FE role UI is not authority | F2 and borrower/suspension policies approved for core slice; source has multi-role capability |
| COMP-V5-LIB-001..002 | `README.md` API version rule and F7 route map | Legacy `/api/v1`–`/api/v3` unchanged; namespace only actual collision/ambiguity | Core; route scan repeated on implementation target |
| NFR-V5-LIB-001..007 | `RequirementBaseline.md` §8; backend authority, MySQL, DB concurrency, batch idempotency, stable code, FE convention, secrets | Error adapter proposal; shared transport | Core/later by linked requirements; concurrency/error adapter plan required; no implementation in this artifact |
| AC-V5-LIB-001..007 | `RequirementBaseline.md` §8; service/fine/batch, database, FE/browser acceptance | Tests and evidence scoped to consumer | Planned only. Tests/browser/runtime not run; report each gate `NOT RUN` until executed in approved implementation slice |
| TBD-V5-LIB-001 | `contract/Authorization.md`; role administration | ADMIN grants/revokes with audit | Resolved for core slice; grant blocked until ADMIN resolves existing WAITING/READY reservations |
| TBD-V5-LIB-002 | `modules/03-CirculationAndReservation.md`; reservation state/queue | FIFO allocation and READY notification | Resolved: pickup due READY date + configurable duration (default 3 calendar days), expires at start of following day Asia/Ho_Chi_Minh; ADMIN resolves reservations before Librarian grant. |
| TBD-V5-LIB-003 | `modules/04-FineAndBatch.md`; offline fine payment/waive | ADMIN/LIBRARIAN pay, ADMIN waive, actor/time/reference; only RETURNED/LOST | Resolved for core slice; active overdue amount provisional until closing; due/card validity inclusive through deadline, overdue starts next day; no partial/refund/PDF; correction workflow remains later/open |
| TBD-V5-LIB-004 | Catalog/card artifact storage | URL cover, on-demand QR/PNG/barcode, no persisted PDF in core | Resolved for core direction; production storage need remains later decision |
| TBD-V5-LIB-005 | AI prompt/output metadata | Minimize PII; feature flag | OPEN and deferred with AI; retention decision required before AI release |

## Decision gates and slice readiness

| Gate | Status | Owner / evidence to close | Blocks |
|---|---|---|---|
| F1 — baseline/CR review and implementation scope | `OPEN` | v5 README/RequirementBaseline remain DRAFT/PENDING; record approved amendments in source docs and separately obtain slice implementation approval per workflow. Approved policy decisions in this artifact do not need reapproval. | All implementation |
| F2 — LIBRARIAN assignment | `APPROVED` | User decision: ADMIN-only grant/revoke + audit. Record in approved CR/baseline before role API/migration. | Platform foundation until amendment recorded |
| F3 — reservation lifecycle | `APPROVED` | User decided FIFO, eligible allocation, READY + notify, configurable 3-calendar-day default, expiry at start of next day Asia/Ho_Chi_Minh; block LIBRARIAN grant while WAITING/READY reservations exist and ADMIN resolves first. | Technical notification atomicity/retry plan remains under F9 |
| F4 — payment/waive/date cutoff | `APPROVED` | User decided offline full payment, pay actor ADMIN/LIBRARIAN, waive ADMIN, audit actor/time/reference, only after RETURNED/LOST; due/card inclusive deadlines, overdue next day, pickup expiry at start of next day. | Fine-state and idempotency design in implementation slice |
| F5 — cover/card artifacts | `APPROVED` | User decision: cover URL; QR/PNG and barcode on demand; no persisted cover/card PDF in core; canonical card table is `library_card`. | Align module wording before schema |
| F6 — AI privacy/retention | `DEFERRED` | Product/privacy owner: decide prompt/output retention before later AI slice. FR-AI-001/002 are deferred, not removed. | AI release only |
| F7 — branch routes/migration/database | `OPEN` | User reports local target name `java_coban`, not the existing production DB. Source head V29/MySQL config confirmed; DBA must confirm exact local server version and applied migration head. Method-level routes/request/response are deferred. | Migration and concrete API contract plans; runtime facts UNKNOWN |
| F8 — MySQL concurrency invariant | `BLOCKED` | Approved invariants: one active loan/copy, one active card/patron, max five active loans/patron, LOST ends active loan; canonical table `library_card`. Backend/DB owner must verify target/version and select/prove DDL plus patron serialization on MySQL. | Catalog/copy, card and circulation schema |
| F9 — API/error/audit/notification contract | `DEFERRED` | User deferred contract planning for legacy-compatible stable error codes, audit fields, notification retry/idempotency, concrete request/response/routes, policy/version and FE contract. Preserve as later plan; do not treat prior adapter/field proposals as approved. | Blocks affected API, audit, notification and FE implementation; reservation READY notifications are included. |

`OPEN` and `BLOCKED` are not approval. This artifact is `INVENTORY_COMPLETE` only. A later slice can be marked `FROZEN_FOR_SLICE` only when every blocking gate for that slice has an owner, dated decision/evidence, and no unresolved contract required to implement it.

## Proposed dependency slices

1. **Platform foundation** — role migration/ADMIN assignment workflow, authenticated identity-to-patron mapping, shared error/audit boundary. Depends F1/F2/F7/F9.
2. **Catalog and copies** — book/copy CRUD/search/barcode and DB copy uniqueness. Depends F1/F7/F8.
3. **Patron and card** — patron lifecycle, signed QR/PNG, `library_card` naming decision, active-card uniqueness and owner matrix. Depends F1/F2/F5/F7/F8.
4. **Circulation, reservation and core fine** — borrow/return/renew/lost, max-five/copy concurrency, configurable policy snapshot, offline fine/pay/waive, FIFO reservation/expiry/READY. Fine model/calculation ships with circulation because return/lost create fine state. Depends F1/F3/F4/F8/F9.
5. **Mandatory overdue Batch/notifications** — idempotent/restartable overdue job, required reminders and job audit/auth. Core under the user direction for fine; expiry reminders or other SHOULD may be separated after explicit scope review.
6. **Frontend core workflows** — catalog, patron/card, circulation, reservation/fines; uploaded-image scan demo plus manual fallback. The approved demo upload flow supersedes the draft live-camera clause for core-slice planning.
7. **Library AI later release** — FR-AI-001/002 MUST and AI SHOULD/BONUS deferred by user direction, retaining privacy, feature flag, structured validation, and no unrestricted DB access requirements.

## Validation performed for this artifact

- Source/controller route scan and branch/commit identification: `PASS` as repository-source inventory only.
- Source migration/config scan: `PASS` as repository-source inventory only; runtime database facts `UNKNOWN`.
- Requirement-ID crosswalk: `PASS` (all 68 baseline IDs mapped, including expanded sequential ranges).
- Automated unit/integration/full tests: `NOT RUN` (no implementation changed).
- Database, cloud, provider and browser/runtime checks: `NOT RUN`.
- Local Markdown link targets and heading anchors: `PASS`.
- `git diff --check`: `PASS` (whitespace only; not test or contract validation).
