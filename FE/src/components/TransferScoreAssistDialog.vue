<script setup lang="ts">
import { ref, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'

import FormAlert from '@/components/FormAlert.vue'
import type { TransferScoreAssistSnapshot, TransferScoreEntryRequest, TransferScoreTargetColumn, TransferWithScoresRequest } from '@/types/enrollment'
import type { ScoreStatus } from '@/types/scorebook'

interface DraftTarget extends TransferScoreTargetColumn {
  scoreStatus: ScoreStatus | null
  scoreValue: number | null
  note: string
  version: number | null
}

const props = withDefaults(defineProps<{
  visible?: boolean
  snapshot?: TransferScoreAssistSnapshot | null
  loading?: boolean
  saving?: boolean
  errorMessage?: string
  effectiveAt: string
  reason: string
}>(), { visible: false, snapshot: null, loading: false, saving: false, errorMessage: '' })

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  confirm: [request: TransferWithScoresRequest]
  cancel: []
  retry: []
}>()

const drafts = ref<Record<number, DraftTarget>>({})
const validationMessage = ref('')
const statuses = [
  { label: 'Chưa nhập', value: null },
  { label: 'Có điểm', value: 'SCORED' },
  { label: 'Vắng', value: 'ABSENT' },
  { label: 'Miễn', value: 'EXEMPTED' },
  { label: 'Hủy', value: 'CANCELLED' },
]

function initialize(): void {
  drafts.value = Object.fromEntries((props.snapshot?.subjects ?? []).flatMap((subject) => subject.targetColumns.map((column) => [column.assessmentColumnId, {
    ...column,
    scoreStatus: column.existingScoreStatus,
    scoreValue: column.existingScoreValue,
    note: column.existingNote ?? '',
    version: column.existingVersion,
  }])))
  validationMessage.value = ''
}

watch(() => [props.visible, props.snapshot], ([visible, snapshot], previous) => {
  if (visible && snapshot && !previous?.[1]) initialize()
}, { immediate: true })

function evidenceFor(subjectId: number, sourceColumnId: number | null) {
  if (sourceColumnId === null) return null
  return props.snapshot?.subjects.find((subject) => subject.subjectId === subjectId)?.sourceEvidence.find((evidence) => evidence.assessmentColumnId === sourceColumnId) ?? null
}

function suggestionFor(subjectId: number, target: DraftTarget) {
  const evidence = evidenceFor(subjectId, target.suggestedSourceColumnId)
  if (!evidence || !evidence.scoreStatus) return
  target.scoreStatus = evidence.scoreStatus
  target.scoreValue = evidence.scoreStatus === 'SCORED' ? evidence.scoreValue : null
  target.note = evidence.note ?? ''
}

function formatScore(value: number | null, status: ScoreStatus | null): string {
  if (status === null) return 'Chưa nhập'
  if (status !== 'SCORED') return statuses.find((item) => item.value === status)?.label ?? status
  return value === null ? 'Thiếu giá trị' : String(value)
}

function roundScore(value: number | null): number | null {
  if (value === null || Number.isNaN(value)) return null
  return Math.round(value * 10) / 10
}

function validateScore(target: DraftTarget): string {
  if (target.note.length > 500) return `Ghi chú của cột ${target.columnName || target.assessmentType} vượt quá 500 ký tự.`
  if (target.scoreStatus === 'SCORED' && (target.scoreValue === null || target.scoreValue < 0 || target.scoreValue > 10 || Math.abs(target.scoreValue * 10 - Math.round(target.scoreValue * 10)) > Number.EPSILON * 100)) {
    return `Điểm của cột ${target.columnName || target.assessmentType} phải từ 0 đến 10 và có tối đa một chữ số thập phân.`
  }
  return ''
}

function scoreRequest(target: DraftTarget): TransferScoreEntryRequest {
  return {
    assessmentColumnId: target.assessmentColumnId,
    scoreStatus: target.scoreStatus ?? 'CANCELLED',
    scoreValue: target.scoreStatus === 'SCORED' ? roundScore(target.scoreValue) : null,
    note: target.note.trim() || null,
    expectedVersion: target.version,
  }
}

