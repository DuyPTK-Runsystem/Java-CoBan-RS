# Contract, Migration và Scope Freeze

## Trạng thái

- Phiên bản tài liệu ứng dụng: v2.
- Nguồn quyết định: Developer Plan 025, được người dùng phê duyệt ngày 2026-08-20.
- Phạm vi thực thi: local/test database; không phải production cutover runbook.

## Contract legacy được giữ

- Giữ JWT stateless, `RestResponse` và các endpoint `/api/v1/auth/**`, `/api/v1/students/**` hiện có.
- Student API hiện chỉ chấp nhận `ADMIN`, `ACADEMIC_OFFICE` hoặc `TEACHER` qua `@PreAuthorize`.
- `STUDENT` không được truy cập Student API hiện tại vì API chưa có ranh giới dữ liệu theo học sinh/lớp.
- Tài khoản tạo mới vẫn không được tự gán role. Luồng quản trị role cho tài khoản mới là scope riêng.

## Migration foundation đã chốt

1. Dùng Flyway.
2. Schema trống chạy tuần tự V1 legacy baseline, V2 rename `user` thành `app_user`, rồi V3 tạo role/user_role và seed role.
3. Schema legacy được Flyway baseline tại version 1, sau đó chạy V2/V3.
4. Giữ nguyên tên cột `password` và giá trị BCrypt hash; không đổi tên thành `password_hash` và không double-hash.
5. Mọi user legacy hiện có được gán role `ADMIN` trong V3. Quyết định này áp dụng cho database training hiện có hai tài khoản.
6. Seed role gồm `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER` và `STUDENT`.
7. `average_score` tiếp tục tồn tại như dữ liệu legacy deprecated cho tới khi luồng tính average score mới hoàn tất; không được dùng để tạo transcript hoặc điểm chính thức.

## Scope freeze

- Không triển khai module academic, enrollment, attendance, score, transcript hoặc calculation worker trong foundation này.
- Không đổi response shape hoặc endpoint legacy.
- Không chạy migration trên database production, không tạo backup/rollback production và không tuyên bố production readiness.
- Trước khi chạy migration trên một database legacy khác, xác minh cột `password` chứa BCrypt hash hợp lệ. Nếu không hợp lệ, dừng để quyết định reset password.

## Sai khác có chủ đích với data model mục tiêu

Data model v2 dùng tên cột `password_hash`. Trong compatibility window của Plan 025, schema triển khai giữ `password` để bảo toàn database legacy và contract code hiện có. Đổi tên cột chỉ được thực hiện trong một plan/CR riêng có migration và regression test tương ứng.
