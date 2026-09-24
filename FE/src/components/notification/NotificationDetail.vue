<script setup lang="ts">
import { computed } from 'vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import NotificationStatusBadge from './NotificationStatusBadge.vue'
import type { NotificationAudienceType, NotificationChannel, NotificationItem } from '@/types/notification'

const props = withDefaults(
  defineProps<{
    notification: NotificationItem
    loading?: boolean
    canManage?: boolean
    submitting?: boolean
  }>(),
  {
    loading: false,
    canManage: false,
    submitting: false,
  },
)

const emit = defineEmits<{
  (e: 'back'): void
  (e: 'markRead'): void
  (e: 'publish'): void
  (e: 'cancel'): void
}>()

const audienceLabels: Record<NotificationAudienceType, string> = {
  INDIVIDUAL: 'Cá nhân',
  CLASS: 'Lớp học',
  SCHOOL: 'Toàn trường',
}

const channelLabels: Record<NotificationChannel, string> = {
  IN_APP: 'Trên website',
  EMAIL: 'Email',
}

const audienceDetails = computed(() => props.notification.audienceDetails ?? null)

const audienceTargetValue = computed(() => {
  const displayLabel = audienceDetails.value?.displayLabel?.trim()
  if (displayLabel) return displayLabel

  const audienceType = audienceDetails.value?.audienceType ?? props.notification.audienceType
  return audienceLabels[audienceType] ?? 'Đối tượng nhận'
})

const recipientCountLabel = computed(() => {
  const count = audienceDetails.value?.recipientCount
  if (!Number.isSafeInteger(count) || count < 0) return ''
  return `${new Intl.NumberFormat('vi-VN').format(count)} người nhận`
})

const recipientDisplayNames = computed(() => (
  (audienceDetails.value?.recipientDisplayNames ?? [])
    .filter((name): name is string => typeof name === 'string')
    .map((name) => name.trim())
    .filter(Boolean)
))

const audienceMetadataHint = computed(() => {
  const details = audienceDetails.value
  if (!details) return 'Chưa có thông tin chi tiết về đối tượng nhận.'
  if (details.displayDataAvailable === false) {
    return 'Thông tin người nhận chưa được cung cấp đầy đủ.'
  }
  if (!details.displayLabel?.trim() && !recipientCountLabel.value && !recipientDisplayNames.value.length) {
    return 'Chưa có thông tin chi tiết về đối tượng nhận.'
  }
  return ''
})

function formatDate(isoStr?: string | null): string {
  if (!isoStr) return '—'
  const d = new Date(isoStr)
  return d.toLocaleString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

const readStatusValue = computed(() => {
  if (props.notification.readAt) {
    return `Đã đọc lúc ${formatDate(props.notification.readAt)}`
  }
  if (props.notification.read === false) {
    return 'Chưa đọc'
  }
  return 'Chưa xác định'
})

const hasRecipientReadState = computed(() => (
  props.notification.read !== null && props.notification.read !== undefined
) || Boolean(props.notification.readAt))
</script>

<template>
  <article data-testid="notification-detail" class="content-surface notification-detail-card">
    <div class="notification-detail-toolbar">
      <Button
        label="Quay lại"
        icon="pi pi-arrow-left"
        text
        size="small"
        @click="emit('back')"
      />
      <div class="notification-detail-actions">
        <Button
          v-if="props.notification.read === false"
          label="Đánh dấu đã đọc"
          icon="pi pi-check"
          size="small"
          :loading="props.submitting"
          @click="emit('markRead')"
        />
        <Button
          v-if="props.canManage && props.notification.status === 'DRAFT'"
          label="Xuất bản"
          icon="pi pi-send"
          size="small"
          severity="success"
          :loading="props.submitting"
          @click="emit('publish')"
        />
        <Button
          v-if="props.canManage && props.notification.status === 'DRAFT'"
          label="Hủy thông báo"
          icon="pi pi-times"
          size="small"
          severity="danger"
          text
          :loading="props.submitting"
          @click="emit('cancel')"
        />
      </div>
    </div>

    <div class="notification-detail-heading">
      <div class="notification-detail-tags">
        <NotificationStatusBadge :status="props.notification.status" />
        <Tag
          :value="audienceLabels[props.notification.audienceType] ?? props.notification.audienceType"
          severity="info"
        />
        <Tag :value="channelLabels[props.notification.channel] ?? props.notification.channel" severity="secondary" />
        <Tag
          v-if="props.notification.read !== null && props.notification.read !== undefined"
          :value="props.notification.read ? 'Đã đọc' : 'Chưa đọc'"
          :severity="props.notification.read ? 'success' : 'warn'"
        />
      </div>
      <h2>
        {{ props.notification.title }}
      </h2>
    </div>

    <div class="notification-detail-meta">
      <div>
        <span>Thời gian phát hành</span>
        <strong>
          {{ formatDate(props.notification.publishAt) }}
        </strong>
      </div>
      <div>
        <span>Đối tượng nhận</span>
        <strong>{{ audienceTargetValue }}</strong>
        <small v-if="recipientCountLabel" class="notification-meta-hint">
          {{ recipientCountLabel }}
        </small>
        <small v-if="recipientDisplayNames.length" class="notification-meta-hint">
          Người nhận: {{ recipientDisplayNames.join(', ') }}
        </small>
        <small v-if="audienceMetadataHint" class="notification-meta-hint">
          {{ audienceMetadataHint }}
        </small>
      </div>
      <div v-if="!props.canManage && hasRecipientReadState">
        <span>Trạng thái của bạn</span>
        <strong>{{ readStatusValue }}</strong>
      </div>
    </div>

    <div class="notification-detail-body">{{ props.notification.body }}</div>
  </article>
</template>

<style scoped>
.notification-detail-card {
  display: grid;
  gap: 24px;
  margin-bottom: 0;
}

.notification-detail-toolbar,
.notification-detail-actions,
.notification-detail-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.notification-detail-toolbar {
  justify-content: space-between;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.notification-detail-heading h2 {
  margin: 10px 0 0;
  color: #1e293b;
  font-size: 24px;
  line-height: 32px;
}

.notification-detail-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  padding: 16px;
  border-radius: 8px;
  background: #f8fafc;
  font-size: 14px;
}

.notification-detail-meta div {
  display: grid;
  gap: 5px;
}

.notification-detail-meta span {
  color: #64748b;
  font-size: 12px;
}

.notification-detail-meta strong {
  color: #334155;
  font-weight: 600;
}

.notification-meta-hint {
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.notification-detail-body {
  color: #334155;
  font-size: 16px;
  line-height: 1.65;
  white-space: pre-wrap;
}

@media (max-width: 900px) {
  .notification-detail-meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .notification-detail-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .notification-detail-actions {
    justify-content: flex-start;
  }

  .notification-detail-meta {
    grid-template-columns: 1fr;
  }
}
</style>
