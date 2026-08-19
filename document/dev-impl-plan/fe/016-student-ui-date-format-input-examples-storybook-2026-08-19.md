# Developer Plan: Student UI Date Format, Input Examples và Storybook

## 1. Mục tiêu

- Chuẩn hoá mọi ngày tháng hiển thị hoặc nhập trong Student UI theo định dạng
  `dd-mm-yyyy`.
- Bổ sung ví dụ nhập liệu rõ ràng cho Student code, Full name, Birthday và Average
  score.
- Cho phép nhập Student code ở Add mode theo `STUxxxxxxx`, tự chuẩn hoá input chỉ có
  chữ số khi rời textbox và báo warning format ngay dưới trường khi input không hợp lệ.
- Bổ sung Storybook độc lập backend cho các reusable component của module Student.

## 2. Requirement và nguồn tham chiếu

- Yêu cầu người dùng ngày 2026-08-19:
  - các trường ngày/tháng/năm trên UI dùng `dd-mm-yyyy`;
  - Student code: `Example: STU1234567`;
  - Birthday: `dd-mm-yyyy`;
  - Full name: `Example: John Doe`;
  - Average score: `Example: 6.7`;
  - Address: `Example: HCMC, Vietnam`;
  - Add Student cho nhập Student code theo `STUxxxxxxx`; khi blur một chuỗi chỉ gồm
    tối đa 7 chữ số, tự fill prefix `STU` và số `0` ở bên trái (ví dụ
    `123456 -> STU0123456`); quá 7 chữ số phải warning ngay dưới textbox;
  - Update Student disable textbox Student code;
  - bổ sung Storybook cho module Student.
- `document/application-doc/modules/StudentModule.md`: Birthday dùng định dạng UI
  `dd-mm-yyyy`, còn API vẫn dùng `yyyy-MM-dd`.
- Plan 015 và mã nguồn hiện tại: `StudentForm`/`StudentSearchForm` dùng PrimeVue
  `DatePicker`; `StudentTable` đang render thô `dateOfBirth` API; view vẫn chịu trách
  nhiệm đổi `Date` sang payload API date-only.
- `FE/AGENTS.override.md`: dùng PrimeVue, giữ page orchestration tại view, và không
  để Storybook gọi backend.

## 3. Phạm vi

### In-scope

- DatePicker Birthday tại form Add/Edit và Search dùng presentation/placeholder
  `dd-mm-yyyy`.
- Cột Birthday của Student table hiển thị date-only API `yyyy-MM-dd` thành
  `dd-mm-yyyy`, an toàn với giá trị rỗng hoặc không hợp lệ.
- Placeholder/example text cho các trường Student được nêu trong requirement. Do
  Student code, Full name và Birthday xuất hiện ở cả form và search, cùng ví dụ sẽ
  được dùng nhất quán tại cả hai nơi; Average score áp dụng ở StudentForm.
- Thay đổi StudentForm code control: Add mode có thể nhập; Edit mode disable textbox.
  Generate Code vẫn giữ nguyên hành vi API của Plan 015 và có thể ghi đè code Add đang
  nhập khi người dùng chủ động bấm nút.
- Stories cho `StudentForm`, `StudentSearchForm` và `StudentTable`, chỉ dùng props,
  emits/callback xác định và fixture cục bộ.
- Cập nhật unit/component tests bị ảnh hưởng và các Dev Plan summary.

### Out-of-scope

- Không sửa Student REST API, database/schema, request/response date format
  `yyyy-MM-dd`, validation/range của `averageScore`, route, auth hoặc API service.
- Không thêm dependency, không thêm Storybook cho route-level `StudentListView` hay
  `StudentFormView` vì chúng phụ thuộc router, session và API orchestration.
- Không đổi label nghiệp vụ hiện có, layout tổng thể, dữ liệu Student hoặc hành vi
  CRUD/search/sort/page/delete của Plan 015.

## 4. Hiện trạng và gap

```text
StudentSearchForm Birthday DatePicker -> yy-mm-dd
StudentForm Birthday DatePicker       -> yy-mm-dd
StudentTable Birthday column          -> raw yyyy-MM-dd from API
Student UI components                 -> no Storybook stories
```

