<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'

import FormAlert from '@/components/FormAlert.vue'
import type { ApplicationScope, ClassSubject, ClassSubjectFormValues, ClassSubjectStatus, Subject } from '@/types/academic'

const props = withDefaults(defineProps<{ visible?: boolean; mode?: 'create' | 'edit'; initialValue?: Partial<ClassSubject> | null; availableSubjects?: Subject[]; classLabel?: string; semesterLabel?: string; classClosed?: boolean; semesterClosed?: boolean; saving?: boolean; errorMessage?: string; conflictMessage?: string }>(), { visible: false, mode: 'create', initialValue: null, availableSubjects: () => [], classLabel: 'Lớp chưa chọn', semesterLabel: 'Học kỳ chưa chọn', classClosed: false, semesterClosed: false, saving: false, errorMessage: '', conflictMessage: '' })
const emit = defineEmits<{ 'update:visible': [visible: boolean]; save: [values: ClassSubjectFormValues]; cancel: []; configureApplicability: [] }>()
const values = reactive<ClassSubjectFormValues>({ subjectId: null, status: 'ACTIVE' })
const errors = reactive<Record<string, string>>({})
const isReadOnly = computed(() => props.classClosed || props.semesterClosed)
const statusOptions: Array<{ label: string; value: ClassSubjectStatus }> = [{ label: 'Đang hoạt động', value: 'ACTIVE' }, { label: 'Ngừng hoạt động', value: 'INACTIVE' }, { label: 'Đã hoàn tất', value: 'COMPLETED' }]
const scopeLabels: Record<ApplicationScope, string> = { GRADE: 'Theo khối', CLASS: 'Theo lớp' }
const subjectOptions = computed(() => props.availableSubjects.map((subject) => ({ ...subject, displayName: `${subject.name} · ${scopeLabels[subject.applicationScope]}` })))

function syncValues(): void { Object.assign(values, { subjectId: props.initialValue?.subjectId ?? null, status: props.initialValue?.status ?? 'ACTIVE' }); Object.keys(errors).forEach((key) => delete errors[key]) }
watch(() => [props.visible, props.initialValue, props.mode], syncValues, { deep: true, immediate: true })
function close(): void { emit('update:visible', false); emit('cancel') }
function validate(): boolean { Object.keys(errors).forEach((key) => delete errors[key]); if (props.mode === 'create' && !values.subjectId) errors.subjectId = 'Môn học là bắt buộc.'; return Object.keys(errors).length === 0 }
function save(): void { if (!isReadOnly.value && validate()) emit('save', { ...values }) }
</script>

<template>
  <Dialog :visible="props.visible" modal header="Gán môn cho lớp" :style="{ width: 'min(100% - 2rem, 600px)' }" :closable="!props.saving" @update:visible="emit('update:visible', $event)">
    <div class="catalog-context"><div><span>Lớp</span><strong>{{ props.classLabel }}</strong></div><div><span>Học kỳ</span><strong>{{ props.semesterLabel }}</strong></div><div><span>Trạng thái context</span><strong>{{ isReadOnly ? 'Chỉ xem' : 'Có thể chỉnh sửa' }}</strong></div></div>
    <FormAlert v-if="props.errorMessage" tone="error" :message="props.errorMessage" />
    <FormAlert v-if="props.conflictMessage" tone="warning" :message="props.conflictMessage" />
    <div v-if="props.conflictMessage" class="catalog-conflict-link"><Button type="button" label="Cấu hình applicability" icon="pi pi-sliders-h" severity="warn" outlined @click="emit('configureApplicability')" /></div>
    <form class="form-stack" novalidate @submit.prevent="save">
      <div class="field-group"><label for="class-subject-subject">Môn học đang giảng dạy đã được áp dụng</label><Select id="class-subject-subject" v-model="values.subjectId" :options="subjectOptions" option-label="displayName" option-value="id" placeholder="Chọn môn học" :disabled="isReadOnly || props.mode === 'edit'" :invalid="Boolean(errors.subjectId)" fluid /><small v-if="errors.subjectId" class="field-error">{{ errors.subjectId }}</small></div>
      <div class="field-group"><label for="class-subject-status">Trạng thái lớp-môn</label><Select id="class-subject-status" v-model="values.status" :options="statusOptions" option-label="label" option-value="value" :disabled="isReadOnly || props.mode === 'create'" fluid /></div>
      <p class="field-hint">Mục mới mặc định là Đang hoạt động. Không có thao tác xóa để bảo toàn lịch sử.</p>
      <div class="form-actions"><Button type="button" label="Hủy" icon="pi pi-times" severity="secondary" outlined :disabled="props.saving" @click="close" /><Button v-if="!isReadOnly" type="submit" :label="props.mode === 'edit' ? 'Lưu trạng thái' : 'Gán môn'" icon="pi pi-check" :loading="props.saving" /></div>
    </form>
  </Dialog>
</template>