function confirm(): void {
  validationMessage.value = ''
  for (const target of Object.values(drafts.value)) {
    if (target.scoreStatus === 'SCORED' && target.scoreValue !== null) target.scoreValue = roundScore(target.scoreValue)
    const error = validateScore(target)
    if (error) {
      validationMessage.value = error
      return
    }
  }
  if (!props.snapshot) {
    return
  }
  emit('confirm', {
    targetClassId: props.snapshot.targetClass.id,
    semesterId: props.snapshot.semesterId,
    effectiveAt: props.effectiveAt,
    reason: props.reason.trim() || null,
    scores: Object.values(drafts.value).filter((target) => target.scoreStatus !== null).map(scoreRequest),
  })
}

function close(): void {
  if (props.saving) return
  emit('update:visible', false)
  emit('cancel')
}
</script>

<template>
  <Dialog :visible="props.visible" modal header="Hỗ trợ chuyển điểm giữa học kỳ" :style="{ width: 'min(1180px, calc(100vw - 2rem))' }" :closable="!props.saving" @update:visible="emit('update:visible', $event)">
    <div class="form-stack">
      <p class="dialog-caption">Đối chiếu điểm đã có ở lớp nguồn rồi nhập điểm tương ứng cho lớp đích. Điểm nguồn chỉ để tham khảo và không bị thay đổi.</p>
      <FormAlert v-if="props.errorMessage" tone="error" :message="props.errorMessage" />
      <FormAlert v-if="validationMessage" tone="error" :message="validationMessage" />
      <div v-if="props.loading" class="transfer-score-loading" role="status"><i class="pi pi-spin pi-spinner" aria-hidden="true" /> Đang tải snapshot điểm...</div>
      <div v-else-if="!props.snapshot" class="transfer-score-empty">
        <strong>Chưa có dữ liệu hỗ trợ điểm</strong>
        <p>Không thể tải các cột điểm của lớp nguồn và lớp đích.</p>
        <Button label="Thử lại" icon="pi pi-refresh" severity="secondary" @click="emit('retry')" />
      </div>
      <template v-else>
        <div class="transfer-score-summary">
          <span><small>Học sinh</small><strong>{{ props.snapshot.studentCode }} · {{ props.snapshot.studentName }}</strong></span>
          <span><small>Học kỳ</small><strong>HK #{{ props.snapshot.semesterId }}</strong></span>
          <span><small>Lớp nguồn → đích</small><strong>{{ props.snapshot.sourceClass.classCode }} → {{ props.snapshot.targetClass.classCode }}</strong></span>
        </div>
        <FormAlert v-if="props.snapshot.warnings.length" tone="warning" :messages="props.snapshot.warnings" />
        <div v-if="!props.snapshot.hasExistingScores" class="transfer-score-empty">
          <strong>Học sinh chưa có điểm trong học kỳ này</strong>
          <p>Không có bằng chứng điểm để đối chiếu. Bạn vẫn có thể xác nhận chuyển lớp.</p>
        </div>
        <div v-else class="transfer-score-table-wrap">
          <table class="transfer-score-table">
            <thead><tr><th>Môn</th><th>Điểm lớp nguồn (read-only)</th><th>Cột điểm lớp mới</th><th>Nhập điểm lớp mới</th><th>Gợi ý</th></tr></thead>
            <tbody>
              <template v-for="subject in props.snapshot.subjects" :key="subject.subjectId">
                <tr v-for="target in subject.targetColumns" :key="target.assessmentColumnId">
                  <td><strong>{{ subject.subjectCode }}</strong><br><span>{{ subject.subjectName }}</span></td>
                  <td><div class="source-evidence-list"><span v-for="evidence in subject.sourceEvidence" :key="evidence.assessmentColumnId" class="source-evidence"><b>{{ evidence.columnName || `${evidence.assessmentType} #${evidence.columnNo}` }}</b>: {{ formatScore(evidence.scoreValue, evidence.scoreStatus) }}</span><span v-if="!subject.sourceEvidence.length">Chưa có bằng chứng</span></div></td>
                  <td><strong>{{ target.columnName || `${target.assessmentType} #${target.columnNo}` }}</strong><br><span class="field-hint">{{ target.mappingKey || 'Chưa ghép' }}</span></td>
                  <td>
                    <div class="target-editor">
                      <Select v-model="drafts[target.assessmentColumnId].scoreStatus" :options="statuses" option-label="label" option-value="value" aria-label="Trạng thái điểm lớp mới" :disabled="props.saving" />
                      <InputNumber v-if="drafts[target.assessmentColumnId].scoreStatus === 'SCORED'" v-model="drafts[target.assessmentColumnId].scoreValue" :min="0" :max="10" :max-fraction-digits="1" :use-grouping="false" inputmode="decimal" placeholder="Điểm" :disabled="props.saving" />
                      <InputText v-model="drafts[target.assessmentColumnId].note" maxlength="500" placeholder="Ghi chú" :disabled="props.saving" />
                    </div>
                  </td>
                  <td><Button v-if="target.suggestedSourceColumnId !== null" label="Gợi ý" icon="pi pi-copy" severity="secondary" text :disabled="props.saving" @click="suggestionFor(subject.subjectId, drafts[target.assessmentColumnId])" /><span v-else class="field-hint">Chưa ghép</span></td>
                </tr>
                <tr v-if="!subject.targetColumns.length"><td>{{ subject.subjectCode }} · {{ subject.subjectName }}</td><td colspan="4" class="field-hint">Lớp mới chưa có cột điểm tương ứng.</td></tr>
              </template>
            </tbody>
          </table>
        </div>
        <p class="field-hint">Gợi ý chỉ điền vào bản nháp; bạn vẫn phải kiểm tra và bấm xác nhận. Ô trống khác với điểm 0.0.</p>
      </template>
      <div class="form-actions"><Button type="button" label="Hủy" severity="secondary" outlined :disabled="props.saving" @click="close" /><Button type="button" label="Xác nhận chuyển lớp và lưu điểm mới" icon="pi pi-check" :loading="props.saving" :disabled="props.loading || !props.snapshot || props.saving" @click="confirm" /></div>
    </div>
  </Dialog>
