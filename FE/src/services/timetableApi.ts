import { apiClient } from '@/services/apiClient'
import type {
  CreateRevisionPayload,
  CreateTimetablePayload,
  PaginatedResult,
  PublishTimetablePayload,
  SessionType,
  TimetableDetail,
  TimetableEntry,
  TimetablePeriod,
  TimetableReview,
  TimetableSummary,
  UpdateTimetableEntriesPayload,
} from '@/types/timetable'

const basePath = '/api/v2/timetables'

export interface GetEntriesParams {
  weekStart?: string
  weekEnd?: string
  classId?: number
  teacherId?: number
  functionalRoomId?: number
  session?: SessionType
  periodIndex?: number
}

export function listTimetables(
  semesterId: number,
  page?: number,
  size?: number,
  token?: string,
): Promise<PaginatedResult<TimetableSummary>> {
  const query: Record<string, string | number | undefined> = { semesterId }
  if (page !== undefined) query.page = page
  if (size !== undefined) query.size = size

  return apiClient.get<PaginatedResult<TimetableSummary>>(basePath, { token, query })
}

export function createTimetable(
  payload: CreateTimetablePayload,
  token?: string,
): Promise<TimetableDetail> {
  return apiClient.post<TimetableDetail>(basePath, payload, { token })
}

export function getTimetableDetail(
  id: number,
  token?: string,
): Promise<TimetableDetail> {
  return apiClient.get<TimetableDetail>(`${basePath}/${id}`, { token })
}

export function getTimetableEntries(
  revisionId: number,
  params?: GetEntriesParams,
  token?: string,
): Promise<TimetableEntry[]> {
  const query: Record<string, string | number | undefined> = {}
  if (params?.weekStart) query.weekStart = params.weekStart
  if (params?.weekEnd) query.weekEnd = params.weekEnd
  if (params?.classId !== undefined) query.classId = params.classId
  if (params?.teacherId !== undefined) query.teacherId = params.teacherId
  if (params?.functionalRoomId !== undefined) query.functionalRoomId = params.functionalRoomId
  if (params?.session) query.session = params.session
  if (params?.periodIndex !== undefined) query.periodIndex = params.periodIndex

  return apiClient.get<TimetableEntry[]>(`${basePath}/revisions/${revisionId}/entries`, {
    token,
    query,
  })
}

export function updateTimetableEntries(
  revisionId: number,
  payload: UpdateTimetableEntriesPayload,
  token?: string,
): Promise<TimetableDetail> {
  return apiClient.put<TimetableDetail>(
    `${basePath}/revisions/${revisionId}/entries`,
    payload,
    { token },
  )
}

export function validateTimetableRevision(
  revisionId: number,
  token?: string,
): Promise<TimetableReview> {
  return apiClient.post<TimetableReview>(
    `${basePath}/revisions/${revisionId}/validate`,
    {},
    { token },
  )
}

export function getTimetableReview(
  revisionId: number,
  token?: string,
): Promise<TimetableReview> {
  return apiClient.get<TimetableReview>(
    `${basePath}/revisions/${revisionId}/review`,
    { token },
  )
}

export function publishTimetableRevision(
  revisionId: number,
  payload: PublishTimetablePayload,
  idempotencyKey?: string,
  token?: string,
): Promise<TimetableDetail> {
  const headers: Record<string, string> = {}
  if (idempotencyKey) {
    headers['Idempotency-Key'] = idempotencyKey
  }
  return apiClient.post<TimetableDetail>(
    `${basePath}/revisions/${revisionId}/publish`,
    payload,
    { token, headers },
  )
}

export function createTimetableRevision(
  revisionId: number,
  payload: CreateRevisionPayload,
  token?: string,
): Promise<TimetableDetail> {
  return apiClient.post<TimetableDetail>(
    `${basePath}/revisions/${revisionId}/revisions`,
    payload,
    { token },
  )
}

export function getTimetablePeriods(
  semesterId?: number,
  token?: string,
): Promise<TimetablePeriod[]> {
  const query: Record<string, string | number | undefined> = {}
  if (semesterId !== undefined) query.semesterId = semesterId

  return apiClient.get<TimetablePeriod[]>(`${basePath}/periods`, { token, query })
}

export function initTimetableCalendar(
  semesterId: number,
  token?: string,
): Promise<TimetablePeriod[]> {
  return apiClient.post<TimetablePeriod[]>(
    `${basePath}/periods/init`,
    {},
    { token, query: { semesterId } },
  )
}

export function getMyTimetable(token?: string): Promise<TimetableEntry[]> {
  return apiClient.get<TimetableEntry[]>('/api/v2/my-timetable', { token })
}

