import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import SubjectApplicabilityDialog from './SubjectApplicabilityDialog.vue'

const subject = { id: 101, code: 'MAT', name: 'Toán', subjectType: 'ACADEMIC' as const, applicationScope: 'GRADE' as const, status: 'ACTIVE' as const }
const applicability = { id: 501, subjectId: 101, semesterId: 11, scopeType: 'GRADE' as const, gradeLevelId: 1, classId: null, status: 'ACTIVE' as const }

const dialogStub = { props: ['visible'], template: '<div v-if="visible"><slot /></div>' }
const selectStub = {
  props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder', 'invalid', 'fluid'],
  emits: ['update:modelValue'],
  template: '<select :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>',
}
const buttonStub = {
  props: ['label'],
  template: '<button v-bind="$attrs">{{ label }}</button>',
}
const tableStub = { template: '<div data-testid="applicability-table" />' }
const formAlertStub = { props: ['message'], template: '<div>{{ message }}</div>' }

function mountDialog(initialValue = null) {
  return mount(SubjectApplicabilityDialog, {
    props: {
      visible: true,
      mode: initialValue ? 'edit' : 'create',
      subject,
      initialValue,
      semesters: [{ id: 11, name: 'Học kỳ 1' }],
      grades: [{ id: 1, name: 'Khối 6' }],
    },
    global: {
      stubs: {
        Button: buttonStub,
        Dialog: dialogStub,
        FormAlert: formAlertStub,
        Select: selectStub,
        SubjectApplicabilityTable: tableStub,
      },
    },
  })
}

describe('SubjectApplicabilityDialog', () => {
  it('submits the initial applicability values in edit mode', async () => {
    const wrapper = mountDialog(applicability)

    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('save')).toEqual([[{
      semesterId: 11,
      scopeType: 'GRADE',
      gradeLevelId: 1,
      classId: null,
    }]])
  })

  it('shows validation errors before emitting an incomplete create', async () => {
    const wrapper = mountDialog()

    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('save')).toBeUndefined()
    expect(wrapper.text()).toContain('Học kỳ là bắt buộc.')
    expect(wrapper.text()).toContain('Khối áp dụng là bắt buộc.')
  })
})
