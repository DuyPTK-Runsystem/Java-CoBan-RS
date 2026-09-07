# Dev Note 027: Academic Subject & Teacher Assignment Implementation

## Related Plan

- Developer Plan: `document/dev-impl-plan/be/academic/027-academic-subject-teacher-assignment-2026-08-21.md`.
- Approval: user requested `thực hiện plan 27, không thực hiện 27.1` on 2026-08-22.
- Explicit exclusion: Implementation Note 27.1 scope was not implemented.

## Actual Scope Completed

- Added backend v2 foundation for semesters, subjects, subject applicability, class-subjects,
  teachers, homeroom assignments and subject teaching assignments.
- Added `SubjectTeachingAssignmentAccessService` for future score authorization checks based
  only on `subject_teaching_assignment`.
- Added audit writes for subject create/update/status change, teacher create/update/status change,
  semester lock/reopen and assignment create/replace/end.
- Added G3 completeness checkpoint decision output only: returns `NEEDS_NOTIFICATION` or
  `NO_NOTIFICATION` for the approved checkpoint dates. No scheduler, email, persistence
  notification log, retry or delivery workflow was added.
- Added Flyway V5 schema for Plan 027 tables and constraints.
- Added focused unit tests for assignment replacement/guard rules, class-subject applicability
  and G3 checkpoint decision behavior.

## Files Changed

### Schema

- `BE/BaiTap-RS/src/main/resources/db/migration/V5__create_semester_subject_teacher_assignment.sql`

### Academic catalog

- Added semester, subject, subject applicability and class-subject entities/enums.
- Added repositories for semester, subject, subject applicability and class-subject.
- Added services/controllers/DTOs for `/api/v2/semesters`, `/api/v2/subjects`,
  `/api/v2/class-subjects` and `/api/v2/classes/{classId}/subjects`.
- Updated `SchoolClassRepository` with `findByIdForUpdate` for homeroom replacement locking.

### Teacher and assignment

- Added teacher entity/DTO/repository/service/controller.
- Added assignment entity/DTO/repository/service/controller, audit helper, guards and access service.

### Tests

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/academic/service/Plan027AcademicServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/assignment/service/AssignmentServiceTest.java`

## Decisions

- Used Plan 027 canonical values `SubjectType = ACADEMIC | SKILL`.
- Kept `subject_type` separate from `application_scope = GRADE | CLASS`.
- Used semester lifecycle `DRAFT -> ACTIVE -> LOCKED -> CLOSED`.
- Used `AssignmentStatus = ACTIVE | ENDED`; no `CANCELLED` status was introduced.
- Teacher status change does not automatically end active assignments; assignment replace/end
  remains explicit.
- `school_class` was not given a homeroom teacher field.

## Validation

| Command | Result |
|---|---|
| `./gradlew test --tests com.JavaTraining.BaiTap_RS.assignment.service.AssignmentServiceTest --tests com.JavaTraining.BaiTap_RS.academic.service.Plan027AcademicServiceTest --tests com.JavaTraining.BaiTap_RS.academic.service.AcademicServiceTest` | PASS |
| `./gradlew test` | PASS |
| `./gradlew checkstyleMain checkstyleTest pmdMain pmdTest` | PASS |
| `./gradlew test build jacocoTestReport` | PASS |

Debug loop count: 4 validation/debug loops.

## Deviations From Plan

- Full CRUD surface was implemented for foundation resources, but advanced scoped read behavior
  for teacher self-scope and student enrollment-scope is not fully expanded beyond authenticated
  metadata reads.
- No Postman collection or frontend work was performed, matching Plan 027 out-of-scope.

## Remaining Risks

- MySQL runtime preflight against a live MySQL instance was not run; validation used the project
  Gradle/H2 test setup.
- Teacher/student scoped authorization should be tightened when the frontend/user scope contract
  is implemented.
- Plan 27.1 remains pending for scheduler, email delivery/configuration, templates, retry/outbox
  and notification persistence.
