import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import RetryConfirmationModal from './RetryConfirmationModal.vue'
import type { ResCalculationTaskDTO } from '@/types/calculationTask'

const sampleTask: ResCalculationTaskDTO = {
  taskId: 1048,
  studentId: 101,
  studentCode: 'HS0001',
  academicYearId: 2,
  taskType: 'STUDENT_YEAR_RECALC',
  requestedVersion: 11,
  status: 'FAILED',
  attemptCount: 3,
  maxAttempts: 3,
  availableAt: null,
  lockedAt: null,
  workerId: null,
  lastError: 'Timeout',
  createdAt: '2026-09-03T09:12:44',
  startedAt: null,
  completedAt: null,
}

describe('RetryConfirmationModal.vue', () => {
  it('renders single retry confirmation with task ID and warning', () => {
    const wrapper = mount(RetryConfirmationModal, {
      props: {
        visible: true,
        mode: 'single',
        task: sampleTask,
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

    expect(wrapper.text()).toContain('Xác nhận retry task?')
    expect(wrapper.text()).toContain('#CT-1048')
    expect(wrapper.text()).toContain('HS0001')
    expect(wrapper.text()).toContain('PENDING')
  })

  it('renders bulk retry confirmation with failed task count', () => {
    const wrapper = mount(RetryConfirmationModal, {
      props: {
        visible: true,
        mode: 'bulk',
        failedCount: 4,
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

    expect(wrapper.text()).toContain('Retry tất cả failed?')
    expect(wrapper.text()).toContain('4')
    expect(wrapper.find('[data-testid="confirm-retry-btn"]').text()).toContain('Retry 4 tasks')
  })

  it('emits confirm when confirm button is clicked', async () => {
    const wrapper = mount(RetryConfirmationModal, {
      props: {
        visible: true,
        mode: 'single',
        task: sampleTask,
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

    await wrapper.find('[data-testid="confirm-retry-btn"]').trigger('click')
    expect(wrapper.emitted('confirm')).toBeTruthy()
  })

  it('emits cancel and update:visible false when cancel button clicked', async () => {
    const wrapper = mount(RetryConfirmationModal, {
      props: {
        visible: true,
        mode: 'single',
        task: sampleTask,
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

    const cancelBtn = wrapper.findAll('button').find((b) => b.text() === 'Hủy')
    await cancelBtn?.trigger('click')

    expect(wrapper.emitted('update:visible')?.[0]).toEqual([false])
    expect(wrapper.emitted('cancel')).toBeTruthy()
  })
})
