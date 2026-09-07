import type { AuthSession, UserSummary } from '@/types/user'

const ACCESS_TOKEN_KEY = 'student-management.access-token'
const USER_KEY = 'student-management.current-user'

function isUserSummary(value: unknown): value is UserSummary {
  if (!value || typeof value !== 'object') {
    return false
  }

  const user = value as Partial<UserSummary>
  return typeof user.id === 'number'
    && typeof user.username === 'string'
    && user.username.length > 0
    && (user.roles === undefined || (Array.isArray(user.roles) && user.roles.every((role) => typeof role === 'string')))
}

// A single record keeps token and account changes atomic across tabs.
export const AUTH_SESSION_KEY = 'student-management.auth-session'

function clearLegacySession(): void {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
}

export function getAuthSession(): AuthSession | null {
  const serializedSession = localStorage.getItem(AUTH_SESSION_KEY)
  try {
    if (serializedSession !== null) {
      clearLegacySession()
      const session: unknown = JSON.parse(serializedSession)
      if (session === null) return null
      if (typeof session === 'object' && session !== null
        && 'accessToken' in session && typeof session.accessToken === 'string' && session.accessToken
        && 'user' in session && isUserSummary(session.user)) {
        return { accessToken: session.accessToken, user: session.user }
      }
    } else {
      const accessToken = sessionStorage.getItem(ACCESS_TOKEN_KEY)
      const serializedUser = sessionStorage.getItem(USER_KEY)
      if (accessToken && serializedUser) {
        const user: unknown = JSON.parse(serializedUser)
        if (isUserSummary(user)) {
          const session = { accessToken, user }
          saveAuthSession(session)
          return session
        }
      } else {
        clearLegacySession()
        return null
      }
    }
  } catch {
    // Treat corrupted browser state as signed out.
  }

  clearAuthSession()
  return null
}

export function saveAuthSession(session: AuthSession): void {
  localStorage.setItem(AUTH_SESSION_KEY, JSON.stringify(session))
  clearLegacySession()
}

export function clearAuthSession(): void {
  // Keep a signed-out marker so an old tab cannot restore its legacy session.
  localStorage.setItem(AUTH_SESSION_KEY, 'null')
  clearLegacySession()
}

export function syncAuthSession(event: StorageEvent): boolean {
  if (event.storageArea !== localStorage
    || (event.key !== AUTH_SESSION_KEY && event.key !== null)) return false
  clearLegacySession()
  return true
}

export function hasAuthenticatedSession(): boolean {
  return getAuthSession() !== null
}
