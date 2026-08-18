import { apiBaseUrl } from '@/services/apiConfig'
import type { LoginResponse, LoginValues, RegisterValues, RestResponse, UserSummary } from '@/types/user'

interface BackendLoginResponse {
  access_token: string
  user: UserSummary
}

interface ApiErrorBody {
  error?: string
  message?: string | string[]
}

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

function toUrl(path: string): string {
  return `${apiBaseUrl.replace(/\/$/, '')}${path}`
}

function errorMessage(body: ApiErrorBody | null, fallback: string): string {
  if (Array.isArray(body?.message)) {
    return body.message.join(' ')
  }
  return body?.message ?? fallback
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(toUrl(path), {
    ...init,
    headers: {
      Accept: 'application/json',
      ...init.headers,
    },
  })

  if (response.status === 204) {
    return undefined as T
  }

  const body: RestResponse<T> | ApiErrorBody = await response.json()
  if (!response.ok) {
    throw new ApiError(response.status, errorMessage(body as ApiErrorBody, response.statusText))
  }

  return (body as RestResponse<T>).data
}

function jsonRequest(body: object, accessToken?: string): RequestInit {
  return {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    },
    body: JSON.stringify(body),
  }
}

export async function register(values: RegisterValues): Promise<UserSummary> {
  return request<UserSummary>('/api/v1/auth/register', jsonRequest({
    username: values.userName,
    password: values.password,
    confirmPassword: values.confirmPassword,
  }))
}

export async function login(values: LoginValues): Promise<LoginResponse> {
  const response = await request<BackendLoginResponse>('/api/v1/auth/login', jsonRequest({
    username: values.userName,
    password: values.password,
  }))
  return { accessToken: response.access_token, user: response.user }
}

export function getCurrentAccount(accessToken: string): Promise<UserSummary> {
  return request<UserSummary>('/api/v1/auth/account', {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
}

export function logout(accessToken: string): Promise<void> {
  return request<void>('/api/v1/auth/logout', jsonRequest({}, accessToken))
}

export function isApiError(error: unknown, status?: number): error is ApiError {
  return error instanceof ApiError && (status === undefined || error.status === status)
}
