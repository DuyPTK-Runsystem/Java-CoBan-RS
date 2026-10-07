import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TimetableAgentReview from './TimetableAgentReview.vue'
import type { TimetableAgentProposal, TimetableAgentReceipt } from '@/types/timetableAgent'

const PrimeVueButtonStub = defineComponent({
  name: 'PrimeVueButtonStub',
  props: {
    label: { type: String, default: '' },
    disabled: { type: Boolean, default: false },
    loading: { type: Boolean, default: false },
  },
  emits: ['click'],
  template: '<button :disabled="disabled" @click="$emit(\'click\', $event)">{{ label }}</button>',
})

const entry = { assignmentId: 501, periodId: 701, functionalRoomId: null, validFrom: '2026-10-05', validTo: '2026-12-31' }
const proposal: TimetableAgentProposal = {
  proposalId: 'proposal-1',
  proposalVersion: 2,
  proposalHash: 'hash-2',
  targetRevisionId: 42,
  expectedVersion: 7,
  status: 'READY_FOR_REVIEW',
  snapshotId: 'snapshot-1',
  expiresAt: '2026-12-31T12:00:00Z',
  entries: [entry],
  issues: [{ code: 'LOAD_WARNING', severity: 'WARNING', path: 'entries[0]', message: 'Kiểm tra tải giáo viên.' }],
  explanation: 'Đã sửa theo phản hồi và bổ sung đủ số tiết yêu cầu.',
  diff: { added: [entry], removed: [], unchanged: [] },
  capabilities: { canGenerate: true, canApprove: true, canExecute: false },
}

function mountReview(overrides: Record<string, unknown> = {}) {
  return mount(TimetableAgentReview, {
    props: {
      proposal,
      phase: 'idle',
      periods: [{ id: 701, dayOfWeek: 1, session: 'MORNING', periodIndex: 1, periodName: 'Tiết 1', startTime: '07:00', endTime: '07:45' }],
      assignments: [{ id: 501, classId: 11, className: '10A1', subjectName: 'Toán', teacherName: 'Cô An' }],
      receipt: null,
      canSave: false,
      ...overrides,
    },
    global: { stubs: { Button: PrimeVueButtonStub } },
  })
}

describe('TimetableAgentReview', () => {
  it('shows warnings and emits one save action when the workspace grants that capability', async () => {
    const wrapper = mountReview({ canSave: true })

    expect(wrapper.text()).toContain('Kiểm tra tải giáo viên.')
    expect(wrapper.text()).not.toContain('Đã sửa theo phản hồi và bổ sung đủ số tiết yêu cầu.')
    expect(wrapper.text()).toContain('Thứ Hai Sáng tiết 1')
    await wrapper.get('button').trigger('click')

    expect(wrapper.emitted('save')).toHaveLength(1)
    expect(wrapper.text()).toContain('Lưu gợi ý sẽ duyệt phương án rồi lưu vào bản nháp.')
  })

  it('keeps the model explanation when a proposal still needs attention', () => {
    const wrapper = mountReview({ proposal: { ...proposal, status: 'CONFLICTS' } })

    expect(wrapper.text()).toContain('Đã sửa theo phản hồi và bổ sung đủ số tiết yêu cầu.')
  })

  it('offers recovery after an uncertain response and keeps execution disabled', async () => {
    const wrapper = mountReview({
      phase: 'response-lost',
      pendingRecovery: true,
      canSave: false,
    })
    const recoveryButton = wrapper.findAll('button').find((button) => button.text().includes('Kiểm tra trạng thái lưu'))
    const saveButton = wrapper.findAll('button').find((button) => button.text().includes('Lưu gợi ý'))

    expect(recoveryButton).toBeDefined()
    expect(saveButton?.attributes('disabled')).toBeDefined()
    await recoveryButton!.trigger('click')

    expect(wrapper.emitted('recover')).toHaveLength(1)
    expect(wrapper.emitted('save')).toBeUndefined()
  })

  it('offers same-action retry only when the workspace grants it', async () => {
    const wrapper = mountReview({
      phase: 'response-lost',
      pendingRecovery: true,
      canRetryPending: true,
    })
    const retryButton = wrapper.findAll('button').find((button) => button.text().includes('Thử lại yêu cầu lưu trước'))

    expect(retryButton).toBeDefined()
    await retryButton!.trigger('click')

    expect(wrapper.emitted('retryPending')).toHaveLength(1)
    expect(wrapper.emitted('save')).toBeUndefined()
  })

  it('treats the server receipt as success and routes refresh through reload', async () => {
    const receipt: TimetableAgentReceipt = {
      actionId: 'action-1',
      proposalId: 'proposal-1',
      targetRevisionId: 42,
      newVersion: 8,
      savedEntryCount: 3,
      committedAt: '2026-10-01T10:00:00Z',
      status: 'SAVED_DRAFT',
    }
    const wrapper = mountReview({ receipt, canSave: true })

    expect(wrapper.text()).toContain('Đã lưu bản nháp theo kết quả của máy chủ.')
    expect(wrapper.text()).toContain('phiên bản 8 · 3 tiết đã lưu')
    expect(wrapper.findAll('button').some((button) => button.text().includes('Lưu gợi ý'))).toBe(false)
    await wrapper.get('button').trigger('click')

    expect(wrapper.emitted('reload')).toHaveLength(1)
    expect(wrapper.emitted('save')).toBeUndefined()
  })
})
