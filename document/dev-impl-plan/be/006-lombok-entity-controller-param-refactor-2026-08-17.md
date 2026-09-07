# Developer Plan: Refactor Lombok Entity and Explicit Controller Params

## 1. Mục tiêu

- Chuẩn hóa các JPA entity chưa sử dụng Lombok để giảm boilerplate nhưng vẫn giữ an toàn cho entity lifecycle.
- Chuẩn hóa controller để mọi `@PathVariable` và `@RequestParam` đều khai báo tên biến rõ ràng theo format `("tenBien")`.
- Giữ nguyên behavior hiện tại của API, entity mapping, service và database contract.

## 2. Requirement liên quan

- Yêu cầu trực tiếp từ người dùng:
  - Sửa các entity chưa sử dụng Lombok.
  - Trong controller, `@RequestParam` và `@PathVariable` đều phải có `("tên biến")`.
- Quy tắc project:
  - Entity dùng Lombok theo hướng an toàn, không dùng `@Data` cho JPA entity.
  - Controller phải mỏng, không đưa business logic vào controller.
  - Mọi thay đổi backend phải chạy test/build/Checkstyle/PMD liên quan trước khi báo hoàn tất.
- Tài liệu/skill liên quan:
  - `.codex/AGENTS.md`
  - `.codex/AGENTS_DETAIL.md`
  - `.codex/workflows/WORKFLOW-BACKEND.md`
  - `.agents/skills/entity-impl/SKILL.md`
  - `.agents/skills/controller-impl/SKILL.md`
  - `.agents/skills/lombok-usage/SKILL.md`

## 3. Phạm vi

### In-scope

- Refactor entity backend hiện có chưa dùng Lombok:
  - `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/domain/entity/User.java`
- Kiểm tra và giữ nguyên entity đã dùng Lombok:
  - `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/entity/Student.java`
  - `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/entity/StudentInfo.java`
- Chuẩn hóa controller annotation hiện có:
  - `@PathVariable Long studentId` thành `@PathVariable("studentId") Long studentId`.
  - Nếu phát hiện `@RequestParam` không khai báo tên biến trong backend, bổ sung `("tenBien")`.
- Cập nhật hoặc bổ sung test nếu behavior bị ảnh hưởng bởi refactor.
- Tạo Dev Note sau khi implementation hoàn tất.

### Out-of-scope

- Không đổi endpoint path, HTTP method, request body, response DTO hoặc status code.
- Không đổi entity table/column mapping, relationship, lifecycle callback hoặc database schema.
- Không thêm Lombok `@Data` cho entity.
- Không refactor service/repository/controller ngoài phần annotation và Lombok đã nêu.
- Không đổi dependency, build config hoặc formatter trừ khi validation chứng minh bắt buộc.

### Không được thay đổi

- Không đổi tên field/entity/table/column.
- Không thay đổi password hashing/authentication behavior.
- Không revert thay đổi ngoài scope hoặc thay đổi do người dùng tạo.

## 4. Kiến trúc hiện tại

- Backend nằm trong `BE/BaiTap-RS`, Spring Boot, Java 21, Gradle Kotlin DSL.
- Lombok dependency và annotation processor đã tồn tại trong `BE/BaiTap-RS/build.gradle.kts`.
- Entity hiện tại:
  - `Student` đã dùng `@Getter`, `@Setter`, `@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)`.
  - `StudentInfo` đã dùng `@Getter`, `@Setter`, `@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)`.
  - `User` chưa dùng Lombok, đang có protected no-arg constructor và getter thủ công.
- Controller hiện tại:
  - `StudentController` có 2 `@PathVariable` chưa khai báo tên biến rõ ràng.
  - `AuthController` hiện không có `@PathVariable` hoặc `@RequestParam`.
  - Chưa phát hiện `@RequestParam` trong `src/main/java`.

## 5. Phương án triển khai

- Với `User` entity:
  - Thêm `@Getter`.
  - Thêm `@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)`.
  - Xóa protected no-arg constructor thủ công.
  - Xóa getter thủ công được Lombok thay thế.
  - Giữ constructor nghiệp vụ `User(String username, String password)`.
  - Không thêm `@Setter` nếu entity hiện tại chỉ expose getter và không cần setter ngoài class.
  - Giữ nguyên `@PrePersist` và `@PreUpdate`.
