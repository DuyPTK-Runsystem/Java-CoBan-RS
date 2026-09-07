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
