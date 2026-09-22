<script setup lang="ts">
import Button from 'primevue/button'
import type { SemesterNotification, SemesterNotificationStatus } from '@/types/academic'

const props = withDefaults(defineProps<{
  notifications?: SemesterNotification[]
  loading?: boolean
  actionLoading?: boolean
  errorMessage?: string
}>(), { notifications: () => [], loading: false, actionLoading: false, errorMessage: '' })

const emit = defineEmits<{ dispatch: []; retry: [] }>()
const statusLabels: Record<SemesterNotificationStatus, string> = {
  PENDING: 'Đang chờ gửi',
  SENT: 'Gửi thành công',
  FAILED: 'Gửi thất bại',
}
</script>

<template>
  <section class="report-block" aria-labelledby="semester-email-heading">
    <div class="section-heading">
      <div><h3 id="semester-email-heading">Thông báo qua email</h3><p class="section-caption">Kết quả chỉ xác nhận hệ thống gửi email thành công.</p></div>
      <div class="form-actions">
        <Button label="Gửi email nhắc điểm" icon="pi pi-send" :loading="props.actionLoading" :disabled="props.actionLoading" @click="emit('dispatch')" />
        <Button v-if="props.notifications.some((item) => item.status === 'FAILED')" label="Thử gửi lại email lỗi" icon="pi pi-refresh" severity="secondary" outlined :loading="props.actionLoading" :disabled="props.actionLoading" @click="emit('retry')" />
      </div>
    </div>
    <p v-if="props.errorMessage" class="form-alert form-alert-error" role="alert">{{ props.errorMessage }}</p>
    <div v-if="props.loading" class="page-state page-state-loading"><i class="pi pi-spin pi-spinner" aria-hidden="true" /><span>Đang tải lịch sử gửi email...</span></div>
    <div v-else-if="!props.notifications.length" class="empty-state"><i class="pi pi-envelope" aria-hidden="true" /><span>Chưa có lịch sử gửi email.</span></div>
    <ul v-else class="notification-list">
      <li v-for="notification in props.notifications" :key="notification.id" class="notification-item">
        <div><strong>{{ notification.recipientEmail }}</strong><span>{{ notification.subject }}</span></div>
        <div class="notification-meta"><span>{{ statusLabels[notification.status] }}</span><span v-if="notification.errorMessage">{{ notification.errorMessage }}</span></div>
      </li>
    </ul>
  </section>
</template>
