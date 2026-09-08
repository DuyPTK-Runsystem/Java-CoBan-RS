# Dev Note 072: FE Source Refactor

## Related plan and approval

- Related plan: `document/dev-impl-plan/fe/tooling/072-frontend-source-refactor-2026-09-08.md`
- Application document: `v2`
- Approval: user approved the plan before implementation.

## Scope completed

- Added `useAuthSession` as the shared view boundary for session, roles and
  missing-token redirect; migrated the academic, student, attendance,
  scorebook, enrollment, teacher and teaching-assignment views in scope.
- Added `dateFormat.ts` as the shared API date/date-time parser and formatter;
  retained domain wrappers and their existing fallbacks.
- Replaced stale local `messageFor` references with the existing typed
  `extractApiErrorMessage` boundary in academic catalog views.
- Split `StudentDetailView.vue` presentation into four typed panels:
  profile/account, enrollment/transfer, attendance history and
  transcript/calculation. Fetching, route state, watchers, authorization and
  recalculation remain owned by the view.
- Added date boundary tests and included the new Student panels in coverage.
- Added the calculation operations entry to the authenticated shell for the
  existing Admin/Academic Office policy.
- Split Attendance workspace orchestration into `useAttendanceSession` and
  `useAttendanceReports`; the view retains only context/calendar loading,
  tab state and the watchers that coordinate the existing presentation panels.
- Split Scorebook dialog state/transitions, Calculation retry/detail flow and
  Retake dialog state into typed composables while retaining API mutations in
  their views.
- Split Transcript tab state and moved the complete Attendance domain CSS block
  from `styles.css` into `styles/attendance.css` without changing selectors or
  visual values.
- Moved scoped Transcript, Scorebook, Calculation Operations and Retake Results
  styles into their domain stylesheet files without changing selectors or
  responsive breakpoints.
- Extracted Transcript academic-year/semester context loading and query-based
  selection into `useTranscriptContext`; the view retains transcript loading,
  route navigation and tab orchestration.
- Migrated Scorebook workspace error rendering from its local error helper to
  the shared typed `extractApiErrorMessage` boundary.
- Moved global base, auth, application-shell, shared form/table and state CSS
  from `styles.css` into `styles/foundation.css`, retaining the import order.
- Moved academic catalog and enrollment-specific CSS into
  `styles/academic-enrollment.css`, while leaving the existing responsive
  overrides at the entrypoint to preserve their source order.
- Moved those responsive overrides into `styles/responsive.css`; `styles.css`
  is now the CSS entrypoint only and preserves import/cascade order.
- Reorganized `FE/src/components` from 180 flat files into domain subfolders:
  `academic/`, `attendance/`, `auth/`, `calculation/`, `common/`, `enrollment/`,
  `retake/`, `scorebook/`, `score-change/`, `student/`, `teacher/`, and `transcript/`.
- Reorganized `FE/src/views` from 40 flat files into domain subfolders:
  `academic/`, `attendance/`, `auth/`, `calculation/`, `enrollment/`,
  `retake/`, `scorebook/`, `shell/`, `student/`, `teacher/`, and `transcript/`.
- Updated all cross-component, view, composable, test and router import paths
  to use the new domain paths with explicit imports.
- Updated `FE/vite.config.ts` coverage patterns (`src/components/**/...` and
  `src/views/**/...`) to seamlessly match the domain structure while maintaining
  exact coverage scope.

## Files changed by purpose

### Shared foundation

- `FE/src/composables/useAuthSession.ts`
- `FE/src/composables/useAuthSession.spec.ts`
- `FE/src/utils/dateFormat.ts`
- `FE/src/utils/dateFormat.spec.ts`
- `FE/src/utils/academicDate.ts`
- `FE/src/utils/calculationTaskDate.ts`
- `FE/src/utils/scoreChangeRequestDate.ts`
- `FE/src/utils/studentDate.ts`
- `FE/vite.config.ts`
- `FE/src/composables/useAttendanceSession.ts`
- `FE/src/composables/useAttendanceReports.ts`
- `FE/src/composables/useScorebookDialogs.ts`
- `FE/src/composables/useCalculationRetry.ts`
- `FE/src/composables/useRetakeDialogState.ts`
- `FE/src/composables/useTranscriptTabState.ts`
- `FE/src/composables/useTranscriptContext.ts`
- `FE/src/styles/attendance.css`
- `FE/src/styles/transcript.css`
- `FE/src/styles/scorebook.css`
- `FE/src/styles/calculation-operations.css`
- `FE/src/styles/retake-results.css`
- `FE/src/styles/foundation.css`
- `FE/src/styles/academic-enrollment.css`
- `FE/src/styles/responsive.css`
- `FE/src/styles.css`

### Student presentation slice

- `FE/src/components/StudentProfilePanel.vue`
- `FE/src/components/StudentEnrollmentPanel.vue`
- `FE/src/components/StudentAttendanceHistoryPanel.vue`
- `FE/src/components/StudentTranscriptPanel.vue`
- `FE/src/views/StudentDetailView.vue`
- `FE/src/views/StudentDetailView.spec.ts` behavior remains covered by the
  existing 9-case view contract.

### View migrations and contract fixtures

