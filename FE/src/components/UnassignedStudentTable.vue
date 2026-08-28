<script setup lang="ts">
import { computed, ref } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import InputText from 'primevue/inputtext'

import type { UnassignedStudent } from '@/types/enrollment'

const props = withDefaults(defineProps<{
  students?: UnassignedStudent[]
  selectedStudents?: UnassignedStudent[]
  loading?: boolean
  readOnly?: boolean
}>(), {
  students: () => [],
  selectedStudents: () => [],
  loading: false,
  readOnly: false,
})

const emit = defineEmits<{
  'update:selectedStudents': [students: UnassignedStudent[]]
  place: [student: UnassignedStudent]
  history: [student: UnassignedStudent]
}>()

const search = ref('')
const filteredStudents = computed(() => {
  const query = search.value.trim().toLocaleLowerCase()
  if (!query) return props.students
  return props.students.filter((student) => `${student.studentCode} ${student.studentName}`.toLocaleLowerCase().includes(query))
})

function updateSelection(value: UnassignedStudent[] | UnassignedStudent | null): void {
  emit('update:selectedStudents', Array.isArray(value) ? value : value ? [value] : [])
}
</script>

<template>
  <div class="table-shell enrollment-table">
    <div class="table-toolbar">
      <div>
        <h2>Học sinh chưa xếp lớp</h2>
        <p class="section-caption">Tìm kiếm cục bộ trong danh sách đã tải theo mã hoặc tên học sinh.</p>
      </div>
      <InputText v-model="search" aria-label="Tìm học sinh chưa xếp lớp" placeholder="Mã hoặc tên học sinh" />
    </div>
    <DataTable
      :value="filteredStudents"
      :loading="props.loading"
      :selection="props.selectedStudents"
      data-key="studentId"
      striped-rows
      responsive-layout="scroll"
      @update:selection="updateSelection"
    >
      <template #empty>
        <div class="empty-state"><i class="pi pi-user-minus" aria-hidden="true" /><strong>Không có học sinh chưa xếp lớp</strong><p>Năm học hiện tại đã được xếp lớp đầy đủ hoặc chưa có dữ liệu.</p></div>
      </template>
      <Column selection-mode="multiple" header-style="width: 3rem" />
      <Column header="Mã học sinh"><template #body="slotProps"><strong>{{ slotProps.data.studentCode }}</strong></template></Column>
      <Column field="studentName" header="Họ và tên" />
      <Column header="Thao tác" style="width: 10rem">
        <template #body="slotProps">
          <div class="table-actions">
            <Button icon="pi pi-plus" text rounded :disabled="props.readOnly" aria-label="Xếp học sinh" title="Xếp học sinh" @click="emit('place', slotProps.data)" />
            <Button icon="pi pi-history" text rounded severity="secondary" aria-label="Xem lịch sử học sinh" title="Xem lịch sử học sinh" @click="emit('history', slotProps.data)" />
          </div>
        </template>
      </Column>
    </DataTable>
  </div>
</template>
