# Plan 098 — Backend Unit Test Plan

Status: design-only, implementation approval received; source-level test targets provisional until F8/F9 contract decisions are frozen. This document does not claim MySQL, API, or concurrency evidence.

## Scope and test boundary

Cover the backend circulation, reservation, fine, policy, audit, and batch behavior in Plan 098. Keep production rules in domain/services; test controllers only for request validation, authorization, status/envelope, and route contract. Test scanned payloads as decoded text; no image/file is sent to backend. Do not modify tests in this planning stage.

## Provisional test targets

Exact class/method names follow the frozen F9 DTO and route contract. Expected service-level targets are:

- `LibraryLoanService.borrow`: eligible patron/card, multiple available copies, duplicate/empty barcode list, missing/reference/withdrawn/lost/unavailable copy, suspended/closed/ineligible patron, invalid card, max-loan boundary, audit failure rollback, and all-or-nothing rollback if any requested copy fails.
- `LibraryLoanService.renew`: valid renewal and due-date extension from current due date; expired/revoked card or suspended patron; closed/lost loan; renewal limit; queue priority conflict; policy changes leave prior snapshots intact.
- `LibraryReturnService.returnCopies`: successful active-loan resolution from barcode without patron scan, not-found/no-active-loan, repeated return, as-of-return fine calculation, reservation allocation, and audit failure rollback.
- `LibraryLostService.markLost`: successful loss transition, repeat/lost/closed rejection, list-price-plus-policy charge, overdue cutoff at loss date, no later accrual, and transactional rollback across loan/copy/fine/audit writes.
- `LibraryReservationService.create/cancel/allocate/expire`: self-ownership and eligibility, duplicate active request, deterministic FIFO and stable tie-break, READY pickup window, expiry boundary in Asia/Ho_Chi_Minh, cancellation/fulfillment history, and suspension behavior.
- `LibraryFineCalculator.calculate`: parameterized tier boundaries (0, 1, 7, 8, 30, 31, cap boundary, above cap), configured tier count/rates/cap, exact `BigDecimal` values, overdue and lost components.
- `LibraryFineService.pay/waive/recompute`: full offline payment only, authorize staff/Admin boundary, invalid amount/reference, only fixed fine settles, waive authorization, duplicate/idempotent requests, and PAID/WAIVED preservation.
- `LibraryPolicyService.update/resolve`: valid and invalid limits/tier ranges/effective times, actor/version audit, concurrent version conflict, effective policy selection, and snapshot immutability for existing loans/reservations/fines.
- `LibraryOverdueFineJob`: deterministic chunk ordering, run-date repeat idempotency, older-date non-decrease, bounded skip and metrics, restart behavior, and suspension recomputation without clearing another suspension reason.
- `LibraryCirculationController`, `LibraryReservationController`, `LibraryFineController`, `LibraryPolicyController`, and `LibraryBatchJobController`: frozen route/request/response contract, malformed input, auth/ownership matrix, status/error code/envelope, and no entity serialization.

## Fixtures and isolation

Use fixed `Clock` and an explicit `Asia/Ho_Chi_Minh` zone for date-boundary cases. Use service-level fakes/mocks for repositories, policy resolver, card/patron eligibility, audit, and notification boundary. Assert calls and persisted state transitions, including that external notification delivery does not occur before transaction commit if the frozen F9 integration contract requires an after-commit/outbox boundary. Keep fine calculations as pure parameterized tests. Use MVC tests with mocked services for HTTP contract and security. MySQL-specific uniqueness/locking, migrations, and multi-thread contention require the exact compatible MySQL Testcontainers profile and remain separate from unit evidence.

## Assertions

For successful commands, assert returned DTO, loan/copy/reservation/fine state, exact timestamps and policy snapshots, audit actor/reason/reference, and expected interactions. For rejected commands, assert stable `Library` machine code and no partial state change. For rollback cases, verify all involved repository writes/audit events are absent or transaction rollback is demonstrated by the approved integration harness. For batch retries, assert stored totals and terminal fine states remain stable.

