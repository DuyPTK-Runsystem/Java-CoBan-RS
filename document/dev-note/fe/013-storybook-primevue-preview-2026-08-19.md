# Dev Note: Storybook PrimeVue Preview Runtime

## 1. Related Developer Plan and approval

- Plan: `document/dev-impl-plan/fe/013-storybook-primevue-preview-2026-08-19.md`.
- User approved implementation on 2026-08-19.

## 2. Actual scope completed

- Registered the existing PrimeVue v4 plugin through Storybook Vue's exported
  global `setup((app) => ...)` hook.
- Reused the application Aura theme with `prefix: 'p'` and
  `darkModeSelector: 'none'`.
- Imported PrimeIcons into the Storybook preview.
- Added `vite/client` types to the existing Storybook TypeScript project so the IDE
  recognizes CSS side-effect imports in `preview.ts`.
- Did not change Auth components, stories, API, router, dependencies or package
  versions.

## 3. Files changed

### Storybook runtime configuration

- `FE/.storybook/preview.ts`.
- `FE/tsconfig.node.json`.

### Plan and tracking

- `document/dev-impl-plan/fe/013-storybook-primevue-preview-2026-08-19.md`.
- `document/dev-impl-plan/fe/FE_DEV_PLAN_SUMMARY.md`.
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`.
- `document/dev-note/fe/013-storybook-primevue-preview-2026-08-19.md`.
- `document/dev-note/fe/FE_DEV_NOTE_SUMMARY.md`.
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`.

## 4. Implementation decisions

- Used Storybook Vue's exported `setup((app) => ...)` hook so PrimeVue is
  installed before any story mounts. This supplies `$primevue.config` required by
  PrimeVue controls. An initial object-property form was ignored by Storybook 8.6
  and was corrected to the supported hook API during this implementation.
- Matched `FE/src/main.ts` instead of introducing a separate Storybook-only theme
  configuration, preventing visual drift between the application and stories.
- Kept the change global because both LoginForm and RegisterForm use PrimeVue
  controls and all existing/future stories should share the same app context.
- Reused Vite's built-in `*.css` declaration instead of adding a duplicate local
  declaration file.

## 5. Validation evidence

All scripted commands ran from `FE/` after the configuration change.

| Command or check | Result | Evidence |
|---|---|---|
| `npm run lint` | PASS | ESLint completed with zero errors and warnings. |
| `npm run test` | PASS | 8 test files and 16 tests passed. |
| `npm run test:coverage` | PASS | 8 test files and 16 tests passed; V8 report generated. |
| `npm run build` | PASS | `vue-tsc --noEmit` and Vite production build completed. |
| `npm run build-storybook` | PASS | Storybook preview build completed. |
| Headless browser: `LoginForm/Default` | PASS | Rendered form controls without the `$primevue` error overlay. |

Coverage read from generated report: 94.64% statements, 82.69% branches, 80.00%
functions and 94.64% lines.

## 6. Deviations from Developer Plan

- No scope deviation. The first implementation used an unsupported `Preview`
  object property; after the Docs runtime error was reported, it was corrected to
  the package's supported exported `setup((app) => ...)` API. No component,
  story, dependency or API behavior changed.

## 7. Remaining risks and next steps

- `npm run build-storybook` still reports the pre-existing warnings about PrimeVue
  package discovery, `eval` in Storybook runtime and a large Docs chunk. It exits
  successfully; none is the `$primevue` runtime error addressed by this plan.
- Restart the running `npm run storybook` process once so it rebuilds the corrected
  preview module, then refresh LoginForm/RegisterForm Docs in the browser.
