# Frontend API v2 — Scorebook, Score Change and Audit

[← Back to `FrontendApiGuide.md`](../FrontendApiGuide.md)

## Scorebook API

### Scorebook lifecycle/config

| Method | Path | Authorization | Request | Response |
|---|---|---|---|---|
| `POST` | `/api/v2/scorebooks` | Office | `{ classSubjectId }` | `201 ResScorebookDTO` |
| `GET` | `/api/v2/scorebooks/{scorebookId}` | Office/Teacher | — | `ResScorebookDTO` |
| `POST` | `/api/v2/scorebooks/{scorebookId}/open` | Office/Teacher | — | `ResScorebookDTO` |
| `POST` | `/api/v2/scorebooks/{scorebookId}/columns` | Office/Teacher | create column | `ResAssessmentColumnDTO` |
| `PUT` | `/api/v2/assessment-columns/{columnId}` | Office/Teacher | `{ columnName? }` | column DTO |
| `DELETE` | `/api/v2/assessment-columns/{columnId}` | Office/Teacher | — | `204` |
| `PUT` | `/api/v2/scorebooks/{scorebookId}/skill-weight` | Office/Teacher | weights | scorebook DTO |
| `POST` | `/api/v2/scorebooks/{scorebookId}/publish` | Office/Teacher | — | scorebook DTO |

Office:

```text
ADMIN
ACADEMIC_OFFICE
```

Scorebook roles:

```text
ADMIN
ACADEMIC_OFFICE
TEACHER
```

Backend service must still enforce actual assignment/scope. FE must not interpret the role list as permission to edit every scorebook.

Scorebook status uses the canonical wire enum in
[`07-enums-and-known-drift.md`](07-enums-and-known-drift.md).

### Assessment types

Java enum:

```text
KTTT
KTDK
KTCK
```

Wire serialization is special:

```text
KTTT
KTĐK
KTCK
```

Input accepts both:

```text
KTĐK
KTDK
```

The canonical FE wire type is documented in
[`07-enums-and-known-drift.md`](07-enums-and-known-drift.md).

Do not accidentally send `"KTDK"` unless the service adapter intentionally relies on backend compatibility.

### Score grid

```text
GET /api/v2/scorebooks/{scorebookId}/score-entries
```

Authorization: Office/Teacher.

Query:

```text
page default 0
size default 10
```

Response:

```ts
interface StudentScoreGrid {
  scorebookId: number
  classSubjectId: number
  scorebookStatus: ScorebookStatus
  columns: Array<{
    columnId: number
    assessmentType: AssessmentType
    columnNo: number
    columnName: string | null
  }>
  page: number
  size: number
  totalElements: number
  totalPages: number
  students: Array<{
    studentId: number
    studentCode: string
    studentName: string
    scores: Record<string, StudentScore>
  }>
}
```

`scores` là map theo `assessmentColumnId`.

Nếu một cell không có entry trong `scores`, FE coi đó là **chưa nhập** theo current score-grid contract.

### Score mutation

Single by numeric id:

```text
PUT /api/v2/assessment-columns/{columnId}/students/{studentId}/score
```

Single by student code:

```text
PUT /api/v2/assessment-columns/{columnId}/students/by-code/{studentCode}/score
```

Bulk:

```text
POST /api/v2/assessment-columns/{columnId}/scores/bulk
```

Single request:

```ts
interface UpsertStudentScoreRequest {
  scoreStatus: ScoreStatus
  scoreValue?: number | null
  note?: string | null
  expectedVersion?: number | null
}
```

The current `ScoreStatus` wire enum is documented canonically in
[`07-enums-and-known-drift.md`](07-enums-and-known-drift.md).

There is **no `NOT_ENTERED` enum value in the implemented API**.

Rules for FE:

- `SCORED` may carry score `0`; zero is valid;
- missing/not-entered is not represented by numeric zero;
- use `expectedVersion` when editing an existing score if the UI has the returned version, so optimistic concurrency is preserved.

Bulk item supports either:

```text
studentId
studentCode
```

and requires at least one.

## Score Change Request API

Base:

```text
/api/v2/score-change-requests
```

Roles for create/read:

```text
ADMIN
ACADEMIC_OFFICE
TEACHER
```

| Method | Path | Authorization | Request/Query | Response |
|---|---|---|---|---|
| `POST` | base | Staff | create request | `201 ResScoreChangeRequestDetailDTO` |
| `GET` | base | Staff | filter | page summary |
| `GET` | `/{requestId}` | Staff | — | detail |
| `POST` | `/{requestId}/approve` | Office | — | detail |
| `POST` | `/{requestId}/reject` | Office | `{ rejectionReason }` | detail |
| `POST` | `/{requestId}/cancel` | `ADMIN`, `TEACHER` | — | detail |

Create request:

```ts
interface CreateScoreChangeRequest {
  assessmentColumnId: number
  studentId?: number | null
  studentCode?: string | null
  proposedStatus: ScoreStatus
  proposedValue?: number | null
  reason: string
}
```

Score-change request status uses the canonical wire enum in
[`07-enums-and-known-drift.md`](07-enums-and-known-drift.md).

Filter:

```text
status?
scorebookId?
columnId?
studentId?
studentCode?
requestedBy?
page
size default 10
```

## Score Audit API

```text
GET /api/v2/scorebooks/audit-logs
```

Authorization:

```text
ADMIN
ACADEMIC_OFFICE
TEACHER
```

Filter:

```text
entityType?
entityId?
studentId?
studentCode?
action?
actorUserId?
fromOccurredAt?
toOccurredAt?
page
size default 10, max 50
```

Response is Spring `Page<ResScoreAuditLogDTO>` inside the standard success envelope.

Audit item includes:

```text
auditLogId
actorUserId
actorUsername
action
entityType
entityId
beforeData
afterData
requestId
ipAddress
occurredAt
```

`beforeData` / `afterData` are JSON values and should be treated as untrusted display data, not as a stable typed domain model unless a specific audit screen defines a schema.
