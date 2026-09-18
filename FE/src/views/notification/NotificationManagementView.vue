<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import FormAlert from '@/components/common/FormAlert.vue'
import PageState from '@/components/common/PageState.vue'
import NotificationList from '@/components/notification/NotificationList.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { fetchManagedNotifications } from '@/services/notificationApi'
import type { NotificationItem } from '@/types/notification'
import { extractApiErrorMessage, isApiError } from '@/types/api'
import type { LoadingState } from '@/types/ui'

const router = useRouter()
const { requireAccessToken } = useAuthSession()

const items = ref<NotificationItem[]>([])
const loading = ref(false)
const currentPage = ref(0)
const pageSize = ref(20)
const totalPages = ref(1)
const totalItems = ref(0)
const errorMessage = ref('')
const forbidden = ref(false)
const pageState = computed<LoadingState>(() => {
  if (loading.value) return 'loading'
  if (errorMessage.value) return 'error'
  return totalItems.value === 0 ? 'empty' : 'success'
})

async function loadManaged(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  forbidden.value = false
  try {
    const token = requireAccessToken()
    const res = await fetchManagedNotifications(token, {
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
    errorMessage.value = isApiError(err, 403)
      ? 'Bạn không có quyền quản lý thông báo.'
      : extractApiErrorMessage(err, 'Không thể tải danh sách thông báo quản lý')
  } finally {
    loading.value = false
  }
}

function handleSelect(item: NotificationItem): void {
  router.push({ name: 'v2-notifications-detail', params: { notificationId: item.id } })
}

function goToPage(page: number): void {
  if (loading.value || page < 0 || page >= totalPages.value || page === currentPage.value) return
  currentPage.value = page
  void loadManaged()
}

onMounted(() => {
  loadManaged()
})
</script>

<template>
  <div class="notification-view notification-management-view" data-testid="notification-management-view">
    <header class="page-heading notification-management-heading">
      <div class="notification-management-heading-copy">
        <h1>Quản lý thông báo</h1>
      </div>

      <div class="page-heading-actions notification-management-actions">
        <Button
          label="Soạn thông báo mới"
          icon="pi pi-plus"
          @click="router.push({ name: 'v2-notifications-compose' })"
        />
        <Button
          icon="pi pi-refresh"
          label="Làm mới"
          severity="secondary"
          outlined
          aria-label="Làm mới danh sách thông báo"
          :loading="loading"
          @click="loadManaged"
        />
      </div>
    </header>

    <FormAlert v-if="errorMessage && !forbidden" tone="error" :message="errorMessage" />

    <section class="content-surface">
      <div class="section-heading">
        <div>
          <h2>Danh sách thông báo</h2>
        </div>
        <span v-if="totalItems > 0" class="field-hint">{{ totalItems }} thông báo</span>
      </div>

      <PageState
        :state="pageState"
        :forbidden="forbidden"
        forbidden-message="Bạn không có quyền quản lý thông báo."
        :error-message="errorMessage"
        empty-heading="Chưa có thông báo"
        empty-message="Chưa có thông báo nào trong danh sách quản lý."
        @retry="loadManaged"
      >
        <NotificationList :items="items" is-manage-view @select="handleSelect" />

        <div v-if="totalPages > 1" class="go-to-page notification-pagination">
          <Button
            label="Trang trước"
            icon="pi pi-chevron-left"
            size="small"
            severity="secondary"
            outlined
            :disabled="currentPage <= 0 || loading"
            @click="goToPage(currentPage - 1)"
          />
          <span>Trang {{ currentPage + 1 }} / {{ totalPages }}</span>
          <Button
            label="Trang sau"
            icon="pi pi-chevron-right"
            size="small"
            severity="secondary"
            outlined
            :disabled="currentPage >= totalPages - 1 || loading"
            @click="goToPage(currentPage + 1)"
          />
        </div>
      </PageState>
    </section>
  </div>
</template>

<style scoped>
.notification-management-view {
  display: grid;
  gap: 28px;
}

.notification-management-heading {
  align-items: center;
  margin-bottom: 0;
  padding-bottom: 20px;
  border-bottom: 1px solid #e2e8f0;
}

.notification-management-heading-copy {
  display: grid;
  gap: 2px;
}

.notification-management-heading-copy .eyebrow,
.notification-management-heading-copy h1,
.notification-management-heading-copy p:last-child {
  margin-top: 0;
  margin-bottom: 0;
}

.notification-management-actions {
  gap: 8px;
}

@media (max-width: 680px) {
  .notification-management-heading {
    align-items: stretch;
    gap: 16px;
  }

  .notification-management-actions {
    justify-content: flex-start;
  }
}
</style>
