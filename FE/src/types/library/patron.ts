import type { ResultPaginationDTO } from '@/types/notification'
import type { UserRole } from '@/types/user'

export type LibraryPatronStatus = 'ACTIVE' | 'BORROWING_SUSPENDED' | 'CLOSED'
export type LibraryCardStatus = 'ACTIVE' | 'EXPIRED' | 'REVOKED'

export interface LibraryCardSummary {
  cardNo: string
  patronId: number
  status: LibraryCardStatus
  issuedAt: string
  expiresAt: string
  payloadVersion: string
  policyVersion: string
  revokedAt?: string | null
  revokedReason?: string | null
}

export interface LibraryPatronSummary {
  patronId: number
  userId: number
  displayName: string
  status: LibraryPatronStatus
  joinedAt: string
  suspensionReasons: string[]
  currentCard: LibraryCardSummary | null
}

export interface LibraryPatronPageQuery {
  keyword?: string
  status?: LibraryPatronStatus
  page: number
  size: number
  sort: string
}

export type LibraryPatronPage = ResultPaginationDTO<LibraryPatronSummary>

export interface LibraryPatronActivationCandidate {
  userId: number
  displayName: string
  username: string
  roleCode: UserRole
}

export type LibraryPatronActivationCandidatePage = ResultPaginationDTO<LibraryPatronActivationCandidate>

export interface ActivateLibraryPatronRequest {
  userId: number
}

export interface UpdateLibraryPatronStatusRequest {
  status: LibraryPatronStatus
  reason: string
}

export interface IssueLibraryCardRequest {
  patronId: number
  expiresAt: string
}

export interface RevokeLibraryCardRequest {
  reason: string
}

export interface ReissueLibraryCardRequest {
  expiresAt: string
  reason: string
}

export interface VerifyLibraryCardRequest {
  payload: string
}

export interface LibraryCardVerificationResult {
  valid: boolean
  card?: LibraryCardSummary | null
  patronStatus?: LibraryPatronStatus | null
  code?: string | null
  message?: string | null
}

export type LibraryCardHistory = LibraryCardSummary[]
