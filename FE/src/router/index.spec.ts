import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it } from 'vitest'

import { clearAuthSession, getAuthSession, saveAuthSession } from '@/services/authSession'
import AuthenticatedV2ShellView from '@/views/AuthenticatedV2ShellView.vue'

import router from './index'

describe('router authentication guard', () => {
  afterEach(async () => {
    clearAuthSession()
    await router.push('/login')
  })

  it('redirects a guest from a protected route to login with its intended path', async () => {
    await router.push('/v2/students/4/edit')

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/v2/students/4/edit')
  })

  it('redirects an authenticated user away from guest-only routes to the first permitted tab', async () => {
    saveAuthSession({ accessToken: 'jwt-token', user: { id: 4, username: 'student01', roles: ['ADMIN'] } })

    await router.push('/register')

    expect(router.currentRoute.value.path).toBe('/v2/academic-years')
    expect(router.currentRoute.value.name).toBe('v2-academic-years')
  })

  it('redirects legacy /students path to /v2/students for authenticated user', async () => {
    saveAuthSession({ accessToken: 'jwt-token', user: { id: 4, username: 'student01', roles: ['ADMIN'] } })

    await router.push('/students')

    expect(router.currentRoute.value.path).toBe('/v2/students')
    expect(router.currentRoute.value.name).toBe('v2-students')
  })

  it('redirects legacy /students/new path to /v2/students/new for authenticated user', async () => {
    saveAuthSession({ accessToken: 'jwt-token', user: { id: 4, username: 'student01', roles: ['ADMIN'] } })

    await router.push('/students/new')

    expect(router.currentRoute.value.path).toBe('/v2/students/new')
    expect(router.currentRoute.value.name).toBe('v2-student-create')
  })

  it('redirects legacy /students/:studentId/edit path to /v2/students/:studentId/edit for authenticated user', async () => {
    saveAuthSession({ accessToken: 'jwt-token', user: { id: 4, username: 'student01', roles: ['ADMIN'] } })

    await router.push('/students/4/edit')

    expect(router.currentRoute.value.path).toBe('/v2/students/4/edit')
    expect(router.currentRoute.value.name).toBe('v2-student-edit')
  })

  it('marks the v2 shell as authenticated and module-neutral', () => {
    const route = router.resolve('/v2')

    expect(route.name).toBe('v2-shell')
    expect(route.meta).toMatchObject({ requiresAuth: true, module: 'v2', shell: 'authenticated' })
  })

  it.each(['/v2', '/v2/students', '/v2/students/new', '/v2/students/4', '/students/4/edit', '/v2/academic-years', '/v2/academic-years/1/semesters', '/v2/academic-catalog/grades', '/v2/academic-catalog/classes', '/v2/academic-catalog/subjects', '/v2/academic-catalog/class-subjects', '/v2/enrollments', '/v2/teachers', '/v2/teaching-assignments', '/v2/scorebooks', '/v2/scorebooks/operations', '/v2/score-change-requests', '/v2/class-transcripts', '/v2/retake-exams', '/v2/unknown'])('redirects STUDENT from %s to attendance', async (path) => {
    saveAuthSession({ accessToken: 'jwt-token', user: { id: 4, username: 'student01', roles: ['STUDENT'] } })

    await router.push(path)

    expect(router.currentRoute.value.name).toBe('v2-attendance')
    expect(getAuthSession()?.user.roles).toEqual(['STUDENT'])
  })

  it.each(['/v2/attendance', '/v2/transcripts', '/v2/transcripts?year=1', '/v2/attendance/'])('allows STUDENT to open %s', async (path) => {
    saveAuthSession({ accessToken: 'jwt-token', user: { id: 4, username: 'student01', roles: ['STUDENT'] } })
    await router.push(path)
    expect(router.currentRoute.value.fullPath).toBe(path)
  })

  it.each([
    '/v2/academic-years',
    '/v2/academic-years/1/semesters',
    '/v2/academic-catalog/grades',
    '/v2/enrollments',
    '/v2/scorebooks/operations',
  ])('redirects TEACHER from restricted %s to attendance', async (path) => {
    saveAuthSession({ accessToken: 'jwt-token', user: { id: 5, username: 'teacher01', roles: ['TEACHER'] } })

    await router.push(path)

    expect(router.currentRoute.value.name).toBe('v2-attendance')
  })

  it.each(['/v2/academic-catalog/classes', '/v2/academic-catalog/subjects', '/v2/academic-catalog/class-subjects'])('allows TEACHER to read %s', async (path) => {
    saveAuthSession({ accessToken: 'jwt-token', user: { id: 5, username: 'teacher01', roles: ['TEACHER'] } })

    await router.push(path)

    expect(router.currentRoute.value.fullPath).toBe(path)
  })

  it('renders a nested v2 child through the authenticated layout outlet', async () => {
    saveAuthSession({ accessToken: 'jwt-token', user: { id: 4, username: 'student01', roles: ['ADMIN'] } })

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

  it('renders nested v2 students child through authenticated layout', async () => {
    saveAuthSession({ accessToken: 'jwt-token', user: { id: 4, username: 'student01', roles: ['ADMIN'] } })

    await router.push('/v2/students')

    expect(router.currentRoute.value.name).toBe('v2-students')
    expect(router.currentRoute.value.matched.map((record) => record.path)).toEqual([
      '/v2',
      '/v2/students',
    ])
    expect(router.currentRoute.value.meta).toMatchObject({
      requiresAuth: true,
      module: 'v2',
      shell: 'authenticated',
    })
  })

  it.each([
    ['/login', 'login'],
    ['/register', 'register'],
    ['/v2/students', 'v2-students'],
    ['/v2/students/new', 'v2-student-create'],
    ['/v2/students/4', 'v2-student-detail'],
    ['/v2/students/4/edit', 'v2-student-edit'],
    ['/v2/academic-years', 'v2-academic-years'],
    ['/v2/academic-years/1/semesters', 'v2-semesters'],
    ['/v2/academic-catalog/grades', 'v2-academic-grades'],
    ['/v2/academic-catalog/classes', 'v2-academic-classes'],
    ['/v2/academic-catalog/subjects', 'v2-academic-subjects'],
    ['/v2/academic-catalog/class-subjects', 'v2-academic-class-subjects'],
    ['/v2/enrollments', 'v2-enrollments'],
    ['/v2/attendance', 'v2-attendance'],
    ['/v2/scorebooks', 'v2-scorebooks'],
    ['/v2/transcripts', 'v2-transcripts'],
    ['/v2/retake-exams', 'v2-retake-exams'],
  ])('keeps the supported route %s mapped to %s', (path, name) => {
    expect(router.resolve(path).name).toBe(name)
  })
})
