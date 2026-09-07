# Dev Note: Refactor Lombok Entity and Explicit Controller Params

## 1. Related Developer Plan

- Plan: `document/dev-impl-plan/be/006-lombok-entity-controller-param-refactor-2026-08-17.md`
- Approval: Approved by user via agent on 2026-08-17.

## 2. Actual Scope Completed

- Refactored the existing `User` JPA entity to use Lombok for boilerplate accessors and JPA no-arg construction.
- Standardized existing `StudentController` path variables to declare explicit parameter names.
- Re-scanned backend controllers for `@RequestParam` and `@PathVariable` annotations.
- Ran backend test, Checkstyle, PMD, and build validation.

## 3. Files Changed

### Entity Lombok refactor

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/domain/entity/User.java`
  - Added `@Getter`.
  - Added `@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)`.
  - Added package-private Lombok setters for `username` and `password`.
  - Removed manual protected no-arg constructor and manual getters.
  - Preserved business constructor and audit lifecycle callbacks.

### Controller explicit params

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/controller/StudentController.java`
  - Changed update path variable to `@PathVariable("studentId")`.
  - Changed delete path variable to `@PathVariable("studentId")`.

### Planning / tracking

- `document/dev-impl-plan/be/006-lombok-entity-controller-param-refactor-2026-08-17.md`
  - Approval status updated after user approval.
- `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`
  - Plan 006 status updated after user approval.
- `document/dev-note/be/refactor/006-lombok-entity-controller-param-refactor-2026-08-17.md`
  - Created this implementation note.
- `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`
  - Added note 006.
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`
  - Added note 006.

## 4. Implementation Decisions

- Did not add `@Data` to JPA entities.
- Did not add broad public setters to `User`.
- Added package-private Lombok setters only for `username` and `password` because PMD `ImmutableField` flagged them after replacing manual boilerplate, while `final` fields are not appropriate for this JPA entity with protected no-arg construction.
- Did not change API endpoints, request/response DTOs, database mapping, auth behavior, or service logic.

## 5. Validation

Commands run from `BE/BaiTap-RS`:

| Command | Result | Notes |
|---|---|---|
| `./gradlew test` | PASS | Also generated JaCoCo report via configured finalizedBy. |
| `./gradlew checkstyleMain checkstyleTest` | PASS | Ran after final source change. |
| `./gradlew pmdMain pmdTest` | PASS | First run failed on `User` `ImmutableField`; passed after package-private Lombok setters. |
| `./gradlew build` | PASS | Build completed successfully after test/checkstyle/PMD. |

Reports/rules read:

- `BE/BaiTap-RS/config/checkstyle/checkstyle.xml`
- `BE/BaiTap-RS/config/pmd/ruleset.xml`
- PMD console output for failed first run.

Debug rounds used: 1.

## 6. Deviations From Developer Plan

- Plan preferred not adding setters to `User` if not needed.
- Implementation added package-private Lombok setters for `username` and `password` to satisfy PMD without making public entity setters or changing database/API behavior.

## 7. Known Blockers / Remaining Risks

- No known blocker.
- Initial sandbox run of `./gradlew test` failed because Gradle needed to write to `~/.gradle`; validation was rerun successfully with approved escalated execution.

## 8. Next Steps

- None required for this scope.
