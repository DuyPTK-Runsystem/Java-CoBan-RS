<script setup lang="ts">
import { reactive, watch } from 'vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'

import FormAlert from '@/components/common/FormAlert.vue'

const props = withDefaults(defineProps<{ visible?: boolean; saving?: boolean; errorMessage?: string; errorTone?: 'error' | 'warning' }>(), { visible: false, saving: false, errorMessage: '', errorTone: 'error' })
const emit = defineEmits<{ 'update:visible': [visible: boolean]; save: [values: { quantity: number; shelfLocation: string; referenceOnly: boolean }]; cancel: [] }>()
const values = reactive({ quantity: 1, shelfLocation: '', referenceOnly: false })
const errors = reactive<Record<string, string>>({})

watch(() => props.visible, (visible) => {
  if (!visible) return
  values.quantity = 1
  values.shelfLocation = ''
  values.referenceOnly = false
  Object.keys(errors).forEach((key) => delete errors[key])
})

function close(): void {
  visibilityChanged(false)
}

function visibilityChanged(visible: boolean): void {
  emit('update:visible', visible)
  if (!visible) emit('cancel')
}

function save(): void {
  Object.keys(errors).forEach((key) => delete errors[key])
  if (!Number.isInteger(values.quantity) || values.quantity < 1 || values.quantity > 100) errors.quantity = 'Số lượng phải từ 1 đến 100.'
  if (values.shelfLocation.trim().length > 100) errors.shelfLocation = 'Vị trí kệ tối đa 100 ký tự.'
  if (Object.keys(errors).length > 0) return
  emit('save', { ...values, shelfLocation: values.shelfLocation.trim() })
}
</script>

<template>
  <Dialog :visible="props.visible" modal header="Thêm bản sao" :style="{ width: 'min(100% - 2rem, 600px)' }" :closable="!props.saving" :close-on-escape="!props.saving" :dismissable-mask="!props.saving" @update:visible="visibilityChanged">
    <p class="dialog-caption">Tạo nhiều bản sao cho đầu sách. Mã vạch được sinh tự động ở máy chủ.</p>
    <FormAlert v-if="props.errorMessage" :tone="props.errorTone" :message="props.errorMessage" />
    <form class="form-stack" novalidate @submit.prevent="save">
      <div class="catalog-form-grid">
        <div class="field-group"><label for="copy-quantity">Số lượng <span class="required-mark" aria-hidden="true">*</span></label><InputNumber id="copy-quantity" v-model="values.quantity" :min="1" :max="100" :use-grouping="false" fluid :invalid="Boolean(errors.quantity)" /><small v-if="errors.quantity" class="field-error">{{ errors.quantity }}</small></div>
        <div class="field-group"><label for="copy-shelf">Vị trí kệ</label><InputText id="copy-shelf" v-model="values.shelfLocation" maxlength="100" fluid :invalid="Boolean(errors.shelfLocation)" /><small v-if="errors.shelfLocation" class="field-error">{{ errors.shelfLocation }}</small></div>
        <div class="field-group wide"><label for="copy-reference" class="checkbox-label"><Checkbox id="copy-reference" v-model="values.referenceOnly" binary /> Chỉ đọc tại chỗ, không cho mượn</label></div>
      </div>
      <div class="form-actions"><Button type="button" label="Hủy" icon="pi pi-times" severity="secondary" outlined :disabled="props.saving" @click="close" /><Button type="submit" label="Thêm bản sao" icon="pi pi-plus" :loading="props.saving" :disabled="props.saving" /></div>
    </form>
  </Dialog>
</template>
