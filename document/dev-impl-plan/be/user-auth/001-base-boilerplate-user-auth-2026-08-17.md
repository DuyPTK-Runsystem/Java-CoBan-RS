# Developer Plan: Base Backend Theo Boilerplate, Rút Gọn User/Auth

## 1. Mục tiêu

- Cấu hình và triển khai base backend trong `BE/BaiTap-RS` dựa trên tài liệu boilerplate tại `BE/BaiTap-RS/boilerplate`.
- Điều chỉnh boilerplate cho đúng phạm vi ứng dụng training hiện tại: user dùng `username/password`, không dùng `Role` và `Permission`.
- Kết quả mong muốn là backend có cấu trúc nền tảng cho User/Auth, response/error/config cơ bản, build được và sẵn sàng mở rộng sang Student module.

## 2. Requirement liên quan

- Yêu cầu trực tiếp:
  - Dựa trên boilerplate trong `BE/BaiTap-RS/boilerplate`.
  - Không implement các entity `Role` và `Permission`.
  - `User` chỉ cần `id`, `username`, `password` và audit fields.
- Tài liệu liên quan:
  - `document/application-doc/v1/ApplicationContext.md`
  - `document/application-doc/v1/modules/UserModule.md`
  - `document/application-doc/v1/DataStructure.md`
  - `BE/BaiTap-RS/boilerplate/BOILERPLATE_0_SUMMARY.md`
  - `BE/BaiTap-RS/boilerplate/BOILERPLATE_1_ENTITIES.md`
  - `BE/BaiTap-RS/boilerplate/BOILERPLATE_2_DTOS.md`
  - `BE/BaiTap-RS/boilerplate/BOILERPLATE_3_REPOSITORIES.md`
  - `BE/BaiTap-RS/boilerplate/BOILERPLATE_4_SERVICES.md`
  - `BE/BaiTap-RS/boilerplate/BOILERPLATE_5_CONTROLLERS_UTILITIES.md`
  - `BE/BaiTap-RS/boilerplate/BOILERPLATE_6_CONFIGURATION.md`
- Business rule/constraint:
  - Username required, max length 20, ASCII/single-byte input.
  - Password required, min length 6, max length 15 cho raw input theo tài liệu training hiện tại.
  - Password lưu DB dùng `VARCHAR(255)` để hỗ trợ password hash.
  - Password confirmation chỉ thuộc request đăng ký, không lưu DB.
  - Không trả password trong response.
  - Username không được trùng khi register.

## 3. Phạm vi

### In-scope

- Tạo package backend theo package thật của project `com.JavaTraining.BaiTap_RS`.
- Implement User entity rút gọn.
- Implement DTO cho login/register/user response/base response.
- Implement repository/service/controller cho user registration, login và account/logout ở mức phù hợp với base.
- Implement config nền tảng: CORS, security filter, password encoder, OpenAPI nếu phù hợp với boilerplate và dependency hiện có.
- Cấu hình `application.properties` theo boilerplate nhưng điều chỉnh tên app/package/database cho `BE/BaiTap-RS`.
- Điều chỉnh dependency nếu bắt buộc để code dùng Spring Data JPA/JWT compile được.

### Out-of-scope

- Không tạo entity/repository/service/controller/DTO cho `Role`.
- Không tạo entity/repository/service/controller/DTO cho `Permission`.
- Không tạo bảng `roles`, `permissions`, `role_permission`.
- Không implement role-based authorization, permission claims hoặc `@Secured`/`@PreAuthorize` theo role.
- Không implement Student module trong plan này.
- Không tự thay đổi tài liệu requirement ngoài phạm vi nếu không cần.

### Không được thay đổi

- Không revert các thay đổi sẵn có ngoài scope, đặc biệt các thay đổi trong `.agents/`/`skills/`.
- Không đổi kiến trúc toàn repo hoặc cấu trúc frontend.
- Không đổi CI/CD hoặc hạ tầng deploy.

## 4. Kiến trúc hiện tại

