import { apiClient } from '@/services/apiClient'
import type {
  CalculationTaskPage,
  ReqFilterCalculationTaskDTO,
  ResCalculationTaskDTO,
} from '@/types/calculationTask'

const basePath = '/api/v2/scorebooks/calculation-tasks'

function buildQueryParams(filter: ReqFilterCalculationTaskDTO): URLSearchParams {
  const query = new URLSearchParams({
    page: String(filter.page),
    size: String(filter.size),
  })
  if (filter.status) query.set('status', filter.status)
  if (filter.studentId != null) query.set('studentId', String(filter.studentId))
  if (filter.studentCode?.trim()) query.set('studentCode', filter.studentCode.trim())
  if (filter.academicYearId != null) query.set('academicYearId', String(filter.academicYearId))
  return query
}

export function fetchCalculationTasks(
  token: string,
  filter: ReqFilterCalculationTaskDTO,
): Promise<CalculationTaskPage> {
  const query = buildQueryParams(filter)
  return apiClient.get<CalculationTaskPage>(basePath, { token, query })
}

export function fetchFailedCalculationTasks(
  token: string,
  filter: ReqFilterCalculationTaskDTO,
): Promise<CalculationTaskPage> {
  const query = buildQueryParams(filter)
  return apiClient.get<CalculationTaskPage>(`${basePath}/failed`, { token, query })
}

export function retryCalculationTask(
  token: string,
  taskId: number,
): Promise<ResCalculationTaskDTO> {
  return apiClient.post<ResCalculationTaskDTO>(`${basePath}/${taskId}/retry`, undefined, { token })
}

export function retryAllFailedCalculationTasks(
  token: string,
): Promise<ResCalculationTaskDTO[]> {
  return apiClient.post<ResCalculationTaskDTO[]>(`${basePath}/retry-all-failed`, undefined, { token })
}

export function recalculateTranscriptByCode(
  token: string,
  studentCode: string,
  academicYearId: number,
): Promise<ResCalculationTaskDTO> {
  const query = new URLSearchParams({ academicYearId: String(academicYearId) })
  return apiClient.post<ResCalculationTaskDTO>(
    `/api/v2/students/${studentCode}/transcripts/recalculate`,
    undefined,
    { token, query },
  )
}

export function recalculateTranscriptById(
  token: string,
  studentId: number,
  academicYearId: number,
): Promise<ResCalculationTaskDTO> {
  const query = new URLSearchParams({ academicYearId: String(academicYearId) })
  return apiClient.post<ResCalculationTaskDTO>(
    `/api/v2/students/${studentId}/transcripts/recalculate`,
    undefined,
    { token, query },
  )
}
