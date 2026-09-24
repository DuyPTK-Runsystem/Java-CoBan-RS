# Vercel frontend deployment — 2026-09-24

## Plan and approval

- Related Developer Plan: none; the user directly requested deployment and then selected Vercel for FE and Azure Container Apps for BE.
- Approval: user's current task authorizes implementation. This note covers the frontend configuration only.

## Actual scope

- Added Vercel build settings for the `FE` project root: Vite, `npm ci`, `npm run build`, and `dist` output.
- Added a rewrite to `index.html` for Vue Router deep links.
- Documented the Azure Container Apps backend public URL required as the Vercel build variable `VITE_API_BASE_URL`.

## Files changed

- Deployment: `FE/vercel.json`.
- Instructions: `FE/README.md`.
- Dev Note: this file and FE/general Dev Note summaries.

## Decisions

- FE and BE use separate public domains. `VITE_API_BASE_URL` is the Azure Container Apps BE origin, with no `/api` suffix; BE must allow the exact Vercel FE origin in CORS.
- The API origin is embedded in `dist` by Vite and requires a new FE build when changed.

## Validation

- `VITE_API_BASE_URL=https://backend.example.invalid npm run build`: PASS (2026-09-24, 733 modules transformed). This validates the Vite build only; the placeholder is not a live BE.
- Independent QA: `VITE_API_BASE_URL=https://backend.example.invalid npm run build` PASS (vue-tsc + Vite, 733 modules); bundle chứa URL mẫu và không chứa `localhost:8081`.
- `npm run lint`: FAIL; 0 errors, 2 warning có sẵn `vue/one-component-per-file` tại `NotificationComposer.spec.ts:8,14`, ngoài diff deploy.
- `git diff --check` và parse `FE/vercel.json`: PASS (QA độc lập).
- `npm run test`: NOT RUN; independent QA pending.
- `npm run test:coverage`: NOT RUN; independent QA pending.
- Vercel build/runtime, deep-link rewrite, and live CORS: NOT RUN.

## Deviation and remaining work

- User changed the FE target from Railway to Vercel; the initial Docker/Caddy files were removed before delivery.
- Set Vercel Root Directory `FE`, `VITE_API_BASE_URL` to the real Azure Container Apps BE URL, and verify a deployed nested route plus an API call. Configure BE CORS for the actual Vercel domain.
