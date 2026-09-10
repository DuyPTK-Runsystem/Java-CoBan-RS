# Plan 073 — v3 Foundation and Cross-cutting Contract

## Status

`IMPLEMENTED - backend PMD baseline còn mở; xem Dev Note 073`.

## Mục tiêu

Chốt contract nền cho CR-V3-001 để BE và FE có thể phát triển song song theo vertical
slice: vocabulary, role/capability matrix, error model, audit, optimistic locking,
pagination/filter conventions và fixture data.

## In-scope

- So sánh v2 contract hiện tại và xác định phần reuse/extension.
- Contract decision cho 074–079, không triển khai toàn bộ feature trong Plan 073.
- BE skeleton contract tests/DTO boundary nếu được approve.
- FE typed boundary, shared query state pattern và Storybook review states nếu được approve.

## Out-of-scope

- Thuật toán xếp lớp, timetable engine, delivery notification, score import commit,
  lesson-log CRUD.
- Chốt con số định mức tiết/tuần khi chưa có văn bản nguồn.
- Invent endpoint/DTO/role chưa được contract checkpoint duyệt.

## Validation dự kiến

- BE: focused contract/integration tests, Checkstyle, PMD, build theo BE workflow.
- FE: lint, unit/component tests, coverage, build, Storybook build theo `FE/package.json`.
- Browser/live: `NOT RUN` nếu chưa có resettable environment và session/fixture đủ role.

## Output

CR amendment/requirement IDs, API/data decision record, role/error matrix, fixture
catalog, matching FE wireframe/Storybook checklist và Plan 074–079 entry criteria.

## Unit test plan

- `V3PageResponse`: copy immutable `items`/`appliedFilters`, reject negative page/total và
  pageSize không dương.
- Không thêm error code/error payload v3; capability plan phải đề xuất semantics chi tiết cùng
  API contract nếu thực sự cần.
- FE review: 401 thể hiện rõ boundary chuyển hướng đăng nhập, còn 403/404/409 giữ state review
  tương ứng.
- Query serializer: chỉ ghi page/pageSize safe integer; pageSize nguồn không hợp lệ quay về
  default do capability truyền vào, không tự suy diễn giá trị `1`.
- Query/endpoint capability chưa tồn tại không được mock thành API integration test trong
  Plan 073; từng Plan 074–079 phải thêm contract/integration test của endpoint thật.
