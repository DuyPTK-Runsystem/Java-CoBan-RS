# Plan 076 FE — Targeted Notification v3 UI

## 1. Trạng thái, nguồn và mục tiêu

- Application-document version: **v3**.
- Status: **IMPLEMENTED — validation incomplete; not COMPLETED**.
- Đi cùng [Plan BE](../../be/notification/076-targeted-notification-2026-09-15.md).
- Nguồn: [RequirementBaseline](../../../application-doc/v3/RequirementBaseline.md),
  [Module 03](../../../application-doc/v3/modules/03-NotificationAndAudience.md),
  [FE API boundary](../../../application-doc/v3/frontend-api/README.md),
  [Master Plan](../../summary/MASTER_PLAN_V3-2026-09-09.md).

Mục tiêu là cung cấp inbox notification cho mọi actor hợp lệ và workspace composer/audience
cho role quản lý, dùng typed API, deterministic fixture và Storybook states. FE không tự
suy diễn authorization hoặc membership; FE gửi `schoolScope: DEFAULT_SCHOOL` và để BE validate.

## 2. Ranh giới và quyền dự kiến

### In-scope

- Inbox phân trang, unread/read state, detail và mark-read.
- Composer: title/body, audience selector, preview, draft, publish/cancel.
- Hiển thị audience/scope/lifecycle/expiry theo response backend.
- Recipient view không hiển thị `targetReference`/internal IDs; targeting details chỉ hiển thị cho
  `ADMIN`/`ACADEMIC_OFFICE` sau khi backend authorize.
- Typed service, route guard bước đầu, error recovery và Storybook.
- Integration nối API thật sau khi BE contract ổn định.

### Out-of-scope

- Email/push UI và SMTP retry; không thay đổi `SemesterNotificationPanel` v2.
- Scheduler UI nếu D09 chưa được duyệt.
- FE tự lọc membership, tự tính school scope hoặc tự cấp quyền.
- Claim browser/live PASS từ fixture/jsdom.

| Route candidate | Actor |
|---|---|
| `/notifications` | Người dùng đã đăng nhập: inbox trong scope của chính mình |
| `/notifications/:notificationId` | Người nhận hợp lệ: chi tiết/read state |
| `/notifications/manage` | `ADMIN`, `ACADEMIC_OFFICE`: danh sách quản lý |
| `/notifications/compose` | `ADMIN`, `ACADEMIC_OFFICE` nếu D01 được duyệt |

Route cuối cùng phải khớp router/navigation convention hiện tại và backend role matrix.

## 3. Luồng giao diện

### W01 — Inbox

- Tab `Tất cả`/`Chưa đọc`, page zero-based và page size từ response.
- Mỗi item hiển thị title, thời gian, audience label, trạng thái đọc và expiry nếu có.
- Empty, loading, expired, unread và lỗi scope phải khác nhau; không lộ body ngoài scope.

### W02 — Detail/read

- Mở detail gọi API backend để kiểm tra scope.
- Mark-read idempotent, sau mutation refresh item/unread count; không cập nhật thành công giả.
- `409` giữ state và cho phép tải bản mới; không retry mutation tự động.

### W03 — Composer/manage

- Chọn `INDIVIDUAL`, `CLASS` hoặc `SCHOOL` bằng lookup/contract đã duyệt; school scope cố định là
  `DEFAULT_SCHOOL` cho single-school app.
- Preview rõ người nhận/audience, title/body, channel và thời điểm publish.
- Lưu draft, publish, cancel theo capability backend; không cho sửa field readonly do backend trả.
- Nếu audience hoặc content chưa hợp lệ, giữ input và hiển thị lỗi tiếng Việt cụ thể.

## 4. Typed contract và state

Candidate service functions, chỉ triển khai sau contract approval:

