<script setup lang="ts">
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Tag from 'primevue/tag'

import type { CalculationTaskStatus, ResCalculationTaskDTO } from '@/types/calculationTask'
import { formatCalculationDateTime } from '@/utils/calculationTaskDate'

const props = withDefaults(
  defineProps<{
    visible: boolean
    task: ResCalculationTaskDTO | null
    canRetry?: boolean
    retrying?: boolean
  }>(),
  {
    canRetry: true,
    retrying: false,
  },
)

const emit = defineEmits<{
  'update:visible': [value: boolean]
  retry: [task: ResCalculationTaskDTO]
  close: []
}>()

function statusSeverity(status?: CalculationTaskStatus): 'danger' | 'warn' | 'info' | 'success' | 'secondary' {
  switch (status) {
    case 'FAILED':
      return 'danger'
    case 'PENDING':
      return 'warn'
    case 'RUNNING':
      return 'info'
    case 'SUCCEEDED':
      return 'success'
    default:
      return 'secondary'
  }
}

function handleClose(): void {
  emit('update:visible', false)
  emit('close')
}

function handleRetry(): void {
  if (props.task) {
    emit('retry', props.task)
  }
}
</script>

<template>
  <Dialog
    :visible="props.visible"
    modal
    :header="props.task ? `Chi tiết Task #CT-${props.task.taskId}` : 'Chi tiết Task'"
    :style="{ width: '560px', maxWidth: '95vw' }"
    @update:visible="emit('update:visible', $event)"
  >
    <div v-if="props.task" class="task-detail-content">
      <div class="task-detail-header">
        <span class="student-label">{{ props.task.studentCode }}</span>
        <span class="dot-separator">·</span>
        <span class="type-label">{{ props.task.taskType }}</span>
      </div>

      <dl class="detail-grid">
        <div class="detail-item">
          <dt class="label">Trạng thái</dt>
          <dd class="value">
            <Tag :value="props.task.status" :severity="statusSeverity(props.task.status)" />
          </dd>
        </div>

        <div class="detail-item">
          <dt class="label">Số lần thử</dt>
          <dd class="value">{{ props.task.attemptCount }} / {{ props.task.maxAttempts }}</dd>
        </div>

        <div class="detail-item">
          <dt class="label">Requested version</dt>
          <dd class="value">{{ props.task.requestedVersion }}</dd>
        </div>

        <div class="detail-item">
          <dt class="label">Worker ID</dt>
          <dd class="value">{{ props.task.workerId || '—' }}</dd>
        </div>

        <div class="detail-item">
          <dt class="label">Thời điểm tạo</dt>
          <dd class="value">{{ formatCalculationDateTime(props.task.createdAt) }}</dd>
        </div>

        <div class="detail-item">
          <dt class="label">Thời điểm bắt đầu</dt>
          <dd class="value">{{ formatCalculationDateTime(props.task.startedAt) }}</dd>
        </div>

        <div class="detail-item">
          <dt class="label">Thời điểm hoàn thành</dt>
          <dd class="value">{{ formatCalculationDateTime(props.task.completedAt) }}</dd>
        </div>

        <div class="detail-item">
          <dt class="label">Thời điểm khả dụng</dt>
          <dd class="value">{{ formatCalculationDateTime(props.task.availableAt) }}</dd>
        </div>

        <div v-if="props.task.lastError" class="detail-item full-width error-box">
          <dt class="label error-label">Lỗi gần nhất</dt>
          <dd class="value error-value">{{ props.task.lastError }}</dd>
        </div>

        <div class="detail-item full-width version-note">
          <dt class="label">Bảo vệ phiên bản & xử lý</dt>
          <dd class="value text-muted">
            Khi retry, task sẽ được đưa về <strong>PENDING</strong>; worker nền sẽ nhận và tính lại dựa trên snapshot điểm mới nhất.
          </dd>
        </div>
      </dl>
    </div>

    <template #footer>
      <div class="modal-footer">
        <Button label="Đóng" severity="secondary" @click="handleClose" />
        <Button
          v-if="props.task && props.task.status === 'FAILED' && props.canRetry"
          label="Retry task"
          severity="danger"
          icon="pi pi-replay"
          :loading="props.retrying"
          data-testid="modal-retry-button"
          @click="handleRetry"
        />
      </div>
    </template>
  </Dialog>
</template>

<style scoped>
.task-detail-content {
  color: #182230;
}

.task-detail-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  color: #667085;
  font-size: 14px;
}

.student-label {
  font-weight: 700;
  color: #176b87;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin: 0;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item.full-width {
  grid-column: 1 / -1;
}

.label {
  font-size: 12px;
  font-weight: 700;
  color: #475467;
}

.value {
  margin: 0;
  font-size: 14px;
}

.error-box {
  background: #ffefed;
  border: 1px solid #f5b8b2;
  border-radius: 8px;
  padding: 10px 12px;
}

.error-label {
  color: #b42318;
}

.error-value {
  color: #8f1d14;
  word-break: break-word;
}

.version-note {
  background: #f8fafb;
  border: 1px solid #dfe4ea;
  border-radius: 8px;
  padding: 10px 12px;
}

.text-muted {
  color: #667085;
  font-size: 13px;
  line-height: 1.4;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  width: 100%;
}
</style>
