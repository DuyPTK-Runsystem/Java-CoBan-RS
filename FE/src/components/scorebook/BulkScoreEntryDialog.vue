<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'

import { getAuthSession } from '@/services/authSession'
import { downloadBulkScoreTemplate, previewBulkStudentScores } from '@/services/scorebookApi'
import { extractApiErrorMessage } from '@/types/api'
import type {
  BulkScoreImportPreview,
  BulkScoreItem,
  BulkUpsertStudentScoreRequest,
  ScoreGridColumn,
  ScoreStatus,
  StudentScoreGridRow,
} from '@/types/scorebook'

const assessmentTypeLabels: Record<string, string> = { KTTT: 'Thường xuyên', 'KTĐK': 'Giữa kỳ', KTCK: 'Cuối kỳ' }
const scoreStatusLabels: Record<ScoreStatus, string> = {
  SCORED: 'Có điểm',
  ABSENT: 'Vắng',
  EXEMPTED: 'Miễn',
  CANCELLED: 'Hủy',
}
const maxImportFileSize = 10 * 1024 * 1024

type EntryMode = 'manual' | 'file'

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
  initialMode?: EntryMode
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
const mode = ref<EntryMode>('manual')
const templateDownloaded = ref(false)
const templateDownloading = ref(false)
const selectedFile = ref<File | null>(null)
const previewLoading = ref(false)
const preview = ref<BulkScoreImportPreview | null>(null)
const previewError = ref('')
const showOnlyErrors = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const statuses = [
  { label: 'Có điểm', value: 'SCORED' as const },
  { label: 'Vắng', value: 'ABSENT' as const },
  { label: 'Miễn', value: 'EXEMPTED' as const },
  { label: 'Hủy', value: 'CANCELLED' as const },
]

const visiblePreviewRows = computed(() => {
  const previewRows = preview.value?.rows ?? []
  return showOnlyErrors.value ? previewRows.filter((row) => row.result === 'ERROR') : previewRows
})
const previewHasErrors = computed(() => (preview.value?.summary.errorRows ?? 0) > 0
  || (preview.value?.rows ?? []).some((row) => row.result === 'ERROR'))
const normalizedPreviewItems = computed(() => preview.value?.normalizedItems ?? preview.value?.items ?? [])
const fileSaveDisabled = computed(() => !preview.value
  || previewLoading.value
  || previewHasErrors.value
  || normalizedPreviewItems.value.length === 0
  || Boolean(props.saving))

function signature(row: EditableScoreRow): string {
  return JSON.stringify([row.scoreStatus, row.scoreValue, row.note.trim()])
}

function resetFileImport(): void {
  templateDownloaded.value = false
  templateDownloading.value = false
  selectedFile.value = null
  previewLoading.value = false
  preview.value = null
  previewError.value = ''
  showOnlyErrors.value = false
  if (fileInput.value) fileInput.value.value = ''
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
  mode.value = props.initialMode ?? 'manual'
  resetFileImport()
}

watch(() => props.visible, (visible) => {
  if (visible) initialize()
}, { immediate: true })

function selectMode(nextMode: EntryMode): void {
  mode.value = nextMode
  validationMessage.value = ''
  previewError.value = ''
  if (nextMode === 'manual') resetFileImport()
}

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

function saveManual(): void {
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

function templateFileName(): string {
  const name = props.column?.columnName || (props.column ? assessmentTypeLabels[props.column.assessmentType] ?? props.column.assessmentType : 'diem')
  return `${name.replace(/[^\p{L}\p{N}-]+/gu, '-').replace(/^-|-$/g, '') || 'diem'}-mau.xlsx`
}

async function downloadTemplate(): Promise<void> {
  if (!props.column) return
  const token = getAuthSession()?.accessToken
  if (!token) {
    previewError.value = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
    return
  }
  templateDownloading.value = true
  previewError.value = ''
  try {
    const blob = await downloadBulkScoreTemplate(token, props.column.columnId)
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = templateFileName()
    document.body.appendChild(anchor)
    anchor.click()
    anchor.remove()
    URL.revokeObjectURL(url)
    templateDownloaded.value = true
  } catch (error) {
    previewError.value = extractApiErrorMessage(error, 'Không thể tải file mẫu điểm.')
  } finally {
    templateDownloading.value = false
  }
}

function chooseFile(): void {
  fileInput.value?.click()
}

async function handleFileSelected(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0] ?? null
  preview.value = null
  previewError.value = ''
  validationMessage.value = ''
  if (!file) {
    selectedFile.value = null
    return
  }
  if (!file.name.toLowerCase().endsWith('.xlsx')) {
    selectedFile.value = null
    input.value = ''
    previewError.value = 'Chỉ hỗ trợ file .xlsx.'
    return
  }
  if (file.size > maxImportFileSize) {
    selectedFile.value = null
    input.value = ''
    previewError.value = 'File nhập điểm không được vượt quá 10 MB.'
    return
  }
  selectedFile.value = file
  await previewFile()
}

