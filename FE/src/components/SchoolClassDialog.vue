<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'

import FormAlert from '@/components/FormAlert.vue'
import StatusTag from '@/components/StatusTag.vue'
import type { AcademicYear, GradeLevel, SchoolClass, SchoolClassFormValues, SchoolClassStatus } from '@/types/academic'

const props = withDefaults(defineProps<{
  visible?: boolean
  mode?: 'create' | 'edit'
  initialValue?: Partial<SchoolClass> | null
  academicYears?: AcademicYear[]
  grades?: GradeLevel[]
  saving?: boolean
  errorMessage?: string
}>(), { visible: false, mode: 'create', initialValue: null, academicYears: () => [], grades: () => [], saving: false, errorMessage: '' })

const emit = defineEmits<{ 'update:visible': [visible: boolean]; save: [values: SchoolClassFormValues]; cancel: [] }>()
const values = reactive<SchoolClassFormValues>({ academicYearId: null, gradeLevelId: null, classCode: '', className: '', capacity: null, status: 'PLANNED' })
const errors = reactive<Record<string, string>>({})
const isEdit = computed(() => props.mode === 'edit')
const isClosed = computed(() => props.initialValue?.status === 'CLOSED')
const heading = computed(() => isEdit.value ? 'Chỉnh sửa lớp' : 'Tạo lớp học')
const statusOptions: Array<{ label: string; value: SchoolClassStatus }> = [{ label: 'Đã khởi tạo', value: 'PLANNED' }, { label: 'Đang hoạt động', value: 'ACTIVE' }]

function syncValues(): void {
  Object.assign(values, { academicYearId: props.initialValue?.academicYearId ?? null, gradeLevelId: props.initialValue?.gradeLevelId ?? null, classCode: props.initialValue?.classCode ?? '', className: props.initialValue?.className ?? '', capacity: props.initialValue?.capacity ?? null, status: props.initialValue?.status === 'CLOSED' ? 'CLOSED' : props.initialValue?.status ?? 'PLANNED' })
  Object.keys(errors).forEach((key) => delete errors[key])
}
watch(() => [props.visible, props.initialValue, props.mode], syncValues, { deep: true, immediate: true })

function close(): void { emit('update:visible', false); emit('cancel') }
function validate(): boolean {
  Object.keys(errors).forEach((key) => delete errors[key])
  if (!values.academicYearId) errors.academicYearId = 'Năm học là bắt buộc.'
  if (!values.gradeLevelId) errors.gradeLevelId = 'Khối là bắt buộc.'
  if (!values.classCode.trim()) errors.classCode = 'Mã lớp là bắt buộc.'
  else if (values.classCode.trim().length > 30) errors.classCode = 'Mã lớp tối đa 30 ký tự.'
  if (values.className.length > 100) errors.className = 'Tên lớp tối đa 100 ký tự.'
  if (values.capacity !== null && values.capacity < 1) errors.capacity = 'Sức chứa phải là số dương.'
  return Object.keys(errors).length === 0
}
function save(): void { if (!isClosed.value && validate()) emit('save', { ...values, classCode: values.classCode.trim(), className: values.className.trim() }) }
</script>

<template>
  <Dialog :visible="props.visible" modal :header="heading" :style="{ width: 'min(100% - 2rem, 640px)' }" :closable="!props.saving" @update:visible="emit('update:visible', $event)">
    <p class="dialog-caption">Lớp thuộc đúng một năm học và một khối.</p>
    <FormAlert v-if="props.errorMessage" tone="error" :message="props.errorMessage" />
    <div v-if="isClosed" class="catalog-readonly-note"><i class="pi pi-lock" aria-hidden="true" /><span>Lớp đã đóng, dữ liệu chỉ được xem.</span><StatusTag label="Đã đóng" severity="contrast" /></div>
    <form class="form-stack" novalidate @submit.prevent="save">
      <div class="catalog-form-grid">
        <div class="field-group"><label for="class-year">Năm học</label><Select id="class-year" v-model="values.academicYearId" :options="props.academicYears" option-label="code" option-value="id" placeholder="Chọn năm học" :disabled="isClosed || isEdit" :invalid="Boolean(errors.academicYearId)" fluid /><small v-if="errors.academicYearId" class="field-error">{{ errors.academicYearId }}</small></div>
        <div class="field-group"><label for="class-grade">Khối</label><Select id="class-grade" v-model="values.gradeLevelId" :options="props.grades" option-label="name" option-value="id" placeholder="Chọn khối" :disabled="isClosed" :invalid="Boolean(errors.gradeLevelId)" fluid /><small v-if="errors.gradeLevelId" class="field-error">{{ errors.gradeLevelId }}</small></div>
        <div class="field-group"><label for="class-code">Mã lớp</label><InputText id="class-code" v-model="values.classCode" maxlength="30" placeholder="Ví dụ: 6A1" :disabled="isClosed" :invalid="Boolean(errors.classCode)" fluid /><small v-if="errors.classCode" class="field-error">{{ errors.classCode }}</small></div>
        <div class="field-group"><label for="class-name">Tên lớp</label><InputText id="class-name" v-model="values.className" maxlength="100" placeholder="Tên hiển thị tùy chọn" :disabled="isClosed" :invalid="Boolean(errors.className)" fluid /><small v-if="errors.className" class="field-error">{{ errors.className }}</small></div>
        <div class="field-group"><label for="class-capacity">Sức chứa dự kiến</label><InputNumber id="class-capacity" v-model="values.capacity" :min="1" :use-grouping="false" placeholder="Không bắt buộc" :disabled="isClosed" :invalid="Boolean(errors.capacity)" fluid /><small v-if="errors.capacity" class="field-error">{{ errors.capacity }}</small></div>
        <div class="field-group"><label for="class-status">Trạng thái</label><Select id="class-status" v-model="values.status" :options="statusOptions" option-label="label" option-value="value" :disabled="isClosed" fluid /></div>
      </div>
      <div class="form-actions"><Button type="button" label="Hủy" icon="pi pi-times" severity="secondary" outlined :disabled="props.saving" @click="close" /><Button v-if="!isClosed" type="submit" :label="isEdit ? 'Lưu thay đổi' : 'Tạo lớp'" icon="pi pi-check" :loading="props.saving" /></div>
    </form>
  </Dialog>
</template>
