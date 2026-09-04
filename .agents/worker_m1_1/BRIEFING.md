# BRIEFING — 2026-09-04T09:57:00Z

## Mission
Thực hiện toàn bộ thay đổi cho Milestone 1 (Navigation, Routing & Shell V2 Integration) theo PROJECT.md và ORIGINAL_REQUEST.md.

## 🔒 My Identity
- Archetype: worker
- Roles: implementer, qa, specialist
- Working directory: /home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/worker_m1_1
- Original parent: 275dfe1c-b9ce-4cb7-b542-547e4cb2fc91
- Milestone: Milestone 1 (Navigation, Routing & Shell V2 Integration)

## 🔒 Key Constraints
- Exclusive file ownership:
  - FE/src/views/LoginView.vue
  - FE/src/views/LoginView.spec.ts
  - FE/src/router/index.ts
  - FE/src/router/index.spec.ts
  - FE/src/views/AuthenticatedV2ShellView.vue
  - FE/src/views/AuthenticatedV2ShellView.spec.ts
  - FE/src/components/AuthenticatedLayout.vue
- Write only to working directory .agents/worker_m1_1 and owned FE files.
- MANDATORY INTEGRITY MANDATE: DO NOT CHEAT, no dummy/facade implementations, genuine logic only.
- 100% tests must pass and build must succeed without TypeScript errors.

## Current Parent
- Conversation ID: 275dfe1c-b9ce-4cb7-b542-547e4cb2fc91
- Updated: 2026-09-04T09:57:00Z

## Task Summary
- **What to build**:
  1. `FE/src/views/LoginView.vue`: safeRedirect fallback -> '/v2', successRedirect = ref('/v2').
  2. `FE/src/router/index.ts`: guestOnly guard redirect -> '/v2'; register child routes under '/v2' (AuthenticatedV2ShellView): 'students' (v2-students), 'students/new' (v2-student-create), 'students/:studentId' (v2-student-detail), 'students/:studentId/edit' (v2-student-edit); add redirect from legacy '/students' to '/v2/students'.
  3. `FE/src/views/AuthenticatedV2ShellView.vue`: add menu item "Hồ sơ học sinh" with icon 'pi pi-user', to: '/v2/students', only visible for isNonStudent (ADMIN, ACADEMIC_OFFICE, TEACHER), active when route starts with '/v2/students'.
  4. `FE/src/components/AuthenticatedLayout.vue`: update logo link to '/v2'.
  5. Update and add unit tests: `LoginView.spec.ts`, `router/index.spec.ts`, `AuthenticatedV2ShellView.spec.ts`.
  6. Run `npm --prefix FE run test -- --run` and `npm --prefix FE run build`.
  7. Write `handoff.md` and send report to parent via `send_message`.
- **Success criteria**:
  - All unit tests pass (401/401 tests pass).
  - TypeScript build passes cleanly.
  - All Milestone 1 requirements met genuinely.
- **Interface contracts**: PROJECT.md § Interface Contracts
- **Code layout**: PROJECT.md § Code Layout

## Key Decisions Made
- `StudentDetailPlaceholder` was created in `FE/src/router/index.ts` to allow route `/v2/students/:studentId` (`v2-student-detail`) to build cleanly without errors until Milestone 4 implements `StudentDetailView.vue`.
- "Hồ sơ học sinh" menu item is placed immediately following "Xếp lớp" (`/v2/enrollments`) for `isNonStudent` roles and is configured with active detection for any subroute starting with `/v2/students`.
- Legacy routes `/students`, `/students/new`, and `/students/:studentId/edit` cleanly redirect to their respective `/v2/students` paths.

## Change Tracker
- **Files modified**:
  - `FE/src/views/LoginView.vue`: Changed successRedirect and safeRedirect fallback to `/v2`.
  - `FE/src/views/LoginView.spec.ts`: Added tests for `/v2` default and unsafe redirect fallback.
  - `FE/src/router/index.ts`: Added student v2 child routes, legacy redirects, and guestOnly redirect to `/v2`.
  - `FE/src/router/index.spec.ts`: Updated tests for guestOnly redirect to `/v2`, legacy redirects, and v2 routes.
  - `FE/src/views/AuthenticatedV2ShellView.vue`: Added "Hồ sơ học sinh" item with icon `pi pi-user` for `isNonStudent` and active state.
  - `FE/src/views/AuthenticatedV2ShellView.spec.ts`: Added 9 unit tests for role visibility, icon, and active states.
  - `FE/src/components/AuthenticatedLayout.vue`: Updated brand logo link to `/v2`.
- **Build status**: PASS (vue-tsc --noEmit && vite build completed with 0 errors).
- **Pending issues**: None.

## Quality Status
- **Build/test result**: 79/79 test files passed, 401/401 tests passed (100% PASS).
- **Lint status**: 0 lint errors in all M1 owned files.
- **Tests added/modified**:
  - `LoginView.spec.ts`: 4 passed (+2 tests)
  - `AuthenticatedV2ShellView.spec.ts`: 15 passed (+9 tests)
  - `router/index.spec.ts`: 25 passed (+3 tests)

## Loaded Skills
- **Source**: /home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/skills/frontend-validation/SKILL.md
- **Local copy**: N/A
- **Core methodology**: Run npm test, lint, build from FE/package.json, verify PASS genuinely without skipping or faking.

## Artifact Index
- /home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/worker_m1_1/DISPATCH.md — Assignment instructions
- /home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/worker_m1_1/BRIEFING.md — Working memory
- /home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/worker_m1_1/progress.md — Liveness heartbeat and progress tracking
- /home/duyptk/Coding/HoiNhapJava/Java-CoBan/.agents/worker_m1_1/handoff.md — Handoff report
