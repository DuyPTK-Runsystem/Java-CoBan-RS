<script setup lang="ts">
import { computed, reactive } from 'vue'
import Button from 'primevue/button'
import DatePicker from 'primevue/datepicker'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'

import type { StudentFormValues } from '@/types/student'

const props = withDefaults(defineProps<{
  mode?: 'add' | 'edit'
  initialValue?: Partial<StudentFormValues>
  saving?: boolean
  errorMessage?: string
}>(), {
  mode: 'add',
  initialValue: () => ({}),
  saving: false,
  errorMessage: '',
})

const emit = defineEmits<{
  save: [values: StudentFormValues]
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

function generateCode(): void {
  values.studentCode = `STU${Math.floor(100000 + Math.random() * 900000)}`
}

function validate(): boolean {
  errors.studentCode = values.studentCode ? undefined : 'Generate a student code before saving.'
  errors.studentName = values.studentName.trim() ? undefined : 'Student name is required.'
  if (!errors.studentName && values.studentName.length > 20) {
    errors.studentName = 'Student name must be 20 characters or fewer.'
  }
  errors.address = values.address.length <= 255 ? undefined : 'Address must be 255 characters or fewer.'
  return !errors.studentCode && !errors.studentName && !errors.address
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
        <InputText id="student-code" v-model="values.studentCode" maxlength="10" readonly :invalid="Boolean(errors.studentCode)" />
        <Button type="button" label="Generate code" icon="pi pi-refresh" :disabled="isEdit" @click="generateCode" />
      </div>
      <small v-if="errors.studentCode" class="field-error">{{ errors.studentCode }}</small>
    </div>
    <div class="field-group">
      <label for="student-name">Student name</label>
      <InputText id="student-name" v-model="values.studentName" maxlength="20" :invalid="Boolean(errors.studentName)" />
      <small v-if="errors.studentName" class="field-error">{{ errors.studentName }}</small>
    </div>
    <div class="field-group">
      <label for="student-birthday">Birthday</label>
      <DatePicker id="student-birthday" v-model="values.dateOfBirth" date-format="yy-mm-dd" show-icon fluid />
    </div>
    <div class="field-group">
      <label for="student-address">Address</label>
      <InputText id="student-address" v-model="values.address" maxlength="255" :invalid="Boolean(errors.address)" />
      <small v-if="errors.address" class="field-error">{{ errors.address }}</small>
    </div>
    <div class="field-group">
      <label for="student-score">Average score</label>
      <InputNumber id="student-score" v-model="values.averageScore" :min-fraction-digits="0" :max-fraction-digits="2" fluid />
    </div>
    <div class="form-actions">
      <Button type="button" label="Back" icon="pi pi-arrow-left" severity="secondary" outlined @click="emit('back')" />
      <Button type="submit" label="Save" icon="pi pi-check" :loading="props.saving" />
    </div>
  </form>
</template>
