# Dev Note 057: Scorebook & Assessment Column UI

## Liên kết và approval

- Developer Plan: [Plan 057](../../../dev-impl-plan/fe/scorebook/057-scorebook-assessment-column-ui-2026-09-01.md).
- Original Plan 057 đã được approve ngày `2026-09-01`.
- Amendment 57.1 đã được approve qua agent message ngày `2026-09-02`.
- Application version: `v2`.
- Implementation status: `IMPLEMENTED; FRONTEND AND VISUAL QA PASS; BACKEND QUALITY GATE BLOCKED`.

## Phạm vi thực tế đã triển khai

### Backend prerequisite

- Thêm `GET /api/v2/scorebooks/by-class-subject/{classSubjectId}`.
- Lookup kiểm tra class-subject tồn tại, áp dụng Office/Teacher scope guard rồi trả scorebook hiện có.
- Trả `404` khi class-subject chưa có scorebook; không dùng duplicate create làm lookup.
- Thêm test cho delegation, success/not-found, Teacher assignment scope, `401` và `403`.
- Cập nhật Frontend API Guide với contract lookup mới.

### Frontend remediation

- Tự lookup scorebook khi chọn class-subject; phân biệt `404` empty state với lỗi transport.
- Dùng Subject catalog và School Class catalog để hiển thị mã/tên lớp, mã/tên môn thay cho technical id.
- Dùng `session.user.roles`: chỉ Office thấy create; Teacher chỉ lookup/manage trong assignment scope; session thiếu roles không suy đoán quyền.
- Ẩn navigation Scorebook khỏi role không thuộc staff.
- Bổ sung server pagination và bulk action theo từng assessment column.
- Bulk dialog prefill trạng thái/value/note/version, hỗ trợ đủ `SCORED | ABSENT | EXEMPTED | CANCELLED` và chỉ gửi row thay đổi.
- Bổ sung validation một chữ số thập phân, `SCORED` bắt buộc value, score `0` hợp lệ, note tối đa 500 ký tự.
- Bổ sung validation assessment column và panel trọng số chỉ dành cho subject `SKILL`.
- Publish và deactivate column dùng confirmation; column mutation reload metadata và grid.
- Xử lý `409` bằng reload authoritative metadata/grid/version, không auto-retry mutation.
- Bỏ qua response cũ khi context/lookup/grid request đã bị thay thế.
- Thêm targeted service/view/component tests và đưa Scorebook files vào coverage include.
- Bổ sung Storybook states cho score dialog, bulk dialog và skill weight panel.
- Visual QA bằng Browser MCP đã phát hiện và sửa việc bảng điểm làm tràn ngang toàn trang trên mobile.
- Bulk dialog khởi tạo dữ liệu ngay khi được mount ở trạng thái mở; bổ sung regression test cho trường hợp này.
- Chỉnh bulk dialog thành bốn cột có kích thước ổn định; control score/note luôn vừa trong cột, không còn overlap trên desktop.
- Điểm nhập ở score dialog và bulk dialog chỉ được format một chữ số thập phân khi `blur`; trong lúc focus, gõ `8` giữ nguyên `8` và vị trí con trỏ không bị nhảy.

## Files thay đổi theo Amendment 57.1

### Backend và API documentation

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/controller/ScorebookController.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/ScorebookService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/ScorebookLifecycleService.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/ScorebookContext.java`
- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/scorebook/service/ScorebookGuard.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/controller/ScorebookAuthorizationIntegrationTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/service/ScorebookLifecycleServiceTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/service/ScorebookGuardTest.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/scorebook/service/ScorebookLookupDelegationTest.java`
- `document/application-doc/v2/frontend-api/05-scorebook-change-audit.md`

### Frontend

- `FE/src/services/scorebookApi.ts`
- `FE/src/services/scorebookApi.spec.ts`
- `FE/src/views/ScorebookWorkspaceView.vue`
- `FE/src/views/ScorebookWorkspaceView.spec.ts`
- `FE/src/views/AuthenticatedV2ShellView.vue`
- `FE/src/components/ScorebookContextPanel.vue`
- `FE/src/components/ScorebookContextPanel.spec.ts`
- `FE/src/components/ScorebookStatusHeader.vue`
- `FE/src/components/AssessmentColumnDialog.vue`
- `FE/src/components/AssessmentColumnDialog.spec.ts`
- `FE/src/components/ScoreGrid.vue`
- `FE/src/components/ScoreGrid.spec.ts`
- `FE/src/components/ScoreEntryDialog.vue`
- `FE/src/components/ScoreEntryDialog.spec.ts`
- `FE/src/components/ScoreEntryDialog.stories.ts`
- `FE/src/components/BulkScoreEntryDialog.vue`
- `FE/src/components/BulkScoreEntryDialog.spec.ts`
- `FE/src/components/BulkScoreEntryDialog.stories.ts`
- `FE/src/components/SkillWeightPanel.vue`
- `FE/src/components/SkillWeightPanel.spec.ts`
- `FE/src/components/SkillWeightPanel.stories.ts`
- `FE/src/components/ScorebookWorkspaceReview.vue`
- `FE/vite.config.ts`

