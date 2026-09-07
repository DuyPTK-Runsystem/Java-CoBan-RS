import { afterEach, describe, expect, it, vi } from 'vitest'

import { ApiError, getCurrentAccount, login, logout, register } from './userApi'

const fetchMock = vi.fn()

describe('userApi', () => {
  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('maps form userName to backend username and unwraps the login response', async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({
      statusCode: 200,
      message: 'Đăng nhập',
      data: {
        access_token: 'jwt-token',
        user: { id: 3, username: 'student01' },
      },
    }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(login({ userName: 'student01', password: 'secret1' })).resolves.toEqual({
      accessToken: 'jwt-token',
      user: { id: 3, username: 'student01' },
    })

    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8081/api/v1/auth/login', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ username: 'student01', password: 'secret1' }),
    }))
  })

  it('uses the register payload and bearer header expected by protected endpoints', async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(JSON.stringify({
        statusCode: 201,
        data: { id: 3, username: 'student01' },
      }), { status: 201 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        statusCode: 200,
        data: { id: 3, username: 'student01' },
      }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await register({ userName: 'student01', password: 'secret1', confirmPassword: 'secret1' })
    await getCurrentAccount('jwt-token')

    expect(fetchMock.mock.calls[0]?.[1]).toEqual(expect.objectContaining({
      body: JSON.stringify({ username: 'student01', password: 'secret1', confirmPassword: 'secret1' }),
    }))
    expect(fetchMock.mock.calls[1]?.[1]).toEqual(expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer jwt-token' }),
    }))
  })

  it('accepts stateless logout no-content responses and normalizes backend errors', async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        statusCode: 401,
        error: 'Unauthorized',
        message: 'Thông tin đăng nhập không hợp lệ',
      }), { status: 401 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(logout('jwt-token')).resolves.toBeUndefined()
    await expect(login({ userName: 'student01', password: 'wrong1' })).rejects.toEqual(expect.objectContaining({
      name: 'ApiError',
      status: 401,
      message: 'Thông tin đăng nhập không hợp lệ',
    } satisfies Partial<ApiError>))
  })
})
