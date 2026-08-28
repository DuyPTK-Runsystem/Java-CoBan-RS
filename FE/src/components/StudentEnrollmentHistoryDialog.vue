<script setup lang="ts">
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'

import FormAlert from '@/components/FormAlert.vue'
import StatusTag from '@/components/StatusTag.vue'
import type { SchoolClass } from '@/types/academic'
import type { EnrollmentStatus, StudentEnrollmentHistory } from '@/types/enrollment'
import type { LoadingState } from '@/types/ui'

const props = withDefaults(defineProps<{
  visible?: boolean
  studentCode?: string
  studentName?: string
  history?: StudentEnrollmentHistory[]
  loading?: boolean
  errorMessage?: string
  classes?: SchoolClass[]
}>(), {
  visible: false,
  studentCode: '',
  studentName: '',
  history: () => [],
  loading: false,
  errorMessage: '',
  classes: () => [],
})

const emit = defineEmits<{ 'update:visible': [visible: boolean] }>()
const statusLabels: Record<EnrollmentStatus, string> = { ACTIVE: 'Đang học', COMPLETED: 'Đã hoàn thành', WITHDRAWN: 'Đã thôi học' }
const statusSeverities: Record<EnrollmentStatus, 'success' | 'secondary' | 'danger'> = { ACTIVE: 'success', COMPLETED: 'secondary', WITHDRAWN: 'danger' }

function formatDate(value: string | null): string { return value ? new Date(value).toLocaleString('vi-VN') : '—' }
function classLabel(classId: number | null): string {
  if (classId === null) return '—'
  return props.classes.find((schoolClass) => schoolClass.id === classId)?.classCode ?? `Lớp #${classId}`
}
function pageState(): LoadingState { return props.loading ? 'loading' : props.errorMessage ? 'error' : props.history.length ? 'success' : 'empty' }
</script>

<template>
  <Dialog :visible="props.visible" modal header="Lịch sử enrollment học sinh" :style="{ width: 'min(100% - 2rem, 860px)' }" @update:visible="emit('update:visible', $event)">
    <div class="status-dialog-content">
      <div class="status-dialog-heading"><div><p class="eyebrow">Read-only history</p><h2>{{ props.studentCode }} · {{ props.studentName }}</h2><p class="dialog-caption">Lịch sử enrollment và transfer được hiển thị theo thứ tự backend trả về.</p></div><i class="pi pi-history" aria-hidden="true" /></div>
      <FormAlert v-if="pageState() === 'error'" tone="error" :message="props.errorMessage" />
      <div v-else-if="pageState() === 'loading'" class="page-state page-state-loading" role="status"><i class="pi pi-spin pi-spinner" aria-hidden="true" /><span>Đang tải lịch sử...</span></div>
      <div v-else-if="pageState() === 'empty'" class="empty-state"><i class="pi pi-history" aria-hidden="true" /><strong>Chưa có lịch sử enrollment</strong><p>Học sinh chưa có bản ghi xếp lớp nào.</p></div>
      <div v-else class="history-list">
        <article v-for="item in props.history" :key="item.enrollment.id" class="history-card">
          <div class="section-heading"><div><h3>{{ item.enrollment.currentClassCode }}</h3><p class="section-caption">Năm học #{{ item.enrollment.academicYearId }} · vào lớp {{ formatDate(item.enrollment.enrolledAt) }}</p></div><StatusTag :label="statusLabels[item.enrollment.status]" :severity="statusSeverities[item.enrollment.status]" /></div>
          <dl class="detail-grid"><div class="detail-item"><span>Student code</span><strong>{{ item.enrollment.studentCode }}</strong></div><div class="detail-item"><span>Hoàn thành</span><strong>{{ formatDate(item.enrollment.completedAt) }}</strong></div></dl>
          <div class="timeline-block"><div class="section-heading"><h3>Lịch sử chuyển lớp</h3><span class="section-caption">{{ item.transfers.length }} lần chuyển</span></div><ol v-if="item.transfers.length" class="history-transfer-list"><li v-for="transfer in item.transfers" :key="transfer.transferId"><strong>{{ classLabel(transfer.fromClassId) }} → {{ classLabel(transfer.toClassId) }}</strong><span>{{ formatDate(transfer.effectiveAt) }} · {{ transfer.reason || 'Không ghi lý do' }}</span></li></ol><p v-else class="section-caption">Chưa có lần chuyển lớp nào.</p></div>
        </article>
      </div>
      <div class="form-actions"><Button label="Đóng" icon="pi pi-times" severity="secondary" outlined @click="emit('update:visible', false)" /></div>
    </div>
  </Dialog>
</template>
