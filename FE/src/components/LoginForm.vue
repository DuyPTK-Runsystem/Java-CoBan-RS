<script setup lang="ts">
import { reactive } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'

import type { FieldErrors, LoginValues } from '@/types/user'

const props = withDefaults(defineProps<{
  initialValues?: Partial<LoginValues>
  submitting?: boolean
  errorMessage?: string
}>(), {
  initialValues: () => ({}),
  submitting: false,
  errorMessage: '',
})

const emit = defineEmits<{
  submit: [values: LoginValues]
  register: []
}>()

const values = reactive<LoginValues>({
  userName: props.initialValues.userName ?? '',
  password: props.initialValues.password ?? '',
})
const errors = reactive<FieldErrors<keyof LoginValues>>({})

function validate(): boolean {
  errors.userName = values.userName.trim() ? undefined : 'User name is required.'
  if (!errors.userName && values.userName.length > 20) {
    errors.userName = 'User name must be 20 characters or fewer.'
  }
  errors.password = values.password ? undefined : 'Password is required.'
  if (!errors.password && (values.password.length < 6 || values.password.length > 15)) {
    errors.password = 'Password must be between 6 and 15 characters.'
  }
  return !errors.userName && !errors.password
}

function submit(): void {
  if (validate()) {
    emit('submit', { ...values })
  }
}
</script>

<template>
  <form class="form-stack" novalidate @submit.prevent="submit">
    <div v-if="props.errorMessage" class="form-alert form-alert-error" role="alert">
      {{ props.errorMessage }}
    </div>
    <div class="field-group">
      <label for="login-user-name">User name</label>
      <InputText id="login-user-name" v-model="values.userName" autocomplete="username" :invalid="Boolean(errors.userName)" />
      <small v-if="errors.userName" class="field-error">{{ errors.userName }}</small>
    </div>
    <div class="field-group">
      <label for="login-password">Password</label>
      <Password id="login-password" v-model="values.password" autocomplete="current-password" :feedback="false" toggle-mask :invalid="Boolean(errors.password)" />
      <small v-if="errors.password" class="field-error">{{ errors.password }}</small>
    </div>
    <Button type="submit" label="Log in" icon="pi pi-sign-in" :loading="props.submitting" />
    <Button type="button" label="Create an account" icon="pi pi-user-plus" severity="secondary" text @click="emit('register')" />
  </form>
</template>
