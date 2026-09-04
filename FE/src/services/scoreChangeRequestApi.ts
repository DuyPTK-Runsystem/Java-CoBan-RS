import { apiClient } from '@/services/apiClient'
import type {
  CreateScoreChangeRequest,
  RejectScoreChangeRequest,
  ScoreChangeRequestDetail,
  ScoreChangeRequestFilter,
  ScoreChangeRequestPage,
} from '@/types/scoreChangeRequest'

const basePath = '/api/v2/score-change-requests'

export function fetchScoreChangeRequests(token: string, filter: ScoreChangeRequestFilter): Promise<ScoreChangeRequestPage> {
  const query = new URLSearchParams({ page: String(filter.page), size: String(filter.size) })
  if (filter.status) query.set('status', filter.status)
  if (filter.scorebookId !== undefined) query.set('scorebookId', String(filter.scorebookId))
  if (filter.columnId !== undefined) query.set('columnId', String(filter.columnId))
  if (filter.studentCode?.trim()) query.set('studentCode', filter.studentCode.trim())
  return apiClient.get<ScoreChangeRequestPage>(basePath, { token, query })
}

export function fetchScoreChangeRequest(token: string, requestId: number): Promise<ScoreChangeRequestDetail> {
  return apiClient.get<ScoreChangeRequestDetail>(`${basePath}/${requestId}`, { token })
}

export function createScoreChangeRequest(token: string, request: CreateScoreChangeRequest): Promise<ScoreChangeRequestDetail> {
  return apiClient.post<ScoreChangeRequestDetail>(basePath, request, { token })
}

export function approveScoreChangeRequest(token: string, requestId: number): Promise<ScoreChangeRequestDetail> {
  return apiClient.post<ScoreChangeRequestDetail>(`${basePath}/${requestId}/approve`, undefined, { token })
}

export function rejectScoreChangeRequest(token: string, requestId: number, request: RejectScoreChangeRequest): Promise<ScoreChangeRequestDetail> {
  return apiClient.post<ScoreChangeRequestDetail>(`${basePath}/${requestId}/reject`, request, { token })
}

export function cancelScoreChangeRequest(token: string, requestId: number): Promise<ScoreChangeRequestDetail> {
  return apiClient.post<ScoreChangeRequestDetail>(`${basePath}/${requestId}/cancel`, undefined, { token })
}
