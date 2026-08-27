# Developer Plan: FE v2 Shell & API Integration Foundation

## Mục tiêu

Xây dựng nền tảng FE dùng chung trước khi triển khai các màn hình nghiệp vụ
v2. Plan này chuẩn hóa cách Vue 3 giao tiếp với Spring Boot REST, cách biểu
diễn trạng thái/lỗi và authenticated shell, nhưng không tạo business feature
mới.

Visual contract phải tiếp tục đồng nhất với legacy UI hiện tại: Vue 3,
PrimeVue 4, Aura, `AuthenticatedLayout` và các class layout/style hiện có
gồm `page-heading`, `content-surface`, `field-group`, `form-actions` và
`table-shell`.

## Requirement và nguồn đối chiếu

- Application version áp dụng: `v2`.
- `document/application-doc/v2/ApplicationContext.md` — JWT session,
  backend authorization và các nguyên tắc ưu tiên contract.
- `document/application-doc/v2/RequirementBaseline.md` — baseline v2.
- `document/application-doc/v2/FrontendApiGuide.md` — response envelope,
  error envelope, `204`, pagination và boundary FE/API.
- `document/application-doc/v2/frontend-api/00-common-contract.md` — contract
  transport dùng chung.
- `document/application-doc/v2/frontend-api/01-auth-student.md` — auth hiện
  tại, ranh giới v1/v2/v3 và legacy compatibility.
- `document/application-doc/v2/frontend-api/07-enums-and-known-drift.md` —
  enum wire contract, đặc biệt `CalculationStatus`.
- `document/application-doc/v2/modules/00-CommonAndAuthModule.md` và
  `modules/07-AccessQualityAndAcceptanceModule.md` — auth, access denied,
  validation, usability và status calculation.
- `FE/AGENTS.override.md` cùng `FE/agent-rules/00-foundation.md`,
  `01-auth-routing-security.md`, `03-api-data-boundaries.md` và
  `04-quality-documentation.md`.
- Implementation hiện tại: `FE/src/services/userApi.ts`,
  `FE/src/services/studentApi.ts`, `FE/src/services/authSession.ts`,
  `FE/src/router/index.ts`, `FE/src/components/AuthenticatedLayout.vue` và
  `FE/src/styles.css`.

## Hiện trạng và nguyên tắc tương thích

- `userApi.ts` và `studentApi.ts` đang có hai bản `fetch`/unwrap/error logic
  riêng; `ApiError` hiện nằm trong `userApi.ts`.
- Legacy Student UI đang dùng v1 API và phải tiếp tục hoạt động. Không đổi
  business behavior, field mapping, route hoặc visual flow của màn Student chỉ
  để phục vụ nền tảng v2.
- `authSession.ts` đã lưu access token và user summary trong `sessionStorage`.
  Không thêm role vào session bằng cách decode JWT hoặc suy luận từ username.
- API guide hiện dùng tên `RestResponse<T>` cho envelope. Plan chuẩn hóa tên FE
  dùng chung thành `ApiResponse<T>` nhưng vẫn map đúng wire shape hiện tại,
  không thay đổi backend contract.
- API v2 hiện chưa expose role/capability cho FE. Shell chỉ hỗ trợ navigation
  tĩnh và route metadata trung lập; backend vẫn là nguồn authorization cuối.

## Phạm vi

### In-scope

### 1. Shared typed API client

Tạo một lớp fetch dùng chung trong `FE/src/services/` với base URL lấy từ
`VITE_API_BASE_URL`, không thêm Axios hoặc thư viện HTTP khác.

Client cần:

- nối base URL với path mà không hard-code origin trong component;
- tự gắn `Accept: application/json` và Bearer token từ session khi request
  protected;
- hỗ trợ method, query string, JSON body, `204` và raw `Blob`/CSV khi cần;
- unwrap `data` từ success envelope và không gọi `response.json()` với
  `204 No Content`;
- dùng `ApiError` chuẩn hóa cho cả JSON error và response không có JSON body;
- không chứa endpoint hoặc business rule cụ thể.

Status behavior:

