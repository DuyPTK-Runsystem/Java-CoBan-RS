import { afterEach, describe, expect, it, vi } from 'vitest'

import {
  activateSemester,
  closeSchoolClass,
  closeAcademicYear,
  createClassSubject,
  createAcademicYear,
  createGrade,
  createSchoolClass,
  createSemester,
  createSubject,
  createSubjectApplicability,
  deactivateSubjectApplicability,
  deleteGrade,
  deleteSchoolClass,
  fetchClassSubjects,
  fetchAcademicYears,
  fetchGrades,
  fetchSchoolClasses,
  fetchSemesters,
  fetchSubjects,
  fetchSubjectApplicabilities,
  getSemesterCompletenessReport,
  fetchSemesterNotifications,
  dispatchSemesterNotifications,
  retryFailedSemesterNotifications,
  lockSemester,
  reopenSemester,
  updateClassSubject,
  updateGrade,
  updateSchoolClass,
  updateSubject,
  updateSubjectApplicability,
  updateAcademicYear,
  updateSemester,
} from './academicApi'

const fetchMock = vi.fn()

describe('academicApi', () => {
  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('fetches academic years with the bearer token', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ data: [] }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await fetchAcademicYears('token')

    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8081/api/v2/academic-years', expect.objectContaining({ headers: { Accept: 'application/json', Authorization: 'Bearer token' } }))
  })

  it('serializes academic year create and update requests without changing date-only values', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(new Response(JSON.stringify({ data: { id: 1 } }), { status: 200 })))
    vi.stubGlobal('fetch', fetchMock)
    const request = { code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE' as const, notes: null }

    await createAcademicYear('token', request)
    await updateAcademicYear('token', 1, request)

    expect(fetchMock.mock.calls[0][0]).toBe('http://localhost:8081/api/v2/academic-years')
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ method: 'POST', body: JSON.stringify(request) })
    expect(fetchMock.mock.calls[1][0]).toBe('http://localhost:8081/api/v2/academic-years/1')
    expect(fetchMock.mock.calls[1][1]).toMatchObject({ method: 'PUT', body: JSON.stringify(request) })
  })

  it('uses the academic year query for semester lists', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ data: [] }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await fetchSemesters('token', 7)

    expect(fetchMock.mock.calls[0][0]).toBe('http://localhost:8081/api/v2/semesters?academicYearId=7')
  })

  it('serializes semester metadata and lifecycle requests on their dedicated endpoints', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(new Response(JSON.stringify({ data: { id: 11 } }), { status: 200 })))
    vi.stubGlobal('fetch', fetchMock)
    const request = { academicYearId: 7, code: 'HK1', name: 'Học kỳ I', displayOrder: 1, startDate: '2026-09-01', endDate: '2026-12-31', automaticLockAt: '2027-01-15T17:00:00' }
    const updateRequest = { code: request.code, name: request.name, displayOrder: request.displayOrder, startDate: request.startDate, endDate: request.endDate, automaticLockAt: request.automaticLockAt }

    await createSemester('token', request)
    await updateSemester('token', 11, updateRequest)
    await activateSemester('token', 11)
    await lockSemester('token', 11)
    await reopenSemester('token', 11, { reason: 'Review completed' })

    expect(fetchMock.mock.calls[0][0]).toBe('http://localhost:8081/api/v2/semesters')
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ method: 'POST', body: JSON.stringify(request) })
    expect(fetchMock.mock.calls[1][0]).toBe('http://localhost:8081/api/v2/semesters/11')
    expect(fetchMock.mock.calls[1][1]).toMatchObject({ method: 'PUT', body: JSON.stringify(updateRequest) })
    expect(fetchMock.mock.calls[2][0]).toBe('http://localhost:8081/api/v2/semesters/11/activate')
    expect(fetchMock.mock.calls[3][0]).toBe('http://localhost:8081/api/v2/semesters/11/lock')
    expect(fetchMock.mock.calls[4][0]).toBe('http://localhost:8081/api/v2/semesters/11/reopen')
    expect(fetchMock.mock.calls[4][1]).toMatchObject({ method: 'POST', body: JSON.stringify({ reason: 'Review completed' }) })
  })

  it('keeps an optional completeness checkpoint explicit and trimmed', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(new Response(JSON.stringify({ data: { semesterId: 11 } }), { status: 200 })))
    vi.stubGlobal('fetch', fetchMock)

    await getSemesterCompletenessReport('token', 11, ' PRE_LOCK ')
    expect(fetchMock.mock.calls[0][0]).toBe('http://localhost:8081/api/v2/semesters/11/completeness-report?checkpointCode=PRE_LOCK')

    await closeAcademicYear('token', 7)
    expect(fetchMock.mock.calls[1][0]).toBe('http://localhost:8081/api/v2/academic-years/7/close')
  })

  it('uses email notification endpoints and strips transport-only fields before returning UI data', async () => {
    const response = {
      id: 9, semesterId: 11, reportId: 101, checkpointCode: 'PRE_LOCK', recipientEmail: 'office@example.test', recipientRole: 'ACADEMIC_OFFICE',
      recipientTeacherId: null, notificationChannel: 'EMAIL', status: 'SENT', subject: 'Nhắc nhập điểm', bodyContent: 'Nội dung', attemptCount: 1,
      sentAt: '2027-01-15T10:00:00', errorMessage: null, createdAt: '2027-01-15T09:00:00', updatedAt: '2027-01-15T10:00:00',
    }
    fetchMock.mockImplementation(() => Promise.resolve(new Response(JSON.stringify({ data: [response] }), { status: 200 })))
    vi.stubGlobal('fetch', fetchMock)

    const notifications = await fetchSemesterNotifications('token', 11)
    await dispatchSemesterNotifications('token', 11)
    await retryFailedSemesterNotifications('token', 11)

    expect(notifications[0]).toEqual({
      id: 9, semesterId: 11, recipientEmail: 'office@example.test', recipientRole: 'ACADEMIC_OFFICE', status: 'SENT', subject: 'Nhắc nhập điểm',
      attemptCount: 1, sentAt: '2027-01-15T10:00:00', errorMessage: null, createdAt: '2027-01-15T09:00:00', updatedAt: '2027-01-15T10:00:00',
    })
    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      'http://localhost:8081/api/v2/semesters/11/notifications',
      'http://localhost:8081/api/v2/semesters/11/notifications/dispatch',
      'http://localhost:8081/api/v2/semesters/11/notifications/retry-failed',
    ])
  })

  it('uses the catalog lifecycle endpoints with typed request bodies', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(new Response(JSON.stringify({ data: { id: 1 } }), { status: 200 })))
    vi.stubGlobal('fetch', fetchMock)

    const gradeRequest = { code: 'G6', name: 'Khối 6', gradeLevel: 6 as const, displayOrder: 1, nextGradeId: null, active: true, description: null }
    const classCreateRequest = { academicYearId: 7, gradeLevelId: 1, classCode: '6A1', className: 'Lớp 6A1', capacity: 40, status: 'PLANNED' as const }
    const classUpdateRequest = { gradeLevelId: 1, classCode: '6A1', className: null, capacity: 42, status: 'ACTIVE' as const }
    const subjectRequest = { code: 'MAT', name: 'Toán', subjectType: 'ACADEMIC' as const, applicationScope: 'GRADE' as const, status: 'ACTIVE' as const }
    const applicabilityRequest = { semesterId: 11, scopeType: 'GRADE' as const, gradeLevelId: 1, classId: null }
    const applicabilityUpdateRequest = { ...applicabilityRequest, status: 'ACTIVE' as const }
    const classSubjectCreateRequest = { classId: 21, subjectId: 31, semesterId: 11, status: 'ACTIVE' as const }

    await fetchGrades('token')
    await createGrade('token', gradeRequest)
    await updateGrade('token', 1, gradeRequest)
    await deleteGrade('token', 1)
    await fetchSchoolClasses('token', 7)
    await createSchoolClass('token', classCreateRequest)
    await updateSchoolClass('token', 21, classUpdateRequest)
    await closeSchoolClass('token', 21)
    await deleteSchoolClass('token', 21)
    await fetchSubjects('token', 'ACTIVE')
    await createSubject('token', subjectRequest)
    await updateSubject('token', 31, subjectRequest)
    await createSubjectApplicability('token', 31, applicabilityRequest)
    await fetchSubjectApplicabilities('token', 31, 11, 'ACTIVE')
    await updateSubjectApplicability('token', 31, 51, applicabilityUpdateRequest)
    await deactivateSubjectApplicability('token', 31, 51)
    await fetchClassSubjects('token', 21, 11)
    await createClassSubject('token', classSubjectCreateRequest)
    await updateClassSubject('token', 41, { status: 'INACTIVE' })

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      'http://localhost:8081/api/v2/grades',
      'http://localhost:8081/api/v2/grades',
      'http://localhost:8081/api/v2/grades/1',
      'http://localhost:8081/api/v2/grades/1',
      'http://localhost:8081/api/v2/classes?academicYearId=7',
      'http://localhost:8081/api/v2/classes',
      'http://localhost:8081/api/v2/classes/21',
      'http://localhost:8081/api/v2/classes/21/close',
      'http://localhost:8081/api/v2/classes/21',
      'http://localhost:8081/api/v2/subjects?status=ACTIVE',
      'http://localhost:8081/api/v2/subjects',
      'http://localhost:8081/api/v2/subjects/31',
      'http://localhost:8081/api/v2/subjects/31/applicabilities',
      'http://localhost:8081/api/v2/subjects/31/applicabilities?semesterId=11&status=ACTIVE',
      'http://localhost:8081/api/v2/subjects/31/applicabilities/51',
      'http://localhost:8081/api/v2/subjects/31/applicabilities/51',
      'http://localhost:8081/api/v2/classes/21/subjects?semesterId=11',
      'http://localhost:8081/api/v2/class-subjects',
      'http://localhost:8081/api/v2/class-subjects/41',
    ])
    expect(fetchMock.mock.calls[1][1]).toMatchObject({ method: 'POST', body: JSON.stringify(gradeRequest) })
    expect(fetchMock.mock.calls[6][1]).toMatchObject({ method: 'PUT', body: JSON.stringify(classUpdateRequest) })
    expect(fetchMock.mock.calls[12][1]).toMatchObject({ method: 'POST', body: JSON.stringify(applicabilityRequest) })
    expect(fetchMock.mock.calls[13][1]).toMatchObject({ method: 'GET' })
    expect(fetchMock.mock.calls[14][1]).toMatchObject({ method: 'PUT', body: JSON.stringify(applicabilityUpdateRequest) })
    expect(fetchMock.mock.calls[15][1]).toMatchObject({ method: 'DELETE' })
    expect(fetchMock.mock.calls[18][1]).toMatchObject({ method: 'PUT', body: JSON.stringify({ status: 'INACTIVE' }) })
  })
})
