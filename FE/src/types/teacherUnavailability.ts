export type SessionType = 'MORNING' | 'AFTERNOON'

export type TeacherUnavailabilityStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN'

export interface TeacherUnavailability {
  id: number
  teacherId: number
  teacherName: string
  semesterId: number
  dayOfWeek?: number | null
  specificDate?: string | null
  validFrom: string
  validTo: string
  session: SessionType
  periodIndexes: string
  note?: string | null
  status: TeacherUnavailabilityStatus
  decisionReason?: string | null
  decidedBy?: number | null
  decidedAt?: string | null
  version: number
  createdAt: string
  updatedAt: string
}

export interface CreateUnavailabilityPayload {
  semesterId: number
  teacherId?: number | null
  dayOfWeek?: number | null
  specificDate?: string | null
  validFrom: string
  validTo: string
  session: SessionType
  periodIndexes: string
  note?: string | null
}

export interface UpdateUnavailabilityPayload {
  expectedVersion: number
  dayOfWeek?: number | null
  specificDate?: string | null
  validFrom: string
  validTo: string
  session: SessionType
  periodIndexes: string
  note?: string | null
}

export interface RejectUnavailabilityPayload {
  expectedVersion: number
  reason: string
}

