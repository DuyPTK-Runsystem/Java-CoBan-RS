# Developer Plan: Storybook PrimeVue Preview Runtime

## 1. Mục tiêu

- Sửa lỗi runtime khi Storybook render `LoginForm` và `RegisterForm`:
  `can't access property "config", this.$primevue is undefined`.
- Để các stories Auth chạy độc lập, sử dụng cùng PrimeVue theme/configuration với
  ứng dụng mà không cần backend.

## 2. Requirement và nguồn tham chiếu

- `FE/AGENTS.override.md`: duy trì các stories bắt buộc cho `LoginForm` và
  `RegisterForm`, không phụ thuộc live backend.
- `document/dev-impl-plan/fe/009-fe-project-skeleton-2026-08-18.md`: PrimeVue và
  Storybook là một phần của FE skeleton đã được phê duyệt.
- `FE/src/main.ts`: nguồn cấu hình PrimeVue runtime hiện tại, gồm Aura theme và
  options `prefix: 'p'`, `darkModeSelector: 'none'`.
- `FE/.storybook/preview.ts`: preview hiện chỉ import `styles.css`, chưa cài
  `PrimeVue` vào Storybook app.

## 3. Phạm vi

### In-scope

- Cài `PrimeVue` trong lifecycle preview của Storybook.
- Dùng `Aura` cùng các theme options hiện có của ứng dụng.
- Nạp `primeicons` để icon trong các form được hiển thị nhất quán.
- Xác thực build Storybook và các quality gate FE hiện có.

### Out-of-scope

- Không đổi props, emits, validation hay presentation của `LoginForm` và
  `RegisterForm`.
- Không sửa API, router, authentication, backend, package version hoặc dependency.
- Không xử lý cảnh báo bundle/chunk và package-discovery của Storybook nếu chúng
  không còn gây lỗi runtime hoặc làm command thất bại.

## 4. Kiến trúc hiện tại và nguyên nhân

```text
src/main.ts
  -> app.use(PrimeVue, { theme: Aura, ... })
  -> LoginForm/RegisterForm dùng PrimeVue Button, InputText, Password

.storybook/preview.ts
  -> chỉ import styles.css
  -> Storybook app không có $primevue
  -> PrimeVue component truy cập this.$primevue.config và lỗi khi mount
```

`npm run build-storybook` hiện có thể PASS vì quá trình build không bắt buộc mount
mọi story trong Canvas/Docs; lỗi được phát hiện khi renderer chạy trên browser.

## 5. Phương án triển khai

- Trong `preview.ts`, import `PrimeVue`, `Aura` và `primeicons` theo API PrimeVue
  v4 đang cài đặt.
- Khai báo `setup(app)` của Storybook Preview và gọi `app.use(PrimeVue, ...)` với
  cấu hình tương đương `src/main.ts`.
- Giữ global `styles.css` import hiện có. Không thêm decorator/router/API mock vì
  hai form chỉ cần component-local state và emits.

Phương án này tái sử dụng source of truth cấu hình runtime của ứng dụng, là thay đổi
nhỏ nhất để Storybook có cùng plugin context mà các component yêu cầu.

## 6. Phạm vi mã nguồn dự kiến

| Path | Thao tác | Mục đích |
|---|---|---|
| `FE/.storybook/preview.ts` | Sửa | Cài PrimeVue/Aura vào Storybook preview và import PrimeIcons. |
| `document/dev-note/fe/013-storybook-primevue-preview-2026-08-19.md` | Tạo sau implementation | Ghi nhận thay đổi thực tế, validation và cảnh báo còn lại. |
| `document/dev-note/fe/FE_DEV_NOTE_SUMMARY.md` | Sửa sau implementation | Thêm Dev Note 013. |
| `document/dev-note/summary/DEV_NOTE_SUMMARY.md` | Sửa sau implementation | Thêm Dev Note 013 vào summary toàn project. |

## 7. API, dữ liệu và integration

- Không thay đổi API, request/response, session storage, database hay backend.
- Storybook vẫn chạy không cần backend; stories không được gọi HTTP.
- Configuration mới chỉ áp dụng cho Storybook preview runtime và khớp app runtime
  hiện tại.

## 8. Test và validation dự kiến

- Mở Canvas/Docs của toàn bộ required stories để kiểm tra không còn lỗi
  `$primevue`:
  - LoginForm: Default, Filled, ValidationError.
  - RegisterForm: Default, Filled, PasswordMismatch, ValidationError.
- Chạy từ `FE/`:

```text
npm run lint
npm run test
npm run test:coverage
npm run build
npm run build-storybook
```

- Ghi rõ kết quả từng command và các warning còn lại; không sửa report artifacts.

## 9. Rủi ro và giảm thiểu

| Rủi ro | Giảm thiểu |
|---|---|
| Theme Storybook lệch app runtime | Reuse chính xác Aura/options trong `src/main.ts`. |
| Thay đổi preview ảnh hưởng stories khác | Chỉ đăng ký plugin global đã là dependency hiện có; kiểm tra cả Login và Register stories. |
| Build PASS nhưng Canvas vẫn lỗi | Kiểm tra runtime Canvas/Docs ngoài `build-storybook`. |
| Cảnh báo Storybook bị nhầm là lỗi blocker | Phân biệt warning với command exit code và runtime behavior. |

## 10. Output dự kiến

- Canvas và Docs của LoginForm/RegisterForm render bình thường, không còn lỗi
  `$primevue` undefined.
- Các icon/form controls PrimeVue có theme Aura và stylesheet icon tương thích với
  ứng dụng.
- Không có thay đổi hành vi ngoài cấu hình Storybook preview.

## 11. Approval status

- Trạng thái: Approved by user on 2026-08-19.

## 12. Amendment: Storybook CSS type declarations

- Approval: user approved on 2026-08-19.
- Add `vite/client` to `FE/tsconfig.node.json` types. The project already includes
  `.storybook/**/*.ts` in this tsconfig; Vite's client declaration supplies the
  `*.css` module typing needed by `preview.ts` side-effect imports.
- No runtime behavior, dependency, component, API or story state changes.
