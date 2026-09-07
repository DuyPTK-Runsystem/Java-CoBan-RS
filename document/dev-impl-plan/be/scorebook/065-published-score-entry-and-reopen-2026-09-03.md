# Developer Plan 065: Published Score Entry and Reopen

## Trạng thái

- Application-document version: `v2`.
- Related CR: `document/application-doc/v2/change-request/CR-SCOREBOOK-001-published-score-entry-and-reopen.md`.
- Approval: user đã chốt rule và yêu cầu triển khai qua agent message ngày `2026-09-03`.
- Implementation status: `Completed`; validation PASS.

## Mục tiêu và phạm vi

- Cho phép `PUBLISHED -> OPEN` cho Office và GVBM có phân công hợp lệ.
- Giữ score-entry khả dụng cho `PUBLISHED` ở FE, còn backend tiếp tục là nguồn
  quyết định mọi validation và authorization.
- Giữ `CLOSED` chỉ xem; không sửa công thức, calculation worker, schema, endpoint
  hoặc quyền assignment.

## Kiến trúc và phương án

- `ScorebookLifecycleService.openScorebook` vẫn dùng `ScorebookGuard`; thay
  validation từ chỉ `DRAFT` sang tập trạng thái `DRAFT | PUBLISHED`, giữ audit
  `SCOREBOOK_OPENED` và endpoint hiện có.
- `ScorebookWorkspaceView` chỉ xem `CLOSED` là read-only cho score grid/dialog.
  Panel cấu hình cột/trọng số tiếp tục khóa tại `PUBLISHED | CLOSED`.
- `ScorebookStatusHeader` đổi action/diễn giải theo trạng thái thực tế.
- Giữ `ScoreChangeRequest` trong dialog vì đây là đường xử lý khi backend từ
  chối sửa trực tiếp (quá hạn hoặc học kỳ khóa).

## Files dự kiến

- `BE/.../ScorebookConfigurationValidator.java`: validation trạng thái mở lại.
- `BE/.../ScorebookLifecycleServiceTest.java`: regression `PUBLISHED -> OPEN`
  và trạng thái không hợp lệ.
- `FE/src/components/ScorebookStatusHeader.vue`: action/copy lifecycle.
- `FE/src/views/ScorebookWorkspaceView.vue` và test: editable PUBLISHED,
  config vẫn read-only.
- V2 CR/API doc, Developer Plan/Dev Note summaries: ghi nhận contract thực tế.

## Unit test plan

- Service: `PUBLISHED -> OPEN` trả `OPEN`, giữ gọi `ScorebookGuard` và audit;
  `CLOSED -> OPEN` trả `409`.
- FE: score grid/dialog nhận `readOnly=false` khi `PUBLISHED`; header phát
  event open khi trạng thái `PUBLISHED` và không mở từ `CLOSED`.
- Regression: publish chỉ từ `OPEN`; mutation score vẫn do backend validator,
  guard assignment và lifecycle hiện có quyết định.

## Validation

- Backend: `test`, `checkstyleMain`, `pmdMain`, `build`, JaCoCo report.
- Frontend: `lint`, `test`, `test:coverage`, `build`, `build-storybook` nếu
  story/component bị ảnh hưởng.

## Rủi ro

- Không được nới `CLOSED` hoặc bypass assignment guard.
- Không được diễn giải role ở FE là quyền authoritative.
- Các thông tin `publishedAt/publishedBy` được giữ làm lịch sử sau khi mở lại.
