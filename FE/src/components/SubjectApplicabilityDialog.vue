<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'

import FormAlert from '@/components/FormAlert.vue'
import SubjectApplicabilityTable from '@/components/SubjectApplicabilityTable.vue'
import type { GradeLevel, SchoolClass, Semester, Subject, SubjectApplicability, SubjectApplicabilityFormValues } from '@/types/academic'

const props = withDefaults(defineProps<{
  visible?: boolean
  mode?: 'create' | 'edit'
  subject?: Subject | null
  initialValue?: SubjectApplicability | null
  applicabilities?: SubjectApplicability[]
  applicabilityLoading?: boolean
  semesters?: Semester[]
  grades?: GradeLevel[]
  schoolClasses?: SchoolClass[]
  saving?: boolean
  errorMessage?: string
}>(), {
  visible: false,
  mode: 'create',
  subject: null,
  initialValue: null,
  applicabilities: () => [],
  applicabilityLoading: false,
  semesters: () => [],
  grades: () => [],
  schoolClasses: () => [],
  saving: false,
  errorMessage: '',
})

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  save: [values: SubjectApplicabilityFormValues]
  create: []
  edit: [applicability: SubjectApplicability]
  deactivate: [applicability: SubjectApplicability]
  reactivate: [applicability: SubjectApplicability]
  cancel: []
}>()

const values = reactive<SubjectApplicabilityFormValues>({ semesterId: null, scopeType: 'GRADE', gradeLevelId: null, classId: null })
const errors = reactive<Record<string, string>>({})
const scopeOptions = [{ label: 'Theo khối', value: 'GRADE' as const }, { label: 'Theo lớp', value: 'CLASS' as const }]
const targetLabel = computed(() => values.scopeType === 'GRADE' ? 'Khối áp dụng' : 'Lớp áp dụng')
const scopeLabel = computed(() => values.scopeType === 'GRADE' ? 'Theo khối' : 'Theo lớp')
const targetOptions = computed(() => values.scopeType === 'GRADE'
  ? props.grades.map((grade) => ({ id: grade.id, label: grade.name }))
  : props.schoolClasses.map((schoolClass) => ({ id: schoolClass.id, label: `${schoolClass.classCode}${schoolClass.className ? ` · ${schoolClass.className}` : ''}` })))
const targetValue = computed<number | null>({
  get: () => values.scopeType === 'GRADE' ? values.gradeLevelId : values.classId,
  set: (value) => { if (values.scopeType === 'GRADE') values.gradeLevelId = value; else values.classId = value },
})
const heading = computed(() => props.mode === 'edit' ? 'Sửa cấu hình phạm vi áp dụng' : 'Cấu hình phạm vi áp dụng')
const formHeading = computed(() => props.mode === 'edit' ? 'Chỉnh sửa cấu hình' : 'Thêm cấu hình')
let syncing = false

function syncValues(): void {
  syncing = true
  Object.assign(values, {
    semesterId: props.initialValue?.semesterId ?? null,
    scopeType: props.initialValue?.scopeType ?? props.subject?.applicationScope ?? 'GRADE',
    gradeLevelId: props.initialValue?.gradeLevelId ?? null,
    classId: props.initialValue?.classId ?? null,
  })
  Object.keys(errors).forEach((key) => delete errors[key])
  syncing = false
}

watch(() => [props.visible, props.subject, props.initialValue, props.mode], syncValues, { deep: true, immediate: true })
watch(() => values.scopeType, () => {
  if (syncing) return
  values.gradeLevelId = null
  values.classId = null
}, { flush: 'sync' })

function close(): void {
  emit('update:visible', false)
  emit('cancel')
}

function validate(): boolean {
  Object.keys(errors).forEach((key) => delete errors[key])
  if (!values.semesterId) errors.semesterId = 'Học kỳ là bắt buộc.'
  if (props.subject && values.scopeType !== props.subject.applicationScope) errors.scopeType = 'Scope type phải khớp applicationScope của môn.'
  if (values.scopeType === 'GRADE' && !values.gradeLevelId) errors.target = 'Khối áp dụng là bắt buộc.'
  if (values.scopeType === 'CLASS' && !values.classId) errors.target = 'Lớp áp dụng là bắt buộc.'
  return Object.keys(errors).length === 0
}

function save(): void {
  if (validate()) emit('save', { ...values })
}
</script>

<template>
  <Dialog :visible="props.visible" modal :header="heading" :style="{ width: 'min(100% - 2rem, 760px)' }" :closable="!props.saving" @update:visible="emit('update:visible', $event)">
    <p class="dialog-caption">{{ props.subject?.name || 'Môn học' }} · Phạm vi áp dụng: {{ scopeLabel }}</p>
    <FormAlert v-if="props.errorMessage" tone="error" :message="props.errorMessage" />
    <SubjectApplicabilityTable
      :applicabilities="props.applicabilities"
      :semesters="props.semesters"
      :grades="props.grades"
      :school-classes="props.schoolClasses"
      :loading="props.applicabilityLoading"
      @create="emit('create')"
      @edit="emit('edit', $event)"
      @deactivate="emit('deactivate', $event)"
      @reactivate="emit('reactivate', $event)"
    />
    <form class="form-stack applicability-form" novalidate @submit.prevent="save">
      <div class="section-heading applicability-form-heading">
        <h2>{{ formHeading }}</h2>
        <span v-if="props.mode === 'edit'" class="section-caption">ID #{{ props.initialValue?.id }}</span>
      </div>
      <div class="field-group"><label for="applicability-semester">Học kỳ</label><Select id="applicability-semester" v-model="values.semesterId" :options="props.semesters" option-label="name" option-value="id" placeholder="Chọn học kỳ" :invalid="Boolean(errors.semesterId)" fluid /><small v-if="errors.semesterId" class="field-error">{{ errors.semesterId }}</small></div>
      <div class="catalog-form-grid">
        <div class="field-group"><label for="applicability-scope">Phạm vi</label><Select id="applicability-scope" v-model="values.scopeType" :options="scopeOptions" option-label="label" option-value="value" :invalid="Boolean(errors.scopeType)" fluid /><small v-if="errors.scopeType" class="field-error">{{ errors.scopeType }}</small></div>
        <div class="field-group"><label for="applicability-target">{{ targetLabel }}</label><Select id="applicability-target" v-model="targetValue" :options="targetOptions" option-label="label" option-value="id" placeholder="Chọn phạm vi" :invalid="Boolean(errors.target)" fluid /><small v-if="errors.target" class="field-error">{{ errors.target }}</small></div>
      </div>
      <div class="form-actions"><Button type="button" label="Hủy" icon="pi pi-times" severity="secondary" outlined :disabled="props.saving" @click="close" /><Button type="submit" :label="props.mode === 'edit' ? 'Lưu thay đổi' : 'Lưu cấu hình'" icon="pi pi-check" :loading="props.saving" /></div>
    </form>
  </Dialog>
</template>
