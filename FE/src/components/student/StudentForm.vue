<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import DatePicker from 'primevue/datepicker'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Select from 'primevue/select'

import type { StudentFormValues } from '@/types/student'

const props = withDefaults(defineProps<{
  mode?: 'add' | 'edit'
  initialValue?: Partial<StudentFormValues>
  saving?: boolean
  generating?: boolean
  errorMessage?: string
  canProvisionAccount?: boolean
}>(), {
  mode: 'add',
  initialValue: () => ({}),
  saving: false,
  generating: false,
  errorMessage: '',
  canProvisionAccount: true,
})

const emit = defineEmits<{
  (e: 'save', values: StudentFormValues): void
  (e: 'generateCode'): void
  (e: 'back'): void
}>()

const values = reactive<StudentFormValues>({
  studentId: props.initialValue.studentId,
  studentCode: props.initialValue.studentCode ?? '',
  studentName: props.initialValue.studentName ?? '',
  dateOfBirth: props.initialValue.dateOfBirth ?? null,
  gender: props.initialValue.gender ?? null,
  address: props.initialValue.address ?? '',
  status: props.initialValue.status ?? 'ACTIVE',
  provisionAccount: props.initialValue.provisionAccount ?? true,
  username: props.initialValue.username ?? '',
  password: props.initialValue.password ?? '',
})

const errors = reactive<Record<string, string | undefined>>({})
const isEdit = computed(() => props.mode === 'edit')
const studentCodePattern = /^STU\d{7}$/
const studentCodeTypingPattern = /^(?:\d{0,7}|STU\d{0,7})$/

watch(() => props.initialValue, (initialValue) => {
  Object.assign(values, {
    studentId: initialValue.studentId,
    studentCode: initialValue.studentCode ?? '',
    studentName: initialValue.studentName ?? '',
    dateOfBirth: initialValue.dateOfBirth ?? null,
    gender: initialValue.gender ?? null,
    address: initialValue.address ?? '',
    status: initialValue.status ?? 'ACTIVE',
    provisionAccount: initialValue.provisionAccount ?? true,
    username: initialValue.username ?? '',
    password: initialValue.password ?? '',
  })
  validateStudentCode(false)
}, { deep: true, immediate: true })

watch(() => props.errorMessage, (msg) => {
  if (!msg) return
  if (msg.toLowerCase().includes('mã sinh viên') || msg.toLowerCase().includes('studentcode')) {
    errors.studentCode = msg
  } else if (msg.toLowerCase().includes('tên đăng nhập') || msg.toLowerCase().includes('username')) {
    errors.username = msg
  }
})

function validateStudentCode(showRequiredError: boolean): boolean {
  const studentCode = values.studentCode.trim()
  values.studentCode = studentCode

  if (!studentCode) {
    errors.studentCode = showRequiredError ? 'Nhập mã học sinh hoặc tạo mã trước khi lưu.' : undefined
    return false
  }

  errors.studentCode = studentCodePattern.test(studentCode)
    ? undefined
    : 'Mã học sinh phải bắt đầu bằng STU và có đúng 7 chữ số phía sau.'
  return !errors.studentCode
}

function updateStudentCode(value: string | undefined): void {
  const nextValue = value ?? ''
  values.studentCode = nextValue
  if (studentCodeTypingPattern.test(nextValue.trim())) {
    errors.studentCode = undefined
    return
  }
  validateStudentCode(false)
}

function normalizeStudentCode(): void {
  const value = values.studentCode.trim()
  if (/^\d{1,7}$/.test(value)) {
    values.studentCode = `STU${value.padStart(7, '0')}`
  }
  validateStudentCode(true)
}

function validate(): boolean {
  const isStudentCodeValid = validateStudentCode(true)
  errors.studentName = values.studentName.trim() ? undefined : 'Vui lòng nhập họ và tên học sinh.'
  if (!errors.studentName && values.studentName.length > 35) {
    errors.studentName = 'Họ và tên học sinh không được vượt quá 35 ký tự.'
  }
  errors.address = values.address.length <= 255 ? undefined : 'Địa chỉ không được vượt quá 255 ký tự.'
  if (values.provisionAccount && values.username && values.username.trim().length > 20) {
    errors.username = 'Tên đăng nhập không được vượt quá 20 ký tự.'
  } else {
    errors.username = undefined
  }

  return isStudentCodeValid && !errors.studentName && !errors.address && !errors.username
}

function save(): void {
  if (validate()) {
    emit('save', { ...values })
  }
}
</script>

