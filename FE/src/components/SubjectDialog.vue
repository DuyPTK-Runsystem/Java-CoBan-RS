<script setup lang="ts">
import { reactive, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'

import FormAlert from '@/components/FormAlert.vue'
import type { Subject, SubjectFormValues, SubjectStatus, SubjectType } from '@/types/academic'

const props = withDefaults(defineProps<{ visible?: boolean; mode?: 'create' | 'edit'; initialValue?: Partial<Subject> | null; saving?: boolean; errorMessage?: string }>(), { visible: false, mode: 'create', initialValue: null, saving: false, errorMessage: '' })
const emit = defineEmits<{ 'update:visible': [visible: boolean]; save: [values: SubjectFormValues]; cancel: [] }>()
const values = reactive<SubjectFormValues>({ code: '', name: '', subjectType: 'ACADEMIC', applicationScope: 'GRADE', status: 'ACTIVE' })
const errors = reactive<Record<string, string>>({})
const typeOptions: Array<{ label: string; value: SubjectType }> = [{ label: 'CHÍNH KHÓA', value: 'ACADEMIC' }, { label: 'KỸ NĂNG', value: 'SKILL' }]
const scopeOptions: Array<{ label: string; value: 'GRADE' | 'CLASS' }> = [{ label: 'Theo khối', value: 'GRADE' }, { label: 'Theo lớp', value: 'CLASS' }]
const statusOptions: Array<{ label: string; value: SubjectStatus }> = [{ label: 'Đang giảng dạy', value: 'ACTIVE' }, { label: 'Tạm ngưng giảng dạy', value: 'INACTIVE' }]

function syncValues(): void { Object.assign(values, { code: props.initialValue?.code ?? '', name: props.initialValue?.name ?? '', subjectType: props.initialValue?.subjectType ?? 'ACADEMIC', applicationScope: props.initialValue?.applicationScope ?? 'GRADE', status: props.initialValue?.status ?? 'ACTIVE' }); Object.keys(errors).forEach((key) => delete errors[key]) }
watch(() => [props.visible, props.initialValue, props.mode], syncValues, { deep: true, immediate: true })
function close(): void { emit('update:visible', false); emit('cancel') }
function validate(): boolean { Object.keys(errors).forEach((key) => delete errors[key]); if (!values.code.trim()) errors.code = 'Mã môn là bắt buộc.'; else if (values.code.trim().length > 30) errors.code = 'Mã môn tối đa 30 ký tự.'; if (!values.name.trim()) errors.name = 'Tên môn là bắt buộc.'; else if (values.name.trim().length > 150) errors.name = 'Tên môn tối đa 150 ký tự.'; return Object.keys(errors).length === 0 }
function save(): void { if (validate()) emit('save', { ...values, code: values.code.trim(), name: values.name.trim() }) }
</script>

<template>
  <Dialog :visible="props.visible" modal :header="props.mode === 'edit' ? 'Chỉnh sửa môn học' : 'Tạo môn học'" :style="{ width: 'min(100% - 2rem, 600px)' }" :closable="!props.saving" @update:visible="emit('update:visible', $event)">
    <p class="dialog-caption">Chọn loại môn, phạm vi áp dụng và trạng thái giảng dạy.</p>
    <FormAlert v-if="props.errorMessage" tone="error" :message="props.errorMessage" />
    <form class="form-stack" novalidate @submit.prevent="save">
      <div class="catalog-form-grid">
        <div class="field-group"><label for="subject-code">Mã môn</label><InputText id="subject-code" v-model="values.code" maxlength="30" placeholder="Ví dụ: MAT" :invalid="Boolean(errors.code)" fluid /><small v-if="errors.code" class="field-error">{{ errors.code }}</small></div>
        <div class="field-group"><label for="subject-name">Tên môn</label><InputText id="subject-name" v-model="values.name" maxlength="150" placeholder="Ví dụ: Toán" :invalid="Boolean(errors.name)" fluid /><small v-if="errors.name" class="field-error">{{ errors.name }}</small></div>
        <div class="field-group"><label for="subject-type">Loại môn</label><Select id="subject-type" v-model="values.subjectType" :options="typeOptions" option-label="label" option-value="value" fluid /></div>
        <div class="field-group"><label for="subject-scope">Phạm vi áp dụng</label><Select id="subject-scope" v-model="values.applicationScope" :options="scopeOptions" option-label="label" option-value="value" fluid /></div>
        <div class="field-group wide"><label for="subject-status">Trạng thái</label><Select id="subject-status" v-model="values.status" :options="statusOptions" option-label="label" option-value="value" fluid /></div>
      </div>
      <div class="form-actions"><Button type="button" label="Hủy" icon="pi pi-times" severity="secondary" outlined :disabled="props.saving" @click="close" /><Button type="submit" :label="props.mode === 'edit' ? 'Lưu thay đổi' : 'Tạo môn'" icon="pi pi-check" :loading="props.saving" /></div>
    </form>
  </Dialog>
</template>
