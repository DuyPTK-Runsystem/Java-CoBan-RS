<script setup lang="ts">
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'

import type { ClassStudent } from '@/types/enrollment'

const props = withDefaults(defineProps<{
  students?: ClassStudent[]
  loading?: boolean
  readOnly?: boolean
}>(), {
  students: () => [],
  loading: false,
  readOnly: false,
})

const emit = defineEmits<{
  transfer: [student: ClassStudent]
  history: [student: ClassStudent]
}>()
</script>

<template>
  <div class="table-shell enrollment-table">
    <div class="table-toolbar">
      <div>
        <h2>Danh sách học sinh trong lớp</h2>
      </div>
    </div>
    <DataTable :value="props.students" :loading="props.loading" data-key="enrollmentId" striped-rows responsive-layout="scroll">
      <template #empty><div class="empty-state"><i class="pi pi-users" aria-hidden="true" /><strong>Lớp chưa có học sinh</strong><p>Học sinh được xếp vào lớp này sẽ hiển thị tại đây.</p></div></template>
      <Column header="#"><template #body="slotProps">{{ slotProps.index + 1 }}</template></Column>
      <Column header="Mã học sinh"><template #body="slotProps"><strong>{{ slotProps.data.studentCode }}</strong></template></Column>
      <Column field="studentName" header="Họ và tên" />
      <Column header="Thao tác" style="width: 10rem">
        <template #body="slotProps">
          <div class="table-actions">
            <Button icon="pi pi-arrow-right-arrow-left" text rounded :disabled="props.readOnly" aria-label="Chuyển lớp" title="Chuyển lớp" @click="emit('transfer', slotProps.data)" />
            <Button icon="pi pi-history" text rounded severity="secondary" aria-label="Xem lịch sử học sinh" title="Xem lịch sử học sinh" @click="emit('history', slotProps.data)" />
          </div>
        </template>
      </Column>
    </DataTable>
  </div>
</template>
