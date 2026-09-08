<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import DatePicker from 'primevue/datepicker'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
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

const examDateModel = computed<Date | null>({
  get: () => examDate.value ? new Date(`${examDate.value}T00:00:00`) : null,
  set: (value) => {
    if (!value) {
      examDate.value = ''
      return
    }
    const year = value.getFullYear()
    const month = String(value.getMonth() + 1).padStart(2, '0')
    const day = String(value.getDate()).padStart(2, '0')
    examDate.value = `${year}-${month}-${day}`
  },
})

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
      studentId.value = null
      academicYearId.value = null
      subjectId.value = null
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
    return 'Chọn học sinh, năm học và môn học để tạo kỳ thi lại.'
  }
  if (props.mode === 'score' && props.item) {
    const student = props.item.studentName || 'Học sinh'
    const subject = props.item.subjectName || 'Môn học'
    return `${student} · ${subject}`
  }
  return 'Kỳ thi lại sẽ được hủy và lịch sử vẫn được lưu lại.'
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
        <strong>Xác nhận:</strong>
        <span>
          Điểm chính thức sẽ được cập nhật lại. Dữ liệu lịch sử vẫn được lưu.
        </span>
      </div>
    </template>

    <!-- MODE: SCORE -->
    <template v-else-if="props.mode === 'score'">
      <div class="compare">
        <div class="compare-card">
          <div class="muted">Điểm trước thi lại</div>
          <div class="score before" style="font-size: 24px">
            {{ props.item?.preRetakeScore !== null && props.item?.preRetakeScore !== undefined ? props.item.preRetakeScore.toFixed(1) : '—' }}
          </div>
        </div>
        <div class="compare-card">
          <div class="muted">Điểm thi lại</div>
          <div class="score after" style="font-size: 24px">
            {{ props.item?.retakeScore !== null && props.item?.retakeScore !== undefined ? props.item.retakeScore.toFixed(1) : '—' }}
          </div>
        </div>
      </div>

      <div v-if="isCancelled" class="notice warn" data-testid="notice-cancelled-readonly">
        <strong>Chỉ đọc:</strong>
        <span>Kỳ thi lại này đã bị hủy. Điểm và thông tin không thể sửa đổi.</span>
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
          <DatePicker
            id="score-date"
            v-model="examDateModel"
            date-format="dd/mm/yy"
            placeholder="dd/mm/yyyy"
            show-icon
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
        <strong>Điểm chính thức:</strong>
        <span>
          {{ props.item.officialDtbmhCn.toFixed(1) }}
          <template v-if="props.item.calculationStatus === 'IN_PROGRESS'"> · đang cập nhật kết quả</template>
          <template v-else-if="props.item.calculationStatus === 'FINISH'"> · đã cập nhật kết quả</template>
        </span>
      </div>
      <div v-else-if="props.item?.calculationStatus === 'IN_PROGRESS'" class="notice warn">
        <strong>Đang xử lý:</strong>
        <span>Kết quả đang được cập nhật. Vui lòng tải lại sau ít phút.</span>
      </div>

      <div class="notice warn">
        <strong>Lưu ý:</strong>
        <span>Điểm hợp lệ từ 0.0 đến 10.0, tối đa 1 chữ số thập phân.</span>
      </div>
    </template>

    <!-- MODE: CREATE -->
    <template v-else>
      <div class="form-grid">
        <div class="field">
          <label for="create-student">Học sinh *</label>
          <Select
            id="create-student"
            v-model="studentId"
            :options="props.students"
            option-value="id"
            :option-label="(option) => option.code ? `${option.code} · ${option.name}` : option.name"
            placeholder="Chọn học sinh"
            fluid
            data-testid="select-student"
          />
        </div>
        <div class="field">
          <label for="create-year">Năm học *</label>
          <Select
            id="create-year"
            v-model="academicYearId"
            :options="props.academicYears"
            option-value="id"
            option-label="code"
            placeholder="Chọn năm học"
            fluid
            data-testid="select-year"
          />
        </div>
        <div class="field">
          <label for="create-subject">Môn học *</label>
          <Select
            id="create-subject"
            v-model="subjectId"
            :options="props.subjects"
            option-value="id"
            option-label="name"
            placeholder="Chọn môn học"
            fluid
            data-testid="select-subject"
          />
        </div>
        <div class="field">
          <label for="create-date">Ngày thi</label>
          <DatePicker
            id="create-date"
            v-model="examDateModel"
            date-format="dd/mm/yy"
            placeholder="dd/mm/yyyy"
            show-icon
            fluid
            data-testid="input-create-date"
          />
        </div>
        <div class="field wide">
          <label for="create-score">Điểm thi lại</label>
          <InputNumber
            id="create-score"
            v-model="retakeScore"
            :min="0"
            :max="10"
            :min-fraction-digits="0"
            :max-fraction-digits="1"
            :step="0.1"
            placeholder="Để trống để nhập điểm sau"
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
          label="Tạo kỳ thi lại"
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
