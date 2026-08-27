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