- `StudentForm` chưa có placeholder/example text cho Student code, Student name,
  Birthday hay Average score.
- `StudentForm` hiện đặt Student code `readonly` ở cả Add/Edit; validation chỉ kiểm
  tra giá trị không rỗng, chưa kiểm tra format `STU` cộng 7 chữ số hoặc chuẩn hoá lúc
  blur.
- `StudentSearchForm` chưa có placeholder/example text cho Student code, Student
  name và Birthday.
- Date payload mapping trong `StudentFormView` đã là `yyyy-MM-dd`; đó là API
  boundary hợp lệ và không được thay đổi trong plan này.

## 5. Phương án triển khai

### 5.1 Date presentation

- Cấu hình hai PrimeVue `DatePicker` với format hiển thị tương ứng `dd-mm-yyyy` và
  placeholder `dd-mm-yyyy`. Với token format PrimeVue, implementation sẽ dùng token
  tạo ra năm bốn chữ số (ví dụ ngày 19/08/2026 hiển thị `19-08-2026`), không hiển thị
  literal `yyyy`.
- Tại `StudentTable`, dùng một formatter presentation nhỏ cho date-only API:
  `yyyy-MM-dd -> dd-mm-yyyy`; giá trị `null`, rỗng hoặc không khớp date-only sẽ không
  được parse qua timezone và sẽ có fallback an toàn, không làm vỡ table.
- Giữ nguyên `Date` trong component state và `yyyy-MM-dd` ở service/view payload để
  không phát sinh timezone shift hoặc breaking API contract.

### 5.2 Input examples

- Đặt `placeholder` nhất quán:
  - Student code: `Example: STU1234567`.
  - Student/Full name: `Example: John Doe`.
  - Birthday: `dd-mm-yyyy`.
  - Average score: `Example: 6.7`.
  - Address: `Example: HCMC, Vietnam`.
- Placeholder chỉ hướng dẫn nhập; không tự điền dữ liệu, không thay validation và
  không thay các business rule khác.

### 5.3 Student code Add/Edit

- Add mode: Student code textbox được phép nhập, có placeholder `Example: STU1234567`
  và một helper text nêu format `STUxxxxxxx`. Không khoá `maxlength` ở 10 để input quá
  7 chữ số vẫn có thể hiển thị warning; validation chỉ chấp nhận mã dài 10 ký tự.
  Người dùng có thể nhập trực tiếp mã đúng định dạng hoặc một chuỗi chỉ có chữ số.
- Khi textbox mất focus, nếu giá trị chỉ gồm từ 1 đến 7 chữ số, chuẩn hoá ngay thành
  `STU` + số đó được left-pad `0` tới 7 chữ số. Ví dụ `123456` thành `STU0123456`.
  Không tự động sửa text có prefix/symbol/ký tự khác để tránh biến một mã mơ hồ thành
  dữ liệu khác.
- Validation hiển thị warning dưới textbox ngay khi giá trị có hơn 7 chữ số hoặc không
  gồm chính xác prefix `STU` kèm 7 chữ số; warning biến mất khi mã hợp lệ. Save chỉ
  emit khi code hợp lệ.
  Empty code vẫn hiển thị validation hiện có (Generate hoặc nhập một mã hợp lệ).
- Edit mode: Student code textbox `disabled`, không thể focus/chỉnh sửa; Generate Code
  cũng tiếp tục disabled. Mã đang lưu vẫn hiển thị để người dùng đối chiếu.
- `POST /students/code` vẫn fill code API trả về trong Add mode. UI không tự kiểm tra
  uniqueness; backend tiếp tục là authoritative khi Save.

### 5.4 Storybook Student components

- Tạo `StudentForm.stories.ts` với các state có ích để review: Add/default, Add có
  code hợp lệ, Edit/code disabled, warning code sai format, loading generate/save và
  API error. Callback `save`, `generateCode`, `back` là no-op deterministic callbacks.
- Tạo `StudentSearchForm.stories.ts` cho default và loading; story không mount view
  hoặc gọi API. Form không có `initialValue`, vì vậy giá trị tìm kiếm minh hoạ (nếu
  cần) sẽ được tạo bằng interaction deterministic thay vì thay đổi public contract
  chỉ phục vụ Storybook.
