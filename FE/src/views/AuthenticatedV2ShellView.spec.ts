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

  it.each(['ADMIN', 'ACADEMIC_OFFICE', 'TEACHER'] as const)(
    'shows Student Profile tab with icon pi pi-user for %s role',
    (role) => {
      saveAuthSession({
        accessToken: `token-${role}`,
        user: {
          id: 10,
          username: `user_${role}`,
          roles: [role],
        },
      })

      const wrapper = mount(AuthenticatedV2ShellView, {
        global: {
          stubs: {
            RouterView: true,
            AuthenticatedLayout: {
              props: ['navigation'],
              template:
                '<div class="mock-layout"><span v-for="item in navigation" :key="item.to" :data-to="item.to" :data-icon="item.icon" :data-active="String(item.active)">{{ item.label }}</span></div>',
            },
          },
        },
      })

      const studentItem = wrapper.find('[data-to="/v2/students"]')
      expect(studentItem.exists()).toBe(true)
      expect(studentItem.text()).toBe('Hồ sơ học sinh')
      expect(studentItem.attributes('data-icon')).toBe('pi pi-user')
    },
  )

  it('hides Student Profile tab for STUDENT role', () => {
    saveAuthSession({
      accessToken: 'token-stu-profile',
      user: {
        id: 11,
        username: 'student_only',
        roles: ['STUDENT'],
      },
    })

    const wrapper = mount(AuthenticatedV2ShellView, {
      global: {
        stubs: {
          RouterView: true,
          AuthenticatedLayout: {
            props: ['navigation'],
            template:
              '<div class="mock-layout"><span v-for="item in navigation" :key="item.to" :data-to="item.to">{{ item.label }}</span></div>',
          },
        },
      },
    })

    expect(wrapper.find('[data-to="/v2/students"]').exists()).toBe(false)
  })

  it.each([
    '/v2/students',
    '/v2/students/new',
    '/v2/students/101',
    '/v2/students/101/edit',
  ])('sets Student Profile tab active when route is %s', (path) => {
    mocks.currentPath = path

    saveAuthSession({
      accessToken: 'token-teacher-active',
      user: {
        id: 12,
        username: 'teacher_active',
        roles: ['TEACHER'],
      },
    })

    const wrapper = mount(AuthenticatedV2ShellView, {
      global: {
        stubs: {
          RouterView: true,
          AuthenticatedLayout: {
            props: ['navigation'],
            template:
              '<div class="mock-layout"><span v-for="item in navigation" :key="item.to" :data-to="item.to" :data-active="String(item.active)">{{ item.label }}</span></div>',
          },
        },
      },
    })

    const studentItem = wrapper.find('[data-to="/v2/students"]')
    expect(studentItem.exists()).toBe(true)
    expect(studentItem.attributes('data-active')).toBe('true')
  })

  it('sets Student Profile tab inactive when route is not under /v2/students', () => {
    mocks.currentPath = '/v2/academic-years'

    saveAuthSession({
      accessToken: 'token-admin-inactive',
      user: {
        id: 13,
        username: 'admin_inactive',
        roles: ['ADMIN'],
      },
    })

    const wrapper = mount(AuthenticatedV2ShellView, {
      global: {
        stubs: {
          RouterView: true,
          AuthenticatedLayout: {
            props: ['navigation'],
            template:
              '<div class="mock-layout"><span v-for="item in navigation" :key="item.to" :data-to="item.to" :data-active="String(item.active)">{{ item.label }}</span></div>',
          },
        },
      },
    })

    const studentItem = wrapper.find('[data-to="/v2/students"]')
    expect(studentItem.exists()).toBe(true)
    expect(studentItem.attributes('data-active')).toBe('false')
  })
})
