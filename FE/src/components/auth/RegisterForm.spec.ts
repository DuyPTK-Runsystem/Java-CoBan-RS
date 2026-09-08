import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import RegisterForm from './RegisterForm.vue'
import { primeVueStubs } from '@/test/stubs'

function mountForm() {
  return mount(RegisterForm, {
    global: { stubs: primeVueStubs },
  })
}

describe('RegisterForm', () => {
  it('rejects mismatched passwords', async () => {
    const wrapper = mountForm()

    await wrapper.get('#register-user-name').setValue('student01')
    await wrapper.get('#register-password').setValue('secret1')
    await wrapper.get('#register-confirm-password').setValue('secret2')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.text()).toContain('Passwords do not match.')
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('emits valid registration values', async () => {
    const wrapper = mountForm()

    await wrapper.get('#register-user-name').setValue('student01')
    await wrapper.get('#register-password').setValue('secret1')
    await wrapper.get('#register-confirm-password').setValue('secret1')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toEqual([[{
      userName: 'student01',
      password: 'secret1',
      confirmPassword: 'secret1',
    }]])
  })
})