- `FE/src/views/AcademicYearListView.vue`
- `FE/src/views/SemesterListView.vue`
- `FE/src/views/GradeListView.vue`
- `FE/src/views/SchoolClassListView.vue`
- `FE/src/views/SubjectListView.vue`
- `FE/src/views/ClassSubjectListView.vue`
- `FE/src/views/StudentListView.vue`
- `FE/src/views/StudentFormView.vue`
- `FE/src/views/EnrollmentListView.vue`
- `FE/src/views/TeacherListView.vue`
- `FE/src/views/TeachingAssignmentView.vue`
- `FE/src/views/AttendanceWorkspaceView.vue`
- `FE/src/views/ScorebookWorkspaceView.vue`
- `FE/src/views/AuthenticatedV2ShellView.vue`
- `FE/src/services/apiClient.spec.ts`
- `FE/src/views/SemesterListView.spec.ts`

### Documentation

- `document/dev-impl-plan/fe/tooling/072-frontend-source-refactor-2026-09-08.md`
- `document/dev-impl-plan/fe/FE_DEV_PLAN_SUMMARY.md`
- `document/dev-note/fe/tooling/072-frontend-source-refactor-2026-09-08.md`
- `document/dev-note/fe/FE_DEV_NOTE_SUMMARY.md`
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`

## Implementation decisions

- Backend remains authoritative for authorization, validation, lifecycle and
  calculation. No endpoint, DTO field, enum, role claim or business rule was
  added.
- `401` still clears the session and redirects; `403` still preserves the
  session and renders access denial through the existing API/error boundary.
- Student panels use props down/events up and do not call APIs or manipulate
  route state.
- Attendance session create/read/mutation and report/history pagination retain
  their existing API calls, `401`/`403` handling, date-range validation and
  exception-as-non-PRESENT semantics; only their ownership moved to composables.
- Date parsing validates calendar and clock ranges while preserving received
  local components; no `toISOString()` or timezone conversion was introduced.
- Existing CSS classes and visual values were retained. Panel-local styles
  were moved with the extracted markup; the full `styles.css` responsibility
  split remains a follow-up.
- Scorebook now follows the shared API-error normalization used by the catalog
  views; its 401/403/404/409 branches and mutation sequencing are unchanged.

## Validation

| Command | Result | Evidence |
| --- | --- | --- |
| `npm run lint` | PASS | ESLint completed with exit code 0 and zero warnings/errors |
| `npm run test` | PASS | 87 files, 469 tests passed |
| `npm run test:coverage` | PASS | 87 files, 469 tests passed; 85.70% statements, 74.79% branches |
| `npm run build` | PASS | `vue-tsc --noEmit` and Vite production build completed |
| `npm run build-storybook` | PASS | Storybook preview built; existing PrimeVue/chunk warnings remain non-fatal |
| `git diff --check` | PASS | no whitespace errors |
| focused Student/date/teaching tests | PASS | 7 files, 29 tests passed |
| focused Attendance service/view tests | PASS | 2 files, 12 tests passed |
| focused Scorebook test | PASS | 1 file, 11 tests passed |
| focused Calculation test | PASS | 1 file, 5 tests passed |
| focused Retake test | PASS | 1 file, 20 tests passed |
| focused high-risk workspace tests | PASS | Scorebook, Calculation, Retake and Transcript: 4 files, 45 tests passed after stylesheet extraction |
| focused Transcript context test | PASS | 1 file, 9 tests passed after context composable extraction |
| browser Scorebook runtime | PASS | authenticated `ACADEMIC_OFFICE` session loaded status, context and 6 score rows; no console error; desktop and 390px viewport had no page overflow |
| browser Attendance runtime | PASS | authenticated session loaded session context and 4 student rows; no console error; desktop and 390px viewport had no page overflow |
| browser Calculation/Retake/Transcript runtime | PASS | authenticated session loaded live operation state, 2 retake rows and student transcript; no console error; Transcript 390px viewport had no page overflow |
| browser Scorebook post-error-boundary check | PASS | current source loaded the authenticated Scorebook with 6 rows, no console error and no desktop page overflow |
| browser Scorebook post-foundation-CSS check | PASS | reload retained title and 6 rows with no console error or desktop page overflow |
| browser Scorebook post-catalog-CSS check | PASS | reload retained title and 6 rows with no desktop page overflow |
| browser Scorebook mobile post-CSS check | PASS | 390px viewport retained title and 6 rows without page overflow |
| browser Scorebook post-responsive-CSS check | PASS | reload retained title and 6 rows with no console error or desktop page overflow |
| browser Academic list | PASS | authenticated list loaded 3 rows at desktop and 390px without page overflow or console error |
| browser Student Detail | PASS | authenticated profile loaded 4 tabs at desktop and 390px without page overflow or console error |

## Deviations and remaining risks

- The plan scope is complete. Large workspaces retain route/lifecycle and
  mutation orchestration in their views by design; typed presentation, dialog,
  context, retry, session/error/date and CSS responsibilities were extracted
  without changing API or business behavior.
- Foundation/global layout, academic/enrollment, Attendance, Transcript,
  Scorebook, Calculation, Retake and responsive overrides now reside in
  responsibility-specific stylesheets. No visual redesign was attempted.
- Browser runtime validation passed for the authenticated `ACADEMIC_OFFICE`
  session across Academic, Student Detail, Attendance, Scorebook, Calculation,
  Retake and Transcript. The user-provided authenticated session also confirms
  the existing login flow without creating demo data.
- Chrome disconnected after the CSS-route checks, so the last Transcript
  context-only extraction is validated by focused tests, lint and production
  build but has not received a second live-browser pass.
- The new panels are covered through the Student Detail view contract and are
  included in coverage, but dedicated Storybook stories and isolated panel
  specs were not added yet.

## Next steps

No further work is required for Plan 072. SMTP and any broader multi-tab/E2E
release exercise belong to separate release-hardening scope.
