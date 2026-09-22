<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'

import LoginForm from '@/components/auth/LoginForm.vue'
import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import { firstPermittedWorkspacePath } from '@/services/studentNavigation'
import { isApiError, login } from '@/services/userApi'
import type { LoginValues } from '@/types/user'

const router = useRouter()
const submitting = ref(false)
const popupVisible = ref(false)
const popupStatus = ref<'success' | 'failure'>('success')
const popupMessage = ref('')
const successRedirect = ref('/v2/attendance')

function localizedAuthError(error: unknown, fallback: string): string {
  const message = error instanceof Error ? error.message.trim() : ''
  const knownTranslations: Record<string, string> = {
    'Invalid credentials.': 'Tên đăng nhập hoặc mật khẩu không đúng.',
    'Unable to log in. Please try again.': fallback,
    'Unauthorized': 'Phiên đăng nhập không hợp lệ hoặc đã hết hạn.',
  }

  if (knownTranslations[message]) return knownTranslations[message]
  if (message && /[À-ỹ]/u.test(message)) return message
  return fallback
}

async function handleSubmit(values: LoginValues): Promise<void> {
  submitting.value = true
  try {
    const session = await login(values)
    saveAuthSession(session)
    // Start each session at the first tab available to its roles. The router
    // remains the authority for guarding direct URL navigation afterwards.
    successRedirect.value = firstPermittedWorkspacePath(session.user.roles ?? [])
    popupStatus.value = 'success'
    popupMessage.value = 'Đăng nhập thành công.'
    popupVisible.value = true
  } catch (error) {
    if (isApiError(error, 401)) {
      clearAuthSession()
    }
    popupStatus.value = 'failure'
    popupMessage.value = localizedAuthError(error, 'Đăng nhập không thành công. Vui lòng thử lại.')
    popupVisible.value = true
  } finally {
    submitting.value = false
  }
}

async function closePopup(): Promise<void> {
  const shouldNavigate = popupStatus.value === 'success'
  popupVisible.value = false
  if (shouldNavigate) {
    await router.replace(successRedirect.value)
  }
}
</script>

<template>
  <main class="auth-shell">
    <section class="auth-surface" aria-labelledby="login-title">
      <div class="auth-heading">
        <span class="brand-mark" aria-hidden="true">AC</span>
        <p class="eyebrow">Academic Core</p>
        <h1 id="login-title">Chào mừng bạn trở lại</h1>
        <p>Đăng nhập để quản lý hồ sơ học sinh.</p>
      </div>
      <LoginForm :submitting="submitting" @submit="handleSubmit" @register="router.push('/register')" />
    </section>
    <Dialog
      v-model:visible="popupVisible"
      modal
      :closable="false"
      :close-on-escape="false"
      :header="popupStatus === 'success' ? 'Đăng nhập thành công' : 'Đăng nhập thất bại'"
    >
      <p class="dialog-message" role="status">{{ popupMessage }}</p>
      <template #footer>
        <Button label="Đóng" @click="closePopup" />
      </template>
    </Dialog>
  </main>
</template>
