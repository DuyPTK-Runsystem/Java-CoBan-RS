# Dev Note 098 — Library circulation

## Plan và approval

[Plan 098](../../../dev-impl-plan/summary/098-library-loan-return-renewal-lost-2026-10-09.md) được user duyệt ngày 2026-10-09. Phạm vi bao gồm borrow/return/renew/lost, reservation, fine, versioned circulation policy, batch overdue fines và FE workspace. User chọn `ADMIN` + `LIBRARIAN` cho policy editor. User cho phép skip full test và `pmdTest`; `pmdMain` vẫn bắt buộc. Không có target DB writes, deploy, commit hoặc push.

## Thực tế triển khai

- BE bổ sung V32, dịch vụ/controller/DTO cho loan, return, renewal, lost, reservation, fine, policy, in-app notification và Spring Batch; dùng actor từ authentication, audit trong transaction và notification persistence cùng transaction với after-commit SSE refresh.
- FE bổ sung các màn hình quầy lưu thông, lịch sử loan, queue reservation, fine, policy và batch history; batch history thực tế là bounded latest-50 list, không paging. API routes và response shape được spot-check giữa FE services/types và BE controllers.
- Batch job dùng chunk/checkpoint và idempotent từng item. Người vận hành có thể restart FAILED job; hard process death để lại Spring Batch metadata `STARTED` chưa được chứng minh/khôi phục tự động.
- Được user duyệt 45 field-local `PMD.ImmutableField` exceptions cho field JPA snapshot/identity đã nêu tại Plan §12.1. Không có exception PMD khác được duyệt.

## Validation Result — IMPLEMENTED, validation partial (2026-10-09)

Các gate được báo riêng theo bằng chứng mới nhất. Backend tests/PMD/Checkstyle/build và disposable-MySQL acceptance dùng source final sau các service/entity/repository split. Full BE test và `pmdTest` được user cho phép skip; QR/barcode processing hiện được user yêu cầu skip. Target DB và hard process-crash recovery không được xác minh.

| Hạng mục | Trạng thái | Bằng chứng / giới hạn |
|---|---|---|
| Focused BE unit tests | PASS | Fresh 26 tests / 14 suites, 0 failure/error/skip; JaCoCo report mới tại `BE/BaiTap-RS/build/reports/jacoco/test/`. Test fixtures/assertions được cập nhật cho borrow preparation/validation, return preparation/item/orchestration, renewal eligibility, reservation expiration và command eligibility splits. |
| `pmdMain` | PASS | Round 14, 0 findings. Giữ đúng 45 exceptions `PMD.ImmutableField` field-local đã được duyệt; không thêm suppression. |
| Checkstyle | PASS với warnings | `checkstyleMain checkstyleTest` exit 0; Main 1,037 warnings / 951 files, gồm 53 warnings / 23 Plan 098 files; Test 415 / 183 files, gồm 37 / 13 Plan 098 test files. |
| Backend build | PASS | `build -x test -x pmdTest` exit 0. Bao gồm compile, assemble, `pmdMain`, Checkstyle và `check`; compile có 10 warnings deprecation/removal của Spring Batch `JobLauncher`. |
| Disposable MySQL migration/schema | PASS | MySQL Community 8.0.46: clean V1–V32 và upgrade V1–V31→V32. Generated `active_copy_id` và unique index mỗi loại xuất hiện đúng một lần; 7 policy columns mỗi cột xuất hiện đúng một lần. Không dùng target DB. |
| Hibernate bootstrap/repository | PASS | App context `ddl-auto=validate`, repository JPQL/derived paths resolve và bean `BookCopyLockQueries` đăng ký được trên schema disposable mới. |
| Service/race/transaction probe | PASS | Final compiled classes: cùng-copy 10-way race có đúng một success; near-cap race 1 success/1 `MAX_ACTIVE_LOANS`; simultaneous returns cấp FIFO reservation cho hai copy khác nhau; mixed valid/missing borrow rollback loan/copy/audit/notification; policy update giữ nguyên persisted loan/renewal snapshots và kích hoạt tại boundary (−1µs/exact); READY expiry chuyển thành EXPIRED và copy AVAILABLE; notification rollback/retry idempotent; batch restart sau committed chunk 100 + 26 injected notification failures không duplicate và đạt 126 totals. `SkipLimitExceededException` là lỗi được cố ý inject. |
| Migration, bootstrap, service evidence | PASS | `/tmp/plan098-runtime-resume/{final-schema-audit.log,policy-columns-audit.log,bootstrap-final.log,service-final.log}`. |
| Batch hard-crash `STARTED` recovery | NOT VERIFIED | FAILED-job checkpoint restart đã được thử; hard process death khi metadata còn `STARTED` chưa thử. |
| Full BE test, `pmdTest` | SKIPPED (user) | User-authorized; không thay cho focused tests hoặc `pmdMain`. |
| FE lint/build/Storybook | PASS | Source FE không đổi trong resume: lint, build và Storybook exit 0. Storybook có warning lookup PrimeVue package metadata và bundle lớn. Log `/tmp/plan098-fe-{lint,build,storybook}.log`. |
| FE focused tests | PASS, selection lịch sử | 115 tests/3 files trước khi user yêu cầu skip QR/barcode; selection có scanner mocks nên không dùng làm evidence nghiệm thu scanner hiện tại. Log `/tmp/plan098-fe-focused-tests-all.log`. |
| QR/barcode processing/native decode | SKIPPED (user) | User tạm dừng feature testing; không claim native QR/Code128 decode. |
| Target DB | NOT VERIFIED | Exact version và applied Flyway head chưa biết; không đọc credentials và không có target writes. |

Resume validation kết thúc ở round 14; PMD findings ở các vòng 11–13 đã được sửa và round cuối PASS. Không commit/push/deploy.

## Bằng chứng lưu

- Backend reports: `BE/BaiTap-RS/build/reports/{tests/test/index.html,jacoco/test/html/index.html,pmd/main.html,checkstyle/main.html,checkstyle/test.html}` reflect final resumed BE validation snapshot.
- Plan §13 là Validation Result tương ứng với summary rows, gồm evidence PASS, SKIPPED và NOT VERIFIED.
- Focused test command: `GRADLE_USER_HOME=/tmp/plan098-qa-gradle ./gradlew --offline --no-daemon --max-workers=1 test --tests 'com.JavaTraining.BaiTap_RS.library.circulation.*'` from `BE/BaiTap-RS`. Backend lifecycle gate: `./gradlew --offline --no-daemon --max-workers=1 build -x test -x pmdTest`.
- FE log files and disposable MySQL/bootstrap/service logs are listed in Plan §13. These are local evidence artifacts under `/tmp`; they do not establish target DB or deployment behavior.

## Cleanup

- Removed the standalone Plan 097 MySQL runtime probe, its VS Code launch entry, and its three generated test classes after confirming it was only a one-off manual acceptance runner with no application or test references. Historical validation results remain recorded above.
- Removed generated Plan 098 probe `.class` and `.args` files from `/tmp/plan098-runtime-resume` after confirming no Java/MySQL process or open file handle used that directory. Kept the Java probe sources, logs, and MySQL data directory for reproducibility and evidence.
- No Plan 098 production code, tests, migrations, fixtures, or wireframes were removed. No tests were run for cleanup.
