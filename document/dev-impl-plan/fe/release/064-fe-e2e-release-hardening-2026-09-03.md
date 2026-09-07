# Developer Plan 064 — FE E2E & Release Hardening

- **Trạng thái:** `Draft — chờ user approval qua agent message`
- **Ngày:** `2026-09-03`
- **Application documentation:** `v2`
- **Wireframe bắt buộc:** [064 FE E2E & Release Hardening](../../../wireframes/fe/release/064-fe-e2e-release-hardening/README.md)

## 1. Mục tiêu

Thiết lập và thực hiện release-validation cho các flow FE v2 đã triển khai, chứng minh bằng evidence rằng các role `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`, `STUDENT` nhìn thấy và thao tác đúng phạm vi; các lỗi `401`, `403`, `404`, `409` được xử lý đúng; dữ liệu sau mutation vẫn bền vững; UI đạt responsive/accessibility cơ bản; và các quality gate FE cùng smoke test deploy được ghi nhận trung thực.

Đây là plan validation/release, không phải plan tạo feature UI. Không thêm endpoint, DTO, role rule, E2E framework hoặc dependency mới nếu chưa có approval riêng.

## 2. Căn cứ và contract hiện tại

- Roadmap còn lại: [`FE_REMAINING_PLANS-2026-09-02.md`](../../summary/FE_REMAINING_PLANS-2026-09-02.md).
- Acceptance/security baseline: [`07-AccessQualityAndAcceptanceModule.md`](../../../application-doc/v2/modules/07-AccessQualityAndAcceptanceModule.md), đặc biệt ma trận quyền, `NFR-SECURITY-004/005`, `NFR-RELIABILITY-001/002/003/004` và `NFR-AUDITABILITY-001..009`.
- FE auth boundary: `FE/src/services/authSession.ts`, `FE/src/services/apiClient.ts`, `FE/src/types/user.ts`; session dùng `sessionStorage`, `401` clear session/redirect Login, `403` giữ session và hiển thị access denied.
- FE service/API boundary: `FE/src/services/**`, `FE/src/types/**`, [`FrontendApiGuide.md`](../../../application-doc/v2/FrontendApiGuide.md). Backend vẫn là authority cho authorization; role UI chỉ là capability/navigation hint từ account contract.
- Tooling hiện có trong `FE/package.json`: `npm run lint`, `npm run test`, `npm run test:coverage`, `npm run build`, `npm run build-storybook`.
- Demo seed tham chiếu: [`048-be-demo-data-bootstrap-2026-08-26.md`](../../../dev-note/be/bootstrap/048-be-demo-data-bootstrap-2026-08-26.md). Hiện seed chưa có assessment columns, skill weight `25/35/40` hoặc điểm mẫu; cần fixture/setup được approve trước walkthrough scorebook.

## 3. In-scope

1. Browser/E2E matrix cho các flow đã có: Attendance, academic context, Scorebook configuration/score entry, và các module FE còn lại khi backend contract và UI đã sẵn sàng: Score Change Request, Semester Completeness/Lock, Transcript, Retake, Calculation/Audit.
2. Permission regression cho bốn role; kiểm tra cả visibility/capability hint và response backend.
3. Status/error matrix: unauthenticated/expired `401`, authenticated forbidden `403`, missing resource/context `404`, lifecycle/version/calendar/duplicate conflict `409`.
4. Persistence sau create/update/bulk/retry/lock/reopen/approval/retake khi endpoint tương ứng đã available.
5. Desktop/mobile responsive, keyboard-only navigation, visible focus, labels/ARIA, dialog escape/close, table overflow và warning-vs-blocking semantics.
6. Storybook deterministic state review/build, FE quality scripts, production build và deploy smoke test.
7. Seed/demo data catalog, browser walkthrough evidence, failure log, rerun evidence và final release decision.

## 4. Out-of-scope

- Không sửa production feature code, backend contract, migration, seed data hoặc CI/deploy infrastructure trong Plan 064.
- Không coi hidden button/route guard là authorization proof.
- Không tự đặt coverage threshold hoặc tự biến `NOT RUN` thành `PASS`.
- Không chạy destructive production test; mutation chỉ dùng môi trường demo/staging có dữ liệu reset được.

## 5. Ma trận kiểm thử dự kiến