<template>
  <form class="form-stack" novalidate @submit.prevent="save">
    <div v-if="props.errorMessage" class="form-alert form-alert-error" role="alert">
      {{ props.errorMessage }}
    </div>

    <div v-if="isEdit" class="field-group">
      <label for="student-id">Mã định danh học sinh</label>
      <InputText id="student-id" :model-value="String(values.studentId ?? '')" disabled />
    </div>

    <div class="field-group">
      <label for="student-code">Mã học sinh</label>
      <div class="inline-field">
        <InputText
          id="student-code"
          :model-value="values.studentCode"
          placeholder="Ví dụ: STU1234567"
          :disabled="isEdit"
          :invalid="Boolean(errors.studentCode)"
          @update:model-value="updateStudentCode"
          @blur="normalizeStudentCode"
        />
        <Button
          type="button"
          label="Tạo mã"
          icon="pi pi-refresh"
          :disabled="isEdit"
          :loading="props.generating"
          @click="emit('generateCode')"
        />
      </div>
      <small class="field-hint">Định dạng: STU và 7 chữ số</small>
      <small v-if="errors.studentCode" class="field-error">{{ errors.studentCode }}</small>
    </div>

    <div class="field-group">
      <label for="student-name">Họ và tên</label>
      <InputText
        id="student-name"
        v-model="values.studentName"
        maxlength="35"
        placeholder="Ví dụ: Nguyễn Văn An"
        :invalid="Boolean(errors.studentName)"
      />
      <small v-if="errors.studentName" class="field-error">{{ errors.studentName }}</small>
    </div>

    <div class="field-group">
      <label for="student-birthday">Ngày sinh</label>
      <DatePicker
        id="student-birthday"
        v-model="values.dateOfBirth"
        date-format="dd-mm-yy"
        placeholder="dd-mm-yyyy"
        show-icon
        fluid
      />
    </div>

    <div class="field-group">
      <label for="student-address">Địa chỉ</label>
      <InputText
        id="student-address"
        v-model="values.address"
        maxlength="255"
        placeholder="Ví dụ: Thành phố Hồ Chí Minh"
        :invalid="Boolean(errors.address)"
      />
      <small v-if="errors.address" class="field-error">{{ errors.address }}</small>
    </div>

    <div class="field-group">
      <label for="student-gender">Giới tính</label>
      <Select
        id="student-gender"
        v-model="values.gender"
        :options="[{ label: 'Nam', value: 'MALE' }, { label: 'Nữ', value: 'FEMALE' }]"
        option-label="label"
        option-value="value"
        placeholder="Chưa cập nhật"
        show-clear
        fluid
      />
    </div>

    <!-- V3 Account Provisioning Section (Only in Add mode for ADMIN / ACADEMIC_OFFICE) -->
    <div v-if="!isEdit && props.canProvisionAccount" class="account-provision-section">
      <div class="checkbox-group">
        <Checkbox
          id="provision-account"
          v-model="values.provisionAccount"
          :binary="true"
        />
        <label for="provision-account" class="font-medium cursor-pointer">
          Cấp tài khoản đăng nhập cho học sinh
        </label>
      </div>
      <p class="section-hint">
        Tạo tài khoản đăng nhập hệ thống với vai trò <strong>Học sinh</strong>.
      </p>

      <div v-if="values.provisionAccount" class="account-fields">
        <div class="field-group">
          <label for="student-username">Tên đăng nhập (Tùy chọn)</label>
          <InputText
            id="student-username"
            v-model="values.username"
            maxlength="20"
            placeholder="Để trống để tự động sinh theo họ tên và mã học sinh"
            :invalid="Boolean(errors.username)"
          />
          <small class="field-hint">Nếu để trống, hệ thống tự sinh username và sẽ hiển thị khi tạo thành công.</small>
          <small v-if="errors.username" class="field-error">{{ errors.username }}</small>
        </div>

        <div class="field-group">
          <label for="student-password">Mật khẩu khởi tạo (Tùy chọn)</label>
          <Password
            id="student-password"
            v-model="values.password"
            placeholder="Mật khẩu có độ dài từ 6 đến 15 kí tự, hoặc bỏ trống để dùng mật khẩu mặc định"
            :feedback="false"
            toggle-mask
            fluid
          />
          <small class="field-hint">Mật khẩu mặc định: 12345678</small>
        </div>
      </div>
    </div>

    <div class="form-actions">
      <Button
        type="button"
        label="Quay lại"
        icon="pi pi-arrow-left"
        severity="secondary"
        outlined
        @click="emit('back')"
      />
      <Button
        type="submit"
        label="Lưu"
        icon="pi pi-check"
        :loading="props.saving"
      />
    </div>
  </form>
</template>

<style scoped>
.account-provision-section {
  padding: 1rem;
  background-color: var(--surface-50, #f8fafc);
  border: 1px solid var(--surface-200, #e2e8f0);
  border-radius: 6px;
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
}
.checkbox-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.cursor-pointer {
  cursor: pointer;
}
.section-hint {
  font-size: 0.8125rem;
  color: var(--text-color-secondary, #64748b);
  margin-top: 0.35rem;
  margin-bottom: 0.75rem;
}
.account-fields {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding-top: 0.5rem;
  border-top: 1px dashed var(--surface-300, #cbd5e1);
}
</style>
