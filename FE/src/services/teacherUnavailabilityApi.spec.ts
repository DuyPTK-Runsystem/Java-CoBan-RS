import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  approveTeacherUnavailability,
  createTeacherUnavailability,
  listTeacherUnavailabilities,
  rejectTeacherUnavailability,
  updateTeacherUnavailability,
  withdrawTeacherUnavailability,
} from './teacherUnavailabilityApi'

const fetchMock = vi.fn()

describe('teacherUnavailabilityApi', () => {
  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('handles list, create, update, approve, reject, withdraw endpoints correctly', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(new Response(JSON.stringify({ data: { id: 1 } }), { status: 200 })),
    )
    vi.stubGlobal('fetch', fetchMock)

    await listTeacherUnavailabilities({ semesterId: 1, teacherId: 100, status: 'PENDING' }, 'test-token')
    await createTeacherUnavailability(
      {
        semesterId: 1,
        teacherId: 100,
        dayOfWeek: 2,
        validFrom: '2026-09-01',
        validTo: '2026-12-31',
        session: 'MORNING',
        periodIndexes: '1,2',
      },
      'test-token',
    )
    await updateTeacherUnavailability(
      1,
      {
        expectedVersion: 0,
        dayOfWeek: 2,
        validFrom: '2026-09-01',
        validTo: '2026-12-31',
        session: 'MORNING',
        periodIndexes: '1,2,3',
      },
      'test-token',
    )
    await approveTeacherUnavailability(1, 0, 'test-token')
    await rejectTeacherUnavailability(1, { expectedVersion: 0, reason: 'Không phù hợp' }, 'test-token')
    await withdrawTeacherUnavailability(1, 0, 'test-token')

    const urls = fetchMock.mock.calls.map(([url]) => url)
    expect(urls[0]).toContain('/api/v2/teacher-unavailabilities?semesterId=1&teacherId=100&status=PENDING')
    expect(urls[1]).toContain('/api/v2/teacher-unavailabilities')
    expect(urls[2]).toContain('/api/v2/teacher-unavailabilities/1')
    expect(urls[3]).toContain('/api/v2/teacher-unavailabilities/1/approve?expectedVersion=0')
    expect(urls[4]).toContain('/api/v2/teacher-unavailabilities/1/reject')
    expect(urls[5]).toContain('/api/v2/teacher-unavailabilities/1/withdraw?expectedVersion=0')
  })
})

