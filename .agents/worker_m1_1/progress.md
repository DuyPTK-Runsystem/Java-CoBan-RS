# Progress - Milestone 1 Worker

Last visited: 2026-09-04T16:57:00+07:00

## Status: COMPLETED

### Completed Steps
1. Investigated requirements and codebase for Milestone 1 (Navigation, Routing & Shell V2 Integration).
2. Modified `FE/src/views/LoginView.vue`:
   - Updated `successRedirect` initialization to `ref('/v2')`.
   - Updated `safeRedirect()` fallback to `'/v2'`.
3. Modified `FE/src/views/LoginView.spec.ts`:
   - Added and updated tests for redirecting to intended path (`/v2/students/new`), fallback redirect to `/v2` when query is omitted, and fallback to `/v2` when redirect query is unsafe (open redirect).
4. Modified `FE/src/router/index.ts`:
   - Updated `guestOnly` guard in `beforeEach` to redirect to `'/v2'`.
   - Registered child routes in `AuthenticatedV2ShellView` under `/v2`:
     - `/v2/students` (`v2-students`) -> `StudentListView.vue`
     - `/v2/students/new` (`v2-student-create`) -> `StudentFormView.vue`
     - `/v2/students/:studentId` (`v2-student-detail`) -> `StudentDetailPlaceholder`
     - `/v2/students/:studentId/edit` (`v2-student-edit`) -> `StudentFormView.vue`
   - Added redirects from legacy paths `/students`, `/students/new`, and `/students/:studentId/edit` to corresponding `/v2/students` routes.
5. Modified `FE/src/router/index.spec.ts`:
   - Updated guest guard tests to expect redirect to `/v2` (`v2-shell`).
   - Added tests for legacy redirects `/students` -> `/v2/students`, `/students/new` -> `/v2/students/new`, and `/students/:id/edit` -> `/v2/students/:id/edit`.
   - Added tests verifying nested rendering of `/v2/students` within the shell.
   - Updated route mappings table to cover `/v2/students`, `/v2/students/new`, `/v2/students/4`, `/v2/students/4/edit`.
6. Modified `FE/src/views/AuthenticatedV2ShellView.vue`:
   - Added "Hồ sơ học sinh" menu item with icon `pi pi-user` pointing to `to: '/v2/students'`.
   - Configured visibility restricted to `isNonStudent` (`ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`), completely hidden from `STUDENT`.
   - Placed right after `Xếp lớp` (`/v2/enrollments`).
   - Configured active state (`active: Boolean(route?.path?.startsWith('/v2/students'))`) so it activates for `/v2/students` and any subroutes.
7. Modified `FE/src/views/AuthenticatedV2ShellView.spec.ts`:
   - Added tests verifying menu item label "Hồ sơ học sinh", `pi pi-user` icon, and `/v2/students` destination for `ADMIN`, `ACADEMIC_OFFICE`, and `TEACHER`.
   - Added test verifying menu item is hidden for `STUDENT`.
   - Added tests verifying active state is true on `/v2/students`, `/v2/students/new`, `/v2/students/101`, and `/v2/students/101/edit`.
   - Added test verifying active state is false when on other routes.
8. Modified `FE/src/components/AuthenticatedLayout.vue`:
   - Updated header brand logo link from `/students` to `/v2`.
9. Executed verification:
   - `npm --prefix FE run test -- --run`: 79/79 test files passed (401/401 tests passed - 100% PASS).
   - `npm --prefix FE run build`: 100% PASS, 0 TypeScript errors, bundle generated cleanly.
   - `npx eslint` on all M1 modified files: 0 errors, 0 warnings.
