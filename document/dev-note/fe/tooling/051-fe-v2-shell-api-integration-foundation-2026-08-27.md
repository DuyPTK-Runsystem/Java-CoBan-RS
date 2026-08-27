# Dev Note: FE v2 Shell & API Integration Foundation

## Related Developer Plan and approval

- Plan: `document/dev-impl-plan/fe/tooling/051-fe-v2-shell-api-integration-foundation-2026-08-27.md`.
- Approval: người dùng phê duyệt plan 051 qua tin nhắn trong agent ngày
  2026-08-27.

## Actual scope completed

- Tạo shared typed `apiClient` dùng `VITE_API_BASE_URL`, query parameters,
  JSON body, Bearer token từ session, success envelope, `204` và raw `Blob`.
- Chuẩn hóa `ApiError` với HTTP status, error kind, raw/global messages và
  field validation errors; xử lý riêng `400`, `401`, `403`, `404`, `409`,
  `5xx` và network failure.
- Chuyển `userApi.ts` và `studentApi.ts` sang shared client, giữ public
  function signatures, endpoint v1 và legacy Student behavior.
- Tạo `LoadingState`, `CalculationStatus`, `PageResponse` và sáu shared
  presentation components: `PageState`, `EmptyState`, `StatusTag`,
  `FormAlert`, `ServerPagination`, `ConfirmAction`.
- Mở rộng `AuthenticatedLayout` bằng static navigation config/named slot;
  thêm authenticated `/v2` shell outlet và route metadata `module`/`shell`.
- Cập nhật Storybook preview cho confirmation service, FE README và coverage
  include.

## Files changed

### API and types

- `FE/src/services/apiClient.ts` — shared request/response/error transport.
- `FE/src/services/apiClient.spec.ts` — status matrix, auth, headers, query,
  `204`, raw Blob và typed service-call coverage.
- `FE/src/services/apiConfig.ts` — bỏ ghi chú trì hoãn REST integration.
- `FE/src/services/userApi.ts` — adapter sang shared client, re-export
  `ApiError`/`isApiError` để giữ compatibility.
- `FE/src/services/studentApi.ts` — adapter sang shared client cho CRUD,
  pagination và CSV.
- `FE/src/types/api.ts` — `ApiResponse`, `ApiError`, `ValidationError`,
  `PageResponse`.
- `FE/src/types/ui.ts` — `LoadingState`, `CalculationStatus`.
- `FE/src/types/user.ts` — giữ alias `RestResponse` tới `ApiResponse`.

### Shared UI, shell and routing

- `FE/src/components/PageState.vue`
- `FE/src/components/EmptyState.vue`
- `FE/src/components/StatusTag.vue`
- `FE/src/components/FormAlert.vue`
- `FE/src/components/ServerPagination.vue`
- `FE/src/components/ConfirmAction.vue`
- `FE/src/components/AuthenticatedLayout.vue`
- `FE/src/components/{PageState,EmptyState,StatusTag,FormAlert,ServerPagination,ConfirmAction,AuthenticatedLayout}.spec.ts`
- `FE/src/components/{PageState,EmptyState,StatusTag,FormAlert,ServerPagination,ConfirmAction}.stories.ts`
- `FE/src/views/AuthenticatedV2ShellView.vue` — authenticated v2 outlet.
- `FE/src/router/index.ts` — route metadata, `/v2` route và auth failure
  redirect callback.
- `FE/src/main.ts` — giữ bootstrap gọn; API/router integration được cấu hình
  ở router boundary.
- `FE/src/styles.css` — shared warning/success/page-state styles.

### Tooling and documentation

- `FE/.storybook/preview.ts` — đăng ký PrimeVue confirmation service.
- `FE/vite.config.ts` — coverage include cho shared components, client và
  types.
- `FE/README.md` — mô tả shared client và authenticated `/v2` outlet.
- `document/dev-impl-plan/fe/tooling/051-fe-v2-shell-api-integration-foundation-2026-08-27.md` — cập nhật trạng thái approval/completion.
- `document/dev-impl-plan/fe/FE_DEV_PLAN_SUMMARY.md`
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`
- `document/dev-note/fe/tooling/051-fe-v2-shell-api-integration-foundation-2026-08-27.md`
- `document/dev-note/fe/FE_DEV_NOTE_SUMMARY.md`
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`

## Important implementation decisions

- `apiClient` không import router. `401` luôn clear session, sau đó gọi
  callback được cấu hình tại router boundary; callback không redirect khi
  route hiện tại đã là Login.
- `StudentListView` bỏ redirect `401` trùng sau khi shared client đã xử lý,
  nhưng vẫn giữ redirect khi không còn session trước request.
- `5xx` dùng fallback message an toàn và không truyền message backend vào
  `globalMessages`, tránh làm lộ stack trace hoặc chi tiết nội bộ.
- `CalculationStatus` chỉ map `IN_PROGRESS` và `FINISH`; không thêm task
  status hoặc enum business chưa có trong wire contract.
- Không đưa role/capability vào session, JWT parsing hoặc navigation
  visibility. Backend vẫn là nguồn authorization cuối cùng.
- Không migrate shared components vào legacy Student UI vì không cần để hoàn
  thành foundation và việc đó có thể làm thay đổi visual/behavior hiện tại.

## Validation

| Check | Result |
|---|---|
| `npm run lint` | PASS |
| `npm run test` | PASS — 20 test files, 57 tests |
| `npm run test:coverage` | PASS — 94.08% statements, 81.65% branches |
| `npm run build` | PASS — `vue-tsc --noEmit` và Vite production build |
| `npm run build-storybook` | PASS |
| `git diff --check` | PASS |

`build-storybook` vẫn in warning từ tooling/runtime về `eval`, package
metadata của PrimeVue và một số chunk lớn; không có warning nào làm command
thất bại.

## Deviations from Developer Plan

- Không có deviation về phạm vi. Callback `401` được đặt trực tiếp tại
  `router/index.ts` thay vì `main.ts` để bảo đảm service/view test và runtime
  dùng cùng một application boundary; đây là cách triển khai cụ thể của
  dependency callback trong plan.
- Không cập nhật `FrontendApiGuide.md` vì wire contract backend không thay
  đổi.
- Không tạo business v2 service/view hoặc Academic Year route; `/v2` chỉ là
  authenticated outlet đúng out-of-scope.

## Known blockers, skipped checks and remaining risks

- `/v2` chưa hiển thị business screen cho tới khi có plan nghiệp vụ và API
  contract được approve; route hiện cố ý không có child view.
- Role-aware navigation/capability guard vẫn chưa thể triển khai vì auth
  response/JWT hiện không expose role contract.
- Storybook warning về chunk size và runtime `eval` thuộc dependency/tooling
  hiện tại, chưa xử lý trong scope plan 051.
- Không chạy backend validation vì plan chỉ thay đổi FE, types, FE tooling và
  documentation.

## Next steps

- Các plan nghiệp vụ v2 có thể đăng ký lazy child route dưới namespace `/v2`
  và dùng `apiClient`, `PageState`, `FormAlert`, `StatusTag` cùng
  `ServerPagination`.
- Khi backend expose role/capability contract đã approve, bổ sung discovery
  và visibility/guard theo contract đó trong một plan riêng.
