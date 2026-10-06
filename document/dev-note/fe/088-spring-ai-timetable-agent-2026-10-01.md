# Dev Note FE 088 — Trợ lý thời khoá biểu

- Ngày: 2026-10-01.
- [Plan 088](../../dev-impl-plan/summary/088-spring-ai-timetable-agent-2026-10-01.md) được người dùng approve qua chat; nền v3, CR v4. Scope provider hiện offline, chưa cài/chọn provider.
- Dev và test là hai agent độc lập; FE production/stories do agent FE, specs do agent kiểm thử.

## Thực tế triển khai

| Khu vực | Thay đổi |
|---|---|
| `FE/src/types/timetableAgent.ts`, `services/timetableAgentApi.ts` | Typed v4 proposal/approve/execute/receipt/keyed recovery; `preferences` string; receipt `SAVED_DRAFT` + `savedEntryCount`, khớp DTO BE checkpoint |
| `components/timetable/TimetableAgentPanel.vue` | Chọn lớp/ngày, tải phân công từ view, số tiết không mặc định, xác nhận demand, locked entries và ưu tiên |
| `components/timetable/TimetableAgentReview.vue` | Grid 2 buổi × 4 tiết, diff/giữ nguyên, warnings/blocking, duyệt/lưu riêng, server receipt/recovery |
| `views/timetable/TimetableAgentWorkspace.vue` | Orchestration typed service; exact approval binding, expiry/version guards, pending key lưu trước execute, khôi phục theo actor/revision sau remount |
| `views/timetable/TimetableWorkspaceView.vue`, `types/timetable.ts` | Entry theo top-level backend `canUseTimetableAgent`; default absent/false không mở agent; receipt reload dữ liệu lịch |
| `components/timetable/TimetableAgent*.stories.ts`, `timetableAgent.fixtures.ts` | 20 states phục vụ review offline, không gọi backend/model |

## Quyết định

- Contract checkpoint trực tiếp với BE: response status `NEEDS_INPUT/NO_SOLUTION_FOUND/CONFLICTS/READY_FOR_REVIEW/APPROVED/SAVED/STALE/EXPIRED`; issue `code/severity/path/message`; diff added schema entries, removed/unchanged có entryId. Không thêm endpoint capability.
- Approval local gắn proposalId/version/hash/snapshotId/targetRevisionId/expectedVersion; response approval thay binding bị reject. Sửa input hoặc revision/permission thay đổi hủy binding; response cũ của request đã bị invalidation bị bỏ.
- UI không render HTML từ model; không dựa explanation/status SAVED để kết luận lưu. Chỉ receipt `SAVED_DRAFT` có matching proposal/revision/action/version được hiển thị thành công.
- Execute giữ pending reference và Idempotency-Key trong sessionStorage theo actor/revision trước HTTP. Timeout/network/invalid receipt giữ reference; recovery lookup theo key, không tạo key mới. HTTP 400/401/403/404/409/422 definitive clear pending và yêu cầu sửa/duyệt lại khi cần.
- Action-state contract BE/FE chốt: 202 `PENDING` với leaseExpiresAt, 200 `FAILED` với retryable, 200 `SAVED_DRAFT` receipt. PENDING/FAILED không phải thành công. Sau lookup 404, FAILED retryable hoặc lease PENDING hết hạn, người dùng có thể thử lại **cùng key/proposal/version**; server kiểm action record trước model call. Không tự retry, không sinh key mới.
- Baseline snapshot-only; không tools đọc bổ sung, keep-warm timer hay provider selection. Timer FE chỉ cập nhật expiry UI, không gọi model.
- BE hiện trả `canUseTimetableAgent=false`; production entry chưa promote cho tới provider/concurrency/validation gates. Không đổi manual publish workflow.

## Validation

| Gate | Kết quả |
|---|---|
| `npm.cmd run lint` | PASS sau sửa source formatting và test stubs; lần đầu FAIL được sửa đúng scope, không suppress rule |
| `npm.cmd run build` | PASS — vue-tsc và Vite production build |
| `npm.cmd run build-storybook` | PASS — final 20 fixture stories sau action-state amendment; cảnh báo Storybook/vendor eval/chunk-size được giữ nguyên |
| Focused API/panel/review/workspace specs | PASS — independent tester; retry/recovery workspace focused 9/9, scoped ESLint specs PASS |
| `npm.cmd run test` | PASS — independent tester, 120 files / 652 tests sau test locked-scope containment; trước đó 651/649 tests |
| `npm.cmd run test:coverage` | PASS lần chạy lại — 120 files / 652 tests, global statements/lines 86.70%, branches 75.76%, functions 71.83%. Agent statements/branches: Panel 96.25%/85.45%, Review 100%/72.72%, API 100%/100%, Workspace 85.32%/72.80%; types-only runtime 0% không phải runtime behavior. Lần trước assertions 652 PASS nhưng command FAIL vì unhandled `history undefined` ở EnrollmentListView router; lỗi không lặp lại ở lần chạy lại, giữ lịch sử và không suy ra đã sửa một lỗi production |
| Chrome fixture Storybook review | PASS — Ready grid/diff/held/new render; Warnings visible; response-lost recover enabled/save disabled; Saved reload; RetryPriorSave explicit same-key retry enabled; PrimeVue form select lớp/fill demand/confirm enable, edit ưu tiên invalidates, Disabled generate disabled |
| Browser API/provider/MySQL save/reload integration | NOT RUN — offline/provider chưa cài, BE save chưa promote; fixture browser review không thay bằng chứng này |

Review URL: [Storybook Ready](http://127.0.0.1:6006/?path=/story/timetable-agent-review--ready). Server local phục vụ checkpoint trong phiên, không deployment. Chrome tab review được giữ làm deliverable; screenshot render đã hiển thị trong tool output. IAB unavailable, dùng browser Chrome đã kết nối.

## Deviation và gate còn lại

- Tách child view `TimetableAgentWorkspace.vue` để orchestration/API/recovery không làm workspace editor lớn thêm; nằm trong FE slice đã duyệt.
- Storybook/contract fixture gate đã thực hiện, production capability giữ false. Chưa có runtime provider, integration backend thật, atomic save hoặc concurrency proof.
- Recovery lookup 404 giữ pending và không tự gửi mutation; explicit retry cùng key đã ACK trực tiếp với BE. Gate runtime server lease/idempotency vẫn cần DB integration evidence.
- Shared summary links do coordinator cập nhật để tránh xung đột agent BE/FE.
