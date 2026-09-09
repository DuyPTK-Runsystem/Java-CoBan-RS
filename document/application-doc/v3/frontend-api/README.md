# v3 Frontend API Boundary

FE chỉ dùng typed service theo contract được phê duyệt. View/component không tự lắp
endpoint hoặc thay thế backend authorization.

## Shared response/error expectations

- List response phải biểu diễn items, page, pageSize, total và applied filters khi có.
- Mutation lỗi cần phân biệt `400`, `401`, `403`, `404`, `409`, `422` theo contract module.
- `401` xóa session và đưa về login; `403` giữ session và hiển thị không đủ quyền.
- `409` phải giữ input/reload guidance, không tự retry mutation mù.

## FE/BE parallel checkpoint

FE có thể dùng fixture contract để dựng Storybook và component test ngay sau khi contract
slice được duyệt. Khi BE endpoint ổn định, typed service chuyển từ fixture sang transport
thật và chạy integration test trong cùng plan. Browser/live evidence được báo riêng, không
đánh đồng với Vitest/jsdom.

## Per-module API status

Placement, timetable, notification, score import và lesson-log endpoint/DTO/enum: `TBD`
cho đến Plan 073 contract checkpoint và plan capability tương ứng.
