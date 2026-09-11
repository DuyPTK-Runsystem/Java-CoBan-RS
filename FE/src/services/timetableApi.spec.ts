import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  createTimetable,
  createTimetableRevision,
  getMyTimetable,
  getTimetableDetail,
  getTimetableEntries,
  getTimetablePeriods,
  getTimetableReview,
  initTimetableCalendar,
  listTimetables,
  publishTimetableRevision,
  updateTimetableEntries,
  validateTimetableRevision,
} from './timetableApi'

const fetchMock = vi.fn()

describe('timetableApi', () => {
  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('handles timetable query, entries, validation, publishing with idempotency key', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(new Response(JSON.stringify({ data: { revisionId: 10 } }), { status: 200 })),
    )
    vi.stubGlobal('fetch', fetchMock)

    await listTimetables(1, 0, 10, 'token')
    await createTimetable({ semesterId: 1, effectiveFrom: '2026-09-01' }, 'token')
    await getTimetableDetail(10, 'token')
    await getTimetableEntries(10, { classId: 5, session: 'MORNING' }, 'token')
    await updateTimetableEntries(10, { expectedVersion: 0, upserts: [], deletedEntryIds: [] }, 'token')
    await validateTimetableRevision(10, 'token')
    await getTimetableReview(10, 'token')
    await publishTimetableRevision(10, { expectedVersion: 0, expectedHeadVersion: 0 }, 'idemp-key-1', 'token')
    await createTimetableRevision(10, { expectedVersion: 0, effectiveFrom: '2026-10-01' }, 'token')
    await getTimetablePeriods(1, 'token')
    await initTimetableCalendar(1, 'token')
    await getMyTimetable('token')

    const urls = fetchMock.mock.calls.map(([url]) => url)
    expect(urls[0]).toContain('/api/v2/timetables?semesterId=1&page=0&size=10')
    expect(urls[1]).toContain('/api/v2/timetables')
    expect(urls[2]).toContain('/api/v2/timetables/10')
    expect(urls[3]).toContain('/api/v2/timetables/revisions/10/entries?classId=5&session=MORNING')
    expect(urls[4]).toContain('/api/v2/timetables/revisions/10/entries')
    expect(urls[5]).toContain('/api/v2/timetables/revisions/10/validate')
    expect(urls[6]).toContain('/api/v2/timetables/revisions/10/review')
    expect(urls[7]).toContain('/api/v2/timetables/revisions/10/publish')
    expect(urls[8]).toContain('/api/v2/timetables/revisions/10/revisions')
    expect(urls[9]).toContain('/api/v2/timetables/periods?semesterId=1')
    expect(urls[10]).toContain('/api/v2/timetables/periods/init?semesterId=1')
    expect(urls[11]).toContain('/api/v2/my-timetable')

    // Check Idempotency-Key header on publish call
    const publishHeaders = fetchMock.mock.calls[7][1]?.headers
    expect(publishHeaders?.['Idempotency-Key']).toBe('idemp-key-1')
  })
})

