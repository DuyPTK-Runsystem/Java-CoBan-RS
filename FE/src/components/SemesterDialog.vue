<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import Button from 'primevue/button'
import DatePicker from 'primevue/datepicker'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'

import FormAlert from '@/components/FormAlert.vue'
import type { AcademicYear, Semester, SemesterFormValues } from '@/types/academic'
import { formatAcademicDate, formatAcademicDateInput, formatAcademicDateTimeInput, parseAcademicDate, parseAcademicDateTime } from '@/utils/academicDate'

const props = withDefaults(defineProps<{
  visible?: boolean
  mode?: 'create' | 'edit'
  academicYear?: AcademicYear | null
  initialValue?: Partial<Semester> | null
  saving?: boolean
  errorMessage?: string
}>(), {
  visible: false,
  mode: 'create',
  academicYear: null,
  initialValue: null,
  saving: false,
  errorMessage: '',
})

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  save: [values: SemesterFormValues]
  cancel: []
}>()

const values = reactive<SemesterFormValues>({
  code: '',
  name: '',
  displayOrder: null,
  startDate: '',
  endDate: '',
  automaticLockAt: '',
})
const errors = reactive<Record<string, string>>({})
const semesterCodeOptions = ['HK1', 'HK2']
const isEdit = computed(() => props.mode === 'edit')
const heading = computed(() => isEdit.value ? 'Chỉnh sửa học kỳ' : 'Tạo học kỳ')
const caption = computed(() => `Năm học ${props.academicYear?.code ?? ''}. ${isEdit.value ? 'Cập nhật thông tin học kỳ.' : 'Thêm học kỳ với trạng thái mặc định Nháp.'}`)
const startDateModel = computed<Date | null>({
  get: () => parseAcademicDate(values.startDate),
  set: (value) => { values.startDate = formatAcademicDateInput(value) },
})
const endDateModel = computed<Date | null>({
  get: () => parseAcademicDate(values.endDate),
  set: (value) => { values.endDate = formatAcademicDateInput(value) },
})
const automaticLockAtModel = computed<Date | null>({
  get: () => parseAcademicDateTime(values.automaticLockAt),
  set: (value) => { values.automaticLockAt = formatAcademicDateTimeInput(value) },
})

