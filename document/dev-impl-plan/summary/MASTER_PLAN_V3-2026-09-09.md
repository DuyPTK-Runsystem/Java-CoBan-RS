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

| Plan | Lát dọc                                       | BE + FE song song                                                                                                            | Gate chính                                              |
| ---: | --------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------- |
|  073 | v3 foundation & cross-cutting contract        | vocabulary, role matrix, audit/error/idempotency; FE shell, query state, Storybook contract states                           | CR/contract checkpoint được duyệt                       |
|  074 | rule-based class placement                    | placement session, criteria, preview/confirm, explanation/audit; FE criteria builder, preview, result review                 | deterministic result, capacity/data warnings            |
|  075 | timetable & teacher load                      | calendar 2 × 4, lịch bận qua duyệt, phòng chức năng/gán môn, conflict/load, draft/publish; ADMIN = ACADEMIC_OFFICE           | no conflicting publish, policy source recorded          |
|  076 | targeted notifications                        | individual/class/school audience, read state, delivery/idempotency; FE composer/inbox/audience views                         | scope isolation, duplicate prevention                   |
|  077 | search/filter foundation                      | reusable paged query contracts for lists and scorebooks; FE filter bars, URL state, empty/loading/error states               | backend filtering and pagination evidence               |
|  078 | one-column score import                       | validate/preview/commit selected column, expectedVersion/audit; FE upload mapping, row errors, commit summary                | wrong-column and partial-commit protection              |
|  079 | lesson log                                    | lesson-log lifecycle, timetable linkage, authorization/audit; FE teacher entry, class history, review states                 | orphan prevention and edit policy                       |
|  080 | cross-feature integration & release hardening | contract/integration fixtures, observability, migration/backfill readiness; FE browser walkthrough, accessibility/responsive | role matrix, mutation reload/re-query, release evidence |

Plan 075: `COMPLETED` ngày `2026-09-14` theo xác nhận của người dùng: implementation BE/FE và các amendment đã được ghi trong Dev Notes. FE quality gates đã có kết quả theo từng note; browser/live, migration và full backend gate vẫn được báo riêng là NOT RUN hoặc baseline blocker khi chưa có bằng chứng PASS.

Plan 074: `IMPLEMENTED SLICE — VALIDATION INCOMPLETE` ngày `2026-09-17`: production route/view BE/FE đã có; focused/automated gates theo Dev Note PASS, nhưng backend full build còn fail ở `pmdTest`, migration/runtime và browser/live chưa chạy.

Plan 077: `TEMPORARILY POSTPONED` theo quyết định người dùng ngày `2026-09-10`; chưa triển
khai code/test và không được tính là completed trong v3 delivery.

Plan 079: `IMPLEMENTED SLICE — VALIDATION INCOMPLETE` ngày `2026-09-17`: BE/FE đã triển khai lifecycle, timetable linkage, roster snapshot, audit, weekly review và policy; BE focused 21/21, compile/checkstyle PASS và FE automated gates PASS. Full BE test/PMD/build còn lỗi hoặc chưa chạy; migration và browser/live NOT RUN.

Plan 076: `IMPLEMENTED SLICE — VALIDATION INCOMPLETE` ngày `2026-09-16`; audience `INDIVIDUAL`/`CLASS`/`SCHOOL`, single-school `DEFAULT_SCHOOL` (FE gửi, BE validate), `IN_APP` only, immediate publish, full-payload + actor idempotency fingerprint và inbox visibility filters đã được ghi nhận. Privacy option A đã chốt và Terra đã triển khai enforcement: recipient không thấy `targetReference`/internal IDs, chỉ `ADMIN`/`ACADEMIC_OFFICE` xem targeting details. Focused BE suites 30+14+5 = 49 PASS; notification PMD 0; backend full test 511 completed/12 failed do `OutOfMemoryError` trong integration tests Auth/Student/JWT; repository còn 9 LessonLog PMD baseline violations; Flyway/H2 chain 4 tests PASS; full JaCoCo, MySQL/runtime migration và browser/live NOT RUN. Notification email v2 vẫn độc lập.

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
