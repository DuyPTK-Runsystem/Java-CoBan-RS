# Dev Note 046: Transcript Query API

## Plan và approval

- Related plan: `document/dev-impl-plan/be/scorebook/046-transcript-query-api-2026-08-26.md`.
- Approval: user đã phê duyệt qua agent ngày `2026-08-26`.

## Scope đã thực hiện

- Thêm bốn endpoint GET transcript theo học kỳ/năm, gồm đường dẫn `/me` cho `STUDENT` và đường dẫn theo `studentId` cho `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`.
- Thêm `TranscriptAccessGuard`: giáo vụ/admin xem toàn trường; GVCN hoặc GVBM phải có assignment giao với kỳ học và lớp/môn của kết quả transcript. Khi chưa có kết quả môn, guard fallback về enrollment hiện tại.
- Bulk-load kết quả môn, subject, scorebook, assessment column, student score, retake và transfer history để map response; endpoint không gọi task/state/worker service.
- Thêm test controller delegation và unit test guard cho academic office, GVBM đúng scope, ngoài scope bị `403`.

## Files thay đổi

- API/DTO/service: `scorebook/controller/TranscriptQueryController.java`, `scorebook/service/TranscriptQueryService.java`, `scorebook/service/TranscriptAccessGuard.java`, DTO response transcript.
- Repository query: `StudentTermTranscriptRepository`, `ScorebookRepository`, `AssessmentColumnRepository`, `ClassSubjectRepository`, `SubjectTeachingAssignmentRepository`.
- Tests: `TranscriptQueryControllerTest`, `TranscriptAccessGuardTest`.

## Validation Result

- `test`: `PASS` cho các test chính của thay đổi: `TranscriptQueryControllerTest` và `TranscriptAccessGuardTest`, chạy từ clean state bằng `./gradlew clean test --no-daemon --console=plain --tests '*TranscriptQueryControllerTest' --tests '*TranscriptAccessGuardTest'`.
- `checkstyle`: `PASS` qua `./gradlew checkstyleMain pmdMain`.
- `PMD`: `PASS` qua `./gradlew checkstyleMain pmdMain` sau khi tách response support, term mapper, assessment-column mapper và current-student resolver.
- `build`: `PASS` cho build artifact qua `./gradlew build -x test -x pmdTest -x checkstyleTest --no-daemon --console=plain`; full `build` không có kết quả cuối vì `pmdTest` vượt giới hạn thời gian tool.
- JaCoCo: `PASS`; report được tạo bởi task `jacocoTestReport` do task `test` finalize.
- Debug rounds: 4 (fixture Mockito, format/empty bulk-load, tách mapper/support theo PMD, clean output test bị corrupt sau run bị ngắt).

## Sai lệch và giới hạn

- PMD yêu cầu tách trách nhiệm được xử lý bằng các component read-only mới; contract API không đổi.
- Chưa có integration/security test JWT hoặc read-only persistence test. Full `./gradlew test` và full `./gradlew build` không hoàn thành trong giới hạn thời gian tool; đây không phải test failure đã quan sát, nhưng cũng không được coi là bằng chứng full suite PASS.
