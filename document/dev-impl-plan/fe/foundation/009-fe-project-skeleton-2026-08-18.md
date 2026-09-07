# Developer Plan: FE Project Skeleton

## 1. Mục tiêu

- Khởi tạo project frontend trong `FE/` theo stack đã thống nhất: Vue 3, Vite, TypeScript, Vue Router, PrimeVue và Storybook.
- Tạo skeleton có thể chạy được, có cấu trúc module rõ ràng để tiếp tục triển khai User và Student module.
- Dựng các route/view/component khung bám theo bốn mẫu HTML trong `document/application-doc/v1/html-sample/`:
  - Login.
  - Register.
  - Student List.
  - Add/Update Student.
- Thiết lập nền tảng layout, typography, màu sắc và responsive behavior theo các file `DESIGN.md` của từng screen.

## 2. Tài liệu và nguồn tham chiếu

- `document/application-doc/v1/ApplicationContext.md`.
- `document/application-doc/v1/modules/UserModule.md`.
- `document/application-doc/v1/modules/StudentModule.md`.
- `FE/AGENTS.override.md`.
- `document/application-doc/v1/html-sample/login-screen/code.html` và `DESIGN.md`.
- `document/application-doc/v1/html-sample/register-screen/code.html` và `DESIGN.md`.
- `document/application-doc/v1/html-sample/student-list-screen/code.html` và `DESIGN.md`.
- `document/application-doc/v1/html-sample/add-or-update-a-student/code.html` và `DESIGN.md`.

## 3. Phạm vi

### In-scope

- Khởi tạo `FE/` bằng Vite với Vue 3 và TypeScript.
- Cấu hình các dependency cần thiết:
  - `vue-router` cho navigation.
  - `primevue` và dependency icon/theme phù hợp với version được cài đặt.
  - Storybook cho Vue 3/Vite.
- Tạo cấu trúc tối thiểu:

```text
FE/
├── src/
│   ├── components/
│   │   ├── LoginForm.vue
│   │   ├── RegisterForm.vue
│   │   ├── StudentSearchForm.vue
│   │   ├── StudentTable.vue
│   │   └── StudentForm.vue
│   ├── views/
│   │   ├── LoginView.vue
│   │   ├── RegisterView.vue
│   │   ├── StudentListView.vue
│   │   └── StudentFormView.vue
│   ├── router/
│   │   └── index.ts
│   ├── services/
│   ├── types/
│   ├── utils/
│   ├── App.vue
│   └── main.ts
└── .storybook/
```

- Khai báo route nền tảng cho flow:

```text
/register -> /login -> /students -> /students/new
                              \-> /students/:studentId/edit
```

- Tạo layout dùng chung cho authentication và authenticated screens ở mức skeleton.
- Dựng presentation structure theo mẫu HTML:
  - Auth screens dùng centered surface tối đa khoảng 480px.
  - Student screens dùng header 64px, sidebar khoảng 260px, main content tối đa 1280px.
  - Student List có vùng search, bảng, action Add/Edit/Delete và pagination placeholder.
  - Student Form có khác biệt tối thiểu giữa add mode và edit mode: ẩn Student Id ở add, read-only Student Id/Student Code ở edit, trạng thái Generate Code theo mode.
- Dùng PrimeVue cho các control student screens như `InputText`, date control, `Button`, `DataTable`, `Column`, paginator và confirmation dialog khi version đã chọn hỗ trợ tương ứng.
- Tạo các type nền cho user/student, form state và page query để các component không dùng `any`.
- Tạo Storybook stories tối thiểu, không phụ thuộc backend:
  - `LoginForm`: Default, Filled, ValidationError.
  - `RegisterForm`: Default, Filled, PasswordMismatch, ValidationError.
- Thêm cấu hình public API base URL bằng Vite environment variable ở mức placeholder, không đưa secret vào frontend.
- Cập nhật scripts/documentation cần thiết để người phát triển có thể chạy dev server, build, lint và Storybook.

### Out-of-scope

- Không gọi REST API thật và không triển khai `userApi.ts`/`studentApi.ts` ngoài type hoặc placeholder cần cho skeleton.
- Không chốt hoặc triển khai cơ chế session/token/auth guard vì tài liệu đang để TBD.
- Không triển khai đầy đủ đăng ký, đăng nhập, logout, CRUD student, search, server-side sort, server-side pagination hoặc delete mutation.
- Không thêm Pinia/Vuex, Axios, Bootstrap, Tailwind hoặc UI framework ngoài stack đã thống nhất.
- Không triển khai CSV export, Spring Batch, permission/role hoặc xử lý backend.
- Không thay đổi các mẫu HTML/ảnh/design document hiện có.

