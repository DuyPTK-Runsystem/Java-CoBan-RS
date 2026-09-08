<script setup lang="ts">
import { computed } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import { type AssessmentColumn, compareAssessmentColumns } from '@/types/scorebook'

const props = defineProps<{ columns: AssessmentColumn[]; readOnly?: boolean }>()
defineEmits<{ create: []; edit: [column: AssessmentColumn]; deactivate: [column: AssessmentColumn] }>()

const displayedColumns = computed(() => [...props.columns].sort(compareAssessmentColumns))
const assessmentTypeLabels: Record<string, string> = { KTTT: 'Thường xuyên', 'KTĐK': 'Giữa kỳ', KTCK: 'Cuối kỳ' }
</script>

<template>
  <section class="content-surface">
    <div class="section-heading"><div><h2>Cấu hình cột điểm</h2></div><Button label="Thêm cột" icon="pi pi-plus" :disabled="readOnly" @click="$emit('create')" /></div>
    <div v-if="columns.length === 0" class="empty-state"><i class="pi pi-inbox" aria-hidden="true" /><p>Chưa có cột điểm.</p></div>
    <DataTable v-else :value="displayedColumns" striped-rows responsive-layout="scroll"><Column field="columnNo" header="#" /><Column header="Loại"><template #body="slot">{{ assessmentTypeLabels[slot.data.assessmentType] ?? slot.data.assessmentType }}</template></Column><Column field="columnName" header="Tên cột" /><Column field="status" header="Trạng thái" /><Column header="Thao tác"><template #body="slot"><Button label="Sửa" text size="small" :disabled="readOnly" @click="$emit('edit', slot.data)" /><Button label="Ngừng sử dụng" text severity="danger" size="small" :disabled="readOnly || slot.data.status !== 'ACTIVE'" @click="$emit('deactivate', slot.data)" /></template></Column></DataTable>
  </section>
</template>
