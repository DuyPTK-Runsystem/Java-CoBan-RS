<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import FormAlert from '@/components/common/FormAlert.vue'
import NotificationComposer from '@/components/notification/NotificationComposer.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { createNotificationDraft } from '@/services/notificationApi'
import type { ReqCreateNotificationDTO } from '@/types/notification'

const router = useRouter()
const { requireAccessToken } = useAuthSession()

const loading = ref(false)
const errorMessage = ref('')

async function handleSubmit(payload: ReqCreateNotificationDTO): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    const token = requireAccessToken()
    const created = await createNotificationDraft(token, payload)
    router.push(`/v2/notifications/${created.id}`)
  } catch (err: unknown) {
    errorMessage.value = err instanceof Error ? err.message : 'Không thể lưu bản nháp thông báo'
  } finally {
    loading.value = false
  }
}

function handleCancel(): void {
  router.push('/v2/notifications/manage')
}
</script>

<template>
  <div class="notification-view" data-testid="notification-composer-view">
    <header class="page-heading">
      <div>
        <p class="eyebrow">THÔNG BÁO</p>
        <h1>Soạn thông báo</h1>
        <p>Tạo bản nháp trong phạm vi được backend cho phép.</p>
      </div>
    </header>

    <FormAlert v-if="errorMessage" tone="error" :message="errorMessage" />

    <NotificationComposer
      :loading="loading"
      @submit="handleSubmit"
      @cancel="handleCancel"
    />
  </div>
</template>
