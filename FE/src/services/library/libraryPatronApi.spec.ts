import { afterEach, describe, expect, it, vi } from 'vitest'

import {
  activateLibraryPatron,
  getLibraryCardQr,
  issueLibraryCard,
  listMyLibraryCardHistory,
  listPatronActivationCandidates,
  listLibraryPatrons,
  reissueLibraryCard,
  revokeLibraryCard,
  updateLibraryPatronStatus,
  verifyLibraryCard,
} from './libraryPatronApi'

const fetchMock = vi.fn()
const envelope = (data: unknown, status = 200) => new Response(JSON.stringify({ statusCode: status, data }), { status })

describe('libraryPatronApi transport contract', () => {
  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('lists patrons and trims activation-candidate search input', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(envelope({ meta: { page: 0, pageSize: 10, totalPages: 0, totalItems: 0 }, result: [] })))
    vi.stubGlobal('fetch', fetchMock)

    await listLibraryPatrons({ keyword: 'An', status: 'ACTIVE', page: 0, size: 10, sort: 'joinedAt,desc' }, 'jwt')
    await listPatronActivationCandidates('  An  ', 1, 20, 'jwt')

    const patronsUrl = new URL(fetchMock.mock.calls[0]?.[0] as string)
    expect(patronsUrl.pathname).toBe('/api/v2/library-patrons')
    expect(Object.fromEntries(patronsUrl.searchParams.entries())).toEqual({ keyword: 'An', status: 'ACTIVE', page: '0', size: '10', sort: 'joinedAt,desc' })

    const candidatesUrl = new URL(fetchMock.mock.calls[1]?.[0] as string)
    expect(candidatesUrl.pathname).toBe('/api/v2/library-patrons/activation-candidates')
    expect(Object.fromEntries(candidatesUrl.searchParams.entries())).toEqual({ keyword: 'An', page: '1', size: '20' })
    expect(new Headers((fetchMock.mock.calls[1]?.[1] as RequestInit).headers).get('Authorization')).toBe('Bearer jwt')
  })

  it('sends patron and card mutations with the approved request shapes', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(envelope({ patronId: 4 })))
    vi.stubGlobal('fetch', fetchMock)

    await activateLibraryPatron({ userId: 15 }, 'jwt')
    await updateLibraryPatronStatus(4, { status: 'BORROWING_SUSPENDED', reason: 'Review' }, 'jwt')
    await issueLibraryCard({ patronId: 4, expiresAt: '2027-10-08' }, 'jwt')
    await revokeLibraryCard('LC-2026-000001', { reason: 'Damaged' }, 'jwt')
    await reissueLibraryCard('LC/2026 000001', { expiresAt: '2028-02-28', reason: 'Replacement' }, 'jwt')
    await verifyLibraryCard({ payload: 'v1|LC-2026-000001|4|21000|signature' }, 'jwt')

    expect(fetchMock.mock.calls.map(([url, init]) => [new URL(url as string).pathname, (init as RequestInit).method])).toEqual([
      ['/api/v2/library-patrons', 'POST'],
      ['/api/v2/library-patrons/4/status', 'PATCH'],
      ['/api/v2/library-cards', 'POST'],
      ['/api/v2/library-cards/LC-2026-000001/revoke', 'POST'],
      ['/api/v2/library-cards/LC%2F2026%20000001/reissue', 'POST'],
      ['/api/v2/library-cards/verify', 'POST'],
    ])
    expect(JSON.parse((fetchMock.mock.calls[0]?.[1] as RequestInit).body as string)).toEqual({ userId: 15 })
    expect(JSON.parse((fetchMock.mock.calls[2]?.[1] as RequestInit).body as string)).toEqual({ patronId: 4, expiresAt: '2027-10-08' })
    expect(JSON.parse((fetchMock.mock.calls[4]?.[1] as RequestInit).body as string)).toEqual({ expiresAt: '2028-02-28', reason: 'Replacement' })
  })

  it('fetches owner card history and downloads the QR as an authenticated PNG blob', async () => {
    const png = new Response(new Uint8Array([137, 80, 78, 71]), { status: 200, headers: { 'Content-Type': 'image/png' } })
    fetchMock.mockResolvedValueOnce(envelope([{ cardNo: 'LC-2026-000001', expiresAt: '2027-10-08' }])).mockResolvedValueOnce(png)
    vi.stubGlobal('fetch', fetchMock)

    await expect(listMyLibraryCardHistory('jwt')).resolves.toEqual([{ cardNo: 'LC-2026-000001', expiresAt: '2027-10-08' }])
    const image = await getLibraryCardQr('LC/2026 000001', 'jwt')

    expect(new URL(fetchMock.mock.calls[0]?.[0] as string).pathname).toBe('/api/v2/library-cards/me/history')
    expect(new URL(fetchMock.mock.calls[1]?.[0] as string).pathname).toBe('/api/v2/library-cards/LC%2F2026%20000001/qr.png')
    expect(new Headers((fetchMock.mock.calls[1]?.[1] as RequestInit).headers).get('Accept')).toBe('image/png')
    expect(new Headers((fetchMock.mock.calls[1]?.[1] as RequestInit).headers).get('Authorization')).toBe('Bearer jwt')
    expect(image.type).toBe('image/png')
  })
})
