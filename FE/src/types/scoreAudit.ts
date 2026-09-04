export interface ResScoreAuditLogDTO {
  auditLogId: number
  actorUserId: number | null
  actorUsername: string | null
  action: string
  entityType: string
  entityId: string
  beforeData: unknown
  afterData: unknown
  requestId: string | null
  ipAddress: string | null
  occurredAt: string
}

export interface ReqFilterScoreAuditLogDTO {
  entityType?: string
  entityId?: string
  studentId?: number
  studentCode?: string
  action?: string
  actorUserId?: number
  fromOccurredAt?: string
  toOccurredAt?: string
  page: number
  size: number
}

export interface ScoreAuditLogPage {
  content: ResScoreAuditLogDTO[]
  page?: number
  number?: number
  size: number
  totalElements: number
  totalPages: number
}
