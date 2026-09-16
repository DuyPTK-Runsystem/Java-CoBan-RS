<script setup lang="ts">
import { ref } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import NotificationAudienceSelector from './NotificationAudienceSelector.vue'
import { DEFAULT_SCHOOL } from '@/types/notification'
import type { NotificationAudienceType, ReqCreateNotificationDTO } from '@/types/notification'

const props = withDefaults(
  defineProps<{
    loading?: boolean
  }>(),
  {
    loading: false,
  },
)

const emit = defineEmits<{
  (e: 'submit', payload: ReqCreateNotificationDTO): void
  (e: 'cancel'): void
}>()

const title = ref('')
const body = ref('')
const audienceType = ref<NotificationAudienceType>('SCHOOL')
const targetReference = ref('')
const isPreview = ref(false)
const errorMessage = ref('')
let idempotencyKey: string | null = null
let idempotencyFingerprint: string | null = null

function createIdempotencyKey(): string {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()
  return `notification-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function handleSubmit(): void {
  errorMessage.value = ''
  const trimmedTitle = title.value.trim()
  const trimmedBody = body.value.trim()

  if (!trimmedTitle) {
    errorMessage.value = 'Tiêu đề thông báo không được để trống'
    return
  }
  if (!trimmedBody) {
    errorMessage.value = 'Nội dung thông báo không được để trống'
    return
  }
  if (audienceType.value === 'CLASS' && !targetReference.value.trim()) {
    errorMessage.value = 'Vui lòng nhập mã lớp học nhận thông báo'
    return
  }
  if (
    audienceType.value === 'CLASS'
    && (!/^\d+$/.test(targetReference.value.trim()) || Number(targetReference.value) <= 0)
  ) {
    errorMessage.value = 'Mã lớp học phải là số nguyên dương'
    return
  }
  if (audienceType.value === 'INDIVIDUAL' && !targetReference.value.trim()) {
    errorMessage.value = 'Vui lòng nhập danh sách User ID nhận thông báo'
    return
  }

  const rawRecipientIds = targetReference.value
    .split(',')
    .map((value) => value.trim())
    .filter(Boolean)
  const recipientIds = rawRecipientIds.map(Number)
  if (
    audienceType.value === 'INDIVIDUAL'
    && (recipientIds.length === 0 || recipientIds.some((id) => !Number.isSafeInteger(id) || id <= 0))
  ) {
    errorMessage.value = 'Danh sách User ID phải là các số nguyên dương, cách nhau bởi dấu phẩy'
    return
  }

  const payload: ReqCreateNotificationDTO = {
    title: trimmedTitle,
    body: trimmedBody,
    audienceType: audienceType.value,
    schoolScope: DEFAULT_SCHOOL,
  }

  if (audienceType.value === 'CLASS') {
    payload.targetReference = targetReference.value.trim()
  } else if (audienceType.value === 'INDIVIDUAL') {
    payload.recipientUserIds = [...new Set(recipientIds)]
  }

  const fingerprint = JSON.stringify(payload)
  if (fingerprint !== idempotencyFingerprint) {
    idempotencyKey = createIdempotencyKey()
    idempotencyFingerprint = fingerprint
  }
  payload.idempotencyKey = idempotencyKey ?? createIdempotencyKey()

  emit('submit', payload)
}
</script>

<template>
  <section data-testid="notification-composer" class="content-surface notification-composer-card">
    <div class="notification-composer-heading">
      <h2>
        {{ isPreview ? 'Xem trước thông báo' : 'Soạn thông báo mới' }}
      </h2>
      <Button
        :label="isPreview ? 'Chỉnh sửa' : 'Xem trước'"
        :icon="isPreview ? 'pi pi-pencil' : 'pi pi-eye'"
        size="small"
        text
        @click="isPreview = !isPreview"
      />
    </div>

    <div v-if="errorMessage" class="form-alert form-alert-error" role="alert">
      {{ errorMessage }}
    </div>

    <div v-if="isPreview" class="notification-preview">
      <div class="notification-preview-audience">Đối tượng: {{ audienceType }} ({{ targetReference || 'Toàn trường' }})</div>
      <h3>{{ title || '(Chưa có tiêu đề)' }}</h3>
      <p>{{ body || '(Chưa có nội dung)' }}</p>
    </div>

    <form v-else class="notification-form" @submit.prevent="handleSubmit">
      <div class="field-group">
        <label>
          Tiêu đề <span class="required-mark">*</span>
        </label>
        <InputText
          v-model="title"
          placeholder="Nhập tiêu đề thông báo..."
          :disabled="props.loading"
        />
      </div>

      <div class="field-group">
        <label>
          Nội dung <span class="required-mark">*</span>
        </label>
        <Textarea
          v-model="body"
          rows="6"
          placeholder="Nhập nội dung chi tiết thông báo..."
          :disabled="props.loading"
        />
      </div>

      <NotificationAudienceSelector
        v-model:audience-type="audienceType"
        v-model:target-reference="targetReference"
        :disabled="props.loading"
      />

      <div class="form-actions notification-form-actions">
        <Button
          label="Hủy bỏ"
          type="button"
          text
          severity="secondary"
          :disabled="props.loading"
          @click="emit('cancel')"
        />
        <Button
          label="Lưu bản nháp"
          type="submit"
          icon="pi pi-save"
          :loading="props.loading"
        />
      </div>
    </form>
  </section>
</template>

<style scoped>
.notification-composer-card {
  display: grid;
  gap: 20px;
  max-width: 820px;
  margin-bottom: 0;
}

.notification-composer-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.notification-composer-heading h2 {
  margin: 0;
  color: #1e293b;
  font-size: 20px;
  line-height: 28px;
}

.notification-form {
  display: grid;
  gap: 18px;
}

.notification-form label {
  color: #334155;
  font-size: 14px;
  font-weight: 600;
}

.required-mark {
  color: var(--error);
}

.notification-form .p-inputtext,
.notification-form .p-textarea {
  width: 100%;
}

.notification-preview {
  display: grid;
  gap: 16px;
  padding: 18px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
}

.notification-preview-audience {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .04em;
  text-transform: uppercase;
}

.notification-preview h3 {
  margin: 0;
  color: #1e293b;
  font-size: 24px;
  line-height: 32px;
}

.notification-preview p {
  margin: 0;
  color: #334155;
  font-size: 16px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.notification-form-actions {
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

@media (max-width: 640px) {
  .notification-composer-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .notification-form-actions {
    align-items: stretch;
    flex-direction: column-reverse;
  }
}
</style>
