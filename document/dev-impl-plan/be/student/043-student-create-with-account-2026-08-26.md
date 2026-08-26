# Developer Plan 043: Create Student With Student Account

## 1. Trạng thái và thông tin chung

- **Status**: `Approved by user on 2026-08-26`.
- **Application-document version**: `v2`.
- **Ngày lập plan**: `2026-08-26`.
- **Module**: Backend `student` và tích hợp `user-auth`.
- **Tài liệu tham chiếu**:
  - `document/application-doc/v2/modules/StudentModule.md`
  - `document/application-doc/v2/modules/UserModule.md`
  - `document/application-doc/v2/modules/00-CommonAndAuthModule.md`
  - `document/application-doc/v2/data-model/03-StudentsAndEnrollment.md`
  - `document/application-doc/v2/data-model/01-IdentityAndAccess.md`
  - `document/dev-impl-plan/be/teacher/034-teacher-user-teacher-role-assignment-2026-08-24.md`
  - `document/dev-impl-plan/be/student/005-student-module-backend-2026-08-17.md`

## 2. Mục tiêu

Bổ sung API v3 để tạo một hồ sơ học sinh đồng thời tạo tài khoản đăng nhập tương ứng. Tài khoản được gán role `STUDENT` trong cùng transaction với hồ sơ `Student` và `StudentInfo`.

## 3. Requirement và business rule

- Endpoint mới: `POST /api/v3/students`.
- Request giữ các trường tạo học sinh hiện tại và bổ sung:
  - `username`: nullable.
  - `password`: nullable.
- Khi `username == null`, sinh username bằng cách:
  1. chuẩn hóa `studentName` về chữ thường;
  2. loại bỏ dấu Unicode;
  3. loại bỏ khoảng trắng/ký tự không phải chữ hoặc số;
  4. nối với 7 ký tự cuối của `studentCode`.
  - Ví dụ: `Khánh Duy`, `STU1234567` -> `khanhduy1234567`.
- Khi `password == null`, dùng mật khẩu mặc định `12345678` trước khi hash bằng `PasswordEncoder`.
- Username phải unique; username trùng trả `409 Conflict` và không lưu học sinh.
- Nếu username đầy đủ sau chuẩn hóa dài hơn giới hạn `app_user.user_name` hiện tại (20 ký tự), fallback sang chữ cái đầu của từng từ trong họ tên rồi nối 7 ký tự cuối của `studentCode`.
  - Ví dụ: `Phạm Trần Khánh Duy`, `STU1234567` -> `ptkd1234567`.
- Nếu phần viết tắt vẫn quá dài, giới hạn phần viết tắt để username cuối cùng không vượt 20 ký tự.
- `studentCode` vẫn phải đúng định dạng hiện tại và unique.
- Tài khoản mới phải được gán role `STUDENT`; role không tồn tại là lỗi cấu hình `500` và toàn bộ transaction rollback.
- Liên kết `student.userId` với user vừa tạo.
- Response không trả password hoặc password hash; chỉ trả dữ liệu học sinh và thông tin tài khoản an toàn gồm `userId`, `username`, `role`.
- Các API v1/v2 hiện tại và hành vi tạo Student không kèm tài khoản không thay đổi.

## 4. Phạm vi

### In-scope

- DTO request/response cho contract v3.
- Endpoint `POST /api/v3/students`.
- Service orchestration tạo `User`, gán `STUDENT`, tạo `Student`/`StudentInfo` và liên kết hai aggregate.
- Chuẩn hóa username mặc định và mã hóa password.
- Unit test service và controller/integration test contract cần thiết.
- Cập nhật tài liệu API/Postman chỉ trong phạm vi cần để kiểm thử endpoint mới.

### Out-of-scope

- Không thay đổi endpoint `POST /api/v1/students` hoặc các endpoint CRUD cũ.
- Không xây dựng chức năng đổi mật khẩu, reset password, cấp lại tài khoản hoặc cấp role qua API riêng.
- Không thay đổi schema/migration vì `app_user`, `user_role` và `student.user_id` đã tồn tại.
- Không trả password mặc định trong response; việc hiển thị/cấp password ban đầu cho người vận hành là quyết định UI/quy trình riêng.

## 5. Kiến trúc và phương án triển khai

Flow dự kiến:

1. Controller nhận và validate `ReqCreateStudentV3DTO`, gọi service và trả `201 Created`.
2. Service kiểm tra duplicate `studentCode` và xác định username explicit hoặc username sinh tự động.
3. Service kiểm tra duplicate username trước khi tạo entity.
4. Service lấy role `STUDENT`, tạo `User` với password đã hash và gán role.
5. Service tạo `Student`, gán `userId`, tạo `StudentInfo`, lưu trong transaction.
6. Service map response không chứa credential nhạy cảm.

Dùng cùng transaction để nếu một bước lỗi thì không để lại user hoặc student mồ côi. Không gọi `UserService.register()` vì endpoint đó không gán role và không tạo liên kết Student; thay vào đó tái sử dụng repository, `PasswordEncoder` và pattern role assignment đã có trong `TeacherService`.

