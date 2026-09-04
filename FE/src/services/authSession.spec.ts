import { afterEach, describe, expect, it } from 'vitest'

import { clearAuthSession, getAuthSession, hasAuthenticatedSession, saveAuthSession } from './authSession'

describe('authSession', () => {
  afterEach(() => {
    clearAuthSession()
  })

  it('stores and restores only the access token and UI-safe user summary', () => {
    saveAuthSession({
      accessToken: 'access-token',
      user: { id: 7, username: 'student01', roles: ['STUDENT'] },
    })

    expect(getAuthSession()).toEqual({
      accessToken: 'access-token',
      user: { id: 7, username: 'student01', roles: ['STUDENT'] },
    })
    expect(hasAuthenticatedSession()).toBe(true)
  })

  it('clears malformed browser state and treats it as signed out', () => {
    sessionStorage.setItem('student-management.access-token', 'access-token')
    sessionStorage.setItem('student-management.current-user', '{not-json')

    expect(getAuthSession()).toBeNull()
    expect(hasAuthenticatedSession()).toBe(false)
    expect(sessionStorage.length).toBe(0)
  })
})
