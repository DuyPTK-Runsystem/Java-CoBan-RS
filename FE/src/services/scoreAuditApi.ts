import { apiClient } from '@/services/apiClient'
import type { ReqFilterScoreAuditLogDTO, ScoreAuditLogPage } from '@/types/scoreAudit'

const basePath = '/api/v2/scorebooks/audit-logs'

function buildQueryParams(filter: ReqFilterScoreAuditLogDTO): URLSearchParams {
  const query = new URLSearchParams({
    page: String(filter.page),
    size: String(filter.size),
  })
  if (filter.entityType?.trim()) query.set('entityType', filter.entityType.trim())
  if (filter.entityId?.trim()) query.set('entityId', filter.entityId.trim())
  if (filter.studentId != null) query.set('studentId', String(filter.studentId))
  if (filter.studentCode?.trim()) query.set('studentCode', filter.studentCode.trim())
  if (filter.action?.trim()) query.set('action', filter.action.trim())
  if (filter.actorUserId != null) query.set('actorUserId', String(filter.actorUserId))
  if (filter.fromOccurredAt?.trim()) query.set('fromOccurredAt', filter.fromOccurredAt.trim())
  if (filter.toOccurredAt?.trim()) query.set('toOccurredAt', filter.toOccurredAt.trim())
  return query
}

export function fetchScoreAuditLogs(
  token: string,
  filter: ReqFilterScoreAuditLogDTO,
): Promise<ScoreAuditLogPage> {
  const query = buildQueryParams(filter)
  return apiClient.get<ScoreAuditLogPage>(basePath, { token, query })
}
