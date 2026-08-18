import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, getAuthSession } from '@/services/authSession'
import router from '@/router'

import LoginView from './LoginView.vue'

const loginMock = vi.hoisted(() => vi.fn())

vi.mock('@/services/userApi', () => ({
  isApiError: (error: unknown, status?: number) => error instanceof Error
    && (status === undefined || (error as Error & { status?: number }).status === status),
  login: loginMock,
}))

const loginFormStub = {
  emits: ['submit', 'register'],
  template: `
    <button
      data-testid="login-submit"
      @click="$emit('submit', { userName: 'student01', password: 'secret1' })"
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
  template: '<button @click="$emit(\'click\')">{{ label }}</button>',
}

function mountView() {
  return mount(LoginView, {
    global: {
      plugins: [router],
      stubs: {
        LoginForm: loginFormStub,
        Dialog: dialogStub,
        Button: buttonStub,
      },
    },
  })
}

describe('LoginView status popup', () => {
  beforeEach(async () => {
    clearAuthSession()
    loginMock.mockReset()
    await router.push('/login?redirect=/students/new')
  })

  afterEach(() => {
    clearAuthSession()
  })

  it('stores the session, shows success, and redirects only after Close', async () => {
    loginMock.mockResolvedValue({
      accessToken: 'jwt-token',
      user: { id: 1, username: 'student01' },
    })
    const wrapper = mountView()

    await wrapper.get('[data-testid="login-submit"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="status-popup"]').text()).toContain('Login successful')
    expect(getAuthSession()?.accessToken).toBe('jwt-token')
    expect(router.currentRoute.value.name).toBe('login')

    await (wrapper.vm as unknown as { closePopup: () => Promise<void> }).closePopup()

    expect(router.currentRoute.value.fullPath).toBe('/students/new')
  })

  it('shows a failure popup without saving a session or redirecting', async () => {
    loginMock.mockRejectedValue(new Error('Invalid credentials.'))
    const wrapper = mountView()

    await wrapper.get('[data-testid="login-submit"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="status-popup"]').text()).toContain('Login failed')
    expect(getAuthSession()).toBeNull()

    await (wrapper.vm as unknown as { closePopup: () => Promise<void> }).closePopup()

    expect(router.currentRoute.value.name).toBe('login')
  })
})
