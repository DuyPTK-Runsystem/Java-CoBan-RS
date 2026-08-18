# Developer Plan: User/Auth API và Route Guard

## 1. Mục tiêu

- Kết nối các màn hình Login/Register hiện có với User/Auth API đã có của backend.
- Thiết lập và quản lý authentication state phía frontend bằng `sessionStorage`, chỉ
  lưu access token và user summary an toàn cho UI.
- Bảo vệ các route Student bằng Vue Router guard và điều hướng người dùng đã đăng
  nhập khỏi Login/Register.
- Thay thế trạng thái demo username/logout hiện có bằng dữ liệu auth thực tế, không
  triển khai Student API trong plan này.

## 2. Requirement và nguồn tham chiếu

- `document/application-doc/ApplicationContext.md`:
  - Sau login lưu access token và user summary trong `sessionStorage`.
  - Logout hoặc API trả `401 Unauthorized` phải xóa auth state và về Login.
  - `403 Forbidden` giữ auth state và hiển thị access-denied message.
- `document/application-doc/modules/UserModule.md`:
  - Login/Register validation phía client vẫn tồn tại; backend là authoritative.
  - Login thành công điều hướng đến Student List.
  - Logout gọi endpoint stateless trả `204 No Content`, sau đó frontend xóa state.
- `FE/AGENTS.override.md`:
  - Dùng Vue 3, Vue Router, typed service trong `src/services/`; không thêm Pinia,
    Vuex, Axios hoặc dependency lớn khác.
- Backend contract đã kiểm tra từ implementation hiện tại:
  - `POST /api/v1/auth/register`: request `username`, `password`, `confirmPassword`;
    thành công `201 Created`.
  - `POST /api/v1/auth/login`: request `username`, `password`; thành công `200 OK`.
  - `GET /api/v1/auth/account`: Bearer JWT; thành công `200 OK`.
  - `POST /api/v1/auth/logout`: Bearer JWT; thành công `204 No Content`.
  - Các response có body thành công dùng wrapper
    `{ statusCode, message, data }`; login trả `data.access_token` và `data.user`.

## 3. Phạm vi

### In-scope

- Typed User/Auth API service trên native `fetch`, dùng `VITE_API_BASE_URL` hiện có.
- Các type request/response cho register, login, current account, REST wrapper và
  lỗi HTTP cần hiển thị ở UI.
- Một auth session utility tập trung cho keys, parse an toàn, read/write/clear
  `sessionStorage`; tuyệt đối không lưu raw password hoặc password hash.
- Tích hợp LoginView, RegisterView, `AuthenticatedLayout`, StudentListView và
  StudentFormView với lifecycle register/login/logout thực tế.
- Vue Router `meta` và global navigation guard cho route guest/protected, gồm
  redirect URL để quay về protected route ban đầu sau login.
- Xử lý lỗi API có chủ đích: lỗi validation/business hiển thị tại form; `401` xóa
  session và về Login; `403` không xóa session và hiển thị thông báo phù hợp tại
  điểm gọi API.
- Unit/component tests deterministic cho auth service/session, view workflow hoặc
  route guard theo boundary nhỏ nhất phù hợp implementation.

### Out-of-scope

- Không sửa backend API, Spring Security, JWT format, schema/database, migration
  hoặc Postman collection.
- Không thêm refresh token, token renewal, token revocation list, persistent login,
  Role/Permission hoặc authorization theo role.
- Không thêm Pinia/Vuex, Axios, dependency HTTP/state-management mới hoặc E2E test.
- Không triển khai Student API, thay thế demo data của Student screens, hoặc thay
  đổi business rule Student ngoài việc truyền username/logout cho layout.
- Không coi route guard là security enforcement; backend Bearer JWT vẫn quyết định
  quyền truy cập API.

## 4. Kiến trúc hiện tại và gap

```text
LoginForm/RegisterForm
  -> LoginView/RegisterView (hiện chỉ hiện placeholder message)
  -> Vue Router
  -> StudentListView/StudentFormView (demo state, logout chỉ router.push)
```

- `FE/src/services/apiConfig.ts` đã có `VITE_API_BASE_URL`, nhưng chưa có HTTP
  service.
