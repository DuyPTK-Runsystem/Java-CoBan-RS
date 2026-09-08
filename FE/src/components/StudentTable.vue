<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable, { type DataTableSortEvent } from 'primevue/datatable'
import InputNumber from 'primevue/inputnumber'
import Paginator, { type PageState } from 'primevue/paginator'
import Tag from 'primevue/tag'

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
  canManageStudents?: boolean
}>(), {
  students: () => [],
  loading: false,
  totalRecords: 0,
  totalPages: 0,
  page: 0,
  rowsPerPage: 10,
  sortField: 'studentCode',
  sortOrder: 1,
  canManageStudents: false,
})

const emit = defineEmits<{
  (e: 'pageChange', page: number, pageSize: number): void
  (e: 'sortChange', field: keyof Student, order: 1 | -1): void
  (e: 'edit', student: Student): void
  (e: 'delete', student: Student): void
  (e: 'viewDetail', student: Student): void
}>()

function handleSort(event: DataTableSortEvent): void {
  const sortableFields: Array<keyof Student> = ['studentCode', 'studentName']
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
    goToPageError.value = `Nhập số trang từ 1 đến ${props.totalPages}.`
    return
  }
  goToPageError.value = ''
  emit('pageChange', requestedPage - 1, props.rowsPerPage)
}

function getStatusSeverity(status?: string | null): 'success' | 'secondary' | 'info' {
  if (status === 'INACTIVE') return 'secondary'
  if (status === 'GRADUATED') return 'info'
  return 'success'
}

function getStatusLabel(status?: string | null): string {
  if (status === 'INACTIVE') return 'Ngừng học'
  if (status === 'GRADUATED') return 'Tốt nghiệp'
  return 'Đang học'
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
          <span>Không có học sinh phù hợp với điều kiện tìm kiếm.</span>
        </div>
      </template>
      <Column header="STT" style="width: 4rem">
        <template #body="slotProps">
          {{ props.page * props.rowsPerPage + slotProps.index + 1 }}
        </template>
      </Column>
      <Column field="studentCode" header="Mã học sinh" sortable>
        <template #body="slotProps">
          <a
            class="student-link"
            href="#"
            @click.prevent="emit('viewDetail', slotProps.data)"
          >
            {{ slotProps.data.studentCode }}
          </a>
        </template>
      </Column>
      <Column field="studentName" header="Họ và tên" sortable>
        <template #body="slotProps">
          <a
            class="student-link font-medium"
            href="#"
            @click.prevent="emit('viewDetail', slotProps.data)"
          >
            {{ slotProps.data.studentName }}
          </a>
        </template>
      </Column>
      <Column field="dateOfBirth" header="Ngày sinh">
        <template #body="slotProps">
          {{ formatStudentDate(slotProps.data.dateOfBirth) }}
        </template>
      </Column>
      <Column field="currentClassCode" header="Lớp">
        <template #body="slotProps">
          <span v-if="slotProps.data.currentClassCode" class="class-badge">
            {{ slotProps.data.currentClassCode }}
          </span>
          <span v-else class="text-secondary text-sm">
            Chưa xếp lớp
          </span>
        </template>
      </Column>
      <Column field="status" header="Trạng thái">
        <template #body="slotProps">
          <Tag
            :value="getStatusLabel(slotProps.data.status)"
            :severity="getStatusSeverity(slotProps.data.status)"
          />
        </template>
      </Column>
      <Column field="address" header="Địa chỉ" />
      <Column header="Thao tác" style="width: 10rem">
        <template #body="slotProps">
          <div class="table-actions">
            <Button
              icon="pi pi-eye"
              text
              rounded
              aria-label="Xem chi tiết học sinh"
              title="Xem chi tiết"
              @click="emit('viewDetail', slotProps.data)"
            />
            <Button
              v-if="props.canManageStudents"
              icon="pi pi-pencil"
              text
              rounded
              aria-label="Chỉnh sửa học sinh"
              title="Chỉnh sửa"
              @click="emit('edit', slotProps.data)"
            />
            <Button
              v-if="props.canManageStudents"
              icon="pi pi-trash"
              text
              rounded
              severity="danger"
              aria-label="Xóa học sinh"
              title="Xóa hoặc chuyển trạng thái"
              @click="emit('delete', slotProps.data)"
            />
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
      <label for="go-to-page">Đến trang</label>
      <InputNumber
        id="go-to-page"
        v-model="goToPageValue"
        :min="1"
        :max="props.totalPages"
        :use-grouping="false"
        :disabled="!hasPages"
        inputmode="numeric"
        @keydown.enter.prevent="goToPage"
      />
      <span aria-live="polite">/ {{ props.totalPages }}</span>
      <Button label="Đi đến" :disabled="!hasPages" @click="goToPage" />
    </div>
    <p v-if="goToPageError" class="field-error go-to-page-error" role="alert">{{ goToPageError }}</p>
  </div>
</template>

<style scoped>
.student-link {
  color: var(--primary-color, #3b82f6);
  text-decoration: none;
  font-weight: 500;
  cursor: pointer;
}
.student-link:hover {
  text-decoration: underline;
}
.class-badge {
  display: inline-block;
  padding: 0.15rem 0.5rem;
  font-size: 0.8125rem;
  font-weight: 600;
  background-color: var(--surface-100, #f1f5f9);
  color: var(--text-color, #1e293b);
  border-radius: 4px;
}
.text-secondary {
  color: var(--text-color-secondary, #64748b);
}
.text-sm {
  font-size: 0.8125rem;
}
</style>
