<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'

import FormAlert from '@/components/common/FormAlert.vue'
import type { TimetableDetail } from '@/types/timetable'

const props = defineProps<{
  visible: boolean
  timetable: TimetableDetail | null
  blockingCount: number
  warningCount: number
  loading?: boolean
  errorMessage?: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (
    e: 'publish',
    payload: {
      expectedVersion: number
      expectedHeadVersion: number
      idempotencyKey: string
    },
  ): void
}>()

const idempotencyKey = ref('')

watch(
  () => props.visible,
  (visible) => {
    if (visible && !idempotencyKey.value) {
      idempotencyKey.value = `pub-${props.timetable?.revisionId}-${Date.now()}`
    }
  },
)

const canPublish = computed(() => {
  if (!props.timetable) return false
  if (!['DRAFT', 'VALIDATED'].includes(props.timetable.status)) return false
  const caps = props.timetable.capabilities as unknown
  const allowed = Array.isArray(caps)
    ? caps.includes('PUBLISH') || caps.includes('CAN_PUBLISH')
    : (props.timetable.capabilities?.canPublish ?? false)
  return props.blockingCount === 0 && allowed
})

const lockReasons = computed(() => {
  if (!props.timetable) return ['Chưa tải được thông tin thời khóa biểu.']
  const reasons: string[] = []
  if (!['DRAFT', 'VALIDATED'].includes(props.timetable.status)) {
    reasons.push('Chỉ bản nháp đã kiểm tra mới có thể công bố.')
  }
  const caps = props.timetable.capabilities as unknown
  const allowed = Array.isArray(caps)
    ? caps.includes('PUBLISH') || caps.includes('CAN_PUBLISH')
    : (props.timetable.capabilities?.canPublish ?? false)
  if (!allowed) reasons.push('Tài khoản hiện không có quyền công bố.')
  if (props.blockingCount > 0) reasons.push(`Còn ${props.blockingCount} lỗi chặn trong toàn bộ lịch.`)
  if (props.timetable.status === 'PUBLISHED' || props.timetable.status === 'ARCHIVED') {
    reasons.push('Bản lịch này đã khóa sau khi công bố; hãy tạo bản điều chỉnh.')
  }
  return reasons
})

function handleConfirm() {
  if (!canPublish.value || !props.timetable) return
  emit('publish', {
    expectedVersion: props.timetable.version,
    expectedHeadVersion: props.timetable.headVersion,
    idempotencyKey: idempotencyKey.value,
  })
}
</script>

<template>
  <Dialog
    class="timetable-dialog"
    :visible="visible"
    header="Công bố thời khóa biểu"
    modal
    :style="{ width: '500px' }"
    @update:visible="emit('update:visible', $event)"
  >
    <div class="flex flex-col gap-4">
      <FormAlert v-if="errorMessage" :message="errorMessage" type="error" />

      <div v-if="!canPublish" class="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-800" aria-live="polite">
        <p class="font-semibold mb-1">⛔ Chưa thể công bố</p>
        <ul class="list-disc pl-5 space-y-1">
          <li v-for="reason in lockReasons" :key="reason">{{ reason }}</li>
        </ul>
      </div>

      <div v-else class="p-3 bg-blue-50 border border-blue-200 rounded-lg text-xs text-blue-800">
        <p class="font-semibold mb-1">ℹ️ Xác nhận công bố</p>
        <p>Bản {{ timetable?.revisionNumber }} sẽ có hiệu lực từ <strong>{{ timetable?.effectiveFrom }}</strong>. Sau khi công bố, bản này sẽ chỉ đọc; mọi thay đổi tiếp theo cần tạo bản điều chỉnh.</p>
      </div>

      <div class="bg-gray-50 p-3 rounded-lg text-xs flex flex-col gap-1 border border-gray-200 text-gray-700">
        <div><strong>Học kỳ:</strong> {{ timetable?.semesterName }}</div>
        <div><strong>Phiên bản:</strong> Bản số {{ timetable?.revisionNumber }}</div>
        <div><strong>Ngày áp dụng:</strong> {{ timetable?.effectiveFrom }}</div>
        <div><strong>Số cảnh báo:</strong> {{ warningCount }} cảnh báo</div>
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-2 mt-4">
        <Button label="Hủy" severity="secondary" text @click="emit('update:visible', false)" />
        <Button
          label="Xác nhận công bố"
          severity="primary"
          :disabled="!canPublish"
          :loading="loading"
          @click="handleConfirm"
        />
      </div>
    </template>
  </Dialog>
</template>
