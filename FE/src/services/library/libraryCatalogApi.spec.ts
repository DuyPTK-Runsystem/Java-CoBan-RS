import { afterEach, describe, expect, it, vi } from 'vitest'

import {
  addBookCopies,
  archiveBook,
  createBook,
  getBarcodePng,
  getBookCopyByBarcode,
  listBooks,
  updateBook,
  updateBookCopy,
  withdrawBookCopy,
} from './libraryCatalogApi'

const fetchMock = vi.fn()
const envelope = (data: unknown, status = 200) => new Response(JSON.stringify({ statusCode: status, data }), { status })

describe('libraryCatalogApi transport contract', () => {
  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('sends catalog filters and pagination as a zero-based query', async () => {
    fetchMock.mockResolvedValue(envelope({ meta: { page: 2, pageSize: 10, totalPages: 3, totalItems: 21 }, result: [] }))
    vi.stubGlobal('fetch', fetchMock)

    await listBooks({ keyword: '  Java ', category: 'Lập trình', publishedYear: 2024, availability: 'AVAILABLE', page: 2, size: 10, sort: 'title,asc' }, 'jwt')

    const [url, options] = fetchMock.mock.calls[0] as [string, RequestInit]
    const parsed = new URL(url)
    expect(parsed.pathname).toBe('/api/v2/books')
    expect(Object.fromEntries(parsed.searchParams.entries())).toEqual({ keyword: '  Java ', category: 'Lập trình', publishedYear: '2024', availability: 'AVAILABLE', page: '2', size: '10', sort: 'title,asc' })
    expect(new Headers(options.headers).get('Authorization')).toBe('Bearer jwt')
  })

  it('preserves expectedVersion and the idempotency header on mutations', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(envelope({ id: 4 }, 200)))
    vi.stubGlobal('fetch', fetchMock)
    const bookRequest = { isbn: null, title: 'Java', author: 'A', publisher: null, publishedYear: null, category: null, listPrice: null, coverUrl: null, expectedVersion: 3 }
    const copyRequest = { shelfLocation: null, referenceOnly: true, expectedVersion: 8 }

    await createBook({ ...bookRequest, expectedVersion: undefined }, 'jwt')
    await updateBook(4, bookRequest, 'jwt')
    await archiveBook(4, 3, 'jwt')
    await addBookCopies(4, { quantity: 2, shelfLocation: ' A-2 ', referenceOnly: false }, 'intent-key-01', 'jwt')
    await updateBookCopy('LIB-000000001', copyRequest, 'jwt')
    await withdrawBookCopy('LIB-000000001', 8, 'jwt')

    expect(fetchMock.mock.calls.map(([url, init]) => [new URL(url as string).pathname, (init as RequestInit).method])).toEqual([
      ['/api/v2/books', 'POST'], ['/api/v2/books/4', 'PUT'], ['/api/v2/books/4', 'DELETE'],
      ['/api/v2/books/4/copies', 'POST'], ['/api/v2/book-copies/LIB-000000001', 'PATCH'], ['/api/v2/book-copies/LIB-000000001', 'DELETE'],
    ])
    expect(JSON.parse((fetchMock.mock.calls[1]?.[1] as RequestInit).body as string)).toEqual(bookRequest)
    expect(new URL(fetchMock.mock.calls[2]?.[0] as string).searchParams.get('expectedVersion')).toBe('3')
    expect(new Headers((fetchMock.mock.calls[3]?.[1] as RequestInit).headers).get('Idempotency-Key')).toBe('intent-key-01')
    expect(JSON.parse((fetchMock.mock.calls[4]?.[1] as RequestInit).body as string)).toEqual(copyRequest)
    expect(new URL(fetchMock.mock.calls[5]?.[0] as string).searchParams.get('expectedVersion')).toBe('8')
  })

  it('maps nested barcode lookup data to the UI copy shape and downloads a PNG blob', async () => {
    const copy = { id: 5, barcode: 'LIB-000000005', status: 'AVAILABLE' }
    const book = { id: 4, title: 'Java' }
    fetchMock.mockResolvedValueOnce(envelope({ copy, book })).mockResolvedValueOnce(new Response(new Uint8Array([137, 80, 78, 71]), { status: 200, headers: { 'Content-Type': 'image/png' } }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(getBookCopyByBarcode('LIB-000000005/1', 'jwt')).resolves.toEqual({ ...copy, book })
    const png = await getBarcodePng('LIB-000000005/1', 'jwt')

    expect(new URL(fetchMock.mock.calls[0]?.[0] as string).pathname).toBe('/api/v2/book-copies/LIB-000000005%2F1')
    expect(new URL(fetchMock.mock.calls[1]?.[0] as string).pathname).toBe('/api/v2/book-copies/LIB-000000005%2F1/barcode.png')
    expect(new Headers((fetchMock.mock.calls[1]?.[1] as RequestInit).headers).get('Accept')).toBe('image/png')
    expect(png.type).toBe('image/png')
    expect((await png.arrayBuffer()).byteLength).toBe(4)
  })

  it('retains scoped Library error codes and field messages while preserving legacy error codes', async () => {
    fetchMock.mockResolvedValueOnce(new Response(JSON.stringify({ statusCode: 400, code: 'BOOK_VALIDATION_FAILED', fieldErrors: { title: ['Tên sách là bắt buộc.'] }, message: 'Invalid book' }), { status: 400 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ statusCode: 409, error: 'Conflict', message: 'Đã có dữ liệu trùng lặp.', data: null }), { status: 409 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(createBook({ isbn: null, title: '', author: '', publisher: null, publishedYear: null, category: null, listPrice: null, coverUrl: null }, 'jwt'))
      .rejects.toMatchObject({ code: 'BOOK_VALIDATION_FAILED', status: 400, validationErrors: [{ field: 'title', messages: ['Tên sách là bắt buộc.'] }] })
    await expect(updateBook(4, { isbn: null, title: 'Java', author: 'A', publisher: null, publishedYear: null, category: null, listPrice: null, coverUrl: null, expectedVersion: 2 }, 'jwt'))
      .rejects.toMatchObject({ status: 409, code: undefined, message: 'Đã có dữ liệu trùng lặp.' })
  })
})
