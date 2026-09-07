import { apiClient } from '@/services/apiClient'
import type {
  CreateHomeroomAssignmentRequest,
  CreateSubjectTeachingAssignmentRequest,
  EndHomeroomAssignmentRequest,
  EndSubjectTeachingAssignmentRequest,
  HomeroomAssignment,
  ReplaceHomeroomAssignmentRequest,
  ReplaceSubjectTeachingAssignmentRequest,
  SubjectTeachingAssignment,
} from '@/types/assignment'

export function fetchHomeroomAssignmentsByClass(token: string, classId: number): Promise<HomeroomAssignment[]> {
  return apiClient.get<HomeroomAssignment[]>(`/api/v2/assignments/classes/${classId}`, { token })
}

export function fetchHomeroomAssignmentsByTeacher(token: string, teacherId: number): Promise<HomeroomAssignment[]> {
  return apiClient.get<HomeroomAssignment[]>(`/api/v2/assignments/teachers/${teacherId}/homeroom`, { token })
}

export function fetchSubjectAssignmentsByTeacher(token: string, teacherId: number): Promise<SubjectTeachingAssignment[]> {
  return apiClient.get<SubjectTeachingAssignment[]>(`/api/v2/assignments/teachers/${teacherId}`, { token })
}

export function fetchSubjectAssignmentsByClass(token: string, classId: number, semesterId: number): Promise<SubjectTeachingAssignment[]> {
  return apiClient.get<SubjectTeachingAssignment[]>(`/api/v2/assignments/classes/${classId}/subjects?semesterId=${semesterId}`, { token })
}

export function createHomeroomAssignment(token: string, classId: number, request: CreateHomeroomAssignmentRequest): Promise<HomeroomAssignment> {
  return apiClient.post<HomeroomAssignment>(`/api/v2/classes/${classId}/homeroom-assignments`, request, { token })
}

export function replaceHomeroomAssignment(token: string, assignmentId: number, request: ReplaceHomeroomAssignmentRequest): Promise<HomeroomAssignment> {
  return apiClient.post<HomeroomAssignment>(`/api/v2/homeroom-assignments/${assignmentId}/replace`, request, { token })
}

export function endHomeroomAssignment(token: string, assignmentId: number, request: EndHomeroomAssignmentRequest): Promise<HomeroomAssignment> {
  return apiClient.post<HomeroomAssignment>(`/api/v2/homeroom-assignments/${assignmentId}/end`, request, { token })
}

export function createSubjectTeachingAssignment(token: string, classSubjectId: number, request: CreateSubjectTeachingAssignmentRequest): Promise<SubjectTeachingAssignment> {
  return apiClient.post<SubjectTeachingAssignment>(`/api/v2/class-subjects/${classSubjectId}/teaching-assignments`, request, { token })
}

export function replaceSubjectTeachingAssignment(token: string, assignmentId: number, request: ReplaceSubjectTeachingAssignmentRequest): Promise<SubjectTeachingAssignment> {
  return apiClient.post<SubjectTeachingAssignment>(`/api/v2/subject-teaching-assignments/${assignmentId}/replace`, request, { token })
}

export function endSubjectTeachingAssignment(token: string, assignmentId: number, request: EndSubjectTeachingAssignmentRequest): Promise<SubjectTeachingAssignment> {
  return apiClient.post<SubjectTeachingAssignment>(`/api/v2/subject-teaching-assignments/${assignmentId}/end`, request, { token })
}
