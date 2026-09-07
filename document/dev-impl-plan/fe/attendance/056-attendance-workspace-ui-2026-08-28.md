# Developer Plan 056: Attendance Workspace UI

## Trạng thái và phiên bản áp dụng

- Application-document version: `v2`.
- Status: `Approved; implementation completed; validation PASS`.
- Ngày lập plan: `2026-08-28`.
- Phạm vi: Frontend Vue 3/TypeScript/PrimeVue.
- Wireframe: [`document/wireframes/fe/attendance/056-attendance-workspace-ui/index.html`](../../../../wireframes/fe/attendance/056-attendance-workspace-ui/index.html).
- Plan này hợp nhất ba hạng mục trong backlog/ảnh tham chiếu:
  `Attendance Session UI`, `Attendance Exception Entry UI` và
  `Attendance History & Summary UI`.
- User đã phê duyệt rõ ràng qua agent message `approved` trước khi triển khai.

## 1. Mục tiêu

Tạo một workspace Điểm danh duy nhất trên authenticated v2 shell để người dùng
đi qua cùng một context và không bị tách thành ba màn hình rời:

1. Chọn năm học, học kỳ, lớp, ngày và buổi sáng/chiều; kiểm tra ngày/buổi học
   hợp lệ trước khi mở hoặc tạo attendance session.
2. Xem danh sách học sinh của session; mặc định hiển thị `PRESENT` khi không
   có exception và chỉ ghi dữ liệu khi người dùng nhập ngoại lệ.
3. Ghi nhận, cập nhật và xóa các trạng thái exception `ABSENT`, `EXCUSED`,
   `LATE`, `EARLY_LEAVE` cùng ghi chú tối đa 500 ký tự.
4. Xem lịch sử chuyên cần của chính học sinh (`STUDENT`) và báo cáo tổng hợp
   theo lớp/khoảng thời gian (`TEACHER`) trong cùng workspace, ở chế độ
   read-only.
5. Giữ rõ ranh giới warning, lỗi validation, lỗi quyền và trạng thái loading;
   responsive ở desktop và mobile.

## 2. Requirement và nguồn đối chiếu

- `document/application-doc/v2/ApplicationContext.md` — source precedence,
  authentication và nguyên tắc backend là nguồn authorization cuối cùng.
- `document/application-doc/v2/RequirementBaseline.md` — nhóm
  `FR/BR-CALENDAR` và `FR/BR-ATTENDANCE`.
- `document/application-doc/v2/modules/03-AttendanceAndSubjectModule.md` —
  `FR-ATTENDANCE-001..008` và `BR-ATTENDANCE-001..012`.
- `document/application-doc/v2/FrontendApiGuide.md` và
  `frontend-api/00-common-contract.md` — envelope, bearer token, HTTP error,
  date-only và typed-service boundary.
- `document/application-doc/v2/frontend-api/04-calendar-attendance.md` —
  endpoint và response shape hiện tại.
- `document/application-doc/v2/frontend-api/07-enums-and-known-drift.md` —
  canonical wire values.
- `document/application-doc/v2/data-model/06-ChangesAndAttendance.md` —
  exception-only persistence và unique session/student record.
- `FE/AGENTS.override.md` cùng `FE/agent-rules/00-foundation.md`,
  `02-domain-rules.md`, `03-api-data-boundaries.md`,
  `04-quality-documentation.md`.
- Backend implementation đã đối chiếu: Plans `028`, `030`, `031`, `033`,
  `035` và các controller/DTO trong `BE/BaiTap-RS/.../attendance/`.

## 3. Phạm vi hợp nhất

### 3.1. Context dùng chung

- Route authenticated mới: `/v2/attendance`, name `v2-attendance`.
- Navigation tĩnh trong `AuthenticatedV2ShellView.vue`: `Điểm danh`, icon
  `pi pi-calendar-clock` hoặc icon PrimeIcons tương đương đang có.
- Tải danh sách academic year, semester và class bằng service/context hiện có;
  ưu tiên academic year `ACTIVE` như pattern của Plan 054.
