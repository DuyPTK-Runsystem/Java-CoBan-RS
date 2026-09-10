export const PLACEMENT_RULE_VERSION = '074-v1'

export type PlacementSessionStatus = 'DRAFT' | 'SIMULATED' | 'READY_FOR_CONFIRM' | 'CONFIRMED' | 'CANCELLED'
export type PlacementResultStatus = 'AUTO_ASSIGNED' | 'MANUAL_REQUIRED'
export type PlacementIssueSeverity = 'WARNING' | 'BLOCKING'
export type PlacementClassProfile = 'ADVANCED' | 'SUPPORT' | 'REGULAR'
export type PlacementCandidateSource = 'CONTINUING' | 'NEW_ADMISSION' | 'REPEAT'

export interface PlacementClassTargetRequest {
  classId: number
  profile: PlacementClassProfile
  capacity?: number | null
}

export interface PlacementClassTarget {
  classId: number
  classCode: string
  className: string | null
  profile: PlacementClassProfile
  capacity: number | null
  genderTargetMale: number | null
  genderTargetFemale: number | null
}

export interface PlacementCandidateRequest {
  studentId: number
  targetGradeId: number
  sourceType: PlacementCandidateSource
  score: number | null
  scoreSourceReference: string | null
  genderSnapshot: 'MALE' | 'FEMALE' | null
  eligibilityEvidence: string | null
  approvalReference: string | null
}

export interface PlacementResult {
  id: number
  studentId: number
  targetClassId: number | null
  resultStatus: PlacementResultStatus
  score: number | null
  issueCode: string | null
  issueSeverity: PlacementIssueSeverity | null
  explanation: string
}

export interface PlacementResultsMeta {
  page: number
  pageSize: number
  totalPages: number
  totalItems: number
}

/** Canonical zero-based envelope returned by GET /placement-sessions/{id}/results. */
export interface PlacementResultsPage {
  meta: PlacementResultsMeta
  result: PlacementResult[]
}

export interface PlacementSession {
  id: number
  academicYearId: number
  targetGradeId: number
  status: PlacementSessionStatus
  ruleVersion: string
  version: number
  targetClassIds: number[]
  targetClasses: PlacementClassTarget[]
  results: PlacementResult[]
}

export interface CreatePlacementSessionRequest {
  academicYearId: number
  targetGradeId: number
  targetClasses: PlacementClassTargetRequest[]
  candidates: PlacementCandidateRequest[]
  ruleVersion: string
}

export interface UpdatePlacementSessionRequest {
  expectedVersion: number
  targetClasses: PlacementClassTargetRequest[]
}

export interface ConfirmPlacementRequest {
  expectedVersion: number
  idempotencyKey: string
}
