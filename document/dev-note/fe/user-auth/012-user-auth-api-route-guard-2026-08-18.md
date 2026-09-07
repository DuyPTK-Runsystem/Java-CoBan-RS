# Dev Note: User/Auth API và Route Guard

## 1. Related Developer Plan and approval

- Plan: `document/dev-impl-plan/fe/user-auth/012-user-auth-api-route-guard-2026-08-18.md`.
- User approved implementation on 2026-08-18.

## 2. Actual scope completed

- Added typed native-`fetch` User/Auth service for register, login, account and
  stateless logout; it maps form `userName` to backend `username` and unwraps the
  existing REST response envelope.
- Added an `ApiError` transport boundary with HTTP status and backend message for
  meaningful view handling.
- Added safe auth-session helpers over `sessionStorage`; only JWT access token and
  UI-safe user summary are stored. Corrupt/missing browser data is cleared and
  treated as signed out.
- Connected Login and Register views to the API, including loading/error state,
  login session persistence and safe internal redirect after login.
- Added Vue Router guest/protected metadata and navigation guard for all Student,
  Login and Register routes; added a fallback route.
- Replaced layout demo username and local-only logout with session username and
  backend logout followed by unconditional local-session cleanup.
- Added deterministic tests for API request mapping/error handling, auth session
  recovery and route guard behavior. Extended coverage inclusion to auth services.
- Added the approved Register status popup with a keyboard-operable `Close` button.
  Success waits for `Close` before navigating to Login; failure closes in place and
  retains the form values.
- Added the matching Login status popup. Login success persists the session first,
  then waits for `Close` before navigating to the validated internal redirect or
  Student List; failure retains the Login form without a session.

## 3. Files changed

### Auth types, services and routing

- `FE/src/types/user.ts`.
- `FE/src/services/authSession.ts`.
- `FE/src/services/userApi.ts`.
- `FE/src/router/index.ts`.
- `FE/vite.config.ts`.

### Screen integration

- `FE/src/views/LoginView.vue`.
- `FE/src/views/LoginView.spec.ts`.
- `FE/src/views/RegisterView.vue`.
- `FE/src/views/RegisterView.spec.ts`.
- `FE/src/views/StudentListView.vue`.
- `FE/src/views/StudentFormView.vue`.
- `FE/src/components/AuthenticatedLayout.vue`.

### Tests and tracking

- `FE/src/services/authSession.spec.ts`.
- `FE/src/services/userApi.spec.ts`.
- `FE/src/router/index.spec.ts`.
- `document/dev-impl-plan/fe/user-auth/012-user-auth-api-route-guard-2026-08-18.md`.
- `document/dev-impl-plan/fe/FE_DEV_PLAN_SUMMARY.md`.
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`.
- `document/dev-note/fe/FE_DEV_NOTE_SUMMARY.md`.
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`.

## 4. Implementation decisions

- Reused native `fetch` and `VITE_API_BASE_URL`; its local default and `.env.example`
  target `http://localhost:8081`, matching the backend `server.port`. No Axios,
  Pinia/Vuex or new runtime dependency was added.
- Kept backend response field `access_token` only at the API boundary and exposed
  camel-case `accessToken` to frontend callers.
- Route guards check local session synchronously for navigation UX. They do not
  replace backend JWT authorization. A future protected API consumer can distinguish
  typed `401` and `403`; Login currently clears invalid local state on `401`.
- Logout clears local state in `finally`, because backend logout is stateless and a
  failed network request must not leave the UI apparently authenticated.
- `/api/v1/auth/account` is available in the typed service but is not called on every
  navigation; login already returns the UI-safe summary and avoiding per-navigation
  network requests keeps guards deterministic.
- The popup is view-owned because Register API orchestration and route navigation are
  view responsibilities. It uses existing PrimeVue `Dialog` and `Button`; it cannot
  be dismissed by an overlay click or Escape, so the explicit `Close` action controls
  the resulting navigation.
- Login uses the same modal behavior while retaining the safe redirect calculated
  before the popup is shown; session persistence remains immediately after a valid
  login response so protected navigation is allowed once `Close` is selected.

## 5. Validation evidence

All validation commands ran from `FE/` after final source changes.

| Command | Result | Evidence |
|---|---|---|
| `npm run lint` | PASS | ESLint completed with zero errors and warnings. |
| `npm run test` | PASS | 8 test files and 16 tests passed. |
| `npm run test:coverage` | PASS | 8 test files and 16 tests passed; V8 report generated. |
| `npm run build` | PASS | `vue-tsc --noEmit` and Vite production build completed. |
| `npm run build-storybook` | PASS | Storybook build completed. |

Coverage read from generated report:

| Metric | Result |
|---|---:|
| Statements | 94.64% |
| Lines | 94.64% |
| Branches | 82.69% |
| Functions | 80.00% |

## 6. Deviations from plan

- No Student API was introduced; Student views retain their approved demo data.
- No view currently calls `account()` because login response already contains user
  summary and no screen requires account refresh yet.
- No dedicated `403` UI was added because the current no-role backend has no
  permission-restricted Student endpoint. The API error preserves its status and
  message so that the future Student API plan can present access denial without
  clearing auth state.
- The approved Register flow now waits for the user to close a success popup before
  going to Login, instead of navigating immediately after the API response.
- The approved Login flow now waits for the user to close a success popup before
  following its safe redirect, instead of navigating immediately after the API response.

## 7. Remaining risks and next steps

- Browser navigation guard only detects local-session presence; expired/invalid JWT
  is authoritatively detected when a protected backend API returns `401`.
- The next Student API integration plan should reuse `userApi` error policy or a
  small shared fetch helper, clear/redirect on `401`, and retain session/show a
  message on `403`.
- Storybook emits pre-existing warnings about PrimeVue package discovery, `eval` in
  Storybook runtime and a large generated docs chunk; its build exits successfully.
