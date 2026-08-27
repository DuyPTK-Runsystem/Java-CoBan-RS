<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'

import FormAlert from '@/components/FormAlert.vue'
import type { GradeLevel, SchoolClass, Semester, Subject, SubjectApplicabilityFormValues } from '@/types/academic'

const props = withDefaults(defineProps<{ visible?: boolean; subject?: Subject | null; semesters?: Semester[]; grades?: GradeLevel[]; schoolClasses?: SchoolClass[]; saving?: boolean; errorMessage?: string; createdApplicability?: string }>(), { visible: false, subject: null, semesters: () => [], grades: () => [], schoolClasses: () => [], saving: false, errorMessage: '', createdApplicability: '' })
const emit = defineEmits<{ 'update:visible': [visible: boolean]; save: [values: SubjectApplicabilityFormValues]; cancel: [] }>()
const values = reactive<SubjectApplicabilityFormValues>({ semesterId: null, scopeType: 'GRADE', gradeLevelId: null, classId: null })
const errors = reactive<Record<string, string>>({})
const scopeOptions = [{ label: 'Theo khối', value: 'GRADE' as const }, { label: 'Theo lớp', value: 'CLASS' as const }]
const targetLabel = computed(() => values.scopeType === 'GRADE' ? 'Khối áp dụng' : 'Lớp áp dụng')
const scopeLabel = computed(() => values.scopeType === 'GRADE' ? 'Theo khối' : 'Theo lớp')
const targetOptions = computed(() => values.scopeType === 'GRADE'
  ? props.grades.map((grade) => ({ id: grade.id, label: grade.name }))
  : props.schoolClasses.map((schoolClass) => ({ id: schoolClass.id, label: `${schoolClass.classCode}${schoolClass.className ? ` · ${schoolClass.className}` : ''}` })))
const targetValue = computed<number | null>({ get: () => values.scopeType === 'GRADE' ? values.gradeLevelId : values.classId, set: (value) => { if (values.scopeType === 'GRADE') values.gradeLevelId = value; else values.classId = value } })

function syncValues(): void { Object.assign(values, { semesterId: null, scopeType: props.subject?.applicationScope ?? 'GRADE', gradeLevelId: null, classId: null }); Object.keys(errors).forEach((key) => delete errors[key]) }
watch(() => [props.visible, props.subject], syncValues, { deep: true, immediate: true })
watch(() => values.scopeType, () => { values.gradeLevelId = null; values.classId = null })
function close(): void { emit('update:visible', false); emit('cancel') }
function validate(): boolean { Object.keys(errors).forEach((key) => delete errors[key]); if (!values.semesterId) errors.semesterId = 'Học kỳ là bắt buộc.'; if (props.subject && values.scopeType !== props.subject.applicationScope) errors.scopeType = 'Scope type phải khớp applicationScope của môn.'; if (values.scopeType === 'GRADE' && !values.gradeLevelId) errors.target = 'Khối áp dụng là bắt buộc.'; if (values.scopeType === 'CLASS' && !values.classId) errors.target = 'Lớp áp dụng là bắt buộc.'; return Object.keys(errors).length === 0 }
function save(): void { if (validate()) emit('save', { ...values }) }
</script>

<template>
  <Dialog :visible="props.visible" modal header="Cấu hình phạm vi áp dụng" :style="{ width: 'min(100% - 2rem, 620px)' }" :closable="!props.saving" @update:visible="emit('update:visible', $event)">
    <p class="dialog-caption">{{ props.subject?.name || 'Môn học' }} · Phạm vi áp dụng: {{ scopeLabel }}</p>
    <FormAlert v-if="props.errorMessage" tone="error" :message="props.errorMessage" />
    <FormAlert v-if="props.createdApplicability" tone="success" :message="props.createdApplicability" />
    <form class="form-stack" novalidate @submit.prevent="save">
      <div class="field-group"><label for="applicability-semester">Học kỳ</label><Select id="applicability-semester" v-model="values.semesterId" :options="props.semesters" option-label="name" option-value="id" placeholder="Chọn học kỳ" :invalid="Boolean(errors.semesterId)" fluid /><small v-if="errors.semesterId" class="field-error">{{ errors.semesterId }}</small></div>
      <div class="catalog-form-grid">
        <div class="field-group"><label for="applicability-scope">Scope type</label><Select id="applicability-scope" v-model="values.scopeType" :options="scopeOptions" option-label="label" option-value="value" :invalid="Boolean(errors.scopeType)" fluid /><small v-if="errors.scopeType" class="field-error">{{ errors.scopeType }}</small></div>
        <div class="field-group"><label for="applicability-target">{{ targetLabel }}</label><Select id="applicability-target" v-model="targetValue" :options="targetOptions" option-label="label" option-value="id" placeholder="Chọn phạm vi" :invalid="Boolean(errors.target)" fluid /><small v-if="errors.target" class="field-error">{{ errors.target }}</small></div>
      </div>
      <FormAlert tone="info" message="Backend hiện chỉ có endpoint tạo applicability; UI không giả lập danh sách tải lại." />
      <div class="form-actions"><Button type="button" label="Hủy" icon="pi pi-times" severity="secondary" outlined :disabled="props.saving" @click="close" /><Button type="submit" label="Lưu cấu hình" icon="pi pi-check" :loading="props.saving" /></div>
    </form>
  </Dialog>
</template>
