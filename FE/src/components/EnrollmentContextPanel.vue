<script setup lang="ts">
import Select from 'primevue/select'

import StatusTag from '@/components/StatusTag.vue'
import type { AcademicYear, GradeLevel, SchoolClass } from '@/types/academic'

const props = withDefaults(defineProps<{
  academicYears?: AcademicYear[]
  grades?: GradeLevel[]
  classes?: SchoolClass[]
  academicYearId?: number | null
  gradeId?: number | null
  classId?: number | null
  loading?: boolean
  classLoading?: boolean
}>(), {
  academicYears: () => [],
  grades: () => [],
  classes: () => [],
  academicYearId: null,
  gradeId: null,
  classId: null,
  loading: false,
  classLoading: false,
})

const emit = defineEmits<{
  'update:academicYearId': [value: number | null]
  'update:gradeId': [value: number | null]
  'update:classId': [value: number | null]
}>()

const statusLabels = { PLANNED: 'Đã khởi tạo', ACTIVE: 'Đang hoạt động', CLOSED: 'Đã đóng' } as const
const statusSeverities = { PLANNED: 'secondary', ACTIVE: 'success', CLOSED: 'contrast' } as const

function selectedClass(): SchoolClass | null {
  return props.classes.find((schoolClass) => schoolClass.id === props.classId) ?? null
}
</script>

<template>
  <section class="content-surface enrollment-context-panel">
    <div class="section-heading">
      <div>
        <h2>Chọn lớp</h2>
        <p class="section-caption">Chọn năm học và lớp để tải danh sách học sinh tương ứng.</p>
      </div>
      <StatusTag
        v-if="selectedClass()"
        :label="statusLabels[selectedClass()!.status]"
        :severity="statusSeverities[selectedClass()!.status]"
      />
    </div>
    <div class="catalog-context-form">
      <div class="field-group">
        <label for="enrollment-academic-year">Năm học</label>
        <Select
          id="enrollment-academic-year"
          :model-value="props.academicYearId"
          :options="props.academicYears"
          option-label="code"
          option-value="id"
          placeholder="Chọn năm học"
          :loading="props.loading"
          fluid
          @update:model-value="emit('update:academicYearId', $event)"
        />
      </div>
      <div class="field-group">
        <label for="enrollment-grade">Khối</label>
        <Select
          id="enrollment-grade"
          :model-value="props.gradeId"
          :options="props.grades"
          option-label="name"
          option-value="id"
          placeholder="Chọn khối"
          :disabled="!props.academicYearId || props.classLoading"
          fluid
          @update:model-value="emit('update:gradeId', $event)"
        />
      </div>
      <div class="field-group">
        <label for="enrollment-class">Lớp hiện tại</label>
        <Select
          id="enrollment-class"
          :model-value="props.classId"
          :options="props.classes"
          option-label="classCode"
          option-value="id"
          placeholder="Chọn lớp"
          :loading="props.classLoading"
          :disabled="!props.academicYearId || props.classLoading"
          fluid
          @update:model-value="emit('update:classId', $event)"
        />
      </div>
      <div class="enrollment-context-summary" aria-live="polite">
        <span>{{ selectedClass()?.className || selectedClass()?.classCode || 'Chưa chọn lớp' }}</span>
        <span v-if="selectedClass()">{{ selectedClass()!.status === 'CLOSED' ? 'Chỉ xem, không xếp/chuyển lớp.' : 'Có thể thực hiện thao tác xếp lớp.' }}</span>
      </div>
    </div>
  </section>
</template>
