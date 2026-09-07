# Frontend Agent Rules — Auth, Routing and Security

[← Back to `FE/AGENTS.override.md`](../AGENTS.override.md)

## Authentication and session

Keep the current stateless JWT session model unless an approved plan changes it.

Frontend session rules:

- store the access token and UI-safe account summary in `sessionStorage`;
- never store password, password hash, default password, or encoded password;
- `401 Unauthorized`: clear auth state and navigate to Login;
- `403 Forbidden`: keep auth state and show an access-denied state;
- never put access tokens in URLs, logs, route params, or presentation-component props.

### Role discovery blocker

The current implemented auth response and JWT do **not** expose the user's roles:

- `ResUserDTO` currently contains user id, username and audit fields only;
- JWT currently contains `sub`, `user_id`, `iat`, and `exp`, but no role claim.

Therefore, until an approved backend contract exposes roles/capabilities to the frontend:

- do not infer role from username, linked records, route, previous API success, or client-side assumptions;
- do not decode the JWT and pretend a role claim exists;
- do not implement authoritative role-aware navigation from guessed data;
- do not treat hidden buttons or route guards as security enforcement.

Backend authorization remains authoritative through Spring Security and service-level scope checks.

A role-aware FE shell must wait for, or be implemented together with, an approved role/account contract.

## Routing

Use Vue Router.

Route metadata may express:

- `requiresAuth`;
- `guestOnly`;
- approved role/capability metadata only after the frontend can obtain that information from a real backend contract.

Do not duplicate backend authorization rules into brittle route logic.

If a route is visible but the backend returns `403`, keep the session and show an access-denied result.

## UX and security

Use PrimeVue consistently with the current project.

Make warning vs blocking error visually distinct.

Historical/read-only data must not look editable.

Never:

- store/log plaintext credentials unnecessarily;
- embed secrets in FE source;
- expose backend secrets through `VITE_*`;
- rely on client validation, hidden buttons, or route guards as authorization;
- present legacy `averageScore` as the official transcript average;
- silently convert an unresolved backend/requirement gap into frontend behavior.

Use Vite environment variables only for public frontend configuration such as API base URL.
