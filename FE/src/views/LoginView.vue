<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'

import LoginForm from '@/components/LoginForm.vue'
import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import { isApiError, login } from '@/services/userApi'
import type { LoginValues } from '@/types/user'

const router = useRouter()
const submitting = ref(false)
const popupVisible = ref(false)
const popupStatus = ref<'success' | 'failure'>('success')
const popupMessage = ref('')
const successRedirect = ref('/v2')

function safeRedirect(): string {
  const redirect = router.currentRoute.value.query.redirect
  return typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')
    ? redirect
    : '/v2'
}

async function handleSubmit(values: LoginValues): Promise<void> {
  submitting.value = true
  try {
    const session = await login(values)
    saveAuthSession(session)
    successRedirect.value = safeRedirect()
    popupStatus.value = 'success'
    popupMessage.value = 'Login completed successfully.'
    popupVisible.value = true
  } catch (error) {
    if (isApiError(error, 401)) {
      clearAuthSession()
    }
    popupStatus.value = 'failure'
    popupMessage.value = error instanceof Error ? error.message : 'Unable to log in. Please try again.'
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
        <h1 id="login-title">Welcome back</h1>
        <p>Sign in to manage your student records.</p>
      </div>
      <LoginForm :submitting="submitting" @submit="handleSubmit" @register="router.push('/register')" />
    </section>
    <Dialog
      v-model:visible="popupVisible"
      modal
      :closable="false"
      :close-on-escape="false"
      :header="popupStatus === 'success' ? 'Login successful' : 'Login failed'"
    >
      <p class="dialog-message" role="status">{{ popupMessage }}</p>
      <template #footer>
        <Button label="Close" @click="closePopup" />
      </template>
    </Dialog>
  </main>
</template>
