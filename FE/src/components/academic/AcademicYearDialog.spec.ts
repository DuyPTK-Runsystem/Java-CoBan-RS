import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import AcademicYearDialog from './AcademicYearDialog.vue'
import ButtonStub from '@/test/stubs/ButtonStub.vue'

const dialogStub = { props: ['visible'], template: '<div><slot /></div>' }
const inputTextStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" :value="modelValue ?? \'\'" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}
const datePickerStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: `<input v-bind="$attrs" :value="modelValue ? modelValue.toISOString().slice(0, 10) : ''" @input="$emit('update:modelValue', new Date($event.target.value + 'T00:00:00'))" />`,
}
const selectStub = {
  props: ['modelValue', 'options'],
  emits: ['update:modelValue'],
  template: '<select v-bind="$attrs" :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="option in options" :key="option.value" :value="option.value">{{ option.label }}</option></select>',
}
const textareaStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<textarea v-bind="$attrs" :value="modelValue ?? \'\'" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}

function mountDialog() {
  return mount(AcademicYearDialog, {
    props: { visible: true },
    global: {
      stubs: {
        Dialog: dialogStub,
        InputText: inputTextStub,
        DatePicker: datePickerStub,
        Select: selectStub,
        Textarea: textareaStub,
        Button: ButtonStub,
        StatusTag: true,
      },
    },
  })
}

describe('AcademicYearDialog', () => {
  it('trims the academic year before emitting a valid save', async () => {
    const wrapper = mountDialog()

    await wrapper.get('#academic-year-code').setValue(' 2026-2027 ')
    await wrapper.get('#academic-year-start').setValue('2026-09-01')
    await wrapper.get('#academic-year-end').setValue('2027-05-31')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('save')?.[0]?.[0]).toMatchObject({ code: '2026-2027' })
  })

  it('rejects academic year codes with characters outside digits, spaces and hyphens', async () => {
    const wrapper = mountDialog()

    await wrapper.get('#academic-year-code').setValue('2026/2027')
    await wrapper.get('#academic-year-start').setValue('2026-09-01')
    await wrapper.get('#academic-year-end').setValue('2027-05-31')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.text()).toContain('Năm học chỉ được gồm chữ số, dấu cách và dấu gạch ngang.')
    expect(wrapper.emitted('save')).toBeUndefined()
  })
})
