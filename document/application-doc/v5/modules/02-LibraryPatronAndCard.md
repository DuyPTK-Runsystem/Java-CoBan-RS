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

Plan 097 approved borrower eligibility:

- Any user with a patron in an eligible state may borrow, except users whose single role is `ADMIN` or `LIBRARIAN`.
- The current role model assigns exactly one role per user. Reuse the shared role enum/constants; do not compare hardcoded role-name strings.

`ADMIN` and `LIBRARIAN` are excluded from borrowing eligibility. Other role categories, including `ACADEMIC_OFFICE`, are eligible when a valid patron exists. Backend authorization remains authoritative.

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
library_card
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

### Mẫu thiết kế thẻ thư viện (Card Template)

Mẫu thiết kế thẻ in và hiển thị số (chuẩn CR-80) được lưu trữ tại [`../assets/sample-library-card.jpg`](../assets/sample-library-card.jpg) và wireframe tại [`document/wireframes/fe/library/097-library-patron-and-card/`](../../../wireframes/fe/library/097-library-patron-and-card/README.md).

Thẻ bao gồm:
- Header: Cơ quan chủ quản (`SỞ GIÁO DỤC VÀ ĐÀO TẠO`), Đơn vị trường (`TRƯỜNG THCS NGUYỄN X`), biểu trưng logo.
- Tiêu đề: `THẺ THƯ VIỆN`.
- Khối trái: Mã QR động (ký HMAC-SHA256) và `MÃ THẺ` (ví dụ `LIB-0001234`).
- Khối phải: Họ và tên, Lớp, Mã độc giả, Ngày sinh, Hiệu lực thẻ.
- Họa tiết nền chìm hoa sen và vệt xiên trang trí.

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
GET  /api/v2/library-patrons/activation-candidates
POST /api/v2/library-patrons
GET  /api/v2/library-patrons/{patronId}
GET  /api/v2/library-patrons/{patronId}/cards
POST /api/v2/library-cards
GET  /api/v2/library-cards/{cardNo}/qr.png
POST /api/v2/library-cards/verify
POST /api/v2/library-cards/{cardNo}/revoke
POST /api/v2/library-cards/{cardNo}/reissue

GET  /api/v2/library-patrons/me
GET  /api/v2/library-cards/me
GET  /api/v2/library-cards/me/history
```

`POST /library-patrons` nhận `userId`, không nhận một payload để tạo Student/Teacher/User mới.
Activation candidates trả về tài khoản chưa có patron; role không giới hạn việc tạo patron. Quy tắc loại `ADMIN`/`LIBRARIAN` áp dụng cho borrower eligibility, không áp dụng cho activation.

Card issuance nhận `expiresAt` là ngày hết hạn. Giao diện cho phép nhập số tháng hoặc nhập ngày trực tiếp; chế độ số tháng được tính ở FE, và cả hai chế độ đều gửi ngày hết hạn cho BE xử lý. Ngày hết hạn là ngày còn hiệu lực, với thời điểm hết hạn tính từ đầu ngày tiếp theo theo `Asia/Ho_Chi_Minh`.

Reissue thay thế nguyên tử qua API riêng: thu hồi thẻ cũ và tạo thẻ mới trong cùng giao dịch, kèm lý do và audit cho cả hai thay đổi. Lịch sử thẻ chỉ đọc; admin/librarian xem lịch sử theo patron, owner xem lịch sử của chính mình.

Loan, return, renewal, lost, circulation transaction/copy locking, due-date calculation và lost-book handling thuộc Plan 098. Các màn hình Plan 097 không triển khai hoặc thay thế các luồng đó.

## Acceptance

- Không tạo duplicate Member identity.
- Patron A không đọc patron B nếu chỉ có borrower capability.
- Librarian/Admin lookup được patron theo code/user reference hợp lệ.
- Revoke card làm payload cũ fail dù HMAC đúng.
- Fine suspension không làm user mất quyền ở module học vụ.
