# Dev Note 079 — Implementation sổ đầu bài

- Ngày: **2026-09-15**; application-document version: **v3**.
- [Plan BE](../../dev-impl-plan/be/lesson-log/079-lesson-log-lifecycle-and-audit-2026-09-14.md), [Plan FE](../../dev-impl-plan/fe/lesson-log/079-lesson-log-ui-2026-09-14.md), [wireframe](../../wireframes/fe/lesson-log/079-lesson-log/README.md).
- D01–D10 và implementation BE/FE đã được người dùng approve ngày 2026-09-14. Trạng thái: **IMPLEMENTED SLICE; full backend quality gate chưa xanh**.

## Phạm vi đã triển khai

- BE có migration `V24__create_lesson_log_and_audit.sql`, package lesson-log đầy đủ entity/DTO/repository/controller/service, occurrence `UNLOGGED`, resolve lịch sử roster/TKB, lifecycle entry, deadline/policy, weekly review/snapshot, append-only audit và optimistic locking.
- BE bổ sung guard liên kết timetable source/publish/calendar và khóa timetable head dùng chung; mở rộng repository/service hỗ trợ historical roster, scope lớp/homeroom, semester và error handling.
- FE có typed API/fixture và các view/component lesson-log cho lịch cá nhân, form nháp/nộp, class-week matrix 2 × 4, review/amend/late-record/audit, ký tuần `STALE`/snapshot và policy settings.
- FE giữ contract `/api/v3/lesson-logs`, capabilities/error recovery 409/422 và pagination zero-based; không tạo actual teacher tùy ý, tiết tự do hoặc đồng bộ attendance/scorebook.

## Validation Result

| Nhóm | Kết quả | Giới hạn |
|---|---|---|
| FE lint, test, coverage, production build, Storybook | **PASS** — test 103 files, 541/541; coverage statements/lines 85.21%, branches 73.29%, functions 69.23% | Coverage là toàn FE theo include hiện tại; lesson-log chưa có coverage riêng trong include. |
| BE compile | **PASS** — `compileJava` | Chưa chứng minh migration/runtime. |
| BE focused tests | **PASS — 21/21** — `LessonLogServiceTest` 12, `TimetablePublishServiceTest` 5, `TimetableCalendarServiceTest` 2, `StudentYearEnrollmentRosterRepositoryTest` 2 | Không thay thế full suite. |
| BE Checkstyle main | **PASS**; 987 warning severity `warning` | Warning không làm task fail. |
| BE full test | **FAIL** | Lần chạy cuối 462 tests; 8 lỗi `OutOfMemoryError` trong Spring integration context. |
| BE PMD main | **FAIL** | 64 violation baseline/repository-wide sau remediation. |
| BE full build | **NOT RUN** trong phase cuối | Lần chạy trước đó **FAIL** do full test/OOM và PMD. |
| Browser/live API, MySQL/Flyway | **NOT RUN** | Chưa có runtime/session/database live để xác nhận. |

## Đối chiếu kế hoạch và rủi ro

- Migration thực tế là V24 vì V23 đã được dùng cho teacher-load rules; plan ban đầu ghi V23 provisional.
- Các gate focused và FE automated đã chạy, nhưng không dùng chúng để tuyên bố Plan 079 hoàn tất toàn hệ thống.
- Còn cần xử lý/phân loại OOM trong Spring integration context và PMD baseline, rồi chạy full backend gate; sau đó kiểm tra browser/live role, deadline, conflict, weekly sign/re-sign và Flyway trên MySQL.

## Dev Notes chi tiết

- [Backend 079](../be/lesson-log/079-lesson-log-backend-2026-09-14.md)
- [Frontend 079](../fe/lesson-log/079-lesson-log-frontend-2026-09-14.md)
