<script setup lang="ts">
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Tag from 'primevue/tag'

import type { PlacementIssueSeverity, PlacementResult, PlacementResultStatus } from '@/types/placement'

const props = defineProps<{
  visible: boolean
  result: PlacementResult | null
  studentLabel?: string
  targetClassLabel?: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'continue-manual': []
}>()

const resultLabels: Record<PlacementResultStatus, string> = {
  AUTO_ASSIGNED: 'Tự động',
  MANUAL_REQUIRED: 'Cần xếp thủ công',
}

const severityLabels: Record<PlacementIssueSeverity, string> = {
  WARNING: 'Cảnh báo',
  BLOCKING: 'Chặn xếp lớp',
}

const issueLabels: Record<string, string> = {
  CAPACITY_EXCEEDED: 'Vượt sĩ số cho phép',
  MISSING_DATA: 'Thiếu dữ liệu cần thiết',
}

function closeDialog(): void {
  emit('update:visible', false)
}

function handleContinueManual(): void {
  closeDialog()
  emit('continue-manual')
}
</script>

<template>
  <Dialog
    :visible="props.visible"
    modal
    header="Chi tiết kết quả xếp lớp"
    :style="{ width: '520px', maxWidth: '95vw' }"
    @update:visible="emit('update:visible', $event)"
  >
    <div v-if="props.result" class="placement-result-detail">
      <div class="detail-row">
        <span class="detail-label">Học sinh:</span>
        <strong class="detail-value">{{ props.studentLabel || `Học sinh #${props.result.studentId}` }}</strong>
      </div>

      <div class="detail-row">
        <span class="detail-label">Lớp đề xuất:</span>
        <span class="detail-value">{{ props.targetClassLabel || (props.result.targetClassId ? `Lớp #${props.result.targetClassId}` : '—') }}</span>
      </div>

      <div class="detail-row">
        <span class="detail-label">Trạng thái:</span>
        <div class="detail-value">
          <Tag
            :value="resultLabels[props.result.resultStatus]"
            :severity="props.result.resultStatus === 'AUTO_ASSIGNED' ? 'success' : 'warn'"
          />
        </div>
      </div>

      <div v-if="props.result.score !== null && props.result.score !== undefined" class="detail-row">
        <span class="detail-label">Điểm dùng để xếp lớp:</span>
        <span class="detail-value">{{ props.result.score }}</span>
      </div>

      <div v-if="props.result.issueCode" class="detail-row">
        <span class="detail-label">Vấn đề:</span>
        <span class="detail-value">{{ issueLabels[props.result.issueCode] || 'Có dữ liệu cần xử lý' }}</span>
      </div>

      <div v-if="props.result.issueSeverity" class="detail-row">
        <span class="detail-label">Mức độ:</span>
        <div class="detail-value">
          <Tag
            :value="severityLabels[props.result.issueSeverity] || props.result.issueSeverity"
            :severity="props.result.issueSeverity === 'BLOCKING' ? 'danger' : 'warn'"
          />
        </div>
      </div>

      <div class="detail-explanation">
        <span class="detail-label">Giải thích:</span>
        <p class="explanation-box">{{ props.result.explanation || 'Không có ghi chú thêm.' }}</p>
      </div>

      <div v-if="props.result.resultStatus === 'MANUAL_REQUIRED'" class="manual-assist-banner">
        <p>Học sinh này chưa được xếp lớp tự động. Giáo vụ có thể xếp lớp trực tiếp qua danh sách học sinh chưa xếp lớp.</p>
        <Button
          label="Tiếp tục xếp thủ công"
          icon="pi pi-external-link"
          severity="secondary"
          size="small"
          @click="handleContinueManual"
        />
      </div>
    </div>

    <template #footer>
      <Button label="Đóng" severity="secondary" text @click="closeDialog" />
    </template>
  </Dialog>
</template>

<style scoped>
.placement-result-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 4px 0;
}
.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--p-content-border-color, #e5e7eb);
}
.detail-label {
  color: var(--p-text-muted-color, #6b7280);
  font-size: 0.9rem;
}
.detail-value {
  font-size: 0.95rem;
}
.font-mono {
  font-family: monospace;
}
.detail-explanation {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 4px;
}
.explanation-box {
  margin: 0;
  padding: 10px 12px;
  background-color: var(--p-surface-100, #f3f4f6);
  border-radius: 6px;
  color: var(--p-text-color, #1f2937);
  font-size: 0.9rem;
  line-height: 1.4;
}
.manual-assist-banner {
  margin-top: 8px;
  padding: 12px;
  background-color: var(--p-amber-50, #fffbeb);
  border: 1px solid var(--p-amber-200, #fde68a);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.manual-assist-banner p {
  margin: 0;
  font-size: 0.85rem;
  color: var(--p-amber-900, #78350f);
}
</style>
