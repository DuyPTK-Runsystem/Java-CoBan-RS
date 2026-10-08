# Migration and Concurrency — MySQL

## 1. Active loan invariant

Invariant:

> Một `book_copy` có tối đa một loan chưa trả.

PostgreSQL source dùng partial unique index:

```sql
UNIQUE(copy_id) WHERE returned_at IS NULL
```

MySQL không hỗ trợ partial unique index theo cú pháp đó.

## 2. Recommended MySQL strategy

Developer Plan nên ưu tiên generated active key:

```sql
ALTER TABLE loan
  ADD COLUMN active_copy_id BIGINT
    GENERATED ALWAYS AS (
      CASE WHEN returned_at IS NULL THEN copy_id ELSE NULL END
    ) STORED,
  ADD CONSTRAINT uk_loan_active_copy UNIQUE (active_copy_id);
```

MySQL unique index cho phép nhiều `NULL`, vì vậy:

- returned loans → `active_copy_id = NULL`;
- active loan → `active_copy_id = copy_id`;
- active loan thứ hai cho cùng copy → unique violation.

Tên/cú pháp cuối cùng phải được verify với exact Azure MySQL/MySQL version trước migration.

Alternative nếu generated column không phù hợp: explicit lock/allocation table hoặc transaction-safe state transition có unique key. Service-only `if available` không đạt requirement.

## 3. Transaction flow

Borrow transaction tối thiểu:

1. load/lock relevant `book_copy` hoặc rely on unique invariant;
2. validate state/reference/card/patron/max-loans;
3. insert loan;
4. update copy state;
5. commit.

Unique violation do race được map thành stable `COPY_ALREADY_ON_LOAN`, không leak SQL exception.

## 4. Fine idempotency

Constraint:

```text
UNIQUE(loan_id, fine_type)
```

Overdue writer tính total as-of `runDate`, không cộng amount cũ.

Update only when:

- fine is UNPAID;
- new `calculatedThrough` > current.

MySQL SQL cụ thể được chọn ở implementation; JPA/service conditional update hoặc MySQL upsert đều được nếu atomicity/test chứng minh đúng.

## 5. Card active uniqueness

MySQL không có partial unique index `WHERE status='ACTIVE'`.

Options:

1. generated `active_patron_id = CASE WHEN status='ACTIVE' THEN patron_id ELSE NULL END` + unique;
2. lock patron row khi issue/reissue;
3. separate current-card pointer.

Developer Plan phải chọn một strategy và có concurrent issuance test nếu API có thể bị gọi song song.

## 6. Test database

DB-specific integration tests phải chạy trên MySQL-compatible Testcontainers/image matching production major version.

H2 có thể giữ cho unit/slice tests không phụ thuộc dialect, nhưng không được dùng làm bằng chứng cho:

- generated-column uniqueness;
- lock behavior;
- MySQL upsert;
- concurrency invariant.

## 7. Migration safety

- additive trước destructive;
- không rename/drop existing platform tables trong v5 initial migration;
- FK đến `app_user` không cascade delete Library history;
- create indexes cho active-loan lookup, patron history, due-date batch scan, reservation queue;
- production data repair phải có plan riêng nếu constraint addition phát hiện legacy conflict.
