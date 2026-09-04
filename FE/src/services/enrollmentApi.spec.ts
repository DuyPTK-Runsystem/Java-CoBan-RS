import { afterEach, describe, expect, it, vi } from 'vitest'

import {
  createBulkEnrollment,
  createEnrollment,
  fetchClassStudents,
  fetchStudentEnrollmentHistory,
  fetchStudentEnrollmentHistoryByCode,
  fetchUnassignedStudents,
  transferEnrollment,
} from './enrollmentApi'

const fetchMock = vi.fn()

describe('enrollmentApi', () => {
  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('loads unassigned students and class roster with scoped query/path', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(new Response(JSON.stringify({ data: [] }), { status: 200 })))
    vi.stubGlobal('fetch', fetchMock)

    await fetchUnassignedStudents('token', 7)
    await fetchClassStudents('token', 21)

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      'http://localhost:8081/api/v2/enrollments/unassigned?academicYearId=7',
      'http://localhost:8081/api/v2/classes/21/students',
    ])
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ headers: { Accept: 'application/json', Authorization: 'Bearer token' } })
  })

  it('serializes single, bulk and transfer mutations using current DTO shapes', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(new Response(JSON.stringify({ data: { enrollments: [], warnings: [] } }), { status: 200 })))
    vi.stubGlobal('fetch', fetchMock)
    const singleRequest = { studentId: 101, academicYearId: 7, classId: 21 }
    const bulkRequest = { academicYearId: 7, classId: 21, studentIds: [101, 102], studentCodes: null }
    const transferRequest = { targetClassId: 22, effectiveAt: '2026-08-28T09:30:00', reason: 'Điều chỉnh sĩ số' }

    await createEnrollment('token', singleRequest)
    await createBulkEnrollment('token', bulkRequest)
    await transferEnrollment('token', 501, transferRequest)

    expect(fetchMock.mock.calls[0][0]).toBe('http://localhost:8081/api/v2/enrollments')
    expect(fetchMock.mock.calls[0][1]).toMatchObject({ method: 'POST', body: JSON.stringify(singleRequest) })
    expect(fetchMock.mock.calls[1][0]).toBe('http://localhost:8081/api/v2/enrollments/bulk')
    expect(fetchMock.mock.calls[1][1]).toMatchObject({ method: 'POST', body: JSON.stringify(bulkRequest) })
    expect(fetchMock.mock.calls[2][0]).toBe('http://localhost:8081/api/v2/enrollments/501/transfer')
    expect(fetchMock.mock.calls[2][1]).toMatchObject({ method: 'POST', body: JSON.stringify(transferRequest) })
  })

  it('uses technical student id or encoded student code for history', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(new Response(JSON.stringify({ data: [] }), { status: 200 })))
    vi.stubGlobal('fetch', fetchMock)

    await fetchStudentEnrollmentHistory('token', 101)
    await fetchStudentEnrollmentHistoryByCode('token', 'STU/101 A')

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      'http://localhost:8081/api/v2/students/101/enrollments',
      'http://localhost:8081/api/v2/students/by-code/STU%2F101%20A/enrollments',
    ])
  })
})
