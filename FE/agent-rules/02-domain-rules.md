# Frontend Agent Rules — v2 Domain Rules

[← Back to `FE/AGENTS.override.md`](../AGENTS.override.md)

## Student rules

### Human-facing identifier

Prefer `studentCode` for human-facing search, display, and supported command input.

Keep numeric `studentId` as the technical identifier for:

- route/resource identity where the API uses it;
- foreign-key-backed requests;
- backend relations.

When an API supports both `studentId` and `studentCode`, follow the exact request semantics documented in `FrontendApiGuide.md`. Do not assume mixed bulk identifier semantics beyond the implemented contract.

### Student account provisioning

For the v2 FE scope, the account-provisioning flow is implemented by:

```text
POST /api/v3/students
```

and is currently restricted to `ADMIN` and `ACADEMIC_OFFICE`.

Do not replace it with self-registration.

Legacy `POST /api/v1/students` remains a compatibility endpoint for profile-only creation. New v2 UX must not choose between v1 and v3 implicitly; the approved FE plan must state which flow the screen uses.

### `averageScore`

`averageScore` is a legacy compatibility field in current Student v1/v3 DTOs.

Requirement/Data Model v2 does **not** treat it as the official academic result source.

For new v2 academic UI:

- do not calculate official averages from `averageScore`;
- do not display it as the official transcript result;
- official academic results come from scorebook/transcript APIs.

Do not remove the field from legacy requests until the backend compatibility contract is changed.

### Delete/lifecycle

Existing Student v1 exposes hard delete, but Requirement v2 requires preserving academic history.

Do not expand hard-delete UX into new v2 workflows. A lifecycle/status replacement requires a real backend contract and an approved plan.

## Academic and teaching rules

Do not infer grade from a class name.

Use backend identifiers and metadata for:

- academic year;
- semester;
- grade;
- class;
- subject;
- class-subject;
- teacher;
- homeroom assignment;
- subject-teaching assignment.

GVCN and GVBM are business roles derived from assignments, not standalone account roles.

A `TEACHER` account does not automatically mean the user may mutate every class or scorebook.

## Attendance rules

Attendance is exception-based.

Frontend behavior must preserve:

```text
valid school session
+ active enrollment
+ no attendance exception
= PRESENT
```

Do not create physical `PRESENT` mutations just to render the grid.

Current API exception values must follow `FrontendApiGuide.md`; do not substitute requirement wording when the wire enum differs.

Student self-service attendance is read-only.

## Scorebook rules

Do not calculate official `Đtbmh`, skill score, `Đtbhk`, `ĐtbmhCN`, or `Đtbcn` in frontend code.

The frontend may perform non-authoritative previews only if an approved requirement explicitly asks for a preview and clearly labels it as non-official. Otherwise, display backend results.

Score mutation must use the exact API score status and optimistic-version semantics.

Important:

- score value `0` is valid;
- missing score is not `0`;
- current API `ScoreStatus` does not contain `NOT_ENTERED`;
- in score-grid responses, absence of a score entry represents a not-entered cell unless the API contract changes.

For `AssessmentType`, use the wire values documented in `FrontendApiGuide.md`. In particular, the backend serializes periodic assessment as `KTĐK`.

## Background calculation and transcript

Official averages are background-calculated.

Frontend must understand:

```text
IN_PROGRESS
FINISH
```

When transcript/calculation status is `IN_PROGRESS`:

- show `Đang tính toán` / `Đang cập nhật`;
- do not present stale values as the newest official result;
- refresh or poll only through the existing status/read APIs;
- do not trigger calculations from GET requests;
- do not reproduce backend calculation formulas as the source of truth.

When status is `FINISH`, results may be presented as current only if the returned version/status contract says they are up to date.