- Với controller:
  - Thay `@PathVariable` bằng `@PathVariable("studentId")`.
  - Quét toàn bộ backend để bổ sung tên biến cho mọi `@RequestParam` nếu có.
- Lý do chọn phương án:
  - Giảm boilerplate đúng yêu cầu nhưng giữ entity an toàn, tránh generated setter không cần thiết trên `User`.
  - Explicit parameter names giúp API ổn định hơn khi build không giữ Java parameter metadata hoặc khi refactor tên tham số.
- Trade-off:
  - Không dùng `@Setter` cho `User` nên nếu code khác đang dựa vào setter thì compile sẽ báo ngay; hiện tại không phát hiện setter thủ công.
  - Không dùng `@Data` nên vẫn không có generated `toString`, `equals`, `hashCode`, phù hợp JPA entity.

## 6. Phạm vi mã nguồn dự kiến

### Chỉnh sửa

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/user/domain/entity/User.java`
  - Class `User`.
  - Thêm Lombok annotations/imports.
  - Gỡ constructor no-arg và getter thủ công được Lombok thay thế.
  - Giữ nguyên constructor nghiệp vụ và audit lifecycle methods.

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/controller/StudentController.java`
  - Method `updateStudent`.
  - Method `deleteStudent`.
  - Bổ sung tên biến cho `@PathVariable("studentId")`.

### Có thể chỉnh nếu phát hiện trong lúc implement

- Các controller khác trong `BE/BaiTap-RS/src/main/java/**/controller/**/*.java`
  - Chỉ bổ sung tên biến cho `@RequestParam` hoặc `@PathVariable` còn thiếu.

### Tạo mới sau implementation

- Dev Note trong `document/dev-note/be/` ghi nhận thay đổi thực tế, validation và vấn đề còn lại.

## 7. API / Database / Integration

- API:
  - Không đổi endpoint.
  - Không đổi request/response payload.
  - Không đổi status code hoặc `ApiMessage`.
- Database:
  - Không đổi table, column, constraint, relationship hoặc migration.
- Integration:
  - Không đổi frontend contract.
  - Không đổi security/auth flow.

## 8. Test và validation

- Unit/context test:
  - Chạy test backend hiện có để bảo đảm Lombok-generated constructor/getter compile và context không bị ảnh hưởng.
- Static analysis:
  - Chạy Checkstyle cho main/test.
  - Chạy PMD cho main/test.
- Build:
  - Chạy build backend.
- Command dự kiến trong `BE/BaiTap-RS`:
  - `./gradlew test`
  - `./gradlew checkstyleMain checkstyleTest`
  - `./gradlew pmdMain pmdTest`
  - `./gradlew build`

## 9. Rủi ro

- Lombok annotation có thể tạo constructor/getter với visibility khác mong muốn.
  - Giảm thiểu: dùng `@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)` giống pattern entity Student hiện có.
- Entity `User` đang không có setter; thêm setter không cần thiết có thể mở rộng mutation surface.
  - Giảm thiểu: chỉ dùng `@Getter` và giữ nguyên constructor nghiệp vụ.
- Controller annotation refactor có thể bỏ sót `@RequestParam` trong file mới hoặc package khác.
  - Giảm thiểu: dùng `rg` quét toàn bộ `BE/BaiTap-RS/src/main/java`.
- Validation có thể phát hiện lỗi cũ ngoài scope.
  - Giảm thiểu: phân biệt lỗi cũ với lỗi do thay đổi hiện tại, không tự sửa ngoài scope khi chưa được duyệt.

## 10. Output dự kiến

- Entity chưa dùng Lombok được refactor theo style an toàn cho JPA.
- Tất cả `@PathVariable` và `@RequestParam` trong controller backend có tên biến rõ ràng.
- Không có thay đổi behavior API/database.
- Validation backend được chạy và kết quả được báo cáo.
- Dev Note được tạo/cập nhật sau khi implementation hoàn tất.

## 11. Approval status

- Trạng thái: Approved by user via agent on 2026-08-17.