- `BE/BaiTap-RS` hiện là Spring Boot skeleton:
  - `src/main/java/com/JavaTraining/BaiTap_RS/BaiTapRsApplication.java`
  - `src/test/java/com/JavaTraining/BaiTap_RS/BaiTapRsApplicationTests.java`
  - `src/main/resources/application.properties`
  - `build.gradle.kts`
- Boilerplate gốc dùng package mẫu `com.uit.nhom7.KiemThuPhanMem`, bảng `users`, field `email/name/role/refreshToken`.
- Tài liệu application hiện tại dùng module User đơn giản với bảng `user`, field `user_id/user_name/password`; password schema đã được chốt cập nhật thành `VARCHAR(255)`.
- `build.gradle.kts` hiện có Spring Boot 4.0.7, Java 21, WebMVC, Security, Validation, Batch JDBC, Data JDBC, MySQL, Lombok, OpenAPI, Checkstyle và PMD. Cần kiểm tra/điều chỉnh dependency nếu implement JPA entity/repository theo boilerplate.

## 5. Phương án triển khai

- Dùng boilerplate làm khung tổ chức nhưng không copy nguyên xi các phần Role/Permission.
- Thiết kế User:
  - Entity: `User`
  - Table: ưu tiên theo tài liệu training hiện tại là `user`
  - Columns dự kiến:
    - `user_id`
    - `user_name`
    - `password`
    - audit fields: `created_at`, `updated_at`, `created_by`, `updated_by`
  - ID: dùng Java `Long` với database auto increment; không dùng UUIDv7 từ boilerplate vì user đã yêu cầu entity rút gọn.
- Auth/User flow:
  - Register: validate request, check duplicate username, lưu user.
  - Login: validate request, xác thực username/password, trả user info không có password.
  - Account/logout: giữ đơn giản theo authentication strategy được chọn.
- Password storage:
  - Dùng `password VARCHAR(255)` trong database để lưu password hash.
  - Vẫn validate raw password theo rule màn hình: required, min 6, max 15, ASCII/single-byte.
- Security:
  - Không đưa role/permission vào JWT hoặc authority.
  - Nếu dùng JWT, token chỉ chứa thông tin user tối thiểu như `id`, `username`.
  - Nếu base app chưa cần JWT đầy đủ, cấu hình security có thể permit các endpoint auth và chuẩn bị PasswordEncoder trước, tránh kéo thêm logic refresh token/role từ boilerplate.
- Lý do chọn phương án:
  - Khớp yêu cầu mới nhất của người dùng và tài liệu application hiện tại.
  - Giảm phụ thuộc không cần thiết từ boilerplate gốc.
  - Tránh tạo schema không dùng tới và tránh sai lệch Role/Permission.
- Trade-off:
  - Không copy nguyên JWT refresh-token flow từ boilerplate vì flow đó phụ thuộc `email`, `role`, `refresh_token`.
  - Nếu cần secure password hashing, schema password trong `DataStructure.md` phải được quyết định lại.

## 6. Phạm vi mã nguồn dự kiến

### Tạo mới

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/entity/User.java`
  - Entity/table mapping cho user rút gọn và audit lifecycle.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/repository/UserRepository.java`
  - Query theo username và duplicate check.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/service/UserService.java`
  - Register/login/account business logic, DTO mapping.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/controller/UserController.java` hoặc `AuthController.java`
  - REST endpoints cho register/login/account/logout tùy theo naming đã chọn khi implement.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/dto/LoginRequest.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/dto/LoginResponse.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/dto/RegisterUserRequest.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/dto/UserResponse.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/dto/RestResponse.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/common/error/...`
  - Exception classes/handler nếu boilerplate-style consistent response cần dùng.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/config/SecurityConfiguration.java`
  - CORS, security filter, PasswordEncoder.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/config/OpenApiConfiguration.java`
  - Swagger/OpenAPI metadata nếu không gây dependency conflict.

### Chỉnh sửa

- `BE/BaiTap-RS/build.gradle.kts`
  - Thêm/chỉnh Spring Data JPA hoặc OAuth2 resource server nếu thật sự cần cho implementation đã duyệt.
- `BE/BaiTap-RS/src/main/resources/application.properties`
  - Port, datasource, JPA, OpenAPI/security properties theo boilerplate đã rút gọn.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/BaiTapRsApplicationTests.java`
  - Chỉ chỉnh nếu context load test cần cấu hình phù hợp.

