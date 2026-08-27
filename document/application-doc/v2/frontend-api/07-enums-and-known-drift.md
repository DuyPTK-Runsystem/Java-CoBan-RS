# Frontend API v2 — Canonical Enums and Known Drift

[← Back to `FrontendApiGuide.md`](../FrontendApiGuide.md)

## Canonical current API enums for FE

The following values come from the current backend wire contract.
This is the single canonical registry for TypeScript string unions in the v2
Frontend API Guide; domain documents should link here instead of redefining them.

```ts
type AcademicYearStatus = 'DRAFT' | 'ACTIVE' | 'CLOSED'

type SemesterStatus = 'DRAFT' | 'ACTIVE' | 'LOCKED' | 'CLOSED'

type SchoolClassStatus = 'PLANNED' | 'ACTIVE' | 'CLOSED'

type SubjectType = 'ACADEMIC' | 'SKILL'
type SubjectStatus = 'ACTIVE' | 'INACTIVE'
type ApplicationScope = 'GRADE' | 'CLASS'
type SubjectApplicabilityStatus = 'ACTIVE' | 'INACTIVE'
type ClassSubjectStatus = 'ACTIVE' | 'INACTIVE' | 'COMPLETED'

type TeacherStatus = 'ACTIVE' | 'ON_LEAVE' | 'INACTIVE'
type AssignmentStatus = 'ACTIVE' | 'ENDED'

type EnrollmentStatus = 'ACTIVE' | 'COMPLETED' | 'WITHDRAWN'

type CalendarDayType = 'SCHOOL_DAY' | 'WEEKEND' | 'HOLIDAY' | 'NO_CLASS'
type CalendarSessionPeriod = 'MORNING' | 'AFTERNOON'
type CalendarSessionStatus = 'SCHEDULED' | 'NO_CLASS'

type AttendanceSessionPeriod = 'MORNING' | 'AFTERNOON'
type AttendanceExceptionStatus = 'ABSENT' | 'EXCUSED' | 'LATE' | 'EARLY_LEAVE'

type ScorebookStatus = 'DRAFT' | 'OPEN' | 'PUBLISHED' | 'CLOSED'
type AssessmentType = 'KTTT' | 'KTĐK' | 'KTCK'
type AssessmentColumnStatus = 'ACTIVE' | 'INACTIVE'
type ScoreStatus = 'SCORED' | 'ABSENT' | 'EXEMPTED' | 'CANCELLED'

type ScoreChangeRequestStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'APPLIED'

type ScoreSnapshotStatus =
  | 'UNSCORED'
  | 'SCORED'
  | 'ABSENT'
  | 'EXEMPTED'
  | 'CANCELLED'

type CalculationStatus = 'IN_PROGRESS' | 'FINISH'

type CalculationTaskStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCEEDED'
  | 'FAILED'

type CalculationTaskType = 'STUDENT_YEAR_RECALC'

type CalculationResultSource = 'REGULAR' | 'RETAKE'

type RetakeExamStatus = 'PLANNED' | 'SCORED' | 'CANCELLED'
```

## Known drift giữa Requirement/Data Model và API hiện tại

### Subject type

Requirement/API dùng:

```text
ACADEMIC
SKILL
```

Một số data-model text dùng `NORMAL | SKILL`.

FE dùng current wire:

```text
ACADEMIC | SKILL
```

### Attendance exception

Requirement wording:

```text
EXCUSED_ABSENCE
UNEXCUSED_ABSENCE
LATE
EARLY_LEAVE
```

Current API enum:

```text
ABSENT
EXCUSED
LATE
EARLY_LEAVE
```

Không tự đổi string ở FE service nếu backend chưa đổi contract.

### NOT_ENTERED

Requirement mô tả trạng thái business `NOT_ENTERED`.

Current `ScoreStatus` enum không có `NOT_ENTERED`.

Score grid thể hiện chưa nhập bằng việc **không có score entry** cho cell.

### Calculation result source

Target data model có text về `MIXED`.

Current Java enum chỉ có:

```text
REGULAR
RETAKE
```

### Auth roles

Requirement v2 cần role-aware UI.

Current login/account/JWT chưa expose role.

Đây là **contract blocker**, không phải frontend-only TODO.

### Student averageScore

Current Student DTO vẫn có `averageScore`.

Target v2 deprecates field này làm nguồn điểm chính thức.

FE chỉ coi đây là compatibility data cho legacy Student contract.
