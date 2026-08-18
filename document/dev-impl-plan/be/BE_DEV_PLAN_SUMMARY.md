# BE Dev Plan Summary

| No. | Plan | Scope | Status | Created |
|---:|---|---|---|---|
| 001 | [Base Backend Theo Boilerplate, Rút Gọn User/Auth](user-auth/001-base-boilerplate-user-auth-2026-08-17.md) | Spring Boot base, User/Auth, no Role/Permission | Approved | 2026-08-17 |
| 002 | [Dev Note Skill and Workflow Enforcement](workflow-skill/002-dev-note-skill-workflow-2026-08-17.md) | Project workflow skill, Dev Note artifact | Approved | 2026-08-17 |
| 003 | [Postman Collection Skill](workflow-skill/003-postman-collection-skill-2026-08-17.md) | Project skill for user-requested Postman collection updates | Approved | 2026-08-17 |
| 004 | [Lombok Annotation Guidance](workflow-skill/004-lombok-annotation-guidance-2026-08-17.md) | Project Lombok usage skill and backend skill references | Approved | 2026-08-17 |
| 005 | [Student Module Backend](student/005-student-module-backend-2026-08-17.md) | Student list/search/sort/page, CRUD, generate code | Approved | 2026-08-17 |
| 006 | [Refactor Lombok Entity and Explicit Controller Params](006-lombok-entity-controller-param-refactor-2026-08-17.md) | Lombok for existing entities, explicit controller `@PathVariable`/`@RequestParam` names | Approved | 2026-08-17 |
| 007 | [Student Integration Test](student/007-student-integration-test-2026-08-18.md) | Student API integration tests with MockMvc and H2 | Approved | 2026-08-18 |
| 008 | [User Integration Test](user-auth/008-user-integration-test-2026-08-18.md) | User/Auth integration tests with MockMvc, H2 and JWT | Approved | 2026-08-18 |

## Module folders

- `user-auth/`: User registration, login/logout, account, and authentication-related backend plans.
- `student/`: Student list, search, sorting, pagination, CRUD, and generated code backend plans.
- `workflow-skill/`: Project workflow and skill changes.

## Current BE decisions

- `User.id` uses Java `Long`.
- User table primary key column is `user_id BIGINT AUTO_INCREMENT`.
- User password column is `VARCHAR(255)` for password hash storage.
- Do not implement `Role`, `Permission`, or `role_permission`.
- Student code is 10 characters: `STU` plus 7 random digits.
- Student code is unique at Entity level.
- Student ids use Java `Long`.
- Student DOB uses Java `LocalDate` and DB `DATE`.
- `student_info.student_id` is a unique foreign key to `student.student_id`.
- Student delete uses entity relationship cascade/orphan removal with service transaction boundary.
- Student repository uses `JpaSpecificationExecutor<Student>`.
- Entity Lombok refactor should avoid `@Data` on JPA entities.
- Controller `@PathVariable` and `@RequestParam` annotations should declare explicit parameter names.