- `fetchNotificationInbox(token, query)`
- `fetchManagedNotifications(token, query)`
- `fetchNotification(token, notificationId)`
- `createNotificationDraft(token, request)`
- `publishNotification(token, notificationId, request)`
- `cancelNotification(token, notificationId, request)`
- `markNotificationRead(token, notificationId, request)`

Types phải phản ánh `ResNotificationDTO`, `ResNotificationReceiptDTO`, audience/status/channel
đã duyệt và `ResultPaginationDTO`. Không tạo type thay thế để FE tự suy diễn capability.

Error/recovery giữ convention hiện tại:

- `401`: `apiClient` xóa session/đưa về login.
- `403`: giữ session, hiển thị không đủ quyền và không dùng cache khác scope.
- `404`: thông báo notification không còn tồn tại hoặc không truy cập được.
- `409`: giữ form/input, tải dữ liệu mới vào vùng so sánh; không ghi đè im lặng.
- `400/422`: giữ input, map lỗi backend an toàn; không gom mọi lỗi thành network error.

## 5. File scope dự kiến

```text
FE/src/types/notification.ts
FE/src/services/notificationApi.ts
FE/src/services/notificationApi.spec.ts
FE/src/fixtures/notificationFixture.ts
FE/src/views/notification/NotificationInboxView.vue
FE/src/views/notification/NotificationDetailView.vue
FE/src/views/notification/NotificationManagementView.vue
FE/src/views/notification/NotificationComposerView.vue
FE/src/components/notification/NotificationList.vue
FE/src/components/notification/NotificationDetail.vue
FE/src/components/notification/NotificationComposer.vue
FE/src/components/notification/NotificationAudienceSelector.vue
FE/src/components/notification/NotificationStatusBadge.vue
FE/src/components/notification/*.stories.ts
```

Router, navigation và tên component có thể gộp/tách theo review UI; không chỉnh các file
academic notification v2 ngoài khi cần đăng ký link tương thích đã được duyệt.

## 6. Test, Storybook và acceptance

- Service tests: query serialization, DTO mapping, pagination, create/publish/cancel/read,
  error mapping và không gửi field transport thừa.
- Component/view tests: inbox unread/read, detail scope, composer audience, empty/loading,
  validation, `401/403/404/409/422`, expiry và mutation refresh.
- Storybook: `InboxUnread`, `InboxEmpty`, `DetailRead`, `ManageDraft`, `AudienceClass`,
  `AudienceSchool`, `PublishConflict`, `Forbidden`, `Expired`, `Loading`.
- FE gates sau implementation: lint, test/coverage, build và Storybook build; kết quả phải báo riêng.
- Integration/browser/live, role isolation, reload/re-query và real BE persistence báo riêng;
  chưa chạy phải ghi `NOT RUN` hoặc `BLOCKED`.

Acceptance: người dùng chỉ thấy notification trong scope, unread/read bền sau reload, composer
không cho chọn audience ngoài quyền, pagination đúng contract và không có success giả khi
mutation lỗi.

## 7. Approval và Dev Note

## 7. Evidence sau implementation

| Gate | Kết quả | Evidence/giới hạn |
|---|---|---|
| FE notification focused tests | **PASS** | `npm test -- --run src/services/notificationApi.spec.ts src/components/notification`: 6 files, 29 tests |
| FE lint/build | **PASS** | `npm run lint && npm run build` |
| FE full test | **PASS** | `npm test -- --run`: 109 test files, 570 tests passed |
| FE coverage | **NOT RUN** | Chưa chạy full coverage gate |
| Storybook build | **NOT RUN** | Chưa chạy Storybook build gate |
| Browser/live integration | **NOT RUN** | Chưa chạy authenticated browser walkthrough |

FE pagination dùng metadata zero-based `page/pageSize/totalPages/totalItems`; service serialize
`pageSize` thành query `size` theo backend boundary. Không đánh dấu Plan 076 là `COMPLETED` khi
backend full test/build và repository PMD còn FAIL; các runtime/migration gate chưa chạy. Luồng notification email
v2 không thuộc scope Plan 076.
