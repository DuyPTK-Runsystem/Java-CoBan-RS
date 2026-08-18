import { afterEach, describe, expect, it } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'

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
})
