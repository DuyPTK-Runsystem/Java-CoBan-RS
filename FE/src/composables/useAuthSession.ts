import { computed } from 'vue'
import { useRouter, type Router } from 'vue-router'

import { clearAuthSession, getAuthSession } from '@/services/authSession'
import type { AuthSession, UserRole } from '@/types/user'

export function requireAccessToken(router: Pick<Router, 'replace'>): string | null {
  const session = getAuthSession()
  if (session) return session.accessToken

  clearAuthSession()
  void router.replace({ name: 'login' })
  return null
}

export function useAuthSession() {
  const router = useRouter()
  const session = computed<AuthSession | null>(() => getAuthSession())
  const accessToken = computed(() => session.value?.accessToken ?? '')
  const roles = computed<UserRole[]>(() => session.value?.user.roles ?? [])
  const hasRoleContract = computed(() => session.value?.user.roles !== undefined)

  return {
    session,
    accessToken,
    roles,
    hasRoleContract,
    requireAccessToken: () => requireAccessToken(router),
  }
}
