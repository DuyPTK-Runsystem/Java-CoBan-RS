---
name: vue-component-impl
description: Create, update, or review reusable Vue 3 TypeScript components using Composition API, typed props/emits, PrimeVue where required, validation UI, and clear presentation boundaries.
---

# Implement a Vue component

## Procedure

1. Inspect neighboring components, project docs, installed PrimeVue version, and the parent view contract.
2. Define props, emits, local UI state, validation display, disabled/loading states, and accessibility behavior.
3. Prefer `<script setup lang="ts">` and explicit TypeScript types.
4. Keep API calls and route orchestration outside reusable presentation components unless the existing project convention clearly differs.
5. Reuse PrimeVue components instead of rebuilding equivalent controls for student-management screens.
6. Add/update Storybook coverage when the component is required or useful in isolation.

## Rules

- Use PascalCase component filenames.
- Prefer props down and events up.
- Avoid `any` without a concrete reason.
- Do not mutate props.
- Do not duplicate backend entities as component state when a smaller UI model is sufficient.
- Show field errors near the relevant controls.
- Preserve keyboard-operable buttons/links and associated labels.
- Prevent duplicate submit while an async submit is pending when applicable.
- Do not add speculative abstractions or wrappers around PrimeVue.

For forms, keep client validation aligned with documented rules while treating backend validation as authoritative.

Read [component-patterns.md](references/component-patterns.md) for form/table examples.
