# Dev Note 096 — Library Catalog & Book Copy

Current authorization: **Plan096 APPROVED, Catalog source implemented; repository quality gates blocked** (`2026-10-08`). Sections drafting/wireframe dưới đây ghi lịch sử trước approval; checkpoint implementation ở cuối là trạng thái hiện hành.

## Related plan and authorization

- [Plan 096](../../dev-impl-plan/summary/096-library-catalog-and-book-copy-2026-10-08.md): **DRAFT — READY FOR REVIEW; implementation NOT APPROVED**.
- User requested drafting Plan 096 and confirmed application baseline v5. Read-only investigation was delegated to two `gpt-6-luna` subagents as requested. This authorization covers plan documentation; no feature implementation approval is inferred.
- Applied `dev-note` workflow to record the documentation work; backend tests/implementation skills have not been executed. Unit-test planning is included in Plan 096.

## Actual scope and files

- Created `document/dev-impl-plan/summary/096-library-catalog-and-book-copy-2026-10-08.md`: BE/FE catalog and copy scope, requirements, current architecture, proposed contracts/schema/source paths, tests, gate owners and expected outputs.
- Updated `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md` with a draft Plan 096 entry.
- Created this note and updated `document/dev-note/summary/DEV_NOTE_SUMMARY.md` and `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`.
- No production/test code, baseline/CR, migration, DB, remote branch or deployment changes.

## Decisions and limitations

- Kept Plan 095 approved amendments and deferred AI scope; cover URL only, barcode on-demand.
- Preserved baseline barcode `LIB-{copy id zero-padded 9 digits}` / Code128, copy status enum and separate referenceOnly flag; barcode persistence timing and ID boundary remain technical decisions.
- Proposed API/error/audit/FE and schema are review material, not frozen contracts. Stable error `code` gap against existing RestResponse is explicit.
- ISBN policy/field limits/batch idempotency/lifecycle remain open C1..C5. F1/F7/F8/F9 are not closed by drafting this plan; role foundation remains an implementation dependency.
- MySQL version/applied Flyway head remain UNKNOWN. Checked-in V29 is source evidence only. Catalog barcode uniqueness tests cannot prove active-loan concurrency; circulation acceptance remains separate.

## Validation

- Read-only source/baseline comparison by 6-Luna investigators: PASS as documentation/source evidence. Corrected package root, FE shell path, barcode format and coverage requirement during review.
- Markdown link targets (new documents and added index rows), 10 required Plan sections, CAT requirement/gate markers, summary five-column row continuity and changed-file scope: PASS. `git diff --check`: PASS; new-file whitespace checked separately: PASS. Historical index links outside this task were not revalidated.
- Independent 6-Luna QA against assigned v5/Plan 095 requirements: PASS; no substantive mismatch reported. This is documentation QA, not runtime/implementation validation.
- Backend/frontend unit/integration/full validation, MySQL migration/concurrency, browser/runtime/cloud/provider/deploy/remote checks: NOT RUN (plan-only task).

## Deviations and next step

- No deviation from requested plan-writing scope. No implementation occurred.
- Review Plan 096 proposals and close applicable catalog gates before coding; do not request reapproval of existing Plan 095 business amendments.

## Wireframe amendment — 2026-10-08

- User authorized interactive wireframe creation by subagents and explicitly required implemented UI to match current application UI. Feature implementation approval remains pending.
- `gpt-6-luna` investigators/builders own production-style survey, HTML/README creation and independent prototype QA; root owns Plan/Dev Note integration.
- Added Plan 096 section 11 with wireframe links, UI consistency acceptance, states and scope. Production design reference is `FE/src/styles/foundation.css`, `AuthenticatedLayout.vue`, `AuthenticatedV2ShellView.vue` and academic/functional-room table/form patterns.
- Wireframe artifacts: `document/wireframes/fe/library/096-library-catalog-and-book-copy/index.html` and `README.md`. All interactions use local demo data; no API/DB calls or FE application modifications.
- Prototype JS syntax/duplicate IDs and independent JSDOM interaction QA: PASS. Verified reader/manager visibility including role switch, create Book, batch add with sequential ID-derived LIB codes, copy shelf/reference editing, dynamic barcode lookup, active-loan/reservation archive guard and loading/empty/error/forbidden states.
- CSS parsing: PASS (120 rules and three responsive media queries). Fixed reader CTA visibility and malformed CSS introduced during prototype editing; final artifact rechecked after fixes.
- Actual Chrome screenshot review: PASS for desktop list (1440x1100), mobile list (390x1100), desktop detail/create-book/add-copies dialogs. Temporary screenshots in `/tmp/plan096-restored-desktop.png`, `/tmp/plan096-mobile-verified.png`, `/tmp/plan096-detail.png`, `/tmp/plan096-form.png`, `/tmp/plan096-copies.png`; temporary screen variants in `/tmp/plan096-wireframe-qa/`. Only those rendered views are visually verified; other states are DOM-tested.
- Chrome headless was blocked in the default sandbox (`setsockopt: Operation not permitted`); root rendered successfully using automatically approved escalation. CUA initialization timed out; it was not used as QA proof.
- Local Plan/README link targets and final whitespace/file scope checks: PASS. Existing FE application, API/DB/runtime authorization, backend/frontend suites and deployment: NOT RUN; no application source changed.

## Implementation authorization checkpoint — 2026-10-08

- User explicitly approved Plan096 and requested new 6-Luna DEV/TEST agents after stopping old agents. Plan section12 records current Catalog freeze and ownership; prior documentation-only sections above are historical, not current implementation authorization status.
- DEV BE, DEV FE and independent QA are implementing in shared workspace with separate production/test ownership. Worktree considered, not selected because ownership avoids concurrent edits and current plan/wireframe artifacts are uncommitted.
- Initial QA runtime probe: localhost:3306 unavailable; Docker socket permission denied. Exact MySQL version/applied head NOT RUN pending safe access; no migration applied. Full code validation and browser proof pending.
- Root authorized narrow Library-path security error adapters and FE optional machine-code transport. Legacy compatibility tests required. Role discovery verified from current source, no role assignment/seed authorized.

## Implementation notes

- [BE implementation / validation](../be/library/096-library-catalog-and-book-copy-2026-10-08.md).
- [FE implementation / validation](../fe/096-library-catalog-and-book-copy-2026-10-08.md).
- Independent QA: FE712 tests/lint/build/coverage PASS. BE latest focused130 tests/2 failures, pmdMain PASS, pmdTest526 findings gồm30 Catalog-owned; Checkstyle tasks PASS nhưng27 Catalog warnings. Tiny test helper correction UNVERIFIED. Stopped at10 debug rounds per backend-validation skill; backend not complete.
- Historical full751/2 failures reproduced on HEAD; user requested skip further full test. Earlier artifact build excludes test+pmdTest and PASS, not full lifecycle-build PASS. Isolated MySQL8.4.11 V30 fixture PASS and cleaned; target DB/integration gates remain OPEN. Historical prototype evidence above is not production runtime proof.
