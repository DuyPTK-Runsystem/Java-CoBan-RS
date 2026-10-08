import { apiClient } from '@/services/apiClient'
import type {
  AddBookCopiesRequest,
  AddBookCopiesResponse,
  BookCopyDetail,
  BookCopyLookupResponse,
  BookCopyPage,
  BookCopyQuery,
  BookCopyUpdateRequest,
  BookCreateRequest,
  BookDetail,
  BookPage,
  BookQuery,
  BookUpdateRequest,
} from '@/types/library/catalog'

const BOOKS_PATH = '/api/v2/books'

export function listBooks(query: BookQuery, token: string): Promise<BookPage> {
  return apiClient.get<BookPage>(BOOKS_PATH, { token, query })
}

export function getBook(bookId: number, token: string): Promise<BookDetail> {
  return apiClient.get<BookDetail>(`${BOOKS_PATH}/${bookId}`, { token })
}

export function createBook(request: BookCreateRequest, token: string): Promise<BookDetail> {
  return apiClient.post<BookDetail>(BOOKS_PATH, request, { token })
}

export function updateBook(bookId: number, request: BookUpdateRequest, token: string): Promise<BookDetail> {
  return apiClient.put<BookDetail>(`${BOOKS_PATH}/${bookId}`, request, { token })
}

export function archiveBook(bookId: number, expectedVersion: number, token: string): Promise<void> {
  return apiClient.delete<void>(`${BOOKS_PATH}/${bookId}`, { token, query: { expectedVersion } })
}

export function listBookCopies(bookId: number, query: BookCopyQuery, token: string): Promise<BookCopyPage> {
  return apiClient.get<BookCopyPage>(`${BOOKS_PATH}/${bookId}/copies`, { token, query })
}

export function addBookCopies(
  bookId: number,
  request: AddBookCopiesRequest,
  idempotencyKey: string,
  token: string,
): Promise<AddBookCopiesResponse> {
  return apiClient.post<AddBookCopiesResponse>(`${BOOKS_PATH}/${bookId}/copies`, request, {
    token,
    headers: { 'Idempotency-Key': idempotencyKey },
  })
}

export function getBookCopyByBarcode(barcode: string, token: string): Promise<BookCopyDetail> {
  return apiClient.get<BookCopyLookupResponse>(`/api/v2/book-copies/${encodeURIComponent(barcode)}`, { token })
    .then(({ copy, book }) => ({ ...copy, book }))
}

export function updateBookCopy(barcode: string, request: BookCopyUpdateRequest, token: string): Promise<BookCopyDetail> {
  return apiClient.patch<BookCopyDetail>(`/api/v2/book-copies/${encodeURIComponent(barcode)}`, request, { token })
}

export function withdrawBookCopy(barcode: string, expectedVersion: number, token: string): Promise<void> {
  return apiClient.delete<void>(`/api/v2/book-copies/${encodeURIComponent(barcode)}`, {
    token,
    query: { expectedVersion },
  })
}

export function getBarcodePng(barcode: string, token: string): Promise<Blob> {
  return apiClient.get<Blob>(`/api/v2/book-copies/${encodeURIComponent(barcode)}/barcode.png`, {
    token,
    responseType: 'blob',
    headers: { Accept: 'image/png' },
  })
}
