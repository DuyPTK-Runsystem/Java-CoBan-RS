import type { AuthSession } from '@/services/authSession'
import type { Student, StudentPage } from '@/types/student'

export const ADMIN_SESSION: AuthSession = {
  accessToken: 'jwt-token-admin',
  user: {
    id: 1,
    username: 'admin.user',
    roles: ['ADMIN'],
  },
}

export const ACADEMIC_OFFICE_SESSION: AuthSession = {
  accessToken: 'jwt-token-office',
  user: {
    id: 2,
    username: 'office.user',
    roles: ['ACADEMIC_OFFICE'],
  },
}

export const TEACHER_SESSION: AuthSession = {
  accessToken: 'jwt-token-teacher',
  user: {
    id: 3,
    username: 'teacher.user',
    roles: ['TEACHER'],
  },
}

export const STUDENT_SESSION: AuthSession = {
  accessToken: 'jwt-token-student',
  user: {
    id: 4,
    username: 'student.user',
    roles: ['STUDENT'],
  },
}

export interface StudentV3CreateRequest {
  studentCode: string
  studentName: string
  dateOfBirth: string | null
  address: string
  username?: string | null
  password?: string | null
}

export interface StudentV3CreateResponse {
  studentId: number
  studentCode: string
  studentName: string
  dateOfBirth: string | null
  address: string
  account: {
    userId: number
    username: string
    role: string
  }
}

export interface ResStudentEnrollmentHistoryDTO {
  enrollmentId: number
  academicYearId: number
  academicYearCode: string
  schoolClassId: number
  schoolClassCode: string
  schoolClassName: string
  status: 'ACTIVE' | 'TRANSFERRED' | 'INACTIVE'
  transfers: Array<{
    transferId: number
    fromClassCode: string
    toClassCode: string
    transferDate: string
    reason: string
  }>
}

export interface StudentAttendanceSummary {
  totalSessions: number
  presentCount: number
  excusedAbsenceCount: number
  unexcusedAbsenceCount: number
  attendanceRate: number
}

export interface StudentAttendanceSessionRecord {
  sessionId: number
  sessionDate: string
  classCode: string
  status: 'PRESENT' | 'EXCUSED_ABSENCE' | 'UNEXCUSED_ABSENCE'
  note?: string
}

export interface StudentAttendanceHistoryResponse {
  studentId: number
  studentCode: string
  summary: StudentAttendanceSummary
  sessions: StudentAttendanceSessionRecord[]
}

export interface TermTranscriptSubjectScore {
  subjectId: number
  subjectName: string
  regularScores: number[]
  midtermScore: number | null
  finalScore: number | null
  termAverageScore: number | null
}

export interface ResStudentTermTranscriptDTO {
  studentId: number
  studentCode: string
  studentName: string
  semesterId: number
  semesterName: string
  academicYearCode: string
  status: 'UP_TO_DATE' | 'OUTDATED' | 'CALCULATING'
  termAverage: number | null
  excusedAbsenceCount: number
  unexcusedAbsenceCount: number
  subjects: TermTranscriptSubjectScore[]
}

export interface ResCalculationTaskDTO {
  taskId: number
  studentCode: string
  academicYearId: number
  status: 'SUBMITTED' | 'RUNNING' | 'COMPLETED' | 'FAILED'
  requestedAt: string
}

export const SAMPLE_STUDENTS: Student[] = [
  {
    studentId: 101,
    studentCode: 'STU0000001',
    studentName: 'Nguyễn Văn An',
    dateOfBirth: '2010-05-15',
    address: '123 Đường Láng, Hà Nội',
    averageScore: null,
  },
  {
    studentId: 102,
    studentCode: 'STU0000002',
    studentName: 'Trần Thị Bình',
    dateOfBirth: '2010-08-20',
    address: '456 Cầu Giấy, Hà Nội',
    averageScore: null,
  },
  {
    studentId: 103,
    studentCode: 'STU0000003',
    studentName: 'Lê Hoàng Long',
    dateOfBirth: '2010-11-02',
    address: '789 Giải Phóng, Hà Nội',
    averageScore: null,
  },
]

export const SAMPLE_STUDENT_PAGE: StudentPage = {
  content: SAMPLE_STUDENTS,
  page: 0,
  size: 10,
  totalElements: 3,
  totalPages: 1,
}

export const SAMPLE_ENROLLMENT_HISTORY: ResStudentEnrollmentHistoryDTO[] = [
  {
    enrollmentId: 501,
    academicYearId: 1,
    academicYearCode: '2026-2027',
    schoolClassId: 101,
    schoolClassCode: '6A1',
    schoolClassName: 'Lớp 6A1',
    status: 'ACTIVE',
    transfers: [
      {
        transferId: 901,
        fromClassCode: '6A2',
        toClassCode: '6A1',
        transferDate: '2026-10-01',
        reason: 'Chuyển theo nguyện vọng gia đình',
      },
    ],
  },
]

export const SAMPLE_ATTENDANCE_HISTORY: StudentAttendanceHistoryResponse = {
  studentId: 101,
  studentCode: 'STU0000001',
  summary: {
    totalSessions: 20,
    presentCount: 18,
    excusedAbsenceCount: 2,
    unexcusedAbsenceCount: 0,
    attendanceRate: 90.0,
  },
  sessions: [
    {
      sessionId: 1001,
      sessionDate: '2026-09-05',
      classCode: '6A1',
      status: 'PRESENT',
    },
    {
      sessionId: 1002,
      sessionDate: '2026-09-06',
      classCode: '6A1',
      status: 'EXCUSED_ABSENCE',
      note: 'Có đơn xin phép của phụ huynh',
    },
  ],
}

export const SAMPLE_TERM_TRANSCRIPT: ResStudentTermTranscriptDTO = {
  studentId: 101,
  studentCode: 'STU0000001',
  studentName: 'Nguyễn Văn An',
  semesterId: 1,
  semesterName: 'Học kỳ I',
  academicYearCode: '2026-2027',
  status: 'UP_TO_DATE',
  termAverage: 8.5,
  excusedAbsenceCount: 2,
  unexcusedAbsenceCount: 0,
  subjects: [
    {
      subjectId: 1,
      subjectName: 'Toán',
      regularScores: [9.0, 8.5],
      midtermScore: 8.0,
      finalScore: 9.0,
      termAverageScore: 8.7,
    },
  ],
}

/**
 * Validates whether a redirect path is safe (strictly internal relative path starting with single /)
 */
export function validateSafeRedirect(target: string | null | undefined, defaultFallback = '/v2'): string {
  if (!target || typeof target !== 'string') return defaultFallback
  const trimmed = target.trim()
  if (!trimmed.startsWith('/') || trimmed.startsWith('//') || trimmed.includes('\\') || trimmed.includes(':')) {
    return defaultFallback
  }
  return trimmed
}

/**
 * Username generator logic following CR-STUDENT-001 & StudentUsernameGenerator
 */
export function generateStudentUsername(fullName: string, studentCode: string): string {
  const normalized = fullName
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'd')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '')

  const codeSuffix = studentCode.replace(/^STU/i, '').slice(-7)
  const fullCandidate = `${normalized}${codeSuffix}`
  if (fullCandidate.length <= 20) {
    return fullCandidate
  }

  // Fallback: take first letter of each word + code suffix
  const initials = fullName
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'd')
    .split(/\s+/)
    .filter(Boolean)
    .map(w => w[0].toLowerCase())
    .join('')

  const fallbackCandidate = `${initials}${codeSuffix}`
  return fallbackCandidate.slice(0, 20)
}
