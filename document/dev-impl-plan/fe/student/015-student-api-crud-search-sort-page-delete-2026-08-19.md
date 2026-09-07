# Developer Plan: Student API, CRUD, Search, Sort, Page và Delete

## 1. Mục tiêu

- Thay thế toàn bộ dữ liệu/demo message của màn hình Student bằng dữ liệu từ Student
  REST API hiện có.
- Hoàn thiện luồng tạo, sửa, tìm kiếm, sắp xếp server-side, phân trang server-side,
  sinh mã và xoá có xác nhận.
- Giữ Student screens dùng Vue 3, PrimeVue, Vue Router và native `fetch`; không thêm
  dependency, state store hay thay đổi backend ngoài contract cần được xác nhận.

## 2. Requirement và nguồn tham chiếu

- `document/application-doc/v1/modules/StudentModule.md`: Student List có search theo
  code/name/birthday với AND semantics; sort các business column; page size 10; Add,
  Edit và Delete confirmation.
- `document/application-doc/v1/ApplicationContext.md`: Student UI gọi REST API, giữ UI
  state và hiển thị validation/API error; backend là authoritative.
- `FE/AGENTS.override.md`: service typed trong `src/services`, không đặt raw HTTP
  call trong component; list dùng server-side sort/page và delete confirmation.
- Backend đã kiểm tra tại `StudentController`:
  - `GET /api/v1/students` với `studentCode`, `studentName`, `birthday`, `page`,
    `size`, `sortField`, `sortDirection`.
  - `POST /api/v1/students`, `PUT /api/v1/students/{studentId}`,
    `DELETE /api/v1/students/{studentId}`, `POST /api/v1/students/code`.
  - Success body có envelope `{ statusCode, message, data }`; page data gồm
    `content`, `page`, `size`, `totalElements`, `totalPages`.
- Quyết định người dùng ngày 2026-08-19: `studentName` có tối đa **35 ký tự**.

## 3. Phạm vi

### In-scope

- Typed Student API service, model request/response/page và tái sử dụng error policy
  của `userApi`/auth session hiện có.
- Chuyển StudentListView từ local filter/demo rows sang lazy query đến backend khi
  load, search, sort, page và sau delete/create/update.
- Kết nối StudentFormView/StudentForm với generate code, create và update API; loading,
  success/error, back navigation và refresh list có chủ đích.
- Xác nhận xoá bằng PrimeVue ConfirmDialog; chỉ gọi DELETE sau accept và reload page
  hiện tại an toàn khi tổng số bản ghi thay đổi.
- Unit/component tests deterministic cho service, list/form orchestration và các
  boundary search/sort/page/delete quan trọng.

### Out-of-scope

- Không sửa Student backend controller/service/entity/schema, authentication/JWT,
  route guard, auth storage, CSV batch hoặc Postman collection.
- Không thêm Pinia/Vuex, Axios, E2E framework, dependency UI mới hay Storybook story
  không được yêu cầu.
- Không thay đổi business rules chưa có contract, gồm range `averageScore`, policy
  retry/network offline hay UX xác nhận rời form chưa lưu.

## 4. Hiện trạng và gap

```text
StudentListView -> hard-coded students -> computed local filtering -> StudentTable
StudentFormView -> hard-coded edit initial value / placeholder save message
StudentForm -> client-side random code generation
```

- `StudentTable` đã emit page/sort/edit/delete phù hợp nhưng list hiện không gửi query
  đến server, không dùng total page metadata và không có trạng thái lỗi API.
- `StudentSearchForm` đã có đúng ba trường nhưng birthday đang dùng Date; adapter API
  phải format thành `yyyy-MM-dd` và không gửi filter rỗng.
- `StudentForm` đã giữ mode/add-edit UI nhưng code sinh tại client là 9 ký tự
  (`STU` + 6 số), trái contract backend `STU` + 7 số; phải thay bằng API generate code.
- Quan hệ BE hiện là hai chiều `Student 1 <-> 0..1 StudentInfo`: `StudentInfo` là
  owning side với FK `student_id` unique, còn `Student` dùng `cascade = ALL` và
  `orphanRemoval = true`. Vì vậy delete aggregate qua `studentRepository.delete()`
  được cascade sang StudentInfo. `Student.studentInfo` lại đặt `optional = false`,
  mâu thuẫn với cardinality `0..1` trong tài liệu và nhánh `info == null` của service;
  đây là technical-debt BE ngoài scope FE, không cản trở API integration hiện tại.
