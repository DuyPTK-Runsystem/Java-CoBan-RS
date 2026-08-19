# Dev Note: FE Project Skeleton

## 1. Related Developer Plan

- Plan: `document/dev-impl-plan/fe/foundation/009-fe-project-skeleton-2026-08-18.md`
- Approval: Approved by user on 2026-08-18.

## 2. Actual scope completed

- Created a Vue 3 + Vite + TypeScript frontend project under `FE/`.
- Added Vue Router routes for `/register`, `/login`, `/students`, `/students/new`, and `/students/:studentId/edit`.
- Added typed auth and student models, local form validation, responsive auth/authenticated layouts, student search/table/form presentation, and PrimeVue configuration.
- Added required Storybook stories: `LoginForm` Default/Filled/ValidationError and `RegisterForm` Default/Filled/PasswordMismatch/ValidationError.
- Added `VITE_API_BASE_URL` placeholder without implementing REST calls, auth guards, or persistence.

## 3. Files changed

- `FE/package.json`, `FE/index.html`, `FE/vite.config.ts`, `FE/tsconfig*.json`, `FE/eslint.config.js`, `FE/.env.example`, `FE/.gitignore`, `FE/README.md`: project, tooling, ignore rules, and run documentation.
- `FE/.storybook/*`: Storybook configuration and global preview styles.
- `FE/src/components/*`: reusable typed forms, student table/search/form, and authenticated layout.
- `FE/src/views/*`: route-level orchestration for the four supplied screen flows.
- `FE/src/router/index.ts`, `FE/src/main.ts`, `FE/src/App.vue`, `FE/src/styles.css`: application bootstrap, routes, and Academic Core design tokens/layout.
- `FE/src/types/*`, `FE/src/services/apiConfig.ts`: UI contracts and API base URL placeholder.
- `document/dev-impl-plan/*`: plan approval status updated to `Approved`.

## 4. Important decisions

- Student list interactions use deterministic demo rows and local presentation state so the skeleton is reviewable without a backend.
- Student form add/edit mode follows the supplied constraints: generated `STU` code in add mode; student id/code and code generation are disabled in edit mode.
- PrimeVue 4 components are used for student controls, table, paginator, and confirmation dialog.
- No unresolved authentication mechanism, API path, score range, or student-code uniqueness rule was invented.

## 5. Validation

| Command | Result | Evidence / reason |
|---|---|---|
| `npm install` | FAIL | Registry/network call stalled in sandbox and escalated execution was rejected by environment policy. |
| `npm run lint` | FAIL | Only global ESLint 6.4.0 is available; it cannot load the configured flat config. |
| `npm run build` | FAIL | `vue-tsc` is unavailable because dependencies are not installed. |
| `npm run build-storybook` | FAIL | `storybook` is unavailable because dependencies are not installed. |

The globally available `eslint` is version 6 and cannot consume the project's flat config; the configured project lint script therefore remains unrun. Static source review was completed, including a guard against sorting with an empty student list and explicit Storybook dependency declaration.

## 6. Deviations and remaining risks

- No deviation from approved feature scope. REST services, real auth, guards, mutations, and server-side query integration remain intentionally deferred.
- Full validation remains blocked until the environment permits npm registry access or dependencies are installed externally.

## 7. Next steps

- Install dependencies in an environment with registry access.
- Run `npm run lint`, `npm run build`, and `npm run build-storybook` from `FE/`.
- Replace demo state with approved API services in a follow-up frontend plan.
