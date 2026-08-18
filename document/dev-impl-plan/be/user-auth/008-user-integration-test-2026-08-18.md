# Developer Plan: User Integration Test

## 1. Mục tiêu

- Bổ sung integration test cho User/Auth API trong `BE/BaiTap-RS`.
- Kiểm tra request/response HTTP, validation, password hashing, persistence và security flow qua JWT.
- Bảo vệ contract hiện tại của `AuthController`, `UserService`, `GlobalExceptionHandler` và `FormatRestResponse`.
- Bổ sung coverage cho phần controller và authentication integration đang chưa có test HTTP riêng.

## 2. Requirement liên quan

- Module: `document/application-doc/modules/UserModule.md`.
- Base User/Auth backend plan đã approved:
  - `document/dev-impl-plan/be/user-auth/001-base-boilerplate-user-auth-2026-08-17.md`
- Quyết định backend hiện tại:
  - User dùng `username/password`, không có Role/Permission.
  - Username bắt buộc, tối đa 20 ký tự ASCII.
  - Password raw bắt buộc, dài từ 6 đến 15 ký tự ASCII.
  - Password trong database phải là password hash, không trả password ra response.
  - Username không được trùng khi đăng ký.
  - Authentication dùng JWT stateless.

Business rule cần kiểm tra:

- Register tạo một User mới khi username chưa tồn tại và password confirmation khớp.
- Register không tạo User khi username bị trùng hoặc confirmation không khớp.
- Login hợp lệ trả access token và thông tin User tối thiểu.
- Login sai thông tin trả lỗi unauthorized.
- Account yêu cầu authenticated JWT và trả thông tin User hiện tại.
- Logout yêu cầu authenticated request và trả `204 No Content`.
- Response không được chứa field password hoặc password hash.

## 3. Phạm vi

### In-scope

- Tạo integration test cho các endpoint:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `GET /api/v1/auth/account`
  - `POST /api/v1/auth/logout`
- Dùng Spring Boot test context với H2 in-memory.
- Dùng `MockMvc` để gọi API qua HTTP layer.
- Dùng password encoder, authentication manager và JWT token service thật trong context.
- Đăng ký User qua API, login qua API và dùng access token nhận được cho account/logout.
- Assert response wrapper `RestResponse` gồm status code, message, data và error khi phù hợp.
- Assert database state bằng `UserRepository`.
- Assert password lưu database khác raw password và không xuất hiện trong response.
- Kiểm tra request chưa authenticated bị security filter từ chối ở account/logout.

### Out-of-scope

- Không sửa production code nếu test không phát hiện bug bắt buộc.
- Không đổi `SecurityConfiguration.java`; file này đang có thay đổi trong worktree và không thuộc scope plan.
- Không thêm Role, Permission, authority hoặc role-based authorization.
- Không test frontend navigation hoặc trạng thái UI.
- Không dùng Testcontainers hoặc MySQL thật.
- Không kiểm tra token bị vô hiệu hóa sau logout; security hiện tại là stateless và logout chỉ trả response `204`.
- Không thêm refresh token, revoke list hoặc server-side session state.

## 4. Kiến trúc hiện tại

- `AuthController` expose:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `GET /api/v1/auth/account`
  - `POST /api/v1/auth/logout`
- `UserService` xử lý:
  - Password confirmation.
  - Duplicate username.
  - Password encoding.
  - Authentication manager.
  - JWT access token.
  - Current user lookup.
- `UserRepository` extends `JpaRepository<User, Long>`.
- `User` entity lưu username, password hash và audit fields.
- `JwtAuthenticationFilter` đọc `Authorization: Bearer <token>` và tạo `UserPrincipal`.
- `AuthController.account(...)` yêu cầu `UserPrincipal`, vì vậy test account phải dùng JWT thật từ login.
- `SecurityConfiguration` permit login/register và yêu cầu authenticated cho endpoint còn lại.
- `FormatRestResponse` bọc response thành `RestResponse` khi request thành công.
- `GlobalExceptionHandler` bọc `AppException` và validation error.

## 5. Phương án triển khai

- Tạo class `AuthControllerIntegrationTest` trong package `user.controller`.
- Annotation dự kiến:
  - `@SpringBootTest(properties = {...H2...})`
  - `@AutoConfigureMockMvc`
  - `@Transactional`
- Inject:
  - `MockMvc`
  - `UserRepository`
