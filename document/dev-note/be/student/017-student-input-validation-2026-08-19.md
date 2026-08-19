# Dev Note: Student Backend Input Validation

## 1. Related Developer Plan and approval

- Plan: `document/dev-impl-plan/be/student/017-student-input-validation-2026-08-19.md`.
- Approved by user on 2026-08-19.

## 2. Actual scope completed

- Create/Update now reject an Average score outside inclusive range `0–10`; `null`
  remains valid.
- Birthday accepts only a valid date that is today or in the past when supplied.
- Fetch query validates search lengths, non-negative page/size and non-future
  birthday; Student detail/update/delete reject non-positive `studentId`.
- Validation failures for request body, query/path parameters and malformed JSON now
  use a common HTTP 400 `RestResponse` envelope.

## 3. Files changed

- Student request DTOs: `ReqCreateStudentDTO`, `ReqUpdateStudentDTO`,
  `ReqFetchStudentDTO`.
- `StudentController` for query/path validation activation.
- `ValidationExceptionHandler` added beside the existing application-error advice.
- `StudentValidationControllerIntegrationTest` added; existing controller integration
  test remains focused on CRUD contract.
- Student/Application context documentation and Dev Plan/Dev Note summaries.

## 4. Implementation decisions

- `@DecimalMin("0.0")`/`@DecimalMax("10.0")` are inclusive and skip `null`, matching
  nullable persistence fields without inferring a decimal-scale rule.
- `@PastOrPresent` implements the user-approved Birthday rule.
- Validation advice was separated from `GlobalExceptionHandler` to keep the existing
  PMD method-count constraint satisfied while preserving the shared response shape.

## 5. Validation

| Command | Result |
|---|---|
| `./gradlew test` | PASS — 11 test classes, 41 `@Test` cases; MockMvc/H2 covers score, Birthday, query/path and no-mutation behavior. |
| `./gradlew jacocoTestReport` | PASS — report generated; overall 1,373/1,603 instructions and 352/405 lines covered. |
| `./gradlew checkstyleMain` | PASS |
| `./gradlew pmdMain` | PASS |
| `./gradlew build` | PASS — includes test, JaCoCo, Checkstyle and PMD test checks. |

## 6. Deviations from Developer Plan

- Created `ValidationExceptionHandler` rather than enlarging `GlobalExceptionHandler`.
  This preserves the planned error envelope and avoids the PMD `TooManyMethods` rule.
- Moved validation scenarios to a dedicated integration test class after PMD flagged
  the existing CRUD integration test for too many methods.

## 7. Known warnings and remaining risks

- PMD prints its pre-existing configuration notice about `LoosePackageCoupling` before
  succeeding; it is not a violation.
- No database `CHECK` constraint was added. The service API is protected by Bean
  Validation; direct database writes remain outside this task's scope.

## 8. Next steps

- FE Plan 16.1 now mirrors the Average score range for early UX feedback; backend
  validation remains authoritative.
