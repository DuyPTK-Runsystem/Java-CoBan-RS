import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import CalculationOperationsView from './CalculationOperationsView.vue'
import * as academicApi from '@/services/academicApi'
import * as calculationTaskApi from '@/services/calculationTaskApi'
import * as scoreAuditApi from '@/services/scoreAuditApi'
import * as transcriptApi from '@/services/transcriptApi'
import { ApiError } from '@/types/api'

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn().mockResolvedValue(undefined),
  }),
  useRoute: () => ({
    path: '/v2/scorebooks/operations',
    query: {},
  }),
}))

vi.mock('@/services/academicApi', () => ({
  fetchAcademicYears: vi.fn(),
}))

vi.mock('@/services/calculationTaskApi', () => ({
  fetchCalculationTasks: vi.fn(),
  fetchFailedCalculationTasks: vi.fn(),
  retryCalculationTask: vi.fn(),
  retryAllFailedCalculationTasks: vi.fn(),
}))

vi.mock('@/services/scoreAuditApi', () => ({
  fetchScoreAuditLogs: vi.fn(),
}))

vi.mock('@/services/transcriptApi', () => ({
  fetchStudentAnnualStatus: vi.fn(),
}))

vi.mock('@/services/studentApi', () => ({
  fetchStudents: vi.fn().mockResolvedValue({ content: [] }),
}))

const mockTasks = [
  {
    taskId: 1048,
    studentId: 101,
    studentCode: 'HS0001',
    academicYearId: 2,
    taskType: 'STUDENT_YEAR_RECALC' as const,
    requestedVersion: 11,
    status: 'FAILED' as const,
    attemptCount: 3,
    maxAttempts: 3,
    availableAt: null,
    lockedAt: null,
    workerId: null,
    lastError: 'Timeout khi đọc transcript HK2',
    createdAt: '2026-09-03T09:12:44',
    startedAt: null,
    completedAt: null,
  },
]