- `FE/src/types/user.ts` dùng camel-case `userName`; backend request dùng
  `username`. Adapter tại API boundary phải map rõ hai naming convention này thay
  vì làm rò backend shape vào form component.
- Router hiện không có `meta`, guard hay fallback route; mọi Student URL đều mở
  được từ browser dù không có session.
- `AuthenticatedLayout` đang default `demo.user`; cả Student views chưa đọc auth
  session và logout chưa gọi API.
- Login response đã có user summary, nên guard chỉ kiểm tra session có token hợp lệ
  theo định dạng local; plan này không gọi `/account` ở mọi navigation để tránh
  network guard chậm/chập chờn. `account()` vẫn được typed/expose để dùng khi một
  màn hình cần đồng bộ user summary sau này.

## 5. Phương án triển khai

### 5.1 API và error boundary

- Tạo `userApi.ts` dùng một helper `fetch` nội bộ để:
  - ghép `apiBaseUrl` với path API;
  - gửi JSON headers và `Authorization: Bearer <token>` khi endpoint yêu cầu;
  - parse `RestResponse<T>` thành `data` cho response thành công;
  - chấp nhận response `204` không body cho logout;
  - chuẩn hóa non-2xx thành typed error chứa HTTP status, error/message và field
    errors khi backend cung cấp.
- Service map `LoginValues`/`RegisterValues` (`userName`) sang payload backend
  (`username`) tại một nơi duy nhất.
- Service expose `register`, `login`, `getCurrentAccount` và `logout`; không có
  component nào tự gọi `fetch` hay tự ghép API URL.

### 5.2 Authentication session và lifecycle

- Tạo auth session utility với key được export tập trung, ví dụ access token và
  UI-safe current user. User summary tối thiểu gồm `id` và `username`; các audit
  fields chỉ giữ nếu response UI cần, không suy diễn password.
- Read session phải chịu được JSON hỏng/missing value: trả unauthenticated state và
  dọn state không hợp lệ, không làm app crash.
- LoginView: khóa trạng thái submit/loading, gọi login, lưu session sau khi nhận
  response hợp lệ, rồi điều hướng `redirect` query nội bộ nếu hợp lệ hoặc
  `/students`; failure giữ user ở form và hiện server message.
- RegisterView: gọi register sau validation form; thành công hiện thông báo ngắn và
  điều hướng Login; lỗi duplicate/validation hiển thị ở form, không lưu session.
- Logout từ cả Student views: gọi endpoint với token hiện tại nếu có, luôn clear
  local session trong `finally` rồi điều hướng Login để thỏa stateless logout UX.
- Các consumer API của plan này catch typed `401` để clear session và về Login;
  `403` giữ token/user, không redirect như unauthenticated. Khi Student API được
  triển khai trong plan riêng, nó phải tái dùng helper/error policy này.

### 5.3 Route guard

- Gắn `meta.requiresAuth: true` cho `/students`, `/students/new` và
  `/students/:studentId/edit`; gắn `meta.guestOnly: true` cho `/login` và
  `/register`.
- Global `beforeEach` đọc auth session đồng bộ:
  - guest truy cập protected route: redirect `/login` với query `redirect` chứa
    `to.fullPath`;
  - authenticated user truy cập guest-only route: redirect `/students`;
  - các trường hợp còn lại: cho phép navigation.
- Validate `redirect` trước khi `router.push` sau login: chỉ chấp nhận path nội bộ
  bắt đầu bằng `/`, không chấp nhận protocol/host để tránh open redirect.
- Thêm route catch-all điều hướng về Login hoặc Student List tùy auth state bằng
  guard hiện có, giữ cấu hình navigation rõ ràng.

## 6. Phạm vi mã nguồn dự kiến

