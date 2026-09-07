<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'

import FormAlert from '@/components/FormAlert.vue'
import type {
  ReqCreateRetakeExamDTO,
  ReqUpdateRetakeScoreDTO,
  RetakeRowItem,
} from '@/types/retake'

export interface StudentOption {
  id: number
  code?: string
  name: string
}

export interface AcademicYearOption {
  id: number
  code: string
}

export interface SubjectOption {
  id: number
  name: string
}

const props = withDefaults(
  defineProps<{
    visible?: boolean
    mode?: 'create' | 'score' | 'cancel'
    item?: RetakeRowItem | null
    students?: StudentOption[]
    academicYears?: AcademicYearOption[]
    subjects?: SubjectOption[]
    saving?: boolean
    errorMessage?: string | string[]
  }>(),
  {
    visible: false,
    mode: 'create',
    item: null,
    students: () => [],
    academicYears: () => [],
    subjects: () => [],
    saving: false,
    errorMessage: '',
  },
)

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  submitCreate: [data: ReqCreateRetakeExamDTO]
  submitScore: [retakeId: number, data: ReqUpdateRetakeScoreDTO]
  submitCancel: [retakeId: number]
  cancel: []
}>()

// Form fields
const studentId = ref<number | null>(null)
const academicYearId = ref<number | null>(null)
const subjectId = ref<number | null>(null)
const examDate = ref('')
const retakeScore = ref<number | null>(null)
const note = ref('')
const validationError = ref('')

const errorList = computed(() => {
  if (Array.isArray(props.errorMessage)) {
    return props.errorMessage
      .filter((m) => typeof m === 'string' && m.trim().length > 0)
      .map((m) => m.trim())
  }
  if (typeof props.errorMessage === 'string' && props.errorMessage.includes('\n')) {
    return props.errorMessage
      .split('\n')
      .map((m) => m.trim())
      .filter((m) => m.length > 0)
  }
  return []
})

const normalizedErrorMessage = computed(() => {
  if (errorList.value.length > 0) {
    return errorList.value
      .map((m, index) => {
        if (index === errorList.value.length - 1) return m
        return /[.!?;:]$/.test(m) ? m : `${m}.`
      })
      .join(' ')
  }
  return typeof props.errorMessage === 'string' ? props.errorMessage.trim() : ''
})

watch(
  () => [props.visible, props.item, props.mode],
  () => {
    if (!props.visible) return
    validationError.value = ''
    if (props.mode === 'create') {
      studentId.value = props.students[0]?.id ?? null
      academicYearId.value = props.academicYears[0]?.id ?? null
      subjectId.value = props.subjects[0]?.id ?? null
      examDate.value = ''
      retakeScore.value = null
      note.value = ''
    } else if (props.item) {
      studentId.value = props.item.studentId
      academicYearId.value = props.item.academicYearId
      subjectId.value = props.item.subjectId
      examDate.value = props.item.examDate ? props.item.examDate.slice(0, 10) : ''
      retakeScore.value = props.item.retakeScore ?? null
      note.value = props.item.note ?? ''
    }
  },
  { immediate: true },
)

const isCancelled = computed(() => props.item?.status === 'CANCELLED')

const dialogTitle = computed(() => {
  if (props.mode === 'create') return 'Tạo kỳ thi lại'
  if (props.mode === 'score') return 'Nhập/sửa điểm thi lại'
  return 'Hủy kỳ thi lại?'
})

const dialogCaption = computed(() => {
  if (props.mode === 'create') {
    return 'Tạo record PLANNED cho một học sinh/môn/năm học.'
  }
  if (props.mode === 'score' && props.item) {
    const student = props.item.studentName || `Học sinh #${props.item.studentId}`
    const subject = props.item.subjectName || `Môn #${props.item.subjectId}`
    return `${student} · ${subject} · retakeId: ${props.item.retakeId}`
  }
  return 'Record sẽ chuyển sang CANCELLED và audit history được giữ lại.'
})

function hasAtMostOneDecimal(input: number): boolean {
  const str = input.toString()
  const dotIndex = str.indexOf('.')
  if (dotIndex === -1) return true
  return str.length - dotIndex - 1 <= 1
}

