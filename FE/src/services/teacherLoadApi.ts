import { apiClient } from '@/services/apiClient'
import type {
  CreateTeacherLoadEligibilityPayload,
  CreateTeacherLoadPolicyPayload,
  TeacherLoad,
  TeacherLoadEligibility,
  TeacherLoadPolicy,
  TeacherLoadRule,
  UpdateTeacherLoadEligibilityPayload,
} from '@/types/timetable'

interface PaginatedPolicyResult {
  result: TeacherLoadPolicy[]
}

function mapPolicy(raw: Record<string, unknown>): TeacherLoadPolicy {
  return {
    id: Number(raw.id), policyName: String(raw.version ?? ''), sourceDocument: String(raw.source ?? ''),
    effectiveFrom: String(raw.effectiveFrom ?? ''), effectiveTo: raw.effectiveTo as string | null,
    policyVersion: Number(raw.versionLock ?? 0), standardPeriodsHighSchool: Number(raw.basePeriods ?? 0),
    homeroomReduction: Number(raw.homeroomReduction ?? 0), nursingChildReduction: Number(raw.nursingReduction ?? 0),
    active: raw.status === 'ACTIVE', version: Number(raw.versionLock ?? 0),
    rules: Array.isArray(raw.rules) ? raw.rules.map((rule) => mapRule(rule as Record<string, unknown>)) : [],
  }
}

function mapRule(raw: Record<string, unknown>): TeacherLoadRule {
  return {
    id: raw.id === undefined ? undefined : Number(raw.id),
    policyId: raw.policyId === undefined ? undefined : Number(raw.policyId),
    ruleCode: String(raw.ruleCode ?? ''),
    ruleName: String(raw.ruleName ?? ''),
    triggerType: raw.triggerType === 'HOMEROOM' ? 'HOMEROOM' : 'ELIGIBILITY',
    reductionPeriods: Number(raw.reductionPeriods ?? 0),
    source: String(raw.source ?? ''),
    active: raw.active !== false,
  }
}

function mapEligibility(raw: Record<string, unknown>): TeacherLoadEligibility {
  return {
    id: Number(raw.id),
    teacherId: Number(raw.teacherId),
    teacherName: String(raw.teacherName ?? 'N/A'),
    ruleCode: String(raw.ruleCode ?? ''),
    validFrom: String(raw.validFrom ?? ''),
    validTo: String(raw.validTo ?? ''),
    evidenceReference: String(raw.evidenceReference ?? ''),
    status: String(raw.status ?? 'ACTIVE'),
    version: Number(raw.version ?? 0),
  }
}

export interface GetTeacherLoadsParams {
  revisionId: number
  weekStart?: string
  weekEnd?: string
  teacherId?: number
}

export function getTeacherLoads(
  params: GetTeacherLoadsParams,
  _token?: string,
): Promise<TeacherLoad[]> {
  const query: Record<string, string | number | undefined> = {
    revisionId: params.revisionId,
  }
  if (params.weekStart) query.weekStart = params.weekStart
  if (params.weekEnd) query.weekEnd = params.weekEnd
  if (params.teacherId !== undefined) query.teacherId = params.teacherId

  // Load evaluation is returned by the timetable review contract; there is
  // no standalone teacher-load endpoint in the current backend.
  return Promise.reject(new Error('Teacher loads are available from timetable review'))
}

export async function getActiveTeacherLoadPolicy(token?: string): Promise<TeacherLoadPolicy | null> {
  const page = await apiClient.get<PaginatedPolicyResult>('/api/v3/teacher-load-policies', { token, query: { page: 0, size: 100 } })
  return (page.result ?? []).map((item) => mapPolicy(item as unknown as Record<string, unknown>)).find((policy) => policy.active) ?? null
}

