export type ScorebookStatus = 'DRAFT' | 'OPEN' | 'PUBLISHED' | 'CLOSED'
export type AssessmentType = 'KTTT' | 'KTĐK' | 'KTCK'
export type AssessmentColumnStatus = 'ACTIVE' | 'INACTIVE'
export type ScoreStatus = 'SCORED' | 'ABSENT' | 'EXEMPTED' | 'CANCELLED'

export interface AssessmentColumn {
  id: number
  scorebookId: number
  assessmentType: AssessmentType
  columnNo: number
  columnName: string | null
  weightFactor: number | null
  required: boolean
  status: AssessmentColumnStatus
}

export interface SkillWeightConfig {
  id: number
  scorebookId: number
  ktttWeightPercent: number
  ktdkWeightPercent: number
  ktckWeightPercent: number
  configuredBy: number | null
  configuredAt: string | null
  lockedBy: number | null
  lockedAt: string | null
}

export interface Scorebook {
  id: number
  classSubjectId: number
  status: ScorebookStatus
  publishedAt: string | null
  publishedBy: number | null
  closedAt: string | null
  columns: AssessmentColumn[]
  skillWeightConfig: SkillWeightConfig | null
}

export interface ScoreGridColumn {
  columnId: number
  assessmentType: AssessmentType
  columnNo: number
  columnName: string | null
}

export interface StudentScore {
  scoreId: number | null
  assessmentColumnId: number
  studentId: number
  studentCode: string
  studentName: string
  scoreStatus: ScoreStatus
  scoreValue: number | null
  note: string | null
  enteredBy: number | null
  enteredAt: string | null
  updatedBy: number | null
  updatedAt: string | null
  version: number | null
}

export interface StudentScoreGridRow {
  studentId: number
  studentCode: string
  studentName: string
  scores: Record<string, StudentScore>
}

export interface StudentScoreGrid {
  scorebookId: number
  classSubjectId: number
  scorebookStatus: ScorebookStatus
  columns: ScoreGridColumn[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  students: StudentScoreGridRow[]
}

export interface CreateScorebookRequest {
  classSubjectId: number
}

export interface CreateAssessmentColumnRequest {
  assessmentType: AssessmentType
  columnNo: number
  columnName?: string | null
}

export interface UpdateAssessmentColumnRequest {
  columnName?: string | null
}

export interface UpsertSkillWeightRequest {
  ktttWeightPercent: number
  ktdkWeightPercent: number
  ktckWeightPercent: number
}

export interface UpsertStudentScoreRequest {
  scoreStatus: ScoreStatus
  scoreValue?: number | null
  note?: string | null
  expectedVersion?: number | null
}

export interface BulkScoreItem extends UpsertStudentScoreRequest {
  studentId?: number | null
  studentCode?: string | null
}

export interface BulkUpsertStudentScoreRequest {
  items: BulkScoreItem[]
}
