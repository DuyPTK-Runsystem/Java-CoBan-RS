<script setup lang="ts">
import { reactive } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'

import type { FieldErrors, RegisterValues } from '@/types/user'

const props = withDefaults(defineProps<{
  initialValues?: Partial<RegisterValues>
  submitting?: boolean
  errorMessage?: string
}>(), {
  initialValues: () => ({}),
  submitting: false,
  errorMessage: '',
})

const emit = defineEmits<{
  submit: [values: RegisterValues]
  back: []
}>()

const values = reactive<RegisterValues>({
  userName: props.initialValues.userName ?? '',
  password: props.initialValues.password ?? '',
  confirmPassword: props.initialValues.confirmPassword ?? '',
})
const errors = reactive<FieldErrors<keyof RegisterValues>>({})

function validate(): boolean {
  errors.userName = values.userName.trim() ? undefined : 'User name is required.'
  if (!errors.userName && values.userName.length > 20) {
    errors.userName = 'User name must be 20 characters or fewer.'
  }
  errors.password = values.password ? undefined : 'Password is required.'
  if (!errors.password && (values.password.length < 6 || values.password.length > 15)) {
    errors.password = 'Password must be between 6 and 15 characters.'
  }
  errors.confirmPassword = values.confirmPassword ? undefined : 'Please confirm your password.'
  if (!errors.confirmPassword && values.password !== values.confirmPassword) {
    errors.confirmPassword = 'Passwords do not match.'
  }
  return !errors.userName && !errors.password && !errors.confirmPassword
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
      <label for="register-user-name">User name</label>
      <InputText id="register-user-name" v-model="values.userName" autocomplete="username" :invalid="Boolean(errors.userName)" />
      <small v-if="errors.userName" class="field-error">{{ errors.userName }}</small>
    </div>
    <div class="field-group">
      <label for="register-password">Password</label>
      <Password id="register-password" v-model="values.password" autocomplete="new-password" :feedback="false" toggle-mask :invalid="Boolean(errors.password)" />
      <small v-if="errors.password" class="field-error">{{ errors.password }}</small>
    </div>
    <div class="field-group">
      <label for="register-confirm-password">Confirm password</label>
      <Password id="register-confirm-password" v-model="values.confirmPassword" autocomplete="new-password" :feedback="false" toggle-mask :invalid="Boolean(errors.confirmPassword)" />
      <small v-if="errors.confirmPassword" class="field-error">{{ errors.confirmPassword }}</small>
    </div>
    <div class="form-actions form-actions-stacked">
      <Button type="submit" label="Register" icon="pi pi-user-plus" :loading="props.submitting" />
      <Button type="button" label="Back to login" icon="pi pi-arrow-left" severity="secondary" outlined @click="emit('back')" />
    </div>
  </form>
</template>
