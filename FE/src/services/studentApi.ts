import { apiBaseUrl } from '@/services/apiConfig'
import { ApiError } from '@/services/userApi'
import type { RestResponse } from '@/types/user'
import type { Student, StudentPage, StudentPayload, StudentQuery } from '@/types/student'

interface ApiErrorBody { message?: string | string[] }

function toUrl(path: string): string {
  return `${apiBaseUrl.replace(/\/$/, '')}${path}`
}

function toDateValue(value: Date | null): string | undefined {
  if (!value) return undefined
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function errorMessage(body: ApiErrorBody | null, fallback: string): string {
  return Array.isArray(body?.message) ? body.message.join(' ') : body?.message ?? fallback
}

async function request<T>(path: string, token: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(toUrl(path), {
    ...init,
    headers: { Accept: 'application/json', Authorization: `Bearer ${token}`, ...init.headers },
  })
  if (response.status === 204) return undefined as T
  const body: RestResponse<T> | ApiErrorBody = await response.json()
  if (!response.ok) throw new ApiError(response.status, errorMessage(body as ApiErrorBody, response.statusText))
  return (body as RestResponse<T>).data
}

function jsonRequest(method: 'POST' | 'PUT', body: object): RequestInit {
  return { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }
}

export function fetchStudents(token: string, query: StudentQuery): Promise<StudentPage> {
  const params = new URLSearchParams({ page: String(query.page), size: String(query.pageSize), sortField: query.sortField, sortDirection: query.sortOrder === 1 ? 'asc' : 'desc' })
  if (query.search.studentCode.trim()) params.set('studentCode', query.search.studentCode.trim())
  if (query.search.studentName.trim()) params.set('studentName', query.search.studentName.trim())
  const birthday = toDateValue(query.search.dateOfBirth)
  if (birthday) params.set('birthday', birthday)
  return request<StudentPage>(`/api/v1/students?${params.toString()}`, token)
}

export async function downloadStudentsCsv(token: string): Promise<Blob> {
  const response = await fetch(toUrl('/api/v1/students/export'), {
    headers: { Accept: 'text/csv', Authorization: `Bearer ${token}` },
  })
  if (!response.ok) {
    let body: ApiErrorBody | null = null
    try {
      body = await response.json() as ApiErrorBody
    } catch {
      body = null
    }
    throw new ApiError(response.status, errorMessage(body, response.statusText))
  }
  return response.blob()
}

export function getStudent(token: string, studentId: number): Promise<Student> { return request<Student>(`/api/v1/students/${studentId}`, token) }
export function generateStudentCode(token: string): Promise<{ studentCode: string }> { return request<{ studentCode: string }>('/api/v1/students/code', token, jsonRequest('POST', {})) }
export function createStudent(token: string, payload: StudentPayload): Promise<Student> { return request<Student>('/api/v1/students', token, jsonRequest('POST', payload)) }
export function updateStudent(token: string, studentId: number, payload: Omit<StudentPayload, 'studentCode'>): Promise<Student> { return request<Student>(`/api/v1/students/${studentId}`, token, jsonRequest('PUT', payload)) }
export function deleteStudent(token: string, studentId: number): Promise<void> { return request<void>(`/api/v1/students/${studentId}`, token, { method: 'DELETE' }) }