- Session context gồm `academicYearId`, `semesterId`, `classId`,
  `attendanceDate` (`yyyy-MM-dd`) và `sessionPeriod` (`MORNING` hoặc
  `AFTERNOON`). Khi context thay đổi, reset session, rows, selection, dialog
  và lỗi cũ trước khi tải dữ liệu mới.
- Có thể tải calendar days cho khoảng ngày đang xem để hiển thị
  `SCHEDULED`/`NO_CLASS`; đây là preflight UX, không thay thế validation của
  backend khi tạo session.

### 3.2. Tab `Điểm danh theo buổi`

- Hiển thị banner context: lớp, ngày, buổi và trạng thái ngày học.
- Nút `Mở buổi điểm danh` gọi create-or-get session; response `201` có thể là
  session mới hoặc session đã tồn tại.
- Sau khi có `sessionId`, gọi danh sách học sinh của session.
- Bảng tối thiểu: mã học sinh, họ tên, trạng thái hiện tại, ghi chú, thời gian
  cập nhật và thao tác.
- Row không có `attendanceRecordId` được hiển thị là `PRESENT` dẫn xuất; không
  gửi request `PRESENT` và không tạo record vật lý cho row đó.
- `Ghi nhận/sửa ngoại lệ` mở dialog dùng chung với status dropdown và note.
  Xóa exception yêu cầu confirmation rồi reload danh sách; thành công thì row
  trở về `PRESENT` dẫn xuất.
- Dùng `studentId` khi row có technical id; chỉ dùng by-code endpoint khi
  flow thực tế không có `studentId`, không trộn hai semantics trong một request.
- Nếu lớp hoặc semester đóng/không hợp lệ, hiển thị read-only/forbidden state;
  backend vẫn quyết định khả năng mutation.

### 3.3. Tab `Lịch sử của học sinh`

- Dành cho endpoint self-service `/api/v2/attendance/students/me/history`;
  read-only, không nhận `studentId` từ input.
- Bộ lọc tùy chọn: năm học, học kỳ, từ ngày, đến ngày, page size.
- Summary strip: `validSessionCount`, `presentCount`,
  `excusedAbsenceCount`, `unexcusedAbsenceCount`, `lateCount`,
  `earlyLeaveCount`.
- Bảng lịch sử: ngày, buổi, lớp, trạng thái, exception status và ghi chú; có
  pagination theo response (`page`, `size`, `totalElements`, `totalPages`).
- Không hiển thị dữ liệu học sinh khác; `403` giữ session và hiển thị access
  denied, `401` xóa auth state theo shared `apiClient`.

### 3.4. Tab `Báo cáo lớp`

- Dành cho `GET /api/v2/attendance/classes/{classId}/summary`, read-only.
- Bắt buộc chọn học kỳ, `from`, `to`; tùy chọn page/size (mặc định size `20`).
- Hiển thị thông tin lớp, số buổi hợp lệ và summary toàn lớp.
- Bảng học sinh: mã, họ tên, số buổi hợp lệ riêng, có mặt, từng loại
  exception và `attendanceRate`.
- Không tính lại tỷ lệ hoặc aggregation ở FE; hiển thị đúng số backend trả về.
- Nếu khoảng ngày không có session/học sinh, hiển thị empty state và các count
  bằng `0`, không coi là lỗi.

## 4. Contract backend hiện tại

### 4.1. Session và exception cho giáo viên

Canonical path được dùng trong service là `/api/v2/attendance-sessions`; backend
vẫn chấp nhận alias `/api/v2/attendance/sessions`.

| Method | Endpoint | Quyền | Payload/response |
|---|---|---|---|
| `POST` | `/api/v2/attendance-sessions` | `TEACHER` | `ReqCreateAttendanceSessionDTO` → `201 ResAttendanceSessionDTO` |
| `GET` | `/api/v2/attendance-sessions/{sessionId}/students` | `TEACHER` | `ResAttendanceStudentDTO[]` |
| `PUT` | `/api/v2/attendance-sessions/{sessionId}/exceptions/{studentId}` | `TEACHER` | `{ status, note }` → `ResAttendanceExceptionDTO` |
| `DELETE` | `/api/v2/attendance-sessions/{sessionId}/exceptions/{studentId}` | `TEACHER` | `204 No Content` |

