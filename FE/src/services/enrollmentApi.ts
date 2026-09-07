import { apiClient } from '@/services/apiClient'
import type {
  BulkCreateEnrollmentRequest,
  ClassStudent,
  CreateEnrollmentRequest,
  EnrollmentMutation,
  StudentEnrollmentHistory,
  TransferEnrollmentRequest,
  TransferScoreAssistSnapshot,
  TransferWithScoresResponse,
  TransferWithScoresRequest,
  UnassignedStudent,
} from '@/types/enrollment'

const enrollmentPath = '/api/v2/enrollments'

export function fetchUnassignedStudents(token: string, academicYearId: number): Promise<UnassignedStudent[]> {
  return apiClient.get<UnassignedStudent[]>(`${enrollmentPath}/unassigned`, {
    token,
    query: new URLSearchParams({ academicYearId: String(academicYearId) }),
  })
}

export function fetchClassStudents(token: string, classId: number): Promise<ClassStudent[]> {
  return apiClient.get<ClassStudent[]>(`/api/v2/classes/${classId}/students`, { token })
}

export function createEnrollment(token: string, request: CreateEnrollmentRequest): Promise<EnrollmentMutation> {
  return apiClient.post<EnrollmentMutation>(enrollmentPath, request, { token })
}

export function createBulkEnrollment(token: string, request: BulkCreateEnrollmentRequest): Promise<EnrollmentMutation> {
  return apiClient.post<EnrollmentMutation>(`${enrollmentPath}/bulk`, request, { token })
}

export function transferEnrollment(token: string, enrollmentId: number, request: TransferEnrollmentRequest): Promise<EnrollmentMutation> {
  return apiClient.post<EnrollmentMutation>(`${enrollmentPath}/${enrollmentId}/transfer`, request, { token })
}

export function fetchTransferScoreAssist(token: string, enrollmentId: number, targetClassId: number, semesterId: number): Promise<TransferScoreAssistSnapshot> {
  return apiClient.get<TransferScoreAssistSnapshot>(`${enrollmentPath}/${enrollmentId}/transfer-score-assist`, {
    token,
    query: new URLSearchParams({ targetClassId: String(targetClassId), semesterId: String(semesterId) }),
  })
}

export function transferWithScores(token: string, enrollmentId: number, request: TransferWithScoresRequest): Promise<TransferWithScoresResponse> {
  return apiClient.post<TransferWithScoresResponse>(`${enrollmentPath}/${enrollmentId}/transfer-with-scores`, request, { token })
}

export function fetchStudentEnrollmentHistory(token: string, studentId: number): Promise<StudentEnrollmentHistory[]> {
  return apiClient.get<StudentEnrollmentHistory[]>(`/api/v2/students/${studentId}/enrollments`, { token })
}

export function fetchStudentEnrollmentHistoryByCode(token: string, studentCode: string): Promise<StudentEnrollmentHistory[]> {
  return apiClient.get<StudentEnrollmentHistory[]>(`/api/v2/students/by-code/${encodeURIComponent(studentCode)}/enrollments`, { token })
}
