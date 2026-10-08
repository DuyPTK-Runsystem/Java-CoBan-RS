<script setup lang="ts">
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'

import FormAlert from '@/components/common/FormAlert.vue'
import type { BookCopy } from '@/types/library/catalog'

const props = withDefaults(defineProps<{
  visible?: boolean
  copy?: BookCopy | null
  imageUrl?: string
  loading?: boolean
  errorMessage?: string
  errorTone?: 'error' | 'warning'
}>(), { visible: false, copy: null, imageUrl: '', loading: false, errorMessage: '', errorTone: 'error' })
const emit = defineEmits<{ 'update:visible': [visible: boolean]; download: []; retry: [] }>()

function close(): void {
  emit('update:visible', false)
}
</script>

<template>
  <Dialog :visible="props.visible" modal header="Mã vạch bản sao" :style="{ width: 'min(100% - 2rem, 480px)' }" @update:visible="emit('update:visible', $event)">
    <div class="barcode-dialog-content">
      <p class="dialog-caption">Mã vạch được tạo theo yêu cầu, không lưu ảnh trên hệ thống.</p>
      <code class="barcode-value">{{ props.copy?.barcode }}</code>
      <div v-if="props.loading" class="page-state page-state-loading" role="status"><i class="pi pi-spin pi-spinner" aria-hidden="true" /><span>Đang tạo ảnh mã vạch...</span></div>
      <FormAlert v-else-if="props.errorMessage" :tone="props.errorTone" :message="props.errorMessage" />
      <img v-else-if="props.imageUrl" class="barcode-image" :src="props.imageUrl" :alt="`Mã vạch ${props.copy?.barcode}`">
      <p v-else class="muted-copy">Ảnh mã vạch chưa được tải.</p>
      <div class="form-actions">
        <Button v-if="props.errorMessage" label="Thử lại" icon="pi pi-refresh" severity="secondary" outlined @click="emit('retry')" />
        <Button label="Tải ảnh PNG" icon="pi pi-download" :disabled="!props.imageUrl || props.loading" @click="emit('download')" />
        <Button label="Đóng" severity="secondary" text @click="close" />
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
.barcode-dialog-content { display: flex; flex-direction: column; align-items: center; gap: 1rem; }
.barcode-value { padding: .5rem .75rem; border-radius: .5rem; background: var(--surface-100); font-size: 1rem; }
.barcode-image { display: block; max-width: 100%; min-height: 72px; object-fit: contain; }
.muted-copy { color: var(--text-color-secondary); }
.form-actions { width: 100%; justify-content: flex-end; flex-wrap: wrap; }
</style>
