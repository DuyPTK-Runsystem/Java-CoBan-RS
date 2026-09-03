<script setup lang="ts">
import { ref, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'

import type { ScoreStatus, StudentScore, UpsertStudentScoreRequest } from '@/types/scorebook'

export interface ScoreChangeRequestContext {
  studentName: string
  score: StudentScore | null
}

const props = defineProps<{
  visible: boolean
  studentName?: string
  score: StudentScore | null
  readOnly?: boolean
  saving?: boolean
  errorMessage?: string
}>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  save: [value: UpsertStudentScoreRequest]
  'request-change': [context: ScoreChangeRequestContext]
  cancel: []
}>()

const status = ref<ScoreStatus>('SCORED')
const value = ref<number | null>(null)
const note = ref('')
const validationMessage = ref('')
const scoreFocused = ref(false)
const statuses = [
  { label: 'Có điểm', value: 'SCORED' },
  { label: 'Vắng', value: 'ABSENT' },
  { label: 'Miễn', value: 'EXEMPTED' },
  { label: 'Hủy', value: 'CANCELLED' },
]

watch(() => props.visible, (visible) => {
  if (!visible) return
  status.value = props.score?.scoreStatus ?? 'SCORED'
  value.value = props.score?.scoreValue ?? null
  note.value = props.score?.note ?? ''
  validationMessage.value = ''
})

function hasAtMostOneDecimal(input: number): boolean {
  return Math.abs(input * 10 - Math.round(input * 10)) < Number.EPSILON * 100
}

function minFractionDigits(): 0 | 1 {
  return scoreFocused.value ? 0 : 1
}

function save(): void {
  if (props.readOnly) return
  validationMessage.value = ''
  if (note.value.length > 500) {
    validationMessage.value = 'Ghi chú không được vượt quá 500 ký tự.'
    return
  }
  if (status.value === 'SCORED') {
    if (value.value === null) {
      validationMessage.value = 'Vui lòng nhập điểm.'
      return
    }
    if (value.value < 0 || value.value > 10 || !hasAtMostOneDecimal(value.value)) {
      validationMessage.value = 'Điểm phải từ 0 đến 10 và có tối đa một chữ số thập phân.'
      return
    }
  }
  emit('save', {
    scoreStatus: status.value,
    scoreValue: status.value === 'SCORED' ? value.value : null,
    note: note.value.trim() || null,
    expectedVersion: props.score?.version ?? null,
  })
}

function requestChange(): void {
  emit('request-change', {
    studentName: props.studentName ?? props.score?.studentName ?? 'Học sinh',
    score: props.score,
  })
}
</script>

<template>
  <Dialog :visible="props.visible" header="Nhập điểm" modal :style="{ width: 'min(520px, calc(100vw - 32px))' }" @update:visible="emit('update:visible', $event)">
    <div class="form-stack">
      <p class="dialog-caption">{{ props.studentName ?? 'Học sinh' }}</p>
      <div v-if="validationMessage || props.errorMessage" class="form-alert form-alert-error" role="alert">{{ validationMessage || props.errorMessage }}</div>
      <div class="field-group">
        <label for="score-status">Trạng thái</label>
        <Select id="score-status" v-model="status" :options="statuses" option-label="label" option-value="value" fluid :disabled="props.readOnly" />
      </div>
      <div v-if="status === 'SCORED'" class="field-group">
        <label for="score-value">Điểm (0–10)</label>
        <InputNumber
          id="score-value"
          v-model="value"
          :min="0"
          :max="10"
          :min-fraction-digits="minFractionDigits()"
          :max-fraction-digits="1"
          :use-grouping="false"
          inputmode="decimal"
          fluid
          :disabled="props.readOnly"
          @focus="scoreFocused = true"
          @blur="scoreFocused = false"
        />
      </div>
      <div class="field-group">
        <label for="score-note">Ghi chú</label>
        <InputText id="score-note" v-model="note" maxlength="500" fluid :disabled="props.readOnly" />
      </div>
      <div class="form-actions score-entry-actions">
        <Button
          type="button"
          label="Yêu cầu sửa điểm"
          icon="pi pi-file-edit"
          severity="secondary"
          outlined
          :disabled="props.saving"
          aria-label="Yêu cầu sửa điểm cho ô điểm hiện tại"
          @click="requestChange"
        />
        <span class="score-entry-actions-spacer" aria-hidden="true" />
        <Button type="button" label="Hủy" severity="secondary" outlined :disabled="props.saving" @click="emit('cancel')" />
        <Button type="button" label="Lưu điểm" icon="pi pi-check" :loading="props.saving" :disabled="props.saving || props.readOnly" @click="save" />
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
.score-entry-actions {
  align-items: center;
  flex-wrap: nowrap;
}

.score-entry-actions-spacer {
  flex: 1 1 auto;
  min-width: 8px;
}

.score-entry-actions .p-button {
  flex: 0 0 auto;
  white-space: nowrap;
}

@media (max-width: 420px) {
  .score-entry-actions {
    gap: 8px;
  }

  .score-entry-actions .p-button {
    padding-inline: 10px;
  }
}
</style>
