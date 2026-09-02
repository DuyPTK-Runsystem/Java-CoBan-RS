<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'

import type {
  AssessmentColumn,
  AssessmentType,
  CreateAssessmentColumnRequest,
  UpdateAssessmentColumnRequest,
} from '@/types/scorebook'

const props = defineProps<{
  visible: boolean
  mode: 'create' | 'edit'
  column: AssessmentColumn | null
  saving?: boolean
  errorMessage?: string
}>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  save: [value: CreateAssessmentColumnRequest | UpdateAssessmentColumnRequest]
  cancel: []
}>()

const type = ref<AssessmentType>('KTTT')
const columnNo = ref<number | null>(1)
const columnName = ref('')
const validationMessage = ref('')
const types = [
  { label: 'KTTT', value: 'KTTT' },
  { label: 'KTĐK', value: 'KTĐK' },
  { label: 'KTCK', value: 'KTCK' },
]
const title = computed(() => props.mode === 'create' ? 'Thêm assessment column' : 'Sửa assessment column')

watch(() => props.visible, (visible) => {
  if (!visible) return
  type.value = props.column?.assessmentType ?? 'KTTT'
  columnNo.value = props.column?.columnNo ?? 1
  columnName.value = props.column?.columnName ?? ''
  validationMessage.value = ''
})

function save(): void {
  validationMessage.value = ''
  if (columnName.value.trim().length > 100) {
    validationMessage.value = 'Tên cột không được vượt quá 100 ký tự.'
    return
  }
  if (props.mode === 'create') {
    if (!Number.isInteger(columnNo.value) || (columnNo.value ?? 0) <= 0) {
      validationMessage.value = 'Số thứ tự phải là số nguyên dương.'
      return
    }
    emit('save', {
      assessmentType: type.value,
      columnNo: columnNo.value as number,
      columnName: columnName.value.trim() || null,
    })
    return
  }
  emit('save', { columnName: columnName.value.trim() || null })
}
</script>

<template>
  <Dialog :visible="props.visible" :header="title" modal :style="{ width: 'min(560px, calc(100vw - 32px))' }" @update:visible="emit('update:visible', $event)">
    <div class="form-stack">
      <div v-if="validationMessage || props.errorMessage" class="form-alert form-alert-error" role="alert">{{ validationMessage || props.errorMessage }}</div>
      <div v-if="props.mode === 'create'" class="form-grid-two">
        <div class="field-group">
          <label for="assessment-type">Loại</label>
          <Select id="assessment-type" v-model="type" :options="types" option-label="label" option-value="value" fluid />
        </div>
        <div class="field-group">
          <label for="assessment-column-no">Số thứ tự</label>
          <InputText id="assessment-column-no" v-model.number="columnNo" type="number" min="1" step="1" fluid />
        </div>
      </div>
      <div class="field-group">
        <label for="assessment-column-name">Tên cột</label>
        <InputText id="assessment-column-name" v-model="columnName" maxlength="100" fluid />
      </div>
      <div class="form-actions">
        <Button label="Hủy" text :disabled="props.saving" @click="emit('cancel')" />
        <Button label="Lưu" icon="pi pi-check" :loading="props.saving" :disabled="props.saving" @click="save" />
      </div>
    </div>
  </Dialog>
</template>

