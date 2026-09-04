export type CalculationTaskStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'
export type CalculationTaskType = 'STUDENT_YEAR_RECALC'

export interface ResCalculationTaskDTO {
  taskId: number
  studentId: number
  studentCode: string
  academicYearId: number
  taskType: CalculationTaskType
  requestedVersion: number
  status: CalculationTaskStatus
  attemptCount: number
  maxAttempts: number
  availableAt: string | null
  lockedAt: string | null
  workerId: string | null
  lastError: string | null
  createdAt: string
  startedAt: string | null
  completedAt: string | null
}

export interface ReqFilterCalculationTaskDTO {
  status?: CalculationTaskStatus
  studentId?: number
  studentCode?: string
  academicYearId?: number
  page: number
  size: number
}

export interface CalculationTaskPage {
  content: ResCalculationTaskDTO[]
  page?: number
  number?: number
  size: number
  totalElements: number
  totalPages: number
}