## 6. Phạm vi mã nguồn dự kiến

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/controller/StudentV3Controller.java` - tạo controller route `/api/v3/students`, authorization và HTTP status.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/service/StudentAccountService.java` - orchestration riêng cho use case tạo Student kèm account, transaction và mapping.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/service/StudentUsernameGenerator.java` - tạo utility/component chuẩn hóa tên và sinh username mặc định.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/DTOs/requests/ReqCreateStudentV3DTO.java` - request contract có username/password.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/DTOs/response/ResStudentWithAccountDTO.java` - response contract không lộ credential.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/student/domain/entity/Student.java` - bổ sung cách liên kết `userId` nếu setter hiện tại không đủ theo access policy.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/student/service/StudentServiceTest.java` hoặc test class tương ứng - unit test business rules.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/student/controller/StudentV3ControllerTest.java` hoặc integration test tương ứng - contract/status/security test nếu pattern hiện tại hỗ trợ.
- `document/postman/Java-CoBan.postman_collection.json` - chỉ cập nhật nếu collection hiện tại là nơi kiểm thử chính của API student v3.

Các file cuối cùng có thể gộp/tách theo pattern test và package hiện tại sau khi kiểm tra đầy đủ, nhưng không mở rộng sang FE trong plan này.

## 7. API / database / integration

### Request mẫu

```json
{
  "studentCode": "STU1234567",
  "studentName": "Khánh Duy",
  "dateOfBirth": "2010-05-10",
  "address": "TPHCM",
  "averageScore": 8.5,
  "username": null,
  "password": null
}
```

### Response dự kiến

HTTP `201 Created`, dùng response envelope chung của backend:

```json
{
  "studentId": 1,
  "studentCode": "STU1234567",
  "studentName": "Khánh Duy",
  "dateOfBirth": "2010-05-10",
  "address": "TPHCM",
  "averageScore": 8.5,
  "account": {
    "userId": 10,
    "username": "khanhduy1234567",
    "role": "STUDENT"
  }
}
```

Không cần migration. Quan hệ sử dụng `student.user_id`, `app_user`, `role` và `user_role` hiện có.

## 8. Unit test plan

### Target

- Use case tạo Student kèm account trong `StudentService`.
- Username generation component.
- Controller `POST /api/v3/students` nếu test layer hiện tại có MockMvc coverage.

### Success cases

1. Username và password được truyền: giữ username, hash password, tạo Student liên kết user và gán `STUDENT`.
2. Username null: `Khánh Duy` + `STU1234567` sinh đúng `khanhduy1234567`.
3. Password null: dùng `12345678`, chỉ lưu giá trị qua `PasswordEncoder`.
4. Username explicit đã hợp lệ: response trả username/userId/role nhưng không chứa password.
5. Existing role khác trên user không áp dụng cho create flow; user mới có đúng role `STUDENT`.

### Error, boundary và regression cases

1. Duplicate student code: `409`, không tạo user và không lưu student.
2. Duplicate username explicit hoặc username sinh tự động: `409`, rollback toàn bộ.
3. Seed thiếu role `STUDENT`: `500`, không lưu user/student.
4. Request validation lỗi cho student code/name/date/score: `400`, không chạm repository save.
5. Username/password blank hoặc vượt giới hạn DTO: kiểm tra theo validation contract đã chọn; không tự coi blank là null nếu chưa có requirement.
6. Regression: endpoint v1 tạo Student vẫn chỉ tạo Student/StudentInfo, không tạo User.
7. Verify transaction interaction: lỗi khi lưu Student không để lại User.

### Mock, fixture và assertion

- Mock `StudentRepository`, `UserRepository`, `RoleRepository`, `PasswordEncoder`.
- Dùng fixture `Role("STUDENT", ...)`; assert repository interaction, `User.roles`, `Student.userId`, response mapping và không lộ credential.
- Dùng `ArgumentCaptor` để assert password truyền vào encoder là password explicit hoặc `12345678`.

### Validation commands

- Focused tests: `./gradlew test --tests '*StudentServiceTest' --tests '*StudentV3ControllerTest'`.
- Full backend validation: `./gradlew test`, `./gradlew checkstyleMain`, `./gradlew pmdMain`, `./gradlew build`.
- Đọc JaCoCo output để đánh giá coverage của nhánh username/password mặc định và rollback; không tự đặt threshold mới.

## 9. Rủi ro và giảm thiểu

- **Username collision**: kiểm tra unique trước khi save và giữ unique constraint DB; trả conflict rõ ràng.
- **Credential exposure**: response DTO tách biệt, không log request password, không serialize entity User.
- **Transaction consistency**: dùng `@Transactional` cho toàn bộ flow; test failure sau khi tạo User.
- **Normalization ambiguity**: giới hạn normalization đúng theo requirement hiện tại; không tự thêm cơ chế tự tăng username khi collision.
- **Generated username length**: fallback sang initials trước khi giới hạn phần username theo cột `app_user.user_name` dài 20 ký tự.
- **Backward compatibility**: route mới độc lập, không sửa contract v1/v2 hiện có.

## 10. Output dự kiến

- Gọi `POST /api/v3/students` tạo đồng thời hồ sơ học sinh và tài khoản login role `STUDENT`.
- Khi bỏ trống username/password, hệ thống tự sinh `khanhduy1234567` và dùng password mặc định `12345678` đã được hash.
- Response trả được thông tin liên kết cần thiết nhưng không trả password.

## 11. Approval gate

- Plan này cần user phê duyệt rõ ràng qua agent trước khi triển khai production code hoặc test.
- Sau khi được duyệt, mọi thay đổi ngoài endpoint v3, account creation, role `STUDENT`, test và tài liệu kiểm thử phải được xác nhận lại.
