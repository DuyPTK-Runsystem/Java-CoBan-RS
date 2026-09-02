import { apiClient } from '@/services/apiClient'
import type {
  AssessmentColumn,
  BulkUpsertStudentScoreRequest,
  CreateAssessmentColumnRequest,
  CreateScorebookRequest,
  Scorebook,
  StudentScore,
  StudentScoreGrid,
  UpdateAssessmentColumnRequest,
  UpsertSkillWeightRequest,
  UpsertStudentScoreRequest,
} from '@/types/scorebook'

const scorebooksPath = '/api/v2/scorebooks'
const assessmentColumnsPath = '/api/v2/assessment-columns'

export function createScorebook(token: string, request: CreateScorebookRequest): Promise<Scorebook> {
  return apiClient.post<Scorebook>(scorebooksPath, request, { token })
}

export function fetchScorebook(token: string, scorebookId: number): Promise<Scorebook> {
  return apiClient.get<Scorebook>(`${scorebooksPath}/${scorebookId}`, { token })
}

export function fetchScorebookByClassSubject(token: string, classSubjectId: number): Promise<Scorebook> {
  return apiClient.get<Scorebook>(`${scorebooksPath}/by-class-subject/${classSubjectId}`, { token })
}

export function openScorebook(token: string, scorebookId: number): Promise<Scorebook> {
  return apiClient.post<Scorebook>(`${scorebooksPath}/${scorebookId}/open`, undefined, { token })
}

export function publishScorebook(token: string, scorebookId: number): Promise<Scorebook> {
  return apiClient.post<Scorebook>(`${scorebooksPath}/${scorebookId}/publish`, undefined, { token })
}

export function createAssessmentColumn(token: string, scorebookId: number, request: CreateAssessmentColumnRequest): Promise<AssessmentColumn> {
  return apiClient.post<AssessmentColumn>(`${scorebooksPath}/${scorebookId}/columns`, request, { token })
}

export function updateAssessmentColumn(token: string, columnId: number, request: UpdateAssessmentColumnRequest): Promise<AssessmentColumn> {
  return apiClient.put<AssessmentColumn>(`${assessmentColumnsPath}/${columnId}`, request, { token })
}

export function deactivateAssessmentColumn(token: string, columnId: number): Promise<void> {
  return apiClient.delete<void>(`${assessmentColumnsPath}/${columnId}`, { token })
}

export function upsertSkillWeight(token: string, scorebookId: number, request: UpsertSkillWeightRequest): Promise<Scorebook> {
  return apiClient.put<Scorebook>(`${scorebooksPath}/${scorebookId}/skill-weight`, request, { token })
}

export function fetchScoreGrid(token: string, scorebookId: number, page = 0, size = 10): Promise<StudentScoreGrid> {
  return apiClient.get<StudentScoreGrid>(`${scorebooksPath}/${scorebookId}/score-entries`, {
    token,
    query: new URLSearchParams({ page: String(page), size: String(size) }),
  })
}

export function upsertStudentScore(token: string, columnId: number, studentId: number, request: UpsertStudentScoreRequest): Promise<StudentScore> {
  return apiClient.put<StudentScore>(`${assessmentColumnsPath}/${columnId}/students/${studentId}/score`, request, { token })
}

export function upsertStudentScoreByCode(token: string, columnId: number, studentCode: string, request: UpsertStudentScoreRequest): Promise<StudentScore> {
  return apiClient.put<StudentScore>(`${assessmentColumnsPath}/${columnId}/students/by-code/${encodeURIComponent(studentCode)}/score`, request, { token })
}

export function bulkUpsertStudentScores(token: string, columnId: number, request: BulkUpsertStudentScoreRequest): Promise<StudentScore[]> {
  return apiClient.post<StudentScore[]>(`${assessmentColumnsPath}/${columnId}/scores/bulk`, request, { token })
}
