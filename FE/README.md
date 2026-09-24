# Academic Core Frontend

Vue 3 + Vite + TypeScript skeleton for the Academic Core student-management screens.

## Commands

```bash
npm install
npm run dev
npm run lint
npm run build
npm run storybook
npm run build-storybook
```

Copy `.env.example` to `.env` when a public API base URL is available. The shared typed `apiClient` uses `VITE_API_BASE_URL`, unwraps the REST envelope, supports authenticated JSON/Blob requests, and sends `401` failures to the configured router boundary. The current Student routes remain the legacy-compatible v1 flow; `/v2` is an authenticated shell outlet for approved feature modules.

## Vercel deployment

Import this repository as a Vercel project with Root Directory `FE`. The checked-in `vercel.json` selects Vite, runs `npm ci` and `npm run build`, publishes `dist`, and rewrites nested Vue Router URLs to `index.html`.

Set the Vercel environment variable `VITE_API_BASE_URL` to the Azure Container Apps backend's public HTTPS origin, for example `https://<backend-app>.<region>.azurecontainerapps.io`. Do not add a trailing slash or `/api`; API paths already include `/api`. Configure the Azure backend to allow the exact Vercel frontend origin in CORS. Vite embeds `VITE_API_BASE_URL` during build, so redeploy the frontend after changing it.