| Nhóm | ADMIN | ACADEMIC_OFFICE | TEACHER | STUDENT | Evidence tối thiểu |
|---|---|---|---|---|---|
| Read academic context | PASS/FAIL/NOT RUN | PASS/FAIL/NOT RUN | PASS/FAIL/NOT RUN | PASS/FAIL/NOT RUN | URL, role, response, screenshot |
| Attendance history/summary | PASS/FAIL/NOT RUN | PASS/FAIL/NOT RUN | PASS/FAIL/NOT RUN | PASS/FAIL/NOT RUN | scope + empty/success |
| Attendance open/adjust | PASS/FAIL/NOT RUN | PASS/FAIL/NOT RUN | PASS/FAIL/NOT RUN | expected `403`/NOT RUN | request/status + session preserved |
| Scorebook config/entry | PASS/FAIL/NOT RUN | PASS/FAIL/NOT RUN | PASS/FAIL/NOT RUN | expected read-only/`403` | create/edit/bulk/persistence |
| Score change approval | PASS/FAIL/NOT RUN | PASS/FAIL/NOT RUN | expected create/read scope | expected read-only/`403` | request lifecycle + audit |
| Semester lock/completeness | PASS/FAIL/NOT RUN | PASS/FAIL/NOT RUN | expected read-only/`403` | expected limited read | report, lock/reopen, notification |
| Transcript/retake | PASS/FAIL/NOT RUN | PASS/FAIL/NOT RUN | scoped read/write | self read-only | status and before/after values |
| Calculation/audit operations | PASS/FAIL/NOT RUN | PASS/FAIL/NOT RUN | scoped/read-only | expected `403` | failed task, retry, audit |

The matrix is an execution template. Each cell remains `NOT RUN` until a real browser/backend run records evidence.

## 6. Status/error and persistence scenarios

- `401`: expired/missing token clears session and returns to Login; no protected data remains visible.
- `403`: authenticated session remains; access-denied state is visible; no unauthorized mutation is sent.
- `404`: missing class/scorebook/semester/resource shows safe not-found state with recovery action.
- `409`: conflict/version/lifecycle/calendar/duplicate response preserves context and offers reload/retry only where contract supports it; optimistic score conflict must reload authoritative state.
- Mutation persistence: reload the page and re-query the relevant resource; verify no false success, duplicate request, stale status, or lost note/value.
- Background calculation: verify `IN_PROGRESS`/`FINISH`, failed task and retry state; do not infer official averages in FE.

## 7. Accessibility and responsive acceptance

- All interactive controls reachable in logical Tab order with visible focus and no keyboard trap.
- Dialogs expose name/role, return focus to trigger, support Escape where intended, and associate validation text with fields.
- Tables remain usable through horizontal overflow on narrow viewport; no clipped primary action.
- Status badges, warnings and errors do not rely on color alone; text remains understandable at zoom/reflow.
- Test at a desktop viewport and a narrow mobile viewport; record viewport, browser and screenshots.

## 8. Execution and evidence protocol

1. Verify environment, backend URL, clean demo/staging tenant, seeded users for four roles, and reset strategy.
2. Record fixture IDs and non-secret usernames only; never record passwords, tokens, hashes or sensitive payloads.
3. Run each matrix row, capture URL/role/action/request status/visible result/screenshot and classify `PASS`, `FAIL`, `BLOCKED` or `NOT RUN`.
4. Re-run failed cases after a targeted fix; keep first failure and rerun evidence.
5. Run configured FE scripts from `FE/`:

   ```text
   npm run lint
   npm run test
   npm run test:coverage
   npm run build
   npm run build-storybook
   ```

6. Run deploy smoke only against an approved deployed environment: load shell, login, one read flow per supported role, one expected forbidden flow, refresh, and verify asset/API base URL.
7. Publish a release evidence note only after user approval and actual execution; this draft itself does not claim any gate passed.

## 9. Dependencies and blockers known at draft time

| Dependency/blocker | Impact | Resolution/exit condition |
|---|---|---|
| Live browser session and reachable backend/staging | Browser walkthrough cannot be `PASS` without it | Provide approved environment and run with evidence |
| Four seeded roles, linked teacher/student data and scoped assignments | Permission matrix cannot be complete | Seed/reset fixture with `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`, `STUDENT` and documented IDs |
| Scorebook demo fixture absent from current seed | Score entry/bulk/persistence cases may be `BLOCKED` | Approve fixture/setup for scorebook and assessment columns |
| Attendance calendar must contain matching `SCHOOL_DAY`/`SCHEDULED` data | Office open-session can return expected `409` instead of success | Prepare calendar/session fixture and record intentional conflict case |
| Plan 046 transcript contract status is inconsistent in docs | Transcript cases cannot be authoritative | Resolve contract/status before marking Transcript coverage complete |
| Plans 038/053 have noted backend follow-ups | Dependent flows may remain `BLOCKED` | Track separately; do not silently fold backend work into FE release plan |
| No E2E runner is configured in `FE/package.json` | Automated browser E2E is not yet executable by existing scripts | Use approved browser walkthrough/manual evidence or approve tooling plan separately |

## 10. Expected outputs after approval

- Executed evidence matrix and role/status result table.
- Accessibility/responsive review record and screenshots.
- Storybook/build/test/coverage outputs with exact status (`PASS`, `FAIL`, `BLOCKED`, `NOT RUN`).
- Deploy smoke result and known-risk list.
- Release recommendation: `READY`, `READY WITH BLOCKERS`, or `NOT READY`.
- Dev Note under `document/dev-note/fe/release/` after execution; no Dev Note is created by this draft.

## 11. Approval gate

Plan 064 is **not approved** until the user approves this plan and the linked wireframe by agent message. Production implementation, test harness changes, seed changes and release execution remain out of scope until that approval.
