# BE Dev Plan Summary

| No. | Plan | Scope | Status | Created |
|---:|---|---|---|---|
| 001 | [Base Backend Theo Boilerplate, Rút Gọn User/Auth](user-auth/001-base-boilerplate-user-auth-2026-08-17.md) | Spring Boot base, User/Auth, no Role/Permission | Approved | 2026-08-17 |
| 002 | [Dev Note Skill and Workflow Enforcement](workflow-skill/002-dev-note-skill-workflow-2026-08-17.md) | Project workflow skill, Dev Note artifact | Approved | 2026-08-17 |
| 003 | [Postman Collection Skill](workflow-skill/003-postman-collection-skill-2026-08-17.md) | Project skill for user-requested Postman collection updates | Approved | 2026-08-17 |

## Module folders

- `user-auth/`: User registration, login/logout, account, and authentication-related backend plans.
- `workflow-skill/`: Project workflow and skill changes.

## Current BE decisions

- `User.id` uses Java `Long`.
- User table primary key column is `user_id BIGINT AUTO_INCREMENT`.
- User password column is `VARCHAR(255)` for password hash storage.
- Do not implement `Role`, `Permission`, or `role_permission`.
