<script setup lang="ts">
import FormAlert from '@/components/FormAlert.vue'
import type { SchoolClass } from '@/types/academic'
import type { CapacityWarning } from '@/types/enrollment'

const props = withDefaults(defineProps<{
  available?: boolean
  warningCount?: number
  message?: string
  warnings?: CapacityWarning[]
  classes?: SchoolClass[]
}>(), { available: false, warningCount: 0, message: '', warnings: () => [], classes: () => [] })

function classLabel(classId: number): string {
  return props.classes.find((schoolClass) => schoolClass.id === classId)?.classCode ?? `Lớp #${classId}`
}
</script>

<template>
  <FormAlert v-if="!props.available" tone="info" message="Chưa có dữ liệu sĩ số hoặc cảnh báo từ backend contract." />
  <div v-else-if="props.warningCount > 0" class="form-alert form-alert-warning" role="status" aria-live="polite">
    <strong>{{ props.warningCount }} cảnh báo sĩ số</strong>
    <ul v-if="props.warnings.length" class="capacity-warning-list">
      <li v-for="warning in props.warnings" :key="`${warning.classId}-${warning.academicYearId}-${warning.gradeLevelId}`">
        <strong>{{ classLabel(warning.classId) }}</strong>: {{ warning.message }}
      </li>
    </ul>
    <span v-else>{{ props.message || 'Cảnh báo không chặn thao tác chỉnh sửa hoặc đóng lớp.' }}</span>
  </div>
  <FormAlert v-else tone="success" message="Không có cảnh báo sĩ số trong dữ liệu hiện tại." />
</template>
