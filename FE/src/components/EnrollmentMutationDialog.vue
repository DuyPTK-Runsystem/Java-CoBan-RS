<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import DatePicker from 'primevue/datepicker'
import Dialog from 'primevue/dialog'

import FormAlert from '@/components/FormAlert.vue'
import type { BulkEnrollmentFormValues, CreateEnrollmentFormValues } from '@/types/enrollment'
import type { UnassignedStudent } from '@/types/enrollment'

const props = withDefaults(defineProps<{
  visible?: boolean
  mode?: 'single' | 'bulk'
  students?: UnassignedStudent[]
  classLabel?: string
  saving?: boolean
  errorMessage?: string
}>(), {
  visible: false,
  mode: 'single',
  students: () => [],
  classLabel: '',
  saving: false,
  errorMessage: '',
})

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  submit: [values: CreateEnrollmentFormValues | BulkEnrollmentFormValues]
  cancel: []
}>()

const enrolledAt = ref<Date | null>(null)
const validationMessage = ref('')
const isBulk = computed(() => props.mode === 'bulk')
const heading = computed(() => isBulk.value ? 'Xếp nhiều học sinh vào lớp' : 'Xếp học sinh vào lớp')

function pad(value: number): string { return String(value).padStart(2, '0') }
function formatLocalDateTime(value: Date): string {
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
}

function sync(): void {
  enrolledAt.value = null
  validationMessage.value = ''
}

watch(() => [props.visible, props.mode, props.students], sync, { deep: true })

function close(): void {
  if (props.saving) return
  emit('update:visible', false)
  emit('cancel')
}

function submit(): void {
  validationMessage.value = ''
  if (props.students.length === 0) {
    validationMessage.value = 'Cần chọn ít nhất một học sinh.'
    return
  }
  const enrolledAtValue = enrolledAt.value ? formatLocalDateTime(enrolledAt.value) : ''
  if (isBulk.value) {
    emit('submit', { studentIds: props.students.map((student) => student.studentId), enrolledAt: enrolledAtValue })
    return
  }
  emit('submit', { studentId: props.students[0].studentId, enrolledAt: enrolledAtValue })
}
</script>

<template>
  <Dialog :visible="props.visible" modal :header="heading" :style="{ width: 'min(100% - 2rem, 620px)' }" :closable="!props.saving" @update:visible="emit('update:visible', $event)">
    <p class="dialog-caption">Context lớp: <strong>{{ props.classLabel || 'chưa chọn' }}</strong>. Cảnh báo sĩ số từ backend không chặn thao tác thành công.</p>
    <FormAlert v-if="props.errorMessage" tone="error" :message="props.errorMessage" />
    <FormAlert v-if="validationMessage" tone="error" :message="validationMessage" />
    <div class="selection-summary">
      <span class="selection-summary-label">{{ isBulk ? 'Số học sinh được chọn' : 'Học sinh' }}</span>
      <strong>{{ isBulk ? props.students.length : props.students[0]?.studentCode || 'Chưa chọn' }}</strong>
      <ul v-if="isBulk" class="selection-list">
        <li v-for="student in props.students" :key="student.studentId"><strong>{{ student.studentCode }}</strong> · {{ student.studentName }}</li>
      </ul>
      <p v-else-if="props.students[0]" class="selection-student-name">{{ props.students[0].studentName }}</p>
    </div>
    <div class="field-group">
      <label for="enrollment-enrolled-at">Ngày xếp lớp (tùy chọn)</label>
      <DatePicker id="enrollment-enrolled-at" v-model="enrolledAt" show-time show-seconds hour-format="24" date-format="dd/mm/yy" placeholder="Để trống để backend mặc định" show-icon fluid :disabled="props.saving" />
      <small class="field-hint">Giữ định dạng giờ địa phương, không đổi sang UTC.</small>
    </div>
    <div class="form-actions">
      <Button type="button" label="Hủy" icon="pi pi-times" severity="secondary" outlined :disabled="props.saving" @click="close" />
      <Button type="button" :label="isBulk ? 'Xếp học sinh' : 'Xếp vào lớp'" icon="pi pi-check" :loading="props.saving" @click="submit" />
    </div>
  </Dialog>
</template>
