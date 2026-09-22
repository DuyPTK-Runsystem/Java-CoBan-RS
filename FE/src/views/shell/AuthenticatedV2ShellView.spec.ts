import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import ButtonStub from '@/test/stubs/ButtonStub.vue'
import { NOTIFICATION_READ_EVENT } from '@/services/notificationApi'
import AuthenticatedV2ShellView from './AuthenticatedV2ShellView.vue'

const mocks = vi.hoisted(() => ({
  currentPath: '/v2/academic-years',
  fetchUnreadNotificationCount: vi.fn(),
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

vi.mock('@/services/notificationApi', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/services/notificationApi')>()
  return {
    ...actual,
    fetchUnreadNotificationCount: mocks.fetchUnreadNotificationCount,
  }
})

enableAutoUnmount(afterEach)

function mountShellWithRealLayout() {
  return mount(AuthenticatedV2ShellView, {
    global: {
      stubs: {
        Button: ButtonStub,
        RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
        RouterView: true,
      },
    },
  })
}

describe('AuthenticatedV2ShellView.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.currentPath = '/v2/academic-years'
    mocks.fetchUnreadNotificationCount.mockResolvedValue(0)
  })

  afterEach(() => {
    clearAuthSession()
  })

  it('shows Attendance, Transcript, and Notification tabs for STUDENT role', () => {
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

    expect(wrapper.findAll('[data-to]').map((item) => [item.attributes('data-to'), item.text()])).toEqual([
      ['/v2/attendance', 'Điểm danh'],
      ['/v2/transcripts', 'Bảng điểm'],
      ['/v2/notifications', 'Thông báo'],
    ])
    expect(wrapper.find('[data-to="/v2/transcripts"]').exists()).toBe(true)
    expect(wrapper.find('[data-to="/v2/notifications"]').attributes('data-active')).toBe('false')
    expect(wrapper.find('[data-to="/v2/class-transcripts"]').exists()).toBe(false)
  })

  it('marks the Notification tab active for STUDENT on the inbox route', () => {
    mocks.currentPath = '/v2/notifications'
    saveAuthSession({
      accessToken: 'token-stu-notification',
      user: {
        id: 6,
        username: 'student_notification',
        roles: ['STUDENT'],
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

    expect(wrapper.find('[data-to="/v2/notifications"]').attributes('data-active')).toBe('true')
  })

  it('loads unread metadata on mount and renders the unread count on the Notification tab', async () => {
    mocks.fetchUnreadNotificationCount.mockResolvedValueOnce(3)
    saveAuthSession({
      accessToken: 'token-stu-unread',
      user: { id: 7, username: 'student_unread', roles: ['STUDENT'] },
    })

    const wrapper = mountShellWithRealLayout()
    await flushPromises()

    expect(mocks.fetchUnreadNotificationCount).toHaveBeenCalledWith('token-stu-unread')
    const notificationLink = wrapper.get('a[href="/v2/notifications"]')
    expect(notificationLink.text()).toContain('Thông báo')
    expect(notificationLink.text()).toContain('3')
  })

  it('does not render a Notification count when unread metadata is zero', async () => {
    saveAuthSession({
      accessToken: 'token-stu-read',
      user: { id: 8, username: 'student_read', roles: ['STUDENT'] },
    })

    const wrapper = mountShellWithRealLayout()
    await flushPromises()

    expect(mocks.fetchUnreadNotificationCount).toHaveBeenCalledWith('token-stu-read')
    expect(wrapper.get('a[href="/v2/notifications"]').text()).toBe('Thông báo')
  })

  it('keeps the authenticated shell usable when the unread count request fails', async () => {
    mocks.fetchUnreadNotificationCount.mockRejectedValueOnce(new Error('notification service unavailable'))
    saveAuthSession({
      accessToken: 'token-stu-error',
      user: { id: 9, username: 'student_error', roles: ['STUDENT'] },
    })

    const wrapper = mountShellWithRealLayout()
    await flushPromises()

    expect(mocks.fetchUnreadNotificationCount).toHaveBeenCalledWith('token-stu-error')
    expect(wrapper.find('main.page-content').exists()).toBe(true)
    expect(wrapper.get('a[href="/v2/notifications"]').text()).toContain('Thông báo')
  })

  it('refreshes the unread count after the notification-read event', async () => {
    let unreadCount = 2
    mocks.fetchUnreadNotificationCount.mockImplementation(() => Promise.resolve(unreadCount))
    saveAuthSession({
      accessToken: 'token-stu-refresh',
      user: { id: 14, username: 'student_refresh', roles: ['STUDENT'] },
    })

    const wrapper = mountShellWithRealLayout()
    await flushPromises()
    const notificationLink = wrapper.get('a[href="/v2/notifications"]')
    expect(notificationLink.text()).toContain('2')

    unreadCount = 1
    window.dispatchEvent(new Event(NOTIFICATION_READ_EVENT))
    await flushPromises()

    expect(mocks.fetchUnreadNotificationCount).toHaveBeenCalledTimes(2)
    expect(mocks.fetchUnreadNotificationCount).toHaveBeenNthCalledWith(1, 'token-stu-refresh')
    expect(mocks.fetchUnreadNotificationCount).toHaveBeenNthCalledWith(2, 'token-stu-refresh')
    expect(notificationLink.text()).toContain('1')
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
    expect(wrapper.find('[data-to="/v2/academic-years"]').exists()).toBe(false)
    expect(wrapper.find('[data-to="/v2/academic-catalog/grades"]').exists()).toBe(false)
    expect(wrapper.find('[data-to="/v2/enrollments"]').exists()).toBe(false)
    expect(wrapper.find('[data-to="/v2/academic-catalog/classes"]').exists()).toBe(true)
    expect(wrapper.find('[data-to="/v2/academic-catalog/subjects"]').exists()).toBe(true)
    expect(wrapper.find('[data-to="/v2/academic-catalog/class-subjects"]').exists()).toBe(true)
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

  it('hides Calculation Operations tab for ACADEMIC_OFFICE role', () => {
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

    expect(wrapper.find('[data-to="/v2/scorebooks/operations"]').exists()).toBe(false)
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
