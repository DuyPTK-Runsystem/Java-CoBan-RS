---
name: frontend-api-service
description: Create, update, or review typed frontend API services under FE/src/services for Vue screens. Use for HTTP calls, request/response types, query parameters, error normalization, and Vite API base configuration.
---

# Implement a frontend API service

## Procedure

1. Inspect the actual backend controller/DTO contract and relevant documentation before coding.
2. Reuse the project's existing HTTP mechanism; do not add Axios or another client unless approved.
3. Define explicit request/response/query types and centralize endpoint construction in `src/services/`.
4. Serialize pagination, search, sort, dates, and request bodies exactly as the backend expects.
5. Normalize only transport concerns that benefit callers; do not silently change business semantics.
6. Surface failures to views in a form suitable for meaningful UI errors.

## Rules

- Name module clients `*Api.ts`, e.g. `userApi.ts`, `studentApi.ts`.
- Use a Vite environment variable for configurable API base URL.
- Never place backend secrets in `VITE_*`.
- Never log passwords, tokens, or sensitive request bodies.
- Do not expose persisted password data to UI models.
- Do not invent endpoint paths, auth headers, response envelopes, or error schemas when unresolved.
- Keep server-side paging/sorting parameters typed and explicit.
- Avoid service functions that manipulate router/UI state.

Read [api-pattern.md](references/api-pattern.md) when implementing query or error handling.
