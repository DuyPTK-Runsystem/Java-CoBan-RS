<script setup lang="ts">
import { computed } from 'vue'
import Dropdown from 'primevue/dropdown'
import InputText from 'primevue/inputtext'
import type { NotificationAudienceType } from '@/types/notification'

const props = defineProps<{
  audienceType: NotificationAudienceType
  targetReference?: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:audienceType', value: NotificationAudienceType): void
  (e: 'update:targetReference', value: string): void
}>()

const audienceOptions = [
  { label: 'Toàn trường (SCHOOL)', value: 'SCHOOL' as const },
  { label: 'Theo lớp học (CLASS)', value: 'CLASS' as const },
  { label: 'Cá nhân (INDIVIDUAL)', value: 'INDIVIDUAL' as const },
]

const currentAudienceType = computed({
  get: () => props.audienceType,
  set: (val: NotificationAudienceType) => {
    emit('update:audienceType', val)
    if (val === 'SCHOOL') {
      emit('update:targetReference', '')
    }
  },
})

const currentTargetReference = computed({
  get: () => props.targetReference ?? '',
  set: (val: string) => emit('update:targetReference', val),
})
</script>

<template>
  <div class="notification-audience-selector" data-testid="audience-selector">
    <div class="field-group">
      <label>
        Phạm vi đối tượng nhận
      </label>
      <Dropdown
        v-model="currentAudienceType"
        :options="audienceOptions"
        option-label="label"
        option-value="value"
        placeholder="Chọn đối tượng nhận"
        :disabled="props.disabled"
      />
    </div>

    <div v-if="currentAudienceType === 'CLASS'" class="field-group">
      <label>
        Mã lớp học (Class ID)
      </label>
      <InputText
        v-model="currentTargetReference"
        placeholder="Ví dụ: 101"
        :disabled="props.disabled"
      />
      <small class="field-hint">Nhập ID của lớp nhận thông báo</small>
    </div>

    <div v-if="currentAudienceType === 'INDIVIDUAL'" class="field-group">
      <label>
        Danh sách User ID người nhận (cách nhau bởi dấu phẩy)
      </label>
      <InputText
        v-model="currentTargetReference"
        placeholder="Ví dụ: 2, 5, 10"
        :disabled="props.disabled"
      />
      <small class="field-hint">Nhập danh sách mã tài khoản người dùng nhận thông báo</small>
    </div>

    <div v-if="currentAudienceType === 'SCHOOL'" class="form-alert form-alert-info">
      Thông báo sẽ được gửi tới toàn bộ thành viên trong trường học.
    </div>
  </div>
</template>

<style scoped>
.notification-audience-selector {
  display: grid;
  gap: 18px;
}

.notification-audience-selector :deep(.p-dropdown),
.notification-audience-selector :deep(.p-inputtext) {
  width: 100%;
}

.notification-audience-selector label {
  color: #334155;
  font-size: 14px;
  font-weight: 600;
}

.notification-audience-selector small {
  display: block;
}
</style>
