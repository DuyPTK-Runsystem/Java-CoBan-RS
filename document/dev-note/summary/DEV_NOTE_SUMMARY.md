# Dev Note Summary

## Naming convention

- Detailed Dev Notes live in area/module folders under `document/dev-note/`.
- Use the related Developer Plan sequence number when available.
- Date is placed at the end of the file name.
- Format:

```text
NNN-short-topic-yyyy-mm-dd.md
```

## Notes

| No. | Area | Note | Status | Updated |
|---:|---|---|---|---|
| 001 | BE | [Base Backend Theo Boilerplate, Rút Gọn User/Auth](../be/user-auth/001-base-boilerplate-user-auth-2026-08-17.md) | Completed | 2026-08-17 |
| 002 | BE | [Dev Note Skill and Workflow Enforcement](../be/workflow-skill/002-dev-note-skill-workflow-2026-08-17.md) | Completed | 2026-08-17 |
| 003 | BE | [Postman Collection Skill](../be/workflow-skill/003-postman-collection-skill-2026-08-17.md) | Completed | 2026-08-17 |
| 004 | BE | [Auth Postman Collection](../be/user-auth/004-auth-postman-collection-2026-08-17.md) | Completed | 2026-08-17 |
| 004 | BE | [Lombok Annotation Guidance](../be/workflow-skill/004-lombok-annotation-guidance-2026-08-17.md) | Completed | 2026-08-17 |
| 005 | BE | [Student Module Backend](../be/student/005-student-module-backend-2026-08-17.md) | Completed | 2026-08-17 |
| 006 | BE | [Refactor Lombok Entity and Explicit Controller Params](../be/refactor/006-lombok-entity-controller-param-refactor-2026-08-17.md) | Completed | 2026-08-17 |
| 007 | BE | [Student Integration Test](../be/student/007-student-integration-test-2026-08-18.md) | Completed with validation blocker | 2026-08-18 |
| 008 | BE | [User Integration Test](../be/user-auth/008-user-integration-test-2026-08-18.md) | Completed | 2026-08-18 |
| 009 | FE | [FE Project Skeleton](../fe/foundation/009-fe-project-skeleton-2026-08-18.md) | Completed; validation blocker resolved by 010/011 | 2026-08-18 |
| 010 | FE | [Frontend Validation Quality Gates](../fe/foundation/010-frontend-validation-quality-gates-2026-08-18.md) | Completed; blocker resolved by 011 | 2026-08-18 |
| 010 | BE | [API Contract and TBD Resolution](../be/010-api-contract-and-tbd-resolution-2026-08-18.md) | Completed | 2026-08-18 |
| 011 | FE | [FE Test/Coverage Foundation](../fe/foundation/011-fe-test-coverage-foundation-2026-08-18.md) | Completed | 2026-08-18 |
| 012 | FE | [User/Auth API và Route Guard](../fe/user-auth/012-user-auth-api-route-guard-2026-08-18.md) | Completed | 2026-08-18 |
| 013 | FE | [Storybook PrimeVue Preview Runtime](../fe/tooling/013-storybook-primevue-preview-2026-08-19.md) | Completed | 2026-08-19 |
| 014 | FE | [Auth Password Input Width](../fe/user-auth/014-auth-password-input-width-2026-08-19.md) | Completed | 2026-08-19 |
| 015.1 | BE | [StudentInfo Cardinality, Document và Student Detail API](../be/student/015.1-student-info-cardinality-document-get-api-2026-08-19.md) | Completed; MySQL schema alter not run | 2026-08-19 |
| 015 | FE | [Student API, CRUD, Search, Sort, Page và Delete](../fe/student/015-student-api-crud-search-sort-page-delete-2026-08-19.md) | Completed | 2026-08-19 |
| 016 | FE | [Student UI Date Format, Input Examples và Storybook](../fe/student/016-student-ui-date-format-input-examples-storybook-2026-08-19.md) | Completed; amendment 16.1 implemented | 2026-08-19 |
| 017 | BE | [Student Backend Input Validation](../be/student/017-student-input-validation-2026-08-19.md) | Completed | 2026-08-19 |
| 018 | BE | [Batch CSV Export](../be/batch/018-batch-csv-export-2026-08-19.md) | Completed | 2026-08-19 |
| 019 | BE | [Dev Trace Logging Skill](../be/workflow-skill/019-dev-trace-logging-skill-2026-08-19.md) | Completed | 2026-08-19 |
| 020 | BE | [Backend Per-Run Log File](../be/020-backend-per-run-log-file-2026-08-19.md) | Completed | 2026-08-19 |
| 021 | FE | [Student CSV Download và Pagination Options](../fe/student/021-student-csv-download-pagination-options-go-to-page-2026-08-19.md) | Completed; Birthday/Address sort removed | 2026-08-19 |
| 022 | FE | [FE Documentation Structure by Module](../fe/tooling/022-fe-doc-structure-by-module-2026-08-19.md) | Completed | 2026-08-19 |
| 023 | Infrastructure | [Docker Image, Test Data Bootstrap và README](023-docker-image-readme-test-data-2026-08-19.md) | Partially completed; external validation pending | 2026-08-19 |
| 024 | BE | [Modular v2 Document Paths in Skills](../be/workflow-skill/024-modular-v2-document-paths-2026-08-20.md) | Completed | 2026-08-20 |
| 025 | BE/Document | [Contract, Migration và Scope Freeze](../be/025-contract-migration-scope-freeze-2026-08-20.md) | Completed; Docker MySQL preflight not run | 2026-08-20 |
| 026 | BE | [Student Enrollment & Class Placement](../be/enrollment/026-student-enrollment-class-placement-2026-08-21.md) | Completed | 2026-08-21 |
| 027.1 | BE | [Plan 027 Decision Update](../be/academic/027.1-plan-027-decision-update-2026-08-22.md) | Documentation decision update; implementation pending | 2026-08-22 |
| 027 | BE | [G3 Semester CR](../be/academic/027-semester-g3-cr-2026-08-22.md) | Completed; CR awaiting approval | 2026-08-22 |
| 027 | BE | [Academic Subject & Teacher Assignment Implementation](../be/academic/027-academic-subject-teacher-assignment-implementation-2026-08-22.md) | Completed; 27.1 not implemented | 2026-08-22 |
| 028 | BE | [Attendance Session Foundation](../be/attendance/028-attendance-session-foundation-2026-08-22.md) | Completed | 2026-08-22 |
| 029 | BE | [Backend Validation Single Owner](../be/workflow-skill/029-backend-validation-single-owner-2026-08-22.md) | Completed | 2026-08-22 |
