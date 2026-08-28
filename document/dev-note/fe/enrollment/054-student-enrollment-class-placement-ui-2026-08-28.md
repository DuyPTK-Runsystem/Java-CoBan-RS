# Dev Note 054: Student Enrollment & Class Placement UI

## Related plan

- Developer Plan: [`document/dev-impl-plan/fe/enrollment/054-student-enrollment-class-placement-ui-2026-08-28.md`](../../../dev-impl-plan/fe/enrollment/054-student-enrollment-class-placement-ui-2026-08-28.md)
- Approval: `APPROVED - 2026-08-28`
- Application version: `v2`
- Implementation status: `COMPLETED`

## Scope completed

- Added authenticated route `/v2/enrollments` and shell navigation `Xếp lớp`.
- Added academic-year context with `ACTIVE`-first fallback, class selection,
  closed-class read-only behavior, local student filtering, unassigned student
  list and class roster.
- Added single/bulk placement, transfer and read-only enrollment history flows.
- Refined enrollment context wording to `Chọn lớp`, renamed the page heading to
  `Xếp lớp`, added a `Khối` selector in the order `Năm học → Khối → Lớp hiện tại`
  and filtered classes/roster by the selected grade.
- Simplified the class roster table by removing its subtitle and visible
  `Enrollment ID` column; the technical enrollment id remains in the row model
  for transfer requests.
- Expanded the `Chưa chọn lớp` context message across all three selector
  columns so it aligns with the full `Năm học`, `Khối`, `Lớp hiện tại` row.
- Updated transfer success feedback to include the student code/name and both
  source and target class names in the format `Đã chuyển {student code}-
  {student name} từ lớp {old class name} sang {new class name}.`; class labels
  prefer `className` and fall back to `classCode`/technical id.
- Updated the global FE typography stack to prioritize `Segoe UI`, with
  system sans-serif fallbacks when that font is unavailable.
- Kept bulk placement payload on `studentIds`; kept local `LocalDateTime`
  formatting without `toISOString()` timezone conversion.
- Reloaded unassigned list and roster after successful mutations; rendered
  mutation capacity warnings as non-blocking status.
- Added focused view/dialog tests and deterministic Storybook states without a
  live backend.

## Files changed

### FE module implementation

- `FE/src/types/enrollment.ts`
- `FE/src/services/enrollmentApi.ts`
- `FE/src/views/EnrollmentListView.vue`
- `FE/src/components/EnrollmentContextPanel.vue`
- `FE/src/components/UnassignedStudentTable.vue`
- `FE/src/components/ClassStudentTable.vue`
- `FE/src/components/EnrollmentMutationDialog.vue`
- `FE/src/components/TransferEnrollmentDialog.vue`
- `FE/src/components/StudentEnrollmentHistoryDialog.vue`
- `FE/src/router/index.ts`
- `FE/src/views/AuthenticatedV2ShellView.vue`
- `FE/src/styles.css`

### Tests and Storybook

- `FE/src/services/enrollmentApi.spec.ts` (by-code history expectation aligned
  with the implemented controller contract)
- `FE/src/views/EnrollmentListView.spec.ts`
- `FE/src/components/EnrollmentMutationDialog.spec.ts`
- `FE/src/components/TransferEnrollmentDialog.spec.ts`
- Enrollment Storybook stories for context, unassigned table, roster,
  placement dialog, transfer dialog and history dialog.
- `FE/src/router/index.spec.ts`

### Project records

- `HANDOFF.md`
- `document/dev-impl-plan/fe/FE_DEV_PLAN_SUMMARY.md`
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`
- `document/dev-note/fe/FE_DEV_NOTE_SUMMARY.md`
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`

The pre-existing unrelated change in
`BE/BaiTap-RS/src/main/resources/application.properties` was preserved.

## Important decisions

- Backend authorization remains authoritative. The FE does not infer roles or
  use guessed claims to hide security boundaries; `403` remains an access
  denied state.
- The view owns API calls, reloads and error/session handling. Presentation
  components use typed props/emits and do not call the backend.
- Only `ACTIVE`, `COMPLETED` and `WITHDRAWN` are used for enrollment status.
  Transfer remains a read-only history event.
- Closed classes disable placement/transfer actions, while history remains
  available.
- The context status message uses the full grid width of the three selectors,
  keeping the `Chưa chọn lớp` notice aligned with `Năm học`, `Khối` and `Lớp
  hiện tại`.
- Removed the hard-coded `ACTIVE theo roster hiện tại` label because the roster
  DTO does not expose enrollment status. Capacity warning UI is now rendered
  only when a mutation response contains at least one warning.
- Reset capacity warnings when the academic-year/class context changes so a
  warning from an older context cannot remain visible.
- Transfer feedback captures the source and target class labels before the
  post-mutation roster/unassigned-list reload, preventing context refresh from
  losing the names shown to the user.

## Validation

| Command | Result |
|---|---|
| `cd FE && npm run lint` | `PASS` |
| `cd FE && npm run test:coverage` | `PASS` — 89.76% statements, 73.94% branches |
| `cd FE && npm run build` | `PASS` |
| `cd FE && npm run build-storybook` | `PASS` |
| Latest layout follow-up: `npm run lint`, `npm run build`, `git diff --check` | `PASS` |
| Latest roster/warning follow-up: `npm run lint`, affected tests (4 tests), `npm run build`, `git diff --check` | `PASS` |
| Latest transfer-message follow-up: `npm run lint`, affected `EnrollmentListView` tests (3 tests), `npm run build`, `git diff --check` | `PASS` |
| Latest typography follow-up: `npm run lint`, affected `EnrollmentListView` tests (3 tests), `npm run build`, `git diff --check` | `PASS` |

Storybook build emitted existing dependency/runtime and chunk-size warnings;
it completed successfully. No backend, migration or Postman changes were made.

## Deviations and remaining risks

- The view always uses the technical `studentId` history endpoint because both
  table contracts provide a technical id. The by-code service remains covered
  separately for fallback use but is not needed in this flow.
- FE cannot authoritatively distinguish `ADMIN`/`ACADEMIC_OFFICE`/`TEACHER`
  before a role-capability contract exists; mutation security is intentionally
  delegated to backend responses.
- No live backend/browser walkthrough was run in this checkpoint; Storybook is
  deterministic and the API boundary is covered by service tests.

## Next steps

- Run a manual browser walkthrough against the configured backend when it is
  available, especially `403`, `409`, capacity warning and timezone cases.