</template>

<style scoped>
.transfer-score-summary { display: grid; grid-template-columns: 1.4fr 1fr 1fr; gap: 12px; padding: 14px; border: 1px solid #dbeafe; border-radius: 10px; background: #eff6ff; }
.transfer-score-summary span { display: grid; gap: 4px; min-width: 0; }
.transfer-score-summary small { color: #64748b; }
.transfer-score-table-wrap { max-height: 52vh; overflow: auto; border: 1px solid #e2e8f0; border-radius: 10px; }
.transfer-score-table { width: 100%; min-width: 1040px; border-collapse: collapse; }
.transfer-score-table th, .transfer-score-table td { padding: 12px; border-bottom: 1px solid #e2e8f0; text-align: left; vertical-align: top; }
.transfer-score-table th { position: sticky; top: 0; z-index: 1; background: #f8fafc; color: #475569; font-size: .82rem; }
.source-evidence-list, .target-editor { display: grid; gap: 8px; }
.source-evidence { display: block; color: #334155; }
.target-editor :deep(.p-select), .target-editor :deep(.p-inputnumber), .target-editor :deep(.p-inputtext) { width: 100%; }
.transfer-score-empty, .transfer-score-loading { display: grid; gap: 8px; padding: 28px; place-items: center; text-align: center; border: 1px dashed #cbd5e1; border-radius: 10px; color: #475569; }
@media (max-width: 760px) { .transfer-score-summary { grid-template-columns: 1fr; } }
</style>
