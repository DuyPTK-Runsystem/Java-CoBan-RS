<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import Button from 'primevue/button'
import DatePicker from 'primevue/datepicker'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'

import FormAlert from '@/components/FormAlert.vue'
import StatusTag from '@/components/StatusTag.vue'
import type { AcademicYear, AcademicYearFormValues, AcademicYearStatus } from '@/types/academic'
import { formatAcademicDateInput, parseAcademicDate } from '@/utils/academicDate'

const props = withDefaults(defineProps<{
  visible?: boolean
  mode?: 'create' | 'edit'
  initialValue?: Partial<AcademicYear> | null
  saving?: boolean
  errorMessage?: string
}>(), {
  visible: false,
  mode: 'create',
  initialValue: null,
  saving: false,
  errorMessage: '',
})

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  save: [values: AcademicYearFormValues]
  cancel: []
}>()

const values = reactive<AcademicYearFormValues>({
  code: '',
  startDate: '',
  endDate: '',
  status: 'DRAFT',
  notes: '',
})
const errors = reactive<Record<string, string>>({})
const statusOptions: Array<{ label: string; value: AcademicYearStatus }> = [
  { label: 'Chưa hoạt động', value: 'DRAFT' },
  { label: 'Đang hoạt động', value: 'ACTIVE' },
]
const academicYearCodePattern = /^[0-9 -]+$/
const isEdit = computed(() => props.mode === 'edit')
const isReadOnly = computed(() => isEdit.value && props.initialValue?.status === 'CLOSED')
const heading = computed(() => isEdit.value ? 'Chỉnh sửa năm học' : 'Tạo năm học')
const caption = computed(() => isReadOnly.value ? 'Năm học đã đóng chỉ được xem.' : isEdit.value ? 'Cập nhật thông tin năm học chưa đóng.' : 'Thêm một năm học mới vào hệ thống.')
const startDateModel = computed<Date | null>({
  get: () => parseAcademicDate(values.startDate),
  set: (value) => { values.startDate = formatAcademicDateInput(value) },
})
const endDateModel = computed<Date | null>({
  get: () => parseAcademicDate(values.endDate),
  set: (value) => { values.endDate = formatAcademicDateInput(value) },
})

function syncValues(): void {
  Object.assign(values, {
    code: props.initialValue?.code ?? '',
    startDate: props.initialValue?.startDate ?? '',
    endDate: props.initialValue?.endDate ?? '',
    status: props.initialValue?.status === 'CLOSED' ? 'CLOSED' : props.initialValue?.status ?? 'DRAFT',
    notes: props.initialValue?.notes ?? '',
  })
  Object.keys(errors).forEach((key) => delete errors[key])
}

watch(() => [props.visible, props.initialValue, props.mode], syncValues, { deep: true, immediate: true })

function close(): void {
  emit('update:visible', false)
  emit('cancel')
}

function validate(): boolean {
  Object.keys(errors).forEach((key) => delete errors[key])
  const code = values.code.trim()
  if (!code) errors.code = 'Năm học là bắt buộc.'
  else if (!academicYearCodePattern.test(code)) errors.code = 'Năm học chỉ được gồm chữ số, dấu cách và dấu gạch ngang.'
  if (code.length > 20) errors.code = 'Năm học tối đa 20 ký tự.'
  if (!values.startDate) errors.startDate = 'Ngày bắt đầu là bắt buộc.'
  if (!values.endDate) errors.endDate = 'Ngày kết thúc là bắt buộc.'
  if (values.startDate && values.endDate && values.endDate <= values.startDate) errors.endDate = 'Ngày kết thúc phải sau ngày bắt đầu.'
  if (values.notes.length > 500) errors.notes = 'Ghi chú tối đa 500 ký tự.'
  return Object.keys(errors).length === 0
}

function save(): void {
  if (!isReadOnly.value && validate()) emit('save', { ...values, code: values.code.trim() })
}
</script>

<template>
  <Dialog :visible="props.visible" modal :header="heading" :style="{ width: 'min(100% - 2rem, 620px)' }" :closable="!props.saving" @update:visible="emit('update:visible', $event)">
    <p class="dialog-caption">{{ caption }}</p>
    <FormAlert v-if="props.errorMessage" tone="error" :message="props.errorMessage" />
    <div v-if="isReadOnly" class="form-alert form-alert-info">Dữ liệu lịch sử được giữ nguyên và không thể chỉnh sửa.</div>
    <form class="form-stack" novalidate @submit.prevent="save">
      <div class="field-group">
        <label for="academic-year-code">Năm học</label>
        <InputText id="academic-year-code" v-model="values.code" maxlength="20" pattern="[0-9 -]+" placeholder="Ví dụ: 2026-2027" :invalid="Boolean(errors.code)" :disabled="isReadOnly" />
        <small v-if="errors.code" class="field-error">{{ errors.code }}</small>
      </div>
      <div class="form-grid-two">
        <div class="field-group">
          <label for="academic-year-start">Ngày bắt đầu</label>
          <DatePicker id="academic-year-start" v-model="startDateModel" date-format="dd/mm/yy" placeholder="dd/mm/yyyy" show-icon fluid :invalid="Boolean(errors.startDate)" :disabled="isReadOnly" />
          <small v-if="errors.startDate" class="field-error">{{ errors.startDate }}</small>
        </div>
        <div class="field-group">
          <label for="academic-year-end">Ngày kết thúc</label>
          <DatePicker id="academic-year-end" v-model="endDateModel" date-format="dd/mm/yy" placeholder="dd/mm/yyyy" show-icon fluid :invalid="Boolean(errors.endDate)" :disabled="isReadOnly" />
          <small v-if="errors.endDate" class="field-error">{{ errors.endDate }}</small>
        </div>
      </div>
      <div class="field-group">
        <label for="academic-year-status">Trạng thái</label>
        <Select id="academic-year-status" v-model="values.status" :options="statusOptions" option-label="label" option-value="value" :disabled="isReadOnly" fluid />
        <small class="field-hint">Chỉ dùng trạng thái DRAFT hoặc ACTIVE khi tạo/cập nhật.</small>
      </div>
      <div class="field-group">
        <label for="academic-year-notes">Ghi chú</label>
        <Textarea id="academic-year-notes" v-model="values.notes" rows="3" maxlength="500" auto-resize :disabled="isReadOnly" />
        <small v-if="errors.notes" class="field-error">{{ errors.notes }}</small>
      </div>
      <div v-if="isReadOnly" class="dialog-readonly-status">
        <span>Trạng thái hiện tại</span>
        <StatusTag label="Đã đóng" severity="contrast" />
      </div>
      <div class="form-actions">
        <Button type="button" label="Hủy" icon="pi pi-times" severity="secondary" outlined :disabled="props.saving" @click="close" />
        <Button v-if="!isReadOnly" type="submit" :label="isEdit ? 'Lưu thay đổi' : 'Tạo năm học'" icon="pi pi-check" :loading="props.saving" />
      </div>
    </form>
  </Dialog>
</template>
