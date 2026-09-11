# Plan 073 FE — v3 Foundation and Cross-cutting Contract

## Status

`IMPLEMENTED - FE validation PASS; wireframe vẫn review-only`:
[`073-v3-foundation-contract`](../../../wireframes/fe/v3/073-v3-foundation-contract/README.md).

Amendment khi lập Plan 077 (`2026-09-10`): typed v3 page response riêng bị supersede;
toàn hệ thống dùng `ResultPaginationDTO`. FE type/fixture cũ được xóa hoặc migrate trong
implementation Plan 077.

## Mục tiêu

Chuẩn bị FE boundary để phát triển song song với BE từ v3: typed contract fixtures,
query/filter state, capability-aware action states, lỗi `401/403/404/409`, và reviewable
shell cho các lát placement, timetable, notification, score import và lesson log.

## In-scope

- Rà soát ApplicationContext/RequirementBaseline/CR v3 và contract checkpoint Plan 073.
- Định nghĩa typed FE service boundary theo contract được duyệt; không tự tạo DTO/API.
- Wireframe/Storybook states cho loading, empty, error, conflict, permission denied và
  review-only states.
- Quy ước URL/query state cho search/filter/pagination dùng lại ở Plan 077.

## Out-of-scope

- Không xây màn hình nghiệp vụ hoàn chỉnh của Plan 074–079.
- Không suy diễn role, endpoint, enum hoặc định mức tiết/tuần còn `TBD`.
- Không gọi fixture/jsdom là browser E2E.

## BE/FE synchronization

FE bắt đầu bằng fixture contract sau khi contract slice của Plan 073 được phê duyệt;
FE có thể review Storybook trước khi endpoint thật hoàn tất. Khi BE contract test ổn
định, thay transport fixture bằng typed API service và chạy integration gate trong cùng
plan.

## Validation dự kiến

FE lint, unit/component tests, coverage, build và Storybook build theo script thực tế.
Browser/live walkthrough: `NOT RUN` nếu chưa có môi trường resettable và đủ role fixture.

## Output

Wireframe được duyệt, Storybook contract states, shared query/error contract và checklist
đủ điều kiện để bắt đầu Plan 074.
