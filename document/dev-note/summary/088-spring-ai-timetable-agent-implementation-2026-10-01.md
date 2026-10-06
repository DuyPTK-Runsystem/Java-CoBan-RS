# Dev Note 088 — Spring AI timetable agent implementation

- Plan: [088](../../dev-impl-plan/summary/088-spring-ai-timetable-agent-2026-10-01.md), người dùng phê duyệt qua tin nhắn ngày 2026-10-01.
- Điều phối: dev backend 6-Luna, tester độc lập 6-Luna; 6.1 Sol xử lý contract, FE và các phần reasoning backend. Công cụ từ chối tạo thread thứ năm, nên dùng lại agent 6.1 Sol.
- Baseline snapshot-only; D08 read tools chưa có lựa chọn mới. Provider chưa được cài đặt; người dùng xác nhận không có database MySQL riêng để reset cho integration test.
- Giữ nguyên file có sẵn `document/application-doc/v4/agent-contract/harness-tools-and-prompt-cache.md`; không deploy/push.
 
## Kết quả hiện tại

| Khu vực | Thực tế                                                                                                                                                                                                                             |
| ------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| FE      | [Dev Note FE](../fe/088-spring-ai-timetable-agent-2026-10-01.md): typed API, input/demand confirmation, preview/diff, approval binding, action receipt và same-key recovery; 20 Storybook states                                    |
| BE      | [Dev Note backend](../be/timetableagent/088-spring-ai-timetable-agent-2026-10-01.md): DTO/gateway/persistence/snapshot/validator/orchestration/approval/action/executor/controller đã triển khai; quality/live acceptance còn thiếu |
| Feature | Mặc định tắt; không promote provider/save khi live gates chưa đạt                                                                                                                                                                   |

## Validation

- Kiểm tra riêng `gradlew.bat checkstyleMain --rerun-tasks` ngày 2026-10-02: task PASS, 975 warning trên 119 file; không phải source sạch Checkstyle. XML mới có 0 finding trong package timetableagent. Không sửa source; goal vẫn paused.

- Kiểm tra riêng theo yêu cầu người dùng ngày 2026-10-02: `gradlew.bat pmdMain` PASS/UP-TO-DATE; chạy tiếp `gradlew.bat pmdMain --rerun-tasks` PASS, 3 tasks thực thi. XML mới 0 findings. PMD cảnh báo bỏ rule `LoosePackageCoupling` vì chưa cấu hình packages/classes; compile có 21 deprecation/removal warnings. Không sửa config/source, không chạy test/build đầy đủ; goal vẫn paused.

| Gate                                  | Kết quả                                                                                                                                                                                                       |
| ------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| FE lint/build/build-storybook         | PASS theo agent FE; 20 stories                                                                                                                                                                                |
| FE full test                          | PASS mới nhất theo tester độc lập: 120 files, 652 tests                                                                                                                                                       |
| FE coverage                           | Retry cuối PASS: 652 tests, statements/lines 86.70%, branches 75.76%, functions 71.83%; lượt trước FAIL do EnrollmentListView history error không lặp ở retry, ghi nhận flaky                                 |
| FE feature coverage                   | Panel 96.25%, Review 100%, API 100%, Workspace 85.32% statements; type-only files không có runtime coverage                                                                                                   |
| Chrome Storybook fixtures             | PASS; không thay API/provider/MySQL runtime proof                                                                                                                                                             |
| BE compileJava/compileTestJava        | PASS sau runtime/config/approval fixes; ngày validation 2026-10-02                                                                                                                                            |
| BE focused tests                      | PASS; controller HTTP/auth/error suite bổ sung 11/11, chạy cô lập, không gọi provider/MySQL                                                                                                                   |
| BE timetable regression               | PASS lượt gộp cuối: 23 suites/103 tests, 0 failures/errors/skips; filters timetableagent, Timetable, TeacherLoadEvaluator, TeacherUnavailabilityService; H2 receipt timing có mocked entry/audit/action repos |
| BE full tests                         | FAIL tại lượt trước refactor cuối: 624 tests, 105 failures; representative missing seeded role ACADEMIC_OFFICE và H2 SQL/context failures                                                                     |
| BE JaCoCo                             | PASS từ lượt gộp cuối: toàn project instructions 41.3%, branches 20.4%, lines 37.4%; đây là coverage của tập test chọn, không phải full-suite coverage                                                        |
| BE Checkstyle                         | Tasks PASS non-fatal: main 975 warnings; test cuối 333 warnings, gồm 5 CustomImportOrder scoped trong TimetableAgentControllerHttpTest                                                                        |
| BE PMD                                | Main PASS; test cuối FAIL 476 findings, 41 scoped Plan 088; 435 findings ngoài scope                                                                                                                          |
| BE build                              | NOT RUN — không mở lượt full build mới theo yêu cầu tạm dừng                                                                                                                                                  |
| Provider live                         | NOT RUN — provider chưa cài đặt                                                                                                                                                                               |
| MySQL migration/atomicity/concurrency | NOT RUN — không có database test cô lập; không ghi/reset `java_coban`                                                                                                                                         |

