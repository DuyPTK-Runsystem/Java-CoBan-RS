import { afterEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from './authSession'
import { apiClient, configureApiClient } from './apiClient'
import { isApiError } from '@/types/api'

const fetchMock = vi.fn()

describe('apiClient', () => {
  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
    clearAuthSession()
    configureApiClient({ onUnauthorized: undefined })
  })

  it.each([200, 201])('unwraps a %s success envelope for typed service calls', async (status) => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ statusCode: status, data: { value: 'ok' } }), { status }))
    vi.stubGlobal('fetch', fetchMock)

    async function getExample(): Promise<{ value: string }> {
      return apiClient.get<{ value: string }>('/api/v2/example', { authenticated: true })
    }

    saveAuthSession({ accessToken: 'session-token', user: { id: 1, username: 'academic.admin' } })
    await expect(getExample()).resolves.toEqual({ value: 'ok' })
    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8081/api/v2/example', expect.objectContaining({
      headers: { Accept: 'application/json', Authorization: 'Bearer session-token' },
    }))
  })

  it('returns undefined for 204 without attempting to parse a body', async () => {
    const response = new Response(null, { status: 204 })
    const jsonMock = vi.fn()
    Object.assign(response, { json: jsonMock })
    fetchMock.mockResolvedValue(response)
    vi.stubGlobal('fetch', fetchMock)

    await expect(apiClient.delete<void>('/api/v2/example')).resolves.toBeUndefined()
    expect(jsonMock).not.toHaveBeenCalled()
  })

  it('normalizes field and global validation messages', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ statusCode: 400, message: ['username: Username is required.', 'The request needs review.'] }), { status: 400 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(apiClient.post('/api/v2/example', {})).rejects.toSatisfy((error: unknown) => {
      return isApiError(error, 400)
        && error.kind === 'validation'
        && error.validationErrors[0]?.field === 'username'
        && error.validationErrors[0]?.messages[0] === 'Username is required.'
        && error.globalMessages[0] === 'The request needs review.'
    })
  })

  it('clears auth and invokes the application redirect handler on 401', async () => {
    const redirect = vi.fn()
    saveAuthSession({ accessToken: 'expired-token', user: { id: 1, username: 'academic.admin' } })
    configureApiClient({ onUnauthorized: redirect })
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ message: 'Expired token' }), { status: 401 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(apiClient.get('/api/v2/private', { authenticated: true })).rejects.toMatchObject({ status: 401, kind: 'unauthorized' })
    expect(sessionStorage.length).toBe(0)
    expect(redirect).toHaveBeenCalledOnce()
  })

  it('keeps the session and preserves forbidden semantics on 403', async () => {
    saveAuthSession({ accessToken: 'valid-token', user: { id: 1, username: 'academic.admin' } })
    fetchMock.mockResolvedValue(new Response(null, { status: 403 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(apiClient.get('/api/v2/private', { authenticated: true })).rejects.toMatchObject({
      status: 403,
      kind: 'forbidden',
      message: 'You do not have permission to perform this action.',
    })
    expect(sessionStorage.getItem('student-management.access-token')).toBe('valid-token')
  })

  it.each([
    [404, 'not-found', 'The requested resource was not found.'],
    [409, 'conflict', 'The request conflicts with existing data.'],
    [500, 'server', 'The server could not complete the request.'],
  ] as const)('keeps status and stable fallback for %s', async (status, kind, message) => {
    fetchMock.mockResolvedValue(new Response(status === 500 ? JSON.stringify({ message: 'java.lang.StackTrace: secret' }) : null, { status }))
    vi.stubGlobal('fetch', fetchMock)

    const error = await apiClient.get('/api/v2/example').catch((value: unknown) => value)
    expect(error).toMatchObject({ status, kind, message })
    if (status === 500) expect((error as { globalMessages: string[] }).globalMessages).toEqual([])
  })

  it('returns raw Blob responses without envelope parsing', async () => {
    fetchMock.mockResolvedValue(new Response('id,name\n1,An\n', { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    const result = await apiClient.get<Blob>('/api/v2/example/export', { responseType: 'blob' })

    expect(await result.text()).toContain('id,name')
    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8081/api/v2/example/export', expect.objectContaining({ headers: { Accept: 'text/csv' } }))
  })
})
