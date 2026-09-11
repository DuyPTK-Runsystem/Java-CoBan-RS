export type SessionType = 'MORNING' | 'AFTERNOON'

export type TimetableRevisionStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export type TimetableIssueSeverity = 'BLOCKING' | 'WARNING'

export interface PaginatedMeta {
  page: number
  size: number
  totalPages: number
  totalElements: number
}

export interface PaginatedResult<T> {
  meta: PaginatedMeta
  result: T[]
}

export interface TimetableCapabilities {
  canEdit: boolean
  canValidate: boolean
  canPublish: boolean
  canRevise: boolean
}

export interface TimetablePeriod {
  id: number
  semesterId: number
  dayOfWeek: number
  session: SessionType
  periodIndex: number
  periodName: string
  startTime: string
  endTime: string
}

export interface TimetableSummary {
  timetableId: number
  semesterId: number
  semesterName: string
  currentRevisionId?: number | null
  currentRevisionNumber?: number | null
  effectiveFrom?: string | null
  effectiveTo?: string | null
  status?: TimetableRevisionStatus | null
  totalRevisions: number
}

export interface TimetableDetail {
  timetableId: number
  semesterId: number
  semesterName: string
  revisionId: number
  revisionNumber: number
  status: TimetableRevisionStatus
  effectiveFrom: string
  effectiveTo?: string | null
  policyId?: number | null
  policyVersion?: number | null
  version: number
  headVersion: number
  blockingCount: number
  warningCount: number
  capabilities: TimetableCapabilities
}

export interface TimetableEntry {
  id: number
  revisionId: number
  assignmentId: number
  periodId: number
  functionalRoomId?: number | null
  validFrom: string
  validTo: string
  classId: number
  className: string
  subjectId: number
  subjectName: string
  teacherId: number
  teacherName: string
  roomCode?: string | null
  roomName?: string | null
  dayOfWeek: number
  session: SessionType
  periodIndex: number
}

export interface TimetableIssue {
  code: string
  severity: TimetableIssueSeverity
  message: string
  entryId?: number | null
  classId?: number | null
  className?: string | null
  teacherId?: number | null
  teacherName?: string | null
  functionalRoomId?: number | null
  roomName?: string | null
  periodId?: number | null
  dayOfWeek?: number | null
  session?: SessionType | null
  periodIndex?: number | null
}

export interface TeacherLoadReduction {
  ruleCode: string
  ruleName: string
  reductionPeriods: number
  evidenceInfo?: string | null
}

export interface TeacherLoad {
  teacherId: number
  teacherName: string
  assignedPeriods: number
  basePeriods: number
  reductions: number
  targetPeriods: number
  difference: number
  evaluationStatus: string
  reductionDetails: TeacherLoadReduction[]
}

export interface TimetableReview {
  revisionId: number
  revisionVersion: number
  validatedAt: string
  status: TimetableRevisionStatus
  blockingCount: number
  warningCount: number
  issues: TimetableIssue[]
  teacherLoads: TeacherLoad[]
}

export interface TeacherLoadPolicy {
  id: number
  policyName: string
  sourceDocument: string
  effectiveFrom: string
  effectiveTo?: string | null
  policyVersion: number
  standardPeriodsHighSchool: number
  homeroomReduction: number
  nursingChildReduction: number
  active: boolean
  version: number
}

export interface TeacherLoadEligibility {
  id: number
  teacherId: number
  teacherName: string
  conditionType: string
  validFrom: string
  validTo: string
  evidenceInfo?: string | null
  version: number
}

export interface CreateTimetablePayload {
  semesterId: number
  effectiveFrom: string
  effectiveTo?: string | null
  policyId?: number | null
  expectedHeadVersion?: number | null
}

export interface ReqEntryItem {
  id?: number | null
  assignmentId: number
  periodId: number
  functionalRoomId?: number | null
  validFrom: string
  validTo: string
}

export interface UpdateTimetableEntriesPayload {
  expectedVersion: number
  upserts: ReqEntryItem[]
  deletedEntryIds: number[]
}

export interface PublishTimetablePayload {
  expectedVersion: number
  expectedHeadVersion: number
}

export interface CreateRevisionPayload {
  expectedVersion: number
  effectiveFrom: string
}

