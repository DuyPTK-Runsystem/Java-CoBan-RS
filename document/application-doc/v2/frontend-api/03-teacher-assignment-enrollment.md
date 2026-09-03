# Frontend API v2 — Teacher, Assignment and Enrollment

[← Back to `FrontendApiGuide.md`](../FrontendApiGuide.md)

## Teacher API

Base:

```text
/api/v2/teachers
```

| Method | Path | Authorization | Request/Query | Response |
|---|---|---|---|---|
| `GET` | `/api/v2/teachers` | Authenticated | `status?` | `ResTeacherDTO[]` |
| `GET` | `/api/v2/teachers/{teacherId}` | Authenticated | — | `ResTeacherDTO` |
| `POST` | `/api/v2/teachers` | Office | `ReqCreateTeacherDTO` | `201 ResTeacherDTO` |
| `PUT` | `/api/v2/teachers/{teacherId}` | Office | `ReqUpdateTeacherDTO` | `ResTeacherDTO` |
| `DELETE` | `/api/v2/teachers/{teacherId}` | Office | — | `204` |

Teacher status uses the canonical wire enum in
[`07-enums-and-known-drift.md`](07-enums-and-known-drift.md).

Teacher DTO includes:

```text
id
userId
teacherCode
teacherName
dateOfBirth
gender
phone
email
department
joinDate
status
```

`userId` may be null.

## Teaching Assignment API

Assignment mutations remain office-only. Assignment queries are available to `TEACHER`
only for the linked teacher and assigned classes.

Office:

```text
ADMIN
ACADEMIC_OFFICE
```

| Method | Path | Request | Response |
|---|---|---|---|
| `GET` | `/api/v2/assignments/classes/{classId}` | — | `ResHomeroomAssignmentDTO[]` |
| `GET` | `/api/v2/assignments/classes/{classId}/subjects?semesterId={semesterId}` | Teacher chỉ với lớp được phân công; Office/Admin với mọi lớp | `ResSubjectTeachingAssignmentDTO[]` của toàn bộ GVBM trong lớp-học kỳ |
| `GET` | `/api/v2/assignments/teachers/{teacherId}` | Office/Teacher (own linked teacher only) | — | `ResSubjectTeachingAssignmentDTO[]` |
| `GET` | `/api/v2/assignments/teachers/{teacherId}/homeroom` | Office/Teacher (own linked teacher only) | — | `ResHomeroomAssignmentDTO[]` |
| `POST` | `/api/v2/classes/{classId}/homeroom-assignments` | `{ teacherId, validFrom, validTo? }` | `201 ResHomeroomAssignmentDTO` |
| `POST` | `/api/v2/homeroom-assignments/{assignmentId}/replace` | replace DTO | homeroom assignment |
| `POST` | `/api/v2/homeroom-assignments/{assignmentId}/end` | `{ validTo }` | homeroom assignment |
| `POST` | `/api/v2/class-subjects/{classSubjectId}/teaching-assignments` | `{ teacherId, validFrom, validTo? }` | `201 ResSubjectTeachingAssignmentDTO` |
| `POST` | `/api/v2/subject-teaching-assignments/{assignmentId}/replace` | replace DTO | subject assignment |
| `POST` | `/api/v2/subject-teaching-assignments/{assignmentId}/end` | `{ validTo }` | subject assignment |

Assignment status uses the canonical wire enum in
[`07-enums-and-known-drift.md`](07-enums-and-known-drift.md).

Important:

- GVCN/GVBM are derived from assignment data, not auth role codes;
- current query endpoints in this controller are office-only even though Requirement v2 contains broader view use cases;
- FE must follow the current endpoint authorization until a contract changes.

## Enrollment API

Base controller:

```text
/api/v2
```

Controller base auth: `ADMIN`, `ACADEMIC_OFFICE`, `TEACHER`.

Mutation endpoints are narrowed to Office.

| Method | Path | Authorization | Request/Query | Response |
|---|---|---|---|---|
| `GET` | `/api/v2/classes/{classId}/students` | Office/Teacher | — | `ResClassStudentDTO[]` |
| `POST` | `/api/v2/enrollments` | Office | `ReqCreateEnrollmentDTO` | `201 ResEnrollmentMutationDTO` |
| `POST` | `/api/v2/enrollments/bulk` | Office | `ReqBulkCreateEnrollmentDTO` | `ResEnrollmentMutationDTO` |
| `POST` | `/api/v2/enrollments/{enrollmentId}/transfer` | Office | `ReqTransferEnrollmentDTO` | mutation DTO |
| `GET` | `/api/v2/enrollments/unassigned` | Office/Teacher | `academicYearId` | unassigned student list |
| `GET` | `/api/v2/students/{studentId}/enrollments` | Office/Teacher | — | enrollment history |
| `GET` | `/api/v2/students/by-code/{studentCode}/enrollments` | Office/Teacher | — | enrollment history |

Single create supports:

```ts
interface CreateEnrollmentRequest {
  studentId?: number | null
  studentCode?: string | null
  academicYearId: number
  classId: number
  enrolledAt?: string | null
}
```

At least one identifier is required.

Bulk request currently has separate arrays:

```ts
interface BulkCreateEnrollmentRequest {
  academicYearId: number
  classId: number
  studentIds?: number[] | null
  studentCodes?: string[] | null
  enrolledAt?: string | null
}
```

**Do not invent positional pairing semantics between `studentIds` and `studentCodes`.**

Mutation response:

```ts
interface EnrollmentMutation {
  enrollments: Enrollment[]
  warnings: CapacityWarning[]
}
```

Capacity warning is **non-blocking**.

Enrollment status uses the canonical wire enum in
[`07-enums-and-known-drift.md`](07-enums-and-known-drift.md).

Transfer uses:

```text
targetClassId
effectiveAt
reason?
```

`effectiveAt` must not be future according to current validation.
