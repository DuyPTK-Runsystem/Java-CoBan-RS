<script setup lang="ts">
import { computed } from 'vue'
import Button from 'primevue/button'
import ProgressBar from 'primevue/progressbar'
import Tag from 'primevue/tag'

import type { ResTranscriptCalculationStatusDTO } from '@/types/transcript'
import { formatCalculationDateTime } from '@/utils/calculationTaskDate'

const props = withDefaults(
  defineProps<{
    status: ResTranscriptCalculationStatusDTO | null
    loading?: boolean
    studentName?: string
  }>(),
  {
    loading: false,
    studentName: '',
  },
)

const emit = defineEmits<{
  refresh: []
}>()

const isInProgress = computed(() => props.status?.calculationStatus === 'IN_PROGRESS')
const isFinish = computed(() => props.status?.calculationStatus === 'FINISH')
const isUpToDate = computed(() => props.status?.isUpToDate ?? false)

const statusBadge = computed(() => {
  if (isInProgress.value) {
    return { label: 'Đang cập nhật (IN_PROGRESS)', severity: 'warn' as const }
  }
  if (isFinish.value) {
    if (isUpToDate.value) {
      return { label: 'Đã cập nhật (FINISH · up-to-date)', severity: 'success' as const }
    }
    return { label: 'Chưa đồng bộ (FINISH · out-of-date)', severity: 'warn' as const }
  }
  return { label: props.status?.calculationStatus ?? 'CHƯA RÕ', severity: 'secondary' as const }
})

const studentDisplay = computed(() => {
  if (props.studentName && props.status?.studentCode) {
    return `${props.studentName} · ${props.status.studentCode}`
  }
  return props.studentName || props.status?.studentCode || 'Học sinh'
})
</script>

<template>
  <div class="transcript-status-card" data-testid="transcript-status-card">
    <div v-if="props.status" class="card-content">
      <div class="status-top-row">
        <div class="title-section">
          <h2 class="student-heading">{{ studentDisplay }}</h2>
          <p class="transcript-meta">
            Bảng điểm học tập · Năm học #{{ props.status.academicYearId }}
            <template v-if="props.status.semesterId"> · Học kỳ #{{ props.status.semesterId }}</template>
          </p>
        </div>
        <div class="badge-and-action">
          <Tag :value="statusBadge.label" :severity="statusBadge.severity" />
          <Button
            label="Làm mới"
            icon="pi pi-refresh"
            severity="secondary"
            size="small"
            :loading="props.loading"
            @click="emit('refresh')"
          />
        </div>
      </div>

      <div v-if="isInProgress" class="progress-container">
        <ProgressBar mode="indeterminate" style="height: 6px" />
        <p class="progress-hint">
          Hệ thống đang xử lý tính toán trong nền. Dữ liệu điểm cũ chưa phải kết quả chính thức mới nhất.
        </p>
      </div>

      <div class="version-row">
        <div class="version-stat">
          <span class="stat-label">Source version:</span>
          <span class="stat-value">{{ props.status.sourceVersion ?? '—' }}</span>
        </div>
        <span class="stat-divider">·</span>
        <div class="version-stat">
          <span class="stat-label">Calculated version:</span>
          <span class="stat-value">{{ props.status.calculatedVersion ?? '—' }}</span>
        </div>
        <span class="stat-divider">·</span>
        <div class="version-stat">
          <span class="stat-label">Thời điểm tính:</span>
          <span class="stat-value">{{ formatCalculationDateTime(props.status.calculatedAt) }}</span>
        </div>
      </div>

      <div v-if="props.status.lastError" class="status-error-box">
        <strong>Lỗi gần nhất:</strong>
        <span>{{ props.status.lastError }}</span>
      </div>

      <div class="notice-info">
        <i class="pi pi-info-circle" />
        <span>
          <strong>Read-only status:</strong> Truy vấn trạng thái không tự kích hoạt lệnh tính toán. Dữ liệu bảng điểm chính thức được tính toán độc lập qua worker nền.
        </span>
      </div>
    </div>

    <div v-else class="empty-status">
      <p class="text-muted">Chưa có thông tin trạng thái bảng điểm. Vui lòng chọn học sinh hoặc năm học.</p>
    </div>
  </div>
</template>

<style scoped>
.transcript-status-card {
  background: #ffffff;
  border: 1px solid #dfe4ea;
  border-radius: 12px;
  padding: 20px 24px;
}

.status-top-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.student-heading {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #182230;
}

.transcript-meta {
  margin: 4px 0 0;
  color: #667085;
  font-size: 13px;
}

.badge-and-action {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-container {
  margin: 16px 0 12px;
}

.progress-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #9a6700;
}

.version-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 14px 0 10px;
  font-size: 13px;
  color: #475467;
  flex-wrap: wrap;
}

.version-stat {
  display: flex;
  gap: 4px;
}

.stat-label {
  color: #667085;
}

.stat-value {
  font-weight: 600;
  color: #182230;
}

.stat-divider {
  color: #cbd5e1;
}

.status-error-box {
  display: flex;
  gap: 8px;
  background: #ffefed;
  border: 1px solid #f5b8b2;
  border-radius: 8px;
  padding: 10px 12px;
  color: #b42318;
  font-size: 13px;
  margin-top: 10px;
}

.notice-info {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 14px;
  padding: 10px 14px;
  background: #e6f4f8;
  border: 1px solid #a8d6e3;
  border-radius: 8px;
  color: #17556b;
  font-size: 12px;
  line-height: 1.45;
}

.notice-info i {
  margin-top: 2px;
  font-size: 14px;
}

.empty-status {
  padding: 24px;
  text-align: center;
}

.text-muted {
  color: #667085;
  font-size: 14px;
  margin: 0;
}
</style>
