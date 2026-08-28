# Dev Plan Summary

## Naming convention

- Detailed dev plan files live in area folders under `document/dev-impl-plan/`.
- Area folders:
  - `be/` for backend plans.
  - `fe/` for frontend plans.
  - `summary/` for cross-area summaries.
- Backend detailed plans are grouped by module folder under `be/`.
- Detailed dev plan file names use a 3-digit sequence number starting from `001`.
- Date is placed at the end of the file name.
- Format:

```text
NNN-short-topic-yyyy-mm-dd.md
```

Example:

```text
001-base-boilerplate-user-auth-2026-08-17.md
```

## Plans

|   No. | Area           | Plan                                                                                                                                      | Status                                                                         | Created    |
| ----: | -------------- | ----------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ | ---------- |
|   001 | BE             | [Base Backend Theo Boilerplate, Rút Gọn User/Auth](../be/user-auth/001-base-boilerplate-user-auth-2026-08-17.md)                          | Approved                                                                       | 2026-08-17 |
|   002 | BE             | [Dev Note Skill and Workflow Enforcement](../be/workflow-skill/002-dev-note-skill-workflow-2026-08-17.md)                                 | Approved                                                                       | 2026-08-17 |
|   003 | BE             | [Postman Collection Skill](../be/workflow-skill/003-postman-collection-skill-2026-08-17.md)                                               | Approved                                                                       | 2026-08-17 |
|   004 | BE             | [Lombok Annotation Guidance](../be/workflow-skill/004-lombok-annotation-guidance-2026-08-17.md)                                           | Approved                                                                       | 2026-08-17 |
|   005 | BE             | [Student Module Backend](../be/student/005-student-module-backend-2026-08-17.md)                                                          | Approved                                                                       | 2026-08-17 |
|   006 | BE             | [Refactor Lombok Entity and Explicit Controller Params](../be/006-lombok-entity-controller-param-refactor-2026-08-17.md)                  | Approved                                                                       | 2026-08-17 |
|   007 | BE             | [Student Integration Test](../be/student/007-student-integration-test-2026-08-18.md)                                                      | Approved                                                                       | 2026-08-18 |
|   008 | BE             | [User Integration Test](../be/user-auth/008-user-integration-test-2026-08-18.md)                                                          | Approved                                                                       | 2026-08-18 |
|   009 | FE             | [FE Project Skeleton](../fe/foundation/009-fe-project-skeleton-2026-08-18.md)                                                             | Approved                                                                       | 2026-08-18 |
|   010 | BE/FE          | [API Contract and TBD Resolution](../be/010-api-contract-and-tbd-resolution-2026-08-18.md)                                                | Approved                                                                       | 2026-08-18 |
|   011 | FE             | [FE Test/Coverage Foundation](../fe/foundation/011-fe-test-coverage-foundation-2026-08-18.md)                                             | Approved                                                                       | 2026-08-18 |
|   012 | FE             | [User/Auth API và Route Guard](../fe/user-auth/012-user-auth-api-route-guard-2026-08-18.md)                                               | Approved                                                                       | 2026-08-18 |
|   013 | FE             | [Storybook PrimeVue Preview Runtime](../fe/tooling/013-storybook-primevue-preview-2026-08-19.md)                                          | Approved                                                                       | 2026-08-19 |
|   014 | FE             | [Auth Password Input Width](../fe/user-auth/014-auth-password-input-width-2026-08-19.md)                                                  | Approved                                                                       | 2026-08-19 |
|   015 | FE             | [Student API, CRUD, Search, Sort, Page và Delete](../fe/student/015-student-api-crud-search-sort-page-delete-2026-08-19.md)               | Approved; awaiting user review                                                 | 2026-08-19 |
|   016 | FE             | [Student UI Date Format, Input Examples và Storybook](../fe/student/016-student-ui-date-format-input-examples-storybook-2026-08-19.md)    | Approved; amendment 16.1 implemented                                           | 2026-08-19 |
| 015.1 | BE/Document    | [StudentInfo Cardinality, Document và Student Detail API](../be/student/015.1-student-info-cardinality-document-get-api-2026-08-19.md)    | Approved                                                                       | 2026-08-19 |
|   017 | BE             | [Student Backend Input Validation](../be/student/017-student-input-validation-2026-08-19.md)                                              | Approved                                                                       | 2026-08-19 |
|   018 | BE             | [Batch CSV Export](../be/018-batch-csv-export-2026-08-19.md)                                                                              | Approved                                                                       | 2026-08-19 |
|   019 | BE             | [Shared Dev Trace Logging Skill](../be/workflow-skill/019-dev-trace-logging-skill-2026-08-19.md)                                          | Approved                                                                       | 2026-08-19 |
|   020 | BE             | [Backend Per-Run Log File](../be/020-backend-per-run-log-file-2026-08-19.md)                                                              | Approved                                                                       | 2026-08-19 |
|   021 | FE             | [Student CSV Download và Pagination Options](../fe/student/021-student-csv-download-pagination-options-go-to-page-2026-08-19.md)          | Completed                                                                      | 2026-08-19 |
|   022 | FE             | [FE Documentation Structure by Module](../fe/tooling/022-fe-doc-structure-by-module-2026-08-19.md)                                        | Completed                                                                      | 2026-08-19 |
|   023 | Infrastructure | [Docker Image, Test Data Bootstrap và README](023-docker-image-readme-test-data-2026-08-19.md)                                            | Partially implemented; external validation pending                             | 2026-08-19 |
|   025 | BE/FE/Document | [Contract, Migration và Scope Freeze](025-contract-migration-scope-freeze-2026-08-20.md)                                                  | Approved                                                                       | 2026-08-20 |
|   026 | BE             | [Student Enrollment & Class Placement](../be/enrollment/026-student-enrollment-class-placement-2026-08-21.md)                             | Approved                                                                       | 2026-08-21 |
|   027 | BE             | [Academic Subject & Teacher Assignment](../be/academic/027-academic-subject-teacher-assignment-2026-08-21.md)                             | Approved                                                                       | 2026-08-21 |
|   028 | BE             | [Attendance Session Foundation](../be/attendance/028-attendance-session-foundation-2026-08-22.md)                                         | Approved                                                                       | 2026-08-22 |
|   030 | BE             | [Attendance Calendar Validity Foundation](../be/attendance/030-attendance-calendar-validity-foundation-2026-08-23.md)                     | Approved                                                                       | 2026-08-23 |
|   031 | BE             | [Student Attendance History, Read-Only](../be/attendance/031-student-attendance-history-read-only-2026-08-23.md)                          | Approved                                                                       | 2026-08-23 |
|   033 | BE             | [Class Attendance Summary](../be/attendance/033-class-attendance-summary-2026-08-24.md)                                                   | Approved                                                                       | 2026-08-24 |
|   034 | BE             | [Teacher User Role Assignment](../be/teacher/034-teacher-user-teacher-role-assignment-2026-08-24.md)                                      | Approved                                                                       | 2026-08-24 |
|   035 | BE             | [Academic Office Attendance Adjustment](../be/attendance/035-academic-office-attendance-adjustment-2026-08-24.md)                         | Approved                                                                       | 2026-08-24 |
|   036 | BE             | [Scorebook Foundation](../be/scorebook/036-scorebook-foundation-2026-08-24.md)                                                            | Approved                                                                       | 2026-08-24 |
|   037 | BE             | [Student Score Entry](../be/scorebook/037-student-score-entry-2026-08-24.md)                                                              | Approved                                                                       | 2026-08-24 |
|   038 | BE             | [Score Change Request](../be/scorebook/038-score-change-request-2026-08-25.md)                                                            | Approved                                                                       | 2026-08-25 |
|   039 | BE             | [Semester Lock Lifecycle, Service & Batch](../be/academic/039-semester-lock-2026-08-25.md)                                                | Approved                                                                       | 2026-08-25 |
|   040 | BE             | [Semester Completeness Notification (CR-SEM-001)](../be/academic/040-semester-completeness-notification-cr-sem-001-2026-08-25.md)         | Approved                                                                       | 2026-08-25 |
|   041 | BE             | [Transcript Result Schema Foundation](../be/scorebook/041-transcript-result-schema-foundation-2026-08-25.md)                              | Approved                                                                       | 2026-08-25 |
|   042 | BE             | [Subject Calculation Engine & Worker Lifecycle](../be/scorebook/042-subject-calculation-engine-and-worker-lifecycle-2026-08-25.md)        | Approved                                                                       | 2026-08-25 |
| 042.1 | BE             | [Student Code Input Resolution & Display Across All Modules](../be/student/042.1-student-code-input-resolution-and-display-2026-08-25.md) | Approved                                                                       | 2026-08-25 |
|   043 | BE             | [Create Student With Student Account](../be/student/043-student-create-with-account-2026-08-26.md)                                        | Approved                                                                       | 2026-08-26 |
|   044 | BE             | [Term and Annual Transcript Aggregation](../be/scorebook/044-transcript-aggregation-2026-08-26.md)                                        | Approved                                                                       | 2026-08-26 |
|   045 | BE             | [Retake Foundation and Calculation Integration](../be/scorebook/045-retake-foundation-and-calculation-integration-2026-08-26.md)          | Approved                                                                       | 2026-08-26 |
|   046 | BE             | [Transcript Query API](../be/scorebook/046-transcript-query-api-2026-08-26.md)                                                            | Completed                                                                      | 2026-08-26 |
|   047 | BE             | [BE Operational Closure](../be/scorebook/047-be-operational-closure-2026-08-26.md)                                                        | Completed                                                                      | 2026-08-26 |
|   048 | BE             | [BE Demo Data Bootstrap](../be/bootstrap/048-be-demo-data-bootstrap-2026-08-26.md)                                                        | Completed; validation PASS                                                     | 2026-08-26 |
|   049 | FE             | [Frontend API Catalog](../fe/tooling/049-frontend-api-catalog-2026-08-26.md)                                                              | Approved                                                                       | 2026-08-26 |
|   050 | FE             | [Frontend Documentation Refactor](../fe/tooling/050-frontend-documentation-refactor-2026-08-27.md)                                        | Approved                                                                       | 2026-08-27 |
|   051 | FE             | [FE v2 Shell & API Integration Foundation](../fe/tooling/051-fe-v2-shell-api-integration-foundation-2026-08-27.md)                        | Approved; implementation completed                                             | 2026-08-27 |
|   053 | FE             | [Grade, Class, Subject & Class-Subject UI](../fe/academic/053-grade-class-subject-management-ui-2026-08-27.md)                            | Approved phases 0–6; phases 4–5 completed; Phase 6 blocked by backend contract | 2026-08-27 |
| 053.1 | BE/FE          | [Subject Applicability Management & Dialog Layout](../fe/academic/053.1-subject-applicability-management-and-dialog-layout-2026-08-28.md) | Completed; BE/FE validation PASS                                               | 2026-08-28 |
|   054 | FE             | [Student Enrollment & Class Placement UI](../fe/enrollment/054-student-enrollment-class-placement-ui-2026-08-28.md)                       | Completed; validation PASS                                                     | 2026-08-28 |
|   055 | FE             | [Teacher Profile & Teaching Assignment UI](../fe/teacher/055-teacher-profile-and-teaching-assignment-ui-2026-08-28.md)                    | Approved; implementation completed; validation PASS                            | 2026-08-28 |
