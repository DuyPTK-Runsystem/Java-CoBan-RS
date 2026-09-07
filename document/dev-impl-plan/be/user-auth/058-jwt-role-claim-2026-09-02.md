# Developer Plan 058: Bổ sung claim `role` cho JWT

## 1. Mục tiêu

Bổ sung thông tin role vào access token JWT để frontend có thể xác định capability của tài khoản sau khi đăng nhập. Backend vẫn là nơi authoritative cho authentication và authorization.

## 2. Requirement liên quan

- JWT hiện tại có các claim `sub`, `user_id`, `iat`, `exp` nhưng chưa có role claim.
- `User` hỗ trợ nhiều role thông qua quan hệ `user_role`.
- `ResUserDTO` hiện đã expose danh sách role code canonical.
- Role trong JWT chỉ phục vụ identity/capability phía client, không được dùng thay cho kiểm tra authorization phía backend.

## 3. Phạm vi

### In-scope

- Bổ sung claim có tên chính xác là `role` vào access token.
- Claim `role` có giá trị là danh sách role code canonical, ví dụ:

```json
{
  "role": ["ADMIN", "TEACHER"]
}
```

- Sort role code trước khi đưa vào token để payload ổn định và dễ kiểm thử.
- Giữ nguyên các claim hiện tại, secret, thuật toán HS256 và cơ chế ký token.
- Bổ sung unit test cho user có một role và nhiều role.
- Bổ sung integration test login để xác nhận payload JWT có claim `role`.
- Cập nhật tài liệu API/auth và Dev Note sau implementation.

### Out-of-scope

- Không đổi tên claim thành `roles`.
- Không chọn một primary role duy nhất; tài khoản nhiều role phải giữ đầy đủ role trong mảng claim.
- Không thay đổi `User`, `Role`, bảng `role`, bảng `user_role` hoặc migration.
- Không dùng claim `role` trong `JwtAuthenticationFilter` để tạo authorities.
- Không thay đổi `SecurityConfiguration`, permission matrix hoặc `@PreAuthorize`.
- Không triển khai frontend role-aware navigation trong Plan 058.
- Không thêm refresh token, revoke token hoặc thay đổi signing secret.

## 4. Kiến trúc hiện tại

- `UserService.login(...)` xác thực thông tin đăng nhập, lấy `UserPrincipal` và gọi `JwtTokenService.createAccessToken(...)`.
- `UserPrincipal` giữ `User` id, username và roles; `getAuthorities()` chuyển role code thành authority dạng `ROLE_<CODE>`.
- `JwtAuthenticationFilter` chỉ đọc `sub` từ JWT, sau đó tải lại user từ database qua `UserDetailsService` để tạo authorities.
- Do đó claim `role` là thông tin bổ sung trong token; authorities backend tiếp tục lấy từ user hiện tại trong database.

## 5. Phương án triển khai

1. Thêm accessor cung cấp role code đã sort từ `UserPrincipal`, hoặc reuse logic tương đương nếu phù hợp convention hiện tại.
2. Thêm `role` vào payload trong `JwtTokenService.createAccessToken(...)`.
3. Dùng danh sách role code như `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`, `STUDENT`; không thêm tiền tố `ROLE_`.
4. Kiểm thử payload bằng cách decode phần payload của token trong test, không bỏ qua kiểm tra signature trong production flow.
5. Cập nhật tài liệu mô tả JWT từ “không có role claim” thành contract mới.

Lý do chọn mảng: domain cho phép một user có nhiều role; dùng string đơn sẽ làm mất thông tin hoặc buộc phải tự định nghĩa primary-role rule chưa có trong contract.

## 6. Phạm vi mã nguồn dự kiến

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/security/JwtTokenService.java`
  - Chỉnh payload tạo access token để thêm claim `role`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/security/UserPrincipal.java`
  - Chỉnh hoặc bổ sung logic expose role code canonical cho token service nếu cần.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/security/...`
  - Bổ sung test mapping/payload role.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/user/controller/AuthControllerIntegrationTest.java`
  - Assert login token có claim `role`.
- `document/application-doc/v2/frontend-api/01-auth-student.md`
  - Cập nhật JWT contract và giới hạn sử dụng role claim.
- `document/dev-note/be/user-auth/`
  - Tạo Dev Note sau implementation.

## 7. API / Database / Integration

Không thay đổi endpoint, request/response DTO, database schema hoặc migration.

Login vẫn trả access token trong `data.access_token`; thay đổi chỉ nằm ở payload bên trong JWT:

```text
sub
user_id
role: string[]
iat
exp
```

Token cũ không có claim `role` vẫn có thể được backend xử lý theo flow hiện tại vì filter không dùng claim này để cấp authorities.

## 8. Test và validation

### Test cases

- User có một role: claim `role` chứa đúng một role code.
- User có nhiều role: claim chứa đầy đủ role code và được sort.
- Claim không chứa dạng authority `ROLE_ADMIN`.
- Login integration trả token có claim `role`.
- Account/logout và authorization hiện tại vẫn hoạt động như trước.
- Token malformed, sai signature và expired vẫn trả về status hiện tại.

### Validation commands

Chạy từ `BE/BaiTap-RS`:

```bash
./gradlew.bat test --tests '*Jwt*'
./gradlew.bat test --tests '*AuthControllerIntegrationTest'
./gradlew.bat test
./gradlew.bat checkstyleMain checkstyleTest
./gradlew.bat pmdMain pmdTest
./gradlew.bat build
```

JaCoCo chỉ được báo cáo nếu task `jacocoTestReport` tồn tại và chạy thành công.

## 9. Rủi ro và giảm thiểu

- Client có thể đọc role claim: không coi claim này là bằng chứng authorization.
- User nhiều role có thể tạo payload lớn hơn: role hiện tại ít và giới hạn; không thêm permission chi tiết vào token.
- Role trong database thay đổi sau khi token phát hành: backend vẫn reload authorities từ database, nên authorization không phụ thuộc claim cũ.
- Token cũ không có claim mới: giữ tương thích đọc token vì claim `role` không bắt buộc cho filter hiện tại.

## 10. Output dự kiến

- Access token mới chứa claim `role` dạng mảng role code canonical.
- JWT authentication và backend authorization giữ nguyên hành vi.
- Unit/integration test bảo vệ contract claim mới.
- Tài liệu auth phản ánh đúng trạng thái JWT sau implementation.
- Có Dev Note ghi nhận thay đổi và kết quả validation.

## 11. Trạng thái

- Status: Approved; implementation completed.
- Approval: User confirmed "approved 58" on 2026-09-02.
