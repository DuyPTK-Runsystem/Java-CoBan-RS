# Module 01 — Rule-based Placement and Enrollment

## Mục tiêu

Mở rộng xếp lớp hiện có thành phiên phân bổ có tiêu chí, preview, xác nhận và giải
thích kết quả.

## Input và rule

- Scope: academic year, grade, source student set, target classes và capacity.
- Criterion: `SCORE`, `GENDER` và criterion được phê duyệt khác; mỗi criterion phải
  khai báo source field, direction, priority/weight và missing-data behavior.
- Tie-breaker: deterministic, có version và được lưu cùng phiên.

## Lifecycle

```text
DRAFT -> SIMULATED -> READY_FOR_CONFIRM -> CONFIRMED
                           \-> REJECTED/CANCELLED
```

Chỉ `CONFIRMED` mới tạo enrollment/assignment. Học sinh đã có attendance hoặc score
không bị ghi đè lịch sử; việc chuyển lớp cần policy riêng.

## Contract boundary

Plan 074 phải định nghĩa request/response và error codes sau khi Plan 073 chốt identity,
authorization và audit. Tên endpoint cụ thể hiện là `TBD`; không được FE tự đoán.

## Acceptance

- Cùng input + rule version cho cùng kết quả.
- Preview hiển thị capacity, thiếu dữ liệu, lý do xếp và conflict.
- Confirm có optimistic lock/idempotency và audit actor/time/rule version.
