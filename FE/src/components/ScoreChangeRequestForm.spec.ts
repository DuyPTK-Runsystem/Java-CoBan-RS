import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ScoreChangeRequestForm from './ScoreChangeRequestForm.vue'

const columns = [{ id: 4, scorebookId: 12, assessmentType: 'KTTT' as const, columnNo: 1, columnName: 'Thường xuyên 1', weightFactor: null, required: true, status: 'ACTIVE' as const }]

const inputStub = {
  props: ['modelValue', 'disabled', 'invalid'],
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" :value="modelValue ?? \'\'" :disabled="disabled" @input="$emit(\'update:modelValue\', $event.target.value)">',
}
const inputNumberStub = {
  props: ['modelValue', 'disabled', 'invalid'],
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" :value="modelValue ?? \'\'" :disabled="disabled" @input="$emit(\'update:modelValue\', $event.target.value === \'\' ? null : Number($event.target.value))">',
}
const textareaStub = {
  props: ['modelValue', 'disabled', 'invalid'],
  emits: ['update:modelValue'],
  template: '<textarea v-bind="$attrs" :value="modelValue ?? \'\'" :disabled="disabled" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}
const selectStub = {
  inheritAttrs: false,
  props: ['modelValue', 'disabled', 'options'],
  emits: ['update:modelValue'],
  template: '<select :id="$attrs.id" :value="modelValue" :disabled="disabled" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>',
}
const buttonStub = {
  props: ['label', 'disabled', 'loading', 'type'],
  emits: ['click'],
  template: '<button :type="type" :disabled="disabled || loading" @click="$emit(\'click\')">{{ label }}</button>',
}

function mountForm(context: Record<string, unknown>) {
  return mount(ScoreChangeRequestForm, {
    props: { columns, context },
    global: { stubs: { Button: buttonStub, InputNumber: inputNumberStub, InputText: inputStub, Select: selectStub, Textarea: textareaStub } },
  })
}

describe('ScoreChangeRequestForm', () => {
  it('renders score context as readonly summary fields', () => {
    const wrapper = mountForm({ studentCode: 'HS-001', studentName: 'Nguyễn Minh An', columnId: 4, columnName: 'Thường xuyên 1', currentStatus: 'SCORED', currentValue: 6.5 })

    expect(wrapper.text()).toContain('Nguyễn Minh An (HS-001)')
    expect(wrapper.text()).toContain('Thường xuyên 1')
    expect(wrapper.text()).toContain('6.5')
    expect(wrapper.get('#score-change-value').attributes('value')).toBe('')
    expect(wrapper.find('#score-change-student').exists()).toBe(false)
    expect(wrapper.find('#score-change-column').exists()).toBe(false)
  })

  it('prefills the proposal received from score entry', () => {
    const wrapper = mountForm({
      studentCode: 'HS-001',
      columnId: 4,
      currentStatus: 'SCORED',
      currentValue: 6.5,
      proposedStatus: 'SCORED',
      proposedValue: 8,
      reason: 'Điều chỉnh theo phiếu chấm.',
    })

    expect(wrapper.get('#score-change-value').attributes('value')).toBe('8')
    expect(wrapper.get('#score-change-reason').element.value).toBe('Điều chỉnh theo phiếu chấm.')
  })

  it('requires a reason when the proposed value is present', async () => {
    const wrapper = mountForm({ studentCode: 'HS-001', columnId: 4, columnName: 'Thường xuyên 1', currentStatus: 'SCORED', currentValue: 6.5 })

    await wrapper.get('form').trigger('submit')

    expect(wrapper.text()).toContain('Vui lòng nêu lý do sửa điểm.')
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('requires a value for a SCORED request', async () => {
    const wrapper = mountForm({ studentCode: 'HS-001', columnId: 4, columnName: 'Thường xuyên 1', currentStatus: 'SCORED', currentValue: null })

    await wrapper.get('#score-change-reason').setValue('Điều chỉnh theo phiếu chấm.')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.text()).toContain('Vui lòng nhập điểm đề xuất.')
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('emits the valid context-based request payload', async () => {
    const wrapper = mountForm({ studentCode: 'HS-001', columnId: 4, columnName: 'Thường xuyên 1', currentStatus: 'SCORED', currentValue: 6.5 })

    await wrapper.get('#score-change-value').setValue('8.5')
    await wrapper.get('#score-change-reason').setValue('Nhập nhầm điểm theo phiếu chấm.')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toEqual([[{
      assessmentColumnId: 4,
      studentCode: 'HS-001',
      proposedStatus: 'SCORED',
      proposedValue: 8.5,
      reason: 'Nhập nhầm điểm theo phiếu chấm.',
    }]])
  })

  it('emits null proposedValue for a non-SCORED request', async () => {
    const wrapper = mountForm({ studentCode: 'HS-002', columnId: 4, columnName: 'Thường xuyên 1', currentStatus: 'ABSENT', currentValue: null })

    expect(wrapper.get('#score-change-value').attributes('disabled')).toBeDefined()
    await wrapper.get('#score-change-reason').setValue('Học sinh vắng có xác nhận.')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toEqual([[{
      assessmentColumnId: 4,
      studentCode: 'HS-002',
      proposedStatus: 'ABSENT',
      proposedValue: null,
      reason: 'Học sinh vắng có xác nhận.',
    }]])
  })
})
