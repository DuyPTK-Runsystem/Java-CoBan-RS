import { apiClient } from '@/services/apiClient'
import type { Student, StudentPage, StudentPayload, StudentQuery } from '@/types/student'

function toDateValue(value: Date | null): string | undefined {
  if (!value) return undefined
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function fetchStudents(token: string, query: StudentQuery): Promise<StudentPage> {
  const params = new URLSearchParams({ page: String(query.page), size: String(query.pageSize), sortField: query.sortField, sortDirection: query.sortOrder === 1 ? 'asc' : 'desc' })
  if (query.search.studentCode.trim()) params.set('studentCode', query.search.studentCode.trim())
  if (query.search.studentName.trim()) params.set('studentName', query.search.studentName.trim())
  const birthday = toDateValue(query.search.dateOfBirth)
  if (birthday) params.set('birthday', birthday)
  return apiClient.get<StudentPage>('/api/v1/students', { token, query: params })
}

export async function downloadStudentsCsv(token: string): Promise<Blob> {
  return apiClient.get<Blob>('/api/v1/students/export', { token, responseType: 'blob' })
}

export function getStudent(token: string, studentId: number): Promise<Student> { return apiClient.get<Student>(`/api/v1/students/${studentId}`, { token }) }
export function generateStudentCode(token: string): Promise<{ studentCode: string }> { return apiClient.post<{ studentCode: string }>('/api/v1/students/code', {}, { token }) }
export function createStudent(token: string, payload: StudentPayload): Promise<Student> { return apiClient.post<Student>('/api/v1/students', payload, { token }) }
export function updateStudent(token: string, studentId: number, payload: Omit<StudentPayload, 'studentCode'>): Promise<Student> { return apiClient.put<Student>(`/api/v1/students/${studentId}`, payload, { token }) }
export function deleteStudent(token: string, studentId: number): Promise<void> { return apiClient.delete<void>(`/api/v1/students/${studentId}`, { token }) }
