# Module 02 — Library Patron and Card

## Mục tiêu

Biểu diễn quyền sử dụng thư viện mà không duplicate identity.

## LibraryPatron

```text
library_patron
- patron_id PK
- user_id FK/UK -> app_user
- status
- joined_at
- borrowing_suspended_at nullable
- borrowing_suspended_reason nullable
- created_at/updated_at
```

Một `app_user` có tối đa một patron.

Không lưu duplicate username/password/studentCode/teacherCode.

Display name/contact được resolve từ canonical profile hiện hữu theo actor type hoặc application read model.

## Eligibility

Baseline borrower:

- STUDENT có account + patron ACTIVE;
- TEACHER có account + patron ACTIVE.

LIBRARIAN có thể đồng thời có patron nếu có nhu cầu mượn; role và patron là hai chiều độc lập.

ACADEMIC_OFFICE không mặc nhiên có borrower/library-admin capability nếu chưa có patron/role tương ứng.

## Lifecycle

```text
ACTIVE
BORROWING_SUSPENDED
CLOSED
```

`BORROWING_SUSPENDED` không disable `app_user`.

Auto suspension do fine và manual suspension nếu sau này có phải được phân biệt bằng reason/source để tránh auto-unlock sai manual suspension.

## Card

```text
membership_card
- id
- patron_id
- card_no
- issued_at
- expires_at
- status
- payload_version
- revoked_at/reason nullable
```

Status:

```text
ACTIVE
EXPIRED
REVOKED
```

Mỗi patron có tối đa một ACTIVE card.

## QR payload

Baseline:

```text
v1|{cardNo}|{patronId}|{expEpochDay}|{signature}
```

Signature: HMAC-SHA256 bằng secret từ runtime configuration.

Verify:

1. parse/version;
2. constant-time signature compare;
3. expiry;
4. DB lookup card;
5. ACTIVE state;
6. patron state/eligibility theo operation.

Signature hợp lệ không đồng nghĩa thẻ đang ACTIVE.

## API

```text
GET  /api/v2/library-patrons
POST /api/v2/library-patrons
GET  /api/v2/library-patrons/{patronId}
GET  /api/v2/library-patrons/{patronId}/loans
GET  /api/v2/library-patrons/{patronId}/fines

POST /api/v2/library-cards
GET  /api/v2/library-cards/{cardNo}/qr.png
GET  /api/v2/library-cards/{cardNo}/card.pdf
POST /api/v2/library-cards/verify
POST /api/v2/library-cards/{cardNo}/revoke

GET  /api/v2/library-patrons/me
GET  /api/v2/library-cards/me
```

`POST /library-patrons` nhận `userId`, không nhận một payload để tạo Student/Teacher/User mới.

## Acceptance

- Không tạo duplicate Member identity.
- Patron A không đọc patron B nếu chỉ có borrower capability.
- Librarian/Admin lookup được patron theo code/user reference hợp lệ.
- Revoke card làm payload cũ fail dù HMAC đúng.
- Fine suspension không làm user mất quyền ở module học vụ.
