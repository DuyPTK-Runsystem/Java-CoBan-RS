import { apiClient } from '@/services/apiClient'
import type {
  CreateRevisionPayload,
  CreateTimetablePayload,
  PaginatedResult,
  PublishTimetablePayload,
  SessionType,
  TimetableDetail,
  TimetableEntry,
  TimetableIssue,
  TimetablePeriod,
  TimetableReview,
  TimetableSummary,
  UpdateTimetableEntriesPayload,
} from '@/types/timetable'

const basePath = '/api/v3/timetables'

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

export function normalizeCapabilities(caps: unknown): TimetableCapabilities {
  if (Array.isArray(caps)) {
    const set = new Set(caps)
    return {
      canEdit: set.has('EDIT_ENTRIES') || set.has('CAN_EDIT'),
      canValidate: set.has('VALIDATE') || set.has('CAN_VALIDATE'),
      canPublish: set.has('PUBLISH') || set.has('CAN_PUBLISH'),
      canRevise: set.has('CREATE_REVISION') || set.has('REVISE') || set.has('CAN_REVISE'),
    }
  }
  if (typeof caps === 'object' && caps !== null) {
    const c = caps as Record<string, boolean>
    return {
      canEdit: Boolean(c.canEdit),
      canValidate: Boolean(c.canValidate),
      canPublish: Boolean(c.canPublish),
      canRevise: Boolean(c.canRevise),
    }
  }
  return {
    canEdit: false,
    canValidate: false,
    canPublish: false,
    canRevise: false,
  }
}

export function normalizeEntry(entry: TimetableEntry): TimetableEntry {
  const id = entry.id ?? entry.entryId ?? 0
  return {
    ...entry,
    id,
    entryId: id,
  }
}

export function normalizePeriod(period: TimetablePeriod & { periodId?: number; name?: string }): TimetablePeriod {
  return {
    ...period,
    id: period.id ?? period.periodId ?? 0,
    periodName: period.periodName ?? period.name ?? `Tiết ${period.periodIndex}`,
  }
}

export function normalizeIssue(issue: TimetableIssue): TimetableIssue {
  const ids = Array.isArray(issue.entryIds) ? issue.entryIds.filter((id): id is number => typeof id === 'number') : []
  return { ...issue, entryIds: ids, entryId: issue.entryId ?? ids[0] ?? null }
}

export function normalizeDetail(detail: TimetableDetail): TimetableDetail {
  return {
    ...detail,
    capabilities: normalizeCapabilities(detail.capabilities),
  }
}

export async function createTimetable(
  payload: CreateTimetablePayload,
  token?: string,
): Promise<TimetableDetail> {
  const detail = await apiClient.post<TimetableDetail>(basePath, payload, { token })
  return normalizeDetail(detail)
}

export async function getTimetableDetail(
  id: number,
  token?: string,
): Promise<TimetableDetail> {
  const detail = await apiClient.get<TimetableDetail>(`${basePath}/${id}`, { token })
  return normalizeDetail(detail)
}

export async function getTimetableEntries(
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

  const entries = await apiClient.get<TimetableEntry[]>(`${basePath}/${revisionId}/entries`, {
    token,
    query,
  })
  return Array.isArray(entries) ? entries.map(normalizeEntry) : []
}

export async function updateTimetableEntries(
  revisionId: number,
  payload: UpdateTimetableEntriesPayload,
  token?: string,
): Promise<TimetableDetail> {
  const detail = await apiClient.put<TimetableDetail>(
    `${basePath}/${revisionId}/entries`,
    payload,
    { token },
  )
  return normalizeDetail(detail)
}

export function validateTimetableRevision(
  revisionId: number,
  token?: string,
): Promise<TimetableReview> {
  return apiClient.post<TimetableReview>(
    `${basePath}/${revisionId}/validate`,
    {},
    { token },
  )
}

export function getTimetableReview(
  revisionId: number,
  token?: string,
): Promise<TimetableReview> {
  return apiClient.get<TimetableReview>(`${basePath}/${revisionId}/review`, { token }).then((review) => ({
    ...review,
    issues: Array.isArray(review.issues) ? review.issues.map(normalizeIssue) : [],
  }))
}

export async function publishTimetableRevision(
  revisionId: number,
  payload: PublishTimetablePayload,
  idempotencyKey?: string,
  token?: string,
): Promise<TimetableDetail> {
  const headers: Record<string, string> = {}
  if (idempotencyKey) {
    headers['Idempotency-Key'] = idempotencyKey
  }
  const detail = await apiClient.post<TimetableDetail>(
    `${basePath}/${revisionId}/publish`,
    payload,
    { token, headers },
  )
  return normalizeDetail(detail)
}

export async function createTimetableRevision(
  revisionId: number,
  payload: CreateRevisionPayload,
  token?: string,
): Promise<TimetableDetail> {
  const detail = await apiClient.post<TimetableDetail>(
    `${basePath}/${revisionId}/revisions`,
    payload,
    { token },
  )
  return normalizeDetail(detail)
}

export async function getTimetablePeriods(
  semesterId?: number,
  token?: string,
): Promise<TimetablePeriod[]> {
  if (!semesterId) return []
  const res = await apiClient.get<{ periods?: TimetablePeriod[] }>('/api/v3/timetable-calendars', {
    token,
    query: { semesterId },
  })
  return Array.isArray(res?.periods) ? res.periods.map(normalizePeriod) : []
}

export async function getMyTimetable(token?: string): Promise<TimetableEntry[]> {
  const entries = await apiClient.get<TimetableEntry[]>('/api/v3/timetables/my-timetable', { token })
  return Array.isArray(entries) ? entries.map(normalizeEntry) : []
}
