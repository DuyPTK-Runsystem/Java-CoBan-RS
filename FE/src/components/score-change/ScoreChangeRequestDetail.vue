<script setup lang="ts">
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import type { ScoreChangeRequestDetail } from '@/types/scoreChangeRequest'
import { formatScoreChangeRequestDateTime } from '@/utils/scoreChangeRequestDate'

const props = defineProps<{ detail: ScoreChangeRequestDetail; canReview: boolean; canCancel: boolean; loading?: boolean }>()
const emit = defineEmits<{ approve: []; reject: []; cancel: [] }>()
const statusLabels: Record<ScoreChangeRequestDetail['status'], string> = { PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Bị từ chối', CANCELLED: 'Đã hủy', APPLIED: 'Đã áp dụng' }
const scoreLabels: Record<ScoreChangeRequestDetail['beforeStatus'], string> = { UNSCORED: 'Chưa nhập', SCORED: 'Có điểm', ABSENT: 'Vắng', EXEMPTED: 'Được miễn', CANCELLED: 'Hủy' }
function displayValue(value: number | null, status: ScoreChangeRequestDetail['beforeStatus']): string { return value === null ? scoreLabels[status] : String(value) }
</script>

<template>
  <section class="score-change-detail" aria-label="Chi tiết yêu cầu sửa điểm">
    <header class="detail-header">
      <div>
        <h2 class="detail-title">Chi tiết yêu cầu sửa điểm</h2>
        <p class="detail-caption">{{ props.detail.studentCode }} · {{ props.detail.studentName }}</p>
      </div>
      <Tag class="detail-status" :value="statusLabels[props.detail.status]" />
    </header>

    <dl class="detail-card-grid">
      <div class="detail-card">
        <dt class="detail-label">Điểm hiện tại</dt>
        <dd class="detail-value">{{ displayValue(props.detail.beforeValue, props.detail.beforeStatus) }}</dd>
      </div>
      <div class="detail-card">
        <dt class="detail-label">Điểm đề xuất</dt>
        <dd class="detail-value">{{ displayValue(props.detail.proposedValue, props.detail.proposedStatus) }}</dd>
      </div>
      <div class="detail-card detail-card--wide">
        <dt class="detail-label">Lý do</dt>
        <dd class="detail-value detail-value--multiline">{{ props.detail.reason }}</dd>
      </div>
      <div v-if="props.detail.rejectionReason" class="detail-card detail-card--wide">
        <dt class="detail-label">Lý do từ chối</dt>
        <dd class="detail-value detail-value--multiline">{{ props.detail.rejectionReason }}</dd>
      </div>
      <div class="detail-card">
        <dt class="detail-label">Thời điểm gửi</dt>
        <dd class="detail-value">{{ formatScoreChangeRequestDateTime(props.detail.requestedAt) }}</dd>
      </div>
    </dl>

    <footer v-if="(props.canReview || props.canCancel) && props.detail.status === 'PENDING'" class="detail-actions">
      <Button v-if="props.canCancel" class="detail-action" label="Hủy yêu cầu" severity="secondary" outlined :disabled="props.loading" @click="emit('cancel')" />
      <Button v-if="props.canReview" class="detail-action" label="Từ chối" severity="danger" outlined :disabled="props.loading" @click="emit('reject')" />
      <Button v-if="props.canReview" class="detail-action detail-action--primary" label="Duyệt và áp dụng" icon="pi pi-check" :loading="props.loading" @click="emit('approve')" />
    </footer>
  </section>
</template>

<style scoped>
.score-change-detail { color: #172b4d; }
.detail-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.detail-title { margin: 0; font-size: 1.2rem; line-height: 1.35; }
.detail-caption { margin: 6px 0 0; color: #64748b; font-size: .9rem; }
.detail-status { flex: 0 0 auto; }
.detail-card-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin: 0; }
.detail-card { min-width: 0; padding: 14px 16px; border: 1px solid #d9e2ec; border-radius: 10px; background: #f8fafc; }
.detail-card--wide { grid-column: 1 / -1; }
.detail-label { margin: 0; color: #64748b; font-size: .82rem; font-weight: 600; line-height: 1.35; }
.detail-value { display: block; margin: 7px 0 0; color: #172b4d; font-size: .98rem; font-weight: 500; line-height: 1.45; overflow-wrap: anywhere; }
.detail-value--multiline { white-space: pre-wrap; }
.detail-actions { display: flex; align-items: center; justify-content: flex-end; gap: 10px; margin-top: 20px; padding-top: 16px; border-top: 1px solid #d9e2ec; }
.detail-action { flex: 0 0 auto; }
@media (max-width: 560px) {
  .detail-header { align-items: flex-start; gap: 10px; }
  .detail-title { font-size: 1.08rem; }
  .detail-card-grid { grid-template-columns: 1fr; }
  .detail-card--wide { grid-column: auto; }
  .detail-actions { align-items: stretch; flex-direction: column-reverse; }
  .detail-action { width: 100%; }
}
</style>