## Validation thực tế

### Backend Validation Result

| Gate | Command/evidence | Result |
|---|---|---|
| Targeted test | `gradlew.bat test --tests <4 Scorebook test classes> checkstyleTest` | **PASS** — 17 tests, 0 failure |
| JaCoCo | `jacocoTestReport` generated from targeted tests | **PASS** — lookup methods in controller/service/lifecycle/context covered; guard lookup 5/6 lines |
| Full test | `gradlew.bat test checkstyleMain checkstyleTest pmdMain pmdTest build` | **PASS** |
| Checkstyle | same full command; `checkstyleMain` and `checkstyleTest` | **PASS** |
| PMD | `pmdMain`, `pmdTest` | **FAIL** — 4 main + 5 test violations outside Plan 057 |
| Build | `gradlew.bat ... build` | **FAIL** — stopped by PMD failures above |

PMD rerun after fixing all violations introduced by the new Scorebook tests no longer reports
any Amendment 57.1 file. Remaining violations are in:

- `SemesterController.java`, `TranscriptQueryService.java`, `StudentService.java`, `TeacherService.java`;
- `AttendanceGuardTest.java`, `CalendarServiceTest.java`.

Backend debug cycles used: `2`.

### Frontend Validation Result

| Command | Result |
|---|---|
| `npm.cmd run lint` | **PASS** |
| `npm.cmd run test` | **PASS** — 45 files, 145 tests |
| `npm.cmd run test:coverage` | **PASS** — all files 85.52% statements; `scorebookApi.ts` 100%; `ScorebookWorkspaceView.vue` 66.84%; Scorebook components appear in report |
| `npm.cmd run build` | **PASS** |
| `npm.cmd run build-storybook` | **PASS** — existing eval/chunk-size warnings |
| Browser visual QA — Storybook | **PASS** — Chrome MCP click-through desktop/mobile cho workspace, score dialog, bulk dialog và skill-weight panel; không còn document-level horizontal overflow ở viewport 390px |
| Browser visual QA — production route | **PASS** — Chrome MCP click-through bằng `ACADEMIC_OFFICE` trên live backend ở desktop/mobile; kiểm tra context, lifecycle, grid, single-score dialog, bulk dialog và assessment-column tab/dialog mà không thực hiện mutation. Patch UI xác nhận `8` khi focus, `8,0` sau blur. |

Frontend test debug cycles used: `2`.

## Deviations và remaining blockers

- Amendment 57.1 mở rộng backend nhỏ để cung cấp lookup contract bắt buộc; không có schema/migration change.
- Full backend PMD/build chưa PASS do violations ngoài phạm vi Plan 057 trong dirty working tree.
- Storybook đã được click-through bằng Chrome MCP. Hai lỗi visual/runtime tìm thấy đã được sửa và xác nhận lại: document-level horizontal overflow trên mobile và bulk dialog rỗng khi mount với `visible=true`.
- Production route đã được live-backend walkthrough bằng authenticated `ACADEMIC_OFFICE`; desktop và viewport 390px không có document-level horizontal overflow.
- AC-057.1-12 đã được chứng minh; Plan 057 chưa được đánh dấu `COMPLETED` chỉ vì backend PMD/build quality gate còn fail ngoài phạm vi Amendment 57.1.

## Next steps

1. Xử lý/approve riêng các PMD violations ngoài Plan 057, rồi chạy lại `pmdMain`, `pmdTest` và `build`.
2. Chỉ chuyển Plan 057 sang `COMPLETED` khi backend quality gate trên PASS.

## PMD remediation follow-up (2026-09-03)

Theo yêu cầu review code change, đã xử lý blocker PMD baseline mà không disable rule hoặc suppress violation:

- Tách lifecycle semester controller khỏi completeness/notification endpoints.
- Tách helper/mapping trách nhiệm khỏi `StudentService`, `TeacherService` và transcript query/status support.
- Tách calendar ensure-scheduled tests và giảm mỗi test về một assertion/verification chính.

Validation sau follow-up:

| Gate | Command | Result |
|---|---|---|
| Test | `gradlew.bat test` | **PASS** |
| Checkstyle | `gradlew.bat checkstyleMain checkstyleTest` | **PASS**; console còn warning style non-blocking ở các file support mới |
| PMD | `gradlew.bat pmdMain pmdTest` | **PASS**; 0 violation |
| Build | `gradlew.bat build` | **PASS** |

Lưu ý: PMD vẫn in thông báo `LoosePackageCoupling` misconfigured do ruleset không khai báo package/class; task PMD vẫn PASS.