| Path | Thao tác | Mục đích |
|---|---|---|
| `FE/src/types/user.ts` | Sửa | Giữ form values; thêm API/user-summary/response/error types rõ ràng nếu không đặt cạnh service. |
| `FE/src/services/userApi.ts` | Tạo | Typed register/login/account/logout qua native `fetch`, mapping naming và error normalization. |
| `FE/src/services/authSession.ts` | Tạo | Đọc/ghi/xóa `sessionStorage`, parse safety và access token/user summary. |
| `FE/src/services/apiConfig.ts` | Có thể sửa nhỏ | Reuse base URL; chỉ thêm helper config chung nếu cần, không đưa auth state vào config. |
| `FE/src/router/index.ts` | Sửa | Route meta, global guard, redirect query và fallback route. |
| `FE/src/views/LoginView.vue` | Sửa | Gọi login API, loading/error state, persist session và redirect sau login. |
| `FE/src/views/RegisterView.vue` | Sửa | Gọi register API, loading/error state và navigation thành công. |
| `FE/src/components/AuthenticatedLayout.vue` | Sửa | Nhận username thực từ view; bỏ default demo user nếu không còn cần thiết. |
| `FE/src/views/StudentListView.vue` | Sửa nhỏ | Đọc current user cho layout và thực hiện stateless logout lifecycle. |
| `FE/src/views/StudentFormView.vue` | Sửa nhỏ | Đọc current user cho layout và thực hiện stateless logout lifecycle. |
| `FE/src/**/*.spec.ts` phù hợp | Tạo/sửa | Test auth session/API/view/guard với fetch và router mock deterministic. |
| `document/dev-note/fe/012-user-auth-api-route-guard-2026-08-18.md` | Tạo sau implementation | Ghi nhận scope thực tế, validation và remaining risks. |
| Các Dev Plan/Dev Note summaries | Sửa sau implementation | Cập nhật status thực tế của plan/note 012. |

## 7. API, storage và integration contract

| UI/API action | HTTP | Request mapping | Success handling |
|---|---|---|---|
| Register | `POST /api/v1/auth/register` | `userName -> username`, password, confirmPassword | `201`; không lưu session, về Login. |
| Login | `POST /api/v1/auth/login` | `userName -> username`, password | Đọc `data.access_token`, `data.user`; lưu session, vào protected route. |
| Account | `GET /api/v1/auth/account` | Bearer token | Trả UI-safe user summary; expose service, không gọi theo navigation mặc định. |
| Logout | `POST /api/v1/auth/logout` | Bearer token | Chấp nhận `204`; clear local state kể cả HTTP failure. |

- Storage scope là `sessionStorage`, vì vậy đóng browser tab/session sẽ không giữ login.
- Không ghi raw form values/password vào storage, URL, log hoặc error display.
- Login/register request contract backend dùng `username`; component form contract nội
  bộ hiện dùng `userName` và không cần đổi để tránh regression Storybook/test.
- Không thay đổi backend wrapper; service là adapter duy nhất từ wrapper sang type
  frontend dùng được.

## 8. Test và validation dự kiến

### Test cases

- `userApi` gửi đúng path/method/body/header; unwrap success wrapper và xử lý logout
  `204`; normalizes response error không chứa `data`.
- Session utility round-trip token/user, clear, và recovery từ JSON malformed.
- LoginView thành công lưu session + redirect; lỗi API hiện message và không lưu
  session.
- RegisterView thành công điều hướng Login; duplicate/validation error hiển thị và
  không gọi login/session logic.
- Router: unauthenticated protected navigation về Login với redirect; authenticated
  guest navigation về Student List; route public vẫn cho phép.
- Logout clear local session dù endpoint trả lỗi; `401` clear + Login, `403` không
  clear. Case `401/403` sẽ test ở policy/helper boundary không cần Student API thật.

### Quality gates

Chạy từ `FE/` sau implementation:

```bash
npm run lint
npm run test
npm run test:coverage
npm run build
```

- Đọc test output và coverage report do tool sinh ra; không chỉnh sửa report artifact.
- Chạy `npm run build-storybook` nếu source thay đổi làm ảnh hưởng `LoginForm` hoặc
  `RegisterForm`/stories; nếu không đổi các component/story, ghi rõ lý do không chạy
  command này.
- Không có backend chạy là điều kiện bắt buộc cho unit/component test; mock native
  `fetch` deterministically.

## 9. Rủi ro và giảm thiểu