## 4. Thiết kế kỹ thuật

### Bootstrap và dependency

- Dùng npm scripts do Vite/Storybook tạo và giữ cấu hình đơn giản.
- Kiểm tra package versions tương thích với Vue 3, Vite và PrimeVue trước khi cài.
- Import global PrimeVue configuration/theme ở entry point; chỉ đăng ký component cần dùng nếu phù hợp với version.
- Dùng `<script setup lang="ts">` và Composition API cho component mới.

### Routing

- Route names/path phải ổn định để các view có thể được thay thế bằng implementation đầy đủ ở plan sau.
- Root route chuyển tới Login hoặc route mặc định đã được thống nhất trong implementation.
- Chưa dùng route guard để enforce security; có thể đặt placeholder state nhưng không giả lập auth thành công.
- Add và Edit dùng cùng `StudentFormView`, phân biệt bằng route path/param.

### Component boundary

- `views` điều phối route và page state.
- `components` chỉ chịu trách nhiệm presentation, typed props/emits và form interaction cục bộ.
- `services` để trống hoặc chỉ chứa contract placeholder; không đặt raw HTTP call trong template/component.
- Shared styles/tokens đặt ở global stylesheet của FE, bám palette Academic Core: neutral surface, white surface, indigo primary, slate secondary và semantic error.

### Screen mapping

| Screen mẫu | View | Component chính | Route |
|---|---|---|---|
| `login-screen` | `LoginView.vue` | `LoginForm.vue` | `/login` |
| `register-screen` | `RegisterView.vue` | `RegisterForm.vue` | `/register` |
| `student-list-screen` | `StudentListView.vue` | `StudentSearchForm.vue`, `StudentTable.vue` | `/students` |
| `add-or-update-a-student` | `StudentFormView.vue` | `StudentForm.vue` | `/students/new`, `/students/:studentId/edit` |

## 5. Validation và trạng thái skeleton

- Hiển thị được các trạng thái presentation cơ bản: default, loading placeholder, empty state, validation error và generic API error placeholder.
- Auth form giữ đúng field/constraint theo tài liệu: username tối đa 20, password 6..15, confirm password phải khớp.
- Student form giữ đúng max length đã tài liệu hóa: student code 10, student name 20, address 255; không tự đặt range cho `averageScore`.
- Các action submit/search/save/delete chỉ emit event hoặc hiển thị placeholder khi chưa có API implementation.
- Không lưu hoặc log plaintext password ngoài state cần thiết cho form interaction.

## 6. Kiểm thử và validation sau implementation

- Chạy từ `FE/` các scripts thực tế có trong `package.json`:

```text
npm run lint
npm run build
```

- Chạy Storybook build hoặc test script tương ứng nếu được cấu hình.
- Kiểm tra thủ công các route và responsive layout ở desktop/mobile nếu môi trường cho phép.
- Kiểm tra không có lỗi TypeScript, import PrimeVue sai version, route không resolve hoặc story cần live backend.
- Chỉ kết luận plan hoàn tất khi skeleton khởi động được và các validation script bắt buộc pass.

## 7. Rủi ro và điểm cần xác nhận

- PrimeVue API/theme và tên date component phụ thuộc version; implementation phải theo package version thực tế, không sao chép mù từ mockup.
- API path, auth mechanism, response envelope và error schema cần giữ TBD cho tới khi backend contract được chốt.
- Mockup HTML có thể chứa markup/style dùng riêng cho static sample; chỉ lấy layout, field, interaction intent và design token cần thiết.
- Sidebar mobile behavior được mô tả là collapse/hamburger hoặc bottom-sheet; skeleton chọn behavior đơn giản, dễ thay đổi và không mở rộng thành navigation system riêng.

## 8. Kết quả mong đợi

- `FE/` là một Vue project skeleton chạy được bằng Vite.
- Bốn screen mẫu có route và component tương ứng, hiển thị đúng cấu trúc chính.
- PrimeVue được sẵn sàng cho các student screens.
- LoginForm và RegisterForm có đủ stories bắt buộc, chạy độc lập với backend.
- Cấu trúc source và type boundary sẵn sàng cho các plan FE tiếp theo.
- Không có quyết định ngầm về auth, API hoặc business rule còn TBD.

## 9. Approval status

- Trạng thái: Approved by user on 2026-08-18.
- Implementation completed within the approved scope.
