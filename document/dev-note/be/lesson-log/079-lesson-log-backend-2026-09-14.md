# Dev Note 079 — Backend sổ đầu bài

## Plan và approval

- Plan: [`document/dev-impl-plan/be/lesson-log/079-lesson-log-lifecycle-and-audit-2026-09-14.md`](../../../dev-impl-plan/be/lesson-log/079-lesson-log-lifecycle-and-audit-2026-09-14.md).
- Plan FE đi cùng: [`document/dev-impl-plan/fe/lesson-log/079-lesson-log-ui-2026-09-14.md`](../../../dev-impl-plan/fe/lesson-log/079-lesson-log-ui-2026-09-14.md).
- D01–D10 và implementation BE/FE đã được người dùng approve ngày 2026-09-14. Trạng thái code: **IMPLEMENTED SLICE; BE FULL QUALITY GATE NOT GREEN**.

## Phạm vi thực tế đã triển khai

- Migration [`V24__create_lesson_log_and_audit.sql`](../../../../BE/BaiTap-RS/src/main/resources/db/migration/V24__create_lesson_log_and_audit.sql) tạo policy, lesson entry, weekly review và append-only revision audit; FK lớp dùng `school_class`, audit dùng `RESTRICT`.
- Package `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/lessonlog/` triển khai entity/enum, request-response DTO, repository, controller và service cho lịch cá nhân, lookup lớp, sổ tuần, tạo/cập nhật nháp, submit, review, amend, late-record, policy, audit phân trang và ký tuần.
- Occurrence không có bản ghi được trả về với trạng thái `UNLOGGED`; occurrence được resolve theo ngày học và revision TKB đã publish, gồm cả nguồn lịch sử đã `ARCHIVED`.
- Sĩ số enrollment tại thời điểm tiết học được snapshot; response tuần có lịch ngày, summary và weekly review. Lifecycle, deadline/timezone, closed semester, capability và optimistic version được kiểm tra ở BE.
- Audit lưu actor/reason/before/after trong cùng transaction; thay đổi entry sau khi ký tuần làm tuần `STALE`, còn snapshot cũ để đọc.
- Timetable calendar/publish và timetable head/revision/audit được bổ sung guard/khóa chung để bảo vệ source occurrence và tránh race giữa publish với lesson-log mutation. Repository enrollment, homeroom assignment, semester và global error handler được mở rộng để phục vụ scope/history/validation này.

## Files chính đã thay đổi

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/lessonlog/` (controller, service, entity, repository, request/response DTO).
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/timetable/{service/TimetableCalendarService.java,service/TimetablePublishService.java,repository/TimetableHeadRepository.java,repository/TimetableRevisionRepository.java,repository/TimetableAuditRepository.java}`.
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/{academic/repository/SemesterRepository.java,assignment/repository/HomeroomAssignmentRepository.java,assignment/service/HomeroomAssignmentService.java,enrollment/repository/StudentYearEnrollmentRepository.java,common/error/GlobalExceptionHandler.java}`.
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/lessonlog/service/LessonLogServiceTest.java` và regression tests cho timetable/supporting repository/error paths.

## Validation Result

| Gate | Lệnh/phạm vi | Kết quả | Giới hạn |
|---|---|---|---|
| Compile production | `./gradlew compileJava` | **PASS** | Chỉ xác nhận biên dịch main, không xác nhận runtime/migration. |
| Focused backend tests | `LessonLogServiceTest` (12), `TimetablePublishServiceTest` (5), `TimetableCalendarServiceTest` (2), `StudentYearEnrollmentRosterRepositoryTest` (2) | **PASS — 21/21** | Các nhóm focused mới nhất; không thay thế full test suite. |
| Checkstyle main | `./gradlew checkstyleMain` | **PASS** | Task hoàn tất; có 987 warning severity `warning`, không làm task fail. |
| Full backend test | `./gradlew test` | **FAIL** | Lần chạy cuối: 462 tests; 8 lỗi do `OutOfMemoryError` trong Spring integration context. |
| PMD main | `./gradlew pmdMain` | **FAIL** | 64 violation baseline/repository-wide sau remediation. |
| Full build | `./gradlew build` | **NOT RUN** | Không chạy lại trong phase cuối; lần chạy trước đó bị fail do full test/OOM và PMD. |
| MySQL/Flyway runtime | Runtime database | **NOT RUN** | Chưa có bằng chứng migrate trên MySQL thật. |

## Deviations, blockers và next steps

- Migration thực tế là `V24`, không phải số provisional `V23` trong plan; số được điều chỉnh theo thứ tự migration hiện có.
- Contract response schedule được tách thành `LessonLogScheduleItemResponse` để biểu diễn occurrence chưa ghi và capability; FE adapter tương ứng được cập nhật ở note FE.
- Full backend quality gate chưa đạt vì lỗi OOM trong Spring integration context và PMD baseline/repository-wide nêu trên; không gắn các lỗi đó thành lỗi riêng của lesson-log khi chưa có bằng chứng.
- Chưa chạy MySQL/Flyway, browser/live API hoặc full role/expiry/concurrency walkthrough. Cần xử lý hoặc phân loại OOM/H2 và PMD baseline, sau đó chạy lại full backend gate và runtime verification.
