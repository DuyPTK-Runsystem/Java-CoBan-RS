import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import CalculationTaskTable from './CalculationTaskTable.vue'
import type { ResCalculationTaskDTO } from '@/types/calculationTask'

const selectStub = {
  props: ['modelValue', 'options'],
  emits: ['update:modelValue'],
  template: '<select :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option></select>',
}

const inputTextStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<input type="text" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}

const buttonStub = {
  props: ['label', 'disabled', 'loading'],
  emits: ['click'],
  template: '<button :disabled="disabled || loading" @click="$emit(\'click\')">{{ label }}</button>',
}

const tagStub = {
  props: ['value'],
  template: '<span>{{ value }}</span>',
}

const sampleTasks: ResCalculationTaskDTO[] = [
  {
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
  },
  {
    taskId: 1046,
    studentId: 102,
    studentCode: 'HS0002',
    academicYearId: 2,
    taskType: 'STUDENT_YEAR_RECALC',
    requestedVersion: 10,
    status: 'PENDING',
    attemptCount: 0,
    maxAttempts: 3,
    availableAt: null,
    lockedAt: null,
    workerId: null,
    lastError: null,
    createdAt: '2026-09-03T08:30:00',
    startedAt: null,
    completedAt: null,
  },
]

describe('CalculationTaskTable.vue', () => {
  it('renders task list rows with task ID, student code and status badge', () => {
    const wrapper = mount(CalculationTaskTable, {
      props: {
        tasks: sampleTasks,
        totalElements: 2,
        canRetry: true,
      },
      global: {
        stubs: {
          Select: selectStub,
          InputText: inputTextStub,
          Button: buttonStub,
          Tag: tagStub,
          ServerPagination: true,
        },
      },
    })

    expect(wrapper.text()).toContain('#CT-1048')
    expect(wrapper.text()).toContain('HS0001')
    expect(wrapper.text()).toContain('FAILED')
    expect(wrapper.text()).toContain('#CT-1046')
    expect(wrapper.text()).toContain('PENDING')
  })

  it('emits view-detail when Chi tiết button is clicked', async () => {
    const wrapper = mount(CalculationTaskTable, {
      props: {
        tasks: sampleTasks,
        canRetry: true,
      },
      global: {
        stubs: {
          Select: selectStub,
          InputText: inputTextStub,
          Button: buttonStub,
          Tag: tagStub,
          ServerPagination: true,
        },
      },
    })

    const detailButtons = wrapper.findAll('button').filter((b) => b.text() === 'Chi tiết')
    expect(detailButtons.length).toBe(2)
    await detailButtons[0]?.trigger('click')

    expect(wrapper.emitted('view-detail')).toBeTruthy()
    expect(wrapper.emitted('view-detail')?.[0]?.[0]).toEqual(sampleTasks[0])
  })

  it('renders retry button only for FAILED tasks when canRetry is true', async () => {
    const wrapper = mount(CalculationTaskTable, {
      props: {
        tasks: sampleTasks,
        canRetry: true,
      },
      global: {
        stubs: {
          Select: selectStub,
          InputText: inputTextStub,
          Button: buttonStub,
          Tag: tagStub,
          ServerPagination: true,
        },
      },
    })

    const retryButtons = wrapper.findAll('[data-testid="retry-single-button"]')
    expect(retryButtons.length).toBe(1)
    await retryButtons[0]?.trigger('click')

    expect(wrapper.emitted('retry')).toBeTruthy()
    expect(wrapper.emitted('retry')?.[0]?.[0]).toEqual(sampleTasks[0])
  })

  it('hides retry button when canRetry is false', () => {
    const wrapper = mount(CalculationTaskTable, {
      props: {
        tasks: sampleTasks,
        canRetry: false,
      },
      global: {
        stubs: {
          Select: selectStub,
          InputText: inputTextStub,
          Button: buttonStub,
          Tag: tagStub,
          ServerPagination: true,
        },
      },
    })

    const retryButtons = wrapper.findAll('[data-testid="retry-single-button"]')
    expect(retryButtons.length).toBe(0)
  })

  it('emits retry-all-failed when bulk retry button is clicked', async () => {
    const wrapper = mount(CalculationTaskTable, {
      props: {
        tasks: sampleTasks,
        canRetry: true,
      },
      global: {
        stubs: {
          Select: selectStub,
          InputText: inputTextStub,
          Button: buttonStub,
          Tag: tagStub,
          ServerPagination: true,
        },
      },
    })

    const bulkBtn = wrapper.find('[data-testid="bulk-retry-button"]')
    expect(bulkBtn.attributes('disabled')).toBeUndefined()
    await bulkBtn.trigger('click')

    expect(wrapper.emitted('retry-all-failed')).toBeTruthy()
  })

  it('disables bulk retry button when there are no failed tasks', () => {
    const wrapper = mount(CalculationTaskTable, {
      props: {
        tasks: [sampleTasks[1]!], // only PENDING task
        canRetry: true,
      },
      global: {
        stubs: {
          Select: selectStub,
          InputText: inputTextStub,
          Button: buttonStub,
          Tag: tagStub,
          ServerPagination: true,
        },
      },
    })

    const bulkBtn = wrapper.find('[data-testid="bulk-retry-button"]')
    expect(bulkBtn.attributes('disabled')).toBeDefined()
  })

  it('shows empty state when task list is empty and not loading', () => {
    const wrapper = mount(CalculationTaskTable, {
      props: {
        tasks: [],
        loading: false,
      },
      global: {
        stubs: {
          Select: selectStub,
          InputText: inputTextStub,
          Button: buttonStub,
          Tag: tagStub,
          ServerPagination: true,
        },
      },
    })

    expect(wrapper.text()).toContain('Không có calculation task phù hợp')
  })
})
