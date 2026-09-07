---
name: frontend-module-impl
description: Define, scaffold, implement, or review a Vue 3 feature/module in FE with views, reusable components, API services, routing, types, and relevant stories. Use when adding a frontend feature, screen flow, or module structure.
---

# Implement a frontend module

## Workflow

1. Read `FE/AGENTS.override.md`, relevant application docs, backend contract, and approved Developer Plan.
2. Inspect neighboring FE code and preserve established Vue/PrimeVue/TypeScript conventions.
3. Define screen flow, API boundary, state ownership, validation, loading/error states, and reusable component boundaries.
4. Implement dependency direction: `view -> component/service -> backend API`; do not scatter HTTP calls through presentation components.
5. Use `@vue-component-impl`, `@vue-view-impl`, `@frontend-api-service`, and `@storybook-impl` where applicable.
6. Run `@frontend-validation` before reporting completion.

## Default structure

```text
src/
├── components/
├── views/
├── services/
├── router/
├── types/
└── utils/
```

Keep the structure proportional to the feature. Do not create stores, composables, domain/use-case layers, or generic abstractions unless existing code or the approved plan requires them.

## Boundaries

- Views own page orchestration, route state, loading/error state, and service calls.
- Components focus on rendering and interaction through typed props/emits.
- Services own HTTP communication and API-specific mapping.
- Types describe API/UI contracts, not database entities mechanically.
- Use PrimeVue for student-management UI where required.
- Do not invent requirements marked `TBD`.

Read [module-example.md](references/module-example.md) when concrete file placement is needed.
