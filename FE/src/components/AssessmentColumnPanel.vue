<script setup lang="ts">
import { computed } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import { type AssessmentColumn, compareAssessmentColumns } from '@/types/scorebook'

const props = defineProps<{ columns: AssessmentColumn[]; readOnly?: boolean }>()
defineEmits<{ create: []; edit: [column: AssessmentColumn]; deactivate: [column: AssessmentColumn] }>()

const displayedColumns = computed(() => [...props.columns].sort(compareAssessmentColumns))
</script>

<template>
  <section class="content-surface">
    <div class="section-heading"><div><h2>Cấu hình assessment column</h2><p class="section-caption">Tên cột, loại và thứ tự theo contract backend.</p></div><Button label="Thêm cột" icon="pi pi-plus" :disabled="readOnly" @click="$emit('create')" /></div>
    <div v-if="columns.length === 0" class="empty-state"><i class="pi pi-inbox" aria-hidden="true" /><p>Chưa có assessment column.</p></div>
    <DataTable v-else :value="displayedColumns" striped-rows responsive-layout="scroll"><Column field="columnNo" header="#" /><Column field="assessmentType" header="Loại" /><Column field="columnName" header="Tên cột" /><Column field="status" header="Trạng thái" /><Column header="Thao tác"><template #body="slot"><Button label="Sửa" text size="small" :disabled="readOnly" @click="$emit('edit', slot.data)" /><Button label="Vô hiệu hóa" text severity="danger" size="small" :disabled="readOnly || slot.data.status !== 'ACTIVE'" @click="$emit('deactivate', slot.data)" /></template></Column></DataTable>
  </section>
</template>
