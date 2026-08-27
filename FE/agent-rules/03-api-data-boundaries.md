# Frontend Agent Rules — API and Data Boundaries

[← Back to `FE/AGENTS.override.md`](../AGENTS.override.md)

## API enum rule

TypeScript string unions must match the **current wire contract**, not a nearby requirement/data-model term.

`FrontendApiGuide.md` records known drift that is especially important for FE, including:

- `SubjectType`: current API uses `ACADEMIC | SKILL`;
- attendance exception values differ from some Requirement v2 wording;
- current `ScoreStatus` has no `NOT_ENTERED`;
- current calculation result source does not expose every value described by the target data model.

If an enum changes in backend source, update:

1. `FrontendApiGuide.md`;
2. FE TypeScript types;
3. service/component tests that depend on the enum.

## Date/time rules

Date-only values use `yyyy-MM-dd` on the API boundary.

Do not use `Date.toISOString()` for local date-only form values when it can introduce a timezone date shift.

Use explicit adapters for:

- `LocalDate` ↔ UI date;
- `LocalDateTime` display;
- query parameters.

The business timezone for current school rules is `Asia/Ho_Chi_Minh` where the requirement/CR specifies it. Do not silently reinterpret backend timestamps as arbitrary local dates.

## Pagination and list behavior

Use server-side pagination where the API supports it.

Respect endpoint-specific defaults documented in `FrontendApiGuide.md`; do not assume every endpoint uses the same page size.

Do not locally sort/page an already paged server result as if it were the complete dataset.

Keep filter semantics aligned with the backend query DTO.

## Error handling

Handle these states intentionally:

- loading;
- empty;
- success;
- validation error;
- `401`;
- `403`;
- `404`;
- `409`;
- background `IN_PROGRESS`;
- retryable operational failure where the API explicitly supports retry.

Backend validation and authorization remain authoritative.

When backend validation returns multiple messages, preserve enough information for the UI to identify the violated field/rule.

Do not expose raw internal stack traces or secrets.
