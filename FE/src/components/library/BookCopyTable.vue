<script setup lang="ts">
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'

import StatusTag from '@/components/common/StatusTag.vue'
import type { BookCopy, BookCopyStatus } from '@/types/library/catalog'
import { BOOK_COPY_STATUS_LABELS } from '@/types/library/catalog'

const props = withDefaults(defineProps<{
  copies?: BookCopy[]
  loading?: boolean
  canManage?: boolean
  actionsDisabled?: boolean
  selection?: BookCopy[]
}>(), { copies: () => [], loading: false, canManage: false, actionsDisabled: false, selection: () => [] })
const emit = defineEmits<{
  'update:selection': [selected: BookCopy[]]
  edit: [copy: BookCopy]
  setStatus: [copy: BookCopy, status: 'AVAILABLE' | 'DAMAGED']
  withdraw: [copy: BookCopy]
  barcode: [copy: BookCopy]
}>()

function onSelectionChange(value: BookCopy[]): void {
  emit('update:selection', value)
}

function severity(status: BookCopyStatus): 'success' | 'warn' | 'danger' | 'secondary' {
  if (status === 'AVAILABLE') return 'success'
  if (status === 'DAMAGED') return 'warn'
  if (status === 'WITHDRAWN' || status === 'LOST') return 'danger'
  return 'secondary'
}

</script>

<template>
  <div class="table-shell library-copy-table">
    <DataTable :value="props.copies" :loading="props.loading" :selection="props.selection" data-key="id" striped-rows responsive-layout="scroll" table-style="min-width: 58rem" @update:selection="onSelectionChange">
      <template #empty><div class="empty-state"><i class="pi pi-barcode" aria-hidden="true" /><strong>Chưa có bản sao</strong><p>Thêm bản sao để quản lý vị trí và trạng thái từng quyển sách.</p></div></template>
      <Column selection-mode="multiple" header-style="width: 3rem" />
      <Column field="barcode" header="Mã vạch" />
      <Column field="shelfLocation" header="Vị trí kệ"><template #body="{ data }">{{ data.shelfLocation || 'Chưa xếp kệ' }}</template></Column>
      <Column header="Trạng thái"><template #body="{ data }"><StatusTag :label="BOOK_COPY_STATUS_LABELS[data.status]" :severity="severity(data.status)" /></template></Column>
      <Column header="Hình thức"><template #body="{ data }">{{ data.referenceOnly ? 'Chỉ đọc tại chỗ' : 'Có thể cho mượn' }}</template></Column>
      <Column header="Thao tác" style="width: 16rem">
        <template #body="{ data }">
          <div class="table-actions">
            <Button v-if="props.canManage && ['AVAILABLE', 'DAMAGED'].includes(data.status)" icon="pi pi-pencil" text rounded aria-label="Sửa thông tin bản sao" title="Sửa thông tin bản sao" :disabled="props.actionsDisabled" @click="emit('edit', data)" />
            <Button v-if="props.canManage && data.status === 'AVAILABLE'" icon="pi pi-exclamation-triangle" text rounded aria-label="Đánh dấu hư hỏng" title="Đánh dấu hư hỏng" :disabled="props.actionsDisabled" @click="emit('setStatus', data, 'DAMAGED')" />
            <Button v-if="props.canManage && data.status === 'DAMAGED'" icon="pi pi-check-circle" text rounded aria-label="Đánh dấu có sẵn" title="Đánh dấu có sẵn" :disabled="props.actionsDisabled" @click="emit('setStatus', data, 'AVAILABLE')" />
            <Button v-if="props.canManage && ['AVAILABLE', 'DAMAGED'].includes(data.status)" icon="pi pi-sign-out" text rounded severity="danger" aria-label="Rút bản sao khỏi kho" title="Rút bản sao khỏi kho" :disabled="props.actionsDisabled" @click="emit('withdraw', data)" />
            <Button icon="pi pi-barcode" text rounded aria-label="Xem mã vạch" title="Xem mã vạch" :disabled="props.actionsDisabled" @click="emit('barcode', data)" />
          </div>
        </template>
      </Column>
    </DataTable>
  </div>
</template>
