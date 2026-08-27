import { apiBaseUrl } from '@/services/apiConfig'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import { ApiError, type ApiErrorKind, type ApiResponse, type ValidationError } from '@/types/api'

type QueryValue = string | number | boolean | null | undefined

export interface ApiRequestOptions extends Omit<RequestInit, 'body' | 'headers'> {
  query?: Record<string, QueryValue> | URLSearchParams
  body?: unknown
  token?: string
  authenticated?: boolean
  responseType?: 'json' | 'blob'
  headers?: Record<string, string>
}

export interface ApiClientConfig {
  baseUrl?: string
  onUnauthorized?: () => void | Promise<void>
}

interface ErrorPayload {
  error?: unknown
  message?: unknown
  errors?: unknown
}

let onUnauthorized: (() => void | Promise<void>) | undefined
let configuredBaseUrl = apiBaseUrl

export function configureApiClient(config: ApiClientConfig): void {
  configuredBaseUrl = config.baseUrl ?? apiBaseUrl
  onUnauthorized = config.onUnauthorized
}

function toUrl(baseUrl: string, path: string, query?: ApiRequestOptions['query']): string {
  const normalizedBase = baseUrl.replace(/\/$/, '')
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const url = new URL(`${normalizedBase}${normalizedPath}`, window.location.origin)
  if (query) {
    const params = query instanceof URLSearchParams ? query : new URLSearchParams()
    if (!(query instanceof URLSearchParams)) {
      Object.entries(query).forEach(([key, value]) => {
        if (value !== undefined && value !== null) params.set(key, String(value))
      })
    }
    url.search = params.toString()
  }
  return url.toString()
}

function requestUrl(baseUrl: string, path: string, query?: ApiRequestOptions['query']): string {
  const url = toUrl(baseUrl, path, query)
  return url.startsWith(`${window.location.origin}/`) && !baseUrl.startsWith(window.location.origin)
    ? `${baseUrl.replace(/\/$/, '')}${path.startsWith('/') ? path : `/${path}`}${url.includes('?') ? url.slice(url.indexOf('?')) : ''}`
    : url
}

function asMessages(value: unknown): string[] {
  if (typeof value === 'string' && value.trim()) return [value.trim()]
  if (Array.isArray(value)) return value.flatMap((item) => asMessages(item))
  return []
}

function isFieldName(value: string): boolean {
  return /^[A-Za-z_$][\w$.[\]-]*$/.test(value)
}

function normalizeErrors(payload: ErrorPayload | null): { rawMessages: string[]; globalMessages: string[]; validationErrors: ValidationError[] } {
  const rawMessages = asMessages(payload?.message)
  const validationMap = new Map<string, string[]>()
  const globalMessages: string[] = []

  rawMessages.forEach((rawMessage) => {
    const separator = rawMessage.indexOf(':')
    const field = separator > 0 ? rawMessage.slice(0, separator).trim() : ''
    const message = separator > 0 ? rawMessage.slice(separator + 1).trim() : rawMessage
    if (field && message && isFieldName(field)) {
      const messages = validationMap.get(field) ?? []
      messages.push(message)
      validationMap.set(field, messages)
    } else {
      globalMessages.push(rawMessage)
    }
  })

  if (validationMap.size === 0 && payload?.errors && typeof payload.errors === 'object' && !Array.isArray(payload.errors)) {
    Object.entries(payload.errors as Record<string, unknown>).forEach(([field, value]) => {
      const messages = asMessages(value)
      if (isFieldName(field) && messages.length > 0) validationMap.set(field, messages)
    })
  }

  const validationErrors = [...validationMap.entries()].map(([field, messages]) => ({ field, messages }))
  return { rawMessages, globalMessages, validationErrors }
}

function kindForStatus(status: number): ApiErrorKind {
  if (status === 400) return 'validation'
  if (status === 401) return 'unauthorized'
  if (status === 403) return 'forbidden'
  if (status === 404) return 'not-found'
  if (status === 409) return 'conflict'
  if (status >= 500) return 'server'
  return 'http'
}

function fallbackMessage(status: number): string {
  if (status === 400) return 'The request is invalid.'
  if (status === 401) return 'Authentication is required.'
  if (status === 403) return 'You do not have permission to perform this action.'
  if (status === 404) return 'The requested resource was not found.'
  if (status === 409) return 'The request conflicts with existing data.'
  if (status >= 500) return 'The server could not complete the request.'
  return 'The request could not be completed.'
}

function safePayloadMessage(payload: ErrorPayload | null, status: number, details: ReturnType<typeof normalizeErrors>): string {
  if (details.globalMessages.length > 0) return details.globalMessages.join(' ')
  if (details.validationErrors.length > 0) return details.validationErrors.flatMap((error) => error.messages).join(' ')
  const errorMessages = asMessages(payload?.error)
  return errorMessages.join(' ') || fallbackMessage(status)
}

async function readJson(response: Response): Promise<unknown> {
  const text = await response.text()
  if (!text.trim()) return null
  try {
    return JSON.parse(text) as unknown
  } catch {
    return null
  }
}

async function toApiError(response: Response): Promise<ApiError> {
  const body = await readJson(response)
  const payload = body && typeof body === 'object' ? body as ErrorPayload : null
  const details = normalizeErrors(payload)
  const safeDetails = response.status >= 500
    ? { rawMessages: [], globalMessages: [], validationErrors: [] }
    : details
  return new ApiError(response.status, safePayloadMessage(payload, response.status, safeDetails), {
    ...safeDetails,
    kind: kindForStatus(response.status),
  })
}

async function request<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const token = options.token ?? (options.authenticated ? getAuthSession()?.accessToken : undefined)
  const headers: Record<string, string> = {
    Accept: options.responseType === 'blob' ? 'text/csv' : 'application/json',
    ...options.headers,
  }
  if (token) headers.Authorization = `Bearer ${token}`
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  let response: Response
  try {
    response = await fetch(requestUrl(configuredBaseUrl, path, options.query), {
      ...options,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    })
  } catch (cause) {
    throw new ApiError(0, 'Unable to reach the server.', { kind: 'network', cause })
  }

  if (!response.ok) {
    const error = await toApiError(response)
    if (response.status === 401) {
      clearAuthSession()
      try {
        await onUnauthorized?.()
      } catch {
        // Preserve the transport error when the application redirect fails.
      }
    }
    throw error
  }
  if (response.status === 204) return undefined as T
  if (options.responseType === 'blob') return await response.blob() as T

  const body = await readJson(response) as ApiResponse<T> | null
  return body?.data as T
}

export const apiClient = {
  request,
  get<T>(path: string, options: Omit<ApiRequestOptions, 'method' | 'body'> = {}): Promise<T> {
    return request<T>(path, { ...options, method: 'GET' })
  },
  post<T>(path: string, body?: unknown, options: Omit<ApiRequestOptions, 'method' | 'body'> = {}): Promise<T> {
    return request<T>(path, { ...options, method: 'POST', body })
  },
  put<T>(path: string, body?: unknown, options: Omit<ApiRequestOptions, 'method' | 'body'> = {}): Promise<T> {
    return request<T>(path, { ...options, method: 'PUT', body })
  },
  delete<T>(path: string, options: Omit<ApiRequestOptions, 'method' | 'body'> = {}): Promise<T> {
    return request<T>(path, { ...options, method: 'DELETE' })
  },
}
