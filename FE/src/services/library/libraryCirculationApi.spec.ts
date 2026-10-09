import { afterEach, describe, expect, it, vi } from 'vitest'

import { markLibraryCopyLost } from './libraryCirculationApi'

const fetchMock = vi.fn()

describe('libraryCirculationApi lost contract', () => {
  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('returns the loan and LOST_ITEM fine from the lost endpoint', async () => {
    const response = {
      loan: { loanId: 22, status: 'LOST' },
      fine: { fineId: 88, loanId: 22, type: 'LOST_ITEM', amount: '173456.78', currency: 'VND' },
    }
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ statusCode: 200, data: response }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(markLibraryCopyLost('LOST/31', 'confirmed missing', 'jwt')).resolves.toEqual(response)

    const [url, options] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(new URL(url).pathname).toBe('/api/v2/book-copies/LOST%2F31/lost')
    expect(JSON.parse(options.body as string)).toEqual({ reason: 'confirmed missing' })
    expect(new Headers(options.headers).get('Authorization')).toBe('Bearer jwt')
  })
})
