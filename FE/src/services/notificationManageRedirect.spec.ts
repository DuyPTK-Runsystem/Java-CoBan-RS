import { afterEach, describe, expect, it, vi } from 'vitest'

import router from '@/router'
import { clearAuthSession, getAuthSession, saveAuthSession } from '@/services/authSession'
import { fetchManagedNotifications } from '@/services/notificationApi'
import { isApiError } from '@/types/api'

const fetchMock = vi.fn()

function saveAcademicOfficeSession(): void {
  saveAuthSession({
    accessToken: 'academic-office-token',
    user: { id: 2, username: 'academic.office', roles: ['ACADEMIC_OFFICE'] },
  })
}

describe('notification manage redirect regression', () => {
  afterEach(async () => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
    clearAuthSession()
    await router.push('/login')
  })

  it('keeps the Academic Office session and manage route when the API returns 500', async () => {
    saveAcademicOfficeSession()
    await router.push('/v2/notifications/manage')
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ message: 'Unexpected server error' }), { status: 500 }))
    vi.stubGlobal('fetch', fetchMock)

    const error = await fetchManagedNotifications('academic-office-token').catch((value: unknown) => value)

    expect(isApiError(error, 500)).toBe(true)
    expect(getAuthSession()?.accessToken).toBe('academic-office-token')
    expect(router.currentRoute.value.name).toBe('v2-notifications-manage')
  })

  it('clears the Academic Office session and redirects only for an actual 401 response', async () => {
    saveAcademicOfficeSession()
    await router.push('/v2/notifications/manage')
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ message: 'Expired token' }), { status: 401 }))
    vi.stubGlobal('fetch', fetchMock)

    const error = await fetchManagedNotifications('academic-office-token').catch((value: unknown) => value)

    expect(isApiError(error, 401)).toBe(true)
    expect(getAuthSession()).toBeNull()
    expect(router.currentRoute.value.name).toBe('login')
  })
})
