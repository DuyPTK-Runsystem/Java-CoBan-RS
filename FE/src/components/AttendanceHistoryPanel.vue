<script setup lang="ts">
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'

import AttendanceSummaryCards, { type AttendanceSummaryMetric } from '@/components/AttendanceSummaryCards.vue'
import FormAlert from '@/components/FormAlert.vue'
import ServerPagination from '@/components/ServerPagination.vue'
import type { AcademicYear, Semester } from '@/types/academic'
import type { AttendanceSessionPeriod, StudentAttendanceHistoryResponse } from '@/types/attendance'

const props = withDefaults(defineProps<{
  academicYears?: AcademicYear[]
  semesters?: Semester[]
  academicYearId?: number | null
  semesterId?: number | null
  from?: string
  to?: string
  page?: number
  pageSize?: number
  response?: StudentAttendanceHistoryResponse | null
  loading?: boolean
  errorMessage?: string
  forbidden?: boolean
}>(), {
  academicYears: () => [],
  semesters: () => [],
  academicYearId: null,
  semesterId: null,
  from: '',
  to: '',
  page: 0,
  pageSize: 10,
  response: null,
  loading: false,
  errorMessage: '',
  forbidden: false,
})

const emit = defineEmits<{
  'update:academicYearId': [value: number | null]
  'update:semesterId': [value: number | null]
  'update:from': [value: string]
  'update:to': [value: string]
  search: []
  pageChange: [page: number, pageSize: number]
}>()

const periodLabels: Record<AttendanceSessionPeriod, string> = { MORNING: 'Sáng', AFTERNOON: 'Chiều' }
const statusLabels: Record<string, string> = {
  PRESENT: 'Có mặt',
  ABSENT: 'Vắng',
  EXCUSED: 'Vắng có phép',
  LATE: 'Đi trễ',
  EARLY_LEAVE: 'Về sớm',
}
const statusSeverity: Record<string, 'success' | 'danger' | 'warn' | 'info' | 'secondary'> = {
  PRESENT: 'success',
  ABSENT: 'danger',
  EXCUSED: 'warn',
  LATE: 'info',
  EARLY_LEAVE: 'secondary',
}

function metrics(): AttendanceSummaryMetric[] {
  const summary = props.response?.summary
  return [
    { key: 'valid', label: 'Buổi hợp lệ', value: summary?.validSessionCount ?? 0 },
    { key: 'present', label: 'Có mặt', value: summary?.presentCount ?? 0, tone: 'success' },
    { key: 'excused', label: 'Vắng có phép', value: summary?.excusedAbsenceCount ?? 0, tone: 'warning' },
    { key: 'unexcused', label: 'Vắng không phép', value: summary?.unexcusedAbsenceCount ?? 0, tone: 'danger' },
    { key: 'late', label: 'Đi trễ', value: summary?.lateCount ?? 0 },
    { key: 'early', label: 'Về sớm', value: summary?.earlyLeaveCount ?? 0 },
  ]
}

function statusLabel(value: string): string {
  return statusLabels[value] ?? value
}

function statusTagSeverity(value: string): 'success' | 'danger' | 'warn' | 'info' | 'secondary' {
  return statusSeverity[value] ?? 'secondary'
}

function handlePageChange(page: number, pageSize: number): void {
  emit('pageChange', page, pageSize)
}
</script>

<template>
  <section class="content-surface attendance-history-panel">
    <div class="section-heading">
      <div><h2>Lịch sử chuyên cần của tôi</h2><p class="section-caption">Read-only · endpoint self-service chỉ trả dữ liệu của học sinh đang đăng nhập.</p></div>
      <span class="field-hint">Không nhận studentId từ giao diện</span>
    </div>
    <FormAlert v-if="props.forbidden" tone="warning" message="Bạn không có quyền xem lịch sử chuyên cần này." />
    <FormAlert v-else-if="props.errorMessage" tone="error" :message="props.errorMessage" />
    <div class="attendance-filter-grid">
      <div class="field-group"><label for="attendance-history-year">Năm học</label><Select id="attendance-history-year" :model-value="props.academicYearId" :options="props.academicYears" option-label="code" option-value="id" placeholder="Tất cả năm học" fluid @update:model-value="emit('update:academicYearId', $event)" /></div>
      <div class="field-group"><label for="attendance-history-semester">Học kỳ</label><Select id="attendance-history-semester" :model-value="props.semesterId" :options="props.semesters" option-label="name" option-value="id" placeholder="Tất cả học kỳ" :disabled="!props.academicYearId" fluid @update:model-value="emit('update:semesterId', $event)" /></div>
      <div class="field-group"><label for="attendance-history-from">Từ ngày</label><InputText id="attendance-history-from" :model-value="props.from" type="date" fluid @update:model-value="emit('update:from', $event ?? '')" /></div>
      <div class="field-group"><label for="attendance-history-to">Đến ngày</label><InputText id="attendance-history-to" :model-value="props.to" type="date" fluid @update:model-value="emit('update:to', $event ?? '')" /></div>
      <Button label="Xem lịch sử" icon="pi pi-search" :loading="props.loading" @click="emit('search')" />
    </div>
    <AttendanceSummaryCards :metrics="metrics()" />
    <div class="table-shell catalog-table attendance-history-table">
      <DataTable :value="props.response?.items ?? []" :loading="props.loading" data-key="attendanceRecordId" striped-rows responsive-layout="scroll">
        <template #empty><div class="empty-state"><i class="pi pi-history" aria-hidden="true" /><strong>Chưa có lịch sử chuyên cần</strong><p>Không có bản ghi phù hợp với bộ lọc hiện tại.</p></div></template>
        <Column header="Ngày"><template #body="slotProps">{{ slotProps.data.attendanceDate.split('-').reverse().join('/') }}</template></Column>
        <Column header="Buổi"><template #body="slotProps">{{ periodLabels[slotProps.data.sessionPeriod] }}</template></Column>
        <Column header="Lớp"><template #body="slotProps">{{ slotProps.data.className || `Lớp #${slotProps.data.classId}` }}</template></Column>
        <Column header="Trạng thái"><template #body="slotProps"><Tag :value="statusLabel(slotProps.data.status)" :severity="statusTagSeverity(slotProps.data.status)" /></template></Column>
        <Column header="Exception"><template #body="slotProps">{{ slotProps.data.exceptionStatus || '—' }}</template></Column>
        <Column header="Ghi chú"><template #body="slotProps">{{ slotProps.data.note || '—' }}</template></Column>
      </DataTable>
    </div>
    <ServerPagination :page="props.response?.page ?? props.page" :page-size="props.response?.size ?? props.pageSize" :total-records="props.response?.totalElements ?? 0" @page-change="handlePageChange" />
  </section>
</template>
