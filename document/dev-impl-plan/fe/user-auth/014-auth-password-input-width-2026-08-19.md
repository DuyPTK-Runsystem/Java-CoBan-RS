# Developer Plan: Auth Password Input Width

## 1. Mục tiêu

- Sửa UI của LoginForm và RegisterForm để Password và Confirm password có cùng
  chiều rộng hiển thị với User name trong form.
- Giữ nguyên validation, toggle-mask, loading state, API flow và public component
  contract hiện có.

## 2. Requirement và nguồn tham chiếu

- `document/application-doc/v1/modules/UserModule.md`: Login có User Name/Password;
  Register có User Name/Password/Confirm Password. Cả input phải sẵn sàng cho người
  dùng thao tác.
- `FE/AGENTS.override.md`: dùng Vue 3/PrimeVue, duy trì LoginForm và RegisterForm
  stories, không tự mở rộng scope.
- `FE/src/styles.css`: đã đặt `.field-group > .p-password { width: 100%; }`.
- PrimeVue Password v4: input bên trong dùng class `p-password-input`; wrapper rộng
  100% nhưng input hiện vẫn dùng độ rộng mặc định, gây lệch so với InputText.

## 3. Phạm vi

### In-scope

- Bổ sung CSS để input thực tế bên trong PrimeVue Password kế thừa chiều rộng đầy
  đủ của field group.
- Áp dụng cho LoginForm Password, RegisterForm Password và Confirm password.
- Kiểm tra các stories Auth ở trạng thái Default, Filled và validation/error vẫn
  hiển thị đúng.

### Out-of-scope

- Không đổi markup, props, emits, client validation, text, icons hoặc toggle-mask.
- Không thay đổi API/auth/session/router/backend/dependency hay PrimeVue theme.
- Không điều chỉnh kích thước các field Student hoặc layout ngoài Auth form.

## 4. Kiến trúc hiện tại và nguyên nhân

```text
.field-group
  ├── InputText (.p-inputtext)            -> width: 100%
  └── Password (.p-password)              -> width: 100%
        └── input (.p-password-input)     -> default intrinsic width
```

Rule hiện tại làm wrapper Password rộng hết field nhưng không truyền width vào input
con. Vì vậy control Password trông ngắn hơn User name dù thuộc cùng field group.

## 5. Phương án triển khai

- Sửa `FE/src/styles.css` bằng selector dưới `.field-group` cho
  `.p-password-input` với `width: 100%`.
- Giữ selector giới hạn trong field group để không làm thay đổi mọi Password control
  có thể xuất hiện ở khu vực khác trong tương lai.
- Không thêm inline style hoặc lặp prop `inputStyle` vào ba component instance,
  vì đây là presentation rule dùng chung của auth form.

## 6. Phạm vi mã nguồn dự kiến

| Path | Thao tác | Mục đích |
|---|---|---|
| `FE/src/styles.css` | Sửa | Đặt chiều rộng 100% cho input PrimeVue Password trong field group. |
| `document/dev-note/fe/user-auth/014-auth-password-input-width-2026-08-19.md` | Tạo sau implementation | Ghi nhận thay đổi và validation thực tế. |
| `document/dev-note/fe/FE_DEV_NOTE_SUMMARY.md` | Sửa sau implementation | Thêm Dev Note 014. |
| `document/dev-note/summary/DEV_NOTE_SUMMARY.md` | Sửa sau implementation | Thêm Dev Note 014 vào summary toàn project. |

## 7. API, dữ liệu và integration

- Không thay đổi API, dữ liệu, storage, router hoặc backend.
- LoginForm và RegisterForm tiếp tục dùng cùng `LoginValues`/`RegisterValues` và
  emits hiện tại.

## 8. Test và validation dự kiến

- Kiểm tra trực quan Storybook cho LoginForm và RegisterForm: password fields thẳng
  hàng/cùng chiều rộng với User name, icon toggle vẫn ở mép phải.
- Chạy từ `FE/`:

```text
npm run lint
npm run test
npm run test:coverage
npm run build
npm run build-storybook
```

- Không thêm unit test CSS-only nếu test hiện tại không có visual/browser assertion;
  không sửa report artifacts.

## 9. Rủi ro và giảm thiểu

| Rủi ro | Giảm thiểu |
|---|---|
| Selector áp dụng ngoài ý muốn | Scope selector dưới `.field-group` đã có của form. |
| Icon toggle bị che hoặc lệch | Kiểm tra Default/Filled/Auth stories sau thay đổi. |
| Regression validation/form emit | Không sửa component logic; chạy bộ test hiện có. |

## 10. Output dự kiến

- Login Password và Register Password/Confirm password có cùng chiều rộng với User
  name trên màn hình và Storybook.
- Không có thay đổi hành vi đăng nhập, đăng ký hoặc validation.

## 11. Approval status

- Trạng thái: Approved by user on 2026-08-19.
