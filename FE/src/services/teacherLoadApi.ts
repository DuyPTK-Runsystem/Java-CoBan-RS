import { apiClient } from '@/services/apiClient'
import type {
  TeacherLoad,
  TeacherLoadEligibility,
  TeacherLoadPolicy,
} from '@/types/timetable'

export interface GetTeacherLoadsParams {
  revisionId: number
  weekStart?: string
  weekEnd?: string
  teacherId?: number
}

export function getTeacherLoads(
  params: GetTeacherLoadsParams,
  token?: string,
): Promise<TeacherLoad[]> {
  const query: Record<string, string | number | undefined> = {
    revisionId: params.revisionId,
  }
  if (params.weekStart) query.weekStart = params.weekStart
  if (params.weekEnd) query.weekEnd = params.weekEnd
  if (params.teacherId !== undefined) query.teacherId = params.teacherId

  return apiClient.get<TeacherLoad[]>('/api/v2/teacher-loads', { token, query })
}

export function getActiveTeacherLoadPolicy(token?: string): Promise<TeacherLoadPolicy> {
  return apiClient.get<TeacherLoadPolicy>('/api/v2/teacher-load-policies/active', { token })
}

export function listTeacherLoadPolicies(token?: string): Promise<TeacherLoadPolicy[]> {
  return apiClient.get<TeacherLoadPolicy[]>('/api/v2/teacher-load-policies', { token })
}

export function createTeacherLoadPolicy(
  payload: Partial<TeacherLoadPolicy>,
  token?: string,
): Promise<TeacherLoadPolicy> {
  return apiClient.post<TeacherLoadPolicy>('/api/v2/teacher-load-policies', payload, { token })
}

export function activateTeacherLoadPolicy(
  id: number,
  token?: string,
): Promise<TeacherLoadPolicy> {
  return apiClient.put<TeacherLoadPolicy>(
    `/api/v2/teacher-load-policies/${id}/activate`,
    {},
    { token },
  )
}

export function listTeacherLoadEligibilities(
  teacherId?: number,
  token?: string,
): Promise<TeacherLoadEligibility[]> {
  const query: Record<string, string | number | undefined> = {}
  if (teacherId !== undefined) query.teacherId = teacherId

  return apiClient.get<TeacherLoadEligibility[]>('/api/v2/teacher-load-eligibilities', {
    token,
    query,
  })
}

export function createTeacherLoadEligibility(
  payload: Partial<TeacherLoadEligibility>,
  token?: string,
): Promise<TeacherLoadEligibility> {
  return apiClient.post<TeacherLoadEligibility>(
    '/api/v2/teacher-load-eligibilities',
    payload,
    { token },
  )
}

export function deleteTeacherLoadEligibility(
  id: number,
  token?: string,
): Promise<void> {
  return apiClient.delete<void>(`/api/v2/teacher-load-eligibilities/${id}`, { token })
}

