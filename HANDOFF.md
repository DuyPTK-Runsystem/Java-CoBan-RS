# Agent Handoff

## Task

- Plan: `053-grade-class-subject-management-ui-2026-08-27`
- Application version: `v2`
- Approval: user approved phases `0-1-2-3` and then approved remaining phases `4-5-6` on 2026-08-27.
- Scope: integrate the approved Storybook catalog UI with the existing v2 API and routes; evaluate Phase 6 against the real backend contract.

## Current checkpoint

- Status: `COMPLETED_WITH_BLOCKER`
- Last updated: 2026-08-27
- Active phase: Validation and documentation
- Last completed action: completed Phase 4-5 API methods, four catalog views, routes, shell navigation, and catalog service route/body coverage.
- Next action: user review of the API-backed catalog flow; create a separate approved backend contract/plan before resuming Phase 6.

## Phase status

| Phase | Status | Notes |
|---|---|---|
| 0 | `COMPLETED` | Plan, wireframe, contract gaps approved. |
| 1 | `COMPLETED` | Static Storybook Grade/Class UI delivered. |
| 2 | `COMPLETED` | Static Storybook Subject/Applicability UI delivered. |
| 3 | `COMPLETED` | Static Storybook Class-Subject UI delivered. |
| 4 | `COMPLETED` | Grade/Class API, views, routes, shell navigation, and service coverage. |
| 5 | `COMPLETED` | Subject, applicability create-only, Class-Subject API, views, routes, and service coverage. |
| 6 | `BLOCKED` | Backend list contract currently lacks active student count, grade average, and warning payload. Do not use N+1 or FE calculations. |

## Existing relevant changes

- Static catalog components/stories exist under `FE/src/components/`.
- Catalog types already exist in `FE/src/types/academic.ts`.
- Existing academic year/semester API and views establish the v2 patterns.
- Preserve unrelated user change in `BE/BaiTap-RS/src/main/resources/application.properties`.

## Contract constraints

- Use `apiClient`; no raw `fetch` in views/components.
- Grade and class endpoints are office-authorized for mutations; backend remains authorization source of truth.
- Class list requires `academicYearId` when the view loads a scoped list.
- Subject applicability is create-only because no list/update/deactivate endpoint exists.
- Class-subject list requires `semesterId`; do not call it with incomplete context.
- Do not infer roles in the frontend.
- Do not invent Phase 6 statistics/warning endpoints or calculate them in FE.

## Recovery instructions

1. Read this file first.
2. Read the approved plan and Dev Note 053.
3. Run `git status --short` and preserve unrelated changes.
4. Inspect the current phase section and `Last completed action` before editing.
5. After each meaningful implementation/validation checkpoint, update this file.

## Checkpoint log

- [2026-08-27] Started Phase 4-5-6 implementation after user approval.
- [2026-08-27] Confirmed Phase 6 is contract-blocked by the current backend API.
- [2026-08-27] Moved the handoff file to project root as `HANDOFF.md` at the user's request.
- [2026-08-27] Phase 4 API boundary and `GradeListView.vue`/`SchoolClassListView.vue` were added; route wiring and tests remain.
- [2026-08-27] Phase 4 route wiring and shell navigation completed; Phase 5 production views and API flow added.
- [2026-08-27] Phase 4-5 service tests cover endpoint URLs, query parameters, request bodies, and lifecycle calls; early lint/build passed.
- [2026-08-27] Phase 6 contract audit confirmed `ResCapacityWarningDTO` is only returned from enrollment mutations; no class-list statistics/warning read endpoint exists.
- [2026-08-27] Final validation passed: lint, tests (87), coverage (89.67%), production build, Storybook build, and `git diff --check`.
- [2026-08-27] Vite production route server returned `HTTP 200` at `http://localhost:5173/` during a foreground smoke check; Storybook remains available at `http://localhost:6007/`.
- [2026-08-27] Detached Vite processes are cleaned up by this environment; rerun `cd FE && npm run dev -- --host 0.0.0.0 --port 5173` for an interactive production-route review.
- [2026-08-27] Plan/Dev Note summaries updated; implementation is ready for user review with Phase 6 explicitly blocked.
