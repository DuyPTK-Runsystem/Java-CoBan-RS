<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import AuthenticatedLayout from '@/components/AuthenticatedLayout.vue'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import { logout as logoutApi } from '@/services/userApi'

const router = useRouter()
const session = computed(() => getAuthSession())

function logout(): void {
  const accessToken = session.value?.accessToken
  if (!accessToken) {
    clearAuthSession()
    void router.replace({ name: 'login' })
    return
  }
  void logoutApi(accessToken).catch(() => undefined).finally(() => {
    clearAuthSession()
    if (router.currentRoute.value.name !== 'login') return router.replace({ name: 'login' })
  })
}
</script>

<template>
  <AuthenticatedLayout
    :user-name="session?.user.username ?? ''"
    :navigation="[{ label: 'Năm học & học kỳ', to: '/v2/academic-years', icon: 'pi pi-calendar' }]"
    @logout="logout"
  >
    <RouterView />
  </AuthenticatedLayout>
</template>
