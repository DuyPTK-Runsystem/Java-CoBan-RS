# Frontend API Guide v2

## Purpose

This file is the **routing index for Frontend v2 API contracts**.

Do not load the entire API catalog for every FE task. Read the common transport contract once, then read only the domain contract files needed by the current task.

Requirement v2 defines intended business behavior. The routed API files document the **current implemented backend wire contract** and known gaps/drift.

## Always read for API integration

- [`frontend-api/00-common-contract.md`](frontend-api/00-common-contract.md)

It defines:

- source precedence;
- JSON success/error envelope;
- authentication transport;
- `401` / `403` / validation handling;
- date, pagination, typed-service, warning/error, and background-calculation integration rules.

## Domain routing

| FE task/domain | Read |
|---|---|
| Login, logout, account, roles, Student v1/v2/v3, Student profile vs academic flows vs account provisioning | [`frontend-api/01-auth-student.md`](frontend-api/01-auth-student.md) |
| Grade, Academic Year, Semester, Class, Subject, Class Subject | [`frontend-api/02-academic-structure.md`](frontend-api/02-academic-structure.md) |
| Teacher, homeroom/teaching assignment, enrollment, transfer | [`frontend-api/03-teacher-assignment-enrollment.md`](frontend-api/03-teacher-assignment-enrollment.md) |
| Calendar, attendance session, attendance exception, attendance reports/history | [`frontend-api/04-calendar-attendance.md`](frontend-api/04-calendar-attendance.md) |
| Scorebook, assessment columns, score entry, score-change requests, audit logs | [`frontend-api/05-scorebook-change-audit.md`](frontend-api/05-scorebook-change-audit.md) |
| Transcript, transcript status, retake, calculation task/retry/recalculate | [`frontend-api/06-transcript-retake-calculation.md`](frontend-api/06-transcript-retake-calculation.md) |
| TypeScript wire enums, Requirement/Data Model/API drift | [`frontend-api/07-enums-and-known-drift.md`](frontend-api/07-enums-and-known-drift.md) |

For any task that defines or edits TypeScript string unions/enums, also read:

- [`frontend-api/07-enums-and-known-drift.md`](frontend-api/07-enums-and-known-drift.md)

## Current contract blockers

### Auth role discovery

Current login/account DTO and JWT do not expose frontend roles.

Therefore role-aware navigation remains blocked until an approved backend contract exposes roles/capabilities. Do not infer them client-side.

### Known wire-contract drift

Important current differences include:

- `SubjectType` wire contract is `ACADEMIC | SKILL`, not `NORMAL | SKILL`;
- attendance exception wire values differ from some Requirement v2 wording;
- current `ScoreStatus` has no `NOT_ENTERED`;
- current `CalculationResultSource` does not expose every value mentioned by the target data model;
- Student `averageScore` still exists in compatibility DTOs but is not the official v2 academic-result source.

Read the enums/drift route before introducing FE union types.

## Legacy contract rule

Some v1 endpoints remain intentionally available for compatibility, especially Auth and Student.

Their presence does **not** make v1 documentation the business source of truth.

For new v2 features:

```text
Requirement v2 / approved CR
        ↓
current routed API contract
        ↓
approved FE plan
```

Use v1 documentation only when a task explicitly concerns legacy compatibility.

## Update rule

When a backend controller, request DTO, response DTO, enum, or authorization contract used by FE changes:

1. update the relevant file under `frontend-api/`;
2. update this router if routing/scope changed;
3. update FE TypeScript types/services;
4. update affected tests;
5. update Requirement/CR when the business contract changed.

Do not add a planned endpoint to the current contract as though it already exists.

## Routing principle

```text
FrontendApiGuide.md
        ↓
00-common-contract                  always for API work
        ↓
domain contract file(s)             task-specific
        ↓
07-enums-and-known-drift            whenever wire enums/types matter
        ↓
FE typed service + tests
```
