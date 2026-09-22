import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import LoginForm from './LoginForm.vue'
import { primeVueStubs } from '@/test/stubs'

function mountForm() {
  return mount(LoginForm, {
    global: { stubs: primeVueStubs },
  })
}

describe('LoginForm', () => {
  it('shows required errors and does not submit empty values', async () => {
    const wrapper = mountForm()

    await wrapper.get('form').trigger('submit')

    expect(wrapper.text()).toContain('User name is required.')
    expect(wrapper.text()).toContain('Password is required.')
    expect(wrapper.emitted('submit')).toBeUndefined()
  })

  it('emits valid credentials', async () => {
    const wrapper = mountForm()

    await wrapper.get('#login-user-name').setValue('student01')
    await wrapper.get('#login-password').setValue('secret1')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toEqual([[{
      userName: 'student01',
      password: 'secret1',
    }]])
  })
})
