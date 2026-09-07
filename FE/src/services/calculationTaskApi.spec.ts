import { beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/services/apiClient'
import {
  fetchCalculationTasks,
  fetchFailedCalculationTasks,
  recalculateTranscriptByCode,
  recalculateTranscriptById,
  retryAllFailedCalculationTasks,
  retryCalculationTask,
} from './calculationTaskApi'

vi.mock('@/services/apiClient', () => ({ apiClient: { get: vi.fn(), post: vi.fn() } }))

describe('calculationTaskApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('queries calculation tasks with correctly mapped parameters', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    })

    await fetchCalculationTasks('token-123', {
      status: 'FAILED',
      studentCode: ' HS0001 ',
      studentId: 10,
      academicYearId: 2,
      page: 1,
      size: 20,
    })

    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/v2/scorebooks/calculation-tasks',
      expect.objectContaining({ token: 'token-123' }),
    )
    const query = vi.mocked(apiClient.get).mock.calls[0]?.[1]?.query as URLSearchParams
    expect(query.get('status')).toBe('FAILED')
    expect(query.get('studentCode')).toBe('HS0001')
    expect(query.get('studentId')).toBe('10')
    expect(query.get('academicYearId')).toBe('2')
    expect(query.get('page')).toBe('1')
    expect(query.get('size')).toBe('20')
  })

  it('queries failed calculation tasks from canonical /failed endpoint', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    })

    await fetchFailedCalculationTasks('token-123', {
      studentCode: 'HS0002',
      page: 0,
      size: 10,
    })

    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/v2/scorebooks/calculation-tasks/failed',
      expect.objectContaining({ token: 'token-123' }),
    )
    const query = vi.mocked(apiClient.get).mock.calls[0]?.[1]?.query as URLSearchParams
    expect(query.get('studentCode')).toBe('HS0002')
  })

  it('calls single retry endpoint with taskId', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({})
    await retryCalculationTask('token-abc', 1048)

    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/v2/scorebooks/calculation-tasks/1048/retry',
      undefined,
      { token: 'token-abc' },
    )
  })

  it('calls bulk retry endpoint for all failed tasks', async () => {
    vi.mocked(apiClient.post).mockResolvedValue([])
    await retryAllFailedCalculationTasks('token-abc')

    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/v2/scorebooks/calculation-tasks/retry-all-failed',
      undefined,
      { token: 'token-abc' },
    )
  })

  it('calls recalculate transcript by studentCode and by studentId', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({})

    await recalculateTranscriptByCode('token-xyz', 'HS0001', 3)
    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/v2/students/HS0001/transcripts/recalculate',
      undefined,
      expect.objectContaining({ token: 'token-xyz' }),
    )
    const query1 = vi.mocked(apiClient.post).mock.calls[0]?.[2]?.query as URLSearchParams
    expect(query1.get('academicYearId')).toBe('3')

    await recalculateTranscriptById('token-xyz', 42, 3)
    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/v2/students/42/transcripts/recalculate',
      undefined,
      expect.objectContaining({ token: 'token-xyz' }),
    )
    const query2 = vi.mocked(apiClient.post).mock.calls[1]?.[2]?.query as URLSearchParams
    expect(query2.get('academicYearId')).toBe('3')
  })
})
