<script setup lang="ts">
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Tag from 'primevue/tag'

import EmptyState from '@/components/common/EmptyState.vue'
import { formatStudentDate } from '@/utils/studentDate'

export interface NormalizedEnrollmentRecord {
  enrollmentId: number
  classCode: string
  academicYear: string | number
  enrolledAt: string
  status: string
  transfers: Array<{
    transferId: number
    fromClass: string
    toClass: string
    effectiveAt: string
    reason: string
  }>
}

defineProps<{
  activeEnrollment: NormalizedEnrollmentRecord | null
  enrollmentCount: number
  allTransfers: NormalizedEnrollmentRecord['transfers']
}>()

function getEnrollmentStatusLabel(status: string): string {
  return ({ ACTIVE: 'Đang học', COMPLETED: 'Đã hoàn thành', WITHDRAWN: 'Đã rút khỏi lớp' } as Record<string, string>)[status] ?? 'Chưa xác định'
}
</script>

<template>
  <div class="tab-stack">
    <section v-if="activeEnrollment" class="content-surface active-enrollment-card">
      <div class="card-header">
        <h2>Thông tin lớp học hiện tại</h2>
      </div>
      <div class="active-class-grid">
        <div class="active-class-pill">
          <span class="label">Lớp học:</span>
          <strong class="text-xl text-primary">{{ activeEnrollment.classCode }}</strong>
        </div>
        <div class="active-class-pill">
          <span class="label">Năm học:</span>
          <strong>{{ activeEnrollment.academicYear }}</strong>
        </div>
        <div class="active-class-pill">
          <span class="label">Ngày vào lớp:</span>
          <strong>{{ formatStudentDate(activeEnrollment.enrolledAt) }}</strong>
        </div>
        <div class="active-class-pill">
          <span class="label">Trạng thái:</span>
          <Tag :value="getEnrollmentStatusLabel(activeEnrollment.status)" severity="success" />
        </div>
      </div>
    </section>

    <section class="content-surface">
      <div class="card-header">
        <h2>Lịch sử chuyển lớp & biến động học vụ</h2>
        <p class="section-caption">Ghi nhận chi tiết các lần chuyển lớp trong suốt quá trình học.</p>
      </div>

      <DataTable v-if="allTransfers.length > 0" :value="allTransfers" class="transfers-table">
        <Column field="fromClass" header="Lớp chuyển từ" />
        <Column field="toClass" header="Lớp chuyển đến" />
        <Column field="effectiveAt" header="Ngày hiệu lực">
          <template #body="{ data }">
            {{ formatStudentDate(data.effectiveAt) }}
          </template>
        </Column>
        <Column field="reason" header="Lý do chuyển lớp" />
      </DataTable>

      <EmptyState
        v-else-if="enrollmentCount > 0"
        icon="pi pi-arrows-alt"
        heading="Không có dữ liệu chuyển lớp"
        message="Học sinh chưa từng chuyển lớp kể từ khi nhập học."
      />

      <EmptyState
        v-else
        icon="pi pi-folder-open"
        heading="Chưa có dữ liệu phân lớp"
        message="Học sinh hiện chưa được xếp vào lớp học nào."
      />
    </section>
  </div>
</template>

<style scoped>
.tab-stack {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
.card-header {
  margin-bottom: 1.25rem;
}
.card-header h2 {
  margin: 0 0 0.25rem;
  font-size: 1.25rem;
  font-weight: 600;
}
.section-caption {
  margin: 0;
  font-size: 0.875rem;
  color: var(--text-color-secondary, #64748b);
}
.active-class-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
}
.active-class-pill {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.active-class-pill .label {
  font-size: 0.8125rem;
  color: var(--text-color-secondary, #64748b);
}
</style>
