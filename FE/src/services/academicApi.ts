import { apiClient } from '@/services/apiClient'
import type {
  AcademicYear,
  AcademicYearRequest,
  ClassSubject,
  CreateClassSubjectRequest,
  CreateSchoolClassRequest,
  CreateSemesterRequest,
  GradeLevel,
  GradeLevelRequest,
  ReopenSemesterRequest,
  SchoolClass,
  Semester,
  SemesterCompletenessReport,
  SemesterNotification,
  SemesterNotificationStatus,
  Subject,
  SubjectApplicability,
  SubjectApplicabilityRequest,
  SubjectApplicabilityStatus,
  SubjectRequest,
  SubjectStatus,
  UpdateClassSubjectRequest,
  UpdateSchoolClassRequest,
  UpdateSubjectApplicabilityRequest,
  UpdateSemesterRequest,
  AcademicYearStatistics,
} from '@/types/academic'

const academicYearsPath = '/api/v2/academic-years'
const semestersPath = '/api/v2/semesters'
const gradesPath = '/api/v2/grades'
const classesPath = '/api/v2/classes'
const subjectsPath = '/api/v2/subjects'
const classSubjectsPath = '/api/v2/class-subjects'

export function fetchGrades(token: string): Promise<GradeLevel[]> {
  return apiClient.get<GradeLevel[]>(gradesPath, { token })
}

export function createGrade(token: string, request: GradeLevelRequest): Promise<GradeLevel> {
  return apiClient.post<GradeLevel>(gradesPath, request, { token })
}

export function updateGrade(token: string, gradeId: number, request: GradeLevelRequest): Promise<GradeLevel> {
  return apiClient.put<GradeLevel>(`${gradesPath}/${gradeId}`, request, { token })
}

export function deleteGrade(token: string, gradeId: number): Promise<void> {
  return apiClient.delete<void>(`${gradesPath}/${gradeId}`, { token })
}

export function fetchSchoolClasses(token: string, academicYearId?: number): Promise<SchoolClass[]> {
  return apiClient.get<SchoolClass[]>(classesPath, {
    token,
    query: academicYearId === undefined ? undefined : new URLSearchParams({ academicYearId: String(academicYearId) }),
  })
}

export function createSchoolClass(token: string, request: CreateSchoolClassRequest): Promise<SchoolClass> {
  return apiClient.post<SchoolClass>(classesPath, request, { token })
}

export function updateSchoolClass(token: string, classId: number, request: UpdateSchoolClassRequest): Promise<SchoolClass> {
  return apiClient.put<SchoolClass>(`${classesPath}/${classId}`, request, { token })
}

export function closeSchoolClass(token: string, classId: number): Promise<SchoolClass> {
  return apiClient.post<SchoolClass>(`${classesPath}/${classId}/close`, undefined, { token })
}

export function deleteSchoolClass(token: string, classId: number): Promise<void> {
  return apiClient.delete<void>(`${classesPath}/${classId}`, { token })
}

export function fetchSubjects(token: string, status?: SubjectStatus): Promise<Subject[]> {
  return apiClient.get<Subject[]>(subjectsPath, {
    token,
    query: status === undefined ? undefined : new URLSearchParams({ status }),
  })
}

export function createSubject(token: string, request: SubjectRequest): Promise<Subject> {
  return apiClient.post<Subject>(subjectsPath, request, { token })
}

export function updateSubject(token: string, subjectId: number, request: SubjectRequest): Promise<Subject> {
  return apiClient.put<Subject>(`${subjectsPath}/${subjectId}`, request, { token })
}

export function createSubjectApplicability(token: string, subjectId: number, request: SubjectApplicabilityRequest): Promise<SubjectApplicability> {
  return apiClient.post<SubjectApplicability>(`${subjectsPath}/${subjectId}/applicabilities`, request, { token })
}

