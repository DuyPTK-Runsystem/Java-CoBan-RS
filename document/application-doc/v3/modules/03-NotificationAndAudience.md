# Module 03 — Targeted In-app Notification

## Audience

- `INDIVIDUAL`: danh sách user/student hợp lệ.
- `CLASS`: membership snapshot hoặc resolution strategy được phê duyệt tại thời điểm
  phát hành.
- `SCHOOL`: toàn bộ audience trong phạm vi trường theo policy.

## Data and lifecycle

Thông báo có title/body, creator, audience type/reference, publishAt, expiry (nếu có),
status và correlation/idempotency key. Receipt có recipient, readAt và access scope.

```text
 DRAFT -> SCHEDULED -> PUBLISHED -> EXPIRED
                    \-> CANCELLED
```

Người nhận chỉ đọc được notification/receipt trong scope đã được backend kiểm tra.
Gửi lại cùng idempotency key không tạo duplicate.

## Contract boundary

Plan 076 phải chốt membership snapshot, template/content policy, role matrix, retention
và delivery channel. CR này chỉ yêu cầu in-app; email/push là `TBD-004`.
