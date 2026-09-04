export type AttendanceSessionPeriod = 'MORNING' | 'AFTERNOON'
export type AttendanceExceptionStatus = 'ABSENT' | 'EXCUSED' | 'LATE' | 'EARLY_LEAVE'
export type AttendanceUiStatus = 'PRESENT' | AttendanceExceptionStatus
export type AttendanceApiScope = 'teacher' | 'office'
export type CalendarDayType = 'SCHOOL_DAY' | 'WEEKEND' | 'HOLIDAY' | 'NO_CLASS'
export type CalendarSessionStatus = 'SCHEDULED' | 'NO_CLASS'

export interface AttendanceSession {
  sessionId: number
  classId: number
  semesterId: number
  attendanceDate: string
  sessionPeriod: AttendanceSessionPeriod
  createdBy: number
  createdAt: string
}

export interface AttendanceStudent {
  studentId: number
  studentCode: string
  studentName: string
  attendanceRecordId: number | null
  status: string
  note: string | null
  recordedBy: number | null
  recordedAt: string | null
  updatedBy: number | null
  updatedAt: string | null
}

export interface AttendanceException {
  attendanceRecordId: number
  sessionId: number
  studentId: number
  studentCode: string
  studentName: string
  status: AttendanceExceptionStatus
  note: string | null
  recordedBy: number | null
  recordedAt: string | null
  updatedBy: number | null
  updatedAt: string | null
}

export interface CreateAttendanceSessionRequest {
  classId: number
  semesterId: number
  attendanceDate: string
  sessionPeriod: AttendanceSessionPeriod
}

export interface UpsertAttendanceExceptionRequest {
  status: AttendanceExceptionStatus
  note: string | null
}

export interface AttendanceCalendarSession {
  id: number
  sessionPeriod: AttendanceSessionPeriod
  sessionStatus: CalendarSessionStatus
  reason: string | null
  configuredBy: number | null
  configuredAt: string | null
  updatedBy: number | null
  updatedAt: string | null
}

export interface AttendanceCalendarDay {
  id: number
  calendarDate: string
  academicYearId: number
  semesterId: number
  dayType: CalendarDayType
  reason: string | null
  configuredBy: number | null
  configuredAt: string | null
  updatedBy: number | null
  updatedAt: string | null
  sessions: AttendanceCalendarSession[]
}

export interface AttendanceCalendarQuery {
  academicYearId: number
  semesterId: number
  from: string
  to: string
}

export interface StudentAttendanceHistoryQuery {
  academicYearId?: number | null
  semesterId?: number | null
  from?: string
  to?: string
  page: number
  size: number
}

export interface StudentAttendanceHistoryItem {
  attendanceDate: string
  sessionPeriod: AttendanceSessionPeriod
  classId: number
  className: string
  status: string
  attendanceRecordId: number | null
  exceptionStatus: string | null
  note: string | null
}

export interface StudentAttendanceHistorySummary {
  validSessionCount: number
  presentCount: number
  excusedAbsenceCount: number
  unexcusedAbsenceCount: number
  lateCount: number
  earlyLeaveCount: number
}

export interface StudentAttendanceHistoryResponse {
  items: StudentAttendanceHistoryItem[]
  summary: StudentAttendanceHistorySummary
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ClassAttendanceSummaryQuery {
  semesterId: number
  from: string
  to: string
  page: number
  size: number
}

export interface ClassAttendanceSummaryTotals {
  presentCount: number
  excusedAbsenceCount: number
  unexcusedAbsenceCount: number
  lateCount: number
  earlyLeaveCount: number
}

export interface ClassAttendanceStudentSummary {
  studentId: number
  studentCode: string
  fullName: string
  validSessionCount: number
  presentCount: number
  excusedAbsenceCount: number
  unexcusedAbsenceCount: number
  lateCount: number
  earlyLeaveCount: number
  attendanceRate: number
}

export interface ClassAttendanceSummaryResponse {
  class: {
    id: number
    name: string
    gradeLevelId: number
  }
  semesterId: number
  from: string
  to: string
  validSessionCount: number
  summary: ClassAttendanceSummaryTotals
  students: ClassAttendanceStudentSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
