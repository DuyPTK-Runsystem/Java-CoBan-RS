# Frontend API v2 — Transcript, Retake and Calculation

[← Back to `FrontendApiGuide.md`](../FrontendApiGuide.md)

## Transcript API

Base:

```text
/api/v2/transcripts/students
```

### Student self-service

Authorization: `STUDENT`.

| Method | Path | Response |
|---|---|---|
| `GET` | `/me/semesters/{semesterId}` | term transcript |
| `GET` | `/me/academic-years/{academicYearId}` | annual transcript |
| `GET` | `/me/semesters/{semesterId}/status` | calculation status |
| `GET` | `/me/academic-years/{academicYearId}/status` | calculation status |

### Staff read

Authorization:

```text
ADMIN
ACADEMIC_OFFICE
TEACHER
```

| Method | Path | Response |
|---|---|---|
| `GET` | `/{studentId}/semesters/{semesterId}` | term transcript |
| `GET` | `/{studentId}/academic-years/{academicYearId}` | annual transcript |
| `GET` | `/{studentId}/semesters/{semesterId}/status` | calculation status |
| `GET` | `/{studentId}/academic-years/{academicYearId}/status` | calculation status |

Frontend must not assume Teacher may view every student's transcript merely because the controller role expression includes `TEACHER`; backend scope rules remain authoritative.

### Calculation status

`CalculationStatus` uses the canonical wire enum in
[`07-enums-and-known-drift.md`](07-enums-and-known-drift.md).

Status shape:

```ts
interface TranscriptCalculationStatus {
  studentId: number
  studentCode: string
  academicYearId: number
  semesterId: number | null
  calculationStatus: CalculationStatus
  sourceVersion: number
  calculatedVersion: number | null
  isUpToDate: boolean
  calculatedAt: string | null
  lastError: string | null
}
```

FE behavior:

```text
IN_PROGRESS
→ show "Đang cập nhật"
→ optionally poll status
→ do not mark stale values as latest official result

FINISH + isUpToDate=true
→ display as current result
```

GET transcript/status must not trigger synchronous calculation.

### Term transcript

Key shape:

```ts
interface TermTranscript {
  studentId: number
  academicYearId: number
  semesterId: number
  calculationStatus: CalculationStatus
  sourceVersion: number
  calculatedVersion: number | null
  calculatedAt: string | null
  dtbhk: number | null
  transferNotes: TransferNote[]
  subjects: TermSubjectResult[]
}
```

Each subject includes:

```text
subjectId
subjectName
subjectType
dtbmh
skillScore
calculatedVersion
calculatedAt
assessmentColumns[]
```

Each assessment column in transcript includes:

```text
columnId
assessmentType
columnNo
columnName
scoreStatus
scoreValue
```

### Annual transcript

Key fields:

```text
studentId
academicYearId
calculationStatus
sourceVersion
calculatedVersion
calculatedAt
regularDtbcn
finalDtbcn
resultSource
lastCalculationTaskId
transferNotes
subjects
```

Annual subject:

```text
subjectId
subjectName
subjectType
hk1
hk2
regularDtbmhCn
officialDtbmhCn
calculationSource
calculatedVersion
calculatedAt
retake?
```

Retake detail:

```text
retakeId
preRetakeScore
retakeScore
examDate
status
note
```

### Result source drift

Current Java `CalculationResultSource` uses the canonical wire enum in
[`07-enums-and-known-drift.md`](07-enums-and-known-drift.md).

Some target data-model text mentions `MIXED`, but current API enum does not expose `MIXED`.

FE must type the current API as `REGULAR | RETAKE` until backend changes.

## Retake API

Base:

```text
/api/v2/retake-exams
```

Authorization: Office.

| Method | Path | Request/Query | Response |
|---|---|---|---|
| `POST` | base | create DTO | `201 ResRetakeExamDTO` |
| `PUT` | `/{retakeId}/score` | update score DTO | retake DTO |
| `POST` | `/{retakeId}/cancel` | — | retake DTO |
| `GET` | `/{retakeId}` | — | retake DTO |
| `GET` | base | filter | Spring Page |

Create:

```ts
interface CreateRetakeExamRequest {
  studentId: number
  academicYearId: number
  subjectId: number
  examDate?: string | null
  retakeScore?: number | null
  note?: string | null
}
```

Update score:

```ts
interface UpdateRetakeScoreRequest {
  retakeScore: number
  examDate?: string | null
  note?: string | null
}
```

Score range:

```text
0.0 .. 10.0
max 1 decimal place
```

Retake exam status uses the canonical wire enum in
[`07-enums-and-known-drift.md`](07-enums-and-known-drift.md).

Filter:

```text
studentId?
academicYearId?
subjectId?
status?
page
size default 10
```

Official annual result after retake must be read from Transcript API; FE must not replace/recalculate `ĐtbmhCN` itself.

## Calculation Task / Operations API

Authorization: Office.

### Query/retry

| Method | Path | Request/Query | Response |
|---|---|---|---|
| `GET` | `/api/v2/scorebooks/calculation-tasks` | filter | Spring Page |
| `GET` | `/api/v2/scorebooks/calculation-tasks/failed` | filter | Spring Page |
| `POST` | `/api/v2/scorebooks/calculation-tasks/{taskId}/retry` | — | task |
| `POST` | `/api/v2/scorebooks/calculation-tasks/retry-all-failed` | — | task list |

Filter:

```text
status?
studentId?
studentCode?
academicYearId?
page
size default 10
```

Calculation task status and task type use the canonical wire enums in
[`07-enums-and-known-drift.md`](07-enums-and-known-drift.md).

### Request recalculation

By code:

```text
POST /api/v2/students/{studentCode}/transcripts/recalculate?academicYearId=...
```

By id:

```text
POST /api/v2/students/{studentId}/transcripts/recalculate?academicYearId=...
```

Success is `202 Accepted`.

This is an explicit command. Transcript GET endpoints do not recalculate.
