import { apiClient } from '@/services/apiClient'
import type { LibraryBatchJob, LibraryFine, LibraryFinePage, LibraryFineQuery, PayLibraryFineRequest, RunOverdueFineBatchRequest, WaiveLibraryFineRequest } from '@/types/library/fine'

const FINES_PATH = '/api/v2/fines'
const BATCH_JOBS_PATH = '/api/v2/library/batch-jobs'

export function getLibraryFine(fineId: number, token: string): Promise<LibraryFine> {
  return apiClient.get<LibraryFine>(`${FINES_PATH}/${fineId}`, { token })
}

export function listLibraryFines(query: LibraryFineQuery, token: string): Promise<LibraryFinePage> {
  return apiClient.get<LibraryFinePage>(FINES_PATH, { token, query })
}

export function payLibraryFine(fineId: number, request: PayLibraryFineRequest, token: string): Promise<LibraryFine> {
  return apiClient.post<LibraryFine>(`${FINES_PATH}/${fineId}/pay`, request, { token })
}

export function waiveLibraryFine(fineId: number, request: WaiveLibraryFineRequest, token: string): Promise<LibraryFine> {
  return apiClient.post<LibraryFine>(`${FINES_PATH}/${fineId}/waive`, request, { token })
}

export function listLibraryBatchJobs(token: string): Promise<LibraryBatchJob[]> {
  return apiClient.get<LibraryBatchJob[]>(BATCH_JOBS_PATH, { token })
}

export function runLibraryOverdueFineBatch(request: RunOverdueFineBatchRequest, token: string): Promise<LibraryBatchJob> {
  return apiClient.post<LibraryBatchJob>(`${BATCH_JOBS_PATH}/overdue-fine`, request, { token })
}
