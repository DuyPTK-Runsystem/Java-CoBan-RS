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

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
