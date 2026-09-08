import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import StudentForm from './StudentForm.vue'
import { primeVueStubs } from '@/test/stubs'

const inputNumberStub = { template: '<input />' }

function mountForm(mode: 'add' | 'edit' = 'add') {
  return mount(StudentForm, {
    props: {
      mode,
      initialValue: {
        studentCode: mode === 'edit' ? 'STU1234567' : '',
        studentName: 'John Doe',
        dateOfBirth: new Date(2000, 7, 19),
        address: 'Ho Chi Minh City',
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
    expect(wrapper.text()).not.toContain('Mã học sinh phải bắt đầu bằng STU và có đúng 7 chữ số phía sau.')
  })

  it('does not show a format warning for a valid partial Mã học sinh while typing', async () => {
    const wrapper = mountForm()

    await wrapper.get('#student-code').setValue('123456')
    expect(wrapper.text()).not.toContain('Mã học sinh phải bắt đầu bằng STU và có đúng 7 chữ số phía sau.')

    await wrapper.get('#student-code').setValue('STU123')
    expect(wrapper.text()).not.toContain('Mã học sinh phải bắt đầu bằng STU và có đúng 7 chữ số phía sau.')
  })

  it('shows a format warning immediately for more than seven digits', async () => {
    const wrapper = mountForm()

    await wrapper.get('#student-code').setValue('STU12345678')

    expect(wrapper.text()).toContain('Mã học sinh phải bắt đầu bằng STU và có đúng 7 chữ số phía sau.')
  })

  it('disables the Mã học sinh and Tạo mã controls in Edit mode', () => {
    const wrapper = mountForm('edit')

    expect(wrapper.get('#student-code').attributes('disabled')).toBeDefined()
    expect(wrapper.get('button').attributes('disabled')).toBeDefined()
  })

  it('emits Save without legacy average score input', async () => {
    const wrapper = mountForm('add')

    await wrapper.get('#student-code').setValue('STU1234567')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('save')).toHaveLength(1)
  })

  it('does not render the legacy average score input', () => {
    const wrapper = mountForm('add')

    expect(wrapper.find('#student-score').exists()).toBe(false)
  })

  it('provisions an account by default and explains the generated username', () => {
    const wrapper = mountForm('add')

    expect(wrapper.get('#provision-account').attributes('checked')).toBeDefined()
    expect(wrapper.text()).toContain('Cấp tài khoản đăng nhập cho học sinh')
    expect(wrapper.text()).toContain('Mật khẩu mặc định: 12345678')
    expect(wrapper.text()).toContain('hệ thống tự sinh username và sẽ hiển thị khi tạo thành công')
  })
})
