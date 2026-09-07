# Frontend Agent Rules — Quality and Documentation

[← Back to `FE/AGENTS.override.md`](../AGENTS.override.md)

## Storybook

Maintain existing Storybook coverage when changing components that already have stories.

Stories must not require a live backend.

Add new stories only when they materially help validate a reusable component state; do not create stories mechanically for every page.

## Validation before completion

Inspect `FE/package.json` and run the relevant existing scripts.

Mandatory quality gates before reporting frontend source work as complete:

```bash
npm run lint
npm run test
npm run test:coverage
npm run build
```

Use the exact configured script names.

If a mandatory script is missing, the task is blocked and must not be reported as complete.

Also run relevant type checks and Storybook validation when configured and affected by the change.

Read command/test/coverage output as evidence.

Do not manually create, edit, rewrite, delete, or patch generated report artifacts.

A failed, skipped, or unrun mandatory gate must be reported as `FAIL` or `BLOCKED`, never as success.

Documentation-only changes do not require FE build/test gates unless they alter executable FE configuration; still run repository/document validation required by the approved plan.

## Documentation maintenance

When a backend contract used by FE changes, update the relevant documentation and FE boundary together.

At minimum review:

```text
document/application-doc/v2/FrontendApiGuide.md
FE/src/types/**
FE/src/services/**
affected tests
```

Do not let v1 API documentation become the implicit contract for a new v2 module.
