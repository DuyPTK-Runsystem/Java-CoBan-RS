# Dev Note 097 — Library Patron & Card

Current status: **Plan 097 implementation and scoped acceptance complete** (`2026-10-09`). The user-approved `pmdTest` and full BE/FE suites remain **SKIPPED**; all executed focused gates and the isolated service/MySQL concurrency probe passed.

## Authorization and approved decisions

- [Plan 097](../../../dev-impl-plan/summary/097-library-patron-and-card-2026-10-08.md) is approved, including its approved clarifications: any user may borrow except ADMIN/LIBRARIAN; the current business model assumes one role per user; role checks reuse shared role types/constants.
- Card issue accepts either a validity duration in months or a direct expiry date. FE calculates the expiry date for duration mode and sends `expiresAt` in both modes; BE processes that date.
- Expiry uses inclusive `LocalDate` in `Asia/Ho_Chi_Minh`. FE calculates an inclusive end date: month addition clamps to the target month's last day, then subtracts one day from the anniversary.
- Plan 098 owns Loan, Return, Renewal, Lost, circulation transactions, copy locks, due dates, returns, renewals, and lost-book handling. Plan 097 does not implement these flows.

## Implemented scope

- BE adds patron activation, list/detail/current-user views, status and suspension audit, activation candidates, card issue/reissue/revoke/history/QR, verification, borrower eligibility, scoped authorization and V31 schema. Active-card uniqueness and patron uniqueness are enforced in MySQL; card signing uses HMAC.
- FE adds the patron list/activation picker, patron detail and status/card actions, issue/reissue dialog with duration/date modes, and current-user card/QR/history view. The shell navigation regression expectations are updated.
- Runtime configuration: `LIBRARY_CARD_HMAC_SECRET` must contain at least 32 UTF-8 bytes; otherwise card/QR signing is unavailable. No secret value is recorded here.
- Role assignment/grant is not exposed because the required Reservation WAITING/READY prerequisite is outside the available Plan 097 scope. `LIBRARIAN` is present in the shared role model. The existing application role cardinality was not globally refactored.

## Validation Result

| Gate | Status | Evidence |
| --- | --- | --- |
| Focused BE tests | **PASS** | 8 Plan 097 test classes, 35 tests, 0 failures/errors. Classes: `LibraryEligibilityServiceTest`, `LibraryPatronServiceTest`, `LibraryPatronFacadeServiceTest`, `LibraryPatronStatusServiceTest`, `LibraryCardCommandServiceTest`, `LibraryCardQueryServiceTest`, `LibraryCardVerificationServiceTest`, `LibraryAccessPolicyTest`. |
| BE JaCoCo | **PASS** | Focused run generated report. Line coverage: card command 40/43 (93.0%), query 32/35 (91.4%), verification 19/21 (90.5%), eligibility 18/19 (94.7%), patron activation 19/19 (100%), status 32/35 (91.4%), access policy 11/11 (100%). Branch coverage varies by class; the lowest listed core service branch coverage is PatronStatusService at 14/28 (50%). |
| Spring/MySQL service concurrency probe | **PASS** | Booted the actual Spring application against disposable MySQL 8.4.11; Flyway validated and applied V1–V31 and Hibernate schema validation completed. Concurrent duplicate activation resulted in one success/one `PATRON_ALREADY_EXISTS` and one patron row. Concurrent issue resulted in one active card/one `CARD_ALREADY_ACTIVE`. Forced sequence exhaustion during reissue left the prior card active and created no replacement, proving transactional rollback through the service. |
| FE focused tests | **PASS** | 7 files, 43 tests: patron API, card date helper, issue dialog, current-user card, patron list, patron detail, and authenticated shell. Covers both expiry input modes, activation picker, reissue/revoke UX and QR request with the session token. |
| FE lint / build | **PASS** | `npm run lint`; `npm run build` (`vue-tsc --noEmit` + Vite production build). |
| BE compile / artifact | **PASS** | `compileTestJava` and `assemble` passed. Initial offline `assemble` was blocked by uncached Spring Boot DevTools 4.0.7; online retry completed successfully. |
| PMD main | **PASS** | `pmdMain` completed with 0 violations. Final combined run reported `UP-TO-DATE` on unchanged main sources. |
| Checkstyle main/test | **PASS** | Both tasks exit successfully. Repository baseline warnings remain (main: 984; test: 375 on latest test-source run); no Plan 097 main/test source warnings remained after fixes. |
| PMD test | **SKIPPED** | Per the user's approved gate decision. |
| Full BE and FE test suites | **SKIPPED** | Per the user's approved gate decision. The focused runs above are the only test suites represented as PASS. |
| Browser/live API/target DB/deployment | **NOT RUN** | Component tests use API mocks. The Spring probe exercises service beans and the disposable DB, not HTTP authentication filters or a deployed target. No target database, remote runtime or deployment was accessed. |

Focused BE command, from `BE/BaiTap-RS`:

```bash
GRADLE_USER_HOME=/tmp/gradle-qa097 /home/duyptk/.gradle/wrapper/dists/gradle-9.5.1-bin/iq79hdu3mqx29lgffhp8bfmx/gradle-9.5.1/bin/gradle --offline --no-daemon --max-workers=1 pmdMain test \
  --tests '*LibraryEligibilityServiceTest' \
  --tests '*LibraryPatronServiceTest' \
  --tests '*LibraryPatronFacadeServiceTest' \
  --tests '*LibraryPatronStatusServiceTest' \
  --tests '*LibraryCardCommandServiceTest' \
  --tests '*LibraryCardQueryServiceTest' \
  --tests '*LibraryCardVerificationServiceTest' \
  --tests '*LibraryAccessPolicyTest'
```

FE commands, from `FE`:

```bash
npm run lint
npm run build
npx vitest run src/services/library/libraryPatronApi.spec.ts src/utils/libraryCardDates.spec.ts \
  src/components/library/LibraryCardIssueDialog.spec.ts src/views/library/LibraryMyCardView.spec.ts \
  src/views/library/LibraryPatronListView.spec.ts src/views/library/LibraryPatronDetailView.spec.ts \
  src/views/shell/AuthenticatedV2ShellView.spec.ts
```

## MySQL probe boundaries and cleanup

The service probe is retained at `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/library/Plan097MySqlRuntimeProbe.java`. It starts the real Spring Boot application, uses direct service beans, and drives MySQL through JDBC; the two Docker containers had no published ports and no host mounts. The probe used only disposable test data and a fake test-only AI key because the application auto-configuration requires a credential-shaped value; network access was disabled. The test containers and anonymous MySQL volume, plus temporary classpath staging, were removed after the run. This is application/service and DB evidence, not browser, HTTP-auth, or target-environment evidence.

The migration required renaming a reserved MySQL column from `last_value` to `sequence_value`; the resulting V31 migration ran successfully on MySQL 8.4.11. One initial probe attempt exposed an overlong generated test username; the test-only fixture was shortened and the full probe then passed. No production changes were required from that probe correction. Four QA debug iterations were used; the 10-round backend-validation cap was not reached.

## Deviations and remaining boundaries

- No user-approved business rule was changed. Shared v5 docs and Plan 097 were synchronized only in directly relevant sections.
- Role grant/revoke is intentionally absent pending Reservation prerequisite. Borrower eligibility excludes ADMIN/LIBRARIAN using shared role codes; activation candidates remain separate from borrowing eligibility.
- PMD test/full suites and real browser, HTTP security-filter, target DB, remote runtime and deployment evidence remain skipped or not run as marked above; none is represented as passing.
- No commit, push, or deployment was performed.
