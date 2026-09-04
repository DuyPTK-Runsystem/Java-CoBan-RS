import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import TransferEnrollmentDialog from './TransferEnrollmentDialog.vue'

const buttonStub = {
  props: ['label', 'disabled', 'loading'],
  emits: ['click'],
  template: '<button :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
}
const dialogStub = { props: ['visible'], template: '<div v-if="visible"><slot /></div>' }
const selectStub = {
  props: ['modelValue', 'options'],
  emits: ['update:modelValue'],
  template: '<select :value="modelValue" @change="$emit(\'update:modelValue\', Number($event.target.value))"><option v-for="option in options" :key="option.id" :value="option.id">{{ option.classCode }}</option></select>',
}
const datePickerStub = { props: ['modelValue'], template: '<input />' }
const textareaStub = { props: ['modelValue'], emits: ['update:modelValue'], template: '<textarea :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />' }
const formAlertStub = { props: ['message'], template: '<div>{{ message }}</div>' }

describe('TransferEnrollmentDialog', () => {
  const classes = [
    { id: 101, academicYearId: 1, gradeLevelId: 1, classCode: '6A1', className: null, capacity: 35, status: 'ACTIVE' as const },
    { id: 102, academicYearId: 1, gradeLevelId: 1, classCode: '6A2', className: null, capacity: 35, status: 'PLANNED' as const },
    { id: 103, academicYearId: 1, gradeLevelId: 1, classCode: '6A3', className: null, capacity: 35, status: 'CLOSED' as const },
  ]

  it('filters current and closed classes and emits local date-time payload', async () => {
    const wrapper = mount(TransferEnrollmentDialog, {
      props: {
        visible: true,
        currentClassId: 101,
        targetClasses: classes,
        student: { studentId: 11, studentCode: 'HS011', studentName: 'Nguyễn An', enrollmentId: 77 },
      },
      global: { stubs: { Button: buttonStub, DatePicker: datePickerStub, Dialog: dialogStub, FormAlert: formAlertStub, Select: selectStub, Textarea: textareaStub } },
    })

    expect(wrapper.findAll('option').map((option) => option.text())).toEqual(['6A2'])
    await wrapper.find('select').setValue('102')
    await wrapper.find('form').trigger('submit')

    const submitted = wrapper.emitted('submit')?.[0]?.[0] as { targetClassId: number; effectiveAt: string; reason: string }
    expect(submitted.targetClassId).toBe(102)
    expect(submitted.effectiveAt).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/)
    expect(submitted.reason).toBe('')
  })

  it('shows a validation error when the target class is missing', async () => {
    const wrapper = mount(TransferEnrollmentDialog, {
      props: { visible: true, currentClassId: 101, targetClasses: classes },
      global: { stubs: { Button: buttonStub, DatePicker: datePickerStub, Dialog: dialogStub, FormAlert: formAlertStub, Select: selectStub, Textarea: textareaStub } },
    })

    await wrapper.find('form').trigger('submit')

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.text()).toContain('Lớp đích là bắt buộc.')
  })
})
