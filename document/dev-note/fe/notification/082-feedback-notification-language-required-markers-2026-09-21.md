# 082 FE: Feedback notification, language and required markers

## Approval and scope

- Approval: direct user request to fix the previously audited UI issues; no new application-document behavior was introduced.
- Scope: notification tone contract and mutation feedback, Vietnamese fallback/user-facing copy, and required-field markers/validation across the audited FE forms.
- Existing worktree changes in `BE/BaiTap-RS/src/main/resources/application.properties` and `document/dev-impl-plan/summary/081-seed-data-v2-v3-2026-09-18.md` were preserved.

## Implemented

- Replaced incorrect `FormAlert type` attributes with the supported `tone` contract, including success feedback in timetable settings.
- Cleared stale messages before mutations and separated successful mutation from reload failure in timetable settings.
- Added per-recipient email outcome presentation: green only when all are `SENT`, yellow for mixed/pending, red when all fail; backend-returned recipient errors remain visible.
- Localized shared PageState/API fallbacks, auth and calculation user-facing copy, status labels, and security-facing messages.
- Added a shared red `.required-mark`, `aria-required`, missing validation/error rendering, and conditional markers for the audited required fields.
- Updated affected unit-test expectations and added notification outcome/FormAlert regressions.

## Validation

- `npm run test`: PASS — 111 test files, 597 tests.
- `npm run build`: PASS.
- `npm run lint`: FAIL — 2 pre-existing warnings in `FE/src/components/notification/NotificationComposer.spec.ts` (`vue/one-component-per-file`); no lint errors.
- `git diff --check`: PASS.
- Browser/live SMTP verification: NOT RUN after code changes.

## Risks and next steps

- The full FE lint gate remains blocked by the two existing test-file warnings.
- Backend delivery status still represents successful mail transport handoff, not proof that a recipient read the message.