- Dùng `@BeforeEach` gọi `userRepository.deleteAll()` để cô lập dữ liệu.
- Dùng fixture JSON trực tiếp cho register/login để test đúng HTTP payload.
- Tạo helper thực hiện flow register -> login và trả access token cho account/logout.
- Dùng `JsonPath` hoặc response JSON parsing để đọc `data.access_token`/`data.user` theo response wrapper hiện tại.
- Không dùng `@WithMockUser` cho account/logout thành công, vì controller cần `UserPrincipal` được tạo bởi JWT filter.
- Dùng repository để kiểm tra username, password hash và số lượng User.
- Timestamp audit chỉ assert có giá trị, không assert thời điểm chính xác.

Trade-off:

- Dùng `@SpringBootTest` chậm hơn `@WebMvcTest`, nhưng cần kiểm tra đồng thời controller, service, JPA, password encoder, authentication manager và JWT filter.
- Dùng H2 giúp test local độc lập, nhưng không thay thế hoàn toàn MySQL ở các khác biệt dialect.
- Dùng JWT thật giúp kiểm tra toàn bộ security path, nhưng fixture phải tạo User trước khi gọi account/logout.

## 6. Phạm vi mã nguồn dự kiến

### Tạo mới

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/user/controller/AuthControllerIntegrationTest.java`
  - Integration test cho toàn bộ User/Auth REST API.

### Cập nhật sau implementation

- `document/dev-note/be/user-auth/008-user-integration-test-2026-08-18.md`
  - Ghi nhận thay đổi thực tế, validation, coverage và blocker nếu có.
- `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`

### Không dự kiến sửa

- `BE/BaiTap-RS/src/main/java/**`
- `BE/BaiTap-RS/build.gradle.kts`
- `BE/BaiTap-RS/src/main/resources/application.properties`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/config/SecurityConfiguration.java`

## 7. API / Database / Security Integration

### Register

`POST /api/v1/auth/register`

Request hợp lệ:

```json
{
  "username": "student01",
  "password": "secret1",
  "confirmPassword": "secret1"
}
```

Kỳ vọng:

- HTTP `201 Created`.
- Response có `data.username` và không có `data.password`.
- Database có một User mới.
- Password database không bằng raw password.

### Login

`POST /api/v1/auth/login`

Kỳ vọng:

- HTTP `200 OK`.
- Response có `data.access_token` không rỗng.
- Response có `data.user.username`.
- Response không có password hoặc password hash.

### Account

`GET /api/v1/auth/account`

Kỳ vọng:

- Không có Bearer token: HTTP `403 Forbidden` theo `SecurityConfiguration` hiện tại.
- Có access token hợp lệ: HTTP `200 OK` và trả đúng username.

### Logout

`POST /api/v1/auth/logout`

Kỳ vọng:

- Không có Bearer token: HTTP `403 Forbidden` theo `SecurityConfiguration` hiện tại.
- Có access token hợp lệ: HTTP `204 No Content`.

### Database

- Dùng schema Hibernate tạo trên H2:
  - `spring.datasource.url=jdbc:h2:mem:user-integration;MODE=MySQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=USER`
  - `spring.jpa.hibernate.ddl-auto=create-drop`
- Không thêm migration hoặc bảng mới.
- Không kiểm tra password bằng plaintext trong database.

## 8. Test plan

### Class/method cần test

- `AuthController.register(...)`
- `AuthController.login(...)`
- `AuthController.account(...)`
- `AuthController.logout(...)`
- Integration path qua `UserService`, `UserRepository`, `BCryptPasswordEncoder`, `AuthenticationManager`, `JwtTokenService`, `JwtAuthenticationFilter`, `FormatRestResponse` và `GlobalExceptionHandler`.

### Case thành công

- Register username mới với password confirmation khớp:
  - Expect `201`.
  - Assert response wrapper và username.
  - Assert không có password trong response.
  - Assert database có một User với password hash.
- Login bằng User vừa register:
  - Expect `200`.
  - Assert access token không rỗng.
  - Assert user summary đúng username/id.
  - Assert không lộ password.
- Account bằng Bearer token được tạo từ login:
  - Expect `200`.
  - Assert trả đúng User hiện tại.
- Logout bằng Bearer token hợp lệ:
  - Expect `204`.
  - Không assert token bị revoke vì implementation hiện tại stateless.

### Case lỗi / validation

- Register duplicate username:
  - Expect `409`.
  - Assert message `Tên đăng nhập đã tồn tại`.
  - Assert số lượng User không tăng.
- Register password confirmation không khớp:
  - Expect `400`.
  - Assert message `Mật khẩu xác nhận không khớp`.
  - Assert không tạo User.
- Register request có username/password/confirmPassword rỗng:
  - Expect `400`.
  - Assert validation response có field tương ứng.
- Register username vượt 20 ký tự hoặc chứa ký tự non-ASCII:
  - Expect `400`.
- Register password ngắn hơn 6, dài hơn 15 hoặc chứa ký tự non-ASCII:
  - Expect `400`.
- Login sai password hoặc username không tồn tại:
  - Expect `401`.
  - Assert message `Thông tin đăng nhập không hợp lệ`.
- Account không có authentication:
  - Expect `403` theo security contract hiện tại.
- Logout không có authentication:
  - Expect `403` theo security contract hiện tại.

### Boundary / null / empty / duplicate

- Username dài đúng 20 ký tự được chấp nhận nếu các field còn lại hợp lệ.
- Password dài đúng 6 và 15 ký tự được chấp nhận nếu các field còn lại hợp lệ.
- Null/blank request field bị reject bởi bean validation.
- Duplicate username không tạo bản ghi thứ hai.
- `confirmPassword` chỉ dùng để validate request, không lưu vào entity.

### Dependency mock/fake

- Không mock `UserService`, `UserRepository`, `PasswordEncoder`, `AuthenticationManager` hoặc `JwtTokenService` trong integration test.
- Dùng H2 in-memory thay database thật.
- Dùng JWT thật được tạo bởi `JwtTokenService`.
- Dùng `UserRepository` để kiểm tra persistence state, không mock interaction.

### Assertion

- Output:
  - HTTP status.
  - `RestResponse.statusCode`, `message`, `error`, `data`.
  - `data.access_token` và `data.user` ở login.
  - Không có `password` trong response JSON.
- State change:
  - Repository count sau register/duplicate/mismatch.
  - Username được lưu.
  - Password được hash.
  - Audit timestamps có giá trị.
- Security:
  - Public register/login không bị chặn.
  - Account/logout yêu cầu authentication.
  - JWT login -> account tạo được `UserPrincipal` đúng User.
- Side effect:
  - Password mismatch và duplicate không gọi persistence theo behavior quan sát được qua database state.

### Regression

- Bảo vệ response wrapper và API message của AuthController.
- Bảo vệ validation DTO của register/login qua HTTP binding.
- Bảo vệ duplicate username và password confirmation business rules.
- Bảo vệ password hashing và không lộ password.
- Bảo vệ JWT authentication path cho account/logout.
- Bảo vệ audit lifecycle của User khi persistence qua JPA.

## 9. Lệnh validation

Chạy từ thư mục `BE/BaiTap-RS`:

```bash
./gradlew test --tests com.JavaTraining.BaiTap_RS.user.controller.AuthControllerIntegrationTest
./gradlew test
./gradlew checkstyleMain checkstyleTest
./gradlew pmdMain pmdTest
./gradlew build
```

JaCoCo:

- HTML: `BE/BaiTap-RS/build/reports/jacoco/test/html/index.html`
- XML: `BE/BaiTap-RS/build/reports/jacoco/test/jacocoTestReport.xml`
- Đọc coverage của `AuthController`, `UserService`, `JwtAuthenticationFilter` và các class bị tác động.
- Không tự đặt coverage threshold nếu project chưa có threshold được phê duyệt.

## 10. Rủi ro và giảm thiểu

- `AuthController.account(...)` yêu cầu `UserPrincipal`, nên `@WithMockUser` không đại diện đúng flow production:
  - Giảm thiểu bằng register/login thật và truyền Bearer token.
- JWT filter có thể làm test account fail nếu token không hợp lệ hoặc hết hạn:
  - Dùng token vừa tạo trong cùng test và giữ thời lượng mặc định của test ngắn.
- `JacksonConfiguration` tự tạo `ObjectMapper` đơn giản:
  - Assert response thực tế qua MockMvc; nếu serialization `Instant` lỗi, ghi nhận là production/config blocker thay vì sửa ngoài scope.
- Logout hiện không revoke JWT stateless:
  - Chỉ assert authentication và HTTP status theo behavior hiện tại.
- H2 khác MySQL ở một số dialect:
  - Dùng MySQL mode và chỉ kiểm tra behavior JPA/API cần thiết.
- PMD/Checkstyle có thể báo lỗi baseline ngoài test mới:
  - Phân biệt lỗi thuộc test mới với lỗi có sẵn và ghi rõ trong Dev Note.

## 11. Output dự kiến

- Có file integration test HTTP cho User/Auth API.
- Test register/login/account/logout chạy với H2, BCrypt, JWT và MockMvc.
- Coverage controller và security integration tăng so với hiện tại.
- Có Dev Note 008 ghi lại implementation và validation sau khi plan được duyệt.

## 12. Trạng thái

- Status: Approved.
- Người dùng đã phê duyệt Plan 008 trong phiên làm việc ngày 2026-08-18.
