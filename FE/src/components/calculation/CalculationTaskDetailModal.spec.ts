import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import CalculationTaskDetailModal from './CalculationTaskDetailModal.vue'
import type { ResCalculationTaskDTO } from '@/types/calculationTask'

const sampleFailedTask: ResCalculationTaskDTO = {
  taskId: 1048,
  studentId: 101,
  studentCode: 'HS0001',
  academicYearId: 2,
  taskType: 'STUDENT_YEAR_RECALC',
  requestedVersion: 11,
  status: 'FAILED',
  attemptCount: 3,
  maxAttempts: 3,
  availableAt: '2026-09-03T09:15:12',
  lockedAt: null,
  workerId: 'worker-1',
  lastError: 'Timeout khi đọc transcript HK2',
  createdAt: '2026-09-03T09:12:44',
  startedAt: '2026-09-03T09:12:45',
  completedAt: '2026-09-03T09:15:12',
}

const samplePendingTask: ResCalculationTaskDTO = {
  ...sampleFailedTask,
  taskId: 1046,
  status: 'PENDING',
  attemptCount: 0,
  lastError: null,
}

describe('CalculationTaskDetailModal.vue', () => {
  it('renders modal with task details and error when visible is true', () => {
    const wrapper = mount(CalculationTaskDetailModal, {
      props: {
        visible: true,
        task: sampleFailedTask,
        canRetry: true,
      },
      global: {
        stubs: {
          Dialog: {
            props: ['visible', 'header'],
            template: '<div v-if="visible" class="mock-dialog"><div class="header">{{ header }}</div><slot /><slot name="footer" /></div>',
          },
        },
      },
    })

    expect(wrapper.text()).toContain('Chi tiết Task #CT-1048')
    expect(wrapper.text()).toContain('HS0001')
    expect(wrapper.text()).toContain('Timeout khi đọc transcript HK2')
    expect(wrapper.text()).toContain('3 / 3')
  })

  it('renders retry button for FAILED task and emits retry event on click', async () => {
    const wrapper = mount(CalculationTaskDetailModal, {
      props: {
        visible: true,
        task: sampleFailedTask,
        canRetry: true,
      },
      global: {
        stubs: {
          Dialog: {
            props: ['visible', 'header'],
            template: '<div v-if="visible" class="mock-dialog"><div class="header">{{ header }}</div><slot /><slot name="footer" /></div>',
          },
        },
      },
    })

    const retryBtn = wrapper.find('[data-testid="modal-retry-button"]')
    expect(retryBtn.exists()).toBe(true)
    await retryBtn.trigger('click')

    expect(wrapper.emitted('retry')).toBeTruthy()
    expect(wrapper.emitted('retry')?.[0]?.[0]).toEqual(sampleFailedTask)
  })

  it('does not render retry button for non-FAILED task', () => {
    const wrapper = mount(CalculationTaskDetailModal, {
      props: {
        visible: true,
        task: samplePendingTask,
        canRetry: true,
      },
      global: {
        stubs: {
          Dialog: {
            props: ['visible', 'header'],
            template: '<div v-if="visible" class="mock-dialog"><div class="header">{{ header }}</div><slot /><slot name="footer" /></div>',
          },
        },
      },
    })

    const retryBtn = wrapper.find('[data-testid="modal-retry-button"]')
    expect(retryBtn.exists()).toBe(false)
  })

  it('emits close and update:visible false when close button clicked', async () => {
    const wrapper = mount(CalculationTaskDetailModal, {
      props: {
        visible: true,
        task: sampleFailedTask,
      },
      global: {
        stubs: {
          Dialog: {
            props: ['visible', 'header'],
            template: '<div v-if="visible" class="mock-dialog"><div class="header">{{ header }}</div><slot /><slot name="footer" /></div>',
          },
        },
      },
    })

    const closeBtn = wrapper.findAll('button').find((b) => b.text() === 'Đóng')
    expect(closeBtn).toBeDefined()
    await closeBtn?.trigger('click')

    expect(wrapper.emitted('update:visible')?.[0]).toEqual([false])
    expect(wrapper.emitted('close')).toBeTruthy()
  })
})