By-code aliases `/exceptions/by-code/{studentCode}` tồn tại nhưng chỉ dùng khi
flow không có numeric `studentId`.

### 4.2. Điều chỉnh của giáo vụ

Backend cung cấp cùng contract dưới các base:

```text
/api/v2/office/attendance-sessions
/api/v2/academic-office/attendance/sessions
```

Quyền là `ADMIN` hoặc `ACADEMIC_OFFICE`. UI dùng lại presentation và request
model; việc chọn base path phải được cấp từ một capability contract đã được
phê duyệt. Auth hiện tại chưa expose role/capability, vì vậy FE không được tự
đoán role hoặc coi việc ẩn nút là security boundary. Nếu chưa có capability
adapter tại implementation, office base được ghi `BLOCKED/TBD` và không giả lập
đường gọi bằng teacher token.

### 4.3. History, summary và calendar

```text
GET /api/v2/attendance/students/me/history
  ?academicYearId&semesterId&from&to&page&size

GET /api/v2/attendance/classes/{classId}/summary
  ?semesterId&from&to&page&size

GET /api/v2/calendar/days
  ?academicYearId&semesterId&from&to
```

Wire enums phải giữ đúng contract hiện tại:

```ts
type AttendanceSessionPeriod = 'MORNING' | 'AFTERNOON'
type AttendanceExceptionStatus = 'ABSENT' | 'EXCUSED' | 'LATE' | 'EARLY_LEAVE'
```

Requirement có wording `EXCUSED_ABSENCE`/`UNEXCUSED_ABSENCE`, nhưng FE không
được gửi các giá trị đó khi backend vẫn dùng `EXCUSED`/`ABSENT`.

## 5. Quy tắc nghiệp vụ và trạng thái UI

- Attendance là exception-based: `SCHEDULED + active enrollment + không có
  exception = PRESENT`.
- Không tạo, cập nhật hoặc xóa `PRESENT` record chỉ để làm đầy bảng.
- Chỉ session hợp lệ của calendar và enrollment tại ngày session được tính;
  không tính ngày nghỉ, `NO_CLASS`, trước ngày vào lớp hoặc sau ngày rời lớp.
- Một học sinh tối đa một exception chính trong một session; UI reload row sau
  mutation để phản ánh response backend.
- `from <= to`; date-only giữ định dạng `yyyy-MM-dd`, không dùng
  `toISOString()` mù quáng gây lệch múi giờ.
- Summary theo học sinh dùng mẫu số riêng `validSessionCount`; `attendanceRate`
  chỉ là tỷ lệ backend trả về, không phải điểm hay xếp loại.
- `401` → shared client clear session và về Login; `403` → giữ session, render
  access denied; `400`/`404`/`409` → normalize và gắn lỗi đúng context;
  network/server error có retry.
- Khi request cũ trả về sau khi context đã đổi, bỏ qua response stale và không
  ghi đè rows của context mới.

## 6. Wireframe hợp nhất

Wireframe tĩnh mô phỏng một page với ba tab:

1. `Điểm danh theo buổi`: context + trạng thái calendar + bảng học sinh +
   exception dialog.
2. `Lịch sử của học sinh`: filter + summary cards + bảng read-only + paging.
3. `Báo cáo lớp`: filter thời gian + class summary + per-student table.

Bố cục dùng shell v2 hiện tại, action chính ở heading/context, table overflow
cục bộ trên mobile và dialog có content scroll/footer cố định theo pattern
PrimeVue. Wireframe có fixture tĩnh để review interaction, không gọi backend;
implementation production đã được triển khai theo cùng boundary.

Chi tiết điểm cần duyệt được ghi trong
[`wireframes/fe/attendance/056-attendance-workspace-ui/README.md`](../../../../wireframes/fe/attendance/056-attendance-workspace-ui/README.md).

## 7. Phương án kỹ thuật FE

### 7.1. Typed boundary

Tạo `FE/src/types/attendance.ts`, tách wire DTO khỏi form state:

- `AttendanceSession`, `AttendanceStudent`, `AttendanceException`.
- `StudentAttendanceHistoryItem`, `StudentAttendanceHistorySummary`,
  `StudentAttendanceHistoryResponse`.
