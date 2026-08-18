<script setup lang="ts">
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable, { type DataTableSortEvent } from 'primevue/datatable'
import Paginator, { type PageState } from 'primevue/paginator'

import type { Student } from '@/types/student'

const props = withDefaults(defineProps<{
  students?: Student[]
  loading?: boolean
  totalRecords?: number
  page?: number
  rowsPerPage?: number
  sortField?: keyof Student
  sortOrder?: 1 | -1
}>(), {
  students: () => [],
  loading: false,
  totalRecords: 0,
  page: 0,
  rowsPerPage: 10,
  sortField: 'studentCode',
  sortOrder: 1,
})

const emit = defineEmits<{
  pageChange: [page: number]
  sortChange: [field: keyof Student, order: 1 | -1]
  edit: [student: Student]
  delete: [student: Student]
}>()

function handleSort(event: DataTableSortEvent): void {
  const sortableFields: Array<keyof Student> = ['studentCode', 'studentName', 'dateOfBirth', 'address', 'averageScore']
  const requestedField = typeof event.sortField === 'string' ? event.sortField as keyof Student : undefined
  const field = requestedField && sortableFields.includes(requestedField) ? requestedField : 'studentCode'
  const order = event.sortOrder === -1 ? -1 : 1
  emit('sortChange', field, order)
}

function handlePage(event: PageState): void {
  emit('pageChange', event.page)
}
</script>

<template>
  <div class="table-shell">
    <DataTable
      :value="props.students"
      :loading="props.loading"
      data-key="studentId"
      removable-sort
      :sort-field="props.sortField"
      :sort-order="props.sortOrder"
      striped-rows
      responsive-layout="scroll"
      @sort="handleSort"
    >
      <template #empty>
        <div class="empty-state">
          <i class="pi pi-users" aria-hidden="true" />
          <span>No students match the current search.</span>
        </div>
      </template>
      <Column header="No" style="width: 5rem">
        <template #body="slotProps">
          {{ props.page * props.rowsPerPage + slotProps.index + 1 }}
        </template>
      </Column>
      <Column field="studentCode" header="Code" sortable />
      <Column field="studentName" header="Name" sortable />
      <Column field="dateOfBirth" header="Birthday" sortable />
      <Column field="address" header="Address" sortable />
      <Column field="averageScore" header="Score" sortable />
      <Column header="Actions" style="width: 9rem">
        <template #body="slotProps">
          <div class="table-actions">
            <Button icon="pi pi-pencil" text rounded aria-label="Edit student" @click="emit('edit', slotProps.data)" />
            <Button icon="pi pi-trash" text rounded severity="danger" aria-label="Delete student" @click="emit('delete', slotProps.data)" />
          </div>
        </template>
      </Column>
    </DataTable>
    <Paginator
      :first="props.page * props.rowsPerPage"
      :rows="props.rowsPerPage"
      :total-records="props.totalRecords"
      :rows-per-page-options="[10]"
      @page="handlePage"
    />
  </div>
</template>
