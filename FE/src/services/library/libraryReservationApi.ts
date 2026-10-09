import { apiClient } from '@/services/apiClient'
import type {
  CreateLibraryReservationRequest,
  LibraryReservation,
  LibraryReservationPage,
  LibraryReservationQuery,
} from '@/types/library/reservation'

const RESERVATIONS_PATH = '/api/v2/reservations'

export function createLibraryReservation(request: CreateLibraryReservationRequest, token: string): Promise<LibraryReservation> {
  return apiClient.post<LibraryReservation>(RESERVATIONS_PATH, request, { token })
}

export function listLibraryReservations(query: LibraryReservationQuery, token: string): Promise<LibraryReservationPage> {
  return apiClient.get<LibraryReservationPage>(RESERVATIONS_PATH, { token, query })
}

export function cancelLibraryReservation(reservationId: number, token: string): Promise<LibraryReservation> {
  return apiClient.post<LibraryReservation>(`${RESERVATIONS_PATH}/${reservationId}/cancel`, {}, { token })
}
