<script setup lang="ts">
import { computed, ref } from 'vue'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'

import type { AssessmentColumn, ScoreStatus } from '@/types/scorebook'
import type { CreateScoreChangeRequest } from '@/types/scoreChangeRequest'

const props = defineProps<{ columns: AssessmentColumn[]; disabled?: boolean; loading?: boolean }>()
const emit = defineEmits<{ submit: [request: CreateScoreChangeRequest]; cancel: [] }>()

const columnId = ref<number | null>(null)
const studentCode = ref('')
const status = ref<ScoreStatus>('SCORED')
const value = ref<number | null>(null)
const reason = ref('')
const validation = ref('')

const statusOptions: { label: string; value: ScoreStatus }[] = [
  { label: 'Có điểm', value: 'SCORED' },
  { label: 'Vắng', value: 'ABSENT' },
  { label: 'Được miễn', value: 'EXEMPTED' },
  { label: 'Hủy', value: 'CANCELLED' },
]
const columnOptions = computed(() => props.columns.map((column) => ({ label: column.columnName ?? `Cột điểm ${column.columnNo}`, value: column.id })))

function submit(): void {
  validation.value = ''
  if (columnId.value === null || !studentCode.value.trim() || !reason.value.trim()) {
    validation.value = 'Vui lòng chọn cột điểm, nhập mã học sinh và nêu lý do.'
    return
  }
  if (status.value === 'SCORED' && value.value === null) {
    validation.value = 'Vui lòng nhập điểm đề xuất.'
    return
  }
  if (status.value !== 'SCORED' && value.value !== null) {
    validation.value = 'Trạng thái không có điểm phải để trống giá trị điểm.'
    return
  }
  emit('submit', { assessmentColumnId: columnId.value, studentCode: studentCode.value.trim(), proposedStatus: status.value, proposedValue: value.value, reason: reason.value.trim() })
}
</script>

<template>
  <form class="score-change-form" @submit.prevent="submit">
    <div class="field-group"><label for="score-change-column">Cột điểm</label><Select id="score-change-column" v-model="columnId" :options="columnOptions" option-label="label" option-value="value" placeholder="Chọn cột điểm" fluid :disabled="props.disabled" /></div>
    <div class="field-group"><label for="score-change-student">Mã học sinh</label><InputText id="score-change-student" v-model="studentCode" placeholder="Ví dụ: HS-001" fluid :disabled="props.disabled" /></div>
    <div class="field-group"><label for="score-change-status">Trạng thái điểm đề xuất</label><Select id="score-change-status" v-model="status" :options="statusOptions" option-label="label" option-value="value" fluid :disabled="props.disabled" /></div>
    <div class="field-group"><label for="score-change-value">Điểm đề xuất</label><InputNumber id="score-change-value" v-model="value" :min="0" :max="10" :min-fraction-digits="0" :max-fraction-digits="2" fluid :disabled="props.disabled || status !== 'SCORED'" /></div>
    <div class="field-group"><label for="score-change-reason">Lý do sửa điểm</label><Textarea id="score-change-reason" v-model="reason" rows="4" maxlength="1000" fluid :disabled="props.disabled" /></div>
    <p v-if="validation" class="form-alert form-alert-error" role="alert">{{ validation }}</p>
    <div class="dialog-actions"><Button type="button" label="Đóng" severity="secondary" text :disabled="props.loading" @click="emit('cancel')" /><Button type="submit" label="Gửi yêu cầu" icon="pi pi-send" :loading="props.loading" :disabled="props.disabled" /></div>
  </form>
</template>
