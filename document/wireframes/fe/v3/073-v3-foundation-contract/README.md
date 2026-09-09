# Wireframe 073 — v3 Foundation Contract

Status: `DRAFT - reviewable, chưa được duyệt`.

Wireframe này mô tả các state dùng chung, không phải màn hình production hoàn chỉnh.

```text
┌─────────────────────────────────────────────────────────────────────┐
│ App shell / context: Năm học · Học kỳ · Module                      │
├─────────────────────────────────────────────────────────────────────┤
│ Page title                         [Search ........] [Filter ▾]      │
│                                                                     │
│ [Loading state]  [Empty state]  [Permission denied 403]             │
│ [Not found 404]  [Conflict 409]  [Retry]                            │
│                                                                     │
│ Review-only banner: Contract/fixture preview — chưa publish          │
│                                                                     │
│ Table/list placeholder                                               │
│   identity | status | scope | updatedAt | actions                   │
│                                                                     │
│ Pagination: [Previous] 1 2 3 [Next]                                  │
└─────────────────────────────────────────────────────────────────────┘
```

## Review questions

- Có cần hiển thị context năm học/học kỳ trên mọi lát v3 không?
- Action nào phải ẩn khi thiếu capability, action nào được hiển thị disabled với lý do?
- Có giữ query/filter trong URL khi chuyển trang hoặc refresh không?
- Copy và mức chi tiết của lỗi conflict có phù hợp cho người dùng nghiệp vụ không?

## Review gate

Chỉ chuyển từ review/fixture sang production FE sau khi contract checkpoint và wireframe
được người dùng phê duyệt qua agent message.
