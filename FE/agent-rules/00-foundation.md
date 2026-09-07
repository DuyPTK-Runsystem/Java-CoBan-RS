# Frontend Agent Rules — Foundation

[← Back to `FE/AGENTS.override.md`](../AGENTS.override.md)

## Scope

Applies to all work under `FE/`.

Follow this file together with:

- `.codex/AGENTS.md`
- `.codex/AGENTS_DETAIL.md`

This file overrides older frontend guidance when the older guidance conflicts with Application Documentation v2.

## Source of truth

For new frontend work, **v2 is the application source of truth**.

Read in this order:

1. `document/application-doc/v2/ApplicationContext.md`
2. `document/application-doc/v2/RequirementBaseline.md`
3. the relevant requirement module under `document/application-doc/v2/modules/`
4. relevant approved Change Requests under `document/application-doc/v2/change-request/`
5. `document/application-doc/v2/FrontendApiGuide.md`
6. the relevant data-model document under `document/application-doc/v2/data-model/` when persistence, identifiers, status, calculation, or lifecycle constraints matter
7. the approved FE Developer Plan for the current task, if one exists

Do **not** use `document/application-doc/v1/**` to fill gaps in v2.

Read v1 documentation only when the task explicitly concerns legacy compatibility or an existing v1 endpoint that v2 intentionally keeps. In that case:

- treat v1 as compatibility evidence, not the business source of truth;
- do not copy v1 behavior into a new v2 module unless v2 or the current backend contract explicitly preserves it.

When sources conflict, use the priority defined by `document/application-doc/v2/ApplicationContext.md`.

Do not invent behavior for requirements marked `TBD`, `Needs confirmation`, or for missing backend contracts.

## Frontend/backend contract rule

For HTTP integration, use the **implemented backend contract** documented in:

- `document/application-doc/v2/FrontendApiGuide.md`;
- the current controller/request/response DTO when the guide explicitly says the contract must be verified from source.

Requirement documents define intended business behavior. They do not authorize the frontend to invent an endpoint, request field, response field, enum value, role claim, or mutation that the backend does not expose.

If Requirement v2 and the implemented API differ:

1. keep the difference visible;
2. do not silently normalize the frontend to an imaginary contract;
3. follow the approved Developer Plan or Change Request that resolves the difference;
4. report the task as blocked when the missing contract is required for correctness.

## Stack

Use:

- Vue 3
- Vite
- TypeScript
- PrimeVue
- Vue Router
- Storybook where already used
- browser `fetch` through typed service modules

Do not add Pinia/Vuex, Axios, Bootstrap, Tailwind, another UI framework, SSR/Nuxt, a micro-frontend framework, or other major dependencies unless required by an approved plan.

Prefer Vue/browser APIs and existing dependencies.

## Structure

Default structure:

```text
src/
├── components/
├── views/
├── services/
├── router/
├── types/
└── utils/
```

Keep:

- page orchestration in `views`;
- reusable UI in `components`;
- backend calls in `services`;
- API request/response models explicitly typed in `types` or next to the service when the type is module-local;
- formatting and pure adapters in `utils` when reuse is justified.

Do not put raw API calls throughout presentation components.

For new Vue code:

- prefer `<script setup lang="ts">`;
- use Composition API;
- type props and emits explicitly;
- avoid `any`;
- keep API DTO types separate from editable form state when their semantics differ.

Do not introduce a generic repository/client abstraction unless at least two real modules need the same behavior and the abstraction reduces duplication without hiding HTTP semantics.

## v2 module boundaries

The target v2 frontend covers these business areas incrementally:

```text
auth
academic structure
student
enrollment
teacher
teaching assignment
calendar
attendance
scorebook
score change
semester completeness / notifications
transcript
retake
calculation operations
```

Implement one approved module/plan at a time.

Do not create placeholder APIs or speculative screens for later modules merely to make navigation look complete.

## Keep it simple

This is a training project.

Prefer:

- small typed service modules;
- explicit DTOs;
- direct route/view/component relationships;
- feature-focused plans;
- readable validation and state handling.

Avoid speculative layers, generic design systems, micro-frontends, SSR, unnecessary stores, and abstractions without a concrete requirement.
