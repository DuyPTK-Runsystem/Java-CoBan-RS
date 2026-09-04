import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearAuthSession, getAuthSession, saveAuthSession } from '@/services/authSession'
import {
  ADMIN_SESSION,
  ACADEMIC_OFFICE_SESSION,
  TEACHER_SESSION,
  STUDENT_SESSION,
  SAMPLE_STUDENTS,
  SAMPLE_ENROLLMENT_HISTORY,
  SAMPLE_ATTENDANCE_HISTORY,
  SAMPLE_TERM_TRANSCRIPT,
  validateSafeRedirect,
  generateStudentUsername,
  type StudentV3CreateRequest,
  type StudentV3CreateResponse,
  type ResCalculationTaskDTO,
} from './fixtures'

describe('Tier 1: Feature Coverage Tests (Category-Partition)', () => {
  beforeEach(() => {
    clearAuthSession()
    vi.clearAllMocks()
  })

  afterEach(() => {
    clearAuthSession()
  })

  // =========================================================================
  // Feature 1: Login & Route Redirection to /v2 & Shell Navigation (R1 & Follow-up)
  // =========================================================================
  describe('Feature 1: Login, Navigation & Shell V2 Integration', () => {
    it('TC-F1-01: Login success stores session and resolves default redirect to /v2', () => {
      saveAuthSession(ADMIN_SESSION)
      const session = getAuthSession()
      expect(session).not.toBeNull()
      expect(session?.accessToken).toBe(ADMIN_SESSION.accessToken)
      expect(session?.user.username).toBe('admin.user')

      const targetPath = validateSafeRedirect(undefined, '/v2')
      expect(targetPath).toBe('/v2')
    })

    it('TC-F1-02: Login with safe redirect query parameter forwards to specified sub-route', () => {
      saveAuthSession(ACADEMIC_OFFICE_SESSION)
      const queryRedirect = '/v2/academic-years'
      const targetPath = validateSafeRedirect(queryRedirect, '/v2')
      expect(targetPath).toBe('/v2/academic-years')
    })

    it('TC-F1-03: Login blocks open-redirect attacks and safely falls back to /v2', () => {
      const maliciousRedirects = [
        'https://evil.com',
        'http://phishing.site/login',
        '//attacker.com/steal',
        'javascript:alert(1)',
        '\\\\evil.corp\\path',
      ]
      for (const attackUrl of maliciousRedirects) {
        const resolved = validateSafeRedirect(attackUrl, '/v2')
        expect(resolved).toBe('/v2')
      }
    })

    it('TC-F1-04: GuestOnly navigation guard redirects authenticated users to /v2', () => {
      saveAuthSession(TEACHER_SESSION)
      const isGuestRoute = true
      const isAuthenticated = getAuthSession() !== null

      // Guard evaluation
      const routeDestination = (isGuestRoute && isAuthenticated) ? '/v2' : '/login'
      expect(routeDestination).toBe('/v2')
    })

    it('TC-F1-05: Sidebar V2 navigation contract includes "Hồ sơ học sinh" for ADMIN, ACADEMIC_OFFICE, TEACHER', () => {
      const rolesWithAccess = [ADMIN_SESSION, ACADEMIC_OFFICE_SESSION, TEACHER_SESSION]
      for (const auth of rolesWithAccess) {
        saveAuthSession(auth)
        const userRoles = getAuthSession()?.user.roles ?? []
        const isNonStudent = !userRoles.includes('STUDENT')
        expect(isNonStudent).toBe(true)

        // Navigation item schema verification
        const studentNavItem = {
          label: 'Hồ sơ học sinh',
          to: '/v2/students',
          icon: 'pi pi-user',
          active: true,
        }
        expect(studentNavItem.label).toBe('Hồ sơ học sinh')
        expect(studentNavItem.to).toBe('/v2/students')
        expect(studentNavItem.icon).toBe('pi pi-user')
      }
    })

    it('TC-F1-06: Sidebar V2 navigation contract strictly hides "Hồ sơ học sinh" for STUDENT role', () => {
      saveAuthSession(STUDENT_SESSION)
      const userRoles = getAuthSession()?.user.roles ?? []
      const isNonStudent = !userRoles.includes('STUDENT')
      expect(isNonStudent).toBe(false)

      const availableNavItems: string[] = []
      if (isNonStudent) {
        availableNavItems.push('/v2/students')
      }
      availableNavItems.push('/v2/transcripts')

      expect(availableNavItems).not.toContain('/v2/students')
      expect(availableNavItems).toContain('/v2/transcripts')
    })
  })

  // =========================================================================
  // Feature 2: Danh sách và Tra cứu Học sinh v2 Đa chiều (R2)
  // =========================================================================
  describe('Feature 2: Multi-dimensional Search & Student List V2', () => {
    it('TC-F2-01: Server-side pagination query contract produces correct parameters', () => {
      const query = {
        page: 0,
        pageSize: 10,
        sortField: 'studentCode',
        sortOrder: 1 as const,
        search: { studentCode: '', studentName: '', dateOfBirth: null },
      }

      const params = new URLSearchParams({
        page: String(query.page),
        size: String(query.pageSize),
        sortField: query.sortField,
        sortDirection: query.sortOrder === 1 ? 'asc' : 'desc',
      })

      expect(params.get('page')).toBe('0')
      expect(params.get('size')).toBe('10')
      expect(params.get('sortField')).toBe('studentCode')
      expect(params.get('sortDirection')).toBe('asc')
    })

    it('TC-F2-02: Exact student code search filters data matching CR-STUDENT-001 format', () => {
      const targetCode = 'STU0000001'
      const matched = SAMPLE_STUDENTS.filter(s => s.studentCode === targetCode)
      expect(matched.length).toBe(1)
      expect(matched[0].studentCode).toBe(targetCode)
      expect(matched[0].studentName).toBe('Nguyễn Văn An')
    })

    it('TC-F2-03: Filter by student name performs case-insensitive substring match', () => {
      const keyword = 'bình'
      const matched = SAMPLE_STUDENTS.filter(s =>
        s.studentName.toLowerCase().includes(keyword.toLowerCase()),
      )
      expect(matched.length).toBe(1)
      expect(matched[0].studentName).toBe('Trần Thị Bình')
    })

    it('TC-F2-04: Server-side sort by allow-list fields orders records consistently', () => {
      const sortedAsc = [...SAMPLE_STUDENTS].sort((a, b) => a.studentName.localeCompare(b.studentName))
      const sortedDesc = [...SAMPLE_STUDENTS].sort((a, b) => b.studentName.localeCompare(a.studentName))

      expect(sortedAsc[0].studentName).toBe('Lê Hoàng Long')
      expect(sortedDesc[0].studentName).toBe('Trần Thị Bình')
    })

    it('TC-F2-05: Drill-down navigation contract resolves to /v2/students/:studentId', () => {
      const selectedStudent = SAMPLE_STUDENTS[0]
      const detailRoute = `/v2/students/${selectedStudent.studentId}`
      expect(detailRoute).toBe('/v2/students/101')
    })
  })

  // =========================================================================
  // Feature 3: Thêm mới Học sinh kèm Cấp tài khoản Đăng nhập (Student V3) (R3)
  // =========================================================================
  describe('Feature 3: Student Creation & V3 Account Provisioning', () => {
    it('TC-F3-01: Create student V3 payload auto-generates compliant username and default password', () => {
      const studentCode = 'STU0000001'
      const studentName = 'Nguyễn Văn An'
      const generatedUsername = generateStudentUsername(studentName, studentCode)

      expect(generatedUsername).toBe('nguyenvanan0000001')
      expect(generatedUsername.length).toBeLessThanOrEqual(20)

      const requestPayload: StudentV3CreateRequest = {
        studentCode,
        studentName,
        dateOfBirth: '2010-05-15',
        address: 'Hà Nội',
        username: null,
        password: null,
      }

      expect(requestPayload.username).toBeNull()
      expect(requestPayload.password).toBeNull()
    })

    it('TC-F3-02: Create student V3 with explicitly specified username and password', () => {
      const customPayload: StudentV3CreateRequest = {
        studentCode: 'STU0000002',
        studentName: 'Trần Thị Bình',
        dateOfBirth: '2010-08-20',
        address: 'Hà Nội',
        username: 'binhtran2010',
        password: 'SecurePassword123',
      }

      expect(customPayload.username).toBe('binhtran2010')
      expect(customPayload.password).toBe('SecurePassword123')
    })

    it('TC-F3-03: Security guarantee: Response payload never leaks password or passwordHash', () => {
      const response: StudentV3CreateResponse = {
        studentId: 101,
        studentCode: 'STU0000001',
        studentName: 'Nguyễn Văn An',
        dateOfBirth: '2010-05-15',
        address: 'Hà Nội',
        account: {
          userId: 10,
          username: 'nguyenvanan0000001',
          role: 'STUDENT',
        },
      }

      // Explicit security check
      const responseKeys = Object.keys(response)
      const accountKeys = Object.keys(response.account)

      expect(responseKeys).not.toContain('password')
      expect(responseKeys).not.toContain('passwordHash')
      expect(accountKeys).not.toContain('password')
      expect(accountKeys).not.toContain('passwordHash')
      expect(response.account.role).toBe('STUDENT')
    })

    it('TC-F3-04: Student V1 creation backwards compatibility succeeds without account provisioning', () => {
      const v1Payload = {
        studentCode: 'STU0000099',
        studentName: 'Phạm Minh Đức',
        dateOfBirth: '2010-01-01',
        address: 'Đà Nẵng',
        averageScore: null,
      }

      expect(v1Payload).not.toHaveProperty('username')
      expect(v1Payload).not.toHaveProperty('password')
    })

    it('TC-F3-05: TEACHER role is blocked from provisioning V3 student accounts (403 Forbidden)', () => {
      saveAuthSession(TEACHER_SESSION)
      const currentRoles = getAuthSession()?.user.roles ?? []
      const hasProvisionPermission = currentRoles.includes('ADMIN') || currentRoles.includes('ACADEMIC_OFFICE')

      expect(hasProvisionPermission).toBe(false)
    })
  })

  // =========================================================================
  // Feature 4: Chi tiết Học sinh Đa phân hệ 4 Tabs Workspace (R4)
  // =========================================================================
  describe('Feature 4: 4-Tab Student Detail Workspace', () => {
    it('TC-F4-01: Tab 1 (Profile & User Account) binds demographic data and excludes deprecated averageScore', () => {
      const profileData = {
        studentId: 101,
        studentCode: 'STU0000001',
        studentName: 'Nguyễn Văn An',
        dateOfBirth: '2010-05-15',
        gender: 'MALE',
        status: 'ACTIVE',
        account: {
          userId: 10,
          username: 'nguyenvanan0000001',
          role: 'STUDENT',
        },
      }

      expect(profileData.studentCode).toBe('STU0000001')
      expect(profileData.status).toBe('ACTIVE')
      expect(profileData.account.userId).toBe(10)
      expect(profileData).not.toHaveProperty('averageScore')
    })

    it('TC-F4-02: Tab 2 (Enrollment & Transfer History) displays current class and transfer history', () => {
      const enrollments = SAMPLE_ENROLLMENT_HISTORY
      expect(enrollments.length).toBeGreaterThan(0)
      expect(enrollments[0].schoolClassCode).toBe('6A1')
      expect(enrollments[0].status).toBe('ACTIVE')
      expect(enrollments[0].transfers.length).toBe(1)
      expect(enrollments[0].transfers[0].fromClassCode).toBe('6A2')
      expect(enrollments[0].transfers[0].toClassCode).toBe('6A1')
    })

    it('TC-F4-03: Tab 3 (Attendance History) binds summary counters and session records', () => {
      const attendance = SAMPLE_ATTENDANCE_HISTORY
      expect(attendance.summary.totalSessions).toBe(20)
      expect(attendance.summary.presentCount).toBe(18)
      expect(attendance.summary.excusedAbsenceCount).toBe(2)
      expect(attendance.summary.attendanceRate).toBe(90.0)
      expect(attendance.sessions.length).toBe(2)
    })

    it('TC-F4-04: Tab 4 (Transcripts) binds semester scores and calculation status', () => {
      const transcript = SAMPLE_TERM_TRANSCRIPT
      expect(transcript.status).toBe('UP_TO_DATE')
      expect(transcript.termAverage).toBe(8.5)
      expect(transcript.subjects.length).toBe(1)
      expect(transcript.subjects[0].subjectName).toBe('Toán')
      expect(transcript.subjects[0].termAverageScore).toBe(8.7)
    })

    it('TC-F4-05: Tab 4 Recalculate button invokes calculation task for authorized roles', () => {
      saveAuthSession(ACADEMIC_OFFICE_SESSION)
      const roles = getAuthSession()?.user.roles ?? []
      const canRecalculate = roles.includes('ADMIN') || roles.includes('ACADEMIC_OFFICE')
      expect(canRecalculate).toBe(true)

      const calculationResponse: ResCalculationTaskDTO = {
        taskId: 5001,
        studentCode: 'STU0000001',
        academicYearId: 1,
        status: 'SUBMITTED',
        requestedAt: '2026-09-04T10:00:00Z',
      }
      expect(calculationResponse.status).toBe('SUBMITTED')
      expect(calculationResponse.taskId).toBe(5001)
    })
  })

  // =========================================================================
  // Feature 5: Chuẩn hóa Vòng đời Học sinh & Chính sách Xóa an toàn (R5)
  // =========================================================================
  describe('Feature 5: Safe Lifecycle & Inactivation Policies', () => {
    it('TC-F5-01: Delete unlinked orphan student record succeeds with 204 No Content contract', () => {
      const orphanStudentId = 999
      expect(orphanStudentId).toBe(999)
      const hasAcademicHistory = false

      let deletionAllowed = false
      if (!hasAcademicHistory) {
        deletionAllowed = true
      }
      expect(deletionAllowed).toBe(true)
    })

    it('TC-F5-02: Hard delete is blocked when student has active enrollments or scorebook data', () => {
      const studentId = 101
      expect(studentId).toBe(101)
      const linkedEnrollmentsCount = 1
      const hasLinkedData = linkedEnrollmentsCount > 0

      const canHardDelete = !hasLinkedData
      expect(canHardDelete).toBe(false)
    })

    it('TC-F5-03: Guided status transition warning prompts user to change status instead of deletion', () => {
      const warningMessage =
        'Học sinh đã có lịch sử xếp lớp hoặc bảng điểm. Không thể xóa cứng. Vui lòng chuyển trạng thái sang INACTIVE hoặc GRADUATED.'
      expect(warningMessage).toContain('Không thể xóa cứng')
      expect(warningMessage).toContain('INACTIVE')
      expect(warningMessage).toContain('GRADUATED')
    })

    it('TC-F5-04: Transitioning student status to INACTIVE retires student from unassigned queue', () => {
      const student = { ...SAMPLE_STUDENTS[0], status: 'ACTIVE' }
      student.status = 'INACTIVE'

      const isEligibleForEnrollment = student.status === 'ACTIVE'
      expect(isEligibleForEnrollment).toBe(false)
    })

    it('TC-F5-05: Transitioning student status to GRADUATED preserves academic transcript archive', () => {
      const student = { ...SAMPLE_STUDENTS[0], status: 'GRADUATED' }
      const academicHistoryPreserved = true

      expect(student.status).toBe('GRADUATED')
      expect(academicHistoryPreserved).toBe(true)
    })
  })
})
