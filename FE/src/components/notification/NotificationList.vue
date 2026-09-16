<script setup lang="ts">
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import NotificationStatusBadge from './NotificationStatusBadge.vue'
import type { NotificationAudienceType, NotificationItem } from '@/types/notification'

const props = withDefaults(
  defineProps<{
    items: NotificationItem[]
    loading?: boolean
    isManageView?: boolean
  }>(),
  {
    loading: false,
    isManageView: false,
  },
)

const emit = defineEmits<{
  (e: 'select', item: NotificationItem): void
  (e: 'markRead', item: NotificationItem): void
}>()

const audienceLabels: Record<NotificationAudienceType, string> = {
  INDIVIDUAL: 'Cá nhân',
  CLASS: 'Lớp học',
  SCHOOL: 'Toàn trường',
}

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
</script>

<template>
  <div data-testid="notification-list" class="notification-list">
    <div v-if="props.loading" class="page-state page-state-loading" role="status" aria-live="polite">
      <i class="pi pi-spin pi-spinner" aria-hidden="true" />
      <span>Đang tải danh sách thông báo...</span>
    </div>

    <div v-else-if="props.items.length === 0" class="empty-state">
      <i class="pi pi-bell" aria-hidden="true" />
      <p>Không có thông báo nào.</p>
    </div>

    <article
      v-for="item in props.items"
      :key="item.id"
      data-testid="notification-item"
      :data-item-id="item.id"
      class="notification-card"
      :class="{ 'notification-card-unread': item.read === false }"
      @click="emit('select', item)"
    >
      <div class="notification-card-content">
        <div class="notification-card-title-row">
          <span
            v-if="item.read === false"
            class="notification-unread-dot"
            title="Chưa đọc"
          />
          <h3 :class="{ 'notification-unread-title': item.read === false }">
            {{ item.title }}
          </h3>
          <NotificationStatusBadge v-if="props.isManageView" :status="item.status" />
          <Tag :value="audienceLabels[item.audienceType] ?? item.audienceType" severity="info" />
        </div>

        <p class="notification-card-body">{{ item.body }}</p>

        <div class="notification-card-meta">
          <span>Gửi: {{ formatDate(item.publishAt ?? item.createdAt) }}</span>
          <span v-if="item.expiresAt">Hết hạn: {{ formatDate(item.expiresAt) }}</span>
          <span v-if="item.read === true && item.readAt" class="notification-read-meta">
            Đã đọc lúc: {{ formatDate(item.readAt) }}
          </span>
        </div>
      </div>

      <div class="notification-card-actions" @click.stop>
        <Button
          v-if="item.read === false && !props.isManageView"
          label="Đánh dấu đã đọc"
          icon="pi pi-check"
          size="small"
          text
          severity="secondary"
          @click="emit('markRead', item)"
        />
        <Button
          label="Xem"
          icon="pi pi-chevron-right"
          size="small"
          text
          @click="emit('select', item)"
        />
      </div>
    </article>
  </div>
</template>

<style scoped>
.notification-list {
  display: grid;
  gap: 12px;
}

.notification-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: border-color .15s ease, box-shadow .15s ease;
}

.notification-card:hover,
.notification-card:focus-within {
  border-color: #a5b4fc;
  box-shadow: 0 2px 5px rgb(15 23 42 / 8%);
}

.notification-card-unread {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.notification-card-content {
  min-width: 0;
  flex: 1;
}

.notification-card-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}

.notification-card-title-row h3 {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: #1e293b;
  font-size: 16px;
  font-weight: 700;
  line-height: 24px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-unread-title {
  color: #1e40af!important;
}

.notification-unread-dot {
  width: 10px;
  height: 10px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: #2563eb;
}

.notification-card-body {
  display: -webkit-box;
  margin: 0 0 10px;
  overflow: hidden;
  color: #475569;
  font-size: 14px;
  line-height: 21px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.notification-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.notification-read-meta {
  color: #047857;
}

.notification-card-actions {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 8px;
}

@media (max-width: 680px) {
  .notification-card {
    align-items: stretch;
    flex-direction: column;
    gap: 12px;
  }

  .notification-card-actions {
    justify-content: flex-end;
  }
}
</style>
