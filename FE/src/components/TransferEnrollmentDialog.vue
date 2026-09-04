<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import DatePicker from 'primevue/datepicker'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'

import FormAlert from '@/components/FormAlert.vue'
import type { SchoolClass } from '@/types/academic'
import type { ClassStudent, TransferEnrollmentFormValues } from '@/types/enrollment'

const props = withDefaults(defineProps<{
  visible?: boolean
  student?: ClassStudent | null
  currentClassId?: number | null
  targetClasses?: SchoolClass[]
  saving?: boolean
  errorMessage?: string
}>(), {
  visible: false,
  student: null,
  currentClassId: null,
  targetClasses: () => [],
  saving: false,
  errorMessage: '',
})

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  submit: [values: TransferEnrollmentFormValues]
  cancel: []
}>()

const targetClassId = ref<number | null>(null)
const effectiveAt = ref<Date | null>(new Date())
const reason = ref('')
const errors = ref<Record<string, string>>({})
const availableClasses = computed(() => props.targetClasses.filter((schoolClass) => schoolClass.id !== props.currentClassId && schoolClass.status !== 'CLOSED'))

function sync(): void {
  targetClassId.value = null
  effectiveAt.value = new Date()
  reason.value = ''
  errors.value = {}
}
watch(() => [props.visible, props.student], sync, { deep: true })

function pad(value: number): string { return String(value).padStart(2, '0') }
function formatLocalDateTime(value: Date): string {
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
}

function close(): void {
  if (props.saving) return
  emit('update:visible', false)
  emit('cancel')
}

function submit(): void {
  errors.value = {}
  if (!targetClassId.value) errors.value.targetClassId = 'Lớp đích là bắt buộc.'
  if (targetClassId.value === props.currentClassId) errors.value.targetClassId = 'Lớp đích phải khác lớp hiện tại.'
  if (!effectiveAt.value) errors.value.effectiveAt = 'Ngày hiệu lực là bắt buộc.'
  else if (effectiveAt.value.getTime() > Date.now()) errors.value.effectiveAt = 'Ngày hiệu lực không được ở tương lai.'
  if (reason.value.length > 500) errors.value.reason = 'Lý do tối đa 500 ký tự.'
  if (Object.keys(errors.value).length > 0 || !targetClassId.value || !effectiveAt.value) return
  emit('submit', { targetClassId: targetClassId.value, effectiveAt: formatLocalDateTime(effectiveAt.value), reason: reason.value.trim() })
}
</script>

<template>
  <Dialog :visible="props.visible" modal header="Chuyển lớp cho học sinh" :style="{ width: 'min(100% - 2rem, 620px)' }" :closable="!props.saving" @update:visible="emit('update:visible', $event)">
    <p class="dialog-caption">Lịch sử enrollment cũ vẫn được giữ lại sau khi backend chuyển lớp.</p>
    <FormAlert v-if="props.errorMessage" tone="error" :message="props.errorMessage" />
    <div class="selection-summary"><span class="selection-summary-label">Học sinh</span><strong>{{ props.student?.studentCode || 'Chưa chọn' }}</strong><p class="selection-student-name">{{ props.student?.studentName }}</p></div>
    <form class="form-stack" novalidate @submit.prevent="submit">
      <div class="field-group">
        <label for="transfer-target-class">Lớp đích</label>
        <Select id="transfer-target-class" v-model="targetClassId" :options="availableClasses" option-label="classCode" option-value="id" placeholder="Chọn lớp đích" :disabled="props.saving" :invalid="Boolean(errors.targetClassId)" fluid />
        <small v-if="errors.targetClassId" class="field-error">{{ errors.targetClassId }}</small>
      </div>
      <div class="field-group">
        <label for="transfer-effective-at">Ngày hiệu lực</label>
        <DatePicker id="transfer-effective-at" v-model="effectiveAt" show-time show-seconds hour-format="24" date-format="dd/mm/yy" show-icon fluid :disabled="props.saving" :invalid="Boolean(errors.effectiveAt)" />
        <small v-if="errors.effectiveAt" class="field-error">{{ errors.effectiveAt }}</small>
      </div>
      <div class="field-group">
        <label for="transfer-reason">Lý do</label>
        <Textarea id="transfer-reason" v-model="reason" rows="4" maxlength="500" auto-resize :disabled="props.saving" :invalid="Boolean(errors.reason)" />
        <small v-if="errors.reason" class="field-error">{{ errors.reason }}</small>
      </div>
      <div class="form-actions"><Button type="button" label="Hủy" icon="pi pi-times" severity="secondary" outlined :disabled="props.saving" @click="close" /><Button type="submit" label="Chuyển lớp" icon="pi pi-arrow-right-arrow-left" :loading="props.saving" /></div>
    </form>
  </Dialog>
</template>
