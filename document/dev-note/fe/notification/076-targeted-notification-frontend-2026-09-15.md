# Dev Note — Plan 076 Frontend: Targeted Notification v3 UI

## 1. Thông tin và approval

- Kế hoạch liên quan: [Plan 076 FE](../../../dev-impl-plan/fe/notification/076-targeted-notification-ui-2026-09-15.md).
- Application-document version: **v3**.
- Approval: implementation slice đã được user yêu cầu sửa/đối chiếu theo backend contract; privacy
  option A đã được user chốt.
- Trạng thái: **IMPLEMENTED SLICE — validation incomplete; không phải COMPLETED**.

## 2. Phạm vi thực tế

- Views/components: Inbox, Detail, Manage, Composer, audience selector, status badge và fixtures.
- Audience UI: `INDIVIDUAL`, `CLASS`, `SCHOOL`; Composer gửi `schoolScope: DEFAULT_SCHOOL`.
- API service typed qua `apiClient`; `IN_APP` là channel duy nhất, không gửi field transport thừa.
- Pagination: FE dùng metadata zero-based `page/pageSize/totalPages/totalItems`; serialize `pageSize`
  thành query `size` theo backend boundary.
- Error handling giữ input khi `400/422/409`, tách `401/403`, không tự retry mutation.
- Publish UI là immediate publish; không có scheduler UI.
- Privacy option A: recipient view không hiển thị `targetReference` hoặc internal IDs; targeting
  details chỉ hiển thị cho `ADMIN`/`ACADEMIC_OFFICE` sau backend authorization.
- FE nullable privacy contract: internal identity/targeting fields phản ánh nullable/optional theo
  JSON response của recipient.
- Composer giữ stable `idempotencyKey` khi tạo draft để retry cùng một lần submit không tạo draft trùng.
- Không chỉnh notification email v2.

## 3. Files changed trong slice FE

- `FE/src/types/notification.ts`
- `FE/src/services/notificationApi.ts`
- `FE/src/services/notificationApi.spec.ts`
- `FE/src/fixtures/notificationFixture.ts`
- `FE/src/components/notification/`
- `FE/src/views/notification/`
- `FE/src/router/index.ts`
- `FE/src/views/shell/AuthenticatedV2ShellView.vue`
- `FE/src/services/studentNavigation.ts`
- `FE/src/views/shell/AuthenticatedV2ShellView.spec.ts`
- `FE/src/router/index.spec.ts`

### Amendment 076.1 — Chuẩn hóa giao diện notification

- Các route view notification dùng layout chung `page-heading`, `page-heading-actions`,
  `content-surface`, `section-heading`, `FormAlert` và `PageState` như các tab học vụ hiện hành.
- `NotificationList`, `NotificationDetail`, `NotificationComposer` và
  `NotificationAudienceSelector` dùng class semantic/scoped CSS, có empty/loading/error state và
  responsive behavior; không thay đổi API, route, authorization, privacy hay pagination.

## 4. Validation Result

| Gate | Kết quả | Evidence/giới hạn |
|---|---|---|
| Notification focused tests | **PASS** | `npm test -- --run src/services/notificationApi.spec.ts src/components/notification`; 6 files, 29 tests passed |
| Notification navigation focused tests | **PASS** | 2 files, 83 tests passed; covers shell tab, active state và STUDENT route access |
| Notification UI focused tests | **PASS** | Cùng focused run: 8 files, 113 tests passed |
| FE lint | **PASS** | `npm run lint` |
| FE build | **PASS** | `npm run build` |
| FE full test | **FAIL** | Latest `npm test -- --run`: 109 files, 574 tests passed nhưng 1 unhandled `ReferenceError: history is not defined` từ `src/views/enrollment/EnrollmentListView.spec.ts`; lỗi ngoài scope patch |
| FE coverage | **NOT RUN** | Chưa chạy full coverage gate |
| Storybook build | **NOT RUN** | Không dùng fixture/story để suy ra runtime integration |
| Browser/live smoke | **PASS (inbox)** | Chrome authenticated `academic.office` hiển thị sidebar, page heading, content surface và empty state notification đồng nhất với tab “Năm học”; console error/warn = 0. Các route manage/detail/compose chưa walkthrough lại sau khi phiên hết hạn; real BE persistence chưa kiểm tra |

## 5. Deviations, blockers và next steps

- Pagination được sửa theo `ResultPaginationDTO` zero-based thay vì metadata cũ `pages/total`.
- Scope single-school là hằng số `DEFAULT_SCHOOL` do FE gửi; BE mới là boundary validate.
- Bổ sung điều hướng “Thông báo” trong authenticated shell và cho phép STUDENT mở inbox theo Plan 076; không thay đổi backend hoặc notification service/view.
- Amendment 076.1 chuẩn hóa notification views/components theo layout foundation; bỏ phụ thuộc utility-only ngoài scope và giữ nguyên hành vi nghiệp vụ.
- Full FE test hiện có unhandled error `history is not defined` trong test Enrollment ngoài scope; focused navigation, lint, build và browser smoke vẫn PASS.
- Không đánh dấu Plan 076 `COMPLETED` vì backend full test/build và repository PMD còn FAIL; các gate runtime/migration
  chưa chạy.
