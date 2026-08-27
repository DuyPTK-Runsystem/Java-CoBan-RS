<script setup lang="ts">
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'

import StatusTag from '@/components/StatusTag.vue'
import type { Semester, SemesterStatus } from '@/types/academic'
import { formatAcademicDate, formatAcademicDateTime } from '@/utils/academicDate'

const props = withDefaults(defineProps<{
  semesters?: Semester[]
  loading?: boolean
}>(), {
  semesters: () => [],
  loading: false,
})

const emit = defineEmits<{
  edit: [semester: Semester]
  activate: [semester: Semester]
  viewStatus: [semester: Semester]
  lock: [semester: Semester]
  reopen: [semester: Semester]
}>()

const statusLabels: Record<SemesterStatus, string> = {
  DRAFT: 'Nháp',
  ACTIVE: 'Đang hoạt động',
  LOCKED: 'Đã khóa',
  CLOSED: 'Đã đóng',
}

const statusSeverities: Record<SemesterStatus, 'secondary' | 'success' | 'warn' | 'contrast'> = {
  DRAFT: 'secondary',
  ACTIVE: 'success',
  LOCKED: 'warn',
  CLOSED: 'contrast',
}
</script>

<template>
  <div class="table-shell">
    <DataTable :value="props.semesters" :loading="props.loading" data-key="id" striped-rows responsive-layout="scroll">
      <template #empty>
        <div class="empty-state">
          <i class="pi pi-calendar-clock" aria-hidden="true" />
          <strong>Chưa có học kỳ</strong>
          <p>Năm học này chưa có học kỳ nào.</p>
        </div>
      </template>
      <Column header="#" style="width: 4rem">
        <template #body="slotProps">{{ slotProps.index + 1 }}</template>
      </Column>
      <Column field="code" header="Mã" />
      <Column header="Học kỳ">
        <template #body="slotProps">
          <strong>{{ slotProps.data.name }}</strong>
        </template>
      </Column>
      <Column header="Thời gian">
        <template #body="slotProps">
          <span class="date-range">{{ formatAcademicDate(slotProps.data.startDate) }} <span aria-hidden="true">→</span> {{ formatAcademicDate(slotProps.data.endDate) }}</span>
        </template>
      </Column>
      <Column header="Tự động khóa">
        <template #body="slotProps">{{ formatAcademicDateTime(slotProps.data.automaticLockAt) }}</template>
      </Column>
      <Column header="Trạng thái">
        <template #body="slotProps">
          <StatusTag :label="statusLabels[slotProps.data.status]" :severity="statusSeverities[slotProps.data.status]" />
        </template>
      </Column>
      <Column header="Thao tác" style="width: 15rem">
        <template #body="slotProps">
          <div class="table-actions">
            <Button v-if="slotProps.data.status !== 'CLOSED'" icon="pi pi-pencil" text rounded aria-label="Sửa học kỳ" title="Sửa học kỳ" @click="emit('edit', slotProps.data)" />
            <Button v-if="slotProps.data.status === 'DRAFT'" icon="pi pi-play" text rounded severity="success" aria-label="Kích hoạt học kỳ" title="Kích hoạt học kỳ" @click="emit('activate', slotProps.data)" />
            <Button v-if="slotProps.data.status === 'ACTIVE' || slotProps.data.status === 'LOCKED'" icon="pi pi-eye" text rounded aria-label="Xem trạng thái khóa" title="Xem trạng thái khóa" @click="emit('viewStatus', slotProps.data)" />
            <Button v-if="slotProps.data.status === 'ACTIVE'" icon="pi pi-lock" text rounded severity="warn" aria-label="Khóa học kỳ" title="Khóa học kỳ" @click="emit('lock', slotProps.data)" />
            <Button v-if="slotProps.data.status === 'LOCKED'" icon="pi pi-lock-open" text rounded severity="info" aria-label="Mở lại học kỳ" title="Mở lại học kỳ" @click="emit('reopen', slotProps.data)" />
            <span v-if="slotProps.data.status === 'CLOSED'" class="table-action-note">Chỉ xem</span>
          </div>
        </template>
      </Column>
    </DataTable>
  </div>
</template>
