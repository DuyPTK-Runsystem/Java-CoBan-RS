import type { PageResponse } from '@/types/api'
import type { ScoreStatus } from '@/types/scorebook'

export type ScoreChangeRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | 'APPLIED'
export type ScoreSnapshotStatus = 'UNSCORED' | 'SCORED' | 'ABSENT' | 'EXEMPTED' | 'CANCELLED'

export interface ScoreChangeRequest {
  requestId: number
  assessmentColumnId: number
  studentId: number
  studentCode: string
  studentName: string
  proposedStatus: ScoreStatus
  proposedValue: number | null
  requestedBy: number
  requestedAt: string
  status: ScoreChangeRequestStatus
  reviewedBy: number | null
  reviewedAt: string | null
}

export interface ScoreChangeRequestDetail extends ScoreChangeRequest {
  studentScoreId: number | null
  beforeStatus: ScoreSnapshotStatus
  beforeValue: number | null
  reason: string
  rejectionReason: string | null
  appliedAt: string | null
}

export interface ScoreChangeRequestFilter {
  status?: ScoreChangeRequestStatus
  scorebookId?: number
  columnId?: number
  studentCode?: string
  page: number
  size: number
}

export interface CreateScoreChangeRequest {
  assessmentColumnId: number
  studentCode: string
  proposedStatus: ScoreStatus
  proposedValue: number | null
  reason: string
}

export interface RejectScoreChangeRequest {
  rejectionReason: string
}

export type ScoreChangeRequestPage = PageResponse<ScoreChangeRequest>
