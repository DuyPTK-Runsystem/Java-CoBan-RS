<script setup lang="ts">
import { ref, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'

import type {
  BulkScoreItem,
  BulkUpsertStudentScoreRequest,
  ScoreGridColumn,
  ScoreStatus,
  StudentScoreGridRow,
} from '@/types/scorebook'

interface EditableScoreRow {
  studentId: number
  studentCode: string
  studentName: string
  scoreStatus: ScoreStatus
  scoreValue: number | null
  note: string
  expectedVersion: number | null
}

const props = defineProps<{
  visible: boolean
  column: ScoreGridColumn | null
  students: StudentScoreGridRow[]
  saving?: boolean
  errorMessage?: string
}>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  save: [value: BulkUpsertStudentScoreRequest]
  cancel: []
}>()

const rows = ref<EditableScoreRow[]>([])
const initialSignatures = ref<Record<number, string>>({})
const validationMessage = ref('')
const focusedStudentId = ref<number | null>(null)
const statuses = [
  { label: 'Có điểm', value: 'SCORED' },
  { label: 'Vắng', value: 'ABSENT' },
  { label: 'Miễn', value: 'EXEMPTED' },
  { label: 'Hủy', value: 'CANCELLED' },
]

function signature(row: EditableScoreRow): string {
  return JSON.stringify([row.scoreStatus, row.scoreValue, row.note.trim()])
}

function initialize(): void {
  const columnId = props.column?.columnId
  rows.value = props.students.map((student) => {
    const score = columnId === undefined ? undefined : student.scores[String(columnId)]
    return {
      studentId: student.studentId,
      studentCode: student.studentCode,
      studentName: student.studentName,
      scoreStatus: score?.scoreStatus ?? 'SCORED',
      scoreValue: score?.scoreValue ?? null,
      note: score?.note ?? '',
      expectedVersion: score?.version ?? null,
    }
  })
  initialSignatures.value = Object.fromEntries(rows.value.map((row) => [row.studentId, signature(row)]))
  validationMessage.value = ''
}

watch(() => props.visible, (visible) => {
  if (visible) initialize()
}, { immediate: true })

function roundScore(value: number | null | undefined): number | null {
  if (value === null || value === undefined || Number.isNaN(value)) return null
  return Math.round(value * 10) / 10
}

function hasAtMostOneDecimal(input: number): boolean {
  return Math.abs(input * 10 - Math.round(input * 10)) < Number.EPSILON * 100
}

function minFractionDigits(row: EditableScoreRow): 0 | 1 {
  return focusedStudentId.value === row.studentId ? 0 : 1
}

function handleScoreBlur(row: EditableScoreRow): void {
  focusedStudentId.value = null
  if (row.scoreStatus === 'SCORED' && row.scoreValue !== null && row.scoreValue !== undefined) {
    row.scoreValue = roundScore(row.scoreValue)
  }
}

function toRequest(row: EditableScoreRow): BulkScoreItem {
  return {
    studentId: row.studentId,
    scoreStatus: row.scoreStatus,
    scoreValue: row.scoreStatus === 'SCORED' ? roundScore(row.scoreValue) : null,
    note: row.note.trim() || null,
    expectedVersion: row.expectedVersion,
  }
}

function save(): void {
  validationMessage.value = ''
  for (const row of rows.value) {
    if (row.scoreStatus === 'SCORED' && row.scoreValue !== null && row.scoreValue !== undefined) {
      row.scoreValue = roundScore(row.scoreValue)
    }
  }
  const changed = rows.value.filter((row) => initialSignatures.value[row.studentId] !== signature(row))
  if (changed.length === 0) {
    validationMessage.value = 'Chưa có thay đổi để lưu.'
    return
  }
  for (const row of changed) {
    if (row.note.length > 500) {
      validationMessage.value = `Ghi chú của ${row.studentCode} vượt quá 500 ký tự.`
      return
    }
    if (row.scoreStatus === 'SCORED'
      && (row.scoreValue === null || row.scoreValue < 0 || row.scoreValue > 10 || !hasAtMostOneDecimal(row.scoreValue))) {
      validationMessage.value = `Điểm của ${row.studentCode} phải từ 0 đến 10 và có tối đa một chữ số thập phân.`
      return
    }
  }
  emit('save', { items: changed.map(toRequest) })
}
</script>

<template>
  <Dialog :visible="props.visible" header="Nhập điểm hàng loạt" modal :style="{ width: 'min(1120px, calc(100vw - 32px))' }" @update:visible="emit('update:visible', $event)">
    <div class="form-stack">
      <p class="dialog-caption">Cột: {{ props.column?.columnName || props.column?.assessmentType || '—' }}</p>
      <div v-if="validationMessage || props.errorMessage" class="form-alert form-alert-error" role="alert">{{ validationMessage || props.errorMessage }}</div>
      <div class="bulk-score-list">
        <div v-for="row in rows" :key="row.studentId" class="bulk-score-row">
          <span class="field-hint bulk-score-student">{{ row.studentCode }} · {{ row.studentName }}</span>
          <Select v-model="row.scoreStatus" :options="statuses" option-label="label" option-value="value" aria-label="Trạng thái điểm" />
          <InputNumber
            v-if="row.scoreStatus === 'SCORED'"
            v-model="row.scoreValue"
            :min="0"
            :max="10"
            :min-fraction-digits="minFractionDigits(row)"
            :max-fraction-digits="1"
            :use-grouping="false"
            inputmode="decimal"
            placeholder="Điểm"
            @focus="focusedStudentId = row.studentId"
            @blur="handleScoreBlur(row)"
          />
          <span v-else class="field-hint">Không có điểm số</span>
          <InputText v-model="row.note" maxlength="500" placeholder="Ghi chú" aria-label="Ghi chú" />
        </div>
      </div>
      <div class="form-actions">
        <Button label="Hủy" text :disabled="props.saving" @click="emit('cancel')" />
        <Button label="Lưu hàng loạt" icon="pi pi-check" :loading="props.saving" :disabled="props.saving" @click="save" />
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
.bulk-score-list { display: grid; gap: 14px; max-height: 56vh; overflow-y: auto; padding: 4px 0; }
.bulk-score-row { display: grid; grid-template-columns: minmax(260px, 1.35fr) minmax(180px, .9fr) minmax(150px, .75fr) minmax(200px, 1fr); align-items: center; gap: 14px; }
.bulk-score-row > * { min-width: 0; }
.bulk-score-row :deep(.p-select), .bulk-score-row :deep(.p-inputnumber), .bulk-score-row :deep(.p-inputtext), .bulk-score-row :deep(.p-inputnumber-input) { width: 100%; }
.bulk-score-student { color: #334155; }
@media (max-width: 720px) { .bulk-score-row { grid-template-columns: 1fr; padding-bottom: 12px; border-bottom: 1px solid #e2e8f0; } }
</style>
