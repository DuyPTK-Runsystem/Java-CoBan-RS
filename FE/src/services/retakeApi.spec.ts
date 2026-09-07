import { beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/services/apiClient'
import {
  cancelRetakeExam,
  createRetakeExam,
  fetchRetakeExam,
  fetchRetakeExams,
  updateRetakeScore,
} from './retakeApi'

vi.mock('@/services/apiClient', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}))

describe('retakeApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('maps query parameters and filters for fetchRetakeExams', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    })

    await fetchRetakeExams('test-token', {
      studentId: 101,
      academicYearId: 2,
      subjectId: 15,
      status: 'PLANNED',
      page: 1,
      size: 10,
    })

    expect(apiClient.get).toHaveBeenCalledWith('/api/v2/retake-exams', expect.objectContaining({
      token: 'test-token',
    }))

    const query = vi.mocked(apiClient.get).mock.calls[0]?.[1]?.query as URLSearchParams
    expect(query.get('studentId')).toBe('101')
    expect(query.get('academicYearId')).toBe('2')
    expect(query.get('subjectId')).toBe('15')
    expect(query.get('status')).toBe('PLANNED')
    expect(query.get('page')).toBe('1')
    expect(query.get('size')).toBe('10')
  })

  it('calls fetchRetakeExam with retakeId', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      retakeId: 7001,
      studentId: 101,
      academicYearId: 2,
      subjectId: 15,
      preRetakeScore: 4.0,
      retakeScore: null,
      examDate: '2027-06-15',
      status: 'PLANNED',
      note: null,
    })

    const result = await fetchRetakeExam('test-token', 7001)

    expect(apiClient.get).toHaveBeenCalledWith('/api/v2/retake-exams/7001', { token: 'test-token' })
    expect(result.retakeId).toBe(7001)
  })

  it('calls createRetakeExam with payload', async () => {
    const payload = {
      studentId: 101,
      academicYearId: 2,
      subjectId: 15,
      examDate: '2027-06-15',
      retakeScore: 6.5,
      note: 'Tạo kỳ thi lại',
    }

    vi.mocked(apiClient.post).mockResolvedValue({
      retakeId: 7002,
      ...payload,
      preRetakeScore: 4.0,
      status: 'SCORED',
    })

    const result = await createRetakeExam('test-token', payload)

    expect(apiClient.post).toHaveBeenCalledWith('/api/v2/retake-exams', payload, { token: 'test-token' })
    expect(result.retakeId).toBe(7002)
  })

  it('calls updateRetakeScore with score payload', async () => {
    const payload = {
      retakeScore: 7.0,
      examDate: '2027-06-20',
      note: 'Cập nhật điểm thi lại',
    }

    vi.mocked(apiClient.put).mockResolvedValue({
      retakeId: 7001,
      studentId: 101,
      academicYearId: 2,
      subjectId: 15,
      preRetakeScore: 4.0,
      retakeScore: 7.0,
      examDate: '2027-06-20',
      status: 'SCORED',
      note: 'Cập nhật điểm thi lại',
    })

    const result = await updateRetakeScore('test-token', 7001, payload)

    expect(apiClient.put).toHaveBeenCalledWith('/api/v2/retake-exams/7001/score', payload, { token: 'test-token' })
    expect(result.retakeScore).toBe(7.0)
  })

  it('calls cancelRetakeExam with cancel path', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({
      retakeId: 7001,
      studentId: 101,
      academicYearId: 2,
      subjectId: 15,
      preRetakeScore: 4.0,
      retakeScore: null,
      examDate: '2027-06-15',
      status: 'CANCELLED',
      note: null,
    })

    const result = await cancelRetakeExam('test-token', 7001)

    expect(apiClient.post).toHaveBeenCalledWith('/api/v2/retake-exams/7001/cancel', undefined, { token: 'test-token' })
    expect(result.status).toBe('CANCELLED')
  })
})
