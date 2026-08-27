<script setup lang="ts">
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'

import StatusTag from '@/components/StatusTag.vue'
import type { GradeLevel, SchoolClass, SchoolClassStatus } from '@/types/academic'

const props = withDefaults(defineProps<{
  schoolClasses?: SchoolClass[]
  grades?: GradeLevel[]
  loading?: boolean
}>(), {
  schoolClasses: () => [],
  grades: () => [],
  loading: false,
})

const emit = defineEmits<{
  edit: [schoolClass: SchoolClass]
  close: [schoolClass: SchoolClass]
  delete: [schoolClass: SchoolClass]
}>()

const statusLabels: Record<SchoolClassStatus, string> = { PLANNED: 'Đã khởi tạo', ACTIVE: 'Đang hoạt động', CLOSED: 'Đã đóng' }
const statusSeverities: Record<SchoolClassStatus, 'secondary' | 'success' | 'contrast'> = { PLANNED: 'secondary', ACTIVE: 'success', CLOSED: 'contrast' }

function gradeName(gradeLevelId: number): string {
  return props.grades.find((grade) => grade.id === gradeLevelId)?.name ?? `Khối #${gradeLevelId}`
}
</script>

<template>
  <div class="table-shell catalog-table">
    <DataTable :value="props.schoolClasses" :loading="props.loading" data-key="id" striped-rows responsive-layout="scroll">
      <template #empty><div class="empty-state"><i class="pi pi-building" aria-hidden="true" /><strong>Chưa có lớp</strong><p>Năm học và bộ lọc hiện tại chưa có lớp nào.</p></div></template>
      <Column header="#" style="width: 4rem"><template #body="slotProps">{{ slotProps.index + 1 }}</template></Column>
      <Column header="Lớp"><template #body="slotProps"><div class="primary-cell"><strong>{{ slotProps.data.classCode }}</strong><span>{{ slotProps.data.className || 'Không có tên hiển thị' }}</span></div></template></Column>
      <Column header="Khối"><template #body="slotProps">{{ gradeName(slotProps.data.gradeLevelId) }}</template></Column>
      <Column header="Sĩ số"><template #body><span class="table-action-note">Chưa có dữ liệu sĩ số</span></template></Column>
      <Column header="Trạng thái"><template #body="slotProps"><StatusTag :label="statusLabels[slotProps.data.status]" :severity="statusSeverities[slotProps.data.status]" /></template></Column>
      <Column header="Ghi chú"><template #body><span class="table-action-note">Chưa có dữ liệu thống kê</span></template></Column>
      <Column header="Thao tác" style="width: 12rem">
        <template #body="slotProps"><div class="table-actions"><Button v-if="slotProps.data.status !== 'CLOSED'" icon="pi pi-pencil" text rounded aria-label="Sửa lớp" title="Sửa lớp" @click="emit('edit', slotProps.data)" /><Button v-if="slotProps.data.status === 'ACTIVE' || slotProps.data.status === 'PLANNED'" icon="pi pi-lock" text rounded severity="warn" aria-label="Đóng lớp" title="Đóng lớp" @click="emit('close', slotProps.data)" /><Button v-if="slotProps.data.status !== 'CLOSED'" icon="pi pi-trash" text rounded severity="danger" aria-label="Xóa lớp" title="Xóa lớp" @click="emit('delete', slotProps.data)" /><span v-if="slotProps.data.status === 'CLOSED'" class="table-action-note">Chỉ xem</span></div></template>
      </Column>
    </DataTable>
  </div>
</template>
