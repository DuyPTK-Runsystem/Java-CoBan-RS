import { beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/services/apiClient'
import {
  fetchMyAnnualStatus,
  fetchMyAnnualTranscript,
  fetchMyTermStatus,
  fetchMyTermTranscript,
  fetchStudentAnnualStatus,
  fetchStudentAnnualTranscript,
  fetchStudentTermStatus,
  fetchStudentTermTranscript,
} from './transcriptApi'

vi.mock('@/services/apiClient', () => ({ apiClient: { get: vi.fn() } }))

describe('transcriptApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('calls student self term transcript and status endpoints', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({})
    await fetchMyTermTranscript('token-123', 5)
    await fetchMyTermStatus('token-123', 5)

    expect(apiClient.get).toHaveBeenNthCalledWith(
      1,
      '/api/v2/transcripts/students/me/semesters/5',
      { token: 'token-123' },
    )
    expect(apiClient.get).toHaveBeenNthCalledWith(
      2,
      '/api/v2/transcripts/students/me/semesters/5/status',
      { token: 'token-123' },
    )
  })

  it('calls student self annual transcript and status endpoints', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({})
    await fetchMyAnnualTranscript('token-456', 2)
    await fetchMyAnnualStatus('token-456', 2)

    expect(apiClient.get).toHaveBeenNthCalledWith(
      1,
      '/api/v2/transcripts/students/me/academic-years/2',
      { token: 'token-456' },
    )
    expect(apiClient.get).toHaveBeenNthCalledWith(
      2,
      '/api/v2/transcripts/students/me/academic-years/2/status',
      { token: 'token-456' },
    )
  })

  it('calls staff scoped transcript endpoints with studentId', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({})
    await fetchStudentTermTranscript('token-789', 101, 5)
    await fetchStudentAnnualTranscript('token-789', 101, 2)
    await fetchStudentTermStatus('token-789', 101, 5)
    await fetchStudentAnnualStatus('token-789', 101, 2)

    expect(apiClient.get).toHaveBeenNthCalledWith(
      1,
      '/api/v2/transcripts/students/101/semesters/5',
      { token: 'token-789' },
    )
    expect(apiClient.get).toHaveBeenNthCalledWith(
      2,
      '/api/v2/transcripts/students/101/academic-years/2',
      { token: 'token-789' },
    )
    expect(apiClient.get).toHaveBeenNthCalledWith(
      3,
      '/api/v2/transcripts/students/101/semesters/5/status',
      { token: 'token-789' },
    )
    expect(apiClient.get).toHaveBeenNthCalledWith(
      4,
      '/api/v2/transcripts/students/101/academic-years/2/status',
      { token: 'token-789' },
    )
  })
})
