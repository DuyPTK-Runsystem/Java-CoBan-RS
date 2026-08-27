<script setup lang="ts">
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'

import StatusTag from '@/components/StatusTag.vue'
import type { GradeLevel } from '@/types/academic'

const props = withDefaults(defineProps<{
  grades?: GradeLevel[]
  loading?: boolean
}>(), {
  grades: () => [],
  loading: false,
})

const emit = defineEmits<{
  edit: [grade: GradeLevel]
  toggleActive: [grade: GradeLevel]
  delete: [grade: GradeLevel]
}>()

function nextGradeName(grade: GradeLevel): string {
  return props.grades.find((candidate) => candidate.id === grade.nextGradeId)?.name ?? 'Không có'
}
</script>

<template>
  <div class="table-shell catalog-table">
    <DataTable :value="props.grades" :loading="props.loading" data-key="id" striped-rows responsive-layout="scroll">
      <template #empty>
        <div class="empty-state">
          <i class="pi pi-sitemap" aria-hidden="true" />
          <strong>Chưa có khối</strong>
          <p>Chưa có metadata khối phù hợp để hiển thị.</p>
        </div>
      </template>
      <Column header="#" style="width: 4rem"><template #body="slotProps">{{ slotProps.index + 1 }}</template></Column>
      <Column header="Khối">
        <template #body="slotProps"><div class="primary-cell"><strong>{{ slotProps.data.name }}</strong><span>{{ slotProps.data.description || 'Không có mô tả' }}</span></div></template>
      </Column>
      <Column field="code" header="Mã" />
      <Column field="gradeLevel" header="Cấp" />
      <Column header="Khối tiếp theo"><template #body="slotProps">{{ nextGradeName(slotProps.data) }}</template></Column>
      <Column header="Thống kê theo năm học"><template #body><span class="table-action-note">Chưa có dữ liệu thống kê</span></template></Column>
      <Column header="Trạng thái">
        <template #body="slotProps"><StatusTag :label="slotProps.data.active ? 'Đang dùng' : 'Ngừng dùng'" :severity="slotProps.data.active ? 'success' : 'secondary'" /></template>
      </Column>
      <Column header="Thao tác" style="width: 13rem">
        <template #body="slotProps">
          <div class="table-actions">
            <Button icon="pi pi-pencil" text rounded aria-label="Sửa khối" title="Sửa khối" @click="emit('edit', slotProps.data)" />
            <Button :icon="slotProps.data.active ? 'pi pi-ban' : 'pi pi-check'" text rounded :severity="slotProps.data.active ? 'warn' : 'success'" :aria-label="slotProps.data.active ? 'Ngừng dùng khối' : 'Kích hoạt khối'" :title="slotProps.data.active ? 'Ngừng dùng khối' : 'Kích hoạt khối'" @click="emit('toggleActive', slotProps.data)" />
            <Button icon="pi pi-trash" text rounded severity="danger" aria-label="Xóa khối" title="Xóa khối" @click="emit('delete', slotProps.data)" />
          </div>
        </template>
      </Column>
    </DataTable>
  </div>
</template>
