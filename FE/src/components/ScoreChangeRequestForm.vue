<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'

import type { ScoreStatus } from '@/types/scorebook'
import type { CreateScoreChangeRequest } from '@/types/scoreChangeRequest'

export interface ScoreChangeRequestFormContext {
  studentCode: string
  studentName?: string | null
  columnId: number
  columnName?: string | null
  currentStatus?: ScoreStatus
  currentValue?: number | null
}

const props = defineProps<{
  /** The scorebook flow supplies the selected student and score cell. */
  context: ScoreChangeRequestFormContext
  disabled?: boolean
  loading?: boolean
}>()
const emit = defineEmits<{ submit: [request: CreateScoreChangeRequest]; cancel: [] }>()

const status = ref<ScoreStatus>('SCORED')
const value = ref<number | null>(null)
const reason = ref('')
const validation = ref({ value: '', reason: '' })

const statusOptions: { label: string; value: ScoreStatus }[] = [
  { label: 'Có điểm', value: 'SCORED' },
  { label: 'Vắng', value: 'ABSENT' },
  { label: 'Được miễn', value: 'EXEMPTED' },
  { label: 'Hủy', value: 'CANCELLED' },
]
const scoreStatusLabels: Record<ScoreStatus, string> = { SCORED: 'Có điểm', ABSENT: 'Vắng', EXEMPTED: 'Được miễn', CANCELLED: 'Hủy' }
const selectedColumnLabel = computed(() => props.context.columnName ?? `Cột điểm ${props.context.columnId}`)
const studentLabel = computed(() => {
  return props.context.studentName ? `${props.context.studentName} (${props.context.studentCode})` : props.context.studentCode
})
const currentScoreLabel = computed(() => {
  if (!props.context?.currentStatus || props.context.currentStatus === 'SCORED') return props.context?.currentValue ?? 'Chưa nhập'
  return scoreStatusLabels[props.context.currentStatus]
})

function syncContext(): void {
  status.value = props.context.currentStatus ?? 'SCORED'
  value.value = null
}

watch(() => props.context, syncContext, { immediate: true })
watch(status, (nextStatus) => {
  if (nextStatus !== 'SCORED') value.value = null
})

function submit(): void {
  validation.value = { value: '', reason: '' }
  if (status.value === 'SCORED' && value.value === null) validation.value.value = 'Vui lòng nhập điểm đề xuất.'
  if (value.value !== null && (value.value < 0 || value.value > 10)) validation.value.value = 'Điểm đề xuất phải từ 0 đến 10.'
  if (status.value !== 'SCORED' && value.value !== null) validation.value.value = 'Trạng thái không có điểm phải để trống giá trị điểm.'
  if (!reason.value.trim()) validation.value.reason = 'Vui lòng nêu lý do sửa điểm.'
  if (Object.values(validation.value).some(Boolean)) return

  emit('submit', { assessmentColumnId: props.context.columnId, studentCode: props.context.studentCode, proposedStatus: status.value, proposedValue: value.value, reason: reason.value.trim() })
}
</script>

<template>
  <form class="score-change-form" novalidate @submit.prevent="submit">
    <div class="score-context" aria-label="Thông tin điểm cần sửa" aria-readonly="true">
      <div class="score-context-item"><span class="context-label">Học sinh</span><output class="context-value">{{ studentLabel }}</output></div>
      <div class="score-context-item"><span class="context-label">Cột điểm</span><output class="context-value">{{ selectedColumnLabel }}</output></div>
      <div class="score-context-item"><span class="context-label">Điểm hiện tại</span><output class="context-value">{{ currentScoreLabel }}</output></div>
    </div>
    <p class="form-intro">Kiểm tra thông tin điểm hiện tại, sau đó nhập giá trị cần đề nghị và lý do điều chỉnh.</p>

    <div class="field-group"><label for="score-change-status">Trạng thái đề xuất</label><Select id="score-change-status" v-model="status" :options="statusOptions" option-label="label" option-value="value" fluid :disabled="props.disabled" /></div>
    <div class="field-group"><label for="score-change-value">Điểm mới <span v-if="status === 'SCORED'" aria-hidden="true">*</span></label><InputNumber id="score-change-value" v-model="value" :min="0" :max="10" :min-fraction-digits="0" :max-fraction-digits="2" placeholder="Nhập điểm từ 0 đến 10" fluid :disabled="props.disabled || status !== 'SCORED'" :invalid="Boolean(validation.value)" :aria-describedby="validation.value ? 'score-change-value-error' : 'score-change-value-hint'" /><small v-if="validation.value" id="score-change-value-error" class="field-error">{{ validation.value }}</small><small v-else id="score-change-value-hint" class="field-hint">Chỉ nhập điểm khi trạng thái là “Có điểm”.</small></div>
    <div class="field-group"><label for="score-change-reason">Lý do sửa điểm <span aria-hidden="true">*</span></label><Textarea id="score-change-reason" v-model="reason" rows="4" maxlength="1000" placeholder="Ví dụ: Nhập nhầm điểm theo phiếu chấm..." fluid :disabled="props.disabled" :invalid="Boolean(validation.reason)" /><small v-if="validation.reason" class="field-error">{{ validation.reason }}</small><small v-else class="field-hint">Nêu rõ nguyên nhân để người duyệt có đủ thông tin đối chiếu.</small></div>
    <div class="dialog-actions"><Button type="button" label="Đóng" severity="secondary" text :disabled="props.loading" @click="emit('cancel')" /><Button type="submit" label="Gửi yêu cầu" icon="pi pi-send" :loading="props.loading" :disabled="props.disabled" /></div>
  </form>
</template>

<style scoped>
.score-change-form { display: flex; flex-direction: column; gap: 14px; }
.score-context { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; padding: 14px; border: 1px solid var(--p-surface-200); border-radius: 8px; background: var(--p-surface-50); }
.score-context-item { display: flex; min-width: 0; flex-direction: column; gap: 4px; }
.context-label { color: var(--p-text-muted-color); font-size: .8125rem; }
.context-value { margin: 0; color: var(--p-text-color); font-weight: 600; overflow-wrap: anywhere; }
.form-intro { margin: -2px 0 0; color: var(--p-text-muted-color); font-size: .875rem; line-height: 1.45; }
.field-group { display: flex; flex-direction: column; gap: 6px; }
.field-group label { font-weight: 600; }
.field-hint, .field-error { font-size: .8125rem; line-height: 1.35; }
.field-hint { color: var(--p-text-muted-color); }
.field-error { color: var(--p-red-600); }
.dialog-actions { display: flex; align-items: center; justify-content: flex-end; gap: 10px; margin-top: 4px; padding-top: 14px; border-top: 1px solid var(--p-surface-200); }
@media (max-width: 560px) { .score-context { grid-template-columns: 1fr; } .dialog-actions { flex-wrap: wrap; } .dialog-actions button { min-width: 120px; } }
</style>
