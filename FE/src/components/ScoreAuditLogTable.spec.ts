import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ScoreAuditLogTable from './ScoreAuditLogTable.vue'
import type { ResScoreAuditLogDTO } from '@/types/scoreAudit'

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

const sampleLogs: ResScoreAuditLogDTO[] = [
  {
    auditLogId: 101,
    actorUserId: 2,
    actorUsername: 'academic.office',
    action: 'CALCULATION_TASK_RETRIED',
    entityType: 'CALCULATION_TASK',
    entityId: '1048',
    beforeData: { status: 'FAILED', attempt: 3 },
    afterData: { status: 'PENDING', attempt: 0 },
    requestId: 'req-7a91',
    ipAddress: '127.0.0.1',
    occurredAt: '2026-09-03T09:16:04',
  },
  {
    auditLogId: 102,
    actorUserId: 5,
    actorUsername: 'teacher.math',
    action: 'SCORE_UPDATED',
    entityType: 'STUDENT_SCORE',
    entityId: '882',
    beforeData: { score: 4.0 },
    afterData: { score: 6.0 },
    requestId: 'req-7a20',
    ipAddress: '127.0.0.1',
    occurredAt: '2026-09-03T08:46:10',
  },
]

describe('ScoreAuditLogTable.vue', () => {
  it('renders table rows with actor, action, entity, and safely formatted json', () => {
    const wrapper = mount(ScoreAuditLogTable, {
      props: {
        logs: sampleLogs,
        totalElements: 2,
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

    expect(wrapper.text()).toContain('academic.office')
    expect(wrapper.text()).toContain('CALCULATION_TASK_RETRIED')
    expect(wrapper.text()).toContain('CALCULATION_TASK · #1048')
    expect(wrapper.text()).toContain('req-7a91')
    expect(wrapper.text()).toContain('"status":"FAILED"')
    expect(wrapper.text()).toContain('"status":"PENDING"')
  })

  it('renders read-only disclaimer and no edit/delete buttons', () => {
    const wrapper = mount(ScoreAuditLogTable, {
      props: {
        logs: sampleLogs,
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

    expect(wrapper.text()).toContain('Read-only audit')
    expect(wrapper.text()).not.toContain('Chỉnh sửa')
    expect(wrapper.text()).not.toContain('Xóa')
    expect(wrapper.text()).not.toContain('Retry')
  })

  it('shows empty state when logs list is empty', () => {
    const wrapper = mount(ScoreAuditLogTable, {
      props: {
        logs: [],
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

    expect(wrapper.text()).toContain('Không tìm thấy nhật ký audit phù hợp')
  })

  it('emits refresh when refresh button is clicked', async () => {
    const wrapper = mount(ScoreAuditLogTable, {
      props: {
        logs: sampleLogs,
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

    const refreshBtn = wrapper.findAll('button').find((b) => b.text() === 'Làm mới')
    await refreshBtn?.trigger('click')

    expect(wrapper.emitted('refresh')).toBeTruthy()
  })
})
