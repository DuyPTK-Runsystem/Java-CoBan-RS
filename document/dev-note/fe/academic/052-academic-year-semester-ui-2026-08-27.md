# Dev Note: Academic Year & Semester UI

## Related Developer Plan

- Plan:
  `document/dev-impl-plan/fe/academic/052-academic-year-semester-ui-2026-08-27.md`.
- Plan approval status: `Approved by user message; implementation is incremental`.
- Bước 1 Storybook UI đã được người dùng đồng ý; các bước tiếp theo đã bổ sung
  typed API boundary, formatter ngày giờ và route/view orchestration.

## Actual scope completed

- Dựng component trình bày PrimeVue cho Academic Year/Semester table và ba
  dialog state theo wireframe đã duyệt.
- Thêm dữ liệu UI type cho status, metadata học kỳ và completeness report; giữ
  date-only và local datetime ở dạng string.
- Thêm Storybook stories deterministic cho lifecycle matrix, form create/edit,
  trạng thái `CLOSED`, report `COMPLETE/INCOMPLETE/FAILED` và loading/empty.
- Thêm unit test cho trim và validation ký tự của Academic Year, cùng adapter
  date/datetime local.
- Cập nhật copy theo feedback review: `DRAFT` của Academic Year là “Chưa hoạt
  động”, nhãn năm học là “Năm học”, mã năm học chỉ nhận chữ số/dấu cách/dấu
  gạch ngang sau khi trim; bỏ display order khỏi Semester table; mã học kỳ có
  gợi ý `HK1`/`HK2` nhưng vẫn editable; report `FAILED` hiển thị “Lỗi”.
- Định dạng ngày hiển thị là `dd/mm/yyyy`, local datetime hiển thị là
  `dd/mm/yyyy hh:mm:ss`; API boundary giữ nguyên chuỗi local date/datetime và
  không dùng `toISOString()`.
- Cập nhật Academic Year/Semester dialog dùng PrimeVue `DatePicker` với
  `dateFormat="dd/mm/yy"`; trường `Thời điểm tự động khóa` bật chọn ngày + giờ,
  hiển thị icon `calendar-clock` và vẫn emit local datetime theo wire contract.
- Triển khai `academicApi.ts` cho toàn bộ endpoint Academic Year/Semester trong
  scope plan, gồm query `academicYearId`, checkpoint tùy chọn và reopen reason.
- Triển khai hai route-level view cho danh sách năm học/học kỳ, nối loading,
  empty, error, forbidden, filter cục bộ, dialog create/edit và lifecycle API.
- Route học kỳ resolve Academic Year từ danh sách năm học trước khi gọi API
  Semester; status dialog tải completeness report và reopen yêu cầu reason.
- Cập nhật router v2 và navigation authenticated shell cho module Academic.
- Thêm unit test orchestration cho hai view và cập nhật coverage include cho
  academic source files.
- Storybook tiếp tục chỉ dùng dữ liệu deterministic ở component/dialog, không gọi
  backend; view orchestration được kiểm tra bằng unit test.
- Giữ lại wireframe, README và Developer Plan từ giai đoạn review.

## Files changed

### Wireframe review

- `document/wireframes/fe/academic/052-academic-year-semester/index.html`.
- `document/wireframes/fe/academic/052-academic-year-semester/README.md`.

### FE Storybook preview

- `FE/src/types/academic.ts`.
- `FE/src/utils/academicDate.ts`.
- `FE/src/utils/academicDate.spec.ts`.
- `FE/src/components/AcademicYearTable.vue` và `.stories.ts`.
- `FE/src/components/AcademicYearDialog.vue`, `.spec.ts` và `.stories.ts`.
- `FE/src/components/SemesterTable.vue` và `.stories.ts`.
- `FE/src/components/SemesterDialog.vue` và `.stories.ts`.
- `FE/src/components/SemesterStatusDialog.vue` và `.stories.ts`.
- `FE/src/views/AcademicYearListView.vue` và `.spec.ts`.
- `FE/src/views/SemesterListView.vue` và `.spec.ts`.
- `FE/src/router/index.ts` và `.spec.ts`.
- `FE/src/views/AuthenticatedV2ShellView.vue`.
- `FE/vite.config.ts`.
- `FE/src/styles.css`.

