import { apiClient } from '@/services/apiClient'
import type {
  AcademicYear,
  AcademicYearRequest,
  CreateSemesterRequest,
  ReopenSemesterRequest,
  Semester,
  SemesterCompletenessReport,
  UpdateSemesterRequest,
} from '@/types/academic'

const academicYearsPath = '/api/v2/academic-years'
const semestersPath = '/api/v2/semesters'

export function fetchAcademicYears(token: string): Promise<AcademicYear[]> {
  return apiClient.get<AcademicYear[]>(academicYearsPath, { token })
}

export function createAcademicYear(token: string, request: AcademicYearRequest): Promise<AcademicYear> {
  return apiClient.post<AcademicYear>(academicYearsPath, request, { token })
}

export function updateAcademicYear(token: string, academicYearId: number, request: AcademicYearRequest): Promise<AcademicYear> {
  return apiClient.put<AcademicYear>(`${academicYearsPath}/${academicYearId}`, request, { token })
}

export function closeAcademicYear(token: string, academicYearId: number): Promise<AcademicYear> {
  return apiClient.post<AcademicYear>(`${academicYearsPath}/${academicYearId}/close`, undefined, { token })
}

export function fetchSemesters(token: string, academicYearId: number): Promise<Semester[]> {
  return apiClient.get<Semester[]>(semestersPath, {
    token,
    query: new URLSearchParams({ academicYearId: String(academicYearId) }),
  })
}

export function createSemester(token: string, request: CreateSemesterRequest): Promise<Semester> {
  return apiClient.post<Semester>(semestersPath, request, { token })
}

export function updateSemester(token: string, semesterId: number, request: UpdateSemesterRequest): Promise<Semester> {
  return apiClient.put<Semester>(`${semestersPath}/${semesterId}`, request, { token })
}

export function activateSemester(token: string, semesterId: number): Promise<Semester> {
  return apiClient.post<Semester>(`${semestersPath}/${semesterId}/activate`, undefined, { token })
}

export function getSemesterCompletenessReport(token: string, semesterId: number, checkpointCode?: string): Promise<SemesterCompletenessReport> {
  const query = checkpointCode?.trim() ? new URLSearchParams({ checkpointCode: checkpointCode.trim() }) : undefined
  return apiClient.get<SemesterCompletenessReport>(`${semestersPath}/${semesterId}/completeness-report`, { token, query })
}

export function lockSemester(token: string, semesterId: number): Promise<Semester> {
  return apiClient.post<Semester>(`${semestersPath}/${semesterId}/lock`, undefined, { token })
}

export function reopenSemester(token: string, semesterId: number, request: ReopenSemesterRequest): Promise<Semester> {
  return apiClient.post<Semester>(`${semestersPath}/${semesterId}/reopen`, request, { token })
}
