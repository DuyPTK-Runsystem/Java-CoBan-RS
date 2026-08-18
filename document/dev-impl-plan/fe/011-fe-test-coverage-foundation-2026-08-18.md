# Developer Plan: FE Test/Coverage Foundation

## 1. Mục tiêu

- Bổ sung nền tảng unit/component test cho frontend Vue 3.
- Bổ sung lệnh tạo coverage report thực tế.
- Viết các test deterministic để gỡ blocker test/coverage đang được ghi nhận trong
  `document/dev-note/fe/FE_DEV_NOTE_SUMMARY.md`.
- Đưa các quality gate bắt buộc trong `FE/AGENTS.override.md` về trạng thái có thể
  chạy và đánh giá bằng bằng chứng thực tế.

## 2. Requirement và nguồn tham chiếu

- `FE/AGENTS.override.md` yêu cầu các gate bắt buộc:
  - `npm run lint`.
  - `npm run test`.
  - `npm run test:coverage`.
  - `npm run build`.
- `document/dev-note/fe/010-frontend-validation-quality-gates-2026-08-18.md`
  ghi nhận `test` và `test:coverage` đang bị block vì chưa có test runner và script.
- Test phải độc lập với backend đang chạy và không được chỉnh sửa thủ công report
  artifact để tạo kết quả giả.

## 3. Trạng thái hiện tại

- Frontend dùng Vue 3, Vite và TypeScript.
- `FE/package.json` chưa có script `test` hoặc `test:coverage`.
- Project chưa có test runner, DOM test environment hoặc coverage provider.
- `LoginForm` và `RegisterForm` đã có Storybook stories deterministic nhưng chưa có
  component test tự động.
- Lint, production build và Storybook build đã có script riêng.

## 4. Phạm vi

### In-scope

- Chọn và cấu hình test runner tương thích với Vue 3, Vite và TypeScript hiện tại.
- Bổ sung thư viện mount/component testing và DOM environment tối thiểu cần thiết.
- Bổ sung coverage provider tương thích với test runner.
- Thêm chính xác các npm scripts:
  - `test` để chạy test một lần, phù hợp với quality gate và CI.
  - `test:coverage` để chạy test và sinh coverage report.
- Tạo setup/config test tối thiểu, không đưa state hoặc network thật vào test.
- Viết test deterministic cho các component hiện có, ưu tiên:
  - `LoginForm`: validation bắt buộc và submit/emit dữ liệu hợp lệ.
  - `RegisterForm`: password mismatch và submit/emit dữ liệu hợp lệ.
  - Một component Student phù hợp: kiểm tra props/emits hoặc trạng thái hiển thị
    có giá trị regression rõ ràng.
- Đọc command output và coverage report để xác nhận test thực sự được thu thập.
- Sau implementation, tạo Dev Note 011 và cập nhật Dev Note Summary theo kết quả
  thực tế.

### Out-of-scope

- Không triển khai API service, auth state, route guard hoặc gọi backend thật.
- Không thay đổi API contract, business rule hoặc behavior production ngoài điều
  chỉnh tối thiểu cần thiết để component hiện tại có thể test đúng contract.
- Không thêm end-to-end test framework hoặc browser automation.
- Không thêm visual regression, snapshot diện rộng hoặc Storybook interaction test.
- Không đặt coverage threshold tùy ý khi project chưa có quyết định chính thức.
- Không chỉnh sửa, tạo giả, xóa hoặc patch thủ công report artifact.
- Không sửa backend, CI/CD hoặc dependency ngoài `FE/`.

## 5. Phương án kỹ thuật dự kiến

- Ưu tiên Vitest vì dùng chung hệ sinh thái và module resolution với Vite.
- Dùng Vue Test Utils để mount Vue component và kiểm tra DOM/emit.
- Dùng `jsdom` làm DOM environment cho component test.
- Dùng coverage provider chính thức tương thích với version Vitest được cài đặt;
  ưu tiên V8 nếu không có constraint khác từ dependency tree.
- Cấu hình test trong Vite config hoặc Vitest config riêng, chọn phương án nhỏ nhất
  nhưng vẫn giữ type checking và module resolution rõ ràng.
- Test dùng dữ liệu cố định, assertion trực tiếp và cleanup giữa các case.
- Không dùng network, thời gian thực, random value, sleep hoặc phụ thuộc thứ tự test.

