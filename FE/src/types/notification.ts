export type NotificationAudienceType = 'INDIVIDUAL' | 'CLASS' | 'SCHOOL'

export type NotificationStatus = 'DRAFT' | 'SCHEDULED' | 'PUBLISHED' | 'CANCELLED' | 'EXPIRED'

export type NotificationChannel = 'IN_APP' | 'EMAIL'

/** The deployed application has one school scope; keep this as a plain constant. */
export const DEFAULT_SCHOOL = 'DEFAULT_SCHOOL' as const

/** Manager-only, display-safe audience metadata returned by the v3 detail contract. */
export interface NotificationAudienceDetails {
  audienceType?: NotificationAudienceType | null
  displayLabel?: string | null
  recipientCount?: number | null
  recipientDisplayNames?: string[] | null
  displayDataAvailable?: boolean
}

export interface NotificationItem {
  id: number
  title: string
  body: string
  channel: NotificationChannel
  status: NotificationStatus
  audienceType: NotificationAudienceType
  audienceDetails?: NotificationAudienceDetails | null
  targetReference?: string | null
  schoolScope?: string | null
  /** Backend omits senderId for recipient-facing responses. */
  senderId?: number | null
  publishAt?: string | null
  expiresAt?: string | null
  idempotencyKey?: string | null
  version: number
  createdAt: string
  updatedAt: string
  read?: boolean | null
  readAt?: string | null
}

export type NotificationDeliveryStatus = 'PENDING' | 'SENT' | 'FAILED'

export interface NotificationReceipt {
  id: number
  notificationId: number
  /** Backend omits the recipient identity from recipient-facing receipt responses. */
  recipientUserId?: number | null
  accessScope: string
  readAt?: string | null
  deliveryStatus?: NotificationDeliveryStatus | null
  deliveryError?: string | null
  deliveredAt?: string | null
  createdAt: string
}

export interface ReqCreateNotificationDTO {
  title: string
  body: string
  audienceType: NotificationAudienceType
  targetReference?: string
  recipientUserIds?: number[]
  /** The single-school scope sent by the composer; the backend remains authoritative. */
  schoolScope: string
  /** Null/omitted keeps the backward-compatible IN_APP default. */
  channel?: NotificationChannel
  expiresAt?: string | null
  /** Same full payload + key is idempotent; a reused key with a different payload is a conflict. */
  idempotencyKey?: string
}

export interface NotificationClassAudience {
  classId: number
  classCode: string
  className: string
  academicYearId: number
  eligibleRecipientCount: number
}

export interface NotificationIndividualAudience {
  userId: number
  displayName: string
  studentCode?: string | null
  username?: string | null
  roleCodes?: NotificationRoleCode[]
  studentClass?: NotificationAudienceClassContext | null
  teacherClasses?: NotificationAudienceClassContext[]
}

export type NotificationRoleCode = 'STUDENT' | 'TEACHER' | 'ACADEMIC_OFFICE' | 'ADMIN'

export interface NotificationAudienceClassContext {
  classId: number
  classCode: string
  className: string
}

export interface ReqPublishNotificationDTO {
  expectedVersion?: number
}

export interface ResultPaginationMeta {
  page: number
  pageSize: number
  totalPages: number
  totalItems: number
}

export interface ResultPaginationDTO<T> {
  meta: ResultPaginationMeta
  result: T[]
}

export type NotificationPage = ResultPaginationDTO<NotificationItem>

export interface NotificationInboxQuery {
  unreadOnly?: boolean
  page?: number
  pageSize?: number
}

export interface NotificationManageQuery {
  page?: number
  pageSize?: number
}

export interface NotificationAudienceLookupQuery {
  q?: string
  roleCode?: NotificationRoleCode
  studentClassId?: number
  teacherClassId?: number
  page?: number
  pageSize?: number
}
