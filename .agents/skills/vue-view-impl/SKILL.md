---
name: vue-view-impl
description: Create, update, or review Vue route-level views that orchestrate components, services, route parameters, navigation, page state, and user-facing API errors.
---

# Implement a Vue view

## Procedure

1. Read the screen/module requirements and inspect router, services, types, and reused components.
2. Define initial load, route params/query, page state, service calls, success navigation, and failure behavior.
3. Keep reusable presentation details in components; keep orchestration in the view.
4. Represent loading, empty, success, validation, and API-error states intentionally.
5. Keep route/query state synchronized when the approved design requires shareable search/sort/page state.
6. Validate the complete user flow, not only isolated rendering.

## Rules

- Name route-level files `*View.vue`.
- Use Vue Router instead of direct URL manipulation.
- Do not call backend APIs directly from template expressions.
- Avoid duplicating service request construction across views.
- Do not use client route guards as backend authorization.
- Preserve required navigation:
  `Register -> Login -> Student List -> Add/Edit`, and `Logout -> Login`.
- Student List search/sort/pagination must be server-side; default page size is `10`.
- Back from Student Form returns without saving.
- Do not invent auth, score-range, or student-code rules still marked `TBD`.

Read [view-flows.md](references/view-flows.md) for project-specific flows.
