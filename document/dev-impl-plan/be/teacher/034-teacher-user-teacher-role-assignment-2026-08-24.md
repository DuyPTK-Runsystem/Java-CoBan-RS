# Developer Plan: Assign TEACHER Role When Creating Teacher

## Status

- Approved by user on `2026-08-24`.
- Request source: user instruction to assign role `TEACHER` to the corresponding app-user when creating a teacher.

## Objective

When `POST /api/v2/teachers` creates a teacher with a non-null `userId`, assign the existing `TEACHER` role to that app-user in the same database transaction. The operation must be idempotent if the user already has that role.

## Scope

- Add a repository lookup for `Role` by unique `code`.
- Update `TeacherService.createTeacher` to load the app-user and `TEACHER` role, add the role to the user, and create the teacher within the existing `@Transactional` boundary.
- Preserve current behavior for `userId = null` and existing teacher/user uniqueness checks.
- Update the new Postman collection to assert that the five linked users can authenticate with the `TEACHER` role after teacher creation.

## Invariants and Error Behavior

- `userId` must refer to an existing app-user when supplied.
- `TEACHER` must exist in the role seed data; if not found, fail with a typed server-side configuration error rather than silently creating an unprivileged teacher.
- Existing role memberships must remain unchanged; only the missing `TEACHER` membership is added.
- Teacher creation and role assignment must commit or roll back together.
- No password, token, or role-management secret is added to Postman.

## Unit Test Plan

### Target

- `TeacherService.createTeacher`.
- New `RoleRepository.findByCode` interaction.

### Success cases

1. Existing app-user without `TEACHER`: role is added, teacher is saved, response maps correctly, and audit is written.
2. Existing app-user already having `TEACHER`: role is not duplicated, teacher is saved, and existing roles remain present.
3. `userId = null`: teacher creation retains current behavior and does not attempt a role lookup.

### Error and boundary cases

1. Positive/nonexistent `userId`: preserve the current `404 Không tìm thấy tài khoản` behavior and do not save a teacher.
2. Missing seeded `TEACHER` role: fail with a typed application error and do not save a teacher.
3. Duplicate teacher code or duplicate user link: preserve current conflict behavior and do not mutate role membership before the duplicate checks pass.

### Mocks, fixtures, and assertions

- Mock `TeacherRepository`, `UserRepository`, `RoleRepository`, assignment repositories, and audit service.
- Use a real `User` and `Role("TEACHER", ...)` fixture; assert `user.getRoles()` contains exactly the expected role membership after success.
- Verify `teacherRepository.save` and audit interactions, and verify no save/audit interaction on failure.
- Add a regression assertion that existing roles are retained.

### Validation commands

- Focused test: `./gradlew test --tests '*TeacherServiceTest'`.
- Full backend validation after implementation: `./gradlew test`, `./gradlew checkstyleMain`, `./gradlew pmdMain`, `./gradlew build`.
- Read JaCoCo output for the changed service branch; do not introduce a new coverage threshold.

## Risks and Open Decisions

- The current repository has no teacher service unit test, so a new focused test class will be added.
- Role assignment changes authentication behavior only after a new login/token issuance; existing JWTs will not gain the role retroactively.
