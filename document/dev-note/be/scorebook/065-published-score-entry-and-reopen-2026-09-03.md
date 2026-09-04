# Dev Note 065: Published Score Entry and Reopen

## Developer Plan và approval

- Related plan: `document/dev-impl-plan/be/scorebook/065-published-score-entry-and-reopen-2026-09-03.md`.
- Approval: user chốt rule và yêu cầu triển khai qua agent message ngày `2026-09-03`.
- Application-document version: `v2`.

## Scope hoàn thành

- Endpoint mở sổ hiện có chấp nhận `PUBLISHED -> OPEN`; `DRAFT -> OPEN` vẫn giữ,
  còn `CLOSED` bị từ chối.
- `ScorebookGuard.assertCanManage` vẫn bảo vệ thao tác, nên Office/Admin và GVBM
  có assignment hợp lệ mới thực hiện được.
- FE cho nhập/sửa điểm khi `PUBLISHED`; backend tiếp tục quyết định assignment,
  time limit, trạng thái học kỳ và validation score-entry.
- Cấu hình cột/trọng số vẫn read-only khi `PUBLISHED`; `CLOSED` vẫn chỉ xem.
- Request sửa điểm vẫn là flow khi backend không cho sửa trực tiếp.
- Nút **Yêu cầu sửa điểm** từ popup nhập điểm mở popup tạo request và chuyển
  sẵn trạng thái/điểm/ghi chú đang nhập thành đề xuất/lý do; chỉ `Gửi yêu cầu`
  ở popup thứ hai mới gọi API tạo request.
- Bổ sung import `primevue/dialog` ở `ScorebookWorkspaceView`; trước đó popup
  thứ hai không render trên dev server vì component `Dialog` chưa được resolve.
- Chờ một render tick sau khi đóng popup nhập điểm trước khi mở popup request,
  tránh hai modal PrimeVue cùng hiển thị.

## Files thay đổi

- Backend: `ScorebookConfigurationValidator`, `ScorebookLifecycleService`,
  `ScorebookLifecycleServiceTest`.
- Frontend: `ScoreEntryDialog`, `ScoreChangeRequestForm`,
  `ScorebookWorkspaceView`, `ScorebookStatusHeader` và các test tương ứng.
- Contract/docs: `CR-SCOREBOOK-001`, Frontend API scorebook guide, Plan 065 và
  các plan/dev-note summary.

## Quyết định

- Không thêm endpoint, DTO, enum hoặc migration: endpoint mở sổ hiện có chỉ mở
  rộng transition nguồn.
- Không xóa `publishedAt`/`publishedBy` khi mở lại để giữ lịch sử công bố.
- FE không suy diễn quyền GVBM; backend vẫn là authority.

## Validation Result

### Backend

- `test`: `PASS` — `GRADLE_USER_HOME=/tmp/java_coban_gradle ./gradlew test --no-daemon --max-workers=1`.
- `checkstyle`: `PASS` — `./gradlew checkstyleMain`; 24 warning baseline ngoài scope, không failure.
- `PMD`: `PASS` — `./gradlew pmdMain`; `pmdTest` PASS trong build.
- `build`: `PASS` — `./gradlew build --no-daemon --max-workers=1`.
- JaCoCo: `PASS` — report tạo bởi `jacocoTestReport`; `ScorebookLifecycleService.openScorebook` có 40/40 instructions và 10/10 lines covered.

### Frontend

- `npm run test -- --run src/components/ScoreEntryDialog.spec.ts src/components/ScoreChangeRequestForm.spec.ts src/views/ScorebookWorkspaceView.spec.ts`: `PASS` — 3 files, 21 tests.
- `npm run lint`: `PASS`.
- `npm run test`: `PASS` — 50 files, 168 tests.
- `npm run test:coverage`: `PASS` — statements 83.86%.
- `npm run build`: `PASS`.
- `npm run build-storybook`: `PASS`; chỉ có warning dependency/chunk-size sẵn có.
- Browser/live API QA: `NOT RUN` — không khởi động backend/browser session cho walkthrough.

## Deviations và rủi ro

- Một lần chạy Gradle mặc định dừng với `EOFException`; chạy lại tuần tự
  `--no-daemon --max-workers=1` PASS. Không có test-result XML failure.
- Không kiểm tra live quyền GVBM hoặc MySQL; validation dùng full suite test.
