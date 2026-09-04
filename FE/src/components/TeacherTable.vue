<script setup lang="ts">
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import StatusTag from '@/components/StatusTag.vue'
import type { Teacher, TeacherStatus } from '@/types/teacher'

const props = withDefaults(defineProps<{ teachers?: Teacher[]; loading?: boolean }>(), { teachers: () => [], loading: false })
const emit = defineEmits<{ view: [teacher: Teacher]; edit: [teacher: Teacher]; delete: [teacher: Teacher] }>()
const labels: Record<TeacherStatus, string> = { ACTIVE: 'Đang công tác', ON_LEAVE: 'Nghỉ phép', INACTIVE: 'Ngừng công tác' }
const severity: Record<TeacherStatus, 'success' | 'warn' | 'contrast'> = { ACTIVE: 'success', ON_LEAVE: 'warn', INACTIVE: 'contrast' }
function date(value: string | null): string { return value ? value.split('-').reverse().join('/') : '—' }
</script>
<template>
  <div class="table-shell catalog-table teacher-table">
    <DataTable :value="props.teachers" :loading="props.loading" data-key="id" striped-rows responsive-layout="scroll">
      <template #empty><div class="empty-state"><i class="pi pi-id-card" aria-hidden="true" /><strong>Chưa có giáo viên</strong><p>Không có hồ sơ phù hợp với bộ lọc hiện tại.</p></div></template>
      <Column header="#" style="width: 3.5rem"><template #body="slot">{{ slot.index + 1 }}</template></Column>
      <Column header="Mã GV"><template #body="slot"><strong>{{ slot.data.teacherCode }}</strong></template></Column>
      <Column header="Họ và tên"><template #body="slot"><div class="primary-cell"><strong>{{ slot.data.teacherName }}</strong><span>{{ slot.data.gender || 'Chưa cập nhật' }}</span></div></template></Column>
      <Column header="Ngày sinh"><template #body="slot">{{ date(slot.data.dateOfBirth) }}</template></Column>
      <Column header="Tổ chuyên môn"><template #body="slot">{{ slot.data.department || '—' }}</template></Column>
      <Column header="Liên hệ"><template #body="slot"><div class="primary-cell"><span>{{ slot.data.phone || '—' }}</span><span>{{ slot.data.email || '—' }}</span></div></template></Column>
      <Column header="Ngày vào trường"><template #body="slot">{{ date(slot.data.joinDate) }}</template></Column>
      <Column header="Trạng thái"><template #body="slot"><StatusTag :label="labels[slot.data.status]" :severity="severity[slot.data.status]" /></template></Column>
      <Column header="Tài khoản"><template #body="slot">{{ slot.data.userId ? `User #${slot.data.userId}` : 'Chưa liên kết' }}</template></Column>
      <Column header="Thao tác" style="width: 10rem"><template #body="slot"><div class="table-actions"><Button icon="pi pi-eye" text rounded aria-label="Xem chi tiết giáo viên" title="Xem chi tiết" @click="emit('view', slot.data)" /><Button icon="pi pi-pencil" text rounded aria-label="Chỉnh sửa giáo viên" title="Chỉnh sửa" @click="emit('edit', slot.data)" /><Button icon="pi pi-trash" text rounded severity="danger" aria-label="Xóa giáo viên" title="Xóa" @click="emit('delete', slot.data)" /></div></template></Column>
    </DataTable>
  </div>
</template>
