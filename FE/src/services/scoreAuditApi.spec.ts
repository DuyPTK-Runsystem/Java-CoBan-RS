import { beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/services/apiClient'
import { fetchScoreAuditLogs } from './scoreAuditApi'

vi.mock('@/services/apiClient', () => ({ apiClient: { get: vi.fn() } }))

describe('scoreAuditApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('queries score audit logs with mapped filters and pagination', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    })

    await fetchScoreAuditLogs('token-audit', {
      entityType: 'CALCULATION_TASK',
      entityId: 'CT-1048',
      studentCode: 'HS0001',
      studentId: 15,
      action: 'CALCULATION_TASK_RETRIED',
      actorUserId: 2,
      fromOccurredAt: '2026-09-01T00:00:00',
      toOccurredAt: '2026-09-03T23:59:59',
      page: 2,
      size: 25,
    })

    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/v2/scorebooks/audit-logs',
      expect.objectContaining({ token: 'token-audit' }),
    )
    const query = vi.mocked(apiClient.get).mock.calls[0]?.[1]?.query as URLSearchParams
    expect(query.get('entityType')).toBe('CALCULATION_TASK')
    expect(query.get('entityId')).toBe('CT-1048')
    expect(query.get('studentCode')).toBe('HS0001')
    expect(query.get('studentId')).toBe('15')
    expect(query.get('action')).toBe('CALCULATION_TASK_RETRIED')
    expect(query.get('actorUserId')).toBe('2')
    expect(query.get('fromOccurredAt')).toBe('2026-09-01T00:00:00')
    expect(query.get('toOccurredAt')).toBe('2026-09-03T23:59:59')
    expect(query.get('page')).toBe('2')
    expect(query.get('size')).toBe('25')
  })
})
