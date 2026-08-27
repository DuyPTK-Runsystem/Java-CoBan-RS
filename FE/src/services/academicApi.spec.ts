import { afterEach, describe, expect, it, vi } from 'vitest'

import {
  activateSemester,
  closeAcademicYear,
  createAcademicYear,
  createSemester,
  fetchAcademicYears,
  fetchSemesters,
  getSemesterCompletenessReport,
  lockSemester,
  reopenSemester,
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
})
