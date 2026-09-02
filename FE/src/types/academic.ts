export type AcademicYearStatus = 'DRAFT' | 'ACTIVE' | 'CLOSED'
export type SemesterStatus = 'DRAFT' | 'ACTIVE' | 'LOCKED' | 'CLOSED'
export type SemesterLockReportStatus = 'COMPLETE' | 'INCOMPLETE' | 'FAILED'

export interface AcademicYear {
  id: number
  code: string
  startDate: string
  endDate: string
  status: AcademicYearStatus
  notes: string | null
}

export interface AcademicYearFormValues {
  code: string
  startDate: string
  endDate: string
  status: AcademicYearStatus
  notes: string
}

export interface AcademicYearRequest {
  code: string
  startDate: string
  endDate: string
  status: AcademicYearStatus
  notes: string | null
}

export interface Semester {
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

export interface SemesterFormValues {
  code: string
  name: string
  displayOrder: number | null
  startDate: string
  endDate: string
  automaticLockAt: string
}

export interface CreateSemesterRequest {
  academicYearId: number
  code: string
  name: string
  displayOrder: number
  startDate: string
  endDate: string
  automaticLockAt: string | null
  status?: SemesterStatus
}

export interface UpdateSemesterRequest {
  code: string
  name: string
  displayOrder: number
  startDate: string
  endDate: string
  automaticLockAt: string | null
  status?: SemesterStatus
}

export interface ReopenSemesterRequest {
  reason: string
}

export interface SemesterCompletenessSummary {
  complete: boolean
  missingKtdkCount: number
  invalidKtckCount: number
  missingSkillColumnsCount: number
  unenteredScoreCount: number
  studentWithoutScoreDataCount: number
  unpublishedScorebookCount: number
  pendingScoreChangeRequestCount: number
  details: string[]
}

export interface SemesterCompletenessReport {
  reportId: number | null
  runId: number | null
  semesterId: number
  checkpointCode: string
  reportStatus: SemesterLockReportStatus
  evaluatedAt: string
  scopeType: string
  summary: SemesterCompletenessSummary
  failureReason: string | null
  correlationId: string | null
}

export type SemesterNotificationStatus = 'PENDING' | 'SENT' | 'FAILED'

/** UI-safe notification model; transport-only fields are intentionally omitted. */
export interface SemesterNotification {
  id: number
  semesterId: number
  recipientEmail: string
  recipientRole: string
  status: SemesterNotificationStatus
  subject: string
  attemptCount: number
  sentAt: string | null
  errorMessage: string | null
  createdAt: string
  updatedAt: string
}

export type SchoolClassStatus = 'PLANNED' | 'ACTIVE' | 'CLOSED'
export type SubjectType = 'ACADEMIC' | 'SKILL'
export type SubjectStatus = 'ACTIVE' | 'INACTIVE'
export type ApplicationScope = 'GRADE' | 'CLASS'
export type SubjectApplicabilityStatus = 'ACTIVE' | 'INACTIVE'
export type ClassSubjectStatus = 'ACTIVE' | 'INACTIVE' | 'COMPLETED'

export interface GradeLevel {
  id: number
  code: string
  name: string
  gradeLevel: 6 | 7 | 8 | 9
  displayOrder: number
  nextGradeId: number | null
  active: boolean
  description: string | null
}

export interface GradeLevelFormValues {
  code: string
  name: string
  gradeLevel: 6 | 7 | 8 | 9 | null
  displayOrder: number | null
  nextGradeId: number | null
  active: boolean
  description: string
}

export interface GradeLevelRequest {
  code: string
  name: string
  gradeLevel: 6 | 7 | 8 | 9
  displayOrder: number
  nextGradeId: number | null
  active: boolean
  description: string | null
}

export interface SchoolClass {
  id: number
  academicYearId: number
  gradeLevelId: number
  classCode: string
  className: string | null
  capacity: number | null
  status: SchoolClassStatus
}

export interface SchoolClassFormValues {
  academicYearId: number | null
  gradeLevelId: number | null
  classCode: string
  className: string
  capacity: number | null
  status: SchoolClassStatus
}

export interface CreateSchoolClassRequest {
  academicYearId: number
  gradeLevelId: number
  classCode: string
  className: string | null
  capacity: number | null
  status: SchoolClassStatus
}

export interface UpdateSchoolClassRequest {
  gradeLevelId: number
  classCode: string
  className: string | null
  capacity: number | null
  status: SchoolClassStatus
}

export interface Subject {
  id: number
  code: string
  name: string
  subjectType: SubjectType
  applicationScope: ApplicationScope
  status: SubjectStatus
}

export interface SubjectFormValues {
  code: string
  name: string
  subjectType: SubjectType
  applicationScope: ApplicationScope
  status: SubjectStatus
}

export interface SubjectRequest {
  code: string
  name: string
  subjectType: SubjectType
  applicationScope: ApplicationScope
  status: SubjectStatus
}

export interface SubjectApplicability {
  id: number
  subjectId: number
  semesterId: number
  scopeType: ApplicationScope
  gradeLevelId: number | null
  classId: number | null
  status: SubjectApplicabilityStatus
}

export interface SubjectApplicabilityFormValues {
  semesterId: number | null
  scopeType: ApplicationScope
  gradeLevelId: number | null
  classId: number | null
}

export interface SubjectApplicabilityRequest {
  semesterId: number
  scopeType: ApplicationScope
  gradeLevelId: number | null
  classId: number | null
}

export interface UpdateSubjectApplicabilityRequest extends SubjectApplicabilityRequest {
  status: SubjectApplicabilityStatus
}

export interface ClassSubject {
  id: number
  classId: number
  subjectId: number
  semesterId: number
  status: ClassSubjectStatus
}

export interface ClassSubjectFormValues {
  subjectId: number | null
  status: ClassSubjectStatus
}

export interface CreateClassSubjectRequest {
  classId: number
  subjectId: number
  semesterId: number
  status: ClassSubjectStatus
}

export interface UpdateClassSubjectRequest {
  status: ClassSubjectStatus
}
