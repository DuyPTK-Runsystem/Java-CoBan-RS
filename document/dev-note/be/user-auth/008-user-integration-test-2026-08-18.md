# Dev Note: User Integration Test

## 1. Developer Plan liên quan

- Plan: `document/dev-impl-plan/be/user-auth/008-user-integration-test-2026-08-18.md`
- Trạng thái phê duyệt: `Approved` bởi người dùng trong phiên làm việc ngày 2026-08-18.

## 2. Phạm vi đã hoàn thành

- Bổ sung integration test HTTP cho `AuthController`.
- Kiểm tra register, login, account và logout bằng `MockMvc`.
- Dùng H2 in-memory, BCrypt password encoder, AuthenticationManager và JWT thật trong Spring context.
- Kiểm tra duplicate username, password confirmation, validation boundary và non-ASCII input.
- Kiểm tra password được hash và không xuất hiện trong response.
- Kiểm tra JWT login -> account -> logout.
- Kiểm tra request anonymous bị chặn ở account/logout.

## 3. File thay đổi

### Test

- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/user/controller/AuthControllerIntegrationTest.java`

### Tài liệu

- Cập nhật trạng thái Plan 008 trong `document/dev-impl-plan/be/user-auth/008-user-integration-test-2026-08-18.md`.
- Cập nhật `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`.
- Cập nhật `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`.
- Cập nhật các Dev Note summary.

### Không thay đổi

- Không thay đổi production code.
- Không thay đổi `SecurityConfiguration.java`.

## 4. Quyết định triển khai

- Dùng `@SpringBootTest` vì test cần đi qua controller, service, repository, BCrypt và JWT filter.
- Dùng access token thật được tạo từ flow register/login để kiểm tra `UserPrincipal` trong account.
- Không dùng `@WithMockUser` cho account/logout thành công vì controller yêu cầu `UserPrincipal`.
- Dùng assertion tổng hợp có message để phù hợp rule PMD của project.
- Logout chỉ assert `204 No Content`; không assert token bị revoke vì authentication hiện tại là stateless.

## 5. Phát hiện và sai lệch so với plan

- Login sai thông tin trả HTTP `401` và response message `Thông tin đăng nhập không hợp lệ`.
- Request anonymous tới `/api/v1/auth/account` và `/api/v1/auth/logout` trả HTTP `403`, không phải `401` như giả định ban đầu.
- Plan 008 đã được cập nhật để ghi nhận `403` theo behavior thực tế của `SecurityConfiguration` hiện tại.
- Không sửa production security vì task chỉ nằm trong phạm vi integration test.

## 6. Validation

| Lệnh | Kết quả thực tế |
|---|---|
| `./gradlew test --tests com.JavaTraining.BaiTap_RS.user.controller.AuthControllerIntegrationTest` | `PASS` - 6 test hoàn thành |
| `./gradlew test checkstyleMain checkstyleTest pmdMain pmdTest build` | `PASS` - `BUILD SUCCESSFUL` |
| Checkstyle main/test | `PASS` - không còn cảnh báo trong test User mới |
| PMD main/test | `PASS` - task hoàn thành; PMD có thông báo rule `LoosePackageCoupling` misconfigured từ cấu hình hiện tại |
| Build | `PASS` |
| JaCoCo `build/reports/jacoco/test/jacocoTestReport.xml` | `PASS` - `AuthController` 5/5 method và 8/8 line; `UserService` 6/8 method và 32 line; `JwtAuthenticationFilter` 3/3 method và 17/18 line |

## 7. Vòng debug

- Tổng số vòng `code -> test -> debug`: 5.
- Vòng 1: test phát hiện anonymous account/logout trả `403`.
- Vòng 2: cập nhật assertion và plan theo security behavior thực tế, test User pass.
- Vòng 3: quality gate pass nhưng phát hiện cảnh báo import order trong test.
- Vòng 4: sửa import order và chạy lại toàn bộ validation, tất cả pass.
- Vòng 5: chỉnh thụt lề assertion và chạy lại validation cuối, tất cả pass.

## 8. Blocker và bước tiếp theo

- Không còn blocker thuộc Plan 008.
- Không thêm dependency, migration hoặc thay đổi production security.
- Có thể tạo commit theo Conventional Commits sau khi người dùng kiểm tra staged changes.