## Vấn đề và bước tiếp theo

- Người dùng yêu cầu kết thúc lượt đang dở rồi tạm dừng goal ngày 2026-10-02; không mở thêm vòng sửa lỗi hoặc full-suite/build mới. Goal chưa hoàn thành.
- Backup có 47 file (43 file theo cây timetableagent và 4 file ở gốc). Ba tên bị cụt: `ableDetailDTO.java` = `ResTimetableDetailDTO.java`, `epository.java` = `TimetableRevisionRepository.java`, `rvice.java` = `TimetableValidationService.java`. `TimetableService.java` giữ đúng tên. Không tìm được script/lệnh tạo backup để chứng minh cơ chế cắt tên; lỗi xử lý đường dẫn là giả thuyết, không phải kết luận.
- Hai backup `rvice.java` và `TimetableService.java` còn mojibake dù giải mã UTF-8 được; đây là snapshot trước phục hồi, không dùng để restore hàng loạt. Backup giữ nguyên, không nằm trong source build.

- Bàn giao lượt agent mới: sửa 41 PMD findings và 5 CustomImportOrder trong test Plan 088, giữ nguyên các assertion/hành vi, không suppression để che lỗi; chạy compileTestJava, pmdTest, checkstyleTest rồi regression gộp. Sau đó phân loại 105 failures của full-suite cũ và chạy lại full test/build; không suy luận tất cả failures cùng nguyên nhân.
- PMD theo file: SpringAiTimetableModelGatewayTest (2), TimetableAgentControllerHttpTest (16), TimetableAgentProposalRepositoryTest (1), TimetableAgentActionDispatcherTest (1), TimetableAgentActionReservationServiceTest (2), TimetableAgentDraftRevalidatorTest (1), TimetableAgentExecutionServiceTest (3), TimetableAgentInputLimitsTest (3), TimetableAgentOrchestratorTest (5), TimetableAgentWorkspaceCapabilityServiceTest (4), TimetableSnapshotServiceTest (3). Nguồn: XML cuối tại `BE/BaiTap-RS/build/reports/pmd/test.xml`.
- Coverage 37.4% lines/20.4% branches toàn project chỉ phản ánh tập test chọn, không thay full-suite coverage. MySQL migration/atomicity/concurrency và provider native output/tool/token/timeout/retry cần môi trường riêng trước acceptance; hiện NOT RUN. FE từng gặp history error ở lượt coverage trước, lượt retry PASS nhưng còn ghi nhận flaky.
- Import-sort script của dev backend từng đọc sai encoding; đã phục hồi trước khi tiếp tục. 53 Java files đọc UTF-8 strict thành công, scan marker sạch, diff legacy chỉ giữ thay đổi có chủ đích; backup được giữ riêng trong thư mục tạm.
- Kiểm tra JSON strictness, snapshot/source binding, occurrence-aware demand, locked-entry identity, native tool guards, lease/idempotency và receipt version.
- Dev Note này đang cập nhật theo gate; không tuyên bố Plan 088 hoàn thành khi backend hoặc live acceptance còn thiếu.