export function fetchSubjectApplicabilities(token: string, subjectId: number, semesterId?: number, status?: SubjectApplicabilityStatus): Promise<SubjectApplicability[]> {
  const query = new URLSearchParams()
  if (semesterId !== undefined) query.set('semesterId', String(semesterId))
  if (status !== undefined) query.set('status', status)
  return apiClient.get<SubjectApplicability[]>(`${subjectsPath}/${subjectId}/applicabilities`, {
    token,
    query: query.size ? query : undefined,
  })
}

export function updateSubjectApplicability(token: string, subjectId: number, applicabilityId: number, request: UpdateSubjectApplicabilityRequest): Promise<SubjectApplicability> {
  return apiClient.put<SubjectApplicability>(`${subjectsPath}/${subjectId}/applicabilities/${applicabilityId}`, request, { token })
}

export function deactivateSubjectApplicability(token: string, subjectId: number, applicabilityId: number): Promise<void> {
  return apiClient.delete<void>(`${subjectsPath}/${subjectId}/applicabilities/${applicabilityId}`, { token })
}

export function fetchClassSubjects(token: string, classId: number, semesterId: number): Promise<ClassSubject[]> {
  return apiClient.get<ClassSubject[]>(`/api/v2/classes/${classId}/subjects`, {
    token,
    query: new URLSearchParams({ semesterId: String(semesterId) }),
  })
}

export function createClassSubject(token: string, request: CreateClassSubjectRequest): Promise<ClassSubject> {
  return apiClient.post<ClassSubject>(classSubjectsPath, request, { token })
}

export function updateClassSubject(token: string, classSubjectId: number, request: UpdateClassSubjectRequest): Promise<ClassSubject> {
  return apiClient.put<ClassSubject>(`${classSubjectsPath}/${classSubjectId}`, request, { token })
}

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

interface SemesterNotificationResponse {
  id: number
  semesterId: number
  recipientEmail: string
  recipientRole: string
  status: SemesterNotificationStatus
  subject: string
  attemptCount: number
  sentAt: string | null
  errorMessage: string | null
  createdAt: string
  updatedAt: string
  checkpointCode?: string
  notificationChannel?: string
  reportId?: number | null
  recipientTeacherId?: number | null
  bodyContent?: string
}

function toSemesterNotification(item: SemesterNotificationResponse): SemesterNotification {
  return {
    id: item.id,
    semesterId: item.semesterId,
    recipientEmail: item.recipientEmail,
    recipientRole: item.recipientRole,
    status: item.status,
    subject: item.subject,
    attemptCount: item.attemptCount,
    sentAt: item.sentAt,
    errorMessage: item.errorMessage,
    createdAt: item.createdAt,
    updatedAt: item.updatedAt,
  }
}

export async function fetchSemesterNotifications(token: string, semesterId: number): Promise<SemesterNotification[]> {
  const result = await apiClient.get<SemesterNotificationResponse[]>(`${semestersPath}/${semesterId}/notifications`, { token })
  return result.map(toSemesterNotification)
}

export async function dispatchSemesterNotifications(token: string, semesterId: number): Promise<SemesterNotification[]> {
  const result = await apiClient.post<SemesterNotificationResponse[]>(`${semestersPath}/${semesterId}/notifications/dispatch`, undefined, { token })
  return result.map(toSemesterNotification)
}

export async function retryFailedSemesterNotifications(token: string, semesterId: number): Promise<SemesterNotification[]> {
  const result = await apiClient.post<SemesterNotificationResponse[]>(`${semestersPath}/${semesterId}/notifications/retry-failed`, undefined, { token })
  return result.map(toSemesterNotification)
}

export function reopenSemester(token: string, semesterId: number, request: ReopenSemesterRequest): Promise<Semester> {
  return apiClient.post<Semester>(`${semestersPath}/${semesterId}/reopen`, request, { token })
}

export function fetchAcademicYearStatistics(token: string, academicYearId: number): Promise<AcademicYearStatistics> {
  return apiClient.get<AcademicYearStatistics>(`/api/v2/academic/years/${academicYearId}/statistics`, { token })
}

