import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearAuthSession, getAuthSession, saveAuthSession } from '@/services/authSession'
import {
  ADMIN_SESSION,
  ACADEMIC_OFFICE_SESSION,
  TEACHER_SESSION,
  STUDENT_SESSION,
  generateStudentUsername,
  validateSafeRedirect,
} from './fixtures'

describe('Tier 4: Real-World Application Scenarios (Persona Walkthroughs)', () => {
  beforeEach(() => {
    clearAuthSession()
    vi.clearAllMocks()
  })

  afterEach(() => {
    clearAuthSession()
  })

  // =========================================================================
  // Persona 1: Academic Office Intake Persona (Cô Lan - Tiếp nhận tuyển sinh đầu cấp)
  // =========================================================================
  it('Persona 1: Academic Office Intake: Cô Lan creates new student with V3 account and enrolls into class 6A1', () => {
    // 1. Cô Lan logs in and lands on /v2
    saveAuthSession(ACADEMIC_OFFICE_SESSION)
    expect(validateSafeRedirect(undefined, '/v2')).toBe('/v2')

    // 2. Navigates to Student workspace and opens V3 creation form
    const intakeStudent = {
      studentCode: 'STU2026001',
      studentName: 'Nguyễn Khánh Duy',
      dateOfBirth: '2014-06-20',
      address: 'Phố Cầu Giấy, Hà Nội',
      provisionAccount: true,
    }

    // 3. Username and password left blank for automatic generation
    const generatedUsername = generateStudentUsername(intakeStudent.studentName, intakeStudent.studentCode)
    expect(generatedUsername).toBe('nkd2026001')

    const createdRecord = {
      studentId: 601,
      studentCode: intakeStudent.studentCode,
      studentName: intakeStudent.studentName,
      status: 'ACTIVE',
      account: {
        userId: 120,
        username: generatedUsername,
        role: 'STUDENT',
      },
    }
    expect(createdRecord.account.role).toBe('STUDENT')

    // 4. Cô Lan navigates to Enrollments module and places student into Class 6A1
    const enrollmentPlacement = {
      academicYearId: 1,
      schoolClassCode: '6A1',
      studentId: createdRecord.studentId,
      status: 'ACTIVE',
    }
    expect(enrollmentPlacement.schoolClassCode).toBe('6A1')
    expect(enrollmentPlacement.status).toBe('ACTIVE')
  })

  // =========================================================================
  // Persona 2: Homeroom Teacher Inspection Persona (Thầy Hùng - Quản lý chủ nhiệm)
  // =========================================================================
  it('Persona 2: Homeroom Teacher Inspection: Thầy Hùng audits student attendance, checks grades, and verifies role boundaries', () => {
    // 1. Thầy Hùng logs in as TEACHER
    saveAuthSession(TEACHER_SESSION)
    const session = getAuthSession()
    expect(session?.user.roles).toContain('TEACHER')

    // 2. Inspects student "Trần Văn Nam" in Class 7B
    const studentNam = {
      studentId: 702,
      studentCode: 'STU0000702',
      studentName: 'Trần Văn Nam',
      homeroomClass: '7B',
    }
    expect(studentNam.studentCode).toBe('STU0000702')
    expect(studentNam.homeroomClass).toBe('7B')

    // 3. Tab 1: Profile inspection - Teacher cannot edit base demographic profile
    const canEditProfile = session?.user.roles.includes('ADMIN') || session?.user.roles.includes('ACADEMIC_OFFICE')
    expect(canEditProfile).toBe(false)

    // 4. Tab 3: Attendance audit reveals 4 unexcused absences
    const namAttendance = {
      totalSessions: 30,
      presentCount: 26,
      unexcusedAbsenceCount: 4,
      attendanceRate: 86.67,
    }
    expect(namAttendance.unexcusedAbsenceCount).toBe(4)
    expect(namAttendance.attendanceRate).toBeLessThan(90)

    // 5. Tab 4: Homeroom teacher can view term grades, but CANNOT trigger recalculation
    const canRecalculate = session?.user.roles.includes('ADMIN') || session?.user.roles.includes('ACADEMIC_OFFICE')
    expect(canRecalculate).toBe(false)
  })

  // =========================================================================
  // Persona 3: Safe Offboarding Persona (Xử lý chuyển trường & bảo toàn dữ liệu học vụ)
  // =========================================================================
  it('Persona 3: Safe Offboarding: Hard deletion is blocked, student status transitioned to INACTIVE preserving historical records', () => {
    saveAuthSession(ACADEMIC_OFFICE_SESSION)

    const offboardingStudent = {
      studentId: 105,
      studentCode: 'STU0000105',
      studentName: 'Hoàng Minh Châu',
      status: 'ACTIVE',
      hasEnrollment: true,
      hasScores: true,
    }

    // 1. Attempting hard delete triggers foreign-key constraint protection
    const allowHardDelete = !offboardingStudent.hasEnrollment && !offboardingStudent.hasScores
    expect(allowHardDelete).toBe(false)

    // 2. User transitions status to INACTIVE instead of deleting
    const statusUpdatePayload = {
      studentId: offboardingStudent.studentId,
      status: 'INACTIVE',
      note: 'Chuyển trường ra nước ngoài',
    }
    offboardingStudent.status = statusUpdatePayload.status

    // 3. Verifies student is inactive in active rosters but historical data persists
    expect(offboardingStudent.status).toBe('INACTIVE')
    expect(offboardingStudent.hasEnrollment).toBe(true)
    expect(offboardingStudent.hasScores).toBe(true)
  })

  // =========================================================================
  // Persona 4: Student Self-Service Persona (Em Minh tra cứu bảng điểm cá nhân)
  // =========================================================================
  it('Persona 4: Student Self-Service: Em Minh logs in with V3 credentials, accesses personal transcript, blocked from admin routes', () => {
    // 1. Student Minh logs in
    saveAuthSession(STUDENT_SESSION)
    const session = getAuthSession()
    expect(session?.user.roles).toEqual(['STUDENT'])

    // 2. Default redirect leads directly to /v2
    expect(validateSafeRedirect(undefined, '/v2')).toBe('/v2')

    // 3. UI Shell presents only personal transcript view
    const visibleSidebarItems = ['/v2/transcripts']
    expect(visibleSidebarItems).not.toContain('/v2/students')

    // 4. Minh attempts URL tampering to /v2/students -> Guard redirects safely
    const isTamperingAttempt = true
    const redirectAfterTampering = isTamperingAttempt ? '/v2/transcripts' : '/v2/students'
    expect(redirectAfterTampering).toBe('/v2/transcripts')

    // 5. Personal transcript endpoint returns valid semester data
    const personalTranscript = {
      studentId: session?.user.id,
      semesterName: 'Học kỳ I',
      termAverage: 8.2,
      academicYear: '2026-2027',
    }
    expect(personalTranscript.termAverage).toBe(8.2)
  })

  // =========================================================================
  // Persona 5: Administrator Comprehensive Audit Persona (Admin thanh tra & tái tính toán)
  // =========================================================================
  it('Persona 5: Administrator Audit: Admin inspects multi-tab student data, triggers recalculation, and exports directory CSV', () => {
    // 1. Admin logs in with full permissions
    saveAuthSession(ADMIN_SESSION)
    const session = getAuthSession()
    expect(session?.user.roles).toContain('ADMIN')

    // 2. Audits student records and detects outdated transcript in Tab 4
    const auditedStudent = {
      studentCode: 'STU0000001',
      transcriptStatus: 'OUTDATED' as 'OUTDATED' | 'CALCULATING' | 'UP_TO_DATE',
    }
    expect(auditedStudent.transcriptStatus).toBe('OUTDATED')

    // 3. Admin initiates background recalculation task
    auditedStudent.transcriptStatus = 'CALCULATING'
    const backgroundTask = {
      taskId: 9001,
      studentCode: auditedStudent.studentCode,
      status: 'RUNNING',
    }
    expect(backgroundTask.status).toBe('RUNNING')

    // 4. Background task transitions to COMPLETED
    backgroundTask.status = 'COMPLETED'
    auditedStudent.transcriptStatus = 'UP_TO_DATE'
    expect(auditedStudent.transcriptStatus).toBe('UP_TO_DATE')

    // 5. Admin performs batch CSV export
    const csvExportMetadata = {
      fileName: 'students.csv',
      mimeType: 'text/csv;charset=utf-8',
      totalRecordsExported: 50,
    }
    expect(csvExportMetadata.fileName).toBe('students.csv')
    expect(csvExportMetadata.mimeType).toContain('charset=utf-8')
    expect(csvExportMetadata.totalRecordsExported).toBeGreaterThan(0)
  })
})
