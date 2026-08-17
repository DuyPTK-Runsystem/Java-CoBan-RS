# Dev Note: Postman Collection Skill

## Related Developer Plan

- Plan: `document/dev-impl-plan/be/workflow-skill/003-postman-collection-skill-2026-08-17.md`
- Approval status: Approved by user via agent on 2026-08-17.

## Actual Scope Completed

- Created project skill `.agents/skills/postman-collection/`.
- Added workflow instructions for user-requested Postman collection creation/update.
- Added UI metadata in `agents/openai.yaml`.
- Updated Developer Plan status and summaries for plan 003.
- No Postman collection was created in this task.
- No backend source code or API behavior was changed.

## Files Changed

### Skill

- `.agents/skills/postman-collection/SKILL.md`
- `.agents/skills/postman-collection/agents/openai.yaml`

### Developer Plan

- `document/dev-impl-plan/be/workflow-skill/003-postman-collection-skill-2026-08-17.md`
- `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`

### Dev Note

- `document/dev-note/be/workflow-skill/003-postman-collection-skill-2026-08-17.md`
- `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`

## Decisions

- Skill triggers only when the user explicitly asks for Postman collection work.
- Existing `*.postman_collection.json` files should be preserved and updated by method + path identity.
- If no collection exists, the default target path is `document/postman/Java-CoBan.postman_collection.json`.
- API facts should be gathered from Dev Notes, Developer Plans, current context, and Spring code.
- Collection examples must use variables such as `{{baseUrl}}` and `{{accessToken}}`.
- Real secrets from `.env` or local config must not be copied into the collection.
- The skill must not create a separate report document after collection work.

## Validation

| Command | Result | Notes |
|---|---|---|
| `python3 /home/duyptk/.codex/skills/.system/skill-creator/scripts/init_skill.py postman-collection --path .agents/skills --interface display_name="Postman Collection" --interface short_description="Create or update Postman collections." --interface default_prompt="Create or update the Postman collection for this project."` | PASS | Needed escalated execution because `.agents` was read-only in sandbox. |
| `python3 /home/duyptk/.codex/skills/.system/skill-creator/scripts/quick_validate.py .agents/skills/postman-collection` | PASS | Output: `Skill is valid!` |
| Backend test/build | NOT RUN | Not applicable; no backend code changed. |

## Deviations

- None.

## Risks

- The skill has not yet been exercised on a real collection update request.
- If multiple collections exist and module context is unclear, the agent must ask before updating.

## Next Steps

- Use `postman-collection` only when the user explicitly requests Postman collection creation or update.
