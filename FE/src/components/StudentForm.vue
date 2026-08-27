<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import Button from 'primevue/button'
import DatePicker from 'primevue/datepicker'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'

import type { StudentFormValues } from '@/types/student'

const props = withDefaults(defineProps<{
  mode?: 'add' | 'edit'
  initialValue?: Partial<StudentFormValues>
  saving?: boolean
  generating?: boolean
  errorMessage?: string
}>(), {
  mode: 'add',
  initialValue: () => ({}),
  saving: false,
  generating: false,
  errorMessage: '',
})

const emit = defineEmits<{
  save: [values: StudentFormValues]
  generateCode: []
  back: []
}>()

const values = reactive<StudentFormValues>({
  studentId: props.initialValue.studentId,
  studentCode: props.initialValue.studentCode ?? '',
  studentName: props.initialValue.studentName ?? '',
  dateOfBirth: props.initialValue.dateOfBirth ?? null,
  address: props.initialValue.address ?? '',
  averageScore: props.initialValue.averageScore ?? null,
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
    address: initialValue.address ?? '',
    averageScore: initialValue.averageScore ?? null,
  })
  validateStudentCode(false)
}, { deep: true, immediate: true })

function validateStudentCode(showRequiredError: boolean): boolean {
  const studentCode = values.studentCode.trim()
  values.studentCode = studentCode

  if (!studentCode) {
    errors.studentCode = showRequiredError ? 'Enter a student code or generate one before saving.' : undefined
    return false
  }

  errors.studentCode = studentCodePattern.test(studentCode)
    ? undefined
    : 'Use the format STU followed by exactly 7 digits.'
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
  errors.studentName = values.studentName.trim() ? undefined : 'Student name is required.'
  if (!errors.studentName && values.studentName.length > 35) {
    errors.studentName = 'Student name must be 35 characters or fewer.'
  }
  errors.address = values.address.length <= 255 ? undefined : 'Address must be 255 characters or fewer.'
  errors.averageScore = values.averageScore === null
    || (values.averageScore >= 0 && values.averageScore <= 10)
    ? undefined
    : 'Average score must be between 0 and 10.'
  return isStudentCodeValid && !errors.studentName && !errors.address && !errors.averageScore
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
      <label for="student-id">Student id</label>
      <InputText id="student-id" :model-value="String(values.studentId ?? '')" disabled />
    </div>
    <div class="field-group">
      <label for="student-code">Student code</label>
      <div class="inline-field">
        <InputText
          id="student-code"
          :model-value="values.studentCode"
          placeholder="Example: STU1234567"
          :disabled="isEdit"
          :invalid="Boolean(errors.studentCode)"
          @update:model-value="updateStudentCode"
          @blur="normalizeStudentCode"
        />
        <Button type="button" label="Generate code" icon="pi pi-refresh" :disabled="isEdit" :loading="props.generating" @click="emit('generateCode')" />
      </div>
      <small class="field-hint">Format: STUxxxxxxx</small>
      <small v-if="errors.studentCode" class="field-error">{{ errors.studentCode }}</small>
    </div>
    <div class="field-group">
      <label for="student-name">Student name</label>
      <InputText id="student-name" v-model="values.studentName" maxlength="35" placeholder="Example: John Doe" :invalid="Boolean(errors.studentName)" />
      <small v-if="errors.studentName" class="field-error">{{ errors.studentName }}</small>
    </div>
    <div class="field-group">
      <label for="student-birthday">Birthday</label>
      <DatePicker id="student-birthday" v-model="values.dateOfBirth" date-format="dd-mm-yy" placeholder="dd-mm-yyyy" show-icon fluid />
    </div>
    <div class="field-group">
      <label for="student-address">Address</label>
      <InputText id="student-address" v-model="values.address" maxlength="255" placeholder="Example: HCMC, Vietnam" :invalid="Boolean(errors.address)" />
      <small v-if="errors.address" class="field-error">{{ errors.address }}</small>
    </div>
    <div class="field-group">
      <label for="student-score">Average score</label>
      <InputNumber id="student-score" v-model="values.averageScore" placeholder="Example: 6.7" :min="0" :max="10" :min-fraction-digits="0" :max-fraction-digits="2" :invalid="Boolean(errors.averageScore)" fluid />
      <small v-if="errors.averageScore" class="field-error">{{ errors.averageScore }}</small>
    </div>
    <div class="form-actions">
      <Button type="button" label="Back" icon="pi pi-arrow-left" severity="secondary" outlined @click="emit('back')" />
      <Button type="submit" label="Save" icon="pi pi-check" :loading="props.saving" />
    </div>
  </form>
</template>
