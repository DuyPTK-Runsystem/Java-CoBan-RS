import type { AuthSession, UserSummary } from '@/types/user'

const ACCESS_TOKEN_KEY = 'student-management.access-token'
const USER_KEY = 'student-management.current-user'

function isUserSummary(value: unknown): value is UserSummary {
  if (!value || typeof value !== 'object') {
    return false
  }

  const user = value as Partial<UserSummary>
  return typeof user.id === 'number' && typeof user.username === 'string' && user.username.length > 0
}

export function getAuthSession(): AuthSession | null {
  const accessToken = sessionStorage.getItem(ACCESS_TOKEN_KEY)
  const serializedUser = sessionStorage.getItem(USER_KEY)
  if (!accessToken || !serializedUser) {
    return null
  }

  try {
    const user: unknown = JSON.parse(serializedUser)
    if (isUserSummary(user)) {
      return { accessToken, user }
    }
  } catch {
    // Treat corrupted browser state as signed out.
  }

  clearAuthSession()
  return null
}

export function saveAuthSession(session: AuthSession): void {
  sessionStorage.setItem(ACCESS_TOKEN_KEY, session.accessToken)
  sessionStorage.setItem(USER_KEY, JSON.stringify(session.user))
}

export function clearAuthSession(): void {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
}

export function hasAuthenticatedSession(): boolean {
  return getAuthSession() !== null
}
