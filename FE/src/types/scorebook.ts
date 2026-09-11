export type ScorebookStatus = 'DRAFT' | 'OPEN' | 'PUBLISHED' | 'CLOSED'
export type AssessmentType = 'KTTT' | 'KTĐK' | 'KTCK'
export type AssessmentColumnStatus = 'ACTIVE' | 'INACTIVE'
export type ScoreStatus = 'SCORED' | 'ABSENT' | 'EXEMPTED' | 'CANCELLED'

export const ASSESSMENT_TYPE_ORDER: Record<string, number> = {
  KTTT: 1,
  KTTX: 1,
  'KTĐK': 2,
  KTDK: 2,
  KTCK: 3,
}

export function compareAssessmentColumns<T extends { assessmentType: AssessmentType; columnNo: number }>(a: T, b: T): number {
  const orderA = ASSESSMENT_TYPE_ORDER[a.assessmentType] ?? 99
  const orderB = ASSESSMENT_TYPE_ORDER[b.assessmentType] ?? 99
  if (orderA !== orderB) return orderA - orderB
  return a.columnNo - b.columnNo
}

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
  columnNo?: number
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

export type BulkScoreImportRowResult = 'VALID' | 'ERROR' | 'SKIPPED'

export interface BulkScoreImportFileMetadata {
  originalName?: string | null
  sizeBytes?: number | null
  sha256?: string | null
  rowCount?: number | null
}

export interface BulkScoreImportRow {
  rowNumber: number
  studentCode: string | null
  studentId: number | null
  studentName: string | null
  oldStatus: ScoreStatus | null
  oldValue: number | null
  oldVersion: number | null
  newStatus: ScoreStatus | null
  newValue: number | null
  note: string | null
  result: BulkScoreImportRowResult
  errorCode: string | null
  message: string | null
}

export interface BulkScoreImportSummary {
  totalRows?: number
  validRows: number
  errorRows: number
  skippedRows?: number
  newScores: number
  updatedScores: number
}

export interface BulkScoreImportPreview {
  batchId?: number | null
  status?: string | null
  scorebookId?: number | null
  assessmentColumnId: number
  fileName?: string | null
  file?: BulkScoreImportFileMetadata
  summary: BulkScoreImportSummary
  rows: BulkScoreImportRow[]
  /** Provisional normalized payload returned by the preview contract. */
  items?: BulkScoreItem[]
  /** Alternate provisional name allowed during the contract checkpoint. */
  normalizedItems?: BulkScoreItem[]
}
