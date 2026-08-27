export interface LoginValues {
  userName: string
  password: string
}

export interface RegisterValues extends LoginValues {
  confirmPassword: string
}

export type FieldErrors<T extends string> = Partial<Record<T, string>>

export type LoginField = keyof LoginValues
export type RegisterField = keyof RegisterValues

export interface UserSummary {
  id: number
  username: string
  created_at?: string
  updated_at?: string
  created_by?: string
  updated_by?: string
}

export interface LoginResponse {
  accessToken: string
  user: UserSummary
}

export interface AuthSession {
  accessToken: string
  user: UserSummary
}

export type { ApiResponse as RestResponse } from '@/types/api'
