# Dev Note 056: Attendance Workspace UI

## Liên kết và approval

- Developer Plan: [`document/dev-impl-plan/fe/attendance/056-attendance-workspace-ui-2026-08-28.md`](../../../dev-impl-plan/fe/attendance/056-attendance-workspace-ui-2026-08-28.md)
- Approval: Đã nhận phê duyệt qua agent message: `approved`.
- Application version: `v2`.
- Implementation status: `COMPLETED`.

## Phạm vi đã hoàn thành

- Hợp nhất ba use case `Attendance Session UI`, `Attendance Exception Entry UI`
  và `Attendance History & Summary UI` vào route authenticated `/v2/attendance`.
- Thêm context năm học, học kỳ, lớp, ngày, buổi; calendar preflight;
  create-or-get session và roster theo teacher scope.
- Hiển thị `PRESENT` dẫn xuất cho học sinh không có exception; hỗ trợ tạo, sửa,
  xóa `ABSENT`, `EXCUSED`, `LATE`, `EARLY_LEAVE` và reload roster sau mutation.
- Thêm tab lịch sử self-service `/me/history` và báo cáo tổng hợp theo lớp có
  filter, summary, bảng read-only, empty state và server pagination.
- Xử lý loading, empty, validation, `401`, `403`, lỗi API/network và trạng thái
  read-only khi lớp/học kỳ đóng; giữ date-only `yyyy-MM-dd`.
- Bổ sung Storybook fixture tĩnh cho context, session table, exception dialog,
  history và class summary.

## Files thay đổi

### FE module implementation

- Types/services: `FE/src/types/attendance.ts`,
  `FE/src/services/attendanceApi.ts`.
- View/routing: `FE/src/views/AttendanceWorkspaceView.vue`,
  `FE/src/router/index.ts`, `FE/src/views/AuthenticatedV2ShellView.vue`.
- Components: `AttendanceContextPanel.vue`, `AttendanceSessionTable.vue`,
  `AttendanceExceptionDialog.vue`, `AttendanceHistoryPanel.vue`,
  `ClassAttendanceSummaryPanel.vue`, `AttendanceSummaryCards.vue`.
- Styling: `FE/src/styles.css` cho tab, context, table overflow và responsive
  attendance layout.

### Tests và Storybook

- `FE/src/services/attendanceApi.spec.ts`.
- `FE/src/views/AttendanceWorkspaceView.spec.ts` và cập nhật
  `FE/src/router/index.spec.ts`.
- `AttendanceContextPanel.stories.ts`, `AttendanceSessionTable.stories.ts`,
  `AttendanceExceptionDialog.stories.ts`, `AttendanceHistoryPanel.stories.ts`,
  `ClassAttendanceSummaryPanel.stories.ts`.

### Project records

- Cập nhật Plan 056 và hai plan summaries.
- Tạo Dev Note này và cập nhật hai Dev Note summaries.
- Không thay đổi backend, migration, Postman collection hoặc auth role contract.

## Quyết định triển khai

- Dùng `apiClient`/Bearer token và typed service; teacher scope được truyền
  tường minh ở view cho các lời gọi session/exception. Office base path vẫn có
  trong service adapter nhưng chưa được nối vào UI vì auth hiện tại chưa expose
  capability contract.
- Chỉ dùng `studentId` cho mutation khi roster có technical id; không gửi
  `PRESENT` lên backend. `DELETE` xử lý `204 No Content` mà không parse JSON.
- View sở hữu API calls, context reset, reload và error/session handling;
  component chỉ nhận typed props/emits. History luôn gọi endpoint `/me`.
- Reuse các primitive hiện có (`FormAlert`, `StatusTag`, `ServerPagination`,
  PrimeVue controls) và giữ bảng scroll cục bộ trên mobile.

## Validation

| Command | Result |
|---|---|
| `cd FE && npm run lint` | **PASS** |
| `cd FE && npm run test -- --run src/services/attendanceApi.spec.ts src/views/AttendanceWorkspaceView.spec.ts` | **PASS** — 2 files, 7 tests |
| `cd FE && npm run test` | **PASS** — 37 test files, 114 tests |
| `cd FE && npm run test:coverage` | **PASS** — 90% statements, 74.53% branches (aggregate report) |
| `cd FE && npm run build` | **PASS** |
| `cd FE && npm run build-storybook` | **PASS** |
| `git diff --check` | **PASS** |

Storybook build có warning đã biết từ toolchain (`primevue` package metadata,
`eval` trong runtime Storybook và chunk lớn), nhưng build hoàn tất thành công.

## Deviations và rủi ro còn lại

- Plan dự kiến component spec riêng cho mọi component; implementation hiện có
  service/view coverage và Storybook deterministic, còn một số component được
  kiểm tra gián tiếp qua suite/build.
- Office adjustment endpoint chưa được expose trong UI do thiếu capability
  adapter được phê duyệt; không giả lập teacher token cho office flow.
- Calendar preflight chỉ là hỗ trợ UX; authorization, enrollment, session
  validity và aggregation vẫn do backend quyết định. Chưa chạy walkthrough
  trình duyệt với backend live trong checkpoint này.

## Next steps

- Chạy manual browser walkthrough với backend đã cấu hình, tập trung vào `403`,
  `409`, ngày `NO_CLASS`, session đóng và office capability khi contract sẵn sàng.
- Khi auth expose capability contract, bổ sung UI entry point cho office scope
  cùng test endpoint adapter tương ứng.
