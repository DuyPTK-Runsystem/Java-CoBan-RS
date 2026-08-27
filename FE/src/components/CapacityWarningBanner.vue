<script setup lang="ts">
import FormAlert from '@/components/FormAlert.vue'

const props = withDefaults(defineProps<{
  available?: boolean
  warningCount?: number
  message?: string
}>(), { available: false, warningCount: 0, message: '' })
</script>

<template>
  <FormAlert v-if="!props.available" tone="info" message="Chưa có dữ liệu sĩ số hoặc cảnh báo từ backend contract." />
  <div v-else-if="props.warningCount > 0" class="form-alert form-alert-warning" role="status" aria-live="polite">
    <strong>{{ props.warningCount }} cảnh báo sĩ số</strong>
    <span>{{ props.message || 'Cảnh báo không chặn thao tác chỉnh sửa hoặc đóng lớp.' }}</span>
  </div>
  <FormAlert v-else tone="success" message="Không có cảnh báo sĩ số trong dữ liệu hiện tại." />
</template>
