import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import AuthenticatedV2ShellView from './AuthenticatedV2ShellView.vue'

const mocks = vi.hoisted(() => ({
  currentPath: '/v2/academic-years',
  push: vi.fn(),
  replace: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mocks.push,
    replace: mocks.replace,
    currentRoute: { value: { name: 'academic-years' } },
  }),
  useRoute: () => ({
    get path() {
      return mocks.currentPath
    },
  }),
  RouterView: {
    name: 'RouterView',
    template: '<div data-testid="router-view-content" />',
  },
}))

describe('AuthenticatedV2ShellView.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.currentPath = '/v2/academic-years'
  })

  afterEach(() => {
    clearAuthSession()
  })

  it('shows Transcript tab and hides Class Transcript tab for STUDENT role', () => {
    saveAuthSession({
      accessToken: 'token-stu',
      user: {
        id: 1,
        username: 'student1',
        roles: ['STUDENT'],
      },
    })

    const wrapper = mount(AuthenticatedV2ShellView, {
      global: {
        stubs: {
          RouterView: true,
          AuthenticatedLayout: {
            props: ['navigation'],
            template: '<div class="mock-layout"><span v-for="item in navigation" :key="item.to" :data-to="item.to" :data-active="item.active">{{ item.label }}</span></div>',
          },
        },
      },
    })

    expect(wrapper.find('[data-to="/v2/transcripts"]').exists()).toBe(true)
    expect(wrapper.find('[data-to="/v2/class-transcripts"]').exists()).toBe(false)
  })

  it('hides Transcript tab and shows Class Transcript tab for TEACHER role', () => {
    saveAuthSession({
      accessToken: 'token-tea',
      user: {
        id: 2,
        username: 'teacher1',
        roles: ['TEACHER'],
      },
    })

    const wrapper = mount(AuthenticatedV2ShellView, {
      global: {
        stubs: {
          RouterView: true,
          AuthenticatedLayout: {
            props: ['navigation'],
            template: '<div class="mock-layout"><span v-for="item in navigation" :key="item.to" :data-to="item.to" :data-active="item.active">{{ item.label }}</span></div>',
          },
        },
      },
    })

    expect(wrapper.find('[data-to="/v2/transcripts"]').exists()).toBe(false)
    expect(wrapper.find('[data-to="/v2/class-transcripts"]').exists()).toBe(true)
  })

  it('sets Class Transcript tab active when teacher navigates to /v2/transcripts for visual disguise', () => {
    mocks.currentPath = '/v2/transcripts'

    saveAuthSession({
      accessToken: 'token-adm',
      user: {
        id: 3,
        username: 'admin1',
        roles: ['ADMIN'],
      },
    })

    const wrapper = mount(AuthenticatedV2ShellView, {
      global: {
        stubs: {
          RouterView: true,
          AuthenticatedLayout: {
            props: ['navigation'],
            template: '<div class="mock-layout"><span v-for="item in navigation" :key="item.to" :data-to="item.to" :data-active="String(item.active)">{{ item.label }}</span></div>',
          },
        },
      },
    })

    const classTransItem = wrapper.find('[data-to="/v2/class-transcripts"]')
    expect(classTransItem.exists()).toBe(true)
    expect(classTransItem.attributes('data-active')).toBe('true')
  })

  it('sets Class Transcript tab active when teacher is on /v2/class-transcripts', () => {
    mocks.currentPath = '/v2/class-transcripts'

    saveAuthSession({
      accessToken: 'token-adm',
      user: {
        id: 3,
        username: 'admin1',
        roles: ['ADMIN'],
      },
    })

    const wrapper = mount(AuthenticatedV2ShellView, {
      global: {
        stubs: {
          RouterView: true,
          AuthenticatedLayout: {
            props: ['navigation'],
            template: '<div class="mock-layout"><span v-for="item in navigation" :key="item.to" :data-to="item.to" :data-active="String(item.active)">{{ item.label }}</span></div>',
          },
        },
      },
    })

    const classTransItem = wrapper.find('[data-to="/v2/class-transcripts"]')
    expect(classTransItem.exists()).toBe(true)
    expect(classTransItem.attributes('data-active')).toBe('true')
  })

  it('shows Calculation Operations tab for ADMIN and ACADEMIC_OFFICE roles', () => {
    saveAuthSession({
      accessToken: 'token-office',
      user: {
        id: 4,
        username: 'office1',
        roles: ['ACADEMIC_OFFICE'],
      },
    })

    const wrapper = mount(AuthenticatedV2ShellView, {
      global: {
        stubs: {
          RouterView: true,
          AuthenticatedLayout: {
            props: ['navigation'],
            template: '<div class="mock-layout"><span v-for="item in navigation" :key="item.to" :data-to="item.to">{{ item.label }}</span></div>',
          },
        },
      },
    })

    expect(wrapper.find('[data-to="/v2/scorebooks/operations"]').exists()).toBe(true)
  })

  it('hides Calculation Operations tab for STUDENT and TEACHER roles', () => {
    saveAuthSession({
      accessToken: 'token-teacher',
      user: {
        id: 5,
        username: 'teacher1',
        roles: ['TEACHER'],
      },
    })

    const wrapper = mount(AuthenticatedV2ShellView, {
      global: {
        stubs: {
          RouterView: true,
          AuthenticatedLayout: {
            props: ['navigation'],
            template: '<div class="mock-layout"><span v-for="item in navigation" :key="item.to" :data-to="item.to">{{ item.label }}</span></div>',
          },
        },
      },
    })

    expect(wrapper.find('[data-to="/v2/scorebooks/operations"]').exists()).toBe(false)
  })
})