function closeDialog(): void {
  emit('update:visible', false)
  emit('cancel')
}

function handleSave(): void {
  validationError.value = ''

  if (props.mode === 'create') {
    if (!studentId.value || !academicYearId.value || !subjectId.value) {
      validationError.value = 'Vui lòng chọn học sinh, năm học và môn học.'
      return
    }
    if (examDate.value.trim() && !/^\d{4}-\d{2}-\d{2}$/.test(examDate.value.trim())) {
      validationError.value = 'Ngày thi không đúng định dạng yyyy-MM-dd.'
      return
    }
    if (retakeScore.value !== null && retakeScore.value !== undefined) {
      if (retakeScore.value < 0 || retakeScore.value > 10) {
        validationError.value = 'Điểm thi lại phải từ 0.0 đến 10.0.'
        return
      }
      if (!hasAtMostOneDecimal(retakeScore.value)) {
        validationError.value = 'Điểm thi lại chỉ được có tối đa 1 chữ số thập phân.'
        return
      }
    }
    if (note.value && note.value.length > 1000) {
      validationError.value = 'Ghi chú không quá 1000 ký tự.'
      return
    }

    emit('submitCreate', {
      studentId: studentId.value,
      academicYearId: academicYearId.value,
      subjectId: subjectId.value,
      examDate: examDate.value.trim() || undefined,
      retakeScore: (retakeScore.value !== null && retakeScore.value !== undefined) ? retakeScore.value : undefined,
      note: note.value.trim() || undefined,
    })
  } else if (props.mode === 'score') {
    if (!props.item) return
    if (isCancelled.value) {
      validationError.value = 'Bản ghi đã bị hủy, không thể cập nhật điểm.'
      return
    }
    if (retakeScore.value === null || retakeScore.value === undefined) {
      validationError.value = 'Điểm thi lại không được để trống.'
      return
    }
    if (retakeScore.value < 0 || retakeScore.value > 10) {
      validationError.value = 'Điểm thi lại phải từ 0.0 đến 10.0.'
      return
    }
    if (!hasAtMostOneDecimal(retakeScore.value)) {
      validationError.value = 'Điểm thi lại chỉ được có tối đa 1 chữ số thập phân.'
      return
    }
    if (examDate.value.trim() && !/^\d{4}-\d{2}-\d{2}$/.test(examDate.value.trim())) {
      validationError.value = 'Ngày thi không đúng định dạng yyyy-MM-dd.'
      return
    }
    if (note.value && note.value.length > 1000) {
      validationError.value = 'Ghi chú không quá 1000 ký tự.'
      return
    }

    emit('submitScore', props.item.retakeId, {
      retakeScore: retakeScore.value,
      examDate: examDate.value.trim() || undefined,
      note: note.value.trim() || undefined,
    })
  } else if (props.mode === 'cancel') {
    if (!props.item) return
    emit('submitCancel', props.item.retakeId)
  }
}
</script>

