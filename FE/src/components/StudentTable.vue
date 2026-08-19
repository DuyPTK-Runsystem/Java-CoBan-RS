<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable, { type DataTableSortEvent } from 'primevue/datatable'
import InputNumber from 'primevue/inputnumber'
import Paginator, { type PageState } from 'primevue/paginator'

import type { Student } from '@/types/student'
import { formatStudentDate } from '@/utils/studentDate'

const props = withDefaults(defineProps<{
  students?: Student[]
  loading?: boolean
  totalRecords?: number
  totalPages?: number
  page?: number
  rowsPerPage?: number
  sortField?: keyof Student
  sortOrder?: 1 | -1
}>(), {
  students: () => [],
  loading: false,
  totalRecords: 0,
  totalPages: 0,
  page: 0,
  rowsPerPage: 10,
  sortField: 'studentCode',
  sortOrder: 1,
})

const emit = defineEmits<{
  pageChange: [page: number, pageSize: number]
  sortChange: [field: keyof Student, order: 1 | -1]
  edit: [student: Student]
  delete: [student: Student]
}>()

function handleSort(event: DataTableSortEvent): void {
  const sortableFields: Array<keyof Student> = ['studentCode', 'studentName', 'averageScore']
  const requestedField = typeof event.sortField === 'string' ? event.sortField as keyof Student : undefined
  if (!requestedField || !sortableFields.includes(requestedField)) return
  const order = event.sortOrder === -1 ? -1 : 1
  emit('sortChange', requestedField, order)
}

function handlePage(event: PageState): void {
  emit('pageChange', event.page, event.rows)
}

const goToPageValue = ref<number | null>(null)
const goToPageError = ref('')
const hasPages = computed(() => props.totalPages > 0)

watch(() => [props.page, props.totalPages], () => {
  goToPageValue.value = hasPages.value ? props.page + 1 : null
  goToPageError.value = ''
}, { immediate: true })

function goToPage(): void {
  const requestedPage = goToPageValue.value
  if (!Number.isInteger(requestedPage) || !requestedPage || requestedPage < 1 || requestedPage > props.totalPages) {
    goToPageError.value = `Enter a page from 1 to ${props.totalPages}.`
    return
  }
  goToPageError.value = ''
  emit('pageChange', requestedPage - 1, props.rowsPerPage)
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
      <Column field="dateOfBirth" header="Birthday">
        <template #body="slotProps">
          {{ formatStudentDate(slotProps.data.dateOfBirth) }}
        </template>
      </Column>
      <Column field="address" header="Address" />
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
      :rows-per-page-options="[10, 20, 50]"
      @page="handlePage"
    />
    <div class="go-to-page">
      <label for="go-to-page">Go to page</label>
      <InputNumber id="go-to-page" v-model="goToPageValue" :min="1" :max="props.totalPages" :use-grouping="false" :disabled="!hasPages" inputmode="numeric" @keydown.enter.prevent="goToPage" />
      <span aria-live="polite">/ {{ props.totalPages }}</span>
      <Button label="Go" :disabled="!hasPages" @click="goToPage" />
    </div>
    <p v-if="goToPageError" class="field-error go-to-page-error" role="alert">{{ goToPageError }}</p>
  </div>
</template>
