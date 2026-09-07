---
name: dev-note
description: Create or update project Dev Notes for implementation work. Use after any approved task that changes code, config, docs, workflow, skills, tests, or validation state; before final reporting; when resuming work and needing a factual implementation note similar to a Developer Plan but based on actual changes.
---

# Dev Note

Dev Note is the post-implementation companion to Developer Plan.

Developer Plan records intended scope before approval. Dev Note records what actually happened: files changed, decisions made, validation run, deviations from plan, blockers, and next steps.

## Required Timing

- Create or update a Dev Note after any approved task that changes code, config, docs, workflow, skills, tests, or validation state.
- Update the Dev Note before final reporting.
- When resuming a task, read the related Developer Plan and Dev Note before editing.
- Dev Note never replaces Developer Plan and never bypasses approval.

## Location

Use this folder structure:

```text
document/dev-note/
├── summary/
│   └── DEV_NOTE_SUMMARY.md
├── be/
│   ├── BE_DEV_NOTE_SUMMARY.md
│   └── <module>/
│       └── NNN-short-topic-yyyy-mm-dd.md
└── fe/
    └── FE_DEV_NOTE_SUMMARY.md
```

For backend workflow/skill changes, use:

```text
document/dev-note/be/workflow-skill/
```

## Naming

- Use the related Developer Plan sequence number when one exists.
- Put the date at the end of the file name.
- Format:

```text
NNN-short-topic-yyyy-mm-dd.md
```

Example:

```text
002-dev-note-skill-workflow-2026-08-17.md
```

## Required Content

Every Dev Note must include:

1. Related Developer Plan path and approval status.
2. Actual scope completed.
3. Files changed, grouped by purpose.
4. Important implementation decisions.
5. Validation commands and actual result: `PASS`, `FAIL`, or `NOT RUN`.
6. Deviations from Developer Plan.
7. Known blockers, skipped checks, or remaining risks.
8. Next steps if any.

Keep it factual and concise. Prefer exact paths and commands over prose.

## Summary Updates

When creating or updating a detailed Dev Note, also update:

- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`
- the area summary, such as `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`

Use relative links that keep working after folder moves.
