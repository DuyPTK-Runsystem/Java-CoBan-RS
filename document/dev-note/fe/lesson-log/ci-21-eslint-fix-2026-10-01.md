# FE Lesson Log - GitHub Actions #21 ESLint Fix

## Related plan

- Developer Plan: Short plan approved in this conversation; no separate plan artifact was created.
- User Approval: User requested the fix after the six ESLint warnings and affected files were identified.

## Actual scope

- Corrected Vue template indentation in `LessonLogAmendDialog.vue` and `LessonLogEntryDialog.vue`.
- Changed the checkbox `<input>` in `LessonLogSettingsView.vue` to HTML void-element syntax accepted by the configured ESLint rule.
- No runtime behavior, lint rules, API contracts, or dependencies changed.

## Files changed

- `FE/src/components/lesson-log/LessonLogAmendDialog.vue` - fixed two `vue/html-indent` warnings.
- `FE/src/components/lesson-log/LessonLogEntryDialog.vue` - fixed three `vue/html-indent` warnings.
- `FE/src/views/lesson-log/LessonLogSettingsView.vue` - fixed one `vue/html-self-closing` warning.

## Validation

- `npm.cmd run lint` - PASS.
- `npm.cmd run test` - PASS, 116 files and 627 tests.
- `npm.cmd run test:coverage` - PASS, 116 files and 627 tests; 86.2% statements/lines, 75.51% branches, 70.19% functions.
- `npm.cmd run build` - PASS (`vue-tsc --noEmit` and Vite production build).
- `git diff --check` - PASS.

## Deviations and remaining items

- No implementation deviation. Existing Vue warnings appeared in Lesson Log tests, but all tests passed.
- GitHub Actions run #21 is still the historical run for commit `6a12323`; this local fix has not been pushed, and that run was not rerun.
- No remaining local validation blockers.

## Next steps

- After separate authorization to push, verify the resulting GitHub Actions run for remote CI confirmation.
