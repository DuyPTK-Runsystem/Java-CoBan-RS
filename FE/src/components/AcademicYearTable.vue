<script setup lang="ts">
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'

import StatusTag from '@/components/StatusTag.vue'
import type { AcademicYear, AcademicYearStatus } from '@/types/academic'
import { formatAcademicDate } from '@/utils/academicDate'

const props = withDefaults(defineProps<{
  academicYears?: AcademicYear[]
  loading?: boolean
}>(), {
  academicYears: () => [],
  loading: false,
})

const emit = defineEmits<{
  edit: [academicYear: AcademicYear]
  close: [academicYear: AcademicYear]
  viewSemesters: [academicYear: AcademicYear]
}>()

const statusLabels: Record<AcademicYearStatus, string> = {
  DRAFT: 'Chưa hoạt động',
  ACTIVE: 'Đang hoạt động',
  CLOSED: 'Đã đóng',
}

const statusSeverities: Record<AcademicYearStatus, 'secondary' | 'success' | 'contrast'> = {
  DRAFT: 'secondary',
  ACTIVE: 'success',
  CLOSED: 'contrast',
}

function canEdit(status: AcademicYearStatus): boolean {
  return status !== 'CLOSED'
}
</script>

<template>
  <div class="table-shell">
    <DataTable
      :value="props.academicYears"
      :loading="props.loading"
      data-key="id"
      striped-rows
      responsive-layout="scroll"
    >
      <template #empty>
        <div class="empty-state">
          <i class="pi pi-calendar" aria-hidden="true" />
          <strong>Chưa có năm học</strong>
          <p>Không có năm học phù hợp với bộ lọc hiện tại.</p>
        </div>
      </template>
      <Column header="#" style="width: 4rem">
        <template #body="slotProps">{{ slotProps.index + 1 }}</template>
      </Column>
      <Column field="code" header="Năm học" />
      <Column header="Thời gian">
        <template #body="slotProps">
          <span class="date-range">{{ formatAcademicDate(slotProps.data.startDate) }} <span aria-hidden="true">→</span> {{ formatAcademicDate(slotProps.data.endDate) }}</span>
        </template>
      </Column>
      <Column header="Trạng thái">
        <template #body="slotProps">
          <StatusTag :label="statusLabels[slotProps.data.status]" :severity="statusSeverities[slotProps.data.status]" />
        </template>
      </Column>
      <Column field="notes" header="Ghi chú">
        <template #body="slotProps">{{ slotProps.data.notes || '-' }}</template>
      </Column>
      <Column header="Thao tác" style="width: 13rem">
        <template #body="slotProps">
          <div class="table-actions">
            <Button icon="pi pi-list" text rounded aria-label="Xem học kỳ" title="Xem học kỳ" @click="emit('viewSemesters', slotProps.data)" />
            <Button v-if="canEdit(slotProps.data.status)" icon="pi pi-pencil" text rounded aria-label="Sửa năm học" title="Sửa năm học" @click="emit('edit', slotProps.data)" />
            <Button v-if="canEdit(slotProps.data.status)" icon="pi pi-lock" text rounded severity="warn" aria-label="Đóng năm học" title="Đóng năm học" @click="emit('close', slotProps.data)" />
          </div>
        </template>
      </Column>
    </DataTable>
  </div>
</template>
