<script setup lang="ts">
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'

import type { ResCalculationTaskDTO } from '@/types/calculationTask'

const props = withDefaults(
  defineProps<{
    visible: boolean
    mode?: 'single' | 'bulk'
    task?: ResCalculationTaskDTO | null
    failedCount?: number
    loading?: boolean
  }>(),
  {
    mode: 'single',
    task: null,
    failedCount: 0,
    loading: false,
  },
)

const emit = defineEmits<{
  'update:visible': [value: boolean]
  confirm: []
  cancel: []
}>()

function handleCancel(): void {
  emit('update:visible', false)
  emit('cancel')
}

function handleConfirm(): void {
  emit('confirm')
}
</script>

<template>
  <Dialog
    :visible="props.visible"
    modal
    :header="props.mode === 'bulk' ? 'Retry tất cả failed?' : 'Xác nhận retry task?'"
    :style="{ width: '480px', maxWidth: '95vw' }"
    @update:visible="emit('update:visible', $event)"
  >
    <div class="confirmation-body">
      <div v-if="props.mode === 'single' && props.task" class="target-summary">
        <strong>Task #CT-{{ props.task.taskId }}</strong> ·
        <span>{{ props.task.studentCode }}</span> ·
        <span class="status-failed">FAILED</span>
      </div>
      <div v-else-if="props.mode === 'bulk'" class="target-summary">
        Phạm vi hiện tại: <strong>{{ props.failedCount }}</strong> task FAILED
      </div>

      <div class="notice-box notice-warning">
        <div class="notice-title">
          <i class="pi pi-exclamation-triangle" />
          <span>{{ props.mode === 'bulk' ? 'Mutation hàng loạt' : 'Mutation bất đồng bộ' }}</span>
        </div>
        <p class="notice-text">
          <template v-if="props.mode === 'bulk'">
            Backend sẽ đưa toàn bộ các task FAILED trong phạm vi command về <strong>PENDING</strong> và ghi audit event theo contract.
          </template>
          <template v-else>
            Task sẽ được đưa về trạng thái <strong>PENDING</strong>; worker nền sẽ xử lý bất đồng bộ. UI sẽ tự động làm mới trạng thái sau khi gửi yêu cầu.
          </template>
        </p>
      </div>

      <p class="idempotency-note">
        Thao tác retry có tính chất idempotent theo backend contract. Nếu hệ thống phản hồi lỗi <strong>409 (Conflict)</strong>, task đã được đổi trạng thái; UI sẽ tải lại dữ liệu mới nhất.
      </p>
    </div>

    <template #footer>
      <div class="modal-footer">
        <Button
          label="Hủy"
          severity="secondary"
          :disabled="props.loading"
          @click="handleCancel"
        />
        <Button
          :label="props.mode === 'bulk' ? `Retry ${props.failedCount} tasks` : 'Retry task'"
          severity="danger"
          icon="pi pi-replay"
          :loading="props.loading"
          data-testid="confirm-retry-btn"
          @click="handleConfirm"
        />
      </div>
    </template>
  </Dialog>
</template>

<style scoped>
.confirmation-body {
  color: #182230;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.target-summary {
  font-size: 14px;
  color: #475467;
}

.status-failed {
  color: #b42318;
  font-weight: 700;
}

.notice-box {
  border-radius: 8px;
  padding: 12px 14px;
}

.notice-warning {
  background: #fff7d6;
  border: 1px solid #efd98d;
  color: #6f4a00;
}

.notice-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 13px;
  margin-bottom: 4px;
}

.notice-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.45;
}

.idempotency-note {
  margin: 0;
  color: #667085;
  font-size: 12px;
  line-height: 1.45;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  width: 100%;
}
</style>
