# Integration Boundaries v5

## 1. Identity

Owner: existing `user`/identity domain.

Library được:

- reference `app_user.user_id`;
- read minimal principal/profile view;
- authorize owner.

Library không được:

- quản lý password;
- tạo login flow khác;
- thay JWT format chỉ cho Library;
- duplicate User/Student/Teacher.

## 2. Student/Teacher

Student/Teacher là nguồn profile canonical cho school-person metadata.

Library Patron chỉ lưu Library state.

Nếu một user chưa có Student/Teacher profile nhưng ADMIN/LIBRARIAN cần patron, support policy phải được chốt; baseline không tự suy đoán actor type.

## 3. Notification/Mail

Reuse notification/mail infrastructure khi gửi:

- overdue reminder;
- card expiry reminder;
- reservation-ready notification.

Không mặc định tạo `outbox_email` subsystem thứ hai.

Nếu existing notification infrastructure không đảm bảo atomic delivery requirement, Developer Plan có thể thêm integration event/outbox sau gap analysis.

## 4. Batch

Reuse Spring Batch configuration/tables hiện hữu.

Library thêm job definitions/listener/business summary khi cần; không initialize Batch metadata một lần nữa.

## 5. Security

Reuse:

- SecurityConfiguration;
- JwtAuthenticationFilter/JwtTokenService;
- method security;
- common authentication/authorization error handling.

Library thêm role/rules, không thêm SecurityFilterChain cạnh tranh nếu không cần.

## 6. Audit

Reuse common `AuditUtil`/project audit conventions.

Library history-critical events có thể có domain audit table nếu generic createdBy/updatedBy không đủ.

## 7. AI

Reuse Spring AI `ChatClient`/model configuration hiện hữu.

Library AI service không được gọi repository từ model tool context một cách unrestricted.

Structured request/result phải đi qua application validation.

## 8. Frontend

Reuse:

- current authenticated session handling;
- router shell;
- services/types patterns;
- PrimeVue design system;
- Storybook/Vitest.

Không thêm login page/library auth store mới.

## 9. Deployment

Library deployment là cùng application artifact.

Các env/config mới phải có safe defaults để Library AI/batch optional features có thể disable mà core school-management vẫn chạy.

## 10. API coexistence

Existing API versions tiếp tục chạy.

Library route allocation:

1. `/api/v2/{resource}` nếu clear;
2. `/api/v2/library/{resource}` nếu collision;
3. không bump unrelated APIs;
4. breaking Library contract sau này mới tạo `/api/v3/...`.

Student v1/v2/v3 coexistence là precedent cho contract-version coexistence.