describe('CalculationOperationsView.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    saveAuthSession({
      accessToken: 'token-office',
      user: {
        id: 1,
        username: 'academic.office',
        roles: ['ACADEMIC_OFFICE'],
      },
    })

    vi.mocked(academicApi.fetchAcademicYears).mockResolvedValue([
      {
        id: 2,
        code: '2026-2027',
        startDate: '2026-09-01',
        endDate: '2027-05-31',
        status: 'ACTIVE',
        notes: null,
      },
    ])

    vi.mocked(calculationTaskApi.fetchFailedCalculationTasks).mockResolvedValue({
      content: mockTasks,
      totalElements: 1,
      totalPages: 1,
      size: 10,
    })

    vi.mocked(calculationTaskApi.fetchCalculationTasks).mockResolvedValue({
      content: mockTasks,
      totalElements: 1,
      totalPages: 1,
      size: 10,
    })

    vi.mocked(scoreAuditApi.fetchScoreAuditLogs).mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 10,
    })

    vi.mocked(transcriptApi.fetchStudentAnnualStatus).mockResolvedValue({
      studentId: 101,
      studentCode: 'HS0001',
      academicYearId: 2,
      semesterId: null,
      calculationStatus: 'FINISH',
      sourceVersion: 11,
      calculatedVersion: 11,
      isUpToDate: true,
      calculatedAt: '2026-09-03T09:15:00',
      lastError: null,
    })
  })

  afterEach(() => {
    clearAuthSession()
  })

  it('mounts and loads tasks by default', async () => {
    const wrapper = mount(CalculationOperationsView, {
      global: {
        stubs: {
          Select: true,
          InputText: true,
          Button: true,
          Tag: true,
          Dialog: true,
          ServerPagination: true,
        },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Calculation Task & Audit')
    expect(calculationTaskApi.fetchFailedCalculationTasks).toHaveBeenCalled()
    expect(wrapper.text()).toContain('#CT-1048')
  })

  it('switches between tabs', async () => {
    const wrapper = mount(CalculationOperationsView, {
      global: {
        stubs: {
          Select: true,
          InputText: true,
          Button: true,
          Tag: true,
          Dialog: true,
          ServerPagination: true,
        },
      },
    })
    await flushPromises()

    // Switch to status tab
    const statusTabBtn = wrapper.find('[data-testid="tab-status"]')
    await statusTabBtn.trigger('click')
    await flushPromises()

    expect(wrapper.findComponent({ name: 'TranscriptStatusCard' }).exists()).toBe(true)

    // Switch to audit tab
    const auditTabBtn = wrapper.find('[data-testid="tab-audit"]')
    await auditTabBtn.trigger('click')
    await flushPromises()

    expect(scoreAuditApi.fetchScoreAuditLogs).toHaveBeenCalled()
    expect(wrapper.findComponent({ name: 'ScoreAuditLogTable' }).exists()).toBe(true)
  })

  it('handles single retry flow successfully', async () => {
    vi.mocked(calculationTaskApi.retryCalculationTask).mockResolvedValue(mockTasks[0]!)

    const wrapper = mount(CalculationOperationsView, {
      global: {
        stubs: {
          Select: true,
          InputText: true,
          Button: true,
          Tag: true,
          Dialog: {
            props: ['visible'],
            template: '<div v-if="visible" class="mock-dialog"><slot /><slot name="footer" /></div>',
          },
          ServerPagination: true,
        },
      },
    })
    await flushPromises()

    // Find retry button in table
    const tableComponent = wrapper.findComponent({ name: 'CalculationTaskTable' })
    expect(tableComponent.exists()).toBe(true)
    tableComponent.vm.$emit('retry', mockTasks[0])
    await flushPromises()

    // Confirmation modal should be opened
    const retryModal = wrapper.findComponent({ name: 'RetryConfirmationModal' })
    expect(retryModal.props('visible')).toBe(true)

    // Confirm retry
    retryModal.vm.$emit('confirm')
    await flushPromises()

    expect(calculationTaskApi.retryCalculationTask).toHaveBeenCalledWith('token-office', 1048)
    expect(wrapper.text()).toContain('Đã yêu cầu retry task #CT-1048')
  })

  it('handles 409 conflict during retry by showing conflict banner and refreshing tasks', async () => {
    vi.mocked(calculationTaskApi.retryCalculationTask).mockRejectedValue(
      new ApiError(409, 'Conflict: task is no longer FAILED', { kind: 'conflict' }),
    )

    const wrapper = mount(CalculationOperationsView, {
      global: {
        stubs: {
          Select: true,
          InputText: true,
          Button: true,
          Tag: true,
          Dialog: {
            props: ['visible'],
            template: '<div v-if="visible" class="mock-dialog"><slot /><slot name="footer" /></div>',
          },
          ServerPagination: true,
        },
      },
    })
    await flushPromises()

    const tableComponent = wrapper.findComponent({ name: 'CalculationTaskTable' })
    tableComponent.vm.$emit('retry', mockTasks[0])
    await flushPromises()

    const retryModal = wrapper.findComponent({ name: 'RetryConfirmationModal' })
    retryModal.vm.$emit('confirm')
    await flushPromises()

    expect(wrapper.find('[data-testid="conflict-banner"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Task đã đổi trạng thái hoặc không còn FAILED')
  })

  it('shows 403 forbidden state when user has no permission for calculation tasks', async () => {
    saveAuthSession({
      accessToken: 'token-teacher',
      user: {
        id: 2,
        username: 'teacher1',
        roles: ['TEACHER'],
      },
    })

    const wrapper = mount(CalculationOperationsView, {
      global: {
        stubs: {
          Select: true,
          InputText: true,
          Button: true,
          Tag: true,
          Dialog: true,
          ServerPagination: true,
        },
      },
    })
    await flushPromises()

    expect(wrapper.find('[data-testid="tasks-forbidden"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('403 — Bạn không có quyền vận hành calculation task')
  })
})
