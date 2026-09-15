# Dev Note 079 — FE sổ đầu bài

## Plan và approval

- Plan: [`document/dev-impl-plan/fe/lesson-log/079-lesson-log-ui-2026-09-14.md`](../../../dev-impl-plan/fe/lesson-log/079-lesson-log-ui-2026-09-14.md).
- Plan BE đi cùng: [`document/dev-impl-plan/be/lesson-log/079-lesson-log-lifecycle-and-audit-2026-09-14.md`](../../../dev-impl-plan/be/lesson-log/079-lesson-log-lifecycle-and-audit-2026-09-14.md).
- D01–D10 và implementation BE/FE đã được người dùng approve ngày 2026-09-14. Trạng thái code: **IMPLEMENTED SLICE; FE AUTOMATED GATES PASS; BROWSER/LIVE NOT RUN**.

## Phạm vi thực tế đã triển khai

- `FE/src/types/lessonLog.ts`, `FE/src/services/lessonLogApi.ts` và fixture định nghĩa contract FE cho schedule, class-week, entry, policy, weekly review, audit pagination và capabilities.
- `FE/src/views/lesson-log/TeacherLessonLogView.vue` cung cấp lịch cá nhân, occurrence `UNLOGGED`, tạo/lưu nháp, submit hai bước và giữ draft khi submit lỗi.
- `FE/src/views/lesson-log/ClassLessonLogWorkspaceView.vue` và `LessonLogWeeklyMatrix.vue` cung cấp lookup lớp, tuần thứ Hai–Chủ nhật, ma trận 2 buổi × 4 tiết, calendar/summary từ BE, filter không làm thay đổi tập ký, review/amend/audit và trạng thái `STALE`.
- `LessonLogEntryDialog.vue`, `LessonLogDetailDialog.vue`, `LessonLogAmendDialog.vue`, `WeeklyHomeroomReviewDialog.vue`, `LessonLogAuditDrawer.vue` và `LessonLogStatusBadge.vue` bao phủ form, readonly/capability, lý do điều chỉnh, lỗi 409/422 và lịch sử.
- `LessonLogSettingsView.vue` cho policy version, hai mode deadline, timezone, effective date, rubric và lý do; request amend/policy dùng optimistic version theo contract BE.
- Không tạo input `actualTeacherId`, tiết tự do, dạy thay ngoài TKB hoặc đồng bộ attendance/scorebook. FE dùng `apiClient` cho session/error boundary hiện có.

## Files đã thay đổi

- `FE/src/types/lessonLog.ts`, `FE/src/services/lessonLogApi.ts`, `FE/src/fixtures/lessonLogFixture.ts`.
- `FE/src/views/lesson-log/{TeacherLessonLogView.vue,ClassLessonLogWorkspaceView.vue,LessonLogSettingsView.vue}`.
- `FE/src/components/lesson-log/` gồm entry/detail/amend/review/matrix/status/audit components và stories/specs liên quan.
- `FE/src/services/lessonLogApi.spec.ts` và các test component lesson-log.

## Đồng bộ contract runtime

- Giữ prefix `/api/v3/lesson-logs`, response occurrence chưa ghi và lookup lớp `{id,name}`.
- Map calendar/summary tuần, `blockedReason`, `signedSnapshot`, audit `actorId/beforeStateJson/afterStateJson` và `ResultPaginationDTO` zero-based.
- Payload amend chỉ gửi các field BE nhận và `expectedVersion`; không tự retry mutation. 409 giữ input để so sánh; 422 giữ input và hiển thị capability/message mới.
- Coverage tổng FE không được dùng để khẳng định coverage riêng lesson-log vì module chưa nằm trong include hiện tại của `vite.config.ts`.

## Validation Result

| Gate | Lệnh/phạm vi | Kết quả | Giới hạn |
|---|---|---|---|
| Lint | `npm run lint` | **PASS** | Static gate. |
| Test | `npm run test` | **PASS — 103 files, 541/541 tests** | Không thay thế browser/live API. |
| Coverage | `npm run test:coverage` | **PASS** — statements/lines 85.21%, branches 73.29%, functions 69.23% | Coverage toàn FE theo include hiện tại; lesson-log chưa nằm trong include. |
| Production build | `npm run build` | **PASS** | Không xác nhận backend runtime. |
| Storybook build | `npm run build-storybook` | **PASS** | Có warning eval/chunk size và PrimeVue package.json nhưng build hoàn tất. |
| Browser/live API/E2E | Role, deadline, conflict, weekly flow thật | **NOT RUN** | Chưa có bằng chứng backend runtime/session. |

## Deviations, blockers và next steps

- FE triển khai theo contract thực tế của BE sau khi code được approve; response tuần bounded được adapter từ calendar/items thay vì giả định thêm endpoint.
- Storybook/stories và automated tests xác nhận state deterministic, chưa xác nhận kết nối API thật, pixel layout hoặc quyền server-side.
- Cần chạy browser/live API với role GVBM/GVCN/manager, deadline, conflict, weekly sign/re-sign; đối chiếu với MySQL/Flyway sau khi backend runtime sẵn sàng.
