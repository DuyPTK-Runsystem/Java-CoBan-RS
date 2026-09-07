# Dev Note: Frontend API Catalog

## Related Developer Plan and approval

- Plan: `document/dev-impl-plan/fe/tooling/049-frontend-api-catalog-2026-08-26.md`.
- Approved by user on 2026-08-26 through the request to aggregate APIs for FE.

## Actual scope completed

- Created one v1 API catalog for FE lookup.
- Consolidated Auth and Student endpoint paths, auth headers, query parameters,
  request bodies, response shapes and error handling.
- Documented FE/backend field-name mappings, date serialization, pagination,
  server-side sorting, CSV download and `204 No Content` behavior.
- Linked the catalog from the v1 README and Application Context.

## Files changed

- `document/application-doc/v1/FrontendApiGuide.md`.
- `document/application-doc/v1/README.md`.
- `document/application-doc/v1/ApplicationContext.md`.
- `document/dev-impl-plan/fe/tooling/049-frontend-api-catalog-2026-08-26.md`.
- FE Dev Plan and Dev Note summaries.

## Important decisions

- The document is an API catalog, not a new frontend abstraction or implementation
  guide.
- Existing FE code and v1 module contracts are the sources for endpoint details.
- Current FE behavior is called out where it is narrower than the general contract,
  especially Student sorting and update payload fields.
- No speculative v2 academic endpoints were added because the current FE scope uses
  the v1 User and Student screens.

## Validation

| Check | Result |
|---|---|
| `git diff --check` | PASS |
| Internal-link target check for new catalog | PASS |
| FE lint/test/build | NOT RUN — documentation-only change; no FE source changed |

## Deviations and remaining risks

- No deviation from the approved documentation scope.
- The catalog mirrors the current v1 FE implementation. If backend contracts change,
  update this catalog and the corresponding FE service tests together.

## Next steps

- Use this catalog as the contract index when adding the next FE module.
- Verify endpoint responses against a running backend during manual integration testing.
