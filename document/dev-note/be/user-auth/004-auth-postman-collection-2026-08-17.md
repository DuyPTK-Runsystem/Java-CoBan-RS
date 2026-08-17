# Dev Note: Auth Postman Collection

## Related Developer Plan

- Plan: none.
- Approval status: Direct user request on 2026-08-17 to make a Postman collection.

## Actual Scope Completed

- Created a new Postman Collection v2.1 JSON for the current auth API.
- Added requests for register, login, account, and logout.
- Added collection variables for `baseUrl` and `accessToken`.
- Added a login test script that stores `data.access_token` into `{{accessToken}}`.
- No backend source code or API behavior was changed.

## Files Changed

### Postman

- `document/postman/Java-CoBan.postman_collection.json`

### Dev Note

- `document/dev-note/be/user-auth/004-auth-postman-collection-2026-08-17.md`
- `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`

## Decisions

- Default collection path is `document/postman/Java-CoBan.postman_collection.json`.
- `baseUrl` defaults to `http://localhost:8081`, matching `server.port=${SERVER_PORT:8081}`.
- Public endpoints use `noauth`: register and login.
- Protected endpoints use bearer token variable `{{accessToken}}`: account and logout.
- Request examples use safe placeholder credentials and do not copy local `.env` or config secrets.
- Response examples follow the current `RestResponse` wrapper and `@ApiMessage` messages.

## Validation

| Command | Result | Notes |
|---|---|---|
| `python3 -m json.tool document/postman/Java-CoBan.postman_collection.json` | PASS | Collection JSON is valid. |
| `rg -n "noVGO4KXf|123456|JWT_SECRET|SPRING_DATASOURCE|root|java_coban" document/postman/Java-CoBan.postman_collection.json` | PASS | No matches found; `rg` exit code 1 is expected for this negative scan. |
| `rg -n '"schema"|"/api/v1/auth"|baseUrl|accessToken' document/postman/Java-CoBan.postman_collection.json` | PASS | Confirmed v2.1 schema, auth endpoints, and variables. |
| Backend test/build | NOT RUN | Not applicable; no backend code changed. |

## Deviations

- None.

## Risks

- Collection was generated from code and docs, not exercised against a running server.
- Future endpoint changes require updating the collection.

## Next Steps

- Import the collection into Postman and run register/login before protected requests.
