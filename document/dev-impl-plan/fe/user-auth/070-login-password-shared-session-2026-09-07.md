# 070 — Login password and shared session

- Approved: user confirmed v2, then “bỏ bước test đi / impl đi” on 2026-09-07.
- Goal: remove login password length validation; preserve login when opening another same-origin tab.
- Current flow: LoginForm validates 6–15 characters; authSession stores token/user per tab; router checks that session.
- Implementation: retain required password, send unchanged; store AuthSession atomically in localStorage, migrate legacy sessionStorage, prevent old sessions returning after logout, reload other tabs on shared session changes.
- Files: LoginForm.vue, LoginForm.stories.ts, services/authSession.ts, main.ts; v2 UserModule session documentation; plan/note and summary indexes.
- Preserve register validation, backend JWT/API/database and 401/403 behavior. No dependency changes.
- Trade-off: shared session survives browser closure; backend remains authoritative for token validity. Never persist passwords. Logout must invalidate legacy fallback.
- Validation: lint, production build, Storybook build. No test additions, test execution or coverage per explicit user instruction. Browser verification not included.
- Output: nonempty passwords of any length reach login API; new tabs reuse stored session; logout/account changes refresh other tabs.
