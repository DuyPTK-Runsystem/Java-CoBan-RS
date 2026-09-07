import { beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/services/apiClient'
import {
  fetchAccessibleClasses,
  fetchClassAnnualTranscript,
  fetchClassTermTranscript,
} from './classTranscriptApi'

vi.mock('@/services/apiClient', () => ({ apiClient: { get: vi.fn() } }))

describe('classTranscriptApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('calls accessible classes endpoint without and with academicYearId', async () => {
    vi.mocked(apiClient.get).mockResolvedValue([])
    await fetchAccessibleClasses('token-1')
    await fetchAccessibleClasses('token-1', 2026)

    expect(apiClient.get).toHaveBeenNthCalledWith(
      1,
      '/api/v2/classes/accessible-for-transcript',
      { token: 'token-1' },
    )
    expect(apiClient.get).toHaveBeenNthCalledWith(
      2,
      '/api/v2/classes/accessible-for-transcript?academicYearId=2026',
      { token: 'token-1' },
    )
  })

  it('calls class term transcript endpoint with classId and semesterId', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({})
    await fetchClassTermTranscript('token-2', 12, 1)

    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/v2/transcripts/classes/12/semesters/1',
      { token: 'token-2' },
    )
  })

  it('calls class annual transcript endpoint with classId and academicYearId', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({})
    await fetchClassAnnualTranscript('token-3', 12, 5)

    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/v2/transcripts/classes/12/academic-years/5',
      { token: 'token-3' },
    )
  })
})

