---
name: frontend-validation
description: Run and evaluate frontend quality checks after FE changes using the scripts actually configured in FE/package.json. Use after frontend implementation and before final reporting.
---

# Frontend Validation

Validate frontend work under `FE/`.

## Procedure

1. Read `FE/package.json`; do not assume script names that are not present.
2. Run the relevant configured checks for the changed scope.
3. At minimum run lint and production build when corresponding scripts exist.
4. Run tests, type checks, and Storybook checks/build when configured and relevant.
5. Distinguish failures caused by the current task from existing baseline failures.
6. Fix only source/test/config changes inside the approved scope, then rerun the failed checks.

## Expected commands

Use only commands supported by `FE/package.json`, commonly:

```text
npm run lint
npm run build
npm run test
npm run type-check
npm run build-storybook
```

The names above are examples, not permission to invent missing scripts.

## Rules

- Do not disable lint/type/test rules merely to obtain PASS.
- Do not edit generated reports or build output to fake success.
- Do not claim PASS for commands not run.
- For `LoginForm`/`RegisterForm`, validate affected Storybook stories.
- For Student List, verify search/sort/pagination query behavior and delete confirmation.
- Report each relevant command as `PASS`, `FAIL`, or `NOT RUN`, with blockers stated explicitly.
