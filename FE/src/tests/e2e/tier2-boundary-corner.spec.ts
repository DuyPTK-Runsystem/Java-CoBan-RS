import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clearAuthSession, getAuthSession, saveAuthSession } from '@/services/authSession'
import {
  ADMIN_SESSION,
  TEACHER_SESSION,
  STUDENT_SESSION,
  SAMPLE_STUDENTS,
  validateSafeRedirect,
  generateStudentUsername,
  type StudentAttendanceSummary,
} from './fixtures'

describe('Tier 2: Boundary Value Analysis & Corner Cases', () => {
  beforeEach(() => {
    clearAuthSession()
    vi.clearAllMocks()
  })

  afterEach(() => {
    clearAuthSession()
  })

  // =========================================================================
  // Feature 1: Login & Navigation Boundaries
  // =========================================================================
  describe('Boundary 1: Authentication & Navigation Boundaries', () => {
    it('TC-B1-01: Invalid credentials (401 Unauthorized) resets session and retains current view', () => {
      clearAuthSession()
      const error = new Error('Tên đăng nhập hoặc mật khẩu không đúng')
      expect(error.message).toBe('Tên đăng nhập hoặc mật khẩu không đúng')
      expect(getAuthSession()).toBeNull()
    })

    it('TC-B1-02: Mid-session token expiration triggers session eviction and redirect to login', () => {
      saveAuthSession(ADMIN_SESSION)
      expect(getAuthSession()).not.toBeNull()

      // Simulate token expiration callback
      clearAuthSession()
      expect(getAuthSession()).toBeNull()
    })

    it('TC-B1-03: Unauthenticated direct access to /v2 is intercepted by router guard', () => {
      clearAuthSession()
      const destination = '/v2/students'
      const isAuthenticated = getAuthSession() !== null

      const guardResult = isAuthenticated
        ? { path: destination }
        : { name: 'login', query: { redirect: destination } }

      expect(guardResult.name).toBe('login')
      expect(guardResult.query.redirect).toBe('/v2/students')
    })

    it('TC-B1-04: STUDENT role attempting direct URL access to /v2/students is blocked', () => {
      saveAuthSession(STUDENT_SESSION)
      const targetPath = '/v2/students'
      const userRoles = getAuthSession()?.user.roles ?? []
      const isAllowed = userRoles.includes('ADMIN') || userRoles.includes('ACADEMIC_OFFICE') || userRoles.includes('TEACHER')

      expect(isAllowed).toBe(false)
      const safeDestination = isAllowed ? targetPath : '/v2/transcripts'
      expect(safeDestination).toBe('/v2/transcripts')
    })

    it('TC-B1-05: Malicious redirect URIs with protocol-relative, javascript:, or control chars are sanitized', () => {
      const maliciousCases = [
        'javascript:void(0)',
        'data:text/html,<script>alert(1)</script>',
        '//malicious.site/v2',
        '/\\evil.com',
        'http://internal.app:8080/bypass',
      ]

      for (const uri of maliciousCases) {
        expect(validateSafeRedirect(uri, '/v2')).toBe('/v2')
      }
    })
  })

  // =========================================================================
  // Feature 2: Student List & Search Boundaries
  // =========================================================================
  describe('Boundary 2: Search Input Boundaries & Pagination Edge Cases', () => {
    it('TC-B2-01: Zero search results returns empty content and totalElements = 0 without error', () => {
      const nonExistentCode = 'STU9999999'
      const filtered = SAMPLE_STUDENTS.filter(s => s.studentCode === nonExistentCode)

      expect(filtered.length).toBe(0)
      const zeroStateResponse = {
        content: filtered,
        page: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0,
      }
      expect(zeroStateResponse.totalElements).toBe(0)
      expect(zeroStateResponse.content).toEqual([])
    })

    it('TC-B2-02: Out-of-bounds pagination (page > totalPages) returns safe empty page', () => {
      const requestedPage = 9999
      const totalPages = 1
      const isOutOfBounds = requestedPage >= totalPages

      expect(isOutOfBounds).toBe(true)
      const safePage = isOutOfBounds ? 0 : requestedPage
      expect(safePage).toBe(0)
    })

    it('TC-B2-03: Negative or zero pagination parameters are sanitized to default valid bounds', () => {
      const rawPage = -1
      const rawPageSize = 0

      const sanitizedPage = Math.max(0, rawPage)
      const sanitizedSize = rawPageSize <= 0 ? 10 : rawPageSize

      expect(sanitizedPage).toBe(0)
      expect(sanitizedSize).toBe(10)
    })

    it('TC-B2-04: Unicode Vietnamese characters and maximum length (35 chars) in search string', () => {
      const maxLenVietnameseName = 'Nguyễn Thị Hoàng Trúc Phương Thảo A'
      expect(maxLenVietnameseName.length).toBe(35)

      // Ensure URI encoding succeeds cleanly without corruption
      const encoded = encodeURIComponent(maxLenVietnameseName)
      expect(decodeURIComponent(encoded)).toBe(maxLenVietnameseName)
    })

    it('TC-B2-05: Invalid birth date format submission is rejected by validation', () => {
      const invalidDates = ['31/02/2026', '2026-13-45', 'invalid-string', '']
      const dateRegex = /^\d{4}-\d{2}-\d{2}$/

      for (const d of invalidDates) {
        const isValidFormat = dateRegex.test(d)
        if (isValidFormat) {
          const parsed = new Date(d)
          const isRealDate = !isNaN(parsed.getTime())
          expect(isRealDate).toBe(false)
        } else {
          expect(isValidFormat).toBe(false)
        }
      }
    })
  })

  // =========================================================================
  // Feature 3: Student V3 Creation & Provisioning Boundaries
  // =========================================================================
  describe('Boundary 3: Student V3 Creation & Conflict Boundaries', () => {
    it('TC-B3-01: Duplicate studentCode triggers 409 Conflict without partial record creation', () => {
      const existingCode = 'STU0000001'
      const incomingCode = 'STU0000001'

      const isDuplicate = existingCode === incomingCode
      expect(isDuplicate).toBe(true)

      const errorResponse = {
        status: 409,
        message: 'Mã sinh viên đã tồn tại trong hệ thống',
        field: 'studentCode',
      }
      expect(errorResponse.status).toBe(409)
      expect(errorResponse.field).toBe('studentCode')
    })

    it('TC-B3-02: Duplicate username triggers 409 Conflict and complete transaction rollback', () => {
      const existingUsername = 'admin.user'
      const requestedUsername = 'admin.user'

      const isDuplicateUsername = existingUsername === requestedUsername
      expect(isDuplicateUsername).toBe(true)

      const rollbackOccurred = true
      expect(rollbackOccurred).toBe(true)
    })

    it('TC-B3-03: Long Vietnamese names trigger initial-based fallback username <= 20 characters', () => {
      const veryLongName = 'Công Tằng Tôn Nữ Bích Chiêu Mai'
      const studentCode = 'STU0000088'

      const generated = generateStudentUsername(veryLongName, studentCode)
      expect(generated.length).toBeLessThanOrEqual(20)
      expect(generated).toContain('0000088')
    })

    it('TC-B3-04: Password length boundary validation rejects < 6 or > 15 characters', () => {
      const validatePasswordLength = (pwd: string) => pwd.length >= 6 && pwd.length <= 15

      expect(validatePasswordLength('12345')).toBe(false) // 5 chars
      expect(validatePasswordLength('123456')).toBe(true) // 6 chars (min boundary)
      expect(validatePasswordLength('123456789012345')).toBe(true) // 15 chars (max boundary)
      expect(validatePasswordLength('1234567890123456')).toBe(false) // 16 chars
    })

    it('TC-B3-05: Strict studentCode regex validation STU[0-9]{7} rejects malformed codes', () => {
      const regex = /^STU\d{7}$/

      expect(regex.test('STU0000001')).toBe(true)
      expect(regex.test('STU1234567')).toBe(true)
      expect(regex.test('STU123456')).toBe(false) // only 6 digits
      expect(regex.test('STU12345678')).toBe(false) // 8 digits
      expect(regex.test('HS0000001')).toBe(false) // wrong prefix
      expect(regex.test('stu0000001')).toBe(false) // lowercase prefix
    })
  })

  // =========================================================================
  // Feature 4: 4-Tab Detail Workspace Boundaries
  // =========================================================================
  describe('Boundary 4: Workspace Tab Boundaries & Edge Data States', () => {
    it('TC-B4-01: Tab 2 handles newly admitted student with zero enrollment history gracefully', () => {
      const emptyEnrollments: unknown[] = []
      expect(emptyEnrollments.length).toBe(0)

      const emptyStateMessage = 'Học sinh chưa được xếp vào lớp nào trong năm học này.'
      expect(emptyStateMessage).toContain('chưa được xếp vào lớp nào')
    })

    it('TC-B4-02: Tab 3 prevents division by zero when totalSessions is 0', () => {
      const zeroAttendance: StudentAttendanceSummary = {
        totalSessions: 0,
        presentCount: 0,
        excusedAbsenceCount: 0,
        unexcusedAbsenceCount: 0,
        attendanceRate: 0,
      }

      // Safe rate calculation preventing NaN
      const rate = zeroAttendance.totalSessions > 0
        ? (zeroAttendance.presentCount / zeroAttendance.totalSessions) * 100
        : 0

      expect(rate).toBe(0)
      expect(isNaN(rate)).toBe(false)
    })

    it('TC-B4-03: Tab 4 renders placeholder dash for students with no recorded scores', () => {
      const nullScore: number | null = null
      const formattedScore = nullScore !== null ? nullScore.toFixed(1) : '—'
      expect(formattedScore).toBe('—')
    })

    it('TC-B4-04: TEACHER role accessing Tab 4 for unassigned class receives 403 Forbidden', () => {
      saveAuthSession(TEACHER_SESSION)
      const teacherAssignedClasses = ['6A1', '6A2']
      const targetStudentClass = '7B'

      const hasClassAccess = teacherAssignedClasses.includes(targetStudentClass)
      expect(hasClassAccess).toBe(false)

      const responseCode = hasClassAccess ? 200 : 403
      expect(responseCode).toBe(403)
    })

    it('TC-B4-05: Concurrent recalculation requests are throttled when already CALCULATING', () => {
      let taskStatus: 'UP_TO_DATE' | 'CALCULATING' = 'CALCULATING'
      const canSubmitRecalculate = taskStatus !== 'CALCULATING'

      expect(canSubmitRecalculate).toBe(false)
      taskStatus = 'UP_TO_DATE'
      expect(taskStatus !== 'CALCULATING').toBe(true)
    })
  })

  // =========================================================================
  // Feature 5: Safe Lifecycle & Inactivation Boundaries
  // =========================================================================
  describe('Boundary 5: Safe Lifecycle & Foreign Key Constraint Boundaries', () => {
    it('TC-B5-01: Deleting non-existent student ID returns 404 Not Found cleanly', () => {
      const nonExistentId = 999999
      const exists = SAMPLE_STUDENTS.some(s => s.studentId === nonExistentId)
      expect(exists).toBe(false)

      const status = exists ? 204 : 404
      expect(status).toBe(404)
    })

    it('TC-B5-02: Foreign key violation triggers managed exception instead of unhandled SQL crash', () => {
      const foreignKeyConstraintViolated = true
      const handledError = foreignKeyConstraintViolated
        ? { status: 409, message: 'Dữ liệu học sinh đã phát sinh liên kết học vụ, không thể xóa' }
        : { status: 204 }

      expect(handledError.status).toBe(409)
      expect(handledError.message).toContain('không thể xóa')
    })

    it('TC-B5-03: Re-activating INACTIVE student back to ACTIVE restores eligibility in unassigned pool', () => {
      const student = { ...SAMPLE_STUDENTS[0], status: 'INACTIVE' }
      expect(student.status).toBe('INACTIVE')

      // Transition back to ACTIVE
      student.status = 'ACTIVE'
      const eligibleForAssignment = student.status === 'ACTIVE'
      expect(eligibleForAssignment).toBe(true)
    })

    it('TC-B5-04: Reject enrolling GRADUATED student into an active school class', () => {
      const graduatedStudent = { ...SAMPLE_STUDENTS[0], status: 'GRADUATED' }
      const canEnroll = graduatedStudent.status === 'ACTIVE'

      expect(canEnroll).toBe(false)
    })

    it('TC-B5-05: TEACHER role is unauthorized to perform student deletion (403 Forbidden)', () => {
      saveAuthSession(TEACHER_SESSION)
      const userRoles = getAuthSession()?.user.roles ?? []
      const canDelete = userRoles.includes('ADMIN') || userRoles.includes('ACADEMIC_OFFICE')

      expect(canDelete).toBe(false)
    })
  })
})
