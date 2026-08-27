import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import AuthenticatedV2ShellView from '@/views/AuthenticatedV2ShellView.vue'

import router from './index'

describe('router authentication guard', () => {
  afterEach(async () => {
    clearAuthSession()
    await router.push('/login')
  })

  it('redirects a guest from a protected route to login with its intended path', async () => {
    await router.push('/students/4/edit')

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/students/4/edit')
  })

  it('redirects an authenticated user away from guest-only routes', async () => {
    saveAuthSession({ accessToken: 'jwt-token', user: { id: 4, username: 'student01' } })

    await router.push('/register')

    expect(router.currentRoute.value.name).toBe('students')
  })

  it('marks the v2 shell as authenticated and module-neutral', () => {
    const route = router.resolve('/v2')

    expect(route.name).toBe('v2-shell')
    expect(route.meta).toMatchObject({ requiresAuth: true, module: 'v2', shell: 'authenticated' })
  })

  it('renders a nested v2 child through the authenticated layout outlet', async () => {
    saveAuthSession({ accessToken: 'jwt-token', user: { id: 4, username: 'student01' } })

    await router.push('/v2/academic-years')

    expect(router.currentRoute.value.name).toBe('v2-academic-years')
    expect(router.currentRoute.value.matched.map((record) => record.path)).toEqual([
      '/v2',
      '/v2/academic-years',
    ])
    expect(router.currentRoute.value.meta).toMatchObject({
      requiresAuth: true,
      module: 'v2',
      shell: 'authenticated',
    })

    const wrapper = mount(AuthenticatedV2ShellView, {
      global: {
        plugins: [router],
        stubs: {
          AuthenticatedLayout: {
            template: '<div data-testid="authenticated-layout"><slot /></div>',
          },
          RouterView: { template: '<div data-testid="nested-route-outlet" />' },
        },
      },
    })

    expect(wrapper.get('[data-testid="authenticated-layout"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="nested-route-outlet"]').exists()).toBe(true)
  })

  it.each([
    ['/login', 'login'],
    ['/register', 'register'],
    ['/students', 'students'],
    ['/students/new', 'student-create'],
    ['/students/4/edit', 'student-edit'],
    ['/v2/academic-years', 'v2-academic-years'],
    ['/v2/academic-years/1/semesters', 'v2-semesters'],
    ['/v2/academic-catalog/grades', 'v2-academic-grades'],
    ['/v2/academic-catalog/classes', 'v2-academic-classes'],
    ['/v2/academic-catalog/subjects', 'v2-academic-subjects'],
    ['/v2/academic-catalog/class-subjects', 'v2-academic-class-subjects'],
  ])('keeps the supported route %s mapped to %s', (path, name) => {
    expect(router.resolve(path).name).toBe(name)
  })
})
