export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081'

// REST services will be introduced in a later approved plan.
export const apiConfiguration = {
  baseUrl: apiBaseUrl,
} as const
