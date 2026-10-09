import type { ResultPaginationDTO } from '@/types/notification'
import type { LibraryFine } from '@/types/library/fine'

export type LibraryLoanStatus = 'ACTIVE' | 'RETURNED' | 'LOST'
export type LibraryErrorCode =
  | 'COPY_ALREADY_ON_LOAN'
  | 'MAX_ACTIVE_LOANS'
  | 'REFERENCE_ONLY'
  | 'PATRON_BORROWING_SUSPENDED'
  | 'CARD_EXPIRED'
  | 'CARD_REVOKED'
  | 'ACTIVE_LOAN_NOT_FOUND'
  | 'RENEW_LIMIT_REACHED'
  | 'COPY_RESERVED'
  | 'COPY_NOT_FOUND'
  | 'INVALID_FINE_AMOUNT'
  | 'LIBRARY_RESOURCE_FORBIDDEN'

export interface LibraryLoan {
  loanId: number
  patronId: number
  copyId: number
  copyBarcode: string
  bookId: number
  bookTitle: string
  cardNo: string
  status: LibraryLoanStatus
  borrowedAt: string
  dueAt: string
  returnedAt: string | null
  lostAt: string | null
  renewCount: number
  policyVersion: number | string
}

export interface LibraryBorrowRequest {
  patronId: number
  cardNo: string
  copyBarcodes: string[]
}

export interface LibraryBorrowResponse {
  items: LibraryLoan[]
}

export interface LibraryReturnRequest {
  copyBarcodes: string[]
}

export interface LibraryReturnItem {
  loan: LibraryLoan
  fine: import('@/types/library/fine').LibraryFine | null
}

export interface LibraryReturnResponse {
  items: LibraryReturnItem[]
}

export interface LibraryLostResult {
  loan: LibraryLoan
  fine: LibraryFine
}

export interface LibraryLoanQuery {
  patronId?: number
  status?: LibraryLoanStatus
  dueBefore?: string
  page?: number
  pageSize?: number
}

export type LibraryLoanPage = ResultPaginationDTO<LibraryLoan>
