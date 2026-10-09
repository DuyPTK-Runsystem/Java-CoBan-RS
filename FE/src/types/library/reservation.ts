import type { ResultPaginationDTO } from '@/types/notification'

export type LibraryReservationStatus = 'WAITING' | 'READY' | 'FULFILLED' | 'CANCELLED' | 'EXPIRED'

export interface LibraryReservation {
  reservationId: number
  bookId: number
  bookTitle: string
  patronId: number
  status: LibraryReservationStatus
  reservedAt: string
  readyAt: string | null
  pickupDueAt: string | null
  allocatedCopyBarcode: string | null
  fulfilledAt: string | null
  cancelledAt: string | null
  policyVersion: number | string
}

export interface LibraryReservationQuery {
  bookId?: number
  status?: LibraryReservationStatus
  patronId?: number
  page?: number
  pageSize?: number
}

export interface CreateLibraryReservationRequest {
  bookId: number
}

export type LibraryReservationPage = ResultPaginationDTO<LibraryReservation>