async function previewFile(): Promise<void> {
  if (!selectedFile.value || !props.column) {
    previewError.value = 'Hãy chọn file .xlsx để xem trước.'
    return
  }
  const token = getAuthSession()?.accessToken
  if (!token) {
    previewError.value = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
    return
  }
  previewLoading.value = true
  previewError.value = ''
  validationMessage.value = ''
  try {
    preview.value = await previewBulkStudentScores(token, props.column.columnId, selectedFile.value)
  } catch (error) {
    preview.value = null
    previewError.value = extractApiErrorMessage(error, 'Không thể xem trước file điểm.')
  } finally {
    previewLoading.value = false
  }
}

function formatStatus(status: ScoreStatus | null): string {
  return status ? scoreStatusLabels[status] : '—'
}

function formatScore(status: ScoreStatus | null, value: number | null): string {
  if (status === null) return '—'
  if (status !== 'SCORED') return formatStatus(status)
  return value === null ? 'Thiếu điểm' : String(value)
}

function saveFilePreview(): void {
  validationMessage.value = ''
  if (!preview.value) {
    validationMessage.value = 'Hãy xem trước file trước khi lưu.'
    return
  }
  if (previewHasErrors.value) {
    validationMessage.value = 'File còn dòng lỗi, chưa thể lưu điểm.'
    return
  }
  if (normalizedPreviewItems.value.length === 0) {
    validationMessage.value = 'Backend chưa trả về dòng điểm hợp lệ để lưu.'
    return
  }
  emit('save', { items: normalizedPreviewItems.value })
}

function save(): void {
  if (mode.value === 'file') saveFilePreview()
  else saveManual()
}
</script>

