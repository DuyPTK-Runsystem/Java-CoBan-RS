# Frontend Agent Rules — Legacy Student UI

[← Back to `FE/AGENTS.override.md`](../AGENTS.override.md)

## Scope

Applies to maintenance or extension of the existing Student profile screens in
`FE/` that still use the v1 Student API.

Use this file together with the foundation and API-boundary rules. It does not
authorize using v1 behavior for a new v2 academic workflow.

## Source and boundary

For this legacy UI, use the current v1 FE/API contract and the approved plans
for the existing Student screens:

- `document/application-doc/v1/FrontendApiGuide.md`;
- `document/application-doc/v1/modules/StudentModule.md`;
- the approved FE Student plans under `document/dev-impl-plan/fe/student/`.

When a task crosses into enrollment, attendance, scorebook, transcript or other
v2 academic behavior, route back through the v2 requirement and API guide.

## Existing user flow

```text
Register → Login → Student List → Add/Edit Student
                       |
                       +→ Logout → Login
```

## Student List

The existing Student List must support:

- search;
- server-side sorting;
- server-side pagination;
- add and edit navigation;
- delete confirmation;
- default page size `10`.

Use PrimeVue DataTable, form controls, buttons, paginator and confirmation UI
consistently with the existing project.

## Student Form

- Add mode hides Student Id.
- Student Code is read-only in the form and Generate Code is enabled in Add mode.
- Edit mode keeps Student Id and Student Code read-only.
- Generate Code is disabled in Edit mode.
- Back navigates away without saving.
- Generated Student Code follows the current v1 backend contract and starts with
  `STU`.

`averageScore` remains compatibility data in this legacy profile form. It must
not be presented as the official v2 transcript result.

## Storybook

Maintain deterministic stories that do not require a live backend:

- `LoginForm`: `Default`, `Filled`, `ValidationError`;
- `RegisterForm`: `Default`, `Filled`, `PasswordMismatch`, `ValidationError`;
- existing Student component stories when those components are changed.

## UX and security

Handle loading, empty, success, validation and API-error states intentionally.
Keep backend validation authoritative and do not treat client validation,
hidden buttons or route guards as authorization.
