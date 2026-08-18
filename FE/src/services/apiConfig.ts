export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

// REST services will be introduced in a later approved plan.
export const apiConfiguration = {
  baseUrl: apiBaseUrl,
} as const
