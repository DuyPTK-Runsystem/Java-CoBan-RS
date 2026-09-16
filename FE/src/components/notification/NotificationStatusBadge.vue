<script setup lang="ts">
import Tag from 'primevue/tag'
import type { NotificationStatus } from '@/types/notification'

type TagSeverity = 'secondary' | 'success' | 'info' | 'warn' | 'danger' | 'contrast'

const props = defineProps<{
  status: NotificationStatus
}>()

const statusLabels: Record<NotificationStatus, string> = {
  DRAFT: 'Bản nháp',
  SCHEDULED: 'Đã lên lịch',
  PUBLISHED: 'Đã phát hành',
  CANCELLED: 'Đã hủy',
  EXPIRED: 'Đã hết hạn',
}

const statusSeverities: Record<NotificationStatus, TagSeverity> = {
  DRAFT: 'secondary',
  SCHEDULED: 'info',
  PUBLISHED: 'success',
  CANCELLED: 'danger',
  EXPIRED: 'warn',
}
</script>

<template>
  <Tag
    data-testid="notification-status-badge"
    :value="statusLabels[props.status] ?? props.status"
    :severity="statusSeverities[props.status] ?? 'secondary'"
  />
</template>

