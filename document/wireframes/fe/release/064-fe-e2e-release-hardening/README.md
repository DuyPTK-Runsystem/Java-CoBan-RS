# Wireframe Plan 064 — FE E2E & Release Hardening

## Mục đích

Wireframe tĩnh để duyệt board/dashboard validation của Plan 064. Nó thể hiện:

- checklist release theo nhóm kiểm thử;
- ma trận role `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`, `STUDENT`;
- trạng thái `PASS`, `FAIL`, `NOT RUN`, `BLOCKED`;
- status matrix `401`, `403`, `404`, `409`;
- persistence, responsive, accessibility, Storybook/build và deploy smoke;
- dependency/blocker của môi trường demo/live backend.

## Cách xem

Mở [`index.html`](index.html) bằng trình duyệt. Dùng bộ lọc `Role` và `Trạng thái` để xem các dòng tương ứng. Đây là fixture độc lập, không gọi backend, không đại diện cho kết quả validation thực tế và không thay thế Vue/PrimeVue production UI.

## Điểm cần duyệt

- Có nên giữ một release board chung hay tách dashboard theo module.
- Cách hiển thị `NOT RUN` khác với `BLOCKED`, và cách nêu blocker ngay cạnh evidence.
- Ma trận quyền bốn role có đủ read/mutation/expected `403` hay không.
- Evidence bắt buộc cho `401/403/404/409`, persistence, keyboard/focus/ARIA, responsive và deploy smoke.
- Cách biểu diễn quyết định cuối: `READY`, `READY WITH BLOCKERS`, `NOT READY`.

## Contract boundary

- Role/capability hiển thị là fixture để review; backend authorization vẫn authoritative.
- `401` phải clear session/đi Login; `403` giữ session; `404` là resource không tồn tại; `409` là conflict/lifecycle/version/calendar/duplicate.
- Không ghi password, token, hash hoặc payload nhạy cảm vào evidence.
- Mọi ô chưa chạy phải giữ `NOT RUN`, không suy diễn thành `PASS` từ unit test hoặc Storybook.

## Liên kết

- Developer Plan draft: [`064-fe-e2e-release-hardening-2026-09-03.md`](../../../../dev-impl-plan/FE/release/064-fe-e2e-release-hardening-2026-09-03.md)
