<script setup lang="ts">
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'

import EmptyState from '@/components/EmptyState.vue'
import ServerPagination from '@/components/ServerPagination.vue'
import type { ResScoreAuditLogDTO } from '@/types/scoreAudit'
import { formatCalculationDateTime } from '@/utils/calculationTaskDate'

const props = withDefaults(
  defineProps<{
    logs: ResScoreAuditLogDTO[]
    loading?: boolean
    page?: number
    size?: number
    totalElements?: number
    totalPages?: number
    entityTypeFilter?: string
    actionFilter?: string
    studentCodeFilter?: string
  }>(),
  {
    loading: false,
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    entityTypeFilter: '',
    actionFilter: '',
    studentCodeFilter: '',
  },
)

const emit = defineEmits<{
  refresh: []
  'page-change': [page: number, size: number]
  'update:entityTypeFilter': [entityType: string]
  'update:actionFilter': [action: string]
  'update:studentCodeFilter': [code: string]
}>()

const entityTypeOptions = [
  { label: 'Tất cả đối tượng', value: '' },
  { label: 'CALCULATION_TASK', value: 'CALCULATION_TASK' },
  { label: 'STUDENT_SCORE', value: 'STUDENT_SCORE' },
  { label: 'SCORE_CHANGE_REQUEST', value: 'SCORE_CHANGE_REQUEST' },
  { label: 'RETAKE_EXAM', value: 'RETAKE_EXAM' },
]

function formatJsonData(data: unknown): string {
  if (data === null || data === undefined) return 'null'
  if (typeof data === 'string') return data
  try {
    return JSON.stringify(data)
  } catch {
    return String(data)
  }
}
</script>

<template>
  <div class="score-audit-log-table-wrapper">
    <div class="table-toolbar">
      <div class="toolbar-filters">
        <div class="filter-field">
          <label for="filter-audit-entity" class="filter-label">Loại đối tượng</label>
          <Select
            id="filter-audit-entity"
            :model-value="props.entityTypeFilter"
            :options="entityTypeOptions"
            option-label="label"
            option-value="value"
            class="filter-select"
            @update:model-value="emit('update:entityTypeFilter', $event ?? '')"
          />
        </div>

        <div class="filter-field">
          <label for="filter-audit-action" class="filter-label">Hành động</label>
          <InputText
            id="filter-audit-action"
            :model-value="props.actionFilter"
            placeholder="Ví dụ: SCORE_UPDATED"
            class="filter-input"
            @update:model-value="emit('update:actionFilter', $event ?? '')"
          />
        </div>

        <div class="filter-field">
          <label for="filter-audit-student" class="filter-label">Mã học sinh</label>
          <InputText
            id="filter-audit-student"
            :model-value="props.studentCodeFilter"
            placeholder="Mã học sinh"
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
      </div>
    </div>

    <div class="read-only-banner">
      <i class="pi pi-shield" />
      <span>
        <strong>Read-only audit:</strong> Toàn bộ audit log là nhật ký bất biến chỉ dùng để đối soát. Không hỗ trợ sửa, xóa hoặc thao tác trực tiếp từ bảng này.
      </span>
    </div>

    <div v-if="props.logs.length === 0 && !props.loading" class="empty-container">
      <EmptyState
        heading="Không tìm thấy nhật ký audit phù hợp"
        message="Thử thay đổi bộ lọc hoặc kiểm tra lại điều kiện tìm kiếm."
      />
    </div>

    <div v-else class="table-responsive">
      <table class="data-table" role="table" aria-label="Bảng nhật ký kiểm toán điểm">
        <thead>
          <tr>
            <th scope="col">Thời điểm</th>
            <th scope="col">Người thực hiện</th>
            <th scope="col">Hành động</th>
            <th scope="col">Đối tượng</th>
            <th scope="col">Request ID</th>
            <th scope="col">Dữ liệu Trước / Sau</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in props.logs" :key="log.auditLogId" :data-testid="`audit-row-${log.auditLogId}`">
            <td class="cell-timestamp">
              {{ formatCalculationDateTime(log.occurredAt) }}
            </td>
            <td>
              <span class="actor-text">{{ log.actorUsername || (log.actorUserId ? `#${log.actorUserId}` : '—') }}</span>
              <span v-if="log.ipAddress" class="ip-text">({{ log.ipAddress }})</span>
            </td>
            <td>
              <Tag :value="log.action" severity="secondary" />
            </td>
            <td>
              <span class="entity-text">{{ log.entityType }} · #{{ log.entityId }}</span>
            </td>
            <td class="cell-request-id">
              <span class="request-code">{{ log.requestId || '—' }}</span>
            </td>
            <td class="cell-json-preview">
              <div class="json-box">
                <div><span class="json-label">before:</span> {{ formatJsonData(log.beforeData) }}</div>
                <div><span class="json-label">after:</span> {{ formatJsonData(log.afterData) }}</div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="props.totalElements > 0" class="pagination-footer">
      <div class="pagination-info">
        Hiển thị {{ props.logs.length }} trong tổng số {{ props.totalElements }} sự kiện (occurredAt DESC)
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
.score-audit-log-table-wrapper {
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
  min-width: 170px;
}

.filter-label {
  font-size: 12px;
  font-weight: 700;
  color: #475467;
}

.read-only-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
  background: #f8fafb;
  border-bottom: 1px solid #e7ebef;
  color: #475467;
  font-size: 12px;
}

.read-only-banner i {
  color: #176b87;
  font-size: 14px;
}

.table-responsive {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 900px;
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

.cell-timestamp {
  white-space: nowrap;
  color: #475467;
}

.actor-text {
  font-weight: 600;
  color: #182230;
}

.ip-text {
  font-size: 11px;
  color: #98a2b3;
  margin-left: 4px;
}

.entity-text {
  font-weight: 500;
  color: #344054;
}

.request-code {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  color: #667085;
}

.cell-json-preview {
  max-width: 340px;
}

.json-box {
  background: #f6f8fa;
  border-radius: 6px;
  padding: 6px 8px;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  line-height: 1.4;
  color: #344054;
  max-height: 80px;
  overflow: auto;
  word-break: break-all;
}

.json-label {
  font-weight: 700;
  color: #667085;
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
</style>
