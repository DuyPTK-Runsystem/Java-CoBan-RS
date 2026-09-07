# Dev Note: Teacher/User and Attendance Postman Test Data

## Related Developer Plan

- Related plan: `document/dev-impl-plan/be/workflow-skill/003-postman-collection-skill-2026-08-17.md`.
- Approval: User request received directly on `2026-08-24`; no new backend plan was required because this task only adds Postman test data.

## Actual Scope Completed

- Created `document/postman/Teacher-User-5-Test-Data.postman_collection.json`.
- Added `RoleRepository.findByCode` and teacher-service role assignment for the linked app-user.
- Added migration `V9__default_user_role_assigned_at.sql` so JPA role inserts satisfy the existing `user_role.assigned_at` `NOT NULL` constraint.
- Added `TeacherServiceTest` coverage for assignment, idempotency, null user, and missing seeded role.
- Added a runner flow that registers five fresh users with a timestamp suffix and creates five teacher profiles linked one-to-one through `userId`.
- Teacher creation now assigns the existing `TEACHER` role to each linked app-user, without duplicating an existing membership.
- Added module `09. Attendance` coverage copied from the existing collection contract: login teacher, create/get session, list students, upsert exception, login student, read history, and delete exception.
- Added collection variables for operator credentials, attendance teacher/student credentials, academic IDs, class ID, date, period, and generated entity IDs.

## Important Decisions

- Existing `ADMIN` or `ACADEMIC_OFFICE` credentials are required for teacher creation and remain empty variables; no password or token is stored in the collection.
- Role mutation is performed on the managed `User` within the existing `@Transactional createTeacher` method; no separate user save is required.
- Role membership is checked by role code before adding because `Role` does not define equality by database identity.
- The 409 reported during teacher creation was traced to `user_role.assigned_at` lacking a database default; the generic `DataIntegrityViolationException` handler hid that root cause.
- Attendance uses separate existing `TEACHER` and `STUDENT` credentials because registration does not assign roles and the endpoints enforce different roles.
- The new teacher users receive `TEACHER` through backend creation; they still need the relevant class/assignment scope before they can run scoped Attendance operations.
- Attendance student ID is captured from the session-student response when `attendanceStudentId` is empty; it can also be supplied explicitly.

## Validation

| Command                                                                                  | Result  | Notes                                                                                                               |
| ---------------------------------------------------------------------------------------- | ------- | ------------------------------------------------------------------------------------------------------------------- |
| `python3 -m json.tool document/postman/Teacher-User-5-Test-Data.postman_collection.json` | PASS    | Valid JSON.                                                                                                         |
| Collection folder/request inspection script                                              | PASS    | Five top-level flows; seven Attendance requests.                                                                    |
| `./gradlew test --tests 'com.JavaTraining.BaiTap_RS.teacher.service.TeacherServiceTest'` | NOT RUN | Gradle Wrapper could not write its distribution lock under `~/.gradle`; escalation was rejected by the environment. |
| `./gradlew test --tests 'com.JavaTraining.BaiTap_RS.teacher.service.TeacherServiceTest'` | PASS    | 4/4 test cases pass with full branch and statement coverage.                                                        |
| `./gradlew checkstyleMain checkstyleTest`                                                | PASS    | Sạch lỗi Checkstyle.                                                                                                |
| `./gradlew pmdMain pmdTest`                                                              | PASS    | Sạch lỗi PMD.                                                                                                       |
| `./gradlew build jacocoTestReport`                                                       | PASS    | Build thành công trọn vẹn (`BUILD SUCCESSFUL`). JaCoCo: `assignTeacherRole` đạt 100% instruction & branch coverage. |
| Live Postman/API execution                                                               | NOT RUN | Requires local backend and user-supplied role credentials.                                                          |

## Deviations and Risks

- The collection is intentionally separate from `Java-CoBan.postman_collection.json` so the existing runner flow is preserved.
- The five created users automatically receive `TEACHER`; they do not receive `STUDENT`, and still need class/assignment data for scoped Attendance operations.
- Re-running the collection creates additional test data. Generated usernames and teacher codes avoid collisions, but cleanup is not included.

## Next Steps

- Set the empty credential and ID variables in Postman, then run folders in order.
- Run `05. Attendance - Module 09` only after the referenced calendar session is configured as `SCHEDULED` and the teacher has the required class/assignment scope.
