import { apiClient } from '@/services/apiClient'
import type {
  LibraryBorrowRequest,
  LibraryBorrowResponse,
  LibraryLoanPage,
  LibraryLoanQuery,
  LibraryReturnRequest,
  LibraryReturnResponse,
  LibraryLoan,
  LibraryLostResult,
} from '@/types/library/circulation'

const LOANS_PATH = '/api/v2/loans'
const RETURNS_PATH = '/api/v2/returns'

export function borrowLibraryCopies(request: LibraryBorrowRequest, token: string): Promise<LibraryBorrowResponse> {
  return apiClient.post<LibraryBorrowResponse>(LOANS_PATH, request, { token })
}

export function listLibraryLoans(query: LibraryLoanQuery, token: string): Promise<LibraryLoanPage> {
  return apiClient.get<LibraryLoanPage>(LOANS_PATH, { token, query })
}

export function renewLibraryLoan(loanId: number, token: string): Promise<LibraryLoan> {
  return apiClient.post<LibraryLoan>(`${LOANS_PATH}/${loanId}/renew`, {}, { token })
}

export function returnLibraryCopies(request: LibraryReturnRequest, token: string): Promise<LibraryReturnResponse> {
  return apiClient.post<LibraryReturnResponse>(RETURNS_PATH, request, { token })
}

export function markLibraryCopyLost(barcode: string, reason: string, token: string): Promise<LibraryLostResult> {
  return apiClient.post<LibraryLostResult>(`/api/v2/book-copies/${encodeURIComponent(barcode)}/lost`, { reason }, { token })
}
