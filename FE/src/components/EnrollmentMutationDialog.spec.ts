import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import EnrollmentMutationDialog from './EnrollmentMutationDialog.vue'

const buttonStub = {
  props: ['label', 'disabled', 'loading'],
  emits: ['click'],
  template: '<button :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
}
const dialogStub = {
  props: ['visible'],
  template: '<div v-if="visible"><slot /></div>',
}
const datePickerStub = { props: ['modelValue'], template: '<input />' }
const formAlertStub = { props: ['message'], template: '<div>{{ message }}</div>' }

describe('EnrollmentMutationDialog', () => {
  it('emits a bulk request with selected technical student ids only', async () => {
    const wrapper = mount(EnrollmentMutationDialog, {
      props: {
        visible: true,
        mode: 'bulk',
        students: [
          { studentId: 11, studentCode: 'HS011', studentName: 'Nguyễn An' },
          { studentId: 12, studentCode: 'HS012', studentName: 'Trần Bình' },
        ],
      },
      global: { stubs: { Button: buttonStub, DatePicker: datePickerStub, Dialog: dialogStub, FormAlert: formAlertStub } },
    })

    await wrapper.findAll('button').at(-1)?.trigger('click')

    expect(wrapper.emitted('submit')).toEqual([[{ studentIds: [11, 12], enrolledAt: '' }]])
  })

  it('blocks submit when no student is selected', async () => {
    const wrapper = mount(EnrollmentMutationDialog, {
      props: { visible: true, mode: 'single', students: [] },
      global: { stubs: { Button: buttonStub, DatePicker: datePickerStub, Dialog: dialogStub, FormAlert: formAlertStub } },
    })

    await wrapper.findAll('button').at(-1)?.trigger('click')

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.text()).toContain('Cần chọn ít nhất một học sinh.')
  })
})
