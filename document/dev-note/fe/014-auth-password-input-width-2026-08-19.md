# Dev Note: Auth Password Input Width

## 1. Related Developer Plan and approval

- Plan: `document/dev-impl-plan/fe/014-auth-password-input-width-2026-08-19.md`.
- User approved implementation on 2026-08-19.

## 2. Actual scope completed

- Added a scoped width rule for the PrimeVue Password input nested in an Auth form
  field group.
- The rule applies to Login Password and Register Password/Confirm password without
  changing form markup, validation, emits, API flow or component contracts.

## 3. Files changed

- `FE/src/styles.css`.
- `document/dev-impl-plan/fe/014-auth-password-input-width-2026-08-19.md`.
- `document/dev-impl-plan/fe/FE_DEV_PLAN_SUMMARY.md`.
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`.
- `document/dev-note/fe/014-auth-password-input-width-2026-08-19.md`.
- `document/dev-note/fe/FE_DEV_NOTE_SUMMARY.md`.
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`.

## 4. Implementation decisions

- Applied `width: 100%` to `.field-group > .p-password .p-password-input` rather
  than using repeated Password `inputStyle` props.
- Kept the selector inside `.field-group`, limiting the change to forms using the
  established field layout and avoiding unrelated Password controls.

## 5. Validation evidence

All scripted commands ran from `FE/` after the CSS change.

| Command or check | Result | Evidence |
|---|---|---|
| `npm run lint` | PASS | ESLint completed with zero errors and warnings. |
| `npm run test` | PASS | 8 test files and 16 tests passed. |
| `npm run test:coverage` | PASS | 8 test files and 16 tests passed; V8 report generated. |
| `npm run build` | PASS | `vue-tsc --noEmit` and Vite production build completed. |
| `npm run build-storybook` | PASS | Storybook preview build completed. |
| Headless browser: `RegisterForm/Default` | PASS | User name, Password and Confirm password rendered at equal width; icons stayed aligned. |

Coverage read from the generated report: 94.64% statements, 82.69% branches,
80.00% functions and 94.64% lines.

## 6. Deviations from Developer Plan

- No source-scope deviation. The static headless check for LoginForm remained on a
  Storybook loading state in this environment, so it is not claimed as an
  independent visual assertion. LoginForm uses the same scoped Password markup and
  CSS selector; validate it by refreshing the normal local Storybook session.

## 7. Remaining risks and next steps

- `npm run build-storybook` retains pre-existing warnings about PrimeVue package
  discovery, Storybook runtime `eval` and a large Docs chunk; it exits successfully.
- Refresh LoginForm and RegisterForm in the normal Storybook session to visually
  confirm every required story state after the CSS update.