| HTTP status | Hành vi client/UI boundary |
|---|---|
| `200` | Parse success envelope và trả `data`. |
| `201` | Parse success envelope và trả `data`, giữ nguyên semantics create. |
| `204` | Trả `undefined`/`void`, không parse JSON. |
| `400` | Tạo `ApiError` có message và danh sách `ValidationError`; map message dạng `field: message` về đúng field, giữ global message nếu không xác định được field. |
| `401` | Xóa toàn bộ auth session trong `sessionStorage`, điều hướng về `/login` qua router integration; vẫn reject bằng `ApiError` để caller không bị treo. Không tạo vòng redirect khi đã ở Login. |
| `403` | Giữ nguyên session, reject `ApiError` với trạng thái forbidden để `PageState`/`FormAlert` hiển thị không đủ quyền. |
| `404` | Reject lỗi resource không tồn tại với fallback message ổn định nếu backend không gửi message. |
| `409` | Reject lỗi conflict/ràng buộc nghiệp vụ, không tự retry hoặc đổi request. |
| `500` | Reject lỗi server với message an toàn, không lộ stack trace; không tự retry khi chưa có contract retry. |

Điều hướng sau `401` phải được nối qua dependency/callback của client hoặc một
auth failure handler ở application boundary để tránh service transport phụ
thuộc cứng vào router. Client không thao tác trực tiếp với component state.

### 2. Shared types

Tạo các type dùng chung, bám wire contract hiện tại:

- `ApiResponse<T>`: `statusCode`, `message`, `error`, `data` tương ứng
  `FormatRestResponse`.
- `ApiError`: runtime error chuẩn hóa có HTTP status, loại lỗi, message an
  toàn, raw messages đã normalize và validation field errors khi có.
- `PageResponse<T>`: `content`, zero-based `page`, `size`, `totalElements`,
  `totalPages`; không giả định page size chung cho mọi endpoint.
- `ValidationError`: field key tùy backend contract, message và khả năng chứa
  nhiều message cho một field; giữ global error riêng khi message không map
  được.
- `LoadingState`: trạng thái dùng chung cho `idle`, `loading`, `success`,
  `empty`, `error` để các view biểu diễn cùng một lifecycle.
- `CalculationStatus`: đúng enum API hiện tại, chỉ gồm `IN_PROGRESS | FINISH`;
  không trộn với `CalculationTaskStatus` (`PENDING | RUNNING | SUCCEEDED |
  FAILED`).

Các type sẽ nằm ở `FE/src/types/` và được export từ một boundary dễ dùng cho
service/component. Không tạo alias business-specific hoặc thêm enum chưa có
trong `07-enums-and-known-drift.md`.

### 3. Shared state components

Tạo các component presentation trong `FE/src/components/`, chỉ nhận props và
phát emits, không gọi API hoặc router business:

- `PageState.vue`: điều phối `LoadingState`, loading, empty, error,
  forbidden và success slot; có retry event nếu parent cung cấp.
- `EmptyState.vue`: icon/heading/message và action tùy chọn, dùng được cho
  table/list nhưng không làm thay đổi data state.
- `StatusTag.vue`: bọc PrimeVue `Tag`, nhận label/severity và hỗ trợ hiển thị
  trạng thái calculation rõ ràng (`IN_PROGRESS`/`FINISH`) theo wire value.
- `FormAlert.vue`: hiển thị error, warning, info hoặc success; hỗ trợ global
  message và danh sách validation message.
- `ServerPagination.vue`: bọc PrimeVue `Paginator`, nhận page/size/total và
  emit page change; tôn trọng page size/options do endpoint truyền vào.
- `ConfirmAction.vue`: bọc cơ chế confirmation PrimeVue hiện có, phát event
  confirm/cancel; không gọi delete/update API.

Các component phải giữ class và spacing tương thích với `styles.css`, tái sử
dụng PrimeVue/Aura thay vì dựng UI framework mới. `StudentTable.vue` legacy
chỉ được dùng lại component mới nếu cần thiết và có test chứng minh không đổi
hành vi; không refactor Student legacy chỉ vì mục đích làm đẹp.

### 4. Mở rộng authenticated shell

Mở rộng `AuthenticatedLayout.vue` theo hướng tương thích ngược:

- giữ nguyên `userName`, `logout` emit, brand, header, sidebar responsive và
  các link Student hiện tại;
- cho phép shell nhận navigation item/config tĩnh hoặc named slot để module
  v2 bổ sung menu mà không copy layout;
- giữ active route, accessible labels và mobile behavior hiện tại;
- hỗ trợ page content qua slot/route outlet; không gắn role visibility vào
  username, JWT hoặc kết quả gọi API;
- không biến sidebar thành ma trận role-aware hoàn chỉnh trong plan này.

