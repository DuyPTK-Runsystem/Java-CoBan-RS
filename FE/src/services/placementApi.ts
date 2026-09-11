import { apiClient } from '@/services/apiClient'
import type { ConfirmPlacementRequest, CreatePlacementSessionRequest, PlacementResultsPage, PlacementSession, UpdatePlacementSessionRequest } from '@/types/placement'

const path = (id: number) => `/api/v3/placement-sessions/${id}`

export function createPlacementSession(token: string, payload: CreatePlacementSessionRequest): Promise<PlacementSession> {
  return apiClient.post<PlacementSession>('/api/v3/placement-sessions', payload, { token })
}

export function getPlacementSession(token: string, id: number): Promise<PlacementSession> {
  return apiClient.get<PlacementSession>(path(id), { token })
}

export function updatePlacementSession(token: string, id: number, payload: UpdatePlacementSessionRequest): Promise<PlacementSession> {
  return apiClient.put<PlacementSession>(path(id), payload, { token })
}

export function simulatePlacementSession(token: string, id: number, expectedVersion: number): Promise<PlacementSession> {
  return apiClient.post<PlacementSession>(`${path(id)}/simulate`, { expectedVersion }, { token })
}

export function fetchPlacementResults(token: string, id: number, page = 0, pageSize = 20): Promise<PlacementResultsPage> {
  return apiClient.get<PlacementResultsPage>(`${path(id)}/results`, {
    token,
    query: new URLSearchParams({ page: String(page), size: String(pageSize) }),
  })
}

export function confirmPlacementSession(token: string, id: number, payload: ConfirmPlacementRequest): Promise<PlacementSession> {
  return apiClient.post<PlacementSession>(`${path(id)}/confirm`, payload, { token })
}

export function cancelPlacementSession(token: string, id: number, expectedVersion: number): Promise<PlacementSession> {
  return apiClient.post<PlacementSession>(`${path(id)}/cancel`, { expectedVersion }, { token })
}
