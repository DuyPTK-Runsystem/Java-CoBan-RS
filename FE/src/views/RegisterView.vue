<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'

import RegisterForm from '@/components/RegisterForm.vue'
import { register } from '@/services/userApi'
import type { RegisterValues } from '@/types/user'

const router = useRouter()
const submitting = ref(false)
const popupVisible = ref(false)
const popupStatus = ref<'success' | 'failure'>('success')
const popupMessage = ref('')

async function handleSubmit(values: RegisterValues): Promise<void> {
  submitting.value = true
  try {
    await register(values)
    popupStatus.value = 'success'
    popupMessage.value = 'Registration completed successfully. You can now log in.'
    popupVisible.value = true
  } catch (error) {
    popupStatus.value = 'failure'
    popupMessage.value = error instanceof Error ? error.message : 'Unable to register. Please try again.'
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
        <h1 id="register-title">Create your account</h1>
        <p>Set up access to the student management workspace.</p>
      </div>
      <RegisterForm :submitting="submitting" @submit="handleSubmit" @back="router.push('/login')" />
    </section>
    <Dialog
      v-model:visible="popupVisible"
      modal
      :closable="false"
      :close-on-escape="false"
      :header="popupStatus === 'success' ? 'Registration successful' : 'Registration failed'"
    >
      <p class="dialog-message" role="status">{{ popupMessage }}</p>
      <template #footer>
        <Button label="Close" @click="closePopup" />
      </template>
    </Dialog>
  </main>
</template>
