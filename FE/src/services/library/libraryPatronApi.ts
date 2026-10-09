import { apiClient } from '@/services/apiClient'
import type {
  ActivateLibraryPatronRequest,
  IssueLibraryCardRequest,
  LibraryCardHistory,
  LibraryCardSummary,
  LibraryCardVerificationResult,
  LibraryPatronActivationCandidatePage,
  LibraryPatronPage,
  LibraryPatronPageQuery,
  LibraryPatronSummary,
  ReissueLibraryCardRequest,
  RevokeLibraryCardRequest,
  UpdateLibraryPatronStatusRequest,
  VerifyLibraryCardRequest,
} from '@/types/library/patron'

const PATRONS_PATH = '/api/v2/library-patrons'
const CARDS_PATH = '/api/v2/library-cards'

export function listLibraryPatrons(query: LibraryPatronPageQuery, token: string): Promise<LibraryPatronPage> {
  return apiClient.get<LibraryPatronPage>(PATRONS_PATH, { token, query })
}

export function listPatronActivationCandidates(
  keyword: string,
  page: number,
  size: number,
  token: string,
): Promise<LibraryPatronActivationCandidatePage> {
  return apiClient.get<LibraryPatronActivationCandidatePage>(`${PATRONS_PATH}/activation-candidates`, {
    token,
    query: { keyword: keyword.trim() || undefined, page, size },
  })
}

export function activateLibraryPatron(request: ActivateLibraryPatronRequest, token: string): Promise<LibraryPatronSummary> {
  return apiClient.post<LibraryPatronSummary>(PATRONS_PATH, request, { token })
}

export function getLibraryPatron(patronId: number, token: string): Promise<LibraryPatronSummary> {
  return apiClient.get<LibraryPatronSummary>(`${PATRONS_PATH}/${patronId}`, { token })
}

export function getMyLibraryPatron(token: string): Promise<LibraryPatronSummary> {
  return apiClient.get<LibraryPatronSummary>(`${PATRONS_PATH}/me`, { token })
}

export function updateLibraryPatronStatus(
  patronId: number,
  request: UpdateLibraryPatronStatusRequest,
  token: string,
): Promise<LibraryPatronSummary> {
  return apiClient.patch<LibraryPatronSummary>(`${PATRONS_PATH}/${patronId}/status`, request, { token })
}

export function listLibraryPatronCards(patronId: number, token: string): Promise<LibraryCardHistory> {
  return apiClient.get<LibraryCardHistory>(`${PATRONS_PATH}/${patronId}/cards`, { token })
}

export function listMyLibraryCardHistory(token: string): Promise<LibraryCardHistory> {
  return apiClient.get<LibraryCardHistory>(`${CARDS_PATH}/me/history`, { token })
}

export function getMyLibraryCard(token: string): Promise<LibraryCardSummary> {
  return apiClient.get<LibraryCardSummary>(`${CARDS_PATH}/me`, { token })
}

export function issueLibraryCard(request: IssueLibraryCardRequest, token: string): Promise<LibraryCardSummary> {
  return apiClient.post<LibraryCardSummary>(CARDS_PATH, request, { token })
}

export function revokeLibraryCard(
  cardNo: string,
  request: RevokeLibraryCardRequest,
  token: string,
): Promise<LibraryCardSummary> {
  return apiClient.post<LibraryCardSummary>(`${CARDS_PATH}/${encodeURIComponent(cardNo)}/revoke`, request, { token })
}

export function reissueLibraryCard(
  cardNo: string,
  request: ReissueLibraryCardRequest,
  token: string,
): Promise<LibraryCardSummary> {
  return apiClient.post<LibraryCardSummary>(`${CARDS_PATH}/${encodeURIComponent(cardNo)}/reissue`, request, { token })
}

export function verifyLibraryCard(request: VerifyLibraryCardRequest, token: string): Promise<LibraryCardVerificationResult> {
  return apiClient.post<LibraryCardVerificationResult>(`${CARDS_PATH}/verify`, request, { token })
}

export function getLibraryCardQr(cardNo: string, token: string): Promise<Blob> {
  return apiClient.get<Blob>(`${CARDS_PATH}/${encodeURIComponent(cardNo)}/qr.png`, {
    token,
    responseType: 'blob',
    headers: { Accept: 'image/png' },
  })
}