- Plan 015.1 đã được phê duyệt để bổ sung `GET /api/v1/students/{studentId}`. Plan
  015 chỉ triển khai phần Edit sau khi Dev Note 015.1 xác nhận endpoint này đã pass;
  không dùng route state hoặc quét trang list làm workaround.

## 5. Phương án triển khai

### 5.1 Service và type boundary

- Tạo `studentApi.ts` và mở rộng `types/student.ts` bằng API payload/page types.
  Các property frontend tiếp tục camelCase, cùng tên với response JSON hiện có.
- Tái sử dụng hoặc trích xuất nhỏ helper request từ `userApi` chỉ khi không làm thay
  đổi public behavior auth. Mọi request protected gửi `Authorization: Bearer` từ
  session; service unwrap `RestResponse.data`, chấp nhận `204` cho DELETE và chuẩn hoá
  lỗi nhất quán.
- Map query như sau: `Date | null -> birthday: yyyy-MM-dd | omitted`,
  `sortOrder 1/-1 -> ASC/DESC`, `page` zero-based và `size: 10`. Không gửi raw `Date`,
  không local-sort/local-page kết quả server.

### 5.2 Student List

- Dùng một `loadStudents` tập trung, quản lý query, loading, page response và error
  message. Lần vào list gọi default query page 0, size 10, sort `studentCode ASC`.
- Search reset page về 0 và gọi API; sort reset page về 0 và gọi API; paginator dùng
  page backend trả về. Table nhận `content` và `totalElements`, vì vậy ordinal `No`
  tiếp tục đúng giữa các trang.
- Search mới, sort/page trong khi đang request, empty response và API error đều có UI
  state rõ ràng; không để dữ liệu demo cũ hiển thị như dữ liệu mới.
- Delete accept gọi API, hiện kết quả/lỗi và reload query hiện tại. Nếu delete làm
  trang hiện tại vượt trang cuối, lùi về trang cuối hợp lệ rồi tải lại.

### 5.3 Student Form

- Add: `Generate code` gọi `POST /students/code`, khóa control trong lúc gọi và gán
  code backend trả về; Save gọi `POST /students`. Thành công về list với thông báo
  success an toàn cho UI.
- Edit: dùng endpoint detail đã được Plan 015.1 triển khai và xác nhận, tải record theo `studentId`,
  giữ Student Id/Code read-only, map `yyyy-MM-dd` sang `Date`, Save gọi
  `PUT /students/{studentId}` không gửi id/code trong body.
- Map form payload `Date | null` sang ISO date hoặc `null`; giữ client validation hiện
  có là UX layer, đổi giới hạn `studentName` thành 35 theo quyết định người dùng và
  bổ sung mapping lỗi server phù hợp mà không tự suy diễn range score.
- Save/generate failure giữ nguyên dữ liệu người dùng nhập, mở lại control/loading và
  hiện message đã chuẩn hoá. Back không gọi API hay tự lưu dữ liệu.

### 5.4 Session/error lifecycle

- Khi Student API trả `401`, áp dụng policy Plan 012: clear session và về Login.
  `403` giữ session và hiện access-denied; các lỗi validation/business/network khác
  giữ người dùng ở màn hình hiện tại.
- Không truyền access token qua URL, props presentation hoặc log; view lấy token từ
  auth session tại boundary service/orchestration.

## 6. API và integration contract

| UI action | HTTP | Payload/query | Thành công FE |
|---|---|---|---|
| Initial/search/sort/page | `GET /api/v1/students` | filters, `page`, `size=10`, allow-listed sort field, `ASC`/`DESC` | Bind `content` + page metadata. |
| Generate code | `POST /api/v1/students/code` | Bearer token, body theo contract backend hiện có | Fill read-only `studentCode`. |
| Add | `POST /api/v1/students` | code, name, dateOfBirth, address, averageScore | Notify rồi về/reload list. |
| Edit | `PUT /api/v1/students/{id}` | name, dateOfBirth, address, averageScore | Notify rồi về/reload list. |
| Delete | `DELETE /api/v1/students/{id}` | Bearer token | Reload valid current page. |
| Load edit detail | `GET /api/v1/students/{id}` | Bearer token; trả `ResStudentDTO` trong envelope | Contract được Plan 015.1 triển khai trước Plan 015. |

## 7. Phạm vi mã nguồn dự kiến

