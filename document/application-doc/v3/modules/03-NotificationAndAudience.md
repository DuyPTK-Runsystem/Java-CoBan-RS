# Module 03 — Targeted Notification

## Audience

- `INDIVIDUAL`: danh sách user/student hợp lệ.
- `CLASS`: membership snapshot hoặc resolution strategy được phê duyệt tại thời điểm
  phát hành.
- `SCHOOL`: toàn bộ audience trong phạm vi trường theo policy.

## Data and lifecycle

Thông báo có title/body, creator, audience type/reference, publishAt, expiry (nếu có),
status và correlation/idempotency key. Receipt có recipient, readAt và access scope.

Kênh `IN_APP` là mặc định khi request bỏ trống `channel`; `EMAIL` gửi cùng title/body
đến từng recipient qua Spring Mail. Email delivery được ghi độc lập trên receipt với
`PENDING`, `SENT` hoặc `FAILED`; thiếu cấu hình sender, thiếu email recipient hoặc lỗi
SMTP không làm hỏng publish/receipt của recipient khác.

```text
 DRAFT -> SCHEDULED -> PUBLISHED -> EXPIRED
                    \-> CANCELLED
```

Người nhận chỉ đọc được notification/receipt trong scope đã được backend kiểm tra.
Gửi lại cùng idempotency key không tạo duplicate.

## Contract boundary

Plan 076 phải chốt membership snapshot, template/content policy, role matrix, retention
và delivery channel. Plan 076 amendment mở `IN_APP` và `EMAIL`; push vẫn thuộc `TBD-004`.
Luồng semester notification v2 và bảng `semester_completeness_notification` không thuộc
contract này.
