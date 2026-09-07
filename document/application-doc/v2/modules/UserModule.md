# User Module

## Vai trò

File này là contract nền hiện tại của auth/user. Requirement mở rộng về role, teacher và authorization nằm tại [`00-CommonAndAuthModule.md`](00-CommonAndAuthModule.md); schema mục tiêu nằm tại [`../data-model/01-IdentityAndAccess.md`](../data-model/01-IdentityAndAccess.md).

## Contract hiện tại

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/auth/account
POST /api/v1/auth/logout
```

Authentication dùng JWT stateless. Frontend lưu access token và UI-safe user summary trong `localStorage` để dùng chung giữa các tab cùng origin, không lưu password/password hash. Phiên cũ trong `sessionStorage` được chuyển khi đọc lần đầu; logout ngăn khôi phục phiên cũ. Thay đổi phiên từ tab khác làm tab hiện tại tải lại. Phiên có thể tồn tại sau khi đóng trình duyệt; backend kiểm tra hiệu lực JWT. Login chỉ yêu cầu mật khẩu không rỗng, không giới hạn 6–15 ký tự ở FE; validation đăng ký giữ nguyên.

## Quy tắc xử lý phiên

- `401 Unauthorized`: xóa auth state và điều hướng về Login.
- `403 Forbidden`: giữ auth state và hiển thị không đủ quyền.
- Token hết hạn, malformed hoặc sai signature được xử lý như `401`.
- Logout backend trả `204 No Content`; frontend chịu trách nhiệm xóa state cục bộ.

## Ranh giới module

Controller chỉ nhận request/validation và gọi service. Service xử lý credential, duplicate username và token response. Repository chỉ đảm nhiệm truy cập dữ liệu. Không trả password hoặc password hash qua API.

Chi tiết requirement đầy đủ: [`../RequirementBaseline.md`](../RequirementBaseline.md).

