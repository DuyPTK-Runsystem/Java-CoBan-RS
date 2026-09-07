import { afterEach, describe, expect, it, vi } from 'vitest'

import {
  fetchAttendanceSession,
  deleteAttendanceException,
  fetchAttendanceCalendar,
  fetchAttendanceSessionStudents,
  fetchClassAttendanceSummary,
  fetchStudentAttendanceHistory,
  fetchStudentAttendanceHistoryById,
  upsertAttendanceException,
} from './attendanceApi'

const fetchMock = vi.fn()

describe('attendanceApi', () => {
  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('serializes calendar preflight and teacher session requests', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(new Response(JSON.stringify({ data: [] }), { status: 200 })))
    vi.stubGlobal('fetch', fetchMock)

    await fetchAttendanceCalendar('token', { academicYearId: 1, semesterId: 2, from: '2026-09-01', to: '2026-09-01' })
    await fetchAttendanceSession('token', { classId: 3, semesterId: 2, attendanceDate: '2026-09-01', sessionPeriod: 'MORNING' })
    await fetchAttendanceSessionStudents('token', 5)
    await upsertAttendanceException('token', 5, 7, { status: 'EXCUSED', note: 'Ốm' })
    await deleteAttendanceException('token', 5, 7)

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      'http://localhost:8081/api/v2/calendar/days?academicYearId=1&semesterId=2&from=2026-09-01&to=2026-09-01',
      'http://localhost:8081/api/v2/attendance-sessions?classId=3&semesterId=2&attendanceDate=2026-09-01&sessionPeriod=MORNING',
      'http://localhost:8081/api/v2/attendance-sessions/5/students',
      'http://localhost:8081/api/v2/attendance-sessions/5/exceptions/7',
      'http://localhost:8081/api/v2/attendance-sessions/5/exceptions/7',
    ])
    expect(fetchMock.mock.calls[1][1]).toMatchObject({ method: 'GET' })
    expect(fetchMock.mock.calls[3][1]).toMatchObject({ method: 'PUT', body: JSON.stringify({ status: 'EXCUSED', note: 'Ốm' }) })
    expect(fetchMock.mock.calls[4][1]).toMatchObject({ method: 'DELETE' })
  })

  it('uses the office base only when explicitly requested', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(new Response(JSON.stringify({ data: [] }), { status: 200 })))
    vi.stubGlobal('fetch', fetchMock)

    await fetchAttendanceSession('token', { classId: 3, semesterId: 2, attendanceDate: '2026-09-01', sessionPeriod: 'AFTERNOON' }, 'office')
    await fetchAttendanceSessionStudents('token', 5, 'office')

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      'http://localhost:8081/api/v2/office/attendance-sessions?classId=3&semesterId=2&attendanceDate=2026-09-01&sessionPeriod=AFTERNOON',
      'http://localhost:8081/api/v2/office/attendance-sessions/5/students',
    ])
  })

  it('serializes optional history filters and class summary pagination', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(new Response(JSON.stringify({ data: { items: [], students: [] } }), { status: 200 })))
    vi.stubGlobal('fetch', fetchMock)

    await fetchStudentAttendanceHistory('token', { academicYearId: 1, semesterId: 2, from: '2026-09-01', to: '2026-09-30', page: 1, size: 20 })
    await fetchClassAttendanceSummary('token', 3, { semesterId: 2, from: '2026-09-01', to: '2026-09-30', page: 2, size: 50 })

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      'http://localhost:8081/api/v2/attendance/students/me/history?page=1&size=20&academicYearId=1&semesterId=2&from=2026-09-01&to=2026-09-30',
      'http://localhost:8081/api/v2/attendance/classes/3/summary?semesterId=2&from=2026-09-01&to=2026-09-30&page=2&size=50',
    ])
  })

  it('serializes student attendance history by id request', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(new Response(JSON.stringify({ data: { items: [], summary: {} } }), { status: 200 })))
    vi.stubGlobal('fetch', fetchMock)

    await fetchStudentAttendanceHistoryById('token', 101, { academicYearId: 1, semesterId: 2, page: 0, size: 1 })

    expect(fetchMock.mock.calls[0][0]).toBe(
      'http://localhost:8081/api/v2/attendance/students/101/history?page=0&size=1&academicYearId=1&semesterId=2',
    )
    expect(fetchMock.mock.calls[0][1]).toMatchObject({
      headers: {
        Authorization: 'Bearer token',
      },
    })
  })
})