### Developer Plan

- `document/dev-impl-plan/fe/academic/052-academic-year-semester-ui-2026-08-27.md`.
- `document/dev-impl-plan/fe/FE_DEV_PLAN_SUMMARY.md`.

### Dev Note

- `document/dev-note/fe/academic/052-academic-year-semester-ui-2026-08-27.md`.
- `document/dev-note/fe/FE_DEV_NOTE_SUMMARY.md`.
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`.

## Important decisions

- Storybook là preview bước 1; dữ liệu trong stories là mock deterministic và
  không đại diện cho dữ liệu production.
- Form Academic Year là dialog trên Academic Year list; form Semester và trạng
  thái Semester là dialog trên Semester list.
- Các table giữ thứ tự dữ liệu nhận từ props và chỉ emit action; view sẽ sở hữu
  orchestration, API calls và state khi triển khai bước tiếp theo.
- Form Semester không expose field status; activate/lock/reopen sẽ dùng
  lifecycle endpoint ở bước integration.
- API service dùng đúng lifecycle endpoint riêng cho activate, lock, reopen và
  close Academic Year; không thêm endpoint delete hoặc arbitrary transition.
- `INCOMPLETE` chỉ hiển thị warning trong status dialog, không chặn nút lock.
- `CLOSED` được trình bày read-only; `lockedBy` chỉ hiển thị ID backend trả về.
- Không suy luận `lockSource` hoặc tên người thực hiện vì current Semester
  response chỉ expose `lockedBy` ID và không expose `MANUAL/AUTOMATIC`.
- DatePicker chỉ là lớp hiển thị/chọn ngày giờ; giá trị gửi ra vẫn dùng format
  API `yyyy-MM-dd` và local datetime, tránh timezone shift.

## Validation result

| Check | Result | Evidence |
|---|---|---|
| `cd FE && npm run lint` | PASS | ESLint không có warning/error |
| `cd FE && npm run test` | PASS | 25 files, 82 tests passed |
| `cd FE && npm run test:coverage` | PASS | FE suite passed; total 89.27% statements; academic views/service/date adapter included |
| `cd FE && npm run build` | PASS | `vue-tsc` và Vite production build hoàn tất |
| `cd FE && npm run build-storybook` | PASS | Storybook 8.6.18 build hoàn tất |
| Storybook dev server + HTTP smoke check | PASS | `http://localhost:6006/` trả `200`; `index.json` liệt kê 5 Academic story groups |
| Integrated browser screenshot | NOT RUN | Browser tích hợp của môi trường không khả dụng; đã dùng build và HTTP smoke check thay thế |
| `git diff --check` | PASS | Exit `0`, không có whitespace error |

## Deviations from Developer Plan

- So với draft đầu tiên, ba page-level form/status screen và năm route phụ đã
  được loại bỏ theo feedback người dùng; thay bằng dialog state trong hai list
  view/route.
- Implementation được chia thành các bước theo yêu cầu người dùng; route/view
  orchestration và confirmation/lifecycle flow đã hoàn tất trong lần tiếp tục
  này.
- Feedback sau bước Storybook được xử lý trong cùng Plan 052: định dạng hiển thị
  chuyển từ dấu `-`/native browser locale sang `dd/mm/yyyy`, và datetime dùng
  DatePicker có ngày + giờ.
- Status của Plan 052 được cập nhật từ draft sang approved/incremental sau khi
  người dùng xác nhận approval qua agent.

## Known blockers and remaining risks

- Chưa chạy kiểm thử flow production với backend thật; các view hiện được kiểm
  tra bằng API mocks trong unit test.
- Chưa chạy browser screenshot tích hợp trong môi trường hiện tại; Storybook
  build đã PASS và các component/dialog stories vẫn deterministic.
- Auth role discovery và Semester lock-source/user-summary gaps vẫn còn như ghi
  trong Developer Plan.

## Next step

- Không còn bước implementation bắt buộc trong phạm vi Plan 052. Bước tiếp theo
  tùy môi trường là chạy manual flow với backend thật và browser screenshot.
