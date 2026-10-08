import type { ResultPaginationDTO } from '@/types/notification'

export type BookCopyStatus = 'AVAILABLE' | 'DAMAGED' | 'ON_LOAN' | 'RESERVED' | 'LOST' | 'WITHDRAWN'
export type BookAvailability = 'AVAILABLE' | 'UNAVAILABLE'

export interface BookSummary {
  id: number
  isbn: string | null
  title: string
  author: string
  publisher: string | null
  publishedYear: number | null
  category: string | null
  listPrice: number | null
  coverUrl: string | null
  totalCopyCount: number
  availableBorrowableCopyCount: number
}

export interface BookDetail extends BookSummary {
  version: number
}

export interface BookCopy {
  id: number
  bookId: number
  barcode: string
  shelfLocation: string | null
  status: BookCopyStatus
  referenceOnly: boolean
  version: number
  createdAt: string
}

export interface BookCopyDetail extends BookCopy {
  book?: BookSummary
}

export interface BookCopyLookupResponse {
  copy: BookCopy
  book: BookSummary
}

export interface BookQuery {
  keyword?: string
  category?: string
  publishedYear?: number
  availability?: BookAvailability
  page: number
  size: number
  sort: string
}

export interface BookCopyQuery {
  status?: BookCopyStatus
  referenceOnly?: boolean
  page: number
  size: number
  sort: string
}

export interface BookFormValues {
  isbn: string
  title: string
  author: string
  publisher: string
  publishedYear: number | null
  category: string
  listPrice: number | null
  coverUrl: string
}

export type BookCreateRequest = Omit<BookFormValues, 'isbn' | 'publisher' | 'publishedYear' | 'category' | 'listPrice' | 'coverUrl'> & {
  isbn: string | null
  publisher: string | null
  publishedYear: number | null
  category: string | null
  listPrice: number | null
  coverUrl: string | null
}

export type BookUpdateRequest = BookCreateRequest & { expectedVersion: number }

export interface AddBookCopiesRequest {
  quantity: number
  shelfLocation: string | null
  referenceOnly: boolean
}

export interface AddBookCopiesResponse {
  bookId: number
  createdCount: number
  copies: BookCopy[]
}

export interface BookCopyUpdateRequest {
  shelfLocation?: string | null
  referenceOnly?: boolean
  status?: 'AVAILABLE' | 'DAMAGED'
  expectedVersion: number
}

export type BookPage = ResultPaginationDTO<BookSummary>
export type BookCopyPage = ResultPaginationDTO<BookCopy>

export const BOOK_COPY_STATUS_LABELS: Record<BookCopyStatus, string> = {
  AVAILABLE: 'Có sẵn',
  DAMAGED: 'Hư hỏng',
  ON_LOAN: 'Đang được mượn',
  RESERVED: 'Đã được giữ chỗ',
  LOST: 'Bị mất',
  WITHDRAWN: 'Đã rút khỏi kho',
}