### 5. Chuẩn bị route cho module v2

- mở rộng `RouteMeta` với metadata module/shell cần thiết, trong đó
  `requiresAuth` tiếp tục là guard duy nhất có thể áp dụng ngay;
- tạo authenticated v2 shell/outlet và quy ước route namespace/lazy module
  registration để các plan nghiệp vụ có thể thêm child route độc lập;
- không tạo route hoặc view giả cho Academic Year hay module chưa có approved
  screen/API contract;
- không thêm role/capability route guard trước khi backend expose contract;
- giữ nguyên redirect và route của Login/Register/legacy Student.

### 6. Test và Storybook

Unit test API client phải bao phủ:

- unwrap `200` và `201`;
- `204` không parse JSON;
- parse validation message dạng string/list và map field;
- `401` clear session + gọi redirect handler đến `/login`;
- `403` giữ nguyên session và giữ loại lỗi forbidden;
- `404`, `409`, `500` giữ status/message/fallback đúng;
- header Bearer, base URL và raw response path không bị phá vỡ.

Unit test component phải bao phủ các state chính của `PageState`,
`EmptyState`, `StatusTag`, `FormAlert`, `ServerPagination` và `ConfirmAction`,
bao gồm loading/empty/error/forbidden/success, field errors, disabled/loading,
pagination emit và confirm emit.

Storybook phải dùng state/props deterministic, không gọi backend. Tối thiểu
thêm story cho các state materially khác nhau của từng shared component và
giữ nguyên các story Login/Register/Student đang có.

## Out-of-scope

- Không làm Academic Year UI hoặc bất kỳ business screen v2 nào.
- Không tạo endpoint/service business v2 giả hoặc tự thêm field/enum vào API
  contract.
- Không làm sidebar role-aware hoàn chỉnh.
- Không tự suy luận `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`, `STUDENT`, GVCN hoặc
  GVBM từ JWT, username, route hay API success.
- Không thêm Pinia/Vuex, Axios, Tailwind, Bootstrap hoặc UI framework mới.
- Không sửa Student legacy nếu không cần để tái sử dụng component hoặc giữ
  compatibility với shared client.
- Không chuyển official calculation vào FE; FE chỉ hiển thị
  `IN_PROGRESS`/`FINISH` từ backend.
- Không thay đổi backend, database, requirement, CR hoặc wire contract.

## Phương án triển khai

1. Tạo shared API/type boundary và thiết kế auth failure callback; giữ
   `apiConfig.ts` là nguồn base URL public.
2. Thêm test cho transport trước khi nối service, dùng mocked `fetch`, mocked
   sessionStorage và router callback để kiểm chứng toàn bộ status matrix.
3. Tạo shared state components theo visual contract, sau đó thêm unit tests và
   Storybook stories deterministic.
4. Mở rộng `AuthenticatedLayout`, router metadata và authenticated v2 outlet;
   xác nhận route legacy Student không đổi.
5. Chỉ migrate phần transport cần thiết của service hiện có nếu việc đó làm
   giảm duplication mà không đổi public API/behavior của Student legacy. Một
   v2 service mới sẽ dùng `apiClient` qua typed method trong test/integration
   boundary, nhưng không thêm endpoint business giả.
6. Cập nhật `FE/README.md` nếu nội dung còn nói REST integration bị defer;
   không cập nhật application API guide nếu wire contract không thay đổi.
7. Tạo Dev Note sau khi implementation được phê duyệt, ghi rõ files thực tế,
   validation, deviation và risk còn lại.

## Files/khu vực dự kiến thay đổi

### API và types

- `FE/src/services/apiClient.ts` — shared fetch client, response parsing và
  status normalization.
- `FE/src/services/apiClient.spec.ts` — unit test status matrix và auth hooks.
- `FE/src/services/apiConfig.ts` — chỉ chỉnh nếu cần chuẩn hóa config hiện có.
- `FE/src/services/authSession.ts` — chỉ chỉnh để expose auth failure/session
  lifecycle dùng chung, không đổi storage contract.
- `FE/src/types/api.ts` — `ApiResponse`, `ApiError`, `PageResponse`,
  `ValidationError`.
- `FE/src/types/ui.ts` hoặc boundary tương đương — `LoadingState`,
  `CalculationStatus` nếu không phù hợp đặt cùng transport types.
