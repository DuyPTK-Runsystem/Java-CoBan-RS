<script setup lang="ts">
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Select from 'primevue/select'
import Tag from 'primevue/tag'

import EmptyState from '@/components/common/EmptyState.vue'
import ServerPagination from '@/components/common/ServerPagination.vue'
import type { AcademicYear, Semester } from '@/types/academic'
import type { StudentAttendanceHistoryItem, StudentAttendanceHistorySummary } from '@/types/attendance'
import { formatStudentDate } from '@/utils/studentDate'

const props = defineProps<{
  academicYears: AcademicYear[]
  semesters: Semester[]
  selectedAcademicYearId: number | null
  selectedSemesterId: number | null
  loading: boolean
  summary: StudentAttendanceHistorySummary
  records: StudentAttendanceHistoryItem[]
  totalElements: number
  page: number
  pageSize: number
  attendanceRate: string
}>()

const emit = defineEmits<{
  (event: 'update:selectedAcademicYearId', value: number | null): void
  (event: 'update:selectedSemesterId', value: number | null): void
  (event: 'search'): void
  (event: 'pageChange', page: number, pageSize: number): void
}>()

function getAttendanceSeverity(status?: string | null): 'success' | 'danger' | 'warn' | 'info' | 'secondary' {
  if (status === 'PRESENT') return 'success'
  if (status === 'ABSENT' || status === 'UNEXCUSED_ABSENCE') return 'danger'
  if (status === 'EXCUSED' || status === 'EXCUSED_ABSENCE') return 'warn'
  if (status === 'LATE') return 'info'
  return 'secondary'
}

function getAttendanceLabel(status?: string | null): string {
  if (status === 'PRESENT') return 'Có mặt'
  if (status === 'ABSENT' || status === 'UNEXCUSED_ABSENCE') return 'Vắng không phép'
  if (status === 'EXCUSED' || status === 'EXCUSED_ABSENCE') return 'Vắng có phép'
  if (status === 'LATE') return 'Đi trễ'
  if (status === 'EARLY_LEAVE') return 'Về sớm'
  return status || '—'
}
</script>

<template>
  <div class="tab-stack">
    <section class="content-surface filter-bar">
      <div class="filter-controls">
        <div class="filter-item">
          <label for="att-year">Năm học</label>
          <Select
            id="att-year"
            :model-value="props.selectedAcademicYearId"
            :options="props.academicYears"
            option-label="code"
            option-value="id"
            placeholder="Chọn năm học"
            @update:model-value="emit('update:selectedAcademicYearId', $event)"
          />
        </div>
        <div class="filter-item">
          <label for="att-sem">Học kỳ</label>
          <Select
            id="att-sem"
            :model-value="props.selectedSemesterId"
            :options="props.semesters"
            option-label="name"
            option-value="id"
            placeholder="Chọn học kỳ"
            @update:model-value="emit('update:selectedSemesterId', $event)"
          />
        </div>
        <div class="filter-actions">
          <Button label="Tìm kiếm" icon="pi pi-search" :loading="props.loading" @click="emit('search')" />
        </div>
      </div>
    </section>

    <div class="attendance-summary-cards">
      <div class="metric-card">
        <span class="metric-label">Buổi học hợp lệ</span>
        <strong class="metric-value">{{ props.summary.validSessionCount }}</strong>
      </div>
      <div class="metric-card metric-success">
        <span class="metric-label">Có mặt (Tỷ lệ)</span>
        <strong class="metric-value">{{ props.summary.presentCount }} ({{ props.attendanceRate }})</strong>
      </div>
      <div class="metric-card metric-warn">
        <span class="metric-label">Vắng có phép</span>
        <strong class="metric-value">{{ props.summary.excusedAbsenceCount }}</strong>
      </div>
      <div class="metric-card metric-danger">
        <span class="metric-label">Vắng không phép</span>
        <strong class="metric-value">{{ props.summary.unexcusedAbsenceCount }}</strong>
      </div>
      <div class="metric-card metric-info">
        <span class="metric-label">Đi trễ</span>
        <strong class="metric-value">{{ props.summary.lateCount }}</strong>
      </div>
    </div>

    <section class="content-surface">
      <div class="card-header">
        <h2>Chi tiết các buổi điểm danh</h2>
      </div>

      <DataTable v-if="props.records.length > 0" :value="props.records" :loading="props.loading">
        <Column field="attendanceDate" header="Ngày điểm danh">
          <template #body="{ data }">{{ formatStudentDate(data.attendanceDate) }}</template>
        </Column>
        <Column field="sessionPeriod" header="Buổi học">
          <template #body="{ data }">{{ data.sessionPeriod === 'MORNING' ? 'Sáng' : 'Chiều' }}</template>
        </Column>
        <Column field="className" header="Lớp học" />
        <Column field="status" header="Trạng thái">
          <template #body="{ data }">
            <Tag :value="getAttendanceLabel(data.status)" :severity="getAttendanceSeverity(data.status)" />
          </template>
        </Column>
        <Column field="note" header="Ghi chú">
          <template #body="{ data }">{{ data.note || '—' }}</template>
        </Column>
      </DataTable>

      <div v-if="props.records.length > 0" class="table-pagination">
        <ServerPagination
          :page="props.page"
          :page-size="props.pageSize"
          :total-records="props.totalElements"
          @page-change="(page, pageSize) => emit('pageChange', page, pageSize)"
        />
      </div>

      <EmptyState
        v-else
        icon="pi pi-calendar-times"
        heading="Chưa có dữ liệu điểm danh"
        message="Không có bản ghi điểm danh nào trong khoảng thời gian đã chọn."
      />
    </section>
  </div>
</template>

<style scoped>
.tab-stack {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
.filter-bar {
  padding: 1rem;
}
.filter-controls {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 1rem;
}
.filter-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 180px;
}
.filter-item label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #334155;
}
.filter-actions {
  display: flex;
  gap: 0.5rem;
}
.attendance-summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 1rem;
}
.metric-card {
  padding: 1rem;
  background: var(--surface-container-lowest, #ffffff);
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.metric-label {
  font-size: 0.8125rem;
  color: var(--text-color-secondary, #64748b);
}
.metric-value {
  font-size: 1.25rem;
  color: #1e293b;
}
.metric-success .metric-value { color: #16a34a; }
.metric-warn .metric-value { color: #d97706; }
.metric-danger .metric-value { color: #dc2626; }
.metric-info .metric-value { color: #2563eb; }
.card-header {
  margin-bottom: 1.25rem;
}
.card-header h2 {
  margin: 0 0 0.25rem;
  font-size: 1.25rem;
  font-weight: 600;
}
.table-pagination {
  margin-top: 1rem;
}
</style>
