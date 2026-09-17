<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FormAlert from '@/components/common/FormAlert.vue'
import NotificationDetail from '@/components/notification/NotificationDetail.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import {
  cancelNotification,
  fetchNotification,
  markNotificationRead,
  publishNotification,
} from '@/services/notificationApi'
import type { NotificationItem } from '@/types/notification'
import { extractApiErrorMessage, isApiError } from '@/types/api'
import Button from 'primevue/button'

const route = useRoute()
const router = useRouter()
const { requireAccessToken, roles, session } = useAuthSession()

const notificationId = Number(route.params.notificationId)
const notification = ref<NotificationItem | null>(null)
const loading = ref(true)
const submitting = ref(false)
const errorMessage = ref('')

const canManage = ref(roles.value.includes('ADMIN') || roles.value.includes('ACADEMIC_OFFICE'))
const conflictRequiresReload = ref(false)

async function loadDetail(): Promise<void> {
  if (!notificationId || isNaN(notificationId)) {
    errorMessage.value = 'Mã thông báo không hợp lệ'
    loading.value = false
    return
  }

  loading.value = true
  errorMessage.value = ''
  conflictRequiresReload.value = false
  try {
    const token = requireAccessToken()
    if (!token) return
    const loadedNotification = await fetchNotification(token, notificationId)
    notification.value = loadedNotification
    canManage.value = roles.value.includes('ADMIN')
      || roles.value.includes('ACADEMIC_OFFICE')
      || loadedNotification.senderId === session.value?.user.id
  } catch (err: unknown) {
    if (isApiError(err, 401)) return
    errorMessage.value = isApiError(err, 403)
      ? 'Bạn không có quyền truy cập thông báo này.'
      : extractApiErrorMessage(err, 'Không thể tải chi tiết thông báo')
  } finally {
    loading.value = false
  }
}

async function handleMarkRead(): Promise<void> {
  if (!notification.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    const token = requireAccessToken()
    if (!token) return
    await markNotificationRead(token, notification.value.id)
    notification.value.read = true
    notification.value.readAt = new Date().toISOString()
  } catch (err: unknown) {
    if (isApiError(err, 401)) return
    errorMessage.value = isApiError(err, 403)
      ? 'Bạn không có quyền đánh dấu thông báo này.'
      : extractApiErrorMessage(err, 'Không thể đánh dấu đã đọc')
  } finally {
    submitting.value = false
  }
}

async function handlePublish(): Promise<void> {
  if (!notification.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    const token = requireAccessToken()
    if (!token) return
    const updated = await publishNotification(token, notification.value.id, {
      expectedVersion: notification.value.version,
    })
    notification.value = updated
  } catch (err: unknown) {
    if (isApiError(err, 401)) return
    conflictRequiresReload.value = isApiError(err, 409)
    errorMessage.value = isApiError(err, 409)
      ? 'Thông báo đã thay đổi. Hãy tải lại dữ liệu trước khi xuất bản.'
      : isApiError(err, 403)
        ? 'Bạn không có quyền xuất bản thông báo này.'
        : extractApiErrorMessage(err, 'Xuất bản thông báo thất bại')
  } finally {
    submitting.value = false
  }
}

async function handleCancel(): Promise<void> {
  if (!notification.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    const token = requireAccessToken()
    if (!token) return
    const updated = await cancelNotification(token, notification.value.id)
    notification.value = updated
  } catch (err: unknown) {
    if (isApiError(err, 401)) return
    conflictRequiresReload.value = isApiError(err, 409)
    errorMessage.value = isApiError(err, 409)
      ? 'Thông báo đã thay đổi. Hãy tải lại dữ liệu trước khi hủy.'
      : isApiError(err, 403)
        ? 'Bạn không có quyền hủy thông báo này.'
        : extractApiErrorMessage(err, 'Hủy thông báo thất bại')
  } finally {
    submitting.value = false
  }
}

function handleBack(): void {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push({ name: 'v2-notifications-inbox' })
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<template>
  <div class="notification-view" data-testid="notification-detail-view">
    <FormAlert v-if="errorMessage" tone="error" :message="errorMessage" />
    <div v-if="conflictRequiresReload" class="notification-conflict-action">
      <Button label="Tải lại dữ liệu" size="small" severity="secondary" outlined @click="loadDetail" />
    </div>

    <div v-if="loading" class="content-surface page-state page-state-loading" role="status" aria-live="polite">
      <i class="pi pi-spin pi-spinner" aria-hidden="true" />
      <span>Đang tải chi tiết thông báo...</span>
    </div>

    <NotificationDetail
      v-else-if="notification"
      :notification="notification"
      :can-manage="canManage"
      :submitting="submitting"
      @back="handleBack"
      @mark-read="handleMarkRead"
      @publish="handlePublish"
      @cancel="handleCancel"
    />

    <div v-else-if="!loading && !errorMessage" class="content-surface empty-state">
      <i class="pi pi-bell" aria-hidden="true" />
      <p>Không tìm thấy thông tin thông báo này.</p>
    </div>
  </div>
</template>

<style scoped>
.notification-conflict-action {
  display: flex;
  justify-content: flex-end;
  margin-top: -12px;
}
</style>
