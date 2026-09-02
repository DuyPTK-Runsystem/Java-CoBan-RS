<script setup lang="ts">
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import type { ScoreChangeRequestDetail } from '@/types/scoreChangeRequest'

const props = defineProps<{ detail: ScoreChangeRequestDetail; canReview: boolean; canCancel: boolean; loading?: boolean }>()
const emit = defineEmits<{ approve: []; reject: []; cancel: [] }>()
const statusLabels: Record<ScoreChangeRequestDetail['status'], string> = { PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Bị từ chối', CANCELLED: 'Đã hủy', APPLIED: 'Đã áp dụng' }
const scoreLabels: Record<ScoreChangeRequestDetail['beforeStatus'], string> = { UNSCORED: 'Chưa nhập', SCORED: 'Có điểm', ABSENT: 'Vắng', EXEMPTED: 'Được miễn', CANCELLED: 'Hủy' }
function displayValue(value: number | null, status: ScoreChangeRequestDetail['beforeStatus']): string { return value === null ? scoreLabels[status] : String(value) }
</script>

<template>
  <section class="score-change-detail" aria-label="Chi tiết yêu cầu sửa điểm">
    <div class="section-heading"><div><h2>Chi tiết yêu cầu sửa điểm</h2><p class="section-caption">{{ props.detail.studentCode }} · {{ props.detail.studentName }}</p></div><Tag :value="statusLabels[props.detail.status]" /></div>
    <dl class="detail-grid"><div><dt>Điểm hiện tại</dt><dd>{{ displayValue(props.detail.beforeValue, props.detail.beforeStatus) }}</dd></div><div><dt>Điểm đề xuất</dt><dd>{{ displayValue(props.detail.proposedValue, props.detail.proposedStatus) }}</dd></div><div><dt>Lý do</dt><dd>{{ props.detail.reason }}</dd></div><div v-if="props.detail.rejectionReason"><dt>Lý do từ chối</dt><dd>{{ props.detail.rejectionReason }}</dd></div><div><dt>Thời điểm gửi</dt><dd>{{ props.detail.requestedAt }}</dd></div></dl>
    <div v-if="props.canReview && props.detail.status === 'PENDING'" class="dialog-actions"><Button label="Từ chối" severity="danger" outlined :disabled="props.loading" @click="emit('reject')" /><Button label="Duyệt và áp dụng" icon="pi pi-check" :loading="props.loading" @click="emit('approve')" /></div>
    <Button v-if="props.canCancel && props.detail.status === 'PENDING'" label="Hủy yêu cầu" severity="secondary" outlined :loading="props.loading" @click="emit('cancel')" />
  </section>
</template>
