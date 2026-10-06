# Dev Note 094 — Timetable model payload logging

Plan: [094](../../../dev-impl-plan/be/timetable/094-timetable-model-payload-logging-2026-10-06.md), approved qua root agent bởi người dùng trước triển khai.

## Thực tế triển khai
DEBUG `[MODEL REQ]` đầy đủ system/user content và actual schema ở proposal/save gateway. DEBUG `[MODEL RESPONSE}` ghi mọi generation text/tool calls ngay sau chatResponse, trước validation/parser; malformed/truncated/multiple generations được ghi trước khi bị từ chối. Helper `TimetableAgentModelTrace` dùng logger gateway để giữ cấu hình DEBUG hiện có; không bật logging mặc định.

Files: `SpringAiTimetableModelGateway.java`, helper mới `TimetableAgentModelTrace.java`, `TimetableAgentProviderCall.java`, `TimetableAgentModelActionService.java`. Hai virtual-thread boundaries chỉ truyền MDC requestId scoped/cleanup để correlate; không thay request, schema, timeout hay validation. Summary BE/global thêm link note này. Không đụng thay đổi đang có của người dùng.

## Validation — QA độc lập
QA source review: exact markers `[MODEL REQ]` và `[MODEL RESPONSE}`, DEBUG guards, toàn bộ generation text/tool calls được log trước validator/parser; system/user/schema thực tế được log cả proposal/action. Không log provider options, headers hoặc credential configuration. MDC requestId được truyền qua virtual threads và cleanup/restore bằng finally. Bốn file logging không có Checkstyle/PMD finding trong report cuối.

Validation Result (full test bị user yêu cầu skip):
- `test`: **NOT RUN** cho full suite hoàn chỉnh; full run đã **CANCELLED/SKIPPED theo user**, Ctrl-C exit 130. Focused tests **PASS 15/15** (Gateway 9, ProposalGenerator 4, CommandExecutor 2), failures/errors 0 trong XML.
- `checkstyle`: **PASS** `checkstyleMain`; warnings toàn repo vẫn có, bốn file logging 0 warning. Đây là exit-code PASS theo cấu hình hiện tại, không phải repo sạch warning.
- `PMD`: **FAIL**, `pmdMain` còn 8 findings chỉ ở `TimetableAgentDomainValidation.java`, ngoài logging scope; file này đã modified trước task. QA không sửa/suppress baseline. Bốn file logging zero finding.
- `build`: **FAIL**, `build -x test` lỗi do `pmdMain` 8 và `pmdTest` 496; compileJava, compileTestJava, bootJar/jar/assemble chạy được. Full tests excluded theo user.

Commands chạy từ `BE/BaiTap-RS`, dùng prefix:
```bash
GRADLE_USER_HOME=/tmp/gradle-normalize-context-qa ./gradlew --offline --no-daemon --max-workers=1 --init-script /tmp/gradle-normalize-context-qa.init.gradle
```
Arguments final focused/static: `test --tests '*SpringAiTimetableModelGatewayTest' --tests '*TimetableAgentProposalGeneratorTest' --tests '*TimetableAgentCommandExecutorTest' checkstyleMain pmdMain --continue`.
Arguments final build: `build -x test --continue`.

Sandbox Gradle lần đầu không tạo wildcard IP; các run thực tế dùng approved escalation. Offline focused lần đầu thiếu runtime jars; retry online tải dependencies rồi focused PASS. Full run trước khi hủy có context-load failures (ClientOptions.kt:561), chưa chứng minh chính xác nguyên nhân; không lấy chúng làm bằng chứng full PASS hay quy kết logging.

QA đọc test XML `build/test-results/test/TEST-*.xml`, Checkstyle `build/reports/checkstyle/main.xml`, PMD `build/reports/pmd/main.xml`, `test.xml`, và console logs `/tmp/model-trace-094-final-focused.log`, `/tmp/model-trace-094-final-build.log`, `/tmp/model-trace-094-validation.log`. Hai vòng DEV sửa PMD mới trong scope, final scoped findings 0. Không sửa report, tests, quality config hoặc source ngoài task. Không thêm tests mirror implementation logging; gateway focused có malformed/multiple/truncated-output regression cases.

Provider/browser/DB runtime NOT RUN; không có live model-log proof. Chưa commit/push. Backend toàn repo chưa đạt gate đầy đủ.

Để thấy log cần bật `--logging.level.com.JavaTraining.BaiTap_RS.timetableagent.ai.SpringAiTimetableModelGateway=DEBUG` rồi restart BE. Launch hiện tại chỉ bật ProposalGenerator DEBUG, không tự bật gateway mới.

## Deviations và risks
Không có deviation ngoài MDC propagation tối thiểu đã nêu trong plan. Full DEBUG payload chứa dữ liệu lịch và có thể lớn; chỉ bật development có chủ đích. Không log HTTP headers/provider options/keys. Timeout trước khi nhận response sẽ chỉ có request log.

## Next
Dùng build hiện tại và bật gateway DEBUG để capture mới. PMD/build toàn repo còn blocker ngoài logging scope; full tests skipped theo user.
