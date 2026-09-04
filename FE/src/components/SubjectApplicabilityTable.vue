<script setup lang="ts">
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Tag from 'primevue/tag'

import type { GradeLevel, SchoolClass, Semester, SubjectApplicability } from '@/types/academic'

const props = withDefaults(defineProps<{
  applicabilities?: SubjectApplicability[]
  semesters?: Semester[]
  grades?: GradeLevel[]
  schoolClasses?: SchoolClass[]
  loading?: boolean
}>(), {
  applicabilities: () => [],
  semesters: () => [],
  grades: () => [],
  schoolClasses: () => [],
  loading: false,
})

const emit = defineEmits<{
  create: []
  edit: [applicability: SubjectApplicability]
  deactivate: [applicability: SubjectApplicability]
  reactivate: [applicability: SubjectApplicability]
}>()

const scopeLabels = { GRADE: 'Theo khối', CLASS: 'Theo lớp' } as const
const statusLabels = { ACTIVE: 'Đang áp dụng', INACTIVE: 'Ngừng áp dụng' } as const

function semesterLabel(semesterId: number): string {
  const semester = props.semesters.find((item) => item.id === semesterId)
  return semester ? `${semester.name} · ${semester.code}` : `Học kỳ #${semesterId}`
}

function targetLabel(applicability: SubjectApplicability): string {
  if (applicability.scopeType === 'GRADE') {
    return props.grades.find((grade) => grade.id === applicability.gradeLevelId)?.name ?? `Khối #${applicability.gradeLevelId}`
  }
  const schoolClass = props.schoolClasses.find((item) => item.id === applicability.classId)
  return schoolClass ? `${schoolClass.classCode}${schoolClass.className ? ` · ${schoolClass.className}` : ''}` : `Lớp #${applicability.classId}`
}
</script>

<template>
  <div class="applicability-list">
    <div class="section-heading">
      <div>
        <h2>Cấu hình hiện tại</h2>
        <p class="section-caption">Các phạm vi áp dụng đã lưu cho môn học này.</p>
      </div>
      <Button label="Thêm cấu hình" icon="pi pi-plus" size="small" @click="emit('create')" />
    </div>
    <DataTable :value="props.applicabilities" :loading="props.loading" class="catalog-table applicability-table" responsive-layout="scroll" striped-rows>
      <template #empty>Chưa có cấu hình phạm vi áp dụng.</template>
      <Column field="semesterId" header="Học kỳ">
        <template #body="slotProps">{{ semesterLabel(slotProps.data.semesterId) }}</template>
      </Column>
      <Column field="scopeType" header="Phạm vi">
        <template #body="slotProps">{{ scopeLabels[slotProps.data.scopeType] }}</template>
      </Column>
      <Column header="Đối tượng">
        <template #body="slotProps">{{ targetLabel(slotProps.data) }}</template>
      </Column>
      <Column field="status" header="Trạng thái">
        <template #body="slotProps">
          <Tag :value="statusLabels[slotProps.data.status]" :severity="slotProps.data.status === 'ACTIVE' ? 'success' : 'secondary'" />
        </template>
      </Column>
      <Column header="Thao tác" style="width: 10rem">
        <template #body="slotProps">
          <div class="table-actions">
            <Button icon="pi pi-pencil" text rounded aria-label="Sửa cấu hình áp dụng" title="Sửa cấu hình áp dụng" @click="emit('edit', slotProps.data)" />
            <Button v-if="slotProps.data.status === 'ACTIVE'" icon="pi pi-ban" text rounded severity="danger" aria-label="Ngừng áp dụng" title="Ngừng áp dụng" @click="emit('deactivate', slotProps.data)" />
            <Button v-else icon="pi pi-refresh" text rounded severity="success" aria-label="Kích hoạt lại" title="Kích hoạt lại" @click="emit('reactivate', slotProps.data)" />
          </div>
        </template>
      </Column>
    </DataTable>
  </div>
</template>
