# CR-V5-001 — Tích hợp Library Management vào Java-CoBan-RS

## Trạng thái

- Ngày: 2026-10-07.
- Status: **DRAFT / IMPLEMENTATION APPROVAL PENDING**.
- Loại: feature baseline v5, không phải standalone application.
- Source: training spec “Thẻ Mượn Số”, đã adapt theo platform hiện hữu.

## Vấn đề

Source requirement giả định một ứng dụng thư viện độc lập với:

- PostgreSQL;
- `MEMBER`/`LIBRARIAN` riêng;
- login + access/refresh token riêng;
- package `com.training.library`;
- Anthropic SDK/provider cố định;
- Docker compose frontend/backend/database riêng.

Áp trực tiếp các assumption này vào Java-CoBan-RS sẽ duplicate identity/security, tạo data ownership mâu thuẫn và làm lệch stack hiện tại.

## Quyết định v5

1. Library là bounded context trong monolith hiện hữu.
2. MySQL/Flyway stream hiện tại là persistence baseline.
3. `app_user` là identity; Library thêm `library_patron`.
4. Thêm role `LIBRARIAN`; không thêm role `MEMBER`.
5. Any user except ADMIN/LIBRARIAN may be a borrower if the patron is eligible. Plan 097 approval clarifies exactly one role per user; reuse role enums/shared constants instead of hardcoded role strings.
6. Nợ thư viện suspend borrowing, không lock account toàn hệ thống.
7. Reuse existing Spring Security/JWT, Batch, notification/mail, audit, Spring AI và FE shell.
8. API mới mặc định `/api/v2`; namespace `/api/v2/library` chỉ khi cần.
9. AI provider/model configurable qua Spring AI.
10. PostgreSQL-specific schema/concurrency phải được chuyển sang MySQL-safe design.
11. Card issuance accepts an expiry date. The FE may compute it from a duration in months or accept a directly entered date; both modes send the expiry date to BE.
12. Plan 098 owns Loan, Return, Renewal and Lost, including circulation transactions, copy locking, due dates, returns, renewals and lost-book handling.

## Compatibility

CR không được:

- đổi behavior của `/api/v1/students`, `/api/v2/students`, `/api/v3/students`;
- đổi meaning của role hiện hữu ngoài Library;
- làm v3/v4 timetable/placement/score/notification contract bị phụ thuộc Library;
- vô hiệu hóa user account vì fine;
- thêm provider AI direct dependency nếu Spring AI hiện hữu đáp ứng requirement.

## Exit criteria trước implementation

- RequirementBaseline v5 được duyệt.
- `TBD-V5-LIB-001..005` được chốt hoặc Developer Plan ghi rõ deferral.
- Migration head được scan lại.
- API path collision scan hoàn tất.
- Role migration và authorization matrix được phê duyệt.
- MySQL concurrency strategy có test plan.
