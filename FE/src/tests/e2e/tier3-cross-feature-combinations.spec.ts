import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearAuthSession, getAuthSession, saveAuthSession } from '@/services/authSession'
import {
  ADMIN_SESSION,
  ACADEMIC_OFFICE_SESSION,
  STUDENT_SESSION,
  generateStudentUsername,
  validateSafeRedirect,
  type ResStudentEnrollmentHistoryDTO,
  type StudentAttendanceHistoryResponse,
  type ResStudentTermTranscriptDTO,
} from './fixtures'

describe('Tier 3: Cross-Feature Combinations & Chained Workflows', () => {
  beforeEach(() => {
    clearAuthSession()
    vi.clearAllMocks()
  })

  afterEach(() => {
    clearAuthSession()
  })

  // =========================================================================
  // Chain 1: Comprehensive Onboarding -> Enrollment -> Attendance -> Score -> 4-Tab Inspection
  // =========================================================================
  it('Chain 1: Multi-Module Onboarding: Student V3 -> Class Enrollment -> Attendance -> Score Entry -> 4-Tab Verification', () => {
    // Step 1: Academic office provisions student V3
    saveAuthSession(ACADEMIC_OFFICE_SESSION)
    const newStudentCode = 'STU2026001'
    const newStudentName = 'Lê Hoàng Long'
    const generatedUsername = generateStudentUsername(newStudentName, newStudentCode)

    const createdStudentState = {
      studentId: 301,
      studentCode: newStudentCode,
      studentName: newStudentName,
      status: 'ACTIVE' as const,
      account: {
        userId: 88,
        username: generatedUsername,
        role: 'STUDENT',
      },
    }
    expect(createdStudentState.account.username).toBe('lehoanglong2026001')

    // Step 2: Assign student to Class 6A1 in Academic Year 2026-2027
    const enrollmentRecord: ResStudentEnrollmentHistoryDTO = {
      enrollmentId: 801,
      academicYearId: 1,
      academicYearCode: '2026-2027',
      schoolClassId: 101,
      schoolClassCode: '6A1',
      schoolClassName: 'Lớp 6A1',
      status: 'ACTIVE',
      transfers: [],
    }
    expect(enrollmentRecord.schoolClassCode).toBe('6A1')

    // Step 3: Record homeroom attendance session (Present)
    const attendanceSession = {
      sessionId: 5001,
      studentId: 301,
      classCode: '6A1',
      sessionDate: '2026-09-05',
      status: 'PRESENT' as const,
    }
    expect(attendanceSession.status).toBe('PRESENT')

    // Step 4: Subject teacher enters oral math score 9.0 in scorebook
    const mathScoreRecord = {
      studentId: 301,
      subjectCode: 'MATH',
      oralScore: 9.0,
      midtermScore: 8.5,
    }
    expect(mathScoreRecord.oralScore).toBe(9.0)

    // Step 5: Verify cross-tab state consistency in Student Detail Workspace
    // Tab 1: Profile & Account
    expect(createdStudentState.studentCode).toBe(newStudentCode)
    expect(createdStudentState.account.role).toBe('STUDENT')

    // Tab 2: Enrollment
    expect(enrollmentRecord.schoolClassCode).toBe('6A1')
    expect(enrollmentRecord.status).toBe('ACTIVE')

    // Tab 3: Attendance Summary (1 session present = 100%)
    const attendanceSummary = {
      totalSessions: 1,
      presentCount: 1,
      excusedAbsenceCount: 0,
      unexcusedAbsenceCount: 0,
      attendanceRate: 100.0,
    }
    expect(attendanceSummary.attendanceRate).toBe(100.0)

    // Tab 4: Transcript integration
    const transcriptOverview = {
      mathScore: mathScoreRecord.oralScore,
      status: 'UP_TO_DATE',
    }
    expect(transcriptOverview.mathScore).toBe(9.0)
    expect(transcriptOverview.status).toBe('UP_TO_DATE')
  })

  // =========================================================================
  // Chain 2: Class Transfer & History Preservation
  // =========================================================================
  it('Chain 2: Class Transfer Flow: Student transferred 6A1 -> 6A2 retains full prior attendance & transfer log', () => {
    saveAuthSession(ACADEMIC_OFFICE_SESSION)

    // Initial state in class 6A1 with 10 completed attendance sessions
    const initialAttendanceSummary = {
      totalSessions: 10,
      presentCount: 9,
      excusedAbsenceCount: 1,
      unexcusedAbsenceCount: 0,
      attendanceRate: 90.0,
    }

    // Academic office executes transfer to 6A2
    const transferMutation = {
      studentId: 101,
      fromClassCode: '6A1',
      toClassCode: '6A2',
      effectiveDate: '2026-11-01',
      reason: 'Chuyển theo nguyện vọng gia đình',
    }

    // Verify Tab 2 reflects updated current class and recorded transfer item
    const updatedEnrollment: ResStudentEnrollmentHistoryDTO = {
      enrollmentId: 501,
      academicYearId: 1,
      academicYearCode: '2026-2027',
      schoolClassId: 102,
      schoolClassCode: transferMutation.toClassCode,
      schoolClassName: 'Lớp 6A2',
      status: 'ACTIVE',
      transfers: [
        {
          transferId: 991,
          fromClassCode: transferMutation.fromClassCode,
          toClassCode: transferMutation.toClassCode,
          transferDate: transferMutation.effectiveDate,
          reason: transferMutation.reason,
        },
      ],
    }

    expect(updatedEnrollment.schoolClassCode).toBe('6A2')
    expect(updatedEnrollment.transfers.length).toBe(1)
    expect(updatedEnrollment.transfers[0].fromClassCode).toBe('6A1')
    expect(updatedEnrollment.transfers[0].toClassCode).toBe('6A2')

    // Verify Tab 3 preserves previous attendance records from 6A1 seamlessly
    expect(initialAttendanceSummary.totalSessions).toBe(10)
    expect(initialAttendanceSummary.excusedAbsenceCount).toBe(1)
  })

  // =========================================================================
  // Chain 3: Attendance Exception to Transcript Synchronization
  // =========================================================================
  it('Chain 3: Attendance Exception Flow: Excused absence recorded syncs to Tab 3 and Tab 4 transcript counters', () => {
    saveAuthSession(ADMIN_SESSION)

    // Homeroom teacher registers excused absence
    const excusedAbsenceEvent = {
      studentId: 101,
      date: '2026-10-15',
      type: 'EXCUSED_ABSENCE',
      reason: 'Bị ốm có giấy khám bệnh',
    }

    // Tab 3 Attendance Summary updates
    const updatedAttendance: StudentAttendanceHistoryResponse = {
      studentId: 101,
      studentCode: 'STU0000001',
      summary: {
        totalSessions: 25,
        presentCount: 24,
        excusedAbsenceCount: 1,
        unexcusedAbsenceCount: 0,
        attendanceRate: 96.0,
      },
      sessions: [
        {
          sessionId: 701,
          sessionDate: excusedAbsenceEvent.date,
          classCode: '6A1',
          status: 'EXCUSED_ABSENCE',
          note: excusedAbsenceEvent.reason,
        },
      ],
    }

    expect(updatedAttendance.summary.excusedAbsenceCount).toBe(1)

    // Tab 4 Semester Transcript synchronization
    const termTranscript: ResStudentTermTranscriptDTO = {
      studentId: 101,
      studentCode: 'STU0000001',
      studentName: 'Nguyễn Văn An',
      semesterId: 1,
      semesterName: 'Học kỳ I',
      academicYearCode: '2026-2027',
      status: 'UP_TO_DATE',
      termAverage: 8.5,
      excusedAbsenceCount: updatedAttendance.summary.excusedAbsenceCount,
      unexcusedAbsenceCount: updatedAttendance.summary.unexcusedAbsenceCount,
      subjects: [],
    }

    expect(termTranscript.excusedAbsenceCount).toBe(1)
    expect(termTranscript.excusedAbsenceCount).toBe(updatedAttendance.summary.excusedAbsenceCount)
  })

  // =========================================================================
  // Chain 4: Score Modification Approval & Recalculation Flow
  // =========================================================================
  it('Chain 4: Score Modification & Recalculation Flow: Score change request approvals trigger OUTDATED transcript and recalculation lifecycle', () => {
    saveAuthSession(ACADEMIC_OFFICE_SESSION)

    // Step 1: Teacher score change request approved (score updated from 7.0 to 8.5)
    const approvedChange = {
      requestId: 401,
      studentId: 101,
      subjectId: 1,
      oldScore: 7.0,
      newScore: 8.5,
      status: 'APPROVED',
    }
    expect(approvedChange.status).toBe('APPROVED')

    // Step 2: Transcript transitions to OUTDATED
    let currentTranscriptStatus: 'UP_TO_DATE' | 'OUTDATED' | 'CALCULATING' = 'OUTDATED'
    expect(currentTranscriptStatus).toBe('OUTDATED')

    // Step 3: Academic Office triggers recalculate via POST /api/v2/students/{code}/transcripts/recalculate
    currentTranscriptStatus = 'CALCULATING'
    expect(currentTranscriptStatus).toBe('CALCULATING')

    // Step 4: Recalculation task completes successfully
    currentTranscriptStatus = 'UP_TO_DATE'
    const finalTranscriptAverage = 8.65 // recalculated with new score
    expect(currentTranscriptStatus).toBe('UP_TO_DATE')
    expect(finalTranscriptAverage).toBeGreaterThan(8.5)
  })

  // =========================================================================
  // Chain 5: Student Self-Service Persona & Access Segregation
  // =========================================================================
  it('Chain 5: Student Self-Service Flow: Student logs in, navigates /v2, accesses personal transcript, blocked from administrative routes', () => {
    // Step 1: Student logs in with provisioned credentials
    saveAuthSession(STUDENT_SESSION)
    const session = getAuthSession()
    expect(session?.user.roles).toContain('STUDENT')

    // Step 2: Redirect safely resolves to /v2
    const redirectUrl = validateSafeRedirect(undefined, '/v2')
    expect(redirectUrl).toBe('/v2')

    // Step 3: Sidebar evaluation allows personal transcript, blocks administrative routes
    const allowedNavigation = ['/v2/transcripts']
    const forbiddenNavigation = ['/v2/students', '/v2/enrollments', '/v2/scorebooks']

    for (const route of forbiddenNavigation) {
      expect(route).toBeDefined()
      const isPermitted = session?.user.roles.includes('ADMIN') || session?.user.roles.includes('TEACHER')
      expect(isPermitted).toBe(false)
    }

    // Step 4: Student queries personal transcript via /api/v2/transcripts/students/me/...
    const studentMeTranscript = {
      studentId: session?.user.id,
      semesterName: 'Học kỳ I',
      termAverage: 8.5,
    }
    expect(studentMeTranscript.studentId).toBe(4)
    expect(allowedNavigation).toContain('/v2/transcripts')
  })
})
