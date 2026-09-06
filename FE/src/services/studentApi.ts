import { apiClient } from '@/services/apiClient'
import type {
  Student,
  StudentAcademicStatus,
  StudentPage,
  StudentQuery,
  StudentV2Payload,
  StudentV3CreateRequest,
  StudentV3CreateResponse,
} from '@/types/student'

function toDateValue(value: Date | null): string | undefined {
  if (!value) return undefined
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function fetchStudents(token: string, query: StudentQuery): Promise<StudentPage> {
  const params = new URLSearchParams({
    page: String(query.page),
    size: String(query.pageSize),
    sortField: query.sortField,
    sortDirection: query.sortOrder === 1 ? 'asc' : 'desc',
  })
  if (query.search.studentCode.trim()) params.set('studentCode', query.search.studentCode.trim())
  if (query.search.studentName.trim()) params.set('studentName', query.search.studentName.trim())
  const birthday = toDateValue(query.search.dateOfBirth)
  if (birthday) params.set('birthday', birthday)
  if (query.search.status) params.set('status', query.search.status)
  if (query.search.classId) params.set('classId', String(query.search.classId))
  return apiClient.get<StudentPage>('/api/v2/students', { token, query: params })
}

export function getStudent(token: string, studentId: number): Promise<Student> {
  return apiClient.get<Student>(`/api/v2/students/${studentId}`, { token })
}

export function generateStudentCode(token: string): Promise<{ studentCode: string }> {
  return apiClient.post<{ studentCode: string }>('/api/v2/students/code', {}, { token })
}

export function createStudent(token: string, payload: StudentV2Payload): Promise<Student> {
  return apiClient.post<Student>('/api/v2/students', payload, { token })
}

export function createStudentV3(token: string, payload: StudentV3CreateRequest): Promise<StudentV3CreateResponse> {
  return apiClient.post<StudentV3CreateResponse>('/api/v3/students', payload, { token })
}

export function updateStudent(
  token: string,
  studentId: number,
  payload: StudentV2Payload,
): Promise<Student> {
  return apiClient.put<Student>(`/api/v2/students/${studentId}`, payload, { token })
}

export function deleteStudent(token: string, studentId: number): Promise<void> {
  return apiClient.delete<void>(`/api/v2/students/${studentId}`, { token })
}

export function transitionStudentStatus(
  token: string,
  studentId: number,
  status: Exclude<StudentAcademicStatus, 'ACTIVE'>,
): Promise<Student> {
  return apiClient.patch<Student>(`/api/v2/students/${studentId}/status`, { status }, { token })
}
