<script setup lang="ts">
import { computed } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'

import EmptyState from '@/components/EmptyState.vue'
import ServerPagination from '@/components/ServerPagination.vue'
import type { CalculationTaskStatus, ResCalculationTaskDTO } from '@/types/calculationTask'
import { formatCalculationDateTime, formatShortDateTime } from '@/utils/calculationTaskDate'

const props = withDefaults(
  defineProps<{
    tasks: ResCalculationTaskDTO[]
    loading?: boolean
    canRetry?: boolean
    retryingTaskId?: number | null
    page?: number
    size?: number
    totalElements?: number
    totalPages?: number
    statusFilter?: CalculationTaskStatus | ''
    studentCodeFilter?: string
  }>(),
  {
    loading: false,
    canRetry: true,
    retryingTaskId: null,
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    statusFilter: 'FAILED',
    studentCodeFilter: '',
  },
)

const emit = defineEmits<{
  'view-detail': [task: ResCalculationTaskDTO]
  retry: [task: ResCalculationTaskDTO]
  'retry-all-failed': []
  refresh: []
  'page-change': [page: number, size: number]
  'update:statusFilter': [status: CalculationTaskStatus | '']
  'update:studentCodeFilter': [code: string]
}>()

const statusOptions: Array<{ label: string; value: CalculationTaskStatus | '' }> = [
  { label: 'FAILED', value: 'FAILED' },
  { label: 'Tất cả trạng thái', value: '' },
  { label: 'PENDING', value: 'PENDING' },
  { label: 'RUNNING', value: 'RUNNING' },
  { label: 'SUCCEEDED', value: 'SUCCEEDED' },
]

const hasFailedTasks = computed(() =>
  props.tasks.some((t) => t.status === 'FAILED'),
)

function statusSeverity(status: CalculationTaskStatus): 'danger' | 'warn' | 'info' | 'success' | 'secondary' {
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
</script>

<template>
  <div class="calculation-task-table-wrapper">
    <div class="table-toolbar">
      <div class="toolbar-filters">
        <div class="filter-field">
          <label for="filter-task-status" class="filter-label">Trạng thái</label>
          <Select
            id="filter-task-status"
            :model-value="props.statusFilter"
            :options="statusOptions"
            option-label="label"
            option-value="value"
            class="filter-select"
            @update:model-value="emit('update:statusFilter', $event)"
          />
        </div>

        <div class="filter-field">
          <label for="filter-task-student" class="filter-label">Mã học sinh</label>
          <InputText
            id="filter-task-student"
            :model-value="props.studentCodeFilter"
            placeholder="Ví dụ: HS0001"
            class="filter-input"
            @update:model-value="emit('update:studentCodeFilter', $event ?? '')"
          />
        </div>
      </div>

      <div class="toolbar-actions">
        <Button
          label="Làm mới"
          icon="pi pi-refresh"
          severity="secondary"
          :loading="props.loading"
          @click="emit('refresh')"
        />
        <Button
          label="Retry tất cả failed"
          icon="pi pi-replay"
          severity="danger"
          :disabled="!props.canRetry || !hasFailedTasks || props.loading"
          data-testid="bulk-retry-button"
          @click="emit('retry-all-failed')"
        />
      </div>
    </div>

    <div v-if="props.tasks.length === 0 && !props.loading" class="empty-container">
      <EmptyState
        heading="Không có calculation task phù hợp"
        message="Thử thay đổi bộ lọc trạng thái hoặc tải lại dữ liệu."
      />
    </div>

    <div v-else class="table-responsive">
      <table class="data-table" role="table" aria-label="Bảng danh sách calculation task">
        <thead>
          <tr>
            <th scope="col">Task ID</th>
            <th scope="col">Học sinh</th>
            <th scope="col">Loại task</th>
            <th scope="col">Trạng thái</th>
            <th scope="col">Số lần thử</th>
            <th scope="col">Lỗi gần nhất</th>
            <th scope="col">Thời gian tạo / hoàn thành</th>
            <th scope="col" class="text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="task in props.tasks" :key="task.taskId" :data-testid="`task-row-${task.taskId}`">
            <td class="cell-task-id">
              <strong>#CT-{{ task.taskId }}</strong>
            </td>
            <td>
              <span class="student-code">{{ task.studentCode }}</span>
            </td>
            <td>
              <span class="task-type">{{ task.taskType }}</span>
            </td>
            <td>
              <Tag :value="task.status" :severity="statusSeverity(task.status)" />
            </td>
            <td>{{ task.attemptCount }} / {{ task.maxAttempts }}</td>
            <td class="cell-error">
              <span v-if="task.lastError" class="error-text" :title="task.lastError">
                {{ task.lastError }}
              </span>
              <span v-else class="text-muted">—</span>
            </td>
            <td>
              <div class="timestamp-col">
                <span>{{ formatCalculationDateTime(task.createdAt) }}</span>
                <span v-if="task.completedAt" class="text-muted text-xs">
                  {{ formatShortDateTime(task.completedAt) }}
                </span>
              </div>
            </td>
            <td class="cell-actions text-right">
              <div class="action-buttons">
                <Button
                  label="Chi tiết"
                  size="small"
                  text
                  @click="emit('view-detail', task)"
                />
                <Button
                  v-if="task.status === 'FAILED' && props.canRetry"
                  label="Retry"
                  size="small"
                  severity="danger"
                  text
                  :loading="props.retryingTaskId === task.taskId"
                  :disabled="props.retryingTaskId !== null"
                  data-testid="retry-single-button"
                  @click="emit('retry', task)"
                />
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="props.totalElements > 0" class="pagination-footer">
      <div class="pagination-info">
        Hiển thị {{ props.tasks.length }} trong tổng số {{ props.totalElements }} tasks
      </div>
      <ServerPagination
        :page="props.page"
        :page-size="props.size"
        :total-records="props.totalElements"
        @page-change="(p, s) => emit('page-change', p, s)"
      />
    </div>
  </div>
</template>

<style scoped>
.calculation-task-table-wrapper {
  background: #ffffff;
  border: 1px solid #dfe4ea;
  border-radius: 12px;
  overflow: hidden;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid #dfe4ea;
  flex-wrap: wrap;
}

.toolbar-filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  flex: 1;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 180px;
}

.filter-label {
  font-size: 12px;
  font-weight: 700;
  color: #475467;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.table-responsive {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 860px;
}

.data-table th,
.data-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #e7ebef;
  font-size: 13px;
}

.data-table th {
  background: #f8fafb;
  color: #475467;
  font-weight: 700;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.cell-task-id {
  white-space: nowrap;
  color: #176b87;
}

.student-code {
  font-weight: 600;
  color: #182230;
}

.cell-error {
  max-width: 260px;
}

.error-text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #b42318;
}

.text-muted {
  color: #667085;
}

.text-xs {
  font-size: 11px;
}

.text-right {
  text-align: right;
}

.timestamp-col {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.action-buttons {
  display: inline-flex;
  gap: 4px;
  justify-content: flex-end;
}

.empty-container {
  padding: 40px 20px;
}

.pagination-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-top: 1px solid #dfe4ea;
  color: #667085;
  font-size: 13px;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 768px) {
  .table-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar-filters {
    flex-direction: column;
  }
  .toolbar-actions {
    justify-content: flex-start;
  }
}
</style>