## 7. API / Database / Integration

### API dự kiến

- `POST /api/v1/auth/register`
  - Request: `username`, `password`, `confirmPassword`
  - Response: user info không có password.
- `POST /api/v1/auth/login`
  - Request: `username`, `password`
  - Response: login result chứa user info tối thiểu; token chỉ thêm nếu security strategy được chốt trong implementation.
- `GET /api/v1/auth/account`
  - Response: current user info nếu có authenticated context.
- `POST /api/v1/auth/logout`
  - Response: success, không phụ thuộc role/permission.

### Database dự kiến

- Bảng `user`:
  - `user_id BIGINT AUTO_INCREMENT PRIMARY KEY`
  - `user_name VARCHAR(20) NOT NULL`
  - `password VARCHAR(255) NOT NULL`
  - `created_at`
  - `updated_at`
  - `created_by`
  - `updated_by`
- Không tạo:
  - `roles`
  - `permissions`
  - `role_permission`

### Integration

- MySQL datasource theo `application.properties`.
- Frontend dự kiến gọi REST JSON.
- Không external service.

## 8. Test và validation

- Unit test/service test dự kiến:
  - Register thành công.
  - Register lỗi duplicate username.
  - Register lỗi confirm password mismatch.
  - Login thành công.
  - Login lỗi sai username/password.
  - Response không chứa password.
- Controller test nếu setup cho WebMVC thuận lợi:
  - Validate request body required/max length.
  - HTTP status cho success/error.
- Validation bắt buộc sau implement:
  - `./gradlew test`
  - `./gradlew build`
  - `./gradlew checkstyleMain checkstyleTest`
  - `./gradlew pmdMain pmdTest`
- Nếu task Gradle khác tên hoặc thiếu dependency, báo rõ và điều chỉnh trong scope đã duyệt.

## 9. Rủi ro

- Password schema/document drift:
  - Tài liệu training ban đầu có `VARCHAR(15)`, nay đã chốt dùng `VARCHAR(255)` cho DB.
  - Giảm thiểu: cập nhật docs trước khi implement và chỉ validate max length 15 trên raw input.
- Package/version mismatch:
  - Boilerplate dùng Spring Boot 4.0.5/Java 17/package khác, project hiện dùng Spring Boot 4.0.7/Java 21/package `com.JavaTraining.BaiTap_RS`.
  - Giảm thiểu: port code theo project hiện tại thay vì copy nguyên package.
- Dependency mismatch:
  - Project hiện có Data JDBC nhưng boilerplate dùng JPA.
  - Giảm thiểu: thêm `spring-boot-starter-data-jpa` nếu cần entity/repository JPA.
- Security regression:
  - Loại Role/Permission có thể làm token/authorization trong boilerplate không còn phù hợp.
  - Giảm thiểu: bỏ toàn bộ role/permission claims và chỉ giữ auth tối thiểu.
- Backward compatibility:
  - Vì app hiện là skeleton, impact thấp; vẫn không chạm phần ngoài `BE/BaiTap-RS` trừ plan/document.

## 10. Output dự kiến

- `BE/BaiTap-RS` có base backend compile được theo boilerplate rút gọn.
- User/Auth hoạt động với username/password, không có Role/Permission.
- Source code nằm đúng package project.
- Validation được chạy và kết quả được báo cáo rõ.
- Password DB dùng `VARCHAR(255)` và có thể lưu hash.

## 11. Approval status

- Trạng thái: Approved by user via agent on 2026-08-17.
