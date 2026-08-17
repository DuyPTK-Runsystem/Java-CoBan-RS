---
name: postman-collection
description: Create or update Postman collections only when the user explicitly asks for a Postman collection, API collection, or Postman update. Use Dev Notes, Developer Plans, current context, and codebase changes to create a new Postman Collection v2.1 JSON or update an existing one while preserving existing collection structure and avoiding secrets.
---

# Postman Collection

Use this skill only after a direct user request to create or update a Postman collection.

## Workflow

1. Identify the target collection:
   - Search for `*.postman_collection.json` under `document/`, `BE/`, and the repository root.
   - If exactly one relevant collection exists, update it.
   - If multiple collections exist, choose the one matching the requested module or context; ask the user when ambiguous.
   - If none exists, create `document/postman/Java-CoBan.postman_collection.json` unless the user provides another path.
2. Gather API facts:
   - Read relevant Dev Notes in `document/dev-note/`.
   - Read related Developer Plans in `document/dev-impl-plan/` when useful.
   - Inspect relevant Spring controllers, request/response DTOs, config, and `application.properties`.
   - Prefer code over docs when they conflict; mention important conflicts only in the final response.
3. Build or update Postman Collection v2.1 JSON:
   - Set `info.schema` to `https://schema.getpostman.com/json/collection/v2.1.0/collection.json`.
   - Use `{{baseUrl}}`; derive its initial value from `server.port` when available, for example `http://localhost:8081`.
   - Use `{{accessToken}}` for bearer token examples.
   - Never copy real secrets, tokens, database credentials, or passwords from `.env` or local config into the collection.
   - Group requests by module or feature folder.
   - Identify existing requests by method and path, then update only the affected request.
   - Preserve existing collection IDs, folders, descriptions, variables, auth, pre-request scripts, tests, and response examples unless the requested change requires editing them.
   - Generate safe example request bodies from request DTO fields.
   - Add response examples only when confidently known from code, Dev Note, or explicit user context.
4. Validate the file:
   - Ensure the saved collection is valid JSON.
   - Ensure it uses Postman Collection v2.1 schema.
   - Search the collection for leaked `.env` values or obvious secrets.
   - Do not call live APIs unless the user explicitly asks.
5. Finish:
   - Save the collection file.
   - Do not create a separate report document.
   - In the final response, state whether the collection was created or updated and give the collection path.
