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
  errors.userName = values.userName.trim() ? undefined : 'Tên đăng nhập là bắt buộc.'
  if (!errors.userName && values.userName.length > 20) {
    errors.userName = 'Tên đăng nhập tối đa 20 ký tự.'
  }
  errors.password = values.password ? undefined : 'Mật khẩu là bắt buộc.'
  if (!errors.password && (values.password.length < 6 || values.password.length > 15)) {
    errors.password = 'Mật khẩu phải từ 6 đến 15 ký tự.'
  }
  errors.confirmPassword = values.confirmPassword ? undefined : 'Vui lòng xác nhận mật khẩu.'
  if (!errors.confirmPassword && values.password !== values.confirmPassword) {
    errors.confirmPassword = 'Mật khẩu xác nhận không khớp.'
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
      <label for="register-user-name">Tên đăng nhập <span class="required-mark" aria-hidden="true">*</span></label>
      <InputText id="register-user-name" v-model="values.userName" autocomplete="username" :invalid="Boolean(errors.userName)" aria-required="true" />
      <small v-if="errors.userName" class="field-error">{{ errors.userName }}</small>
    </div>
    <div class="field-group">
      <label for="register-password">Mật khẩu <span class="required-mark" aria-hidden="true">*</span></label>
      <Password id="register-password" v-model="values.password" autocomplete="new-password" :feedback="false" toggle-mask :invalid="Boolean(errors.password)" aria-required="true" />
      <small v-if="errors.password" class="field-error">{{ errors.password }}</small>
    </div>
    <div class="field-group">
      <label for="register-confirm-password">Xác nhận mật khẩu <span class="required-mark" aria-hidden="true">*</span></label>
      <Password id="register-confirm-password" v-model="values.confirmPassword" autocomplete="new-password" :feedback="false" toggle-mask :invalid="Boolean(errors.confirmPassword)" aria-required="true" />
      <small v-if="errors.confirmPassword" class="field-error">{{ errors.confirmPassword }}</small>
    </div>
    <div class="form-actions form-actions-stacked">
      <Button type="submit" label="Đăng ký" icon="pi pi-user-plus" :loading="props.submitting" />
      <Button type="button" label="Quay lại đăng nhập" icon="pi pi-arrow-left" severity="secondary" outlined @click="emit('back')" />
    </div>
  </form>
</template>