- Tạo `StudentTable.stories.ts` cho populated/default, loading và empty state; dùng
  fixtures `Student` tại story và callback no-op cho `pageChange`, `sortChange`,
  `edit`, `delete`.
- Reuse `FE/.storybook/preview.ts` PrimeVue/Aura hiện có; không thêm cấu hình hoặc
  package mới.

## 6. Phạm vi mã nguồn dự kiến

| Path | Thao tác | Mục đích |
|---|---|---|
| `FE/src/components/StudentForm.vue` | Sửa | DatePicker `dd-mm-yyyy`; required placeholders; Student code Add editable/blur-normalize/format warning, Edit disabled. |
| `FE/src/components/StudentSearchForm.vue` | Sửa | DatePicker và placeholders đồng nhất cho code, name, Birthday. |
| `FE/src/components/StudentTable.vue` | Sửa | Format cột Birthday từ API date-only sang `dd-mm-yyyy`. |
| `FE/src/utils/studentDate.ts` | Tạo | Formatter date-only thuần, tránh timezone shift tại StudentTable. |
| `FE/src/components/StudentForm.stories.ts` | Tạo | Storybook state xác định cho form Add/Edit/loading/error. |
| `FE/src/components/StudentSearchForm.stories.ts` | Tạo | Storybook default/loading và interaction search không backend. |
| `FE/src/components/StudentTable.stories.ts` | Tạo | Storybook populated/loading/empty, fixture cục bộ. |
| `FE/src/components/StudentSearchForm.spec.ts` | Sửa nếu cần | Giữ regression test search và assertions format/placeholder phù hợp. |
| `FE/src/components/StudentForm.spec.ts` | Tạo hoặc sửa nếu đã tồn tại khi triển khai | Test placeholders, date configuration và emit/readonly behavior bị ảnh hưởng. |
| `FE/src/utils/studentDate.spec.ts` | Tạo | Test Birthday formatter và fallback không throw. |
| `document/dev-impl-plan/fe/FE_DEV_PLAN_SUMMARY.md` | Sửa | Đăng ký Plan 016, trạng thái theo approval. |
| `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md` | Sửa | Đăng ký Plan 016, trạng thái theo approval. |
| `document/dev-note/fe/016-student-ui-date-format-input-examples-storybook-2026-08-19.md` | Tạo sau implementation | Ghi nhận thay đổi thực tế và validation. |

## 7. API / Database / Integration

- Không đổi endpoint, request/response, schema, migration hay data persistence.
- Boundary API giữ nguyên:

```text
UI Date / DatePicker (dd-mm-yyyy) <-> view state Date <-> API date-only (yyyy-MM-dd)
```

- Storybook chỉ render component và fixture cục bộ; không dùng access token,
  sessionStorage, Vue Router navigation hay HTTP request.

## 8. Test và validation dự kiến

### Test cases

- StudentForm/SearchForm render đúng placeholder yêu cầu, gồm Address
  `Example: HCMC, Vietnam`, và DatePicker nhận định dạng
  hiển thị bốn chữ số năm.
- StudentForm Add cho nhập Student code; blur `123456` thành `STU0123456`; 7 chữ số
  được chuẩn hoá đúng; input trên 7 chữ số, prefix sai, ký tự khác hoặc thiếu chữ số
  hiển thị warning và không emit Save. Mã `STU` + đúng 7 chữ số cho phép Save.
- StudentForm Edit render mã hiện có trong textbox disabled, không cho sửa hay sinh mã.
- StudentTable render `2026-08-19` thành `19-08-2026`; null/empty/malformed value có
  fallback không throw.
- Form vẫn emit đúng `Date`/form values; search vẫn emit criteria; readonly code và
  Generate button theo add/edit không regression.
- Stories build được với PrimeVue preview; populated/loading/error/empty states dùng
  dữ liệu cố định, không tạo HTTP request.

### Quality gates

Chạy từ `FE/` sau implementation:

```text
npm run lint
npm run test
npm run test:coverage
npm run build
npm run build-storybook
```

## 9. Rủi ro và giảm thiểu

