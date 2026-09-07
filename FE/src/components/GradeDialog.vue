<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Textarea from 'primevue/textarea'

import FormAlert from '@/components/FormAlert.vue'
import type { GradeLevel, GradeLevelFormValues } from '@/types/academic'

const props = withDefaults(defineProps<{
  visible?: boolean
  mode?: 'create' | 'edit'
  initialValue?: Partial<GradeLevel> | null
  grades?: GradeLevel[]
  saving?: boolean
  errorMessage?: string
}>(), {
  visible: false,
  mode: 'create',
  initialValue: null,
  grades: () => [],
  saving: false,
  errorMessage: '',
})

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  save: [values: GradeLevelFormValues]
  cancel: []
}>()

const values = reactive<GradeLevelFormValues>({ code: '', name: '', gradeLevel: null, displayOrder: 1, nextGradeId: null, active: true, description: '' })
const errors = reactive<Record<string, string>>({})
const isEdit = computed(() => props.mode === 'edit')
const heading = computed(() => isEdit.value ? 'Chỉnh sửa khối' : 'Tạo khối')
const gradeOptions = [6, 7, 8, 9].map((value) => ({ label: `Cấp ${value}`, value }))
const nextGradeOptions = computed(() => props.grades.filter((grade) => grade.id !== props.initialValue?.id).map((grade) => ({ label: grade.name, value: grade.id })))

function syncValues(): void {
  Object.assign(values, {
    code: props.initialValue?.code ?? '',
    name: props.initialValue?.name ?? '',
    gradeLevel: props.initialValue?.gradeLevel ?? null,
    displayOrder: props.initialValue?.displayOrder ?? 1,
    nextGradeId: props.initialValue?.nextGradeId ?? null,
    active: props.initialValue?.active ?? true,
    description: props.initialValue?.description ?? '',
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
  if (!values.code.trim()) errors.code = 'Mã khối là bắt buộc.'
  else if (values.code.trim().length > 10) errors.code = 'Mã khối tối đa 10 ký tự.'
  if (!values.name.trim()) errors.name = 'Tên khối là bắt buộc.'
  else if (values.name.trim().length > 50) errors.name = 'Tên khối tối đa 50 ký tự.'
  if (!values.gradeLevel) errors.gradeLevel = 'Cấp là bắt buộc.'
  if (values.nextGradeId === props.initialValue?.id) errors.nextGradeId = 'Khối tiếp theo phải khác khối hiện tại.'
  if (values.description.length > 255) errors.description = 'Mô tả tối đa 255 ký tự.'
  return Object.keys(errors).length === 0
}

function save(): void {
  if (validate()) emit('save', { ...values, code: values.code.trim(), name: values.name.trim() })
}
</script>

<template>
  <Dialog :visible="props.visible" modal :header="heading" :style="{ width: 'min(100% - 2rem, 640px)' }" :closable="!props.saving" @update:visible="emit('update:visible', $event)">
    <p class="dialog-caption">Metadata khối dùng chung; trạng thái không xóa lịch sử.</p>
    <FormAlert v-if="props.errorMessage" tone="error" :message="props.errorMessage" />
    <form class="form-stack" novalidate @submit.prevent="save">
      <div class="catalog-form-grid">
        <div class="field-group"><label for="grade-code">Mã khối</label><InputText id="grade-code" v-model="values.code" maxlength="10" placeholder="Ví dụ: GRADE_6" :invalid="Boolean(errors.code)" fluid /><small v-if="errors.code" class="field-error">{{ errors.code }}</small></div>
        <div class="field-group"><label for="grade-name">Tên khối</label><InputText id="grade-name" v-model="values.name" maxlength="50" placeholder="Ví dụ: Khối 6" :invalid="Boolean(errors.name)" fluid /><small v-if="errors.name" class="field-error">{{ errors.name }}</small></div>
        <div class="field-group"><label for="grade-level">Cấp</label><Select id="grade-level" v-model="values.gradeLevel" :options="gradeOptions" option-label="label" option-value="value" placeholder="Chọn cấp" :invalid="Boolean(errors.gradeLevel)" fluid /><small v-if="errors.gradeLevel" class="field-error">{{ errors.gradeLevel }}</small></div>
        <div class="field-group"><label for="next-grade">Khối tiếp theo</label><Select id="next-grade" v-model="values.nextGradeId" :options="nextGradeOptions" option-label="label" option-value="value" placeholder="Không có" show-clear :invalid="Boolean(errors.nextGradeId)" fluid /><small v-if="errors.nextGradeId" class="field-error">{{ errors.nextGradeId }}</small></div>
        <div class="field-group"><label for="grade-active">Trạng thái</label><div class="catalog-status-option"><Checkbox id="grade-active" v-model="values.active" binary /><span>{{ values.active ? 'Đang dùng' : 'Ngừng dùng' }}</span></div></div>
        <div class="field-group wide"><label for="grade-description">Mô tả</label><Textarea id="grade-description" v-model="values.description" rows="3" maxlength="255" auto-resize :invalid="Boolean(errors.description)" fluid /><small v-if="errors.description" class="field-error">{{ errors.description }}</small></div>
      </div>
      <div class="form-actions"><Button type="button" label="Hủy" icon="pi pi-times" severity="secondary" outlined :disabled="props.saving" @click="close" /><Button type="submit" :label="isEdit ? 'Lưu thay đổi' : 'Tạo khối'" icon="pi pi-check" :loading="props.saving" /></div>
    </form>
  </Dialog>
</template>
