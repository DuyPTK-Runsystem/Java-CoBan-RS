import { afterEach, describe, expect, it, vi } from 'vitest'

const { getMock, postMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
  postMock: vi.fn(),
}))

vi.mock('@/services/apiClient', () => ({
  apiClient: {
    get: getMock,
    post: postMock,
  },
}))

import {
  approveTimetableAgentProposal,
  createTimetableAgentProposal,
  executeTimetableAgentProposal,
  getTimetableAgentAction,
  getTimetableAgentActionByKey,
  getTimetableAgentProposal,
} from './timetableAgentApi'
import type { TimetableAgentProposal, TimetableAgentRequest } from '@/types/timetableAgent'

describe('timetableAgentApi', () => {
  afterEach(() => {
    getMock.mockReset()
    postMock.mockReset()
  })

  it('sends only the requested scope and demand when creating a proposal', async () => {
    const request: TimetableAgentRequest = {
      targetRevisionId: 42,
      expectedVersion: 7,
      classIds: [3],
      validFrom: '2026-10-05',
      validTo: '2026-12-31',
      demands: [{ assignmentId: 88, periodsPerWeek: 4 }],
      lockedEntryIds: [1001],
      preferences: 'Ưu tiên buổi sáng',
      userRequest: 'Tránh tiết cuối ngày',
    }
    const proposal = { proposalId: 'proposal/42' } as TimetableAgentProposal
    postMock.mockResolvedValue(proposal)

    await expect(createTimetableAgentProposal(request, 'session-token')).resolves.toBe(proposal)

    expect(postMock).toHaveBeenCalledWith('/api/v4/timetable-agent/proposals', request, { token: 'session-token' })
  })

  it('encodes proposal IDs and binds approval to the exact version and hash', async () => {
    const proposal = {
      proposalId: 'proposal/42',
      proposalVersion: 3,
      proposalHash: 'sha256:approved-payload',
    } as TimetableAgentProposal
    postMock.mockResolvedValue(proposal)

    await approveTimetableAgentProposal(proposal, 'session-token')

    expect(postMock).toHaveBeenCalledWith(
      '/api/v4/timetable-agent/proposals/proposal%2F42/approve',
      { proposalVersion: 3, proposalHash: 'sha256:approved-payload' },
      { token: 'session-token' },
    )
  })

  it('preserves the idempotency key for execute and supports receipt recovery by key', async () => {
    postMock.mockResolvedValue({ actionId: 'action-1' })
    getMock.mockResolvedValue({ actionId: 'action-1' })

    await executeTimetableAgentProposal('proposal/42', 3, 'retry-key-123', 'session-token')
    await getTimetableAgentActionByKey('retry-key-123', 'session-token')

    expect(postMock).toHaveBeenCalledWith(
      '/api/v4/timetable-agent/proposals/proposal%2F42/execute',
      { proposalVersion: 3 },
      { token: 'session-token', headers: { 'Idempotency-Key': 'retry-key-123' } },
    )
    expect(getMock).toHaveBeenCalledWith('/api/v4/timetable-agent/actions/by-key', {
      token: 'session-token',
      query: { key: 'retry-key-123' },
    })
  })

  it('encodes server receipt and proposal identifiers on reads', async () => {
    getMock.mockResolvedValue({ proposalId: 'p1' })

    await getTimetableAgentProposal('proposal/42', 'session-token')
    await getTimetableAgentAction('action/9', 'session-token')

    expect(getMock).toHaveBeenNthCalledWith(1, '/api/v4/timetable-agent/proposals/proposal%2F42', { token: 'session-token' })
    expect(getMock).toHaveBeenNthCalledWith(2, '/api/v4/timetable-agent/actions/action%2F9', { token: 'session-token' })
  })
})
