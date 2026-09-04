# Dev Note: Base Backend Theo Boilerplate, Rút Gọn User/Auth

## 1. Related Plan

- Developer Plan: `document/dev-impl-plan/be/user-auth/001-base-boilerplate-user-auth-2026-08-17.md`
- Approval: approved by user via agent on 2026-08-17.

## 2. Actual Scope Completed

- Implemented base User/Auth backend in `BE/BaiTap-RS`.
- User model uses `Long id`, `username`, `password`, and audit fields.
- Password is stored as BCrypt hash in `password VARCHAR(255)`.
- Implemented register, login, account, and logout endpoints.
- Added JWT access token support without Role/Permission.
- Added global response formatting with `FormatRestResponse` and `@ApiMessage`.
- Added global application exception handling.
- Added unit tests for core `UserService` auth behavior.
- Enabled JaCoCo coverage reporting after the follow-up approved request.

## 3. Files Changed

### User/Auth module

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/controller/AuthController.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/service/UserService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/repository/UserRepository.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/domain/entity/User.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/domain/DTOs/requests/ReqLoginUserDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/domain/DTOs/requests/ReqRegisterUserDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/domain/DTOs/response/ResLoginUserDTO.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/domain/DTOs/response/ResUserDTO.java`

### Security

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/security/JwtAuthenticationFilter.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/security/JwtTokenService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/security/UserPrincipal.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/config/SecurityConfiguration.java`

### Common/config

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/annotation/ApiMessage.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/dto/RestResponse.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/error/AppException.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/error/GlobalExceptionHandler.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/util/AuditUtil.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/util/FormatRestResponse.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/config/JacksonConfiguration.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/config/OpenApiConfiguration.java`

### Build/config/docs

- `BE/BaiTap-RS/build.gradle.kts`
- `BE/BaiTap-RS/src/main/resources/application.properties`
- `BE/BaiTap-RS/.gitignore`
- `document/application-doc/v1/DataStructure.md`
- `document/application-doc/v1/modules/UserModule.md`

### Tests

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/BaiTapRsApplicationTests.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/user/service/UserServiceTest.java`

## 4. Decisions

- Do not implement `Role`, `Permission`, or `role_permission`.
- Use username-based auth instead of boilerplate email-based auth.
- Use `Long`/`BIGINT AUTO_INCREMENT` for `User.id`.
- Use `password VARCHAR(255)` for password hash storage while validating raw password length 6-15.
- Use JWT access token with user identity only; no role/permission claims.
- Keep logout stateless for now: endpoint returns `204 No Content` and does not manage refresh tokens.
- Wrap success responses through `FormatRestResponse`; error responses are created by `GlobalExceptionHandler`.
- Use H2 in `contextLoads` test to avoid requiring local MySQL for test validation.

## 5. Validation

- `./gradlew test`: PASS.
- `./gradlew checkstyleMain checkstyleTest`: PASS.
- `./gradlew pmdMain pmdTest`: PASS.
- `./gradlew build`: PASS.
- `./gradlew test jacocoTestReport`: PASS after JaCoCo was enabled.

JaCoCo report:

- XML: `BE/BaiTap-RS/build/reports/jacoco/test/jacocoTestReport.xml`
- HTML: `BE/BaiTap-RS/build/reports/jacoco/test/html/index.html`

Coverage summary from latest report:

| Metric | Covered | Total | Coverage |
|---|---:|---:|---:|
| Instruction | 434 | 967 | 44.88% |
| Branch | 4 | 34 | 11.76% |
| Line | 109 | 251 | 43.43% |
| Method | 42 | 85 | 49.41% |
| Class | 17 | 19 | 89.47% |

## 6. Deviations

- Plan mentioned `UserController.java` or `AuthController.java`; implementation uses `AuthController.java`.
- Plan left JWT strategy flexible; implementation added a minimal custom HS256 JWT service.
- Boilerplate refresh-token flow was not copied because it depends on `email`, `refresh_token`, and role data.
- `FormatRestResponse` and `ApiMessage` were added after the user noticed the formatter was missing.
- JaCoCo was enabled after a follow-up user request, not in the original approved plan `001`.

## 7. Risks / Blockers

- No current blocker for the implemented base auth module.
- No coverage threshold is configured yet.
- Token invalid/expired failures currently surface through the security filter path and may need a dedicated JSON authentication entry point later.
- Refresh token/session invalidation is out of scope for this base module.
- Controller-level MVC tests are not yet added; current focused coverage is service-level plus Spring context load.

## 8. Next Steps

- Add controller tests for `/api/v1/auth/register`, `/login`, `/account`, and `/logout`.
- Consider a dedicated authentication entry point for consistent JSON errors on invalid bearer tokens.
- Define refresh-token/logout behavior if server-side logout becomes required.
- Add coverage thresholds only after an approved project decision.
