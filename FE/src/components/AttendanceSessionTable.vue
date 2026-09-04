<script setup lang="ts">
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Tag from 'primevue/tag'

import type { AttendanceStudent, AttendanceUiStatus } from '@/types/attendance'

const props = withDefaults(defineProps<{
  students?: AttendanceStudent[]
  loading?: boolean
  readOnly?: boolean
}>(), {
  students: () => [],
  loading: false,
  readOnly: false,
})

const emit = defineEmits<{
  exception: [student: AttendanceStudent]
  delete: [student: AttendanceStudent]
}>()

const statusLabels: Record<AttendanceUiStatus, string> = {
  PRESENT: 'Có mặt',
  ABSENT: 'Vắng',
  EXCUSED: 'Vắng có phép',
  LATE: 'Đi trễ',
  EARLY_LEAVE: 'Về sớm',
}

const statusSeverity: Record<AttendanceUiStatus, 'success' | 'danger' | 'warn' | 'info' | 'secondary'> = {
  PRESENT: 'success',
  ABSENT: 'danger',
  EXCUSED: 'warn',
  LATE: 'info',
  EARLY_LEAVE: 'secondary',
}

function rowStatus(student: AttendanceStudent): AttendanceUiStatus {
  if (student.attendanceRecordId === null) return 'PRESENT'
  const status = student.status as AttendanceUiStatus
  return status in statusLabels ? status : 'PRESENT'
}

function statusLabel(student: AttendanceStudent): string {
  const status = rowStatus(student)
  return status === 'PRESENT' && student.attendanceRecordId === null ? 'Có mặt · mặc định' : statusLabels[status]
}

function timestamp(value: string | null): string {
  return value ? new Date(value).toLocaleString('vi-VN') : 'Chưa có record'
}
</script>

<template>
  <div class="table-shell enrollment-table attendance-session-table">
    <DataTable :value="props.students" :loading="props.loading" data-key="studentId" striped-rows responsive-layout="scroll">
      <template #empty>
        <div class="empty-state"><i class="pi pi-calendar-times" aria-hidden="true" /><strong>Chưa có danh sách học sinh</strong><p>Mở một session hợp lệ để tải danh sách điểm danh.</p></div>
      </template>
      <Column header="Học sinh">
        <template #body="slotProps">
          <div class="primary-cell"><strong>{{ slotProps.data.studentName }}</strong><span>{{ slotProps.data.studentCode }}</span></div>
        </template>
      </Column>
      <Column header="Trạng thái">
        <template #body="slotProps"><Tag :value="statusLabel(slotProps.data)" :severity="statusSeverity[rowStatus(slotProps.data)]" /></template>
      </Column>
      <Column header="Ghi chú"><template #body="slotProps">{{ slotProps.data.note || '—' }}</template></Column>
      <Column header="Cập nhật gần nhất"><template #body="slotProps">{{ timestamp(slotProps.data.updatedAt || slotProps.data.recordedAt) }}</template></Column>
      <Column header="Thao tác" style="width: 12rem">
        <template #body="slotProps">
          <div class="table-actions">
            <Button v-if="!props.readOnly" :icon="slotProps.data.attendanceRecordId === null ? 'pi pi-plus' : 'pi pi-pencil'" text rounded :aria-label="slotProps.data.attendanceRecordId === null ? 'Ghi nhận ngoại lệ' : 'Sửa ngoại lệ'" :title="slotProps.data.attendanceRecordId === null ? 'Ghi nhận ngoại lệ' : 'Sửa ngoại lệ'" @click="emit('exception', slotProps.data)" />
            <Button v-if="!props.readOnly && slotProps.data.attendanceRecordId !== null" icon="pi pi-trash" text rounded severity="danger" aria-label="Xóa ngoại lệ" title="Xóa ngoại lệ" @click="emit('delete', slotProps.data)" />
          </div>
        </template>
      </Column>
    </DataTable>
  </div>
</template>
