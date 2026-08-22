# Application Documentation

Application documents are separated by version. Do not mix files across
versions when reading requirements, planning work or implementing changes.

## Version routing

| Version | Root | Purpose |
|---|---|---|
| v1 | `document/application-doc/v1/` | Existing baseline for the original user, student and UI flows |
| v2 | `document/application-doc/v2/` | Modular application baseline, expanded academic model and change requests |

## v1 structure

- `ApplicationContext.md`: application scope and architecture context.
- `DataStructure.md`: database and persistence baseline.
- `modules/`: user and student module contracts.
- `html-sample/`: UI samples used by the original frontend flows.

## v2 structure

- `ApplicationContext.md`: modular application context.
- `RequirementBaseline.md`: v2 requirement baseline and scope.
- `ContractMigrationScopeFreeze.md`: contract and migration freeze.
- `modules/`: modular feature and domain contracts.
- `data-model/`: data model, constraints, migration and calculation references.
- `change-request/`: approved or pending v2 change requests.

## Rules

- A task must identify `v1` or `v2` before reading application documents.
- Use only the selected version as the source of truth for that task.
- Do not infer a version from a file's content or from the affected module.
- When a task changes version-specific documentation, update references in the
  same version and preserve the other version unchanged.