| Rủi ro | Giảm thiểu |
|---|---|
| Sai khác `userName` của form với `username` backend | Map/verify ở typed API service, có test request body. |
| Token/user JSON hỏng trong storage làm app lỗi hoặc bypass guard | Parse defensive, clear state hỏng, guard coi là unauthenticated. |
| Route guard tạo cảm giác bảo mật đủ | Ghi rõ đây là UX; mọi endpoint protected tiếp tục gửi Bearer JWT và backend quyết định. |
| Open redirect qua query sau login | Chỉ chấp nhận relative internal path trước navigation. |
| Logout backend fail khiến UI vẫn có session cũ | Clear session trong `finally`, backend vẫn stateless. |
| Backend error payload có thể khác lỗi validation/business | Normalized error giữ fallback message; không tự sửa backend ngoài scope. |
| Token hết hạn chỉ được phát hiện khi gọi API | Guard kiểm tra session local đồng bộ; API `401` là nơi authoritative để clear/redirect. |

## 10. Output dự kiến

- Login/Register gọi đúng User/Auth API và hiển thị lỗi server có ích.
- Access token cùng UI-safe user summary chỉ tồn tại trong `sessionStorage` của session.
- Protected Student routes không mở được khi không có session; guest routes không mở
  lại cho user đã authenticated.
- Header hiển thị username đăng nhập thực tế và Logout dọn state đúng kể cả lỗi
  network/backend.
- Có tests regression và bốn FE quality gates bắt buộc pass, hoặc failure/blocker
  được báo minh bạch trong Dev Note.

## 11. Approval status

- Trạng thái: Approved by user on 2026-08-18.

## 12. Amendment: Register status popup

### Mục tiêu và hành vi

- Sau khi gọi Register API, hiển thị popup modal thông báo kết quả thay vì điều
  hướng hoặc chỉ đặt lỗi inline ngay lập tức.
- Popup có tiêu đề/trạng thái success hoặc failure, message từ kết quả API và nút
  `Close` có thể thao tác bằng bàn phím.
- Khi Register thành công, người dùng bấm `Close` mới điều hướng về Login. Khi thất
  bại, `Close` chỉ đóng popup để người dùng giữ nguyên form và chỉnh dữ liệu.

### Phạm vi kỹ thuật

- Sửa `FE/src/views/RegisterView.vue` để view quản lý state popup, gọi API và route
  sau khi close success popup.
- Dùng PrimeVue `Dialog` và `Button` đã có trong dependency; không thêm package hay
  gọi API trong component popup.
- Giữ validation field hiện có. Popup failure không che mất dữ liệu form, raw password
  hoặc response nhạy cảm; message chỉ dùng error được chuẩn hóa từ API service.
- Bổ sung/điều chỉnh test deterministic cho success/failure/Close behavior; chạy các
  FE quality gates hiện có.

### Out-of-scope

- Không thay đổi Register API, route guard, backend, `RegisterForm` validation hoặc
  tạo thông báo popup toàn cục cho các màn hình khác.

### Approval cần thiết

- Amendment approved by user on 2026-08-18.

## 13. Amendment: Login status popup

### Mục tiêu và hành vi

- Sau Login API, hiển thị popup modal success hoặc failure với nút `Close`, đồng bộ
  hành vi Register popup đã được phê duyệt.
- Login thành công vẫn lưu auth session ngay sau response hợp lệ, nhưng chỉ điều
  hướng tới internal redirect an toàn hoặc Student List sau khi người dùng bấm
  `Close`.
- Login thất bại hiển thị popup error; `Close` chỉ đóng popup, giữ dữ liệu form để
  người dùng có thể sửa và thử lại.

### Phạm vi kỹ thuật

- Sửa `FE/src/views/LoginView.vue` bằng PrimeVue `Dialog`/`Button` sẵn có; không
  thay đổi `LoginForm`, API service, session format hoặc route guard.
- Bổ sung test deterministic cho success/failure/Close behavior, gồm bảo toàn redirect
  sau success.
- Chạy lại các FE quality gates và cập nhật Dev Note 012 theo kết quả thực tế.

### Approval cần thiết

- Amendment approved by user on 2026-08-18.
