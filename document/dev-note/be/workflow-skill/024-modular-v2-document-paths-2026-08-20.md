# Dev Note 024: Modular v2 Document Paths in Skills

## Related Developer Plan and approval

- Developer Plan: none; this was a direct documentation-path update requested by the user on 2026-08-20.
- Approval: user request authorizes the scoped update.

## Actual scope completed

- Audited repository-managed skills for literal `document/application-doc/v1/` references.
- Preserved all v1 application-document routes in `start-agent-session`.
- Added a v2 application-document section rooted at the modular v2 document package.
- Added a mandatory version-selection gate: when the prompt does not identify v1 or v2, the agent stops work that depends on application documents and asks the user which version applies.

## Files changed

| Purpose | Paths |
|---|---|
| Skill documentation routing | `.agents/skills/start-agent-session/SKILL.md` |
| Implementation record | `document/dev-note/be/workflow-skill/024-modular-v2-document-paths-2026-08-20.md` |
| Dev Note indexes | `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`, `document/dev-note/summary/DEV_NOTE_SUMMARY.md` |

## Important decisions

- v1 remains available at `document/application-doc/v1/`.
- v2 is available at `document/application-doc/v2/`.
- The agent must not choose either version by default; user confirmation is required when the prompt is ambiguous.
- Existing general `document/` paths for Dev Notes, Developer Plans and Postman collections remain unchanged because they are not application-document references.

## Validation

| Command or check | Result |
|---|---|
| Verify every listed v1 and v2 application-doc path resolves | PASS |
| `python3 /home/duyptk/.codex/skills/.system/skill-creator/scripts/quick_validate.py .agents/skills/start-agent-session` | PASS |
| `git diff --check` | PASS |

## Deviations, blockers and next steps

- The initial update replaced v1 routes; the user corrected the scope to preserve them, so this note records the final additive result.
- No Developer Plan existed for this direct, documentation-only request.
- No backend or frontend validation was run because no application code or build configuration changed.
- No blockers or required next steps.
