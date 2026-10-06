import { apiClient } from '@/services/apiClient'
import type { TimetableAgentActionState, TimetableAgentProposal, TimetableAgentRequest } from '@/types/timetableAgent'

const basePath = '/api/v4/timetable-agent'

export function createTimetableAgentProposal(payload: TimetableAgentRequest, token: string): Promise<TimetableAgentProposal> {
  return apiClient.post(`${basePath}/proposals`, payload, { token })
}

export function getTimetableAgentProposal(id: string, token: string): Promise<TimetableAgentProposal> {
  return apiClient.get(`${basePath}/proposals/${encodeURIComponent(id)}`, { token })
}

export function approveTimetableAgentProposal(proposal: Pick<TimetableAgentProposal, 'proposalId' | 'proposalVersion' | 'proposalHash'>, token: string): Promise<TimetableAgentProposal> {
  return apiClient.post(`${basePath}/proposals/${encodeURIComponent(proposal.proposalId)}/approve`, {
    proposalVersion: proposal.proposalVersion,
    proposalHash: proposal.proposalHash,
  }, { token })
}

export function executeTimetableAgentProposal(id: string, proposalVersion: number, idempotencyKey: string, token: string): Promise<TimetableAgentActionState> {
  return apiClient.post(`${basePath}/proposals/${encodeURIComponent(id)}/execute`, { proposalVersion }, {
    token, headers: { 'Idempotency-Key': idempotencyKey },
  })
}

export function getTimetableAgentAction(id: string, token: string): Promise<TimetableAgentActionState> {
  return apiClient.get(`${basePath}/actions/${encodeURIComponent(id)}`, { token })
}

export function getTimetableAgentActionByKey(key: string, token: string): Promise<TimetableAgentActionState> {
  return apiClient.get(`${basePath}/actions/by-key`, { token, query: { key } })
}