## Regression and validation

Cover unchanged catalog, patron/card, shared security, and global error-envelope behavior through focused regression tests where the touched boundary requires it. Run focused BE tests, Checkstyle, required `pmdMain`, and build after implementation. `pmdTest` and full test suite are explicitly waived by the user; report them as `SKIPPED` if the QA summary includes waived gates. PMD main is not waived.

If JaCoCo is configured for the focused test task, inspect the report for the changed circulation/reservation/fine/policy service classes and report class-level instruction/branch coverage available from that report. Do not claim a new numeric threshold; the Plan 098 draft has no confirmed feature-specific threshold.

## Resolved implementation gates

F8 authorizes a disposable MySQL 8.0.46 instance for migration, locking, and restart proof. The production target version and Flyway head remain unverified; no target database writes are in scope. F9 is frozen for approved routes and DTOs, stable Library error codes, authenticated audit actor, ADMIN/LIBRARIAN policy editing, and transactional IN_APP notification persistence with after-commit inbox refresh. Manual Spring Batch runs persist the launch actor for restart. Full tests and `pmdTest` are waived by the user; focused tests, `pmdMain`, Checkstyle, compile, and build remain required.

The user separately authorized field-local `@SuppressWarnings("PMD.ImmutableField")` on exactly 45 PMD-reported persistent JPA fields whose identity or policy snapshot values are immutable after construction. Each annotation documents the Jakarta JPA non-final-field constraint and domain immutability rationale. No class-level suppression or suppression of another rule is authorized.

## Frozen F9 API contract

Routes: `POST/GET /api/v2/loans`, `POST /api/v2/loans/{loanId}/renew`, `POST /api/v2/returns`, `POST/GET /api/v2/reservations`, `POST /api/v2/reservations/{reservationId}/cancel`, `POST /api/v2/book-copies/{barcode}/lost`, `GET /api/v2/fines/{fineId}`, `POST /api/v2/fines/{fineId}/pay`, `POST /api/v2/fines/{fineId}/waive`, and `GET/POST /api/v2/library/batch-jobs[/overdue-fine]`.

Request bodies: borrow `{patronId, cardNo, copyBarcodes[]}`; renew `{}`; return `{copyBarcodes[]}`; reservation `{bookId}`; cancel `{}`; lost `{reason}`; pay `{reference}` (full payment); waive `{reason}`; manual overdue run `{runDate}`.

`LoanDTO`: `loanId, patronId, copyId, copyBarcode, bookId, bookTitle, cardNo, status, borrowedAt, dueAt, returnedAt, lostAt, renewCount, policyVersion`. Borrow response is `{items: LoanDTO[]}`. Return response is `{items: [{loan: LoanDTO, fine: FineDTO|null}]}`. `FineDTO`: `fineId, loanId, type, status, amount, currency, calculatedThrough, policyVersion, provisional, paidAt, paymentReference, waivedAt, waiveReason`. `provisional` is true for active-loan overdue estimates and false after return/lost fixes the amount. `ReservationDTO`: `reservationId, bookId, bookTitle, patronId, status, reservedAt, readyAt, pickupDueAt, allocatedCopyBarcode, fulfilledAt, cancelledAt, policyVersion`. Paged response uses `ResultPaginationDTO(meta:{page,pageSize,totalPages,totalItems}, result)` inside `RestResponse`.

Policy read/write uses `/api/v2/library/policies/circulation`; `fineTiers` entries are `{throughDay|null,dailyRate}` with `expectedVersion` optimistic concurrency and scheduled `effectiveAt`. ADMIN and LIBRARIAN may edit policy. Batch history is an array of `LibraryBatchRunDTO` with `runId,jobName,runDate,status,processedCount,skippedCount,startedAt,completedAt` and status `STARTED|COMPLETED|FAILED`.
