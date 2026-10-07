# Module 01 — Library Catalog

## Scope

Quản lý `Book` và `BookCopy`.

`Book` là metadata của đầu sách. `BookCopy` là vật thể có thể được mượn.

Không gộp hai khái niệm.

## Domain

### Book

Tối thiểu:

- `id`
- `isbn` nullable/unique theo policy
- `title`
- `author`
- `publisher`
- `publishedYear`
- `category`
- `listPrice`
- `coverUrl`
- AI enrichment fields nếu feature bật
- audit/lifecycle fields theo convention repo

### BookCopy

- `id`
- `bookId`
- `barcode`
- `shelfLocation`
- `status`
- `referenceOnly`
- audit/lifecycle fields nếu cần

Status baseline:

```text
AVAILABLE
ON_LOAN
RESERVED
LOST
DAMAGED
WITHDRAWN
```

Service không được coi status field là hàng rào duy nhất cho active-loan concurrency; xem MigrationAndConcurrency.

## API

Baseline:

```text
GET    /api/v2/books
GET    /api/v2/books/{id}
POST   /api/v2/books
PUT    /api/v2/books/{id}
DELETE /api/v2/books/{id}

POST   /api/v2/books/{id}/copies
GET    /api/v2/book-copies/{barcode}
GET    /api/v2/book-copies/{barcode}/barcode.png
GET    /api/v2/book-copies/labels.pdf?ids=...
```

Mutation: LIBRARIAN hoặc ADMIN.

Catalog read: authenticated platform users trong baseline. Public anonymous catalog cần amendment vì platform Security hiện mặc định authenticated cho non-auth APIs.

## Search

Backend hỗ trợ:

- keyword;
- category;
- published-year range;
- availability;
- pagination;
- sort allowlist.

Không dùng client-side filtering giả trên một page.

AI natural-language search phải translate sang filter contract này rồi gọi cùng query service.

## Acceptance

- CRUD không expose JPA entity trực tiếp.
- Barcode unique.
- Batch-create copies atomic theo transaction boundary được plan chốt.
- Reference-only copy không thể tạo loan.
- Delete book có history không làm mất circulation/fine history.
