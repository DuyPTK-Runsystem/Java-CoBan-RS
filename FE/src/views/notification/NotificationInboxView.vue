<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import FormAlert from '@/components/common/FormAlert.vue'
import PageState from '@/components/common/PageState.vue'
import NotificationList from '@/components/notification/NotificationList.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { fetchNotificationInbox, markNotificationRead } from '@/services/notificationApi'
import type { NotificationItem } from '@/types/notification'
import { extractApiErrorMessage, isApiError } from '@/types/api'
import type { LoadingState } from '@/types/ui'

const router = useRouter()
const { requireAccessToken, roles } = useAuthSession()

const items = ref<NotificationItem[]>([])
const loading = ref(false)
const unreadOnly = ref(false)
const currentPage = ref(0)
const pageSize = ref(20)
const totalPages = ref(1)
const totalItems = ref(0)
const errorMessage = ref('')
const pageErrorMessage = ref('')
const forbidden = ref(false)

const pageState = computed<LoadingState>(() => {
  if (loading.value) return 'loading'
  if (pageErrorMessage.value) return 'error'
  return totalItems.value === 0 ? 'empty' : 'success'
})

const canManage = computed(() => roles.value.includes('ADMIN')
  || roles.value.includes('ACADEMIC_OFFICE')
  || roles.value.includes('TEACHER'))

async function loadInbox(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  pageErrorMessage.value = ''
  forbidden.value = false
  try {
    const token = requireAccessToken()
    const res = await fetchNotificationInbox(token, {
      unreadOnly: unreadOnly.value,
      page: currentPage.value,
      pageSize: pageSize.value,
    })
    items.value = res.result
    currentPage.value = res.meta.page
    pageSize.value = res.meta.pageSize
    totalPages.value = Math.max(res.meta.totalPages, 1)
    totalItems.value = res.meta.totalItems
  } catch (err: unknown) {
    if (isApiError(err, 401)) return
    forbidden.value = isApiError(err, 403)
    pageErrorMessage.value = isApiError(err, 403)
      ? 'Bạn không có quyền xem hộp thư thông báo này.'
      : extractApiErrorMessage(err, 'Không thể tải hộp thư thông báo')
  } finally {
    loading.value = false
  }
}

async function handleMarkRead(item: NotificationItem): Promise<void> {
  try {
    const token = requireAccessToken()
    await markNotificationRead(token, item.id)
    item.read = true
    item.readAt = new Date().toISOString()
  } catch (err: unknown) {
    if (isApiError(err, 401)) return
    if (isApiError(err, 403)) {
      errorMessage.value = 'Bạn không có quyền đánh dấu thông báo này.'
      return
    }
    errorMessage.value = extractApiErrorMessage(err, 'Không thể đánh dấu đã đọc')
  }
}

function handleSelect(item: NotificationItem): void {
  router.push({ name: 'v2-notifications-detail', params: { notificationId: item.id } })
}

function goToPage(page: number): void {
  if (loading.value || page < 0 || page >= totalPages.value || page === currentPage.value) return
  currentPage.value = page
  void loadInbox()
}

watch(unreadOnly, () => {
  currentPage.value = 0
  void loadInbox()
})

onMounted(() => {
  loadInbox()
})
</script>

<template>
  <div class="notification-view" data-testid="notification-inbox-view">
    <header class="page-heading">
      <div>
        <h1>Thông báo của tôi</h1>
      </div>
      <div class="page-heading-actions">
        <Button
          v-if="canManage"
          label="Quản lý thông báo"
          icon="pi pi-cog"
          severity="secondary"
          @click="router.push({ name: 'v2-notifications-manage' })"
        />
        <Button
          icon="pi pi-refresh"
          label="Làm mới"
          severity="secondary"
          outlined
          :loading="loading"
          @click="loadInbox"
        />
      </div>
    </header>

    <FormAlert v-if="errorMessage" tone="error" :message="errorMessage" />

    <section class="content-surface">
      <div class="section-heading notification-section-heading">
        <div>
          <h2>Hộp thư thông báo</h2>
        </div>
        <div class="notification-filter" role="group" aria-label="Lọc thông báo">
          <Button label="Tất cả" :severity="!unreadOnly ? 'primary' : 'secondary'" :outlined="unreadOnly" @click="unreadOnly = false" />
          <Button label="Chưa đọc" :severity="unreadOnly ? 'primary' : 'secondary'" :outlined="!unreadOnly" @click="unreadOnly = true" />
        </div>
      </div>
      <PageState
        :state="pageState"
        :forbidden="forbidden"
        forbidden-message="Bạn không có quyền xem hộp thư thông báo này."
        :error-message="pageErrorMessage"
        empty-heading="Chưa có thông báo"
        empty-message="Hiện chưa có thông báo nào phù hợp với bộ lọc."
        @retry="loadInbox"
      >
        <NotificationList :items="items" @select="handleSelect" @mark-read="handleMarkRead" />
        <div class="notification-list-footer">
          <span v-if="totalItems > 0" class="section-caption">{{ totalItems }} thông báo</span>
          <div v-if="totalPages > 1" class="page-heading-actions notification-pagination">
            <Button label="Trang trước" icon="pi pi-chevron-left" severity="secondary" text :disabled="currentPage <= 0 || loading" @click="goToPage(currentPage - 1)" />
            <span class="section-caption">Trang {{ currentPage + 1 }} / {{ totalPages }}</span>
            <Button label="Trang sau" icon="pi pi-chevron-right" icon-pos="right" severity="secondary" text :disabled="currentPage >= totalPages - 1 || loading" @click="goToPage(currentPage + 1)" />
          </div>
        </div>
      </PageState>
    </section>
  </div>
</template>

<style scoped>
.notification-filter,
.notification-pagination {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.notification-list-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 20px;
}

@media (max-width: 680px) {
  .notification-section-heading,
  .notification-list-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .notification-filter,
  .notification-filter .p-button {
    width: 100%;
  }

  .notification-filter .p-button {
    flex: 1;
  }

  .notification-pagination {
    justify-content: space-between;
  }
}
</style>