export async function listTeacherLoadPolicies(token?: string): Promise<TeacherLoadPolicy[]> {
  const page = await apiClient.get<PaginatedPolicyResult>('/api/v3/teacher-load-policies', { token, query: { page: 0, size: 100 } })
  return (page.result ?? []).map((item) => mapPolicy(item as unknown as Record<string, unknown>))
}

export function createTeacherLoadPolicy(
  payload: CreateTeacherLoadPolicyPayload,
  token?: string,
): Promise<TeacherLoadPolicy> {
  const body = {
    version: payload.policyName,
    source: payload.sourceDocument,
    effectiveFrom: payload.effectiveFrom,
    effectiveTo: payload.effectiveTo,
    basePeriods: payload.standardPeriodsHighSchool,
    homeroomReduction: payload.homeroomReduction,
    nursingReduction: payload.nursingChildReduction,
    rules: payload.rules,
  }
  return apiClient.post<TeacherLoadPolicy>('/api/v3/teacher-load-policies', body, { token }).then((item) => mapPolicy(item as unknown as Record<string, unknown>))
}

export function listTeacherLoadRules(policyId: number, token?: string): Promise<TeacherLoadRule[]> {
  return apiClient.get<TeacherLoadRule[] | { result?: TeacherLoadRule[] }>(
    `/api/v3/teacher-load-policies/${policyId}/rules`, { token })
    .then((response) => {
      const items = Array.isArray(response) ? response : response.result ?? []
      return items.map((item) => mapRule(item as unknown as Record<string, unknown>))
    })
}

export function createTeacherLoadRule(
  policyId: number,
  payload: Omit<TeacherLoadRule, 'id' | 'policyId' | 'active'>,
  token?: string,
): Promise<TeacherLoadRule> {
  return apiClient.post<TeacherLoadRule>(`/api/v3/teacher-load-policies/${policyId}/rules`, payload, { token })
    .then((item) => mapRule(item as unknown as Record<string, unknown>))
}

export function activateTeacherLoadPolicy(
  id: number,
  expectedVersion: number,
  token?: string,
): Promise<TeacherLoadPolicy> {
  return apiClient.post<TeacherLoadPolicy>(`/api/v3/teacher-load-policies/${id}/activate`, {}, {
    token,
    query: { expectedVersion },
  }).then((item) => mapPolicy(item as unknown as Record<string, unknown>))
}

export function listTeacherLoadEligibilities(
  teacherId?: number,
  token?: string,
): Promise<TeacherLoadEligibility[]> {
  const query: Record<string, string | number | undefined> = {}
  if (teacherId !== undefined) query.teacherId = teacherId

  return apiClient.get<TeacherLoadEligibility[] | { result?: TeacherLoadEligibility[] }>('/api/v3/teacher-load-eligibilities', {
    token,
    query,
  }).then((response) => {
    const items = Array.isArray(response) ? response : response.result ?? []
    return items.map((item) => mapEligibility(item as unknown as Record<string, unknown>))
  })
}

export function createTeacherLoadEligibility(
  payload: CreateTeacherLoadEligibilityPayload,
  token?: string,
): Promise<TeacherLoadEligibility> {
  return apiClient.post<TeacherLoadEligibility>(
    '/api/v3/teacher-load-eligibilities',
    payload,
    { token },
  ).then((item) => mapEligibility(item as unknown as Record<string, unknown>))
}

export function updateTeacherLoadEligibility(
  id: number,
  payload: UpdateTeacherLoadEligibilityPayload,
  token?: string,
): Promise<TeacherLoadEligibility> {
  return apiClient.put<TeacherLoadEligibility>(`/api/v3/teacher-load-eligibilities/${id}`, payload, { token })
    .then((item) => mapEligibility(item as unknown as Record<string, unknown>))
}

export function revokeTeacherLoadEligibility(
  id: number,
  expectedVersion: number,
  token?: string,
): Promise<TeacherLoadEligibility> {
  return updateTeacherLoadEligibility(id, {
    expectedVersion,
    status: 'REVOKED',
  }, token)
}
