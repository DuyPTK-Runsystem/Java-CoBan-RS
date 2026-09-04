# Agent Handoff

## Task

- Plan: `054-student-enrollment-class-placement-ui-2026-08-28`
- Application version: `v2`
- Approval: user approved implementation on 2026-08-28.
- Scope: implement the FE Student Enrollment UI using the existing enrollment
  API contract for academic-year context, unassigned students, class roster,
  single/bulk placement, transfer and read-only history.

## Current checkpoint

- Status: `COMPLETED`
- Last updated: 2026-08-28
- Active phase: completed; ready for manual review
- Last completed action: changed transfer success feedback to include the
  student code/name and source/target class names, added a regression test,
  updated Dev Note 054 and passed FE validation.
- Next action: optional manual browser walkthrough against a running backend;
  no further implementation is pending in this checkpoint.

## Mini-task status

| Mini-task | Status | Checkpoint |
|---|---|---|
| 0. Reset handoff and record Plan 054 approval | `COMPLETED` | Old handoff content removed; Plan 054 checkpoint initialized. |
| 1. Add enrollment types and typed API service | `COMPLETED` | Types and seven endpoint wrappers added; service tests pass. |
| 2. Add route, shell navigation and view scaffold | `COMPLETED` | `/v2/enrollments`, shell navigation and `EnrollmentListView.vue` added. |
| 3. Add context, unassigned, roster, placement, transfer and history UI | `COMPLETED` | Context, tables, mutation/transfer/history dialogs and responsive styles added. |
| 4. Add unit tests and deterministic Storybook states | `COMPLETED` | Bulk/transfer boundary tests and deterministic Enrollment stories added. |
| 5. Run FE validation and update Dev Note 054 | `COMPLETED` | Lint, test, coverage, build and Storybook all PASS; Dev Note and summaries updated. |

## Contract constraints

- Use `apiClient`; no raw `fetch` in views/components.
- Use the current endpoints in `document/application-doc/v2/frontend-api/03-teacher-assignment-enrollment.md`.
- Bulk placement sends `studentIds` selected from rows; do not invent pairing
  semantics with `studentCodes`.
- Use `EnrollmentStatus = ACTIVE | COMPLETED | WITHDRAWN`; do not add a
  `TRANSFERRED` wire status.
- Capacity warnings are non-blocking and only come from mutation responses.
- Backend remains the authorization source of truth; do not infer roles or hide
  mutation security based on unavailable FE role claims.
- Do not calculate class statistics or create N+1 requests for missing counts.
- Transfer history and enrollment history are read-only in this plan.
- Preserve the unrelated change in
  `BE/BaiTap-RS/src/main/resources/application.properties`.

## Recovery instructions

1. Read this file first.
2. Read the approved Plan 054 and current enrollment API/DTO contract.
3. Run `git status --short` and preserve unrelated changes.
4. Inspect the mini-task table and `Last completed action` before editing.
5. After each meaningful implementation or validation checkpoint, update this
   file with the exact files/actions completed and the next action.

## Checkpoint log

- [2026-08-28] User approved Plan 054 and requested incremental implementation
  with continuous handoff checkpoints.
- [2026-08-28] Previous Plan 053 handoff content was cleared and replaced by
  this Plan 054 recovery record.
- [2026-08-28] Confirmed Plan 053.1 is already committed as `f41b105`; current
  unrelated change is `application.properties`.
- [2026-08-28] Added `FE/src/types/enrollment.ts`,
  `FE/src/services/enrollmentApi.ts` and endpoint tests; first fixture reused a
  consumed `Response`, then was corrected to create a fresh response per call.
- [2026-08-28] Mini-task 1 service test passed after the fixture correction;
  no backend contract or unrelated file was changed.
- [2026-08-28] Resumed from the handoff checkpoint, re-verified Plan 054,
  FE rules, current enrollment controller/DTOs and package scripts before
  continuing implementation.
- [2026-08-28] Added enrollment route/navigation and UI boundaries:
  `EnrollmentListView.vue`, `EnrollmentContextPanel.vue`,
  `UnassignedStudentTable.vue`, `ClassStudentTable.vue`,
  `EnrollmentMutationDialog.vue`, `TransferEnrollmentDialog.vue`, and
  `StudentEnrollmentHistoryDialog.vue`; added responsive enrollment styles.
- [2026-08-28] Added `EnrollmentMutationDialog.spec.ts`,
  `TransferEnrollmentDialog.spec.ts` and six Enrollment Storybook files;
  stories use static props and never call the live backend.
- [2026-08-28] Added `EnrollmentListView.spec.ts` for context loading and bulk
  placement reload behavior; fixed list error-state preservation and guarded
  duplicate academic-year loads caused by the initial watcher.
- [2026-08-28] Final validation PASS: `npm run lint`, `npm run test` (31 files,
  101 tests), `npm run test:coverage` (89.76% statements, 73.94% branches),
  `npm run build`, and `npm run build-storybook`.
- [2026-08-28] Created `document/dev-note/fe/enrollment/054-student-enrollment-class-placement-ui-2026-08-28.md` and updated both plan summaries and both Dev Note summaries. Preserved unrelated `application.properties` change.
- [2026-08-28] Follow-up UI wording update: in the academic subject/class-
  subject screens, renamed `Năm học cấu hình` to `Năm học` and `Lớp-môn` to
  `Quản lí môn học các lớp`; no API/backend behavior changed.
- [2026-08-28] Wording follow-up validation PASS: FE lint, affected Subject/
  Router tests (18 tests), production build and `git diff --check`.
- [2026-08-28] Enrollment UI refinement: changed `Student Enrollment UI` to
  `Xếp lớp`, changed `Context xếp lớp` to `Chọn lớp`, added `Khối` between
  `Năm học` and `Lớp hiện tại` with class filtering, and removed the roster
  subtitle and visible `Enrollment ID` column.
- [2026-08-28] Enrollment refinement validation PASS: lint, full test (31
  files, 101 tests), coverage, production build, Storybook build and diff check.
- [2026-08-28] Adjusted `.enrollment-context-summary` to span all three
  enrollment selectors; no logic/API change.
- [2026-08-28] Removed the hard-coded roster status text because status is not
  present in the roster DTO; hid the capacity banner when mutation `warnings`
  is empty and reset warnings when context changes.
- [2026-08-28] Updated capacity warning presentation: each API warning now
  renders on its own line with the resolved class code/fallback class id;
  source is the mutation responses from enrollment create, bulk create or
  transfer endpoints. Validation PASS: lint, affected test (1 file, 2 tests),
  build and Storybook build.
- [2026-08-28] Changed transfer success feedback from the generic destination
  message to `Đã chuyển {student code}-{student name} từ lớp {old class name}
  sang {new class name}.`; labels prefer `className`, then `classCode`/id
  fallback, and are captured before the post-mutation reload. Added a view
  regression test for the complete message. Validation PASS: lint, affected
  EnrollmentListView tests (3 tests), production build and diff check.
- [2026-08-28] Changed the global FE font stack to prioritize `Segoe UI`;
  system sans-serif fallbacks remain for environments without that installed
  font. Validation PASS: lint, affected EnrollmentListView tests (3 tests),
  production build and diff check.
