# Dev Note 079 — Backend sổ đầu bài

## Plan và approval

Plan: `document/dev-impl-plan/be/lesson-log/079-lesson-log-lifecycle-and-audit-2026-09-14.md` (v3, D01–D10 và implementation được người dùng phê duyệt).

## Phạm vi đã thực hiện

- Thêm migration `V24__create_lesson_log_and_audit.sql` với policy, entry, weekly review và append-only revision audit; FK lớp dùng `school_class` và audit dùng `RESTRICT`.
- Thêm package `lessonlog` với entity, enum, repository, DTO create/update/response, service cập nhật có optimistic version và controller GET/PUT `/api/v3/lesson-logs/entries/{entryId}`.

## Validation

- `./gradlew compileJava`: **NOT RUN/FAIL TO START** — Gradle wrapper không tạo được lock tại `/home/duyptk/.gradle` vì filesystem read-only.
- Chưa chạy test, Checkstyle, PMD, build hoặc MySQL/Flyway runtime.

## Deviations và rủi ro

Lát dọc hiện tại mới cung cấp GET/PUT và schema nền; các flow create/submit/review/amend/late-record/weekly/policy và guard TKB cần tiếp tục hoàn thiện trước khi gọi Plan 079 hoàn tất. Cần chạy validation trong môi trường có Gradle distribution khả dụng.
