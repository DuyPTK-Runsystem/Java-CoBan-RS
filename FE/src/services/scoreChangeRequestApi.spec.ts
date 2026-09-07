import { beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/services/apiClient'
import { approveScoreChangeRequest, createScoreChangeRequest, fetchScoreChangeRequests, rejectScoreChangeRequest } from './scoreChangeRequestApi'

vi.mock('@/services/apiClient', () => ({ apiClient: { get: vi.fn(), post: vi.fn() } }))

describe('scoreChangeRequestApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('maps the friendly filters to the server query without exposing technical identifiers to the caller', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 })
    await fetchScoreChangeRequests('token', { status: 'PENDING', studentCode: ' HS-001 ', page: 1, size: 20 })
    expect(apiClient.get).toHaveBeenCalledWith('/api/v2/score-change-requests', expect.objectContaining({ token: 'token' }))
    const query = vi.mocked(apiClient.get).mock.calls[0]?.[1]?.query as URLSearchParams
    expect(query.get('status')).toBe('PENDING')
    expect(query.get('studentCode')).toBe('HS-001')
    expect(query.get('page')).toBe('1')
    expect(query.get('size')).toBe('20')
  })

  it('uses the documented mutation paths and bodies', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({})
    await createScoreChangeRequest('token', { assessmentColumnId: 4, studentCode: 'HS-001', proposedStatus: 'SCORED', proposedValue: 8, reason: 'Nhập nhầm điểm' })
    await approveScoreChangeRequest('token', 9)
    await rejectScoreChangeRequest('token', 9, { rejectionReason: 'Chưa đủ minh chứng' })
    expect(apiClient.post).toHaveBeenNthCalledWith(1, '/api/v2/score-change-requests', expect.objectContaining({ studentCode: 'HS-001' }), { token: 'token' })
    expect(apiClient.post).toHaveBeenNthCalledWith(2, '/api/v2/score-change-requests/9/approve', undefined, { token: 'token' })
    expect(apiClient.post).toHaveBeenNthCalledWith(3, '/api/v2/score-change-requests/9/reject', { rejectionReason: 'Chưa đủ minh chứng' }, { token: 'token' })
  })
})
