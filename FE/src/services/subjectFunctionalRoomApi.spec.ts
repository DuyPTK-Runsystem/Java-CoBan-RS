import { afterEach, describe, expect, it, vi } from 'vitest'
import { getRoomsForSubject, updateRoomsForSubject } from './subjectFunctionalRoomApi'

const fetchMock = vi.fn()

describe('subjectFunctionalRoomApi', () => {
  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('handles subject room mapping endpoints correctly', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(new Response(JSON.stringify({ data: { subjectId: 10, rooms: [] } }), { status: 200 })),
    )
    vi.stubGlobal('fetch', fetchMock)

    await getRoomsForSubject(10, 'test-token')
    await updateRoomsForSubject(10, { functionalRoomIds: [1, 2], expectedVersion: 0 }, 'test-token')

    const urls = fetchMock.mock.calls.map(([url]) => url)
    expect(urls[0]).toContain('/api/v2/subjects/10/functional-rooms')
    expect(urls[1]).toContain('/api/v2/subjects/10/functional-rooms')

    expect(fetchMock.mock.calls[1][1]).toMatchObject({
      method: 'PUT',
      body: JSON.stringify({ functionalRoomIds: [1, 2], expectedVersion: 0 }),
    })
  })
})

