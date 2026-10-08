<script setup lang="ts">
import { reactive, watch } from 'vue'
import Button from 'primevue/button'
import Checkbox from 'primevue/checkbox'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'

import FormAlert from '@/components/common/FormAlert.vue'
import type { BookCopy } from '@/types/library/catalog'

const props = withDefaults(defineProps<{ visible?: boolean; copy?: BookCopy | null; saving?: boolean; errorMessage?: string; errorTone?: 'error' | 'warning'; canReload?: boolean; reloading?: boolean }>(), { visible: false, copy: null, saving: false, errorMessage: '', errorTone: 'error', canReload: false, reloading: false })
const emit = defineEmits<{ 'update:visible': [visible: boolean]; save: [values: { shelfLocation: string; referenceOnly: boolean }]; reload: []; cancel: [] }>()
const values = reactive({ shelfLocation: '', referenceOnly: false })
const shelfError = reactive({ message: '' })

watch(() => [props.visible, props.copy], () => {
  values.shelfLocation = props.copy?.shelfLocation ?? ''
  values.referenceOnly = props.copy?.referenceOnly ?? false
  shelfError.message = ''
}, { immediate: true })

function close(): void {
  if (props.saving || props.reloading) return
  emit('update:visible', false)
  emit('cancel')
}

function save(): void {
  shelfError.message = values.shelfLocation.trim().length > 100 ? 'Vị trí kệ tối đa 100 ký tự.' : ''
  if (shelfError.message) return
  emit('save', { shelfLocation: values.shelfLocation.trim(), referenceOnly: values.referenceOnly })
}
</script>

<template>
  <Dialog :visible="props.visible" modal header="Sửa thông tin bản sao" :style="{ width: 'min(100% - 2rem, 600px)' }" :closable="!props.saving && !props.reloading" :close-on-escape="!props.saving && !props.reloading" :dismissable-mask="!props.saving && !props.reloading" @update:visible="emit('update:visible', $event)">
    <p class="dialog-caption">Mã vạch <strong>{{ props.copy?.barcode }}</strong>. Trạng thái mượn và giữ chỗ do nghiệp vụ lưu thông quản lý.</p>
    <FormAlert v-if="props.errorMessage" :tone="props.errorTone" :message="props.errorMessage" />
    <form class="form-stack" novalidate @submit.prevent="save">
      <div class="catalog-form-grid">
        <div class="field-group"><label for="copy-edit-shelf">Vị trí kệ</label><InputText id="copy-edit-shelf" v-model="values.shelfLocation" maxlength="100" fluid :invalid="Boolean(shelfError.message)" /><small v-if="shelfError.message" class="field-error">{{ shelfError.message }}</small></div>
        <div class="field-group wide"><label for="copy-edit-reference" class="checkbox-label"><Checkbox id="copy-edit-reference" v-model="values.referenceOnly" binary /> Chỉ đọc tại chỗ, không cho mượn</label></div>
      </div>
      <div class="form-actions"><Button v-if="props.canReload" type="button" label="Tải dữ liệu mới" icon="pi pi-refresh" severity="secondary" outlined :loading="props.reloading" :disabled="props.saving || props.reloading" @click="emit('reload')" /><Button type="button" label="Hủy" icon="pi pi-times" severity="secondary" outlined :disabled="props.saving || props.reloading" @click="close" /><Button type="submit" label="Lưu thay đổi" icon="pi pi-check" :loading="props.saving" :disabled="props.saving || props.reloading" /></div>
    </form>
  </Dialog>
</template>
