# Dev Note: Student UI Date Format, Input Examples và Storybook

## 1. Related Developer Plan and approval

- Plan: `document/dev-impl-plan/fe/016-student-ui-date-format-input-examples-storybook-2026-08-19.md`.
- Approved by user on 2026-08-19.

## 2. Actual scope completed

- Student Birthday DatePicker at Add/Edit and Search now presents `dd-mm-yyyy`; the
  Student table converts API date-only values to the same presentation format.
- Added the requested example placeholders for Student code, Student name, Birthday
  and Average score.
- Add mode Student code is editable. On blur, an input of 1–7 digits is normalized to
  `STU` plus seven digits using left-zero padding; malformed codes show an inline
  message and cannot be saved. Edit mode displays the code in a disabled textbox and
  keeps Generate Code disabled.
- Added deterministic Storybook stories for StudentForm, StudentSearchForm and
  StudentTable; no story calls a backend, router or session boundary.
- Amendment 16.1: partial numeric Student code and partial `STU` code no longer show
  a warning while typing; warning remains immediate for overflow/invalid input and
  full validation runs on blur/Save. Address now has its requested example text and
  Average score is client-validated in the inclusive range `0–10`.

## 3. Files changed

### Student presentation and tests

- `FE/src/components/StudentForm.vue`, `StudentSearchForm.vue`, `StudentTable.vue`.
- `FE/src/utils/studentDate.ts`, `FE/src/utils/studentDate.spec.ts`.
- `FE/src/components/StudentForm.spec.ts`.
- `FE/src/test/stubs/ButtonStub.vue` to accurately represent the disabled prop used by
  StudentForm tests.
- `FE/src/styles.css` for the small format-hint presentation.

### Storybook

- `FE/src/components/StudentForm.stories.ts`.
- `FE/src/components/StudentSearchForm.stories.ts`.
- `FE/src/components/StudentTable.stories.ts`.

### Documentation

- Plan 016 and FE/project Developer Plan summaries.
- This Dev Note and FE/project Dev Note summaries.

## 4. Implementation decisions

- `studentDate.ts` reformats the three date-only string parts directly instead of
  constructing a JavaScript Date, so table rendering cannot shift a birthday by
  timezone.
- Student code is normalized only when the blur value consists exclusively of 1–7
  digits. Other values are retained unchanged and receive validation feedback, so the
  UI does not silently reinterpret a malformed value.
- The Add textbox intentionally has no `maxlength=10`; an input longer than seven
  digits after `STU` remains visible long enough for the required inline warning.
- The existing Create API remains authoritative for uniqueness; this task adds format
  UX only.

## 5. Validation

| Command | Result |
|---|---|
| `npm run lint` | PASS |
| `npm run test` | PASS — 11 test files, 32 tests |
| `npm run test:coverage` | PASS — 11 test files, 32 tests; 94.64% statements, 82.69% branches, 80% functions, 94.64% lines |
| `npm run build` | PASS |
| `npm run build-storybook` | PASS |

## 6. Deviations from Developer Plan

- Added the small pure utility `FE/src/utils/studentDate.ts` and its direct test,
  rather than coupling date-format testing to PrimeVue DataTable internals. It keeps
  the approved StudentTable behavior and scope unchanged.

## 7. Known warnings and remaining risks

- `npm run build-storybook` still emits pre-existing/non-blocking warnings about
  PrimeVue package discovery, `eval` in Storybook runtime, and a large Docs chunk;
  the command completed with exit code 0.
- API uniqueness remains enforced by the backend; a manually entered, correctly
  formatted duplicate is rejected only by the existing Create API response.
- Average score is also enforced authoritatively by backend Plan 017.

## 8. Next steps

- Optional manual review: open the three Student Storybook groups and exercise the
  blur normalization in Add mode against a running backend.