Version dependency cụ thể phải được xác định tại thời điểm implementation dựa trên
`FE/package.json`, `FE/package-lock.json` và compatibility thực tế; plan này không
khóa version khi chưa cài đặt và kiểm chứng.

## 6. Khu vực dự kiến thay đổi khi implementation được phê duyệt

- `FE/package.json` và `FE/package-lock.json`.
- `FE/vite.config.ts` hoặc một file Vitest config riêng nếu cần.
- File test setup trong `FE/src/` hoặc thư mục test phù hợp.
- Các file `*.spec.ts` cạnh component hoặc trong cấu trúc test rõ ràng, nhất quán.
- Source component chỉ khi test phát hiện vấn đề testability nhỏ nằm đúng contract;
  mọi thay đổi behavior đáng kể phải dừng để xin phê duyệt.
- `document/dev-note/fe/011-fe-test-coverage-foundation-2026-08-18.md` sau khi
  implementation hoàn tất.
- `document/dev-note/fe/FE_DEV_NOTE_SUMMARY.md` và
  `document/dev-note/summary/DEV_NOTE_SUMMARY.md` sau implementation.

## 7. Test case dự kiến

### LoginForm

- Không submit khi các field bắt buộc rỗng và hiển thị validation tương ứng.
- Emit đúng payload khi username/password hợp lệ.
- Không phụ thuộc router hoặc backend.

### RegisterForm

- Hiển thị lỗi khi password và confirm password không khớp.
- Không emit submit với dữ liệu không hợp lệ.
- Emit đúng payload khi toàn bộ dữ liệu hợp lệ.

### Student component

- Chọn một hành vi ổn định đã tồn tại như render empty state, emit search hoặc
  emit action từ dữ liệu cố định.
- Không giả lập API contract chưa được triển khai.

Case cụ thể sẽ bám implementation hiện tại tại thời điểm code; không thay đổi
component contract chỉ để khớp với test dự kiến trong plan.

## 8. Validation dự kiến

Chạy từ `FE/` bằng scripts thực tế sau khi được bổ sung:

```text
npm run lint
npm run test
npm run test:coverage
npm run build
npm run build-storybook
```

Tiêu chí hoàn thành:

- Mỗi command trên trả về exit code thành công.
- Test runner phát hiện và chạy các file test thực tế, không pass do không có test.
- Coverage report được tool sinh ra và có số liệu cho source được test.
- Không chỉnh sửa thủ công generated report.
- Failure do task gây ra được sửa và chạy lại; baseline failure ngoài scope được
  ghi rõ, không che giấu.

## 9. Rủi ro và cách xử lý

- Version Vitest/Vite hoặc coverage provider không tương thích:
  - Chọn version dựa trên peer dependency và lockfile, không nâng cấp Vite/Vue ngoài
    scope nếu không cần thiết.
- PrimeVue component có thể cần plugin hoặc stub khi mount:
  - Chỉ đăng ký dependency tối thiểu hoặc dùng stub có typed contract rõ ràng.
- Test dễ phụ thuộc chi tiết markup:
  - Ưu tiên hành vi người dùng, emitted event và validation message ổn định.
- Coverage thấp ở skeleton chưa có logic hoàn chỉnh:
  - Báo số liệu thực tế; không đặt hoặc hạ threshold tùy ý để tạo trạng thái PASS.
- Test phát hiện behavior production không nhất quán:
  - Dừng phần thay đổi behavior, ghi nhận phát hiện và xin phê duyệt nếu vượt plan.

## 10. Output mong đợi

- FE có test runner và component-test environment hoạt động.
- `npm run test` và `npm run test:coverage` tồn tại và chạy deterministic.
- Có test regression cho auth form và ít nhất một hành vi Student phù hợp.
- Có coverage report thực tế để đọc và báo cáo.
- Blocker test/coverage trong FE Dev Note Summary chỉ được gỡ sau khi toàn bộ gate
  bắt buộc đã chạy PASS.
- Dev Note 011 phản ánh đúng file thay đổi, validation, coverage và vấn đề còn lại.

## 11. Approval status

- Trạng thái: Approved by user on 2026-08-18.
- Plan documentation được phê duyệt trước; implementation được người dùng phê
  duyệt riêng qua tin nhắn `impl` ngày 2026-08-18.
