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
  return props.blockingCount === 0 && (props.timetable?.capabilities.canPublish ?? false)
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
    :visible="visible"
    header="Công bố thời khóa biểu"
    modal
    :style="{ width: '500px' }"
    @update:visible="emit('update:visible', $event)"
  >
    <div class="flex flex-col gap-4">
      <FormAlert v-if="errorMessage" :message="errorMessage" type="error" />

      <div v-if="blockingCount > 0" class="p-3 bg-red-50 border border-red-200 rounded-lg text-xs text-red-800">
        <p class="font-semibold mb-1">⛔ Không thể công bố thời khóa biểu!</p>
        <p>Hiện đang có {{ blockingCount }} lỗi chặn chưa được giải quyết (trùng giáo viên, trùng lớp hoặc trùng phòng chức năng). Vui lòng điều chỉnh lịch trước khi công bố.</p>
      </div>

      <div v-else class="p-3 bg-blue-50 border border-blue-200 rounded-lg text-xs text-blue-800">
        <p class="font-semibold mb-1">ℹ️ Xác nhận công bố</p>
        <p>Thời khóa biểu bản {{ timetable?.revisionNumber }} sẽ chính thức có hiệu lực từ ngày <strong>{{ timetable?.effectiveFrom }}</strong>. Sau khi công bố, bản này sẽ trở thành Read-only.</p>
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

