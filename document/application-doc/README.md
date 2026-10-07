# Application Documentation

Application documents are separated by version. Do not mix files across versions when reading requirements, planning work or implementing changes.

## Version routing

| Version | Root                           | Purpose                                                                                    |
| ------- | ------------------------------ | ------------------------------------------------------------------------------------------ |
| v1      | `document/application-doc/v1/` | Existing baseline for original user, student and UI flows                                  |
| v2      | `document/application-doc/v2/` | Modular application baseline, expanded academic model and change requests                  |
| v3      | `document/application-doc/v3/` | Scheduling, placement, notifications, search/filter, score import and lesson-log baseline  |
| v4      | `document/application-doc/v4/` | Incremental AI timetable-agent improvements on top of v3; not a replacement full baseline  |
| v5      | `document/application-doc/v5/` | Library Management bounded context integrated into the existing school-management platform |

## v1 structure

- `ApplicationContext.md`
- `DataStructure.md`
- `modules/`
- `html-sample/`

## v2 structure

- `ApplicationContext.md`
- `RequirementBaseline.md`
- `ContractMigrationScopeFreeze.md`
- `modules/`
- `data-model/`
- `change-request/`

## v3 structure

- `ApplicationContext.md`
- `RequirementBaseline.md`
- `modules/`
- `data-model/`
- `frontend-api/`
- `contract/`
- `change-request/`

## v4 structure

- `README.md`
- `change-request/`
- `agent-contract/`

v4 remains an improvement/CR area over v3 according to its own README.

## v5 structure

- `README.md`
- `ApplicationContext.md`
- `RequirementBaseline.md`
- `requirement-adaptation/`
- `modules/`
- `data-model/`
- `contract/`
- `frontend-api/`
- `change-request/`

## Rules

- A task must identify its application-document version before using requirements.
- Use the selected version as source of truth for the capability being changed, together with explicitly inherited platform contracts.
- Do not infer a version from file contents or affected module.
- A later feature version does not automatically replace unrelated contracts from earlier versions.
- v5 Library reuses platform identity/security/database/deployment capabilities; it does not redefine them unless an approved v5 contract explicitly says so.