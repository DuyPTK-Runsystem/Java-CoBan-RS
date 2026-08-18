---
name: storybook-impl
description: Create, update, or review Storybook stories for Vue components, especially required LoginForm and RegisterForm stories, using deterministic state without a live backend.
---

# Implement Storybook stories

## Procedure

1. Inspect the component public contract and existing Storybook configuration/version.
2. Cover meaningful visual/interaction states rather than duplicating trivial stories.
3. Provide deterministic props and callbacks; mock API/router boundaries instead of requiring Spring Boot.
4. Keep stories synchronized when component props, emits, validation states, or behavior change.
5. Run the available Storybook validation/build command before reporting success.

## Required project coverage

```text
LoginForm
├── Default
├── Filled
└── ValidationError

RegisterForm
├── Default
├── Filled
├── PasswordMismatch
└── ValidationError
```

## Rules

- Do not call the live backend from stories.
- Do not hard-code secrets or real credentials.
- Prefer component-level state and mocked callbacks.
- Use the Storybook API matching the installed version.
- Add extra stories only for behavior that materially helps review or testing.
- If Storybook is not initialized yet, do not invent configuration incompatible with the installed Vue/Vite setup.

Read [story-pattern.md](references/story-pattern.md) for story intent.