| Rủi ro | Giảm thiểu |
|---|---|
| Sai token date-format PrimeVue khiến hiển thị năm hai chữ số hoặc literal. | Kiểm tra Canvas/Docs Storybook và test/visual evidence với ví dụ 19-08-2026. |
| Parse `yyyy-MM-dd` qua UTC làm lệch ngày theo timezone. | Tách chuỗi date-only, không dùng `toISOString()` hoặc `new Date()` để format table. |
| Placeholder cho InputNumber không hiện do wrapper PrimeVue. | Xác nhận component prop theo PrimeVue 4 và thêm test/story visual thực tế. |
| Auto-format Student code làm mất input người dùng. | Chỉ chuẩn hoá giá trị hoàn toàn là 1–7 chữ số lúc blur; các input khác giữ nguyên và được warning rõ ràng. |
| Frontend cho nhập code không đảm bảo uniqueness. | Chỉ hỗ trợ format/UX ở client; backend vẫn quyết định uniqueness khi Create. |
| Stories vô tình phụ thuộc app runtime/API. | Chỉ story reusable component, dùng fixture và no-op callback. |
| Plan 015 còn trạng thái awaiting user review. | Plan 016 không sửa lifecycle/API của Plan 015; chỉ triển khai sau approval riêng của Plan 16. |

## 10. Output dự kiến

- Mọi Birthday trong Student form, search và table hiển thị/được hướng dẫn dưới dạng
  `dd-mm-yyyy`; API vẫn nhận/trả `yyyy-MM-dd` theo contract hiện hành.
- Các trường yêu cầu có ví dụ nhập liệu đúng nguyên văn.
- Add Student nhận Student code theo `STUxxxxxxx`, tự chuẩn hoá giá trị chỉ có tối đa
  7 chữ số khi blur và warning format hợp lệ; Update Student hiển thị code disabled.
- Storybook có nhóm StudentForm, StudentSearchForm và StudentTable với các state
  hữu ích, không cần backend.
- Năm quality gates frontend, bao gồm `build-storybook`, pass hoặc được Dev Note ghi
  nhận minh bạch nếu có blocker có sẵn.

## 11. Approval status

- Trạng thái: Approved by user on 2026-08-19; amendment 16.1 approved and implemented on 2026-08-19.

## 12. Amendment 16.1: Deferred Student code format warning while typing

### Requirement

- Khi người dùng đang nhập Student code, không hiện warning nếu phần numeric chưa quá
  7 chữ số.
- Average score tại StudentForm, nếu có giá trị, phải nằm trong range `0–10`.

### Phương án cập nhật

- Trong khi textbox còn focus, coi các trạng thái nhập dở sau là hợp lệ tạm thời và
  không hiển thị warning: chuỗi chỉ gồm 1–7 chữ số; `STU`; hoặc `STU` theo sau bởi
  tối đa 7 chữ số.
- Hiển thị warning ngay trong lúc nhập nếu có hơn 7 chữ số hoặc có ký tự/prefix không
  hợp lệ. Khi textbox blur hoặc người dùng Save, chạy validation đầy đủ: chỉ
  `STU` + đúng 7 chữ số mới hợp lệ. Blur chuỗi chỉ gồm 1–7 chữ số vẫn tự chuẩn hoá như
  scope ban đầu.
- Giữ `averageScore` nullable. Khi người dùng Save, value khác `null` ngoài range
  inclusive `0–10` hiển thị field error và không emit Save; InputNumber cũng nhận
  `min="0"`/`max="10"` như guardrail UX. Backend trong Plan 17 vẫn là authoritative.
- Cập nhật unit test để phân biệt trạng thái input dở không warning, overflow warning
  ngay lập tức, validation sau blur/Save và score boundaries `0`, `10`, invalid
  `-0.01`/`10.01`.

### Phạm vi thay đổi

- `FE/src/components/StudentForm.vue`.
- `FE/src/components/StudentForm.spec.ts`.
- `FE/src/components/StudentForm.stories.ts` nếu cần thêm state score invalid.
- Dev Note 016 và các summary chỉ cập nhật sau implementation.

### Approval

- Amendment approved by user and implemented on 2026-08-19.
