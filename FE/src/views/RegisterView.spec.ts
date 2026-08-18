import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession } from '@/services/authSession'
import router from '@/router'

import RegisterView from './RegisterView.vue'

const registerMock = vi.hoisted(() => vi.fn())

vi.mock('@/services/userApi', () => ({ register: registerMock }))

const registerFormStub = {
  emits: ['submit', 'back'],
  template: `
    <button
      data-testid="register-submit"
      @click="$emit('submit', { userName: 'student01', password: 'secret1', confirmPassword: 'secret1' })"
    >
      Submit
    </button>
  `,
}

const dialogStub = {
  props: ['visible', 'header'],
  template: '<section v-if="visible" data-testid="status-popup"><h2>{{ header }}</h2><slot /><slot name="footer" /></section>',
}

const buttonStub = {
  props: ['label'],
  emits: ['click'],
  template: '<button :data-testid="label === \'Close\' ? \'close-popup\' : \'button\'" @click="$emit(\'click\')">{{ label }}</button>',
}

function mountView() {
  return mount(RegisterView, {
    global: {
      plugins: [router],
      stubs: {
        RegisterForm: registerFormStub,
        Dialog: dialogStub,
        Button: buttonStub,
      },
    },
  })
}

describe('RegisterView status popup', () => {
  beforeEach(async () => {
    clearAuthSession()
    registerMock.mockReset()
    await router.push('/register')
  })

  afterEach(() => {
    clearAuthSession()
  })

  it('shows a success popup and only navigates to login after Close', async () => {
    registerMock.mockResolvedValue({ id: 1, username: 'student01' })
    const wrapper = mountView()

    await wrapper.get('[data-testid="register-submit"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="status-popup"]').text()).toContain('Registration successful')
    expect(router.currentRoute.value.name).toBe('register')

    await (wrapper.vm as unknown as { closePopup: () => Promise<void> }).closePopup()

    expect(router.currentRoute.value.name).toBe('login')
  })

  it('shows a failure popup and retains the user on Register after Close', async () => {
    registerMock.mockRejectedValue(new Error('Username already exists.'))
    const wrapper = mountView()

    await wrapper.get('[data-testid="register-submit"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="status-popup"]').text()).toContain('Registration failed')
    expect(wrapper.text()).toContain('Username already exists.')

    await (wrapper.vm as unknown as { closePopup: () => Promise<void> }).closePopup()

    expect(wrapper.find('[data-testid="status-popup"]').exists()).toBe(false)
    expect(router.currentRoute.value.name).toBe('register')
  })
})
