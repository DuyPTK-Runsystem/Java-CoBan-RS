<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import LoginForm from '@/components/LoginForm.vue'
import type { LoginValues } from '@/types/user'

const router = useRouter()
const message = ref('')

function handleSubmit(_values: LoginValues): void {
  message.value = 'Login API integration is ready for a later implementation plan.'
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
      <LoginForm :error-message="message" @submit="handleSubmit" @register="router.push('/register')" />
    </section>
  </main>
</template>
