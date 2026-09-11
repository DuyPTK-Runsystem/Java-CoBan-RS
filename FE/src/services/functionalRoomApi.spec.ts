import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  createFunctionalRoom,
  deleteFunctionalRoom,
  getFunctionalRoom,
  listFunctionalRooms,
  lookupFunctionalRooms,
  updateFunctionalRoom,
} from './functionalRoomApi'

const fetchMock = vi.fn()

describe('functionalRoomApi', () => {
  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('handles CRUD and lookup endpoints correctly', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(new Response(JSON.stringify({ data: { id: 1 } }), { status: 200 })),
    )
    vi.stubGlobal('fetch', fetchMock)

    await listFunctionalRooms({ search: 'Lab', status: 'ACTIVE', page: 0, size: 10 }, 'test-token')
    await getFunctionalRoom(1, 'test-token')
    await createFunctionalRoom({ code: 'LAB_01', name: 'Phòng Tin 1' }, 'test-token')
    await updateFunctionalRoom(1, { code: 'LAB_01', name: 'Phòng Tin 1', status: 'ACTIVE', expectedVersion: 0 }, 'test-token')
    await deleteFunctionalRoom(1, 0, 'test-token')
    await lookupFunctionalRooms({ subjectId: 5, status: 'ACTIVE' }, 'test-token')

    const urls = fetchMock.mock.calls.map(([url]) => url)
    expect(urls[0]).toContain('/api/v2/functional-rooms?search=Lab&status=ACTIVE&page=0&size=10')
    expect(urls[1]).toContain('/api/v2/functional-rooms/1')
    expect(urls[2]).toContain('/api/v2/functional-rooms')
    expect(urls[3]).toContain('/api/v2/functional-rooms/1')
    expect(urls[4]).toContain('/api/v2/functional-rooms/1?expectedVersion=0')
    expect(urls[5]).toContain('/api/v2/functional-rooms/lookup?subjectId=5&status=ACTIVE')

    expect(fetchMock.mock.calls[2][1]).toMatchObject({
      method: 'POST',
      body: JSON.stringify({ code: 'LAB_01', name: 'Phòng Tin 1' }),
    })
    expect(fetchMock.mock.calls[4][1]).toMatchObject({ method: 'DELETE' })
  })
})

