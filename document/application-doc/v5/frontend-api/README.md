# v5 Frontend and API Contract

## 1. API prefix

Default: `/api/v2`.

Baseline endpoints:

### Catalog

```text
GET    /api/v2/books
GET    /api/v2/books/{id}
POST   /api/v2/books
PUT    /api/v2/books/{id}
DELETE /api/v2/books/{id}

POST   /api/v2/books/{id}/copies
GET    /api/v2/book-copies/{barcode}
GET    /api/v2/book-copies/{barcode}/barcode.png
GET    /api/v2/book-copies/labels.pdf
```

### Patron/Card

```text
GET  /api/v2/library-patrons
POST /api/v2/library-patrons
GET  /api/v2/library-patrons/{id}
GET  /api/v2/library-patrons/{id}/loans
GET  /api/v2/library-patrons/{id}/fines
GET  /api/v2/library-patrons/me

POST /api/v2/library-cards
GET  /api/v2/library-cards/me
GET  /api/v2/library-cards/{cardNo}/qr.png
GET  /api/v2/library-cards/{cardNo}/card.pdf
POST /api/v2/library-cards/verify
POST /api/v2/library-cards/{cardNo}/revoke
```

### Circulation

```text
POST /api/v2/loans
POST /api/v2/loans/{id}/renew
POST /api/v2/returns
POST /api/v2/reservations
POST /api/v2/reservations/{id}/cancel
POST /api/v2/book-copies/{barcode}/lost
```

### Fine/Batch

```text
GET  /api/v2/fines/{id}
POST /api/v2/fines/{id}/pay
POST /api/v2/fines/{id}/waive

GET  /api/v2/library/batch-jobs
POST /api/v2/library/batch-jobs/overdue-fine
```

### AI

```text
POST /api/v2/library/ai/book-metadata
POST /api/v2/library/ai/search
GET  /api/v2/library/ai/recommendations
POST /api/v2/library/ai/policy-chat
```

## 2. Collision rule

Trước implementation, scan all controllers trên target branch.

Nếu `/api/v2/books`, `/api/v2/loans` hoặc resource khác đã có unrelated contract, chuyển affected group thành:

```text
/api/v2/library/books
/api/v2/library/loans
```

Không đổi toàn bộ API nếu chỉ một group conflict.

## 3. FE routes

Proposed:

```text
/library/books
/library/books/:id
/library/books/new
/library/circulation
/library/patrons
/library/patrons/:id
/library/me/card
/library/me/loans
/library/me/fines
/library/me/recommendations
/library/jobs
```

Không tạo `/login` riêng.

## 4. Source layout

Follow current FE architecture, ví dụ:

```text
src/
  views/library/
  components/library/
  services/library/
  types/library/
  composables/
```

Không bắt buộc exact folder nếu repo convention tại implementation time khác.

## 5. UI states

Mỗi async surface có:

- loading;
- success;
- empty;
- error;
- forbidden nếu relevant.

Circulation thêm:

- camera unavailable;
- permission denied;
- manual input;
- invalid card;
- suspended patron;
- duplicate copy scan;
- conflict on confirm;
- partial validation summary nếu multi-copy request.

## 6. Circulation UX

Mục tiêu: thao tác nhanh, có thể dùng camera hoặc keyboard scanner.

```text
Patron panel              Borrow basket
-------------             ---------------------
name/code                 scanned copies
card status               title + barcode
active 3/5                validation state
unpaid fine               due date preview
manual input              confirm
```

FE preview không thay backend validation.

## 7. API client

Reuse project transport/auth handling.

Component không được tự tạo auth token logic.

Service functions return typed DTOs and normalize common errors theo project convention.

## 8. Storybook/tests

Tối thiểu stories/components cho:

- catalog search states;
- book-copy status;
- patron summary ACTIVE/SUSPENDED;
- card ACTIVE/EXPIRED/REVOKED;
- circulation scanner states;
- fine summary;
- AI disabled/loading/invalid/fallback states.

Browser evidence cho camera cần HTTPS/localhost awareness.