- `ClassAttendanceSummary`, `ClassAttendanceStudentSummary`.
- `CreateAttendanceSessionRequest`, `UpsertAttendanceExceptionRequest` và
  query types cho history/summary.
- Derived UI status có thể gồm `PRESENT` nhưng không đưa `PRESENT` vào request
  exception union.

Tạo `FE/src/services/attendanceApi.ts` dùng `apiClient`, dự kiến các hàm:

```text
fetchAttendanceCalendar(token, query)
createOrGetAttendanceSession(token, request, scope)
fetchAttendanceSessionStudents(token, sessionId, scope)
upsertAttendanceException(token, sessionId, studentId, request, scope)
deleteAttendanceException(token, sessionId, studentId, scope)
fetchStudentAttendanceHistory(token, query)
fetchClassAttendanceSummary(token, classId, query)
```

`scope` chỉ là adapter endpoint đã được cấu hình; không được lấy từ role suy
đoán trong JWT.

### 7.2. View và component boundary

Tạo `FE/src/views/AttendanceWorkspaceView.vue` để giữ context, active tab,
session, list/history/summary state, loading và lỗi; không đặt raw `fetch` hoặc
business mapping phức tạp trong table component.

Component dự kiến:

- `AttendanceContextPanel.vue`: năm học, học kỳ, lớp, ngày, buổi và calendar
  validity.
- `AttendanceSessionTable.vue`: row status, selection/action và empty state.
- `AttendanceExceptionDialog.vue`: create/update exception, note validation.
- `AttendanceHistoryPanel.vue`: filter, summary, table, pagination.
- `ClassAttendanceSummaryPanel.vue`: filter, aggregate cards, student table.
- `AttendanceSummaryCards.vue` và `AttendanceStatusTag.vue` nếu boundary test
  cho thấy cần tái sử dụng; ưu tiên reuse `PageState`, `FormAlert`,
  `ServerPagination`, `ConfirmAction`, `StatusTag` hiện có.

### 7.3. Điều hướng và responsive

- Thêm child route `v2-attendance` và navigation item trong
  `AuthenticatedV2ShellView.vue`.
- Không tạo route riêng cho exception dialog/history dialog; đây là state của
  workspace.
- Desktop: context nằm trên bảng; mobile: context xếp một cột, tab có overflow
  ngang, bảng có scroll cục bộ, dialog không làm footer mất khỏi viewport.
- Action icon-only phải có tooltip/accessible label; action chính dùng icon +
  text. Không dùng negative margin cho heading/caption.

## 8. File/khu vực dự kiến thay đổi

| File/khu vực | Mục đích |
|---|---|
| `FE/src/types/attendance.ts` | Wire DTO, enum, query và form state |
| `FE/src/services/attendanceApi.ts` | Typed wrapper cho calendar/session/exception/history/summary |
| `FE/src/services/attendanceApi.spec.ts` | Kiểm tra path, query, body, token và `204` |
| `FE/src/router/index.ts` | Route `/v2/attendance` |
| `FE/src/views/AuthenticatedV2ShellView.vue` | Navigation Điểm danh |
| `FE/src/views/AttendanceWorkspaceView.vue` | Context/tab orchestration và reload sau mutation |
| `FE/src/components/AttendanceContextPanel.vue` | Context + calendar validity |
| `FE/src/components/AttendanceSessionTable.vue` | Bảng session và action exception |
| `FE/src/components/AttendanceExceptionDialog.vue` | Form upsert exception |
| `FE/src/components/AttendanceHistoryPanel.vue` | Student self history |
| `FE/src/components/ClassAttendanceSummaryPanel.vue` | Class summary |
| `FE/src/components/AttendanceSummaryCards.vue`, `AttendanceStatusTag.vue` | Chỉ tạo nếu boundary cần |
| `FE/src/views/AttendanceWorkspaceView.spec.ts` và component specs | Context reset, derived `PRESENT`, errors, pagination |
| `FE/src/components/*.stories.ts` | Fixture deterministic cho tab, dialog, empty/forbidden/error |
| `FE/src/styles.css` | Chỉ CSS responsive/cục bộ cần thiết |
| `document/dev-impl-plan/fe/FE_DEV_PLAN_SUMMARY.md` | Thêm Plan 056 |
| `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md` | Thêm Plan 056 |
| `document/dev-note/fe/attendance/056-...md` | Tạo sau implementation, không thuộc phase lập plan |

