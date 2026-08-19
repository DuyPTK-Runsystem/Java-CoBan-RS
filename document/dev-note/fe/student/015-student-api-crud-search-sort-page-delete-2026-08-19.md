# Dev Note: Student API, CRUD, Search, Sort, Page và Delete

## Related plan and approval

- Plan: `document/dev-impl-plan/fe/student/015-student-api-crud-search-sort-page-delete-2026-08-19.md`.
- Approved by user on 2026-08-19.

## Actual scope completed

- Added typed Student API calls for server-side list, detail, create, update, delete
  and code generation.
- Replaced local demo Student list/form state with API orchestration.
- Updated Student name UI validation to 35 characters and added API regression tests.

## Files changed

- `FE/src/types/student.ts`, `FE/src/services/studentApi.ts`,
  `FE/src/services/studentApi.spec.ts`.
- `FE/src/views/StudentListView.vue`, `FE/src/views/StudentFormView.vue`.
- `FE/src/components/StudentForm.vue`.
- This Dev Note and FE/project Dev Note summaries.

## Implementation decisions

- Views own API/loading/error/navigation state; components emit interactions only.
- List uses backend paging, filtering and sorting with page size 10.
- Date-only API values are serialized from local Date parts, not `toISOString()`.

## Validation

| Command | Result |
|---|---|
| `npm run lint` | PASS |
| `npm run test` | PASS — 18 tests |
| `npm run test:coverage` | PASS |
| `npm run build` | PASS |
| `npm run build-storybook` | PASS — Storybook static preview build completed. |

## Deviations and risks

- No functional deviation from Plan 015.
- View-level Student flow tests remain a follow-up risk; the typed service query/detail
  boundary is covered deterministically.
- Storybook retains pre-existing warnings about PrimeVue package discovery, runtime
  `eval` and a large Docs chunk; the command exits successfully.

## Next step

- Verify Student API against a running backend and complete manual UI flow review.
