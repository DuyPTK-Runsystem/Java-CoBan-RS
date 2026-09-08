import { afterEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import { requireAccessToken } from './useAuthSession'

describe('useAuthSession helpers', () => {
  afterEach(() => {
    clearAuthSession()
    vi.restoreAllMocks()
  })

  it('returns the current access token without touching the router', () => {
    const replace = vi.fn()
    saveAuthSession({ accessToken: 'valid-token', user: { id: 1, username: 'admin.demo' } })

    expect(requireAccessToken({ replace })).toBe('valid-token')
    expect(replace).not.toHaveBeenCalled()
  })

  it('clears the session and redirects to login when no token exists', () => {
    const replace = vi.fn()

    expect(requireAccessToken({ replace })).toBeNull()
    expect(replace).toHaveBeenCalledWith({ name: 'login' })
  })
})
