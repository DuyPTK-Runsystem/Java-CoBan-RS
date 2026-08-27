# Frontend API v2 — Calendar and Attendance

[← Back to `FrontendApiGuide.md`](../FrontendApiGuide.md)

## Calendar API

### Configure day

```text
PUT /api/v2/calendar/days/{calendarDate}
```

Authorization: Office.

Path date:

```text
yyyy-MM-dd
```

Request:

```ts
interface UpsertCalendarDayRequest {
  academicYearId: number
  semesterId: number
  dayType: CalendarDayType
  reason?: string | null
  sessions: Array<{
    sessionPeriod: CalendarSessionPeriod
    sessionStatus: CalendarSessionStatus
    reason?: string | null
  }>
}
```

At most two session entries.

### Read calendar

```text
GET /api/v2/calendar/days
```

Authorization:

```text
ADMIN
ACADEMIC_OFFICE
TEACHER
STUDENT
```

Required query:

```text
academicYearId
semesterId
from=yyyy-MM-dd
to=yyyy-MM-dd
```

Calendar day, session period and session status use the canonical wire enums in
[`07-enums-and-known-drift.md`](07-enums-and-known-drift.md).

## Attendance API

### Teacher attendance session

Accepted base aliases:

```text
/api/v2/attendance-sessions
/api/v2/attendance/sessions
```

Authorization: `TEACHER`.

| Method | Path suffix | Request | Response |
|---|---|---|---|
| `POST` | base | create session DTO | `201 ResAttendanceSessionDTO` |
| `GET` | `/{sessionId}/students` | — | `ResAttendanceStudentDTO[]` |
| `PUT` | `/{sessionId}/exceptions/{studentId}` | exception DTO | `ResAttendanceExceptionDTO` |
| `PUT` | `/{sessionId}/exceptions/by-code/{studentCode}` | exception DTO | `ResAttendanceExceptionDTO` |
| `DELETE` | `/{sessionId}/exceptions/{studentId}` | — | `204` |
| `DELETE` | `/{sessionId}/exceptions/by-code/{studentCode}` | — | `204` |

Create:

```ts
interface CreateAttendanceSessionRequest {
  classId: number
  semesterId: number
  attendanceDate: string
  sessionPeriod: 'MORNING' | 'AFTERNOON'
}
```

The current exception wire enum is documented canonically in
[`07-enums-and-known-drift.md`](07-enums-and-known-drift.md).

**Known drift:** Requirement v2 uses more descriptive wording such as `EXCUSED_ABSENCE` / `UNEXCUSED_ABSENCE`; current API wire enum is the four values above. FE must use the current wire values until backend contract changes.

Student row:

```ts
interface AttendanceStudent {
  studentId: number
  studentCode: string
  studentName: string
  attendanceRecordId: number | null
  status: string
  note: string | null
  recordedBy: number | null
  recordedAt: string | null
  updatedBy: number | null
  updatedAt: string | null
}
```

A row with no exception is allowed to represent derived `PRESENT`.

### Academic-office attendance adjustment

Accepted bases:

```text
/api/v2/office/attendance-sessions
/api/v2/academic-office/attendance/sessions
```

Same session/student/exception contract as Teacher attendance.

Authorization:

```text
ADMIN
ACADEMIC_OFFICE
```

### Student self attendance history

```text
GET /api/v2/attendance/students/me/history
```

Authorization: `STUDENT`.

Query:

```text
academicYearId?
semesterId?
from?
to?
page?       default 0
size?       default 10
```

Response includes:

```text
items
summary
page
size
totalElements
totalPages
```

Summary:

```text
validSessionCount
presentCount
excusedAbsenceCount
unexcusedAbsenceCount
lateCount
earlyLeaveCount
```

### Class attendance summary

```text
GET /api/v2/attendance/classes/{classId}/summary
```

Authorization: `TEACHER`.

Required query:

```text
semesterId
from
to
```

Optional:

```text
page default 0
size default 20
```

Response includes class info, aggregate counts, paged per-student summaries and `attendanceRate`.
