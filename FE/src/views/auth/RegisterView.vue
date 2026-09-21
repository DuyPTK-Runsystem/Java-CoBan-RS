<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'

import RegisterForm from '@/components/auth/RegisterForm.vue'
import { register } from '@/services/userApi'
import type { RegisterValues } from '@/types/user'

const router = useRouter()
const submitting = ref(false)
const popupVisible = ref(false)
const popupStatus = ref<'success' | 'failure'>('success')
const popupMessage = ref('')

function localizedRegistrationError(error: unknown, fallback: string): string {
  const message = error instanceof Error ? error.message.trim() : ''
  const knownTranslations: Record<string, string> = {
    'Username already exists.': 'Tên đăng nhập đã tồn tại.',
    'Unable to register. Please try again.': fallback,
    'Unauthorized': 'Phiên đăng nhập không hợp lệ hoặc đã hết hạn.',
  }

  if (knownTranslations[message]) return knownTranslations[message]
  if (message && /[À-ỹ]/u.test(message)) return message
  return fallback
}

async function handleSubmit(values: RegisterValues): Promise<void> {
  submitting.value = true
  try {
    await register(values)
    popupStatus.value = 'success'
    popupMessage.value = 'Đăng ký thành công. Bạn có thể đăng nhập ngay.'
    popupVisible.value = true
  } catch (error) {
    popupStatus.value = 'failure'
    popupMessage.value = localizedRegistrationError(error, 'Đăng ký không thành công. Vui lòng thử lại.')
    popupVisible.value = true
  } finally {
    submitting.value = false
  }
}

async function closePopup(): Promise<void> {
  const shouldNavigateToLogin = popupStatus.value === 'success'
  popupVisible.value = false
  if (shouldNavigateToLogin) {
    await router.replace('/login')
  }
}
</script>

<template>
  <main class="auth-shell">
    <section class="auth-surface" aria-labelledby="register-title">
      <div class="auth-heading">
        <span class="brand-mark" aria-hidden="true">AC</span>
        <p class="eyebrow">Academic Core</p>
        <h1 id="register-title">Tạo tài khoản</h1>
        <p>Đăng ký quyền truy cập không gian quản lý học sinh.</p>
      </div>
      <RegisterForm :submitting="submitting" @submit="handleSubmit" @back="router.push('/login')" />
    </section>
    <Dialog
      v-model:visible="popupVisible"
      modal
      :closable="false"
      :close-on-escape="false"
      :header="popupStatus === 'success' ? 'Đăng ký thành công' : 'Đăng ký thất bại'"
    >
      <p class="dialog-message" role="status">{{ popupMessage }}</p>
      <template #footer>
        <Button label="Đóng" @click="closePopup" />
      </template>
    </Dialog>
  </main>
</template>
