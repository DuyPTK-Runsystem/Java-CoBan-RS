export type ApiMessage = string | string[]

export interface ApiResponse<T> {
  statusCode: number
  error?: string
  message?: ApiMessage
  data: T
}

export type ApiErrorKind =
  | 'validation'
  | 'unauthorized'
  | 'forbidden'
  | 'not-found'
  | 'conflict'
  | 'server'
  | 'http'
  | 'network'

export interface ValidationError {
  field: string
  messages: string[]
}

export interface ApiErrorDetails {
  kind: ApiErrorKind
  rawMessages: string[]
  globalMessages: string[]
  validationErrors: ValidationError[]
  cause?: unknown
}

export class ApiError extends Error {
  readonly status: number
  readonly kind: ApiErrorKind
  readonly rawMessages: string[]
  readonly globalMessages: string[]
  readonly validationErrors: ValidationError[]
  readonly cause?: unknown

  constructor(status: number, message: string, details: Partial<ApiErrorDetails> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.kind = details.kind ?? 'http'
    this.rawMessages = details.rawMessages ?? []
    this.globalMessages = details.globalMessages ?? [message]
    this.validationErrors = details.validationErrors ?? []
    this.cause = details.cause
  }
}

export function isApiError(error: unknown, status?: number): error is ApiError {
  return error instanceof ApiError && (status === undefined || error.status === status)
}

const GENERIC_ERROR_MESSAGES = new Set([
  'the request is invalid',
  'authentication is required',
  'you do not have permission to perform this action',
  'the requested resource was not found',
  'the request conflicts with existing data',
  'the server could not complete the request',
  'the request could not be completed',
  'unable to reach the server',
  'conflict',
  'forbidden',
  'not found',
  'bad request',
  'unauthorized',
  'internal server error',
  'bad gateway',
  'service unavailable',
  'gateway timeout',
  'error',
  'fail',
  'failure',
  'undefined',
  'null',
  '[object object]',
])

export function isGenericErrorMessage(message?: string | null): boolean {
  if (!message) return true
  let trimmed = message.trim().toLowerCase()
  if (!trimmed) return true
  trimmed = trimmed.replace(/[.!?]+$/, '').trim()
  if (!trimmed) return true
  if (/^\d{3}$/.test(trimmed)) return true
  const withoutStatusCode = trimmed.replace(/^\d{3}\s*[-:]?\s*/, '').trim()
  return GENERIC_ERROR_MESSAGES.has(trimmed) || GENERIC_ERROR_MESSAGES.has(withoutStatusCode)
}

function normalizeMessageList(messages: unknown[]): string[] {
  return messages
    .flatMap((item) => {
      if (typeof item === 'string') return [item.trim()]
      if (Array.isArray(item)) return normalizeMessageList(item)
      return []
    })
    .filter((m) => Boolean(m) && !isGenericErrorMessage(m))
}

export function extractApiErrorMessages(error: unknown, fallback?: string): string[] {
  if (!error) return fallback ? [fallback] : []

  if (isApiError(error) && error.kind === 'network') {
    const netMsg = fallback || error.message || 'Không thể kết nối đến máy chủ.'
    return [netMsg]
  }

  if (isApiError(error)) {
    if (error.rawMessages && error.rawMessages.length > 0) {
      const messages = normalizeMessageList(error.rawMessages)
      if (messages.length > 0) return messages
    }
    if (error.globalMessages && error.globalMessages.length > 0) {
      const messages = normalizeMessageList(error.globalMessages)
      if (messages.length > 0) return messages
    }
    if (error.validationErrors && error.validationErrors.length > 0) {
      const messages = error.validationErrors
        .flatMap((v) => v.messages.map((m) => (v.field ? `${v.field}: ${m}` : m)))
        .map((m) => (typeof m === 'string' ? m.trim() : ''))
        .filter((m) => Boolean(m) && !isGenericErrorMessage(m))
      if (messages.length > 0) return messages
    }
  }

  if (typeof error === 'object' && error !== null) {
    const obj = error as Record<string, unknown>
    if ('message' in obj && obj.message !== undefined && obj.message !== null) {
      const rawMsg = obj.message
      if (Array.isArray(rawMsg)) {
        const messages = normalizeMessageList(rawMsg)
        if (messages.length > 0) return messages
      } else if (typeof rawMsg === 'string' && rawMsg.trim() && !isGenericErrorMessage(rawMsg)) {
        return [rawMsg.trim()]
      }
    }
    if ('detail' in obj && typeof obj.detail === 'string' && obj.detail.trim() && !isGenericErrorMessage(obj.detail)) {
      return [obj.detail.trim()]
    }
    if ('details' in obj) {
      const rawDetails = obj.details
      if (Array.isArray(rawDetails)) {
        const messages = normalizeMessageList(rawDetails)
        if (messages.length > 0) return messages
      } else if (typeof rawDetails === 'string' && rawDetails.trim() && !isGenericErrorMessage(rawDetails)) {
        return [rawDetails.trim()]
      }
    }
    if ('errors' in obj && obj.errors !== undefined && obj.errors !== null) {
      const rawErrors = obj.errors
      if (Array.isArray(rawErrors)) {
        const messages = normalizeMessageList(rawErrors)
        if (messages.length > 0) return messages
      } else if (typeof rawErrors === 'object') {
        const flatErrors = Object.entries(rawErrors as Record<string, unknown>).flatMap(([field, val]) => {
          const list = Array.isArray(val) ? val : [val]
          return list.map((m) => (typeof m === 'string' ? (field ? `${field}: ${m.trim()}` : m.trim()) : ''))
        }).filter((m) => Boolean(m) && !isGenericErrorMessage(m))
        if (flatErrors.length > 0) return flatErrors
      }
    }
  }

  if (error instanceof Error && error.message && error.message.trim()) {
    if (!isGenericErrorMessage(error.message)) {
      return [error.message.trim()]
    }
  }

  if (fallback) return [fallback]
  if (error instanceof Error && error.message && error.message.trim()) {
    return [error.message.trim()]
  }
  return ['Đã có lỗi xảy ra.']
}

export function extractApiErrorMessage(error: unknown, fallback?: string): string {
  const messages = extractApiErrorMessages(error, fallback)
  if (messages.length === 0) return fallback ?? ''
  if (messages.length === 1) return messages[0]!
  return messages
    .map((m, index) => {
      if (index === messages.length - 1) return m
      return /[.!?;:]$/.test(m) ? m : `${m}.`
    })
    .join(' ')
}

export function extractApiError(error: unknown, fallback?: string): string | string[] {
  const messages = extractApiErrorMessages(error, fallback)
  if (messages.length === 0) return fallback ?? ''
  if (messages.length === 1) return messages[0]!
  return messages
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
