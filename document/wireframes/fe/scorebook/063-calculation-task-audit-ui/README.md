# Wireframe Plan 063 — Calculation Task & Audit UI

## Mục đích

Wireframe tĩnh, deterministic để User duyệt phạm vi Plan 063 trước khi code FE.
Wireframe không gọi backend, không mô phỏng production implementation và không khẳng
định các contract còn `TBD`.

## Cách xem

Mở [`index.html`](index.html) bằng trình duyệt. Có thể chuyển giữa ba tab, đổi trạng
thái demo, mở chi tiết task và xem confirmation của retry đơn/hàng loạt.

## Nội dung cần duyệt

- Task list/detail: `FAILED`, `PENDING`, `RUNNING`, `SUCCEEDED`, latest error, attempts, timestamps.
- Retry single/bulk có confirmation, retrying state và thông báo chuyển về `PENDING`.
- Transcript status `IN_PROGRESS`/`FINISH`, version, up-to-date và calculated time.
- Audit table read-only với before/after JSON preview; không có edit/retry action trên audit row.
- Role/capability banner và các state `401`/`403`/`404`/`409` ở mức UX.
- Responsive table overflow, detail modal và keyboard-visible focus.

## Contract boundaries

- Canonical task status: `PENDING | RUNNING | SUCCEEDED | FAILED`.
- Canonical task type: `STUDENT_YEAR_RECALC`.
- Transcript calculation status: `IN_PROGRESS | FINISH`.
- Audit query: `GET /api/v2/scorebooks/audit-logs`, read-only.
- Task query/retry: các endpoint trong frontend API guide; canonical path/DTO/bulk response cần verify trước implementation.
- Backend là authoritative về authorization và assignment scope.
- Wireframe không tính official average và không coi `IN_PROGRESS` là kết quả chính thức.

## TBD

- Endpoint canonical giữa query filter `status=FAILED` và `/failed`.
- Response fields đầy đủ, bulk retry response và idempotency key/version contract.
- Role claim/capability mapping, teacher scope và transcript context.
- Polling interval/stop condition và audit JSON redaction policy.

## Liên kết

- Developer Plan: [`063-calculation-task-audit-ui-2026-09-03.md`](../../../../dev-impl-plan/FE/scorebook/063-calculation-task-audit-ui-2026-09-03.md)
- [Frontend API — scorebook change/audit](../../../../application-doc/v2/frontend-api/05-scorebook-change-audit.md)
- [Frontend API — transcript/retake/calculation](../../../../application-doc/v2/frontend-api/06-transcript-retake-calculation.md)
