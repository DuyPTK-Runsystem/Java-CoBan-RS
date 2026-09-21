<script setup lang="ts">
import { computed } from 'vue'
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
  SENT: 'Đã chuyển gửi',
  FAILED: 'Gửi thất bại',
}
const deliveryOutcome = computed(() => {
  if (!props.notifications.length) return null

  const sentCount = props.notifications.filter((item) => item.status === 'SENT').length
  const failedCount = props.notifications.filter((item) => item.status === 'FAILED').length
  const pendingCount = props.notifications.filter((item) => item.status === 'PENDING').length
  if (failedCount === 0 && pendingCount === 0) {
    return {
      tone: 'success',
      message: `Đã chuyển gửi thành công đến ${sentCount} người nhận.`,
    }
  }
  if (failedCount > 0 && sentCount === 0 && pendingCount === 0) {
    return {
      tone: 'error',
      message: `Không chuyển gửi được đến ${failedCount} người nhận. Xem lỗi chi tiết theo từng người nhận bên dưới.`,
    }
  }
  const pendingMessage = pendingCount > 0 ? `, ${pendingCount} đang chờ xử lý` : ''
  return {
    tone: 'warning',
    message: `Đã chuyển gửi ${sentCount} người nhận, ${failedCount} thất bại${pendingMessage}. Xem trạng thái chi tiết bên dưới.`,
  }
})
</script>

<template>
  <section class="report-block" aria-labelledby="semester-email-heading">
    <div class="section-heading">
      <div><h3 id="semester-email-heading">Thông báo qua email</h3><p class="section-caption">Kết quả được xác định theo trạng thái của từng người nhận.</p></div>
      <div class="form-actions">
        <Button label="Gửi email nhắc điểm" icon="pi pi-send" :loading="props.actionLoading" :disabled="props.actionLoading" @click="emit('dispatch')" />
        <Button v-if="props.notifications.some((item) => item.status === 'FAILED')" label="Thử gửi lại email lỗi" icon="pi pi-refresh" severity="secondary" outlined :loading="props.actionLoading" :disabled="props.actionLoading" @click="emit('retry')" />
      </div>
    </div>
    <p v-if="props.errorMessage" class="form-alert form-alert-error" role="alert">{{ props.errorMessage }}</p>
    <p v-if="deliveryOutcome" :class="['form-alert', `form-alert-${deliveryOutcome.tone}`]" role="status" aria-live="polite">{{ deliveryOutcome.message }}</p>
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
