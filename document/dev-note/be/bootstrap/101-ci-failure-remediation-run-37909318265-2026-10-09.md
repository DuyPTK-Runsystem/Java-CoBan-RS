# Dev Note: Remediate CI Failure — GitHub Actions Run #37909318265

- **Date:** 2026-10-09
- **Scope:** Remediate CI release gates failures (Frontend unhandled rejection & Backend Flyway/H2 syntax error)
- **Status:** Completed

## 1. Related Information
- Target Workflow: `.github/workflows/demo-ci-cd.yml`
- Failed Run: GitHub Actions Run `#37909318265`
- Failed Commit: `6b57c3a80b5f91bf862a46fdc452533a9fcf1150`

## 2. Actual Scope Completed
- Fixed unhandled rejection in Frontend release gate:
  - Enabled auto-unmount via `enableAutoUnmount(afterEach)` and added explicit `wrapper.unmount()` in `FE/src/router/index.spec.ts`.
  - Hardened SSE lifecycle and error handling in `FE/src/services/notificationApi.ts` (idempotent stop, controller abort, safe timer clearing and guarded window access, unhandled rejection suppression).
  - Added regression test suite in `FE/src/services/notificationApi.spec.ts`.
- Fixed Flyway/H2 syntax error in Backend focused release gate:
  - Configured `"spring.flyway.target=29"` in `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/bootstrap/DemoDataSeederIntegrationTest.java`.
  - Preserved all production Flyway migrations (V31, V32) intact with zero checksum alteration.

## 3. Files Changed
- `FE/src/router/index.spec.ts`: Added auto-unmount and explicit unmount for `AuthenticatedV2ShellView`.
- `FE/src/services/notificationApi.ts`: Hardened `startNotificationEventStream` lifecycle, timer safety, and error handling.
- `FE/src/services/notificationApi.spec.ts`: Added regression tests for stop before resolve, stop during reconnect delay, idempotency, and network errors.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/bootstrap/DemoDataSeederIntegrationTest.java`: Added `spring.flyway.target=29` to test configuration.

## 4. Implementation Decisions
- **Frontend:** The root cause was that `mount(AuthenticatedV2ShellView)` was never unmounted during test execution, allowing the background SSE reconnect loop to persist after Vitest environment teardown. When the teardown destroyed `window`, `window.setTimeout` threw `ReferenceError: window is not defined` inside an unhandled promise. Fixed by enforcing component unmount and defensively hardening the SSE reconnect loop.
- **Backend:** `DemoDataSeederIntegrationTest` tests Academic/Placement/Timetable/Notification/Scorebook fixtures (V1-V29) and does not test Library (V30-V32). H2 does not support MySQL `STORED` syntax on generated columns in V31/V32. Setting `spring.flyway.target=29` allows the seeder integration test to run cleanly on H2 without modifying production Flyway migrations V31 and V32.

## 5. Validation Commands and Results
- `FE/`:
  - `npx vitest run src/router/index.spec.ts src/services/notificationApi.spec.ts`: **PASS** (105/105 tests)
  - `npm test`: **PASS** (140/140 files, 747/747 tests, 0 unhandled rejections)
  - `npm run lint`: **PASS** (0 warnings, 0 errors)
  - `VITE_API_BASE_URL=https://example.com/api npm run build`: **PASS**
- `BE/BaiTap-RS`:
  - `./gradlew --no-daemon test --tests 'com.JavaTraining.BaiTap_RS.bootstrap.DemoSeedCompletionTest' --tests 'com.JavaTraining.BaiTap_RS.bootstrap.DemoDataSeederIntegrationTest' --tests 'com.JavaTraining.BaiTap_RS.bootstrap.DemoNotificationSeedRunnerTest' --tests 'com.JavaTraining.BaiTap_RS.bootstrap.DemoSeedActivationTest'`: **PASS** (11/11 tests)
  - `./gradlew --no-daemon bootJar`: **PASS**
  - `./gradlew --no-daemon checkstyleMain`: **PASS**
  - `./gradlew --no-daemon pmdMain`: **PASS**

## 6. Known Blockers / Remaining Risks
- Full-suite baseline non-gating tests (`backend-baseline-observation`) have pre-existing issues (heap space / H2 incompatibility on other tests) which are non-release-gating and documented as out-of-scope.