Không sửa backend, migration, entity, Postman collection, auth role contract
hoặc các màn hình v1 trong plan này. Nếu office capability hoặc một response
shape cần thay đổi, dừng phần bị ảnh hưởng và đề xuất delta riêng.

## 9. Test và validation đã thực hiện sau approval

### Service tests

- Serialize đúng `classId`, `semesterId`, `attendanceDate`, `sessionPeriod`.
- Serialize status/note đúng wire enum; không gửi `PRESENT`.
- Phân biệt teacher base và office base; không tự chọn office từ token không có
  role claim.
- History/summary serialize date, page và size đúng; parse response envelope.
- Delete xử lý `204 No Content` không cố parse JSON.

### View/component tests

- Đổi context reset session, rows, selection và dialog state.
- Không gọi student list trước khi có `sessionId` hợp lệ.
- Row không có record hiển thị `PRESENT`; upsert/delete cập nhật đúng row.
- Validation `from <= to`, note tối đa 500 ký tự và trường bắt buộc.
- Hiển thị loading/empty/success/`401`/`403`/`404`/`409`/network error đúng
  vùng; warning không biến thành blocking error.
- History và class summary giữ pagination, zero summary và dữ liệu read-only.
- Storybook không cần live backend và có state cho `NO_CLASS`, session trống,
  exception đã ghi, forbidden và empty report.

### Quality gates

Đã chạy đúng scripts hiện có trong `FE/package.json` sau khi nhận approval:

```text
npm run lint
npm run test
npm run test:coverage
npm run build
npm run build-storybook
```

Kết quả: tất cả quality gates `PASS`; full suite có 37 test files và 114 tests
đều pass. Coverage report ghi nhận 90% statements và 74.53% branches. Storybook
build hoàn tất; chỉ có warning dependency/chunk size từ toolchain.

## 10. Rủi ro, assumption và blocker

- **Role/capability chưa expose:** navigation tĩnh là chấp nhận được, nhưng
  service không được suy role từ username/JWT. Teacher path triển khai theo
  contract; office path chỉ hoàn tất khi có capability adapter hoặc chỉ rõ
  cấu hình endpoint đã được phê duyệt.
- **Enum drift:** giữ `ABSENT`/`EXCUSED` của wire hiện tại dù requirement dùng
  wording dài hơn; không normalize im lặng.
- **Authorization theo assignment:** `TEACHER` không mặc nhiên đọc/ghi mọi
  lớp. `403` từ backend được giữ nguyên và hiển thị access denied.
- **Calendar/enrollment boundary:** preflight FE chỉ giúp UX; backend vẫn là
  nguồn quyết định session hợp lệ và mẫu số summary.
- **Hiệu năng report:** class summary có pagination server-side, không tải
  toàn bộ lớp hoặc tạo N+1 request từ FE.
- **Timezone:** ngày điểm danh là date-only; không chuyển qua UTC string.
- **Identity history:** student self history dùng `/me`, không cho người dùng
  nhập student ID để mở dữ liệu khác.

## 11. Output mong đợi

- Một route `/v2/attendance` hợp nhất ba use case trong ảnh.
- GVCN có thể mở session, nhập/sửa/xóa exception và thấy `PRESENT` dẫn xuất.
- Học sinh xem lịch sử của chính mình; GVCN xem summary lớp theo kỳ/khoảng ngày
  khi backend cho phép.
- UI hiển thị đúng wire contract, status, pagination, error và responsive state.
- Có typed service, component boundary, Storybook/spec và validation evidence
  sau khi plan được approve.

## 12. Approval gate

Approval đã được ghi nhận qua agent message `approved`. Production code và test
code chỉ được triển khai sau mốc approval; không thay đổi backend, migration,
Postman collection hoặc auth role contract.
