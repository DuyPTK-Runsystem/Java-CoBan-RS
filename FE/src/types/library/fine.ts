import type { ResultPaginationDTO } from '@/types/notification'

export type LibraryFineType = 'OVERDUE' | 'LOST_ITEM'
export type LibraryFineStatus = 'UNPAID' | 'PAID' | 'WAIVED'

export interface LibraryFine {
  fineId: number
  loanId: number
  type: LibraryFineType
  status: LibraryFineStatus
  provisional: boolean
  amount: string
  currency: 'VND'
  calculatedThrough: string
  policyVersion: number | string
  paidAt: string | null
  paymentReference: string | null
  waivedAt: string | null
  waiveReason: string | null
}

export interface PayLibraryFineRequest {
  reference: string
}

export interface WaiveLibraryFineRequest {
  reason: string
}

export interface LibraryBatchJob {
  runId: number
  jobName: string
  status: 'STARTED' | 'COMPLETED' | 'FAILED'
  runDate: string
  processedCount: number
  skippedCount: number
  startedAt: string | null
  completedAt: string | null
}

export interface RunOverdueFineBatchRequest {
  runDate: string
}

export interface LibraryFineQuery {
  patronId?: number
  status?: LibraryFineStatus
  page?: number
  pageSize?: number
}

export type LibraryFinePage = ResultPaginationDTO<LibraryFine>
