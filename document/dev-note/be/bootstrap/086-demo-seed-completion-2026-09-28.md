# Plan 086 — Backend demo seed completion marker

## Plan and approval

- Related Developer Plan: [Plan 086](../../../dev-impl-plan/summary/086-github-actions-first-demo-seed-2026-09-28.md).
- Approval: user approved Plan 086 and selected GitHub Actions.
- This note records the backend seed contract and tests. CI workflow/runbook work is documented separately in [the CI/CD Dev Note](../workflow-skill/086-github-actions-demo-ci-cd-2026-09-28.md).

## Actual scope completed

- Added a versioned durable marker for the Plan 081 demo fixture and ran it after ordered seed runners.
- Added fail-closed marker checks to applicable seed runners, including notification runner, so a completed fixture does not mutate seed data on rerun.
- Kept demo seeding disabled by default and added target/deployment metadata to the completion marker.
- Added focused tests for first successful completion, rerun no-op, failure without marker, notification runner guard, and absent/false activation flag.
- Corrected deterministic student canonical indexing so all 160 seeded students receive their intended unique names/identities.
- Updated existing fixture integration assertions to reflect canonical current fixture scopes, separated into named assertions for diagnosis.

## Files changed

- Marker and ordering: `BE/BaiTap-RS/src/main/resources/db/migration/V28__add_demo_seed_completion_marker.sql`, `.../bootstrap/DemoSeedCompletion.java`, `DemoSeedCompletionRunner.java`, `DemoNotificationSeedRunner.java`.
- Runner integration: `DemoDataSeeder.java`, `DemoFunctionalRoomSeeder.java`, `DemoPlacementSeeder.java`, `DemoScorebookSeeder.java`, `DemoNotificationSeeder.java`, and `BE/BaiTap-RS/src/main/resources/application.properties`.
- Identity fixture correction: `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/identity/DemoIdentitySeeder.java`.
- Tests: `DemoSeedCompletionTest.java`, `DemoNotificationSeedRunnerTest.java`, `DemoSeedActivationTest.java`, and `DemoDataSeederIntegrationTest.java` under `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/bootstrap/`.

## Important decisions

- Marker key: `DEMO_FIXTURE_PLAN_081`; fixture version: `PLAN_081_V1`; marker table: `app_demo_seed_completion`.
- Completion marker is inserted transactionally only after the ordered seed runners complete. A marker already present prevents seed runner writes. Failures propagate and do not record completion.
- `APP_SEED_DEMO_ENABLED` defaults to false. Target and deployment reference are recorded with the marker for workflow postflight verification.
- The canonical Plan 081 seed creates 160 total enrollments: 120 current-year and 40 historical; 40 Grade 7 students remain unassigned in the current year. Active subject applicability is 87 for the current seed rules.

## Validation evidence

- Focused tests (`DemoDataSeederIntegrationTest`, `DemoSeedCompletionTest`, `DemoNotificationSeedRunnerTest`, `DemoSeedActivationTest`): PASS, 15/15.
- `checkstyleMain pmdMain`: PASS. Checkstyle reported 977 warnings across 121 files, none in changed production files; PMD main reported no violations.
- `checkstyleTest`: PASS. New tests have no warnings; the modified integration test retains seven existing warnings unrelated to the added import.
- Full `test`: FAIL. QA observed a `PlacementServiceTest` Mockito expectation mismatch (`save` vs implementation `saveAndFlush`) that reproduces in isolation, followed by Spring context initialization OOM and cascading context failures. No backend seed-focused test failed.
- `build -x test`: FAIL at `pmdTest` with 400 test-source PMD violations. `bootJar`, `jar`, `assemble`, `checkstyleMain`, and `checkstyleTest` completed before the PMD failure.
- Validation was run by independent QA; implementation agent did not run tests or validation.

## Deviations and remaining limits

- Student canonical indexing required a small fixture correction discovered by the focused deterministic fixture test; names were kept canonical and unique rather than weakening the assertion.
- The full repository test/build gates are not green because of unrelated baseline Placement/OOM and PMD test-source findings. MySQL, Flyway application against the deployment DB, workflow execution, cloud deployment, and live smoke remain NOT RUN.

## Next steps

- Address baseline test-suite resource pressure and the unrelated PlacementServiceTest expectation separately.
- Reduce existing test-source PMD violations before claiming a full Gradle build PASS.
- The CI operator must perform a read-only preflight against the intended private database before first bootstrap.
