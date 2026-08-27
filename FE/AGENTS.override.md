# AGENTS.override.md

## Purpose

This file is the **frontend routing file** for work under `FE/`.

Do not load every frontend rule file for every task. Read the mandatory base, then route to only the rule files relevant to the current work.

This file applies together with:

- `.codex/AGENTS.md`
- `.codex/AGENTS_DETAIL.md`

When older frontend guidance conflicts with Application Documentation v2, this routing structure wins.

## Mandatory source order

For new frontend work, **Application Documentation v2 is the business source of truth**.

Read in this order:

1. `document/application-doc/v2/ApplicationContext.md`
2. `document/application-doc/v2/RequirementBaseline.md`
3. the relevant module under `document/application-doc/v2/modules/`
4. relevant approved CRs under `document/application-doc/v2/change-request/`
5. `document/application-doc/v2/FrontendApiGuide.md`
6. the relevant data-model file when identifiers, persistence, lifecycle, status, or calculation matter
7. the approved FE Developer Plan for the current task, if one exists

Do **not** use `document/application-doc/v1/**` to fill gaps in v2. Read v1 only when the task explicitly concerns legacy compatibility.

## Rule routing

### Always read before FE implementation

- [`agent-rules/00-foundation.md`](agent-rules/00-foundation.md)

This defines source precedence, contract discipline, stack, structure, module boundaries, and anti-overengineering rules.

### Read by task

| Task touches | Read |
|---|---|
| Login, session, account, authorization UX, router, sidebar/menu visibility | [`agent-rules/01-auth-routing-security.md`](agent-rules/01-auth-routing-security.md) |
| Existing Student profile CRUD/search/sort/page/form UI using v1 | [`agent-rules/05-legacy-student-ui.md`](agent-rules/05-legacy-student-ui.md) |
| Student, academic structure, teacher, assignment, attendance, scorebook, transcript, retake/calculation behavior | [`agent-rules/02-domain-rules.md`](agent-rules/02-domain-rules.md) |
| API DTOs, enums, dates, pagination, validation/error handling, transport mapping | [`agent-rules/03-api-data-boundaries.md`](agent-rules/03-api-data-boundaries.md) |
| Tests, coverage, build, Storybook, Dev Note, documentation updates, completion reporting | [`agent-rules/04-quality-documentation.md`](agent-rules/04-quality-documentation.md) |

A task may require more than one routed file. Read only the files whose scope materially affects the task.

## Critical blockers that must not be guessed

### Role discovery

The current login/account response and JWT do **not** expose frontend roles.

Until an approved backend contract exposes roles/capabilities:

- do not infer `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`, or `STUDENT`;
- do not infer GVCN/GVBM from account role strings;
- do not implement authoritative role-aware navigation from guessed data.

Backend authorization remains authoritative.

### Missing v2 contract

Requirement v2 does not authorize the frontend to invent:

- endpoints;
- DTO fields;
- enum values;
- lifecycle mutations;
- role claims;
- calculation behavior.

If a required contract does not exist, follow the approved plan/CR that resolves it or report the task as blocked.

## Completion routing

Before reporting FE source work complete, always read:

- [`agent-rules/04-quality-documentation.md`](agent-rules/04-quality-documentation.md)

and run the mandatory configured quality gates.

## Routing principle

```text
AGENTS.override.md
        ↓
00-foundation                     always
        ↓
task-specific rule file(s)        only when relevant
        ↓
Requirement module / CR
        ↓
FrontendApiGuide router
        ↓
relevant API contract file(s)
        ↓
approved FE Dev Plan
```

The goal is to keep agent context small without weakening source-of-truth discipline.
