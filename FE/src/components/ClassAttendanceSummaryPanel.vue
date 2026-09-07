<script setup lang="ts">
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'

import AttendanceSummaryCards, { type AttendanceSummaryMetric } from '@/components/AttendanceSummaryCards.vue'
import FormAlert from '@/components/FormAlert.vue'
import ServerPagination from '@/components/ServerPagination.vue'
import type { SchoolClass, Semester } from '@/types/academic'
import type { ClassAttendanceSummaryResponse } from '@/types/attendance'

const props = withDefaults(defineProps<{
  classes?: SchoolClass[]
  semesters?: Semester[]
  classId?: number | null
  semesterId?: number | null
  from?: string
  to?: string
  page?: number
  pageSize?: number
  response?: ClassAttendanceSummaryResponse | null
  loading?: boolean
  errorMessage?: string
  forbidden?: boolean
}>(), {
  classes: () => [],
  semesters: () => [],
  classId: null,
  semesterId: null,
  from: '',
  to: '',
  page: 0,
  pageSize: 20,
  response: null,
  loading: false,
  errorMessage: '',
  forbidden: false,
})

const emit = defineEmits<{
  'update:classId': [value: number | null]
  'update:semesterId': [value: number | null]
  'update:from': [value: string]
  'update:to': [value: string]
  pageChange: [page: number, pageSize: number]
}>()

function metrics(): AttendanceSummaryMetric[] {
  const summary = props.response?.summary
  return [
    { key: 'valid', label: 'Buổi hợp lệ', value: props.response?.validSessionCount ?? 0 },
    { key: 'present', label: 'Tổng có mặt', value: summary?.presentCount ?? 0, tone: 'success' },
    { key: 'excused', label: 'Vắng có phép', value: summary?.excusedAbsenceCount ?? 0, tone: 'warning' },
    { key: 'unexcused', label: 'Vắng không phép', value: summary?.unexcusedAbsenceCount ?? 0, tone: 'danger' },
    { key: 'late', label: 'Đi trễ', value: summary?.lateCount ?? 0 },
    { key: 'early', label: 'Về sớm', value: summary?.earlyLeaveCount ?? 0 },
  ]
}

function percent(value: number): string {
  return `${Math.round(value * 100)}%`
}

function handlePageChange(page: number, pageSize: number): void {
  emit('pageChange', page, pageSize)
}
</script>

<template>
  <section class="content-surface attendance-summary-panel">
    <div class="section-heading">
      <div><h2>Báo cáo chuyên cần theo lớp</h2><p class="section-caption">Số liệu read-only theo buổi học hợp lệ và enrollment tại ngày điểm danh.</p></div>
      <span class="field-hint">Aggregation do backend thực hiện</span>
    </div>
    <FormAlert v-if="props.forbidden" tone="warning" message="Bạn không có quyền xem báo cáo của lớp này." />
    <FormAlert v-else-if="props.errorMessage" tone="error" :message="props.errorMessage" />
    <div class="attendance-filter-grid attendance-summary-filter-grid">
      <div class="field-group"><label for="attendance-summary-class">Lớp</label><Select id="attendance-summary-class" :model-value="props.classId" :options="props.classes" option-label="classCode" option-value="id" placeholder="Chọn lớp" fluid @update:model-value="emit('update:classId', $event)" /></div>
      <div class="field-group"><label for="attendance-summary-semester">Học kỳ</label><Select id="attendance-summary-semester" :model-value="props.semesterId" :options="props.semesters" option-label="name" option-value="id" placeholder="Chọn học kỳ" fluid @update:model-value="emit('update:semesterId', $event)" /></div>
      <div class="field-group"><label for="attendance-summary-from">Từ ngày</label><InputText id="attendance-summary-from" :model-value="props.from" type="date" fluid @update:model-value="emit('update:from', $event ?? '')" /></div>
      <div class="field-group"><label for="attendance-summary-to">Đến ngày</label><InputText id="attendance-summary-to" :model-value="props.to" type="date" fluid @update:model-value="emit('update:to', $event ?? '')" /></div>
    </div>
    <div v-if="props.response" class="attendance-report-context"><strong>{{ props.response.class.name }}</strong><span>{{ props.response.from }} → {{ props.response.to }}</span><span>{{ props.response.totalElements }} học sinh</span></div>
    <AttendanceSummaryCards :metrics="metrics()" />
    <div class="table-shell catalog-table attendance-summary-table">
      <DataTable :value="props.response?.students ?? []" :loading="props.loading" data-key="studentId" striped-rows responsive-layout="scroll">
        <template #empty><div class="empty-state"><i class="pi pi-chart-bar" aria-hidden="true" /><strong>Chưa có dữ liệu báo cáo</strong><p>Chọn đủ lớp, học kỳ và khoảng ngày để tải thống kê.</p></div></template>
        <Column header="Học sinh"><template #body="slotProps"><div class="primary-cell"><strong>{{ slotProps.data.fullName }}</strong><span>{{ slotProps.data.studentCode }}</span></div></template></Column>
        <Column header="Buổi hợp lệ" field="validSessionCount" />
        <Column header="Có mặt" field="presentCount" />
        <Column header="Vắng phép" field="excusedAbsenceCount" />
        <Column header="Vắng K/P" field="unexcusedAbsenceCount" />
        <Column header="Đi trễ" field="lateCount" />
        <Column header="Về sớm" field="earlyLeaveCount" />
        <Column header="Tỷ lệ"><template #body="slotProps"><Tag :value="percent(slotProps.data.attendanceRate)" :severity="slotProps.data.attendanceRate >= 0.9 ? 'success' : 'warn'" /></template></Column>
      </DataTable>
    </div>
    <ServerPagination :page="props.response?.page ?? props.page" :page-size="props.response?.size ?? props.pageSize" :total-records="props.response?.totalElements ?? 0" @page-change="handlePageChange" />
  </section>
</template>
