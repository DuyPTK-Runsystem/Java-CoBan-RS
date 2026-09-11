import { apiClient } from '@/services/apiClient'
import type {
  CreateFunctionalRoomPayload,
  FunctionalRoom,
  RoomStatus,
  UpdateFunctionalRoomPayload,
} from '@/types/functionalRoom'
import type { PaginatedResult } from '@/types/timetable'

const basePath = '/api/v2/functional-rooms'

export interface ListRoomsParams {
  search?: string
  status?: RoomStatus
  page?: number
  size?: number
}

export function listFunctionalRooms(
  params?: ListRoomsParams,
  token?: string,
): Promise<PaginatedResult<FunctionalRoom>> {
  const query: Record<string, string | number | undefined> = {}
  if (params?.search) query.search = params.search
  if (params?.status) query.status = params.status
  if (params?.page !== undefined) query.page = params.page
  if (params?.size !== undefined) query.size = params.size

  return apiClient.get<PaginatedResult<FunctionalRoom>>(basePath, { token, query })
}

export function getFunctionalRoom(id: number, token?: string): Promise<FunctionalRoom> {
  return apiClient.get<FunctionalRoom>(`${basePath}/${id}`, { token })
}

export function createFunctionalRoom(
  payload: CreateFunctionalRoomPayload,
  token?: string,
): Promise<FunctionalRoom> {
  return apiClient.post<FunctionalRoom>(basePath, payload, { token })
}

export function updateFunctionalRoom(
  id: number,
  payload: UpdateFunctionalRoomPayload,
  token?: string,
): Promise<FunctionalRoom> {
  return apiClient.put<FunctionalRoom>(`${basePath}/${id}`, payload, { token })
}

export function deleteFunctionalRoom(
  id: number,
  expectedVersion: number,
  token?: string,
): Promise<void> {
  return apiClient.delete<void>(`${basePath}/${id}`, {
    token,
    query: { expectedVersion },
  })
}

export function lookupFunctionalRooms(
  params?: { subjectId?: number; status?: RoomStatus },
  token?: string,
): Promise<FunctionalRoom[]> {
  const query: Record<string, string | number | undefined> = {}
  if (params?.subjectId !== undefined) query.subjectId = params.subjectId
  if (params?.status) query.status = params.status

  return apiClient.get<FunctionalRoom[]>(`${basePath}/lookup`, { token, query })
}

