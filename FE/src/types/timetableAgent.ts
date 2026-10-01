export interface TimetableAgentEntry {
  assignmentId: number
  periodId: number
  functionalRoomId: number | null
  validFrom: string
  validTo: string
}

export interface TimetableAgentExistingEntry extends TimetableAgentEntry {
  entryId: number
}

export interface TimetableAgentRequest {
  targetRevisionId: number
  expectedVersion: number
  classIds: number[]
  validFrom: string
  validTo: string
  demands: { assignmentId: number; periodsPerWeek: number }[]
  lockedEntryIds: number[]
  preferences: string
  userRequest: string
}

export type TimetableAgentStatus = 'NEEDS_INPUT' | 'NO_SOLUTION_FOUND' | 'CONFLICTS'
  | 'READY_FOR_REVIEW' | 'APPROVED' | 'SAVED' | 'STALE' | 'EXPIRED'

export interface TimetableAgentProposal {
  proposalId: string
  proposalVersion: number
  proposalHash: string
  targetRevisionId: number
  expectedVersion: number
  status: TimetableAgentStatus
  snapshotId: string
  expiresAt: string
  entries: TimetableAgentEntry[]
  issues: { code: string; severity: 'BLOCKING' | 'WARNING'; path: string; message: string }[]
  explanation: string
  diff: {
    added: TimetableAgentEntry[]
    removed: TimetableAgentExistingEntry[]
    unchanged: TimetableAgentExistingEntry[]
  }
  capabilities: { canGenerate: boolean; canApprove: boolean; canExecute: boolean }
}

export interface TimetableAgentReceipt {
  actionId: string
  proposalId: string
  targetRevisionId: number
  newVersion: number
  savedEntryCount: number
  committedAt: string
  status: 'SAVED_DRAFT'
}

export interface TimetableAgentPendingState {
  actionId: string
  proposalId: string
  targetRevisionId: number
  status: 'PENDING'
  leaseExpiresAt: string
}

export interface TimetableAgentFailedState {
  actionId: string
  proposalId: string
  targetRevisionId: number
  status: 'FAILED'
  errorCode: string
  retryable: boolean
}

export type TimetableAgentActionState = TimetableAgentReceipt | TimetableAgentPendingState | TimetableAgentFailedState

export type TimetableAgentPhase = 'idle' | 'generating' | 'approving' | 'executing' | 'recovering'
  | 'response-lost' | 'invalid-schema' | 'provider-timeout' | 'denied' | 'unavailable' | 'stale' | 'expired'

export interface TimetableAgentAssignmentOption {
  id: number
  classId: number
  className: string
  subjectName: string
  teacherName: string
}

export interface TimetableAgentPendingAction {
  proposalId: string
  proposalVersion: number
  targetRevisionId: number
  idempotencyKey: string
}
