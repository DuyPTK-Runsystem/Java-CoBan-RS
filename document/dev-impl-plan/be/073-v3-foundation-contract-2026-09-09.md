# Plan 073 — v3 Foundation and Cross-cutting Contract

## Status

`DRAFT - chờ approval`; không triển khai code trước khi người dùng phê duyệt.

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
