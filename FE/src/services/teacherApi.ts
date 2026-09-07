import { apiClient } from '@/services/apiClient'
import type { CreateTeacherRequest, Teacher, TeacherStatus, UpdateTeacherRequest } from '@/types/teacher'

const path = '/api/v2/teachers'

export function fetchTeachers(token: string, status?: TeacherStatus): Promise<Teacher[]> {
  return apiClient.get<Teacher[]>(path, { token, query: status ? { status } : undefined })
}

export function fetchTeacherById(token: string, teacherId: number): Promise<Teacher> {
  return apiClient.get<Teacher>(`${path}/${teacherId}`, { token })
}

export function createTeacher(token: string, request: CreateTeacherRequest): Promise<Teacher> {
  return apiClient.post<Teacher>(path, request, { token })
}

export function updateTeacher(token: string, teacherId: number, request: UpdateTeacherRequest): Promise<Teacher> {
  return apiClient.put<Teacher>(`${path}/${teacherId}`, request, { token })
}

export function deleteTeacher(token: string, teacherId: number): Promise<void> {
  return apiClient.delete<void>(`${path}/${teacherId}`, { token })
}