<template>
  <Dialog :visible="props.visible" header="Nhập điểm hàng loạt" modal :style="{ width: 'min(1180px, calc(100vw - 32px))' }" @update:visible="emit('update:visible', $event)">
    <div class="form-stack">
      <p class="dialog-caption">Cột: {{ props.column?.columnName || (props.column ? assessmentTypeLabels[props.column.assessmentType] ?? props.column.assessmentType : '—') }}</p>
      <div class="mode-switch" role="tablist" aria-label="Cách nhập điểm">
        <Button label="Nhập trực tiếp" :severity="mode === 'manual' ? 'primary' : 'secondary'" :outlined="mode !== 'manual'" role="tab" :aria-selected="mode === 'manual'" @click="selectMode('manual')" />
        <Button label="Dùng file mẫu" icon="pi pi-file-excel" :severity="mode === 'file' ? 'primary' : 'secondary'" :outlined="mode !== 'file'" role="tab" :aria-selected="mode === 'file'" @click="selectMode('file')" />
      </div>

      <div v-if="validationMessage || props.errorMessage || previewError" class="form-alert form-alert-error" role="alert">
        {{ validationMessage || props.errorMessage || previewError }}
      </div>

      <template v-if="mode === 'manual'">
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
      </template>

      <template v-else>
        <section class="file-import-panel" aria-label="Nhập điểm bằng file mẫu">
          <div class="file-import-step">
            <div class="step-index">1</div>
            <div class="step-content">
              <strong>Tải file mẫu có danh sách học sinh của lớp</strong>
              <Button label="Tải file mẫu" icon="pi pi-download" :loading="templateDownloading" :disabled="templateDownloading || !props.column" @click="downloadTemplate" />
              <span v-if="templateDownloaded" class="step-success"><i class="pi pi-check-circle" aria-hidden="true" /> Đã tải file mẫu. Bạn có thể chọn file để nhập.</span>
            </div>
          </div>

          <div class="file-import-step">
            <div class="step-index">2</div>
            <div class="step-content">
              <strong>Chọn file đã điền điểm</strong>
              <input ref="fileInput" class="visually-hidden" type="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" @change="handleFileSelected">
              <div class="file-actions">
                <Button label="Chọn file .xlsx" icon="pi pi-upload" outlined :disabled="previewLoading" @click="chooseFile" />
                <span v-if="selectedFile" class="selected-file">{{ selectedFile.name }} · {{ Math.ceil(selectedFile.size / 1024) }} KB</span>
              </div>
            </div>
          </div>
        </section>

        <section v-if="preview" class="preview-panel" aria-label="Xem trước điểm nhập từ file">
          <div class="preview-heading">
            <div>
              <strong>Xem trước dữ liệu</strong>
              <span>{{ preview.summary.validRows }} dòng hợp lệ · {{ preview.summary.errorRows }} dòng lỗi · {{ preview.summary.newScores }} điểm mới · {{ preview.summary.updatedScores }} điểm cập nhật</span>
            </div>
            <label class="error-filter"><input v-model="showOnlyErrors" type="checkbox"> Chỉ xem dòng lỗi</label>
          </div>
          <div class="preview-table-wrap">
            <table class="preview-table">
              <thead><tr><th>Dòng</th><th>Học sinh</th><th>Hiện tại</th><th>Giá trị mới</th><th>Ghi chú</th><th>Kết quả</th></tr></thead>
              <tbody>
                <tr v-for="row in visiblePreviewRows" :key="row.rowNumber" :class="{ 'preview-row-error': row.result === 'ERROR' }">
                  <td>{{ row.rowNumber }}</td>
                  <td><strong>{{ row.studentCode || '—' }}</strong><span>{{ row.studentName || 'Không xác định' }}</span></td>
                  <td>{{ formatScore(row.oldStatus, row.oldValue) }}<small v-if="row.oldStatus"> · {{ formatStatus(row.oldStatus) }}</small></td>
                  <td>{{ formatScore(row.newStatus, row.newValue) }}<small v-if="row.newStatus"> · {{ formatStatus(row.newStatus) }}</small></td>
                  <td>{{ row.note || '—' }}</td>
                  <td><span :class="row.result === 'ERROR' ? 'row-error' : 'row-valid'">{{ row.result === 'ERROR' ? (row.message || 'Dữ liệu lỗi') : 'Hợp lệ' }}</span></td>
                </tr>
                <tr v-if="visiblePreviewRows.length === 0"><td colspan="6" class="preview-empty">Không có dòng phù hợp.</td></tr>
              </tbody>
            </table>
          </div>
        </section>
      </template>

      <div class="form-actions">
        <Button label="Hủy" text :disabled="props.saving || templateDownloading || previewLoading" @click="emit('cancel')" />
        <Button label="Lưu" icon="pi pi-check" :loading="props.saving" :disabled="props.saving || (mode === 'file' ? fileSaveDisabled : false)" @click="save" />
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
.mode-switch { display: flex; flex-wrap: wrap; gap: 8px; }
.bulk-score-list { display: grid; gap: 14px; max-height: 56vh; overflow-y: auto; padding: 4px 0; }
.bulk-score-row { display: grid; grid-template-columns: minmax(260px, 1.35fr) minmax(180px, .9fr) minmax(150px, .75fr) minmax(200px, 1fr); align-items: center; gap: 14px; }
.bulk-score-row > * { min-width: 0; }
.bulk-score-row :deep(.p-select), .bulk-score-row :deep(.p-inputnumber), .bulk-score-row :deep(.p-inputtext), .bulk-score-row :deep(.p-inputnumber-input) { width: 100%; }
.bulk-score-student { color: #334155; }
.file-import-panel { display: grid; gap: 12px; }
.file-import-step { display: flex; gap: 14px; padding: 16px; border: 1px solid #dbe4ee; border-radius: 12px; background: #f8fafc; }
.step-muted { opacity: .72; }
.step-index { display: grid; place-items: center; flex: 0 0 28px; height: 28px; border-radius: 50%; color: #fff; background: #2563eb; font-weight: 700; }
.step-content { display: grid; gap: 8px; min-width: 0; }
.step-content > span { color: #64748b; font-size: .9rem; }
.step-success { color: #15803d !important; }
.file-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.selected-file { color: #334155; font-size: .9rem; word-break: break-word; }
.preview-panel { display: grid; gap: 12px; padding-top: 4px; }
.preview-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.preview-heading > div { display: grid; gap: 4px; }
.preview-heading span { color: #64748b; font-size: .9rem; }
.error-filter { display: inline-flex; align-items: center; gap: 6px; color: #475569; white-space: nowrap; font-size: .9rem; }
.preview-table-wrap { overflow-x: auto; border: 1px solid #dbe4ee; border-radius: 10px; }
.preview-table { width: 100%; border-collapse: collapse; min-width: 760px; font-size: .9rem; }
.preview-table th, .preview-table td { padding: 10px 12px; border-bottom: 1px solid #e2e8f0; text-align: left; vertical-align: top; }
.preview-table th { color: #475569; background: #f8fafc; font-weight: 700; }
.preview-table td:nth-child(2) { display: grid; gap: 3px; }
.preview-table td small { color: #64748b; }
.preview-row-error { background: #fff7f7; }
.row-error { color: #b91c1c; }
.row-valid { color: #15803d; }
.preview-empty { color: #64748b; text-align: center !important; }
.visually-hidden { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
@media (max-width: 720px) {
  .bulk-score-row { grid-template-columns: 1fr; padding-bottom: 12px; border-bottom: 1px solid #e2e8f0; }
  .preview-heading { flex-direction: column; }
}
</style>
