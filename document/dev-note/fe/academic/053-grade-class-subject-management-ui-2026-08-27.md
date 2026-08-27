# Dev Note 053: Grade, Class, Subject & Class-Subject UI

## Liên kết và approval

- Developer Plan: [`document/dev-impl-plan/fe/academic/053-grade-class-subject-management-ui-2026-08-27.md`](../../../../dev-impl-plan/fe/academic/053-grade-class-subject-management-ui-2026-08-27.md)
- Approval: user approved Plan 053 phases `0–1–2–3`, then phases `4–5–6` via agent on 2026-08-27.
- Delivered checkpoint: phases `4–5` integrated with the current v2 API; Phase `6` audited and blocked by missing backend read contract.

## Phạm vi đã hoàn thành

- Phase 0: recorded approval and preserved the wireframe/contract-gap decisions.
- Phase 1: grade and school-class tables/dialogs, lifecycle status actions,
  read-only closed state, loading/empty states and capacity warning placeholder.
- Phase 2: subject table/dialog and applicability create dialog for
  `ACADEMIC`/`SKILL` and `GRADE`/`CLASS`, including create-only contract warning
  and conflict state.
- Phase 3: class-subject table/dialog with year/class/semester context,
  `ACTIVE`/`INACTIVE`/`COMPLETED`, closed read-only state, missing subject
  fallback and applicability conflict action.
- Phase 4: typed Grade/Class API methods, production views, v2 routes, shell
  navigation, lifecycle confirmations and context/filter handling.
- Phase 5: typed Subject, applicability create-only and Class-Subject API
  methods, production views, year/semester/class context, conflict handling and
  status lifecycle actions.
- Phase 6: backend contract audit completed; no FE statistics implementation
  was added because the class list does not return count/average/warning data.

## Files changed

- `FE/src/types/academic.ts`: added typed catalog and form models used by the
  presentation components.
- `FE/src/services/academicApi.ts` and `academicApi.spec.ts`: added typed catalog
  API methods and endpoint/query/body coverage.
- `FE/src/views/GradeListView.vue`, `SchoolClassListView.vue`,
  `SubjectListView.vue`, `ClassSubjectListView.vue`: added route-level
  orchestration and API-backed state flows.
- `FE/src/router/index.ts`, `AuthenticatedV2ShellView.vue` and
  `FE/src/router/index.spec.ts`: added catalog routes, navigation and route
  coverage.
- `FE/src/styles.css`: added responsive catalog table, form, context and status
  layout classes.
- `FE/src/components/GradeTable.vue`, `GradeDialog.vue` and stories.
- `FE/src/components/SchoolClassTable.vue`, `SchoolClassDialog.vue` and stories.
- `FE/src/components/CapacityWarningBanner.vue` and story.
- `FE/src/components/SubjectTable.vue`, `SubjectDialog.vue`,
  `SubjectApplicabilityDialog.vue` and stories.
- `FE/src/components/ClassSubjectTable.vue`, `ClassSubjectDialog.vue` and
  stories.
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md` and
  `document/dev-impl-plan/fe/FE_DEV_PLAN_SUMMARY.md`: registered Plan 053
  approval/progress.
- `document/dev-impl-plan/fe/academic/053-grade-class-subject-management-ui-2026-08-27.md`:
  recorded approval and phase progress.

## Quyết định triển khai

- Reused PrimeVue `DataTable`, `Dialog`, `Select`, `InputNumber`, `Checkbox`,
  `Textarea`, `Button`, `Tag` through the existing `StatusTag` component.
- Kept API calls, routes and view orchestration out of this checkpoint. Stories
  use deterministic fixtures and do not require a live backend.
- Displayed capacity/student statistics as unavailable because the current
  contract does not provide list summary data; no N+1 lookup or FE calculation
  was introduced.
- Kept applicability create-only and exposed its missing read-back API as an
  explicit informational state.
- Kept `CLOSED` and `COMPLETED` records read-only where the phase UI requires
  historical protection, while status changes remain event-driven via emits.
- Created root [`HANDOFF.md`](../../../../HANDOFF.md) with crash-recovery
  instructions, phase status and checkpoint log; it is updated during the
  implementation workflow.
- Aligned Grade dialog validation with backend limits and kept new
  class-subject records `ACTIVE`; status changes use the update endpoint.
- Applied the approved UI wording refinement: hid `displayOrder` in
  `GradeDialog`; renamed class columns to `Sĩ số` and `Ghi chú`, with notes after
  status; mapped `PLANNED` to `Đã khởi tạo`; and changed Subject labels to
  `CHÍNH KHÓA`/`KỸ NĂNG`, `Đang giảng dạy`/`Tạm ngưng giảng dạy`, and
  `Theo khối`/`Theo lớp`.
- Refined class-subject presentation so status and subject-type explanations
  are inside Vietnamese tags without adjacent duplicate text; the dialog shows
  subject scope as `Theo khối` or `Theo lớp`, Vietnamese status options, and no
  raw `ACTIVE` status value in visible dialog copy.

## Validation

| Command | Result |
|---|---|
| `cd FE && npm run lint` | PASS |
| `cd FE && npm run test` | PASS — 25 files, 87 tests |
| `cd FE && npm run test:coverage` | PASS — 25 files, 87 tests, 89.67% statements overall |
| `cd FE && npm run build` | PASS |
| `cd FE && npm run build-storybook` | PASS — Storybook bundle generated |
| `git diff --check` | PASS |

## Deviations, blockers and next steps

- Deviation: phases `4–5` were implemented in the same approved continuation
  rather than separate turns, consistent with the user's explicit approval of
  all remaining phases.
- Existing `BE/BaiTap-RS/src/main/resources/application.properties` change was
  preserved and not part of this task.
- Storybook build emits existing dependency/chunk warnings (`primevue` package
  metadata, Storybook runtime `eval`, large chunks); these did not fail the
  build.
- Phase 6 remains blocked: `ResCapacityWarningDTO` is only returned by
  enrollment mutation responses, while `GET /api/v2/classes` returns no active
  student count, grade average or warning payload. No N+1 lookup or frontend
  calculation was introduced.
- Applicability remains create-only because the backend exposes no list/update/
  deactivate endpoint; full read-back needs a separate approved contract.
- Storybook build emits existing package/chunk/eval warnings; the build still
  passed. Runtime browser review was not automated in this environment.