| Path | Thao tác | Mục đích |
|---|---|---|
| `FE/src/types/student.ts` | Sửa | Tách type UI/API/page/query rõ ràng. |
| `FE/src/services/studentApi.ts` | Tạo | Typed protected Student API và mapping envelope/error. |
| `FE/src/services/userApi.ts` | Có thể sửa nhỏ | Chỉ trích helper dùng chung nếu giúp policy HTTP nhất quán, không đổi contract auth. |
| `FE/src/views/StudentListView.vue` | Sửa | Thay demo/local state bằng server query, delete lifecycle và UI state. |
| `FE/src/views/StudentFormView.vue` | Sửa | Load/mapping form, generate/create/update lifecycle. |
| `FE/src/components/StudentForm.vue` | Sửa | Nhận generate/loading/error state từ view; bỏ random local generation. |
| `FE/src/components/StudentTable.vue` | Có thể sửa nhỏ | Đồng bộ lazy/page event hoặc accessible busy/error presentation nếu cần. |
| `FE/src/components/StudentSearchForm.vue` | Có thể sửa nhỏ | Disable submit/controls theo loading nếu cần. |
| `FE/src/**/*.spec.ts` phù hợp | Tạo/sửa | Regression tests ở API service, list/form và components bị đổi. |
| `document/dev-note/fe/student/015-student-api-crud-search-sort-page-delete-2026-08-19.md` | Tạo sau implementation | Ghi nhận scope thực tế và validation. |
| Các Dev Plan/Dev Note summary | Sửa theo trạng thái thực tế | Đồng bộ links/status sau approval/implementation. |

## 8. Test và validation dự kiến

### Test cases

- Student service map đúng URL, bearer header, envelope, empty DELETE, `Date`, query
  filter/sort/page và API error.
- List initial load, search reset page, sort direction, paginator request, empty/error
  state; không còn local filtering/sorting demo.
- Delete cancel không gọi API; accept gọi DELETE, reload list và lùi trang nếu cần;
  lỗi delete không xoá row giả tại client.
- Form generate code dùng response backend; Add/Update gửi payload đúng; loading/error
  giữ input; Back không save. Edit detail tests chỉ thực hiện sau contract detail được
  cung cấp/approved.
- `401` clear/redirect và `403` giữ session theo policy đã có của Plan 012.

### Quality gates

Chạy từ `FE/` sau implementation:

```text
npm run lint
npm run test
npm run test:coverage
npm run build
```

- Đọc output và coverage report thực tế; không sửa report artifact.
- `npm run build-storybook` không bắt buộc nếu không đổi LoginForm/RegisterForm hoặc
  stories; Dev Note sẽ ghi lý do nếu không chạy.

## 9. Rủi ro, dependency và giảm thiểu

| Mục | Ảnh hưởng | Giảm thiểu / quyết định cần có |
|---|---|---|
| Thiếu API lấy chi tiết theo id | Không thể edit an toàn sau reload/direct URL. | Xác nhận/thêm contract detail ở backend trong plan riêng trước phần Edit. |
| Quyết định tên 35 ký tự chưa đồng bộ BE | `ReqUpdateStudentDTO` và cột entity hiện đặt 20, nên API update/persistence có thể từ chối hoặc cắt dữ liệu hợp lệ theo rule mới. | Cần một BE plan/approval để đồng bộ DTO create/update và `Student.student_name` thành 35 trước hoặc cùng lúc FE rollout. |
| `Student.studentInfo optional=false` lệch cardinality `0..1` | Mapping không phản ánh chính xác trường hợp service đã xử lý `info == null`. | Ghi technical debt; chỉ sửa mapping trong BE plan riêng để tránh mở rộng scope FE. |
| Date format UI/API khác nhau | Search/form sai hoặc timezone shift. | Chỉ format local date `yyyy-MM-dd`; không dùng `toISOString()` cho date-only. |
| Request đồng thời | Response cũ có thể ghi đè query mới. | Có request identity/AbortController phù hợp, chỉ áp dụng response mới nhất. |
| Delete cuối trang | List trống sai trang. | Dùng `totalPages`/total sau delete để tải trang hợp lệ. |

## 10. Output dự kiến

- Student List hiển thị dữ liệu thật, search/sort/page server-side với page size 10 và
  delete confirmation an toàn.
- Add tạo student bằng code do backend sinh; error/loading/success được xử lý rõ ràng.
- Update hoàn chỉnh chỉ được triển khai khi contract load-detail theo id được phê duyệt;
  plan không che giấu dependency này bằng dữ liệu route/local demo.
- Có test regression và bốn quality gates FE pass, hoặc Dev Note ghi `FAIL`/`BLOCKED`
  cùng bằng chứng thực tế.

## 11. Approval status

- Trạng thái: Approved by user on 2026-08-19.
- Chỉ bắt đầu implementation sau khi người dùng kiểm tra và cho phép tiếp tục, theo
  yêu cầu phê duyệt ngày 2026-08-19.
