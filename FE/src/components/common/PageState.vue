<script setup lang="ts">
import Button from 'primevue/button'

import EmptyState from '@/components/common/EmptyState.vue'
import FormAlert from '@/components/common/FormAlert.vue'
import type { LoadingState } from '@/types/ui'

const props = withDefaults(defineProps<{
  state: LoadingState
  errorMessage?: string
  forbidden?: boolean
  forbiddenMessage?: string
  emptyHeading?: string
  emptyMessage?: string
  retryLabel?: string
}>(), {
  errorMessage: '',
  forbidden: false,
  forbiddenMessage: 'Bạn không có quyền xem nội dung này.',
  emptyHeading: 'Không có dữ liệu',
  emptyMessage: 'Chưa có bản ghi nào.',
  retryLabel: 'Thử lại',
})

const emit = defineEmits<{ retry: [] }>()
</script>

<template>
  <div v-if="props.state === 'loading'" class="page-state page-state-loading" role="status" aria-live="polite">
    <i class="pi pi-spin pi-spinner" aria-hidden="true" />
    <span>Đang tải...</span>
  </div>
  <FormAlert v-else-if="props.forbidden" tone="warning" :message="props.forbiddenMessage" />
  <div v-else-if="props.state === 'error'" class="page-state page-state-error">
    <FormAlert tone="error" :message="props.errorMessage || 'Không thể tải nội dung này.'" />
    <Button :label="props.retryLabel" icon="pi pi-refresh" severity="secondary" @click="emit('retry')" />
  </div>
  <EmptyState v-else-if="props.state === 'empty'" :heading="props.emptyHeading" :message="props.emptyMessage" />
  <slot v-else />
</template>
