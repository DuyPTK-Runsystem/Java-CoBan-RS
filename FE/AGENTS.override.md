# AGENTS.override.md

## Scope

Applies to all work under `FE/`.
Follow this file together with `.codex/AGENTS.md` and `.codex/AGENTS_DETAIL.md`.

## Source of truth

Before frontend work, read the relevant files:

- `document/application-doc/ApplicationContext.md`
- `document/application-doc/modules/UserModule.md`
- `document/application-doc/modules/StudentModule.md`
- `document/application-doc/DataStructure.md` when data constraints matter
- the approved Developer Plan, if any

Do not invent behavior for requirements marked `TBD`.

## Stack

Use:

- Vue 3 + Vite + TypeScript
- PrimeVue for student-management UI
- Vue Router
- Storybook for `LoginForm` and `RegisterForm`

Do not add Pinia/Vuex, Axios, Bootstrap, Tailwind, another UI framework, or other major dependencies unless required and approved.

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
- request/response models explicitly typed.

Do not put raw API calls throughout presentation components.

For new Vue code, prefer `<script setup lang="ts">`, Composition API, explicit props/emits types, and avoid `any`.

## Functional rules

Required flow:

```text
Register -> Login -> Student List -> Add/Edit Student
                    |
                    +-> Logout -> Login
```

Frontend validation must follow documented rules, but backend validation remains authoritative.

Student List must support search, server-side sorting, server-side pagination, add, edit, delete confirmation, and default page size `10`.

Use PrimeVue for student screens, especially DataTable, form controls, buttons, pagination, and delete confirmation.

Student Form:

- Add: hide Student Id; Student Code read-only; Generate Code enabled.
- Edit: Student Id and Student Code read-only; Generate Code disabled.
- Back returns without saving.
- Generated code starts with `STU`.

Do not invent unresolved rules such as:

- auth mechanism;
- exact API paths if backend has not established them;
- student-code numeric format or uniqueness;
- `averageScore` range.

## Storybook

Maintain stories for:

- `LoginForm`: Default, Filled, ValidationError
- `RegisterForm`: Default, Filled, PasswordMismatch, ValidationError

Stories must not require a live backend.

## UX and security

Handle loading, empty, success, validation, and API-error states intentionally.

Do not:

- log/store plaintext passwords unnecessarily;
- embed secrets in frontend code;
- rely on client validation or route guards as security enforcement;
- expose backend secrets through `VITE_*`.

Use Vite environment variables for configurable public frontend values such as API base URL.

## Validation before completion

Inspect `FE/package.json` and run the relevant existing scripts.

The following are mandatory quality gates before reporting frontend work as complete:

```bash
npm run lint
npm run test
npm run test:coverage
npm run build
```

- Use the exact test and coverage script names configured by `FE/package.json`; do not invent commands.
- If a test or coverage script is missing, the frontend task is blocked and must not be reported as complete.
- Also run relevant type checks and Storybook validation when configured.
- Read test/coverage reports and command output as evidence. Do not manually create, edit, rewrite, delete, or patch report artifacts.
- A failed, skipped, or unrun mandatory gate must be reported as `FAIL` or `BLOCKED`, never as success.

Do not claim completion if required checks fail.

## Keep it simple

This is a training project. Do not over-engineer it.

Avoid speculative layers, generic design systems, micro-frontends, SSR/Nuxt, unnecessary stores, or abstractions without a concrete need.

Prefer small, readable changes that match the approved plan and existing project documentation.
