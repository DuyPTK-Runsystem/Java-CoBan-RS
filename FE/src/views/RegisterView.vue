<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import RegisterForm from '@/components/RegisterForm.vue'
import type { RegisterValues } from '@/types/user'

const router = useRouter()
const message = ref('')

function handleSubmit(_values: RegisterValues): void {
  message.value = 'Registration API integration is ready for a later implementation plan.'
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
      <RegisterForm :error-message="message" @submit="handleSubmit" @back="router.push('/login')" />
    </section>
  </main>
</template>
