<script setup lang="ts">
import { reactive, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'

import FormAlert from '@/components/FormAlert.vue'
import type { AttendanceExceptionStatus, AttendanceSession, AttendanceStudent, UpsertAttendanceExceptionRequest } from '@/types/attendance'

const props = withDefaults(defineProps<{
  visible?: boolean
  student?: AttendanceStudent | null
  session?: AttendanceSession | null
  saving?: boolean
  errorMessage?: string
}>(), {
  visible: false,
  student: null,
  session: null,
  saving: false,
  errorMessage: '',
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  save: [values: UpsertAttendanceExceptionRequest]
  cancel: []
}>()

const values = reactive<{ status: AttendanceExceptionStatus; note: string }>({ status: 'ABSENT', note: '' })
const errors = reactive<Record<string, string>>({})
const statusOptions: Array<{ label: string; value: AttendanceExceptionStatus }> = [
  { label: 'ABSENT · Vắng không phép', value: 'ABSENT' },
  { label: 'EXCUSED · Vắng có phép', value: 'EXCUSED' },
  { label: 'LATE · Đi trễ', value: 'LATE' },
  { label: 'EARLY_LEAVE · Về sớm', value: 'EARLY_LEAVE' },
]

watch(() => [props.visible, props.student], () => {
  const status = props.student?.attendanceRecordId !== null && props.student?.status in { ABSENT: true, EXCUSED: true, LATE: true, EARLY_LEAVE: true }
    ? props.student.status as AttendanceExceptionStatus
    : 'ABSENT'
  values.status = status
  values.note = props.student?.note ?? ''
  Object.keys(errors).forEach((key) => delete errors[key])
}, { immediate: true, deep: true })

function save(): void {
  Object.keys(errors).forEach((key) => delete errors[key])
  if (!values.status) errors.status = 'Trạng thái là bắt buộc.'
  if (values.note.length > 500) errors.note = 'Ghi chú tối đa 500 ký tự.'
  if (Object.keys(errors).length === 0) emit('save', { status: values.status, note: values.note.trim() || null })
}

function close(): void {
  emit('update:visible', false)
  emit('cancel')
}
</script>

<template>
  <Dialog :visible="props.visible" modal header="Ghi nhận ngoại lệ điểm danh" :style="{ width: 'min(100% - 2rem, 640px)' }" :closable="!props.saving" @update:visible="emit('update:visible', $event)">
    <FormAlert v-if="props.errorMessage" tone="error" :message="props.errorMessage" />
    <form class="form-stack attendance-exception-form" novalidate @submit.prevent="save">
      <div class="context-strip"><strong>{{ props.student?.studentCode }} · {{ props.student?.studentName }}</strong><span v-if="props.session">Session #{{ props.session.sessionId }} · {{ props.session.attendanceDate }} · {{ props.session.sessionPeriod === 'MORNING' ? 'Sáng' : 'Chiều' }}</span></div>
      <div class="field-group">
        <label for="attendance-exception-status">Trạng thái exception *</label>
        <Select id="attendance-exception-status" v-model="values.status" :options="statusOptions" option-label="label" option-value="value" fluid :invalid="Boolean(errors.status)" :disabled="props.saving" />
        <small v-if="errors.status" class="field-error">{{ errors.status }}</small>
      </div>
      <div class="field-group">
        <label for="attendance-exception-note">Ghi chú</label>
        <Textarea id="attendance-exception-note" v-model="values.note" rows="4" maxlength="500" auto-resize fluid :invalid="Boolean(errors.note)" :disabled="props.saving" placeholder="Tối đa 500 ký tự" />
        <small v-if="errors.note" class="field-error">{{ errors.note }}</small>
      </div>
      <div class="form-actions"><Button type="button" label="Hủy" icon="pi pi-times" severity="secondary" outlined :disabled="props.saving" @click="close" /><Button type="submit" label="Lưu ngoại lệ" icon="pi pi-check" :loading="props.saving" /></div>
    </form>
  </Dialog>
</template>
