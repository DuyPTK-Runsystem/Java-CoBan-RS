<script setup lang="ts">
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'

import StatusTag from '@/components/StatusTag.vue'
import type { ApplicationScope, Subject, SubjectStatus, SubjectType } from '@/types/academic'

const props = withDefaults(defineProps<{ subjects?: Subject[]; loading?: boolean }>(), { subjects: () => [], loading: false })
const emit = defineEmits<{ edit: [subject: Subject]; configureApplicability: [subject: Subject] }>()

const typeLabels: Record<SubjectType, string> = { ACADEMIC: 'CHÍNH KHÓA', SKILL: 'KỸ NĂNG' }
const scopeLabels: Record<ApplicationScope, string> = { GRADE: 'Theo khối', CLASS: 'Theo lớp' }
const statusLabels: Record<SubjectStatus, string> = { ACTIVE: 'Đang giảng dạy', INACTIVE: 'Tạm ngưng giảng dạy' }
</script>

<template>
  <div class="table-shell catalog-table">
    <DataTable :value="props.subjects" :loading="props.loading" data-key="id" striped-rows responsive-layout="scroll">
      <template #empty><div class="empty-state"><i class="pi pi-book" aria-hidden="true" /><strong>Chưa có môn học</strong><p>Chưa có môn học phù hợp với bộ lọc hiện tại.</p></div></template>
      <Column header="#" style="width: 4rem"><template #body="slotProps">{{ slotProps.index + 1 }}</template></Column>
      <Column header="Môn học"><template #body="slotProps"><div class="primary-cell"><strong>{{ slotProps.data.name }}</strong><span>{{ slotProps.data.code }}</span></div></template></Column>
      <Column header="Loại"><template #body="slotProps"><StatusTag :label="typeLabels[slotProps.data.subjectType]" :severity="slotProps.data.subjectType === 'ACADEMIC' ? 'success' : 'secondary'" /></template></Column>
      <Column header="Phạm vi áp dụng"><template #body="slotProps">{{ scopeLabels[slotProps.data.applicationScope] }}</template></Column>
      <Column header="Trạng thái"><template #body="slotProps"><StatusTag :label="statusLabels[slotProps.data.status]" :severity="slotProps.data.status === 'ACTIVE' ? 'success' : 'danger'" /></template></Column>
      <Column header="Thao tác" style="width: 17rem"><template #body="slotProps"><div class="table-actions"><Button :icon="slotProps.data.status === 'ACTIVE' ? 'pi pi-pencil' : 'pi pi-eye'" text rounded :aria-label="slotProps.data.status === 'ACTIVE' ? 'Sửa môn học' : 'Xem môn học'" :title="slotProps.data.status === 'ACTIVE' ? 'Sửa môn học' : 'Xem môn học'" @click="emit('edit', slotProps.data)" /><Button v-if="slotProps.data.status === 'ACTIVE'" icon="pi pi-sliders-h" text rounded aria-label="Cấu hình phạm vi áp dụng" title="Cấu hình phạm vi áp dụng" @click="emit('configureApplicability', slotProps.data)" /></div></template></Column>
    </DataTable>
  </div>
</template>
