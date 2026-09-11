import { apiClient } from '@/services/apiClient'
import type {
  CreateUnavailabilityPayload,
  RejectUnavailabilityPayload,
  TeacherUnavailability,
  TeacherUnavailabilityStatus,
  UpdateUnavailabilityPayload,
} from '@/types/teacherUnavailability'

const basePath = '/api/v2/teacher-unavailabilities'

export interface ListUnavailabilitiesParams {
  semesterId?: number
  teacherId?: number
  status?: TeacherUnavailabilityStatus
  from?: string
  to?: string
}

export function listTeacherUnavailabilities(
  params?: ListUnavailabilitiesParams,
  token?: string,
): Promise<TeacherUnavailability[]> {
  const query: Record<string, string | number | undefined> = {}
  if (params?.semesterId !== undefined) query.semesterId = params.semesterId
  if (params?.teacherId !== undefined) query.teacherId = params.teacherId
  if (params?.status) query.status = params.status
  if (params?.from) query.from = params.from
  if (params?.to) query.to = params.to

  return apiClient.get<TeacherUnavailability[]>(basePath, { token, query })
}

export function createTeacherUnavailability(
  payload: CreateUnavailabilityPayload,
  token?: string,
): Promise<TeacherUnavailability> {
  return apiClient.post<TeacherUnavailability>(basePath, payload, { token })
}

export function updateTeacherUnavailability(
  id: number,
  payload: UpdateUnavailabilityPayload,
  token?: string,
): Promise<TeacherUnavailability> {
  return apiClient.put<TeacherUnavailability>(`${basePath}/${id}`, payload, { token })
}

export function approveTeacherUnavailability(
  id: number,
  expectedVersion: number,
  token?: string,
): Promise<TeacherUnavailability> {
  return apiClient.put<TeacherUnavailability>(
    `${basePath}/${id}/approve`,
    { expectedVersion },
    { token, query: { expectedVersion } },
  )
}

export function rejectTeacherUnavailability(
  id: number,
  payload: RejectUnavailabilityPayload,
  token?: string,
): Promise<TeacherUnavailability> {
  return apiClient.put<TeacherUnavailability>(`${basePath}/${id}/reject`, payload, { token })
}

export function withdrawTeacherUnavailability(
  id: number,
  expectedVersion: number,
  token?: string,
): Promise<TeacherUnavailability> {
  return apiClient.put<TeacherUnavailability>(
    `${basePath}/${id}/withdraw`,
    { expectedVersion },
    { token, query: { expectedVersion } },
  )
}

