# Frontend API v2 — Academic Structure

[← Back to `FrontendApiGuide.md`](../FrontendApiGuide.md)

## Academic Structure API

### Grade

Base:

```text
/api/v2/grades
```

Authorization hiện tại: `ADMIN`, `ACADEMIC_OFFICE`.

| Method | Path | Request | Response |
|---|---|---|---|
| `GET` | `/api/v2/grades` | — | `ResGradeLevelDTO[]` |
| `POST` | `/api/v2/grades` | `ReqCreateGradeLevelDTO` | `201 ResGradeLevelDTO` |
| `PUT` | `/api/v2/grades/{gradeId}` | `ReqUpdateGradeLevelDTO` | `ResGradeLevelDTO` |
| `DELETE` | `/api/v2/grades/{gradeId}` | — | `204` |

Shape:

```ts
interface GradeLevel {
  id: number
  code: string
  name: string
  gradeLevel: 6 | 7 | 8 | 9
  displayOrder: number
  nextGradeId: number | null
  active: boolean
  description: string | null
}
```

### Academic Year

Base:

```text
/api/v2/academic-years
```

Authorization hiện tại: `ADMIN`, `ACADEMIC_OFFICE`.

| Method | Path | Request | Response |
|---|---|---|---|
| `GET` | `/api/v2/academic-years` | — | `ResAcademicYearDTO[]` |
| `POST` | `/api/v2/academic-years` | `ReqCreateAcademicYearDTO` | `201 ResAcademicYearDTO` |
| `PUT` | `/api/v2/academic-years/{academicYearId}` | `ReqUpdateAcademicYearDTO` | `ResAcademicYearDTO` |
| `POST` | `/api/v2/academic-years/{academicYearId}/close` | — | `ResAcademicYearDTO` |
| `DELETE` | `/api/v2/academic-years/{academicYearId}` | — | `204` |

AcademicYear status uses the canonical wire enum in
[`07-enums-and-known-drift.md`](07-enums-and-known-drift.md).

Dates are `yyyy-MM-dd`.

### Semester

Base:

```text
/api/v2/semesters
```

| Method | Path | Authorization | Request/Query | Response |
|---|---|---|---|---|
| `GET` | `/api/v2/semesters?academicYearId=...` | `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`, `STUDENT` | `academicYearId` required | `ResSemesterDTO[]` |
| `POST` | `/api/v2/semesters` | Office | `ReqCreateSemesterDTO` | `201 ResSemesterDTO` |
| `PUT` | `/api/v2/semesters/{semesterId}` | Office | `ReqUpdateSemesterDTO` | `ResSemesterDTO` |
| `POST` | `/api/v2/semesters/{semesterId}/activate` | Office | — | `ResSemesterDTO` |
| `POST` | `/api/v2/semesters/{semesterId}/lock` | Office | — | `ResSemesterDTO` |
| `POST` | `/api/v2/semesters/{semesterId}/reopen` | Office | `{ reason }` | `ResSemesterDTO` |
| `GET` | `/api/v2/semesters/{semesterId}/completeness-report` | Office | `checkpointCode?` | `ResSemesterCompletenessReportDTO` |
| `GET` | `/api/v2/semesters/{semesterId}/completeness-decision` | Office | `checkpointDate=yyyy-MM-dd` | `ResSemesterCompletenessDecisionDTO` |
| `GET` | `/api/v2/semesters/{semesterId}/notifications` | Office | — | `ResSemesterNotificationDTO[]` |
| `POST` | `/api/v2/semesters/{semesterId}/notifications/dispatch` | Office | `checkpointCode?` | notification list |
| `POST` | `/api/v2/semesters/{semesterId}/notifications/retry-failed` | Office | — | notification list |

Office:

```text
ADMIN
ACADEMIC_OFFICE
```

Semester status uses the canonical wire enum in
[`07-enums-and-known-drift.md`](07-enums-and-known-drift.md).

Core response:

```ts
interface Semester {
  id: number
  academicYearId: number
  code: string
  name: string
  displayOrder: number
  startDate: string
  endDate: string
  automaticLockAt: string | null
  status: SemesterStatus
  lockedAt: string | null
  lockedBy: number | null
  lockReason: string | null
  reopenUntil: string | null
}
```

### School Class

Base:

```text
/api/v2/classes
```

| Method | Path | Authorization | Request/Query | Response |
|---|---|---|---|---|
| `GET` | `/api/v2/classes` | `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER` | `academicYearId?` | `ResSchoolClassDTO[]` |
| `POST` | `/api/v2/classes` | Office | create DTO | `201 ResSchoolClassDTO` |
| `PUT` | `/api/v2/classes/{classId}` | Office | update DTO | `ResSchoolClassDTO` |
| `POST` | `/api/v2/classes/{classId}/close` | Office | — | `ResSchoolClassDTO` |
| `DELETE` | `/api/v2/classes/{classId}` | Office | — | `204` |

School class status uses the canonical wire enum in
[`07-enums-and-known-drift.md`](07-enums-and-known-drift.md).

### Subject

Base:

```text
/api/v2/subjects
```

| Method | Path | Authorization | Request/Query | Response |
|---|---|---|---|---|
| `GET` | `/api/v2/subjects` | Authenticated | `status?` | `ResSubjectDTO[]` |
| `POST` | `/api/v2/subjects` | Office | `ReqCreateSubjectDTO` | `201 ResSubjectDTO` |
| `PUT` | `/api/v2/subjects/{subjectId}` | Office | `ReqUpdateSubjectDTO` | `ResSubjectDTO` |
| `POST` | `/api/v2/subjects/{subjectId}/applicabilities` | Office | applicability DTO | `201 ResSubjectApplicabilityDTO` |

Subject type, status and application scope use the canonical wire enums in
[`07-enums-and-known-drift.md`](07-enums-and-known-drift.md).

**Do not use `NORMAL` for `SubjectType` in FE current wire types.**

### Class Subject

| Method | Path | Authorization | Request/Query | Response |
|---|---|---|---|---|
| `GET` | `/api/v2/classes/{classId}/subjects` | Authenticated | `semesterId` required | `ResClassSubjectDTO[]` |
| `POST` | `/api/v2/class-subjects` | Office | create DTO | `201 ResClassSubjectDTO` |
| `PUT` | `/api/v2/class-subjects/{classSubjectId}` | Office | `{ status }` | `ResClassSubjectDTO` |

Class-subject status uses the canonical wire enum in
[`07-enums-and-known-drift.md`](07-enums-and-known-drift.md).
