import { apiClient } from '@/services/apiClient'
import { ApiError, isApiError } from '@/types/api'
import type { LoginResponse, LoginValues, RegisterValues, UserSummary } from '@/types/user'

interface BackendLoginResponse {
  access_token: string
  user: UserSummary
}

export { ApiError, isApiError }

export async function register(values: RegisterValues): Promise<UserSummary> {
  return apiClient.post<UserSummary>('/api/v1/auth/register', {
    username: values.userName,
    password: values.password,
    confirmPassword: values.confirmPassword,
  })
}

export async function login(values: LoginValues): Promise<LoginResponse> {
  const response = await apiClient.post<BackendLoginResponse>('/api/v1/auth/login', {
    username: values.userName,
    password: values.password,
  })
  return { accessToken: response.access_token, user: response.user }
}

export function getCurrentAccount(accessToken: string): Promise<UserSummary> {
  return apiClient.get<UserSummary>('/api/v1/auth/account', { token: accessToken })
}

export function logout(accessToken: string): Promise<void> {
  return apiClient.post<void>('/api/v1/auth/logout', {}, { token: accessToken })
}
