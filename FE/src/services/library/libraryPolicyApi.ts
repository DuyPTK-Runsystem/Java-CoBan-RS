import { apiClient } from '@/services/apiClient'
import type { LibraryCirculationPolicy, UpdateLibraryCirculationPolicyRequest } from '@/types/library/policy'

const POLICY_PATH = '/api/v2/library/policies/circulation'

export function getLibraryCirculationPolicy(token: string): Promise<LibraryCirculationPolicy> {
  return apiClient.get<LibraryCirculationPolicy>(POLICY_PATH, { token })
}

export function updateLibraryCirculationPolicy(
  request: UpdateLibraryCirculationPolicyRequest,
  token: string,
): Promise<LibraryCirculationPolicy> {
  return apiClient.put<LibraryCirculationPolicy>(POLICY_PATH, request, { token })
}
