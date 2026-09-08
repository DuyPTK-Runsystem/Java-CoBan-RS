# 070 — Login password and shared session

## Approval and scope

- Plan: [070](../../../dev-impl-plan/fe/user-auth/070-login-password-shared-session-2026-09-07.md).
- Approved on 2026-09-07: user confirmed v2 and instructed “bỏ bước test đi / impl đi”.
- Removed login-only 6–15 character validation; required password remains and submitted value is unchanged. Registration/backend/API/database unchanged.

## Actual changes

- `FE/src/components/LoginForm.vue`: remove password length rejection.
- `FE/src/components/LoginForm.stories.ts`: validation-error example uses an empty password.
- `FE/src/services/authSession.ts`: atomic localStorage session record; migrate and clear legacy sessionStorage; validate stored shape; persist null on logout to prevent legacy restoration.
- `FE/src/main.ts`: listen for cross-tab shared-session changes and reload to discard stale account/page state; storage clear also clears legacy state.
- `document/application-doc/v2/modules/UserModule.md`: document shared persistence and login validation. This approved requirement supersedes the earlier sessionStorage rule in FE agent guidance.
- Added plan/note and entries in both FE and global summary indexes.

## Validation

- Initial lint/build/Storybook attempts failed because workspace dependencies were missing (global ESLint 6, missing vue-tsc/storybook).
- `npm ci --cache /tmp/login-fix-npm-cache`: PASS; lockfile unchanged.
- `npm run lint`: PASS after dependency installation.
- `npm run build`: PASS (includes vue-tsc).
- `npm run build-storybook`: PASS.
- `git diff --check`: PASS.
- `npm run test`, `npm run test:coverage`: NOT RUN per user instruction; no tests added or modified.
- Browser/live API/multiple-tab verification: NOT RUN. Builds do not establish runtime behavior.

## Decisions and limitations

- localStorage shares one account across same-origin tabs and persists after browser closure. Backend JWT validation remains authoritative; existing 401 clears the shared session and 403 preserves it.
- Other tabs reload on login/account change/logout; unsaved in-memory UI state is discarded on that session change.
- The stored null marker intentionally remains after logout, without token/user data, so old legacy tabs cannot restore stale credentials.
- Existing tests with sessionStorage expectations were left unchanged under the explicit instruction to skip the test step; they will need alignment when tests resume.
- No layout change and no backend contract changes. No browser verification claimed.

## Documentation correction

- Renumbered this task from 069 to 070 because 069 already belongs to mid-semester transfer score assist. Updated plan/note links and moved entries into the existing tables in all four summary indexes.
- Documentation check: relative links for task 070 and `git diff --check` PASS. Application validation above belongs to the implementation run; not rerun for this documentation-only correction.