function syncValues(): void {
  Object.assign(values, {
    code: props.initialValue?.code ?? '',
    name: props.initialValue?.name ?? '',
    displayOrder: props.initialValue?.displayOrder ?? null,
    startDate: props.initialValue?.startDate ?? '',
    endDate: props.initialValue?.endDate ?? '',
    automaticLockAt: props.initialValue?.automaticLockAt ?? '',
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
  if (!values.code.trim()) errors.code = 'Mã học kỳ là bắt buộc.'
  if (values.code.length > 20) errors.code = 'Mã học kỳ tối đa 20 ký tự.'
  if (!values.name.trim()) errors.name = 'Tên học kỳ là bắt buộc.'
  if (values.name.length > 100) errors.name = 'Tên học kỳ tối đa 100 ký tự.'
  if (!values.displayOrder || values.displayOrder < 1) errors.displayOrder = 'Thứ tự hiển thị phải là số dương.'
  if (!values.startDate) errors.startDate = 'Ngày bắt đầu là bắt buộc.'
  if (!values.endDate) errors.endDate = 'Ngày kết thúc là bắt buộc.'
  if (values.startDate && values.endDate && values.endDate <= values.startDate) errors.endDate = 'Ngày kết thúc phải sau ngày bắt đầu.'
  if (props.academicYear?.startDate && values.startDate && values.startDate < props.academicYear.startDate) errors.startDate = `Ngày phải từ ${formatAcademicDate(props.academicYear.startDate)} trở đi.`
  if (props.academicYear?.endDate && values.endDate && values.endDate > props.academicYear.endDate) errors.endDate = `Ngày phải đến ${formatAcademicDate(props.academicYear.endDate)}.`
  return Object.keys(errors).length === 0
}

function save(): void {
  if (validate()) emit('save', { ...values, code: values.code.trim(), name: values.name.trim() })
}
</script>

<template>
  <Dialog :visible="props.visible" modal :header="heading" :style="{ width: 'min(100% - 2rem, 620px)' }" :closable="!props.saving" @update:visible="emit('update:visible', $event)">
    <p class="dialog-caption">{{ caption }}</p>
    <FormAlert v-if="props.errorMessage" tone="error" :message="props.errorMessage" />
    <div v-if="props.academicYear" class="context-strip">
      <span class="context-strip-label">Phạm vi năm học</span>
      <strong>{{ props.academicYear.code }}</strong>
      <span>{{ formatAcademicDate(props.academicYear.startDate) }} → {{ formatAcademicDate(props.academicYear.endDate) }}</span>
    </div>
    <form class="form-stack" novalidate @submit.prevent="save">
      <div class="form-grid-two">
        <div class="field-group">
          <label for="semester-code">Mã học kỳ</label>
          <Select id="semester-code" v-model="values.code" :options="semesterCodeOptions" editable maxlength="20" placeholder="Ví dụ: HK1" :invalid="Boolean(errors.code)" fluid />
          <small v-if="errors.code" class="field-error">{{ errors.code }}</small>
        </div>
        <div class="field-group">
          <label for="semester-order">Trong năm học, đây là học kì thứ</label>
          <InputNumber id="semester-order" v-model="values.displayOrder" :min="1" :use-grouping="false" :invalid="Boolean(errors.displayOrder)" fluid />
          <small v-if="errors.displayOrder" class="field-error">{{ errors.displayOrder }}</small>
        </div>
      </div>
      <div class="field-group">
        <label for="semester-name">Tên học kỳ</label>
        <InputText id="semester-name" v-model="values.name" maxlength="100" placeholder="Ví dụ: Học kỳ I" :invalid="Boolean(errors.name)" />
        <small v-if="errors.name" class="field-error">{{ errors.name }}</small>
      </div>
      <div class="form-grid-two">
        <div class="field-group">
          <label for="semester-start">Ngày bắt đầu</label>
          <DatePicker id="semester-start" v-model="startDateModel" date-format="dd/mm/yy" placeholder="dd/mm/yyyy" show-icon fluid :invalid="Boolean(errors.startDate)" />
          <small v-if="errors.startDate" class="field-error">{{ errors.startDate }}</small>
        </div>
        <div class="field-group">
          <label for="semester-end">Ngày kết thúc</label>
          <DatePicker id="semester-end" v-model="endDateModel" date-format="dd/mm/yy" placeholder="dd/mm/yyyy" show-icon fluid :invalid="Boolean(errors.endDate)" />
          <small v-if="errors.endDate" class="field-error">{{ errors.endDate }}</small>
        </div>
      </div>
      <div class="field-group">
        <label for="semester-lock-at">Thời điểm tự động khóa</label>
        <DatePicker id="semester-lock-at" v-model="automaticLockAtModel" date-format="dd/mm/yy" placeholder="dd/mm/yyyy hh:mm:ss" show-icon icon="pi pi-calendar-clock" show-time show-seconds hour-format="24" fluid />
        <small class="field-hint">Chọn ngày và giờ theo định dạng dd/mm/yyyy hh:mm:ss; dữ liệu gửi API vẫn là local datetime.</small>
      </div>
      <div class="form-alert form-alert-info">Học kỳ mới luôn bắt đầu ở trạng thái Nháp. Kích hoạt, khóa và mở lại dùng action lifecycle riêng.</div>
      <div class="form-actions">
        <Button type="button" label="Hủy" icon="pi pi-times" severity="secondary" outlined :disabled="props.saving" @click="close" />
        <Button type="submit" :label="isEdit ? 'Lưu thay đổi' : 'Tạo học kỳ'" icon="pi pi-check" :loading="props.saving" />
      </div>
    </form>
  </Dialog>
</template>
