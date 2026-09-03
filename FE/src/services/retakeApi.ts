import { apiClient } from '@/services/apiClient'
import type {
  ReqCreateRetakeExamDTO,
  ReqFilterRetakeExamDTO,
  ReqUpdateRetakeScoreDTO,
  ResRetakeExamDTO,
  RetakeExamPage,
} from '@/types/retake'

const basePath = '/api/v2/retake-exams'

export function fetchRetakeExams(
  token: string,
  filter: ReqFilterRetakeExamDTO,
): Promise<RetakeExamPage> {
  const query = new URLSearchParams({
    page: String(filter.page),
    size: String(filter.size),
  })
  if (filter.studentId != null) query.set('studentId', String(filter.studentId))
  if (filter.academicYearId != null) query.set('academicYearId', String(filter.academicYearId))
  if (filter.subjectId != null) query.set('subjectId', String(filter.subjectId))
  if (filter.status) query.set('status', filter.status)

  return apiClient.get<RetakeExamPage>(basePath, { token, query })
}

export function fetchRetakeExam(
  token: string,
  retakeId: number,
): Promise<ResRetakeExamDTO> {
  return apiClient.get<ResRetakeExamDTO>(`${basePath}/${retakeId}`, { token })
}

export function createRetakeExam(
  token: string,
  payload: ReqCreateRetakeExamDTO,
): Promise<ResRetakeExamDTO> {
  return apiClient.post<ResRetakeExamDTO>(basePath, payload, { token })
}

export function updateRetakeScore(
  token: string,
  retakeId: number,
  payload: ReqUpdateRetakeScoreDTO,
): Promise<ResRetakeExamDTO> {
  return apiClient.put<ResRetakeExamDTO>(`${basePath}/${retakeId}/score`, payload, { token })
}

export function cancelRetakeExam(
  token: string,
  retakeId: number,
): Promise<ResRetakeExamDTO> {
  return apiClient.post<ResRetakeExamDTO>(`${basePath}/${retakeId}/cancel`, undefined, { token })
}
