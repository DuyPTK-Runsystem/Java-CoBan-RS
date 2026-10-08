# Dev Note 095 — v5 Foundation and Contract Inventory

## Related plan and approval

- Plan: [`document/dev-impl-plan/summary/095-v5-foundation-contract-freeze-2026-10-08.md`](../../dev-impl-plan/summary/095-v5-foundation-contract-freeze-2026-10-08.md).
- Plan 095 is `COMPLETE — inventory/documentation`; contract freeze and implementation approval are not complete/granted. The user authorized bounded documentation completion only. No Library implementation, migration, API/UI, DB/runtime write, provider call, or deployment was performed.

## Scope completed

- Added v5 foundation and contract inventory with branch/commit, code-vs-runtime evidence, route/resource scan, source migration head, MySQL runtime `UNKNOWN`, F7–F9 gates, requirement traceability, approved decision amendments, and proposed implementation dependencies.
- Updated Plan 095 decision log, gate states and dependency slices to match user decisions and the actual inventory.
- Linked the inventory and its honest `INVENTORY_COMPLETE` state from v5 README.

## Files changed

- `document/application-doc/v5/FoundationAndContractFreeze.md` — inventory, compatibility boundary, decision amendments, traceability matrix, open technical gates and slice sequence.
- `document/application-doc/v5/README.md` — artifact link and status.
- `document/dev-impl-plan/summary/095-v5-foundation-contract-freeze-2026-10-08.md` — decision/gate/sequence alignment.
- `document/dev-note/summary/095-v5-foundation-contract-freeze-2026-10-08.md` — this note.
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md` and `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md` — indexes.

## Decisions and remaining gates

The artifact records core-first scope, deferred AI-001/002 plus SHOULD/BONUS, ADMIN-only audited Librarian grant/revoke, borrower and suspension policy, reservation lifecycle, fine/payment policy, configurable date policy, and scanner/card decisions. User reports local target DB name `java_coban`; this is not runtime DB evidence and is not production. The exact MySQL version and applied Flyway head remain UNKNOWN. Approved F8 invariants include canonical `library_card`, copy/card/patron max-five constraints, and LOST ending the active loan; exact DDL remains unselected. F9 API/error/audit/notification/policy-version/FE contract planning is deferred and blocks affected implementation. Fine correction/reversal remains outside core and open for a later plan.

Remaining gates: exact MySQL version/applied Flyway head; MySQL DDL/concurrency proof for approved invariants; deferred API/error/audit/notification/policy-version/FE contracts; AI prompt/output retention before later AI implementation. The inventory is `INVENTORY_COMPLETE`, not `FROZEN_FOR_SLICE`.

## Validation

- Source route/config scan and branch/commit capture: `PASS` as repository-source inventory only; runtime DB/cloud state remains `UNKNOWN`.
- Requirement-ID coverage: `PASS` (68 baseline IDs mapped).
- Local Markdown-link target check: `PASS` across six edited/index Markdown files.
- Local Markdown heading-anchor check: `PASS` across the edited/index Markdown links.
- `git diff --check`: `PASS`.
- Unit/integration/full tests, DB/cloud/provider/browser checks: `NOT RUN` (documentation-only scope; no runtime mutation).

## Deviations and next steps

- No implementation files were changed. The approved user decisions are recorded in the artifact without silently rewriting `RequirementBaseline.md` or other baseline contracts; synchronize baseline/CR in a separately authorized documentation step.
- Before any implementation slice, close F1/F7/F8/F9 as applicable; re-scan route mapping on the target commit and confirm exact runtime database version/head. Preserve all deferred AI requirement IDs for a later plan.
