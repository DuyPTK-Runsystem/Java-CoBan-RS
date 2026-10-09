# Module 03 — Circulation and Reservation

## Loan flow

Baseline quầy mượn:

1. identify patron bằng signed card hoặc librarian lookup;
2. validate card + patron eligibility;
3. scan/add copy barcodes;
4. validate business rules;
5. commit loans atomically theo request strategy;
6. trả due date và summary.

## Business rules

- Max active loans: policy-configured; default 5.
- Default due: borrowed date + policy-configured duration; default 14 days.
- Reference-only copy không được mượn.
- Max renew count: policy-configured; default 2.
- Mỗi renew cộng policy-configured duration; default +7 days.
- Không renew khi reservation queue tạo conflict theo policy.
- Copy chỉ có tối đa một active loan.
- Borrowing-suspended patron không tạo loan mới.
- Lost copy không borrowable.

## Concurrency

Service-level:

```text
if (copy.isAvailable()) { createLoan(); }
```

không đủ.

Implementation phải dùng DB/transaction-level protection theo `data-model/MigrationAndConcurrency.md`.

Concurrent acceptance:

> 10 concurrent attempts cùng mượn một copy → đúng 1 thành công; các request còn lại nhận stable conflict code.

## Return flow

`POST /api/v2/returns` nhận barcode hoặc list barcode theo Developer Plan.

Backend:

- resolve active loan;
- set returned timestamp/state;
- update copy availability;
- calculate fine as-of return date;
- preserve audit.

Return không yêu cầu quét patron card.

## Reservation

Reservation gắn `bookId`, không gắn một copy cố định cho tới khi allocation policy chọn copy.

Status baseline:

```text
WAITING
READY
FULFILLED
CANCELLED
EXPIRED
```

Queue order mặc định theo `reservedAt`, có tie-breaker stable.

Pickup window là `TBD-V5-LIB-002`.

## Lost

Librarian đánh dấu LOST:

- active loan → LOST;
- copy → LOST;
- create/update fine component theo policy;
- audit actor/reason.

Không xóa copy/loan.

## API

```text
POST /api/v2/loans
POST /api/v2/loans/{loanId}/renew
POST /api/v2/returns
POST /api/v2/reservations
POST /api/v2/reservations/{id}/cancel
POST /api/v2/book-copies/{barcode}/lost
```

Self-service reservation: STUDENT/TEACHER patron.

Loan/return/lost: LIBRARIAN/ADMIN.

## Stable error examples

```text
COPY_ALREADY_ON_LOAN
MAX_ACTIVE_LOANS
REFERENCE_ONLY
PATRON_BORROWING_SUSPENDED
CARD_EXPIRED
CARD_REVOKED
RENEW_LIMIT_REACHED
COPY_RESERVED
COPY_NOT_FOUND
ACTIVE_LOAN_NOT_FOUND
```
