import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import StudentForm from './StudentForm.vue'
import { primeVueStubs } from '@/test/stubs'

const inputNumberStub = { template: '<input />' }

function mountForm(mode: 'add' | 'edit' = 'add', averageScore = 6.7) {
  return mount(StudentForm, {
    props: {
      mode,
      initialValue: {
        studentCode: mode === 'edit' ? 'STU1234567' : '',
        studentName: 'John Doe',
        dateOfBirth: new Date(2000, 7, 19),
        address: 'Ho Chi Minh City',
        averageScore,
      },
    },
    global: {
      stubs: {
        ...primeVueStubs,
        InputNumber: inputNumberStub,
      },
    },
  })
}

describe('StudentForm', () => {
  it('normalizes a numeric student code when the Add textbox loses focus', async () => {
    const wrapper = mountForm()
    const studentCode = wrapper.get('#student-code')

    await studentCode.setValue('123456')
    await studentCode.trigger('blur')

    expect((studentCode.element as HTMLInputElement).value).toBe('STU0123456')
    expect(wrapper.text()).not.toContain('Use the format STU followed by exactly 7 digits.')
  })

  it('does not show a format warning for a valid partial Student code while typing', async () => {
    const wrapper = mountForm()

    await wrapper.get('#student-code').setValue('123456')
    expect(wrapper.text()).not.toContain('Use the format STU followed by exactly 7 digits.')

    await wrapper.get('#student-code').setValue('STU123')
    expect(wrapper.text()).not.toContain('Use the format STU followed by exactly 7 digits.')
  })

  it('shows a format warning immediately for more than seven digits', async () => {
    const wrapper = mountForm()

    await wrapper.get('#student-code').setValue('STU12345678')

    expect(wrapper.text()).toContain('Use the format STU followed by exactly 7 digits.')
  })

  it('disables the Student code and Generate code controls in Edit mode', () => {
    const wrapper = mountForm('edit')

    expect(wrapper.get('#student-code').attributes('disabled')).toBeDefined()
    expect(wrapper.get('button').attributes('disabled')).toBeDefined()
  })

  it.each([0, 10])('emits Save for the Average score boundary %s', async (averageScore) => {
    const wrapper = mountForm('add', averageScore)

    await wrapper.get('#student-code').setValue('STU1234567')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('save')).toHaveLength(1)
  })

  it.each([-0.01, 10.01])('rejects an out-of-range Average score %s', async (averageScore) => {
    const wrapper = mountForm('add', averageScore)

    await wrapper.get('form').trigger('submit')

    expect(wrapper.text()).toContain('Average score must be between 0 and 10.')
    expect(wrapper.emitted('save')).toBeUndefined()
  })
})