<template>
  <Dialog
    :visible="props.visible"
    modal
    :header="dialogTitle"
    :style="{ width: 'min(640px, 94vw)' }"
    data-testid="retake-dialog"
    @update:visible="emit('update:visible', $event)"
  >
    <p class="dialog-caption">{{ dialogCaption }}</p>

    <FormAlert
      v-if="validationError"
      tone="error"
      :message="validationError"
      data-testid="dialog-validation-error"
    />
    <FormAlert
      v-if="normalizedErrorMessage"
      tone="error"
      :message="errorList.length > 1 ? undefined : normalizedErrorMessage"
      :messages="errorList.length > 1 ? errorList : []"
      data-testid="dialog-api-error"
    />

    <!-- MODE: CANCEL -->
    <template v-if="props.mode === 'cancel'">
      <div class="notice warn">
        <strong>Ảnh hưởng:</strong>
        <span>
          Nếu record đang SCORED, backend có thể tạo task để khôi phục official result về regular score. Không xóa dữ liệu lịch sử.
        </span>
      </div>
    </template>

    <!-- MODE: SCORE -->
    <template v-else-if="props.mode === 'score'">
      <div class="compare">
        <div class="compare-card">
          <div class="muted">Trước thi lại · preRetakeScore</div>
          <div class="score before" style="font-size: 24px">
            {{ props.item?.preRetakeScore !== null && props.item?.preRetakeScore !== undefined ? props.item.preRetakeScore.toFixed(1) : '—' }}
          </div>
        </div>
        <div class="compare-card">
          <div class="muted">Sau thi lại · retakeScore</div>
          <div class="score after" style="font-size: 24px">
            {{ props.item?.retakeScore !== null && props.item?.retakeScore !== undefined ? props.item.retakeScore.toFixed(1) : '—' }}
          </div>
        </div>
      </div>

      <div v-if="isCancelled" class="notice warn" data-testid="notice-cancelled-readonly">
        <strong>Chỉ đọc:</strong>
        <span>Kỳ thi lại này đã bị hủy (CANCELLED). Điểm và thông tin không thể sửa đổi.</span>
      </div>

      <div class="form-grid">
        <div class="field">
          <label for="score-input">Điểm thi lại *</label>
          <InputNumber
            id="score-input"
            v-model="retakeScore"
            :min="0"
            :max="10"
            :min-fraction-digits="0"
            :max-fraction-digits="1"
            :step="0.1"
            :disabled="isCancelled"
            fluid
            data-testid="input-retake-score"
          />
        </div>
        <div class="field">
          <label for="score-date">Ngày thi</label>
          <InputText
            id="score-date"
            v-model="examDate"
            type="date"
            :disabled="isCancelled"
            fluid
            data-testid="input-exam-date"
          />
        </div>
        <div class="field wide">
          <label for="score-note">Ghi chú</label>
          <Textarea
            id="score-note"
            v-model="note"
            rows="3"
            maxlength="1000"
            placeholder="Tối đa 1000 ký tự"
            :disabled="isCancelled"
            fluid
            data-testid="input-note"
          />
        </div>
      </div>

      <div
        v-if="props.item?.officialDtbmhCn !== null && props.item?.officialDtbmhCn !== undefined"
        class="notice success"
        data-testid="notice-official"
      >
        <strong>Official:</strong>
        <span>
          {{ props.item.officialDtbmhCn.toFixed(1) }}
          <template v-if="props.item.calculationSource"> · calculationSource: {{ props.item.calculationSource }}</template>
          <template v-if="props.item.calculationStatus"> · status {{ props.item.calculationStatus }}</template>
          (dữ liệu đọc từ Transcript API).
        </span>
      </div>
      <div v-else-if="props.item?.calculationStatus === 'IN_PROGRESS'" class="notice warn">
        <strong>Đang xử lý:</strong>
        <span>Backend đang tính lại transcript. Kết quả cũ chưa được cập nhật chính thức.</span>
      </div>

      <div class="notice warn">
        <strong>Rule:</strong>
        <span>Điểm hợp lệ từ 0.0 đến 10.0, tối đa 1 chữ số thập phân. Lưu điểm sẽ tạo calculation task.</span>
      </div>
    </template>

    <!-- MODE: CREATE -->
    <template v-else>
      <div class="form-grid">
        <div class="field">
          <label for="create-student">Học sinh *</label>
          <Select
            v-if="props.students.length > 0"
            id="create-student"
            v-model="studentId"
            :options="props.students"
            option-value="id"
            :option-label="(option) => option.code ? `${option.code} · ${option.name}` : option.name"
            placeholder="Chọn học sinh"
            fluid
            data-testid="select-student"
          />
          <InputNumber
            v-else
            id="create-student"
            v-model="studentId"
            :min="1"
            placeholder="Nhập ID học sinh"
            fluid
            data-testid="select-student"
          />
        </div>
        <div class="field">
          <label for="create-year">Năm học *</label>
          <Select
            v-if="props.academicYears.length > 0"
            id="create-year"
            v-model="academicYearId"
            :options="props.academicYears"
            option-value="id"
            option-label="code"
            placeholder="Chọn năm học"
            fluid
            data-testid="select-year"
          />
          <InputNumber
            v-else
            id="create-year"
            v-model="academicYearId"
            :min="1"
            placeholder="Nhập ID năm học"
            fluid
            data-testid="select-year"
          />
        </div>
        <div class="field">
          <label for="create-subject">Môn học *</label>
          <Select
            v-if="props.subjects.length > 0"
            id="create-subject"
            v-model="subjectId"
            :options="props.subjects"
            option-value="id"
            option-label="name"
            placeholder="Chọn môn học"
            fluid
            data-testid="select-subject"
          />
          <InputNumber
            v-else
            id="create-subject"
            v-model="subjectId"
            :min="1"
            placeholder="Nhập ID môn học"
            fluid
            data-testid="select-subject"
          />
        </div>
        <div class="field">
          <label for="create-date">Ngày thi</label>
          <InputText
            id="create-date"
            v-model="examDate"
            type="date"
            fluid
            data-testid="input-create-date"
          />
        </div>
        <div class="field wide">
          <label for="create-score">Điểm thi lại (optional)</label>
          <InputNumber
            id="create-score"
            v-model="retakeScore"
            :min="0"
            :max="10"
            :min-fraction-digits="0"
            :max-fraction-digits="1"
            :step="0.1"
            placeholder="Để trống = PLANNED"
            fluid
            data-testid="input-create-score"
          />
        </div>
        <div class="field wide">
          <label for="create-note">Ghi chú</label>
          <Textarea
            id="create-note"
            v-model="note"
            rows="3"
            maxlength="1000"
            placeholder="Tối đa 1000 ký tự"
            fluid
            data-testid="input-create-note"
          />
        </div>
      </div>

      <div class="notice info">
        <strong>Snapshot:</strong>
        <span>preRetakeScore được backend lấy từ regular_dtbmh_cn; FE không tự nhập hoặc tính snapshot.</span>
      </div>
    </template>

    <template #footer>
      <div class="dialog-footer">
        <Button
          label="Hủy"
          severity="secondary"
          :disabled="props.saving"
          data-testid="btn-dialog-cancel"
          @click="closeDialog"
        />
        <Button
          v-if="props.mode === 'cancel'"
          label="Xác nhận hủy"
          severity="danger"
          :loading="props.saving"
          data-testid="btn-dialog-confirm-cancel"
          @click="handleSave"
        />
        <Button
          v-else-if="props.mode === 'score'"
          label="Lưu điểm"
          :disabled="isCancelled"
          :loading="props.saving"
          data-testid="btn-dialog-save-score"
          @click="handleSave"
        />
        <Button
          v-else
          :label="(retakeScore !== null && retakeScore !== undefined) ? 'Tạo kỳ thi lại' : 'Tạo PLANNED'"
          :loading="props.saving"
          data-testid="btn-dialog-save-create"
          @click="handleSave"
        />
      </div>
    </template>
  </Dialog>
</template>

<style scoped>
.dialog-caption {
  margin: 0 0 16px;
  color: #64748b;
  font-size: 13px;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin: 14px 0;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.field label {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}
.wide {
  grid-column: 1 / -1;
}
.compare {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin: 14px 0;
}
.compare-card {
  padding: 12px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}
.muted {
  color: #64748b;
  font-size: 12px;
}
.score {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  margin-top: 4px;
}
.score.before {
  color: #64748b;
}
.score.after {
  color: #047857;
}
.notice {
  border-radius: 8px;
  padding: 10px 12px;
  display: flex;
  gap: 8px;
  font-size: 13px;
  margin: 12px 0;
  align-items: flex-start;
}
.notice.info {
  background: #eff6ff;
  color: #1e40af;
  border: 1px solid #bfdbfe;
}
.notice.warn {
  background: #fffbeb;
  color: #92400e;
  border: 1px solid #fde68a;
}
.notice.success {
  background: #f0fdf4;
  color: #166534;
  border: 1px solid #bbf7d0;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 14px;
}
@media (max-width: 560px) {
  .form-grid,
  .compare {
    grid-template-columns: 1fr;
  }
}
</style>
