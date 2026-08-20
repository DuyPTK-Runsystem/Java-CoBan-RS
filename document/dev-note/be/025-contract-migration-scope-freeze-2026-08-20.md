# Dev Note 025: Contract, Migration và Scope Freeze

## Related Developer Plan and approval

- Plan: `document/dev-impl-plan/summary/025-contract-migration-scope-freeze-2026-08-20.md`.
- Approval: user approved via agent on 2026-08-20.

## Actual scope completed

- Added Flyway foundation for clean and legacy training schemas.
- Renamed legacy `user` table to `app_user` while retaining BCrypt values in the `password` column.
- Added role/user-role mappings, seeded `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`, `STUDENT`, and assigned all legacy users `ADMIN`.
- Enabled method security and restricted current Student APIs to `ADMIN`, `ACADEMIC_OFFICE` and `TEACHER`.
- Added migration, role-authority and Student API authorization regression tests.
- Added the v2 contract/migration scope-freeze artifact and linked it from `ApplicationContext.md`.

## Files changed

| Purpose | Paths |
|---|---|
| Flyway setup | `BE/BaiTap-RS/build.gradle.kts`, `BE/BaiTap-RS/src/main/resources/application.properties`, `BE/BaiTap-RS/src/main/resources/db/migration/V1__create_legacy_schema.sql`, `V2__rename_user_to_app_user.sql`, `V3__create_roles_and_assign_legacy_administrators.sql` |
| Security/domain | `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/config/SecurityConfiguration.java`, `security/UserPrincipal.java`, `student/controller/StudentController.java`, `user/domain/entity/User.java`, `user/domain/entity/Role.java` |
| Tests | `config/FlywayMigrationTest.java`, `security/UserPrincipalRoleTest.java`, `student/controller/StudentAuthorizationIntegrationTest.java` and existing Student integration tests |
| v2 documentation | `document/application-doc/Java-CoBan-RS-application-doc-modular-v2/document/application-doc/ApplicationContext.md`, `ContractMigrationScopeFreeze.md` |
| Tracking | Plan 025, this Dev Note, `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`, `document/dev-note/summary/DEV_NOTE_SUMMARY.md` |

## Important decisions

- Flyway uses `baseline-on-migrate=true` with baseline version 1: clean schemas run V1–V3; legacy schemas baseline at V1 then run V2/V3.
- The approved compatibility decision keeps the physical column name `password`; it stores the existing BCrypt hash unchanged.
- New registrations are not automatically assigned a role. Role-management APIs and per-student/per-class authorization remain outside this foundation.
- `average_score` remains present as deprecated legacy data until the replacement calculation flow is implemented.

## Validation

| Command/check | Result | Evidence |
|---|---|---|
| `./gradlew test --tests ...FlywayMigrationTest --tests ...UserPrincipalRoleTest --tests ...StudentAuthorizationIntegrationTest` | PASS | Clean/legacy migration, BCrypt preservation, role authority and `403` for STUDENT verified. |
| `./gradlew test` | PASS | Full backend suite and JaCoCo report task completed. |
| `./gradlew checkstyleMain checkstyleTest pmdMain pmdTest build` | PASS | Final quality gate completed successfully. |
| `git diff --check` | PASS | No whitespace errors. |
| Docker MySQL legacy preflight | NOT RUN | Docker daemon socket was unavailable; no shared/local MySQL migration was attempted. |

## Deviations, blockers and next steps

- No production or Docker MySQL migration was run because M7 is deferred for the learning-project scope and the Docker daemon was unavailable.
- `password_hash` remains the v2 target naming; current Flyway foundation deliberately preserves `password` during the compatibility window.
- Before using another legacy database, verify its `password` values are BCrypt hashes. Stop and choose a reset-password path if they are not.
- Add a role-management flow and resource-level Student authorization before assigning non-admin roles to new accounts.

## Debug rounds

- `code → test → debug`: 2/10.
- Round 1 corrected test-only H2 dependency and retained in-memory fixture state.
- Round 2 fixed Checkstyle and PMD findings; final validation passed.
