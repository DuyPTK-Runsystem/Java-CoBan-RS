# Master Plan v3 — Plans 073 trở đi

## Trạng thái

- Application version: `v3`
- Ngày: `2026-09-09`
- Status: `APPROVED - Plan 073 và Plan 078 đã triển khai; Plan 077 tạm hoãn; backend PMD/build baseline, wireframe review và live validation còn mở`
- CR: [`CR-V3-001`](../../application-doc/v3/change-request/CR-V3-001-academic-operations-and-targeted-communication.md)
- Nguyên tắc: phát triển BE và FE song song theo vertical slice.

## Mục tiêu delivery

Mỗi plan từ 073 trở đi phải có cùng một contract checkpoint, workstream BE, workstream
FE, fixture/test data, Storybook review state và integration/release gate. FE có thể bắt
đầu từ mock/contract fixture sau khi contract slice được chốt; không cần đợi toàn bộ BE
hoàn thành. BE phải cung cấp endpoint thật và contract test trong cùng plan hoặc plan
được liên kết rõ ràng.

## Chuỗi kế hoạch đề xuất

| Plan | Lát dọc | BE + FE song song | Gate chính |
|---:|---|---|---|
| 073 | v3 foundation & cross-cutting contract | vocabulary, role matrix, audit/error/idempotency; FE shell, query state, Storybook contract states | CR/contract checkpoint được duyệt |
| 074 | rule-based class placement | placement session, criteria, preview/confirm, explanation/audit; FE criteria builder, preview, result review | deterministic result, capacity/data warnings |
| 075 | timetable & teacher load | calendar 2 × 4, lịch bận qua duyệt, phòng chức năng/gán môn, conflict/load, draft/publish; ADMIN = ACADEMIC_OFFICE | no conflicting publish, policy source recorded |
| 076 | targeted notifications | individual/class/school audience, read state, delivery/idempotency; FE composer/inbox/audience views | scope isolation, duplicate prevention |
| 077 | search/filter foundation | reusable paged query contracts for lists and scorebooks; FE filter bars, URL state, empty/loading/error states | backend filtering and pagination evidence |
| 078 | one-column score import | validate/preview/commit selected column, expectedVersion/audit; FE upload mapping, row errors, commit summary | wrong-column and partial-commit protection |
| 079 | lesson log | lesson-log lifecycle, timetable linkage, authorization/audit; FE teacher entry, class history, review states | orphan prevention and edit policy |
| 080 | cross-feature integration & release hardening | contract/integration fixtures, observability, migration/backfill readiness; FE browser walkthrough, accessibility/responsive | role matrix, mutation reload/re-query, release evidence |

Plan 075: `DRAFT — AWAITING APPROVAL` ngày `2026-09-11`: đã soạn [plan BE](../be/timetable/075-timetable-teacher-load-2026-09-11.md), [plan FE](../fe/timetable/075-timetable-teacher-load-ui-2026-09-11.md) và [wireframe](../../wireframes/fe/timetable/075-timetable-teacher-load/README.md). D02 đã chốt ADMIN = ACADEMIC_OFFICE và teacher đăng ký lịch bận; D04 chốt 2 buổi × 4 tiết, phòng chức năng/gán môn; D09 chốt cần duyệt trước. Giờ/ngày, xử lý published, cardinality mapping và các quyết định khác còn mở; chưa triển khai code hoặc xác nhận policy active.

Plan 077: `TEMPORARILY POSTPONED` theo quyết định người dùng ngày `2026-09-10`; chưa triển
khai code/test và không được tính là completed trong v3 delivery.

## Quy trình trong từng Plan

1. Contract slice: update v3 requirement/API/data decision and fixture shape.
2. BE lane: implement minimal endpoint/service/schema and contract/integration tests.
3. FE lane in parallel: typed service, view/component, deterministic Storybook and tests
   against the approved fixture; no invented endpoint/DTO.
4. Integration lane: connect FE to BE, verify 401/403/404/409 and persistence after
   reload/re-query where mutation exists.
5. Review gate: user reviews Storybook/wireframe where UI changes materially; only after
   approval may the slice be promoted beyond review/prototype state.
6. Documentation gate: update Dev Note, plan status and validation result independently
   for BE, FE, browser/live and release checks.

## Definition of Done cho v3 plan

- Requirement, contract, role matrix và open decisions được ghi rõ.
- BE test/checkstyle/PMD/build theo cấu hình hiện tại; FE lint/test/coverage/build và
  Storybook theo script thực tế trong `FE/package.json`.
- Unit/integration không được gọi là browser E2E. Browser/live/resettable environment
  chưa chạy phải ghi `NOT RUN` hoặc `BLOCKED`.
- Có audit/privacy/conflict/optimistic-locking phù hợp với capability.
- Không claim `PASS` khi gate chưa chạy; không trộn thay đổi ngoài plan.

## Thứ tự phê duyệt cần người dùng xác nhận

- Phê duyệt CR-V3-001 và baseline v3-draft để chuyển thành approved.
- `TBD-001` đã được cung cấp nội dung nghiệp vụ (19 tiết chuẩn, giảm 4 cho GVCN, giảm
  thêm 3 cho GV nữ nuôi con dưới 12 tháng, cộng dồn); vẫn cần source, effective date và
  policy version trước khi coi là policy active.
- `TBD-005` đã được chốt một phần: `.xlsx`, preview bắt buộc, một `assessmentColumnId`
  cho mỗi lần upload và review/update các ô đã có điểm; template, row mapping, limits và
  semantics còn lại do Plan 078 chốt.
- Xác nhận/cung cấp `TBD-002` đến `TBD-004`.
- Plan 073 và Plan 078 đã được phê duyệt và triển khai; các gate chưa chạy vẫn phải ghi `BLOCKED` hoặc `NOT RUN` theo Dev Note.