- `FE/src/services/userApi.ts`, `FE/src/services/studentApi.ts` — chỉ adapter
  tối thiểu nếu cần chia sẻ `ApiError`/client; giữ public function signature,
  endpoint và legacy behavior.

### Shell, router và components

- `FE/src/components/PageState.vue`
- `FE/src/components/PageState.spec.ts`
- `FE/src/components/PageState.stories.ts`
- `FE/src/components/EmptyState.vue`, `.spec.ts`, `.stories.ts`
- `FE/src/components/StatusTag.vue`, `.spec.ts`, `.stories.ts`
- `FE/src/components/FormAlert.vue`, `.spec.ts`, `.stories.ts`
- `FE/src/components/ServerPagination.vue`, `.spec.ts`, `.stories.ts`
- `FE/src/components/ConfirmAction.vue`, `.spec.ts`, `.stories.ts`
- `FE/src/components/AuthenticatedLayout.vue` và test/story liên quan nếu
  contract hiện tại có coverage.
- `FE/src/router/index.ts` và route-level v2 shell/outlet file nếu cần.
- `FE/src/styles.css` — chỉ bổ sung token/class cần cho shared states, giữ
  nguyên visual contract hiện tại.
- `FE/src/views/` — chỉ route shell/outlet nếu cần, không tạo business view.
- `FE/README.md` — cập nhật hướng dẫn foundation nếu nội dung hiện tại lỗi
  thời.

Danh sách trên là vùng dự kiến; khi triển khai phải kiểm tra lại từng file và
không tạo file placeholder không có behavior/test rõ ràng.

## API, schema và dữ liệu thay đổi

- Không đổi backend API, schema hoặc database.
- Không đổi success/error envelope trên wire.
- FE chuẩn hóa `RestResponse<T>` hiện tại thành `ApiResponse<T>` ở boundary
  TypeScript; đây là rename/type adaptation nội bộ, không phải API migration.
- Không thêm endpoint v2 vào `FrontendApiGuide.md` chỉ để chứng minh route.
- Auth storage tiếp tục chỉ gồm access token và UI-safe user summary.

## Acceptance criteria và bằng chứng cần thu thập

1. `npm run lint` pass.
2. `npm run test` pass.
3. `npm run test:coverage` pass và có coverage cho API client cùng shared state
   components; không sửa thủ công generated report.
4. `npm run build` pass.
5. `npm run build-storybook` pass; Storybook không gọi backend thật.
6. Có test chứng minh một typed v2-style service call có thể dùng chung
   `apiClient` mà không tự viết raw `fetch`.
7. Có test chứng minh `200`, `201`, `204`, `400`, `401`, `403`, `404`, `409`,
   `500` theo status matrix.
8. `401` clear toàn bộ auth session và điều hướng `/login`; `403` giữ session
   và hiển thị access denied.
9. Validation error map được field khi backend trả message có prefix field;
   message không map được vẫn hiển thị như global error.
10. Shared components biểu diễn nhất quán loading, empty, error, forbidden và
    success; `CalculationStatus` hiển thị đúng `IN_PROGRESS`/`FINISH`.
11. Legacy Student list/form/login/register routes và visual flow không bị phá
    vỡ; các test hiện có vẫn pass.
12. Không có presentation component nào chứa raw `fetch` hoặc endpoint call.

## Rủi ro, assumption và cách giảm thiểu

- `ApiError` hiện được import từ `userApi.ts`: nếu chuyển về boundary chung,
  phải giữ type guard/import compatibility đủ để test và Student legacy không
  đổi hành vi.
- Router redirect từ transport client có thể tạo import cycle: dùng callback
  hoặc auth boundary được cấu hình ở application bootstrap, test độc lập bằng
  mock handler.
- Backend error message có thể không có field prefix hoặc dùng format mới:
  parser phải giữ global message, không đoán field từ nội dung tự do.
- `CalculationStatus` là status của transcript/result, không phải task status;
  registry v2 là nguồn canonical khi viết component/status mapping.
- Một số route v2 chưa có business screen/API contract: chỉ chuẩn bị shell,
  metadata và outlet; mọi module child route phải đi cùng plan nghiệp vụ đã
  được duyệt.
- Student legacy hiện có behavior 401 xử lý ở view: nếu shared client được
  dùng lại, tránh double redirect và xác nhận bằng regression tests.

## Approval status

- `Approved; implementation completed`.
- Người dùng xác nhận phê duyệt plan 051 qua tin nhắn trong agent ngày
  2026-08-27.
