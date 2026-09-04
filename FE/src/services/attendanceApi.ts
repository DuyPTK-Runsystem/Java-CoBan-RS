import { apiClient } from '@/services/apiClient'
import type {
  AttendanceApiScope,
  AttendanceCalendarDay,
  AttendanceCalendarQuery,
  AttendanceException,
  AttendanceSession,
  AttendanceStudent,
  ClassAttendanceSummaryQuery,
  ClassAttendanceSummaryResponse,
  CreateAttendanceSessionRequest,
  StudentAttendanceHistoryQuery,
  StudentAttendanceHistoryResponse,
  UpsertAttendanceExceptionRequest,
} from '@/types/attendance'

const teacherSessionPath = '/api/v2/attendance-sessions'
const officeSessionPath = '/api/v2/office/attendance-sessions'

function sessionPath(scope: AttendanceApiScope): string {
  return scope === 'office' ? officeSessionPath : teacherSessionPath
}

export function fetchAttendanceCalendar(token: string, query: AttendanceCalendarQuery): Promise<AttendanceCalendarDay[]> {
  return apiClient.get<AttendanceCalendarDay[]>('/api/v2/calendar/days', {
    token,
    query: new URLSearchParams({
      academicYearId: String(query.academicYearId),
      semesterId: String(query.semesterId),
      from: query.from,
      to: query.to,
    }),
  })
}

export function createOrGetAttendanceSession(token: string, request: CreateAttendanceSessionRequest, scope: AttendanceApiScope = 'teacher'): Promise<AttendanceSession> {
  return apiClient.post<AttendanceSession>(sessionPath(scope), request, { token })
}

export function fetchAttendanceSession(token: string, query: CreateAttendanceSessionRequest, scope: AttendanceApiScope = 'teacher'): Promise<AttendanceSession> {
  return apiClient.get<AttendanceSession>(sessionPath(scope), {
    token,
    query: new URLSearchParams({
      classId: String(query.classId),
      semesterId: String(query.semesterId),
      attendanceDate: query.attendanceDate,
      sessionPeriod: query.sessionPeriod,
    }),
  })
}

export function fetchAttendanceSessionStudents(token: string, sessionId: number, scope: AttendanceApiScope = 'teacher'): Promise<AttendanceStudent[]> {
  return apiClient.get<AttendanceStudent[]>(`${sessionPath(scope)}/${sessionId}/students`, { token })
}

export function upsertAttendanceException(token: string, sessionId: number, studentId: number, request: UpsertAttendanceExceptionRequest, scope: AttendanceApiScope = 'teacher'): Promise<AttendanceException> {
  return apiClient.put<AttendanceException>(`${sessionPath(scope)}/${sessionId}/exceptions/${studentId}`, request, { token })
}

export function upsertAttendanceExceptionByCode(token: string, sessionId: number, studentCode: string, request: UpsertAttendanceExceptionRequest, scope: AttendanceApiScope = 'teacher'): Promise<AttendanceException> {
  return apiClient.put<AttendanceException>(`${sessionPath(scope)}/${sessionId}/exceptions/by-code/${encodeURIComponent(studentCode)}`, request, { token })
}

export function deleteAttendanceException(token: string, sessionId: number, studentId: number, scope: AttendanceApiScope = 'teacher'): Promise<void> {
  return apiClient.delete<void>(`${sessionPath(scope)}/${sessionId}/exceptions/${studentId}`, { token })
}

export function deleteAttendanceExceptionByCode(token: string, sessionId: number, studentCode: string, scope: AttendanceApiScope = 'teacher'): Promise<void> {
  return apiClient.delete<void>(`${sessionPath(scope)}/${sessionId}/exceptions/by-code/${encodeURIComponent(studentCode)}`, { token })
}

export function fetchStudentAttendanceHistory(token: string, query: StudentAttendanceHistoryQuery): Promise<StudentAttendanceHistoryResponse> {
  const params = new URLSearchParams({ page: String(query.page), size: String(query.size) })
  if (query.academicYearId !== undefined && query.academicYearId !== null) params.set('academicYearId', String(query.academicYearId))
  if (query.semesterId !== undefined && query.semesterId !== null) params.set('semesterId', String(query.semesterId))
  if (query.from) params.set('from', query.from)
  if (query.to) params.set('to', query.to)
  return apiClient.get<StudentAttendanceHistoryResponse>('/api/v2/attendance/students/me/history', { token, query: params })
}

export function fetchStudentAttendanceHistoryById(
  token: string,
  studentId: number,
  query: StudentAttendanceHistoryQuery,
): Promise<StudentAttendanceHistoryResponse> {
  const params = new URLSearchParams({ page: String(query.page), size: String(query.size) })
  if (query.academicYearId !== undefined && query.academicYearId !== null) params.set('academicYearId', String(query.academicYearId))
  if (query.semesterId !== undefined && query.semesterId !== null) params.set('semesterId', String(query.semesterId))
  if (query.from) params.set('from', query.from)
  if (query.to) params.set('to', query.to)
  return apiClient.get<StudentAttendanceHistoryResponse>(`/api/v2/attendance/students/${studentId}/history`, { token, query: params })
}

export function fetchClassAttendanceSummary(token: string, classId: number, query: ClassAttendanceSummaryQuery): Promise<ClassAttendanceSummaryResponse> {
  return apiClient.get<ClassAttendanceSummaryResponse>(`/api/v2/attendance/classes/${classId}/summary`, {
    token,
    query: new URLSearchParams({
      semesterId: String(query.semesterId),
      from: query.from,
      to: query.to,
      page: String(query.page),
      size: String(query.size),
    }),
  })
}
