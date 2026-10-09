# Module 04 — Fine and Spring Batch

## Fine policy

Với `d = max(0, daysLate)`, các bậc ngày, đơn giá và trần được đọc từ policy snapshot của lần tính. Giá trị mặc định là:

```text
fine(d) =
min(
  fineCapPerLoan,
  tierOneRate * min(d, tierOneDays)
+ tierTwoRate * clamp(d - tierOneDays, 0, tierTwoDays - tierOneDays)
+ tierThreeRate * max(d - tierTwoDays, 0)
)

Default policy: `fineCapPerLoan=500000`, `tierOneRate=5000`, `tierOneDays=7`, `tierTwoRate=10000`, `tierTwoDays=30`, `tierThreeRate=20000`.
```

Reference values:

| Days late | Fine |
|---:|---:|
| 0 | 0 |
| 1 | 5,000 |
| 7 | 35,000 |
| 8 | 45,000 |
| 30 | 265,000 |
| 31 | 285,000 |
| 45 | 465,000 |
| 60 | 500,000 |
| 365 | 500,000 |

Tiền: `BigDecimal`; DB: `DECIMAL`.

## Fine model

Baseline cần phân biệt reason/type để overdue và lost không làm nhau mơ hồ:

```text
FineType:
OVERDUE
LOST_ITEM
```

Status:

```text
UNPAID
PAID
WAIVED
```

Mỗi loan tối đa một `OVERDUE` fine.

LOST_ITEM representation được Developer Plan chọn giữa cùng bảng `fine` có type hoặc charge component, nhưng phải audit và không phá overdue idempotency.

## Batch job

Tên logical: `libraryOverdueFineJob`.

Default schedule: 00:15 hằng ngày nếu scheduling enabled.

Parameter: `runDate`.

Requirements:

- paging/chunk processing;
- deterministic order;
- restartable;
- idempotent;
- bounded skip policy;
- execution metrics/history;
- manual launch by authorized actor.

Không yêu cầu copy chính xác class/config của source standalone nếu Spring Boot 4 Batch API khác.

## Idempotency

Không cộng incremental fine vào amount cũ.

Job tính lại **total fine through runDate** và:

- insert nếu chưa có;
- update nếu current fine UNPAID và `calculatedThrough < runDate`;
- không giảm fine do rerun ngày cũ;
- không overwrite PAID/WAIVED;
- unique `(loan_id, fine_type)` hoặc equivalent constraint.

Run cùng `runDate` ba lần phải cho cùng business state.

## Patron suspension

Sau fine write, recompute unpaid-total.

```text
unpaidTotal > 500000 -> BORROWING_SUSPENDED
```

Khi debt giảm, chỉ auto-activate nếu suspension source là fine policy và không có manual/other blocking reason.

Không lock `app_user`.

## Notification

Không tạo mail infrastructure thứ hai mặc định.

Job publish/reuse notification/email delivery mechanism hiện hữu. Nếu atomicity yêu cầu outbox mới thì Developer Plan phải chứng minh gap và thiết kế integration rõ.

AI draft reminder là optional enhancement; fallback template luôn tồn tại.

## API

```text
GET  /api/v2/fines/{id}
POST /api/v2/fines/{id}/pay
POST /api/v2/fines/{id}/waive        # ADMIN only nếu được duyệt

GET  /api/v2/library/batch-jobs
POST /api/v2/library/batch-jobs/overdue-fine
```

Payment baseline là ghi nhận nghiệp vụ thu tiền, không payment gateway.

## Test gates

- FineCalculator parameterized cases.
- Same runDate x3 idempotent.
- Old runDate không làm giảm fine.
- Paid fine không bị overwrite.
- Restart sau failure không duplicate business data.
- Skip count đúng.
- Suspension/un-suspension đúng source.
