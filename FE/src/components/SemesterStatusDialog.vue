<script setup lang="ts">
import { computed } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'

import FormAlert from '@/components/FormAlert.vue'
import StatusTag from '@/components/StatusTag.vue'
import SemesterNotificationPanel from '@/components/SemesterNotificationPanel.vue'
import type { Semester, SemesterCompletenessReport, SemesterLockReportStatus, SemesterNotification, SemesterStatus } from '@/types/academic'
import { formatAcademicDateTime } from '@/utils/academicDate'

const props = withDefaults(defineProps<{
  visible?: boolean
  semester?: Semester | null
  report?: SemesterCompletenessReport | null
  loading?: boolean
  actionLoading?: boolean
  errorMessage?: string
  notifications?: SemesterNotification[]
  notificationsLoading?: boolean
  notificationActionLoading?: boolean
  notificationErrorMessage?: string
}>(), {
  visible: false,
  semester: null,
  report: null,
  loading: false,
  actionLoading: false,
  errorMessage: '',
  notifications: () => [],
  notificationsLoading: false,
  notificationActionLoading: false,
  notificationErrorMessage: '',
})

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  lock: []
  reopen: []
  dispatchNotifications: []
  retryNotifications: []
  dispatchNotifications: []
  retryNotifications: []
}>()

const statusLabels: Record<SemesterStatus, string> = {
  DRAFT: 'Nháp',
  ACTIVE: 'Đang hoạt động',
  LOCKED: 'Đã khóa',
  CLOSED: 'Đã đóng',
}
const statusSeverities: Record<SemesterStatus, 'secondary' | 'success' | 'warn' | 'contrast'> = {
  DRAFT: 'secondary',
  ACTIVE: 'success',
  LOCKED: 'warn',
  CLOSED: 'contrast',
}
const reportLabels: Record<SemesterLockReportStatus, string> = {
  COMPLETE: 'Đã hoàn tất',
  INCOMPLETE: 'Chưa hoàn tất',
  FAILED: 'Lỗi',
}
const reportSeverities: Record<SemesterLockReportStatus, 'success' | 'warn' | 'danger'> = {
  COMPLETE: 'success',
  INCOMPLETE: 'warn',
  FAILED: 'danger',
}
const summaryItems = computed(() => {
  const summary = props.report?.summary
  if (!summary) return []
  return [
    ['Thiếu điểm KTĐK', summary.missingKtdkCount],
    ['KTCK không hợp lệ', summary.invalidKtckCount],
    ['Thiếu cột kỹ năng', summary.missingSkillColumnsCount],
    ['Chưa nhập điểm', summary.unenteredScoreCount],
    ['Học sinh thiếu dữ liệu', summary.studentWithoutScoreDataCount],
    ['Sổ điểm chưa công bố', summary.unpublishedScorebookCount],
    ['Yêu cầu sửa điểm chờ xử lý', summary.pendingScoreChangeRequestCount],
  ] as Array<[string, number]>
})
const canLock = computed(() => props.semester?.status === 'ACTIVE')
const canReopen = computed(() => props.semester?.status === 'LOCKED')
</script>

<template>
  <Dialog :visible="props.visible" modal header="Trạng thái học kỳ" :style="{ width: 'min(100% - 2rem, 860px)' }" :closable="!props.actionLoading" @update:visible="emit('update:visible', $event)">
    <div v-if="props.semester" class="status-dialog-content">
      <div class="status-dialog-heading">
        <div>
          <p class="eyebrow">Academic structure</p>
          <h2>{{ props.semester.name }} <span class="muted-code">· {{ props.semester.code }}</span></h2>
          <p class="section-caption">Thông tin và trạng thái học kỳ</p>
        </div>
        <StatusTag :label="statusLabels[props.semester.status]" :severity="statusSeverities[props.semester.status]" />
      </div>
      <FormAlert v-if="props.errorMessage" tone="error" :message="props.errorMessage" />
      <div v-if="props.semester.status === 'ACTIVE' && props.report?.reportStatus === 'INCOMPLETE'" class="form-alert form-alert-warning">
        Dữ liệu điểm chưa hoàn chỉnh là cảnh báo tham khảo và không chặn thao tác khóa học kỳ.
      </div>
      <div class="detail-grid">
        <div class="detail-item"><span>Thời điểm tự động khóa</span><strong>{{ formatAcademicDateTime(props.semester.automaticLockAt) }}</strong></div>
        <div class="detail-item"><span>Đã khóa lúc</span><strong>{{ formatAcademicDateTime(props.semester.lockedAt) }}</strong></div>
        <div class="detail-item"><span>Người khóa (ID)</span><strong>{{ props.semester.lockedBy ?? '-' }}</strong></div>
        <div class="detail-item"><span>Thời hạn mở lại</span><strong>{{ formatAcademicDateTime(props.semester.reopenUntil) }}</strong></div>
        <div class="detail-item detail-item-wide"><span>Lý do khóa</span><strong>{{ props.semester.lockReason || '-' }}</strong></div>
      </div>
      <div class="timeline-block">
        <div class="section-heading"><div><h3>Vòng đời học kỳ</h3><p class="section-caption">Trạng thái lịch sử lấy từ dữ liệu backend.</p></div></div>
        <ol class="lifecycle-timeline">
          <li class="timeline-step timeline-step-complete"><span class="timeline-dot"><i class="pi pi-check" aria-hidden="true" /></span><div><strong>Nháp</strong><span>Đã tạo metadata học kỳ</span></div></li>
          <li :class="['timeline-step', props.semester.status !== 'DRAFT' ? 'timeline-step-complete' : 'timeline-step-pending']"><span class="timeline-dot"><i :class="props.semester.status !== 'DRAFT' ? 'pi pi-check' : 'pi pi-minus'" aria-hidden="true" /></span><div><strong>Đang hoạt động</strong><span>{{ props.semester.status !== 'DRAFT' ? 'Đã kích hoạt' : 'Chưa kích hoạt' }}</span></div></li>
          <li :class="['timeline-step', props.semester.status === 'LOCKED' || props.semester.status === 'CLOSED' ? 'timeline-step-complete' : 'timeline-step-pending']"><span class="timeline-dot"><i :class="props.semester.status === 'LOCKED' || props.semester.status === 'CLOSED' ? 'pi pi-check' : 'pi pi-minus'" aria-hidden="true" /></span><div><strong>Đã khóa</strong><span>{{ props.semester.lockedAt ? formatAcademicDateTime(props.semester.lockedAt) : 'Chưa khóa' }}</span></div></li>
          <li :class="['timeline-step', props.semester.status === 'CLOSED' ? 'timeline-step-complete' : 'timeline-step-pending']"><span class="timeline-dot"><i :class="props.semester.status === 'CLOSED' ? 'pi pi-check' : 'pi pi-minus'" aria-hidden="true" /></span><div><strong>Đã đóng</strong><span>{{ props.semester.status === 'CLOSED' ? 'Dữ liệu lịch sử' : 'Không áp dụng trong UI hiện tại' }}</span></div></li>
        </ol>
      </div>
      <div class="report-block">
        <div class="section-heading"><div><h3>Báo cáo hoàn thành dữ liệu điểm</h3><p class="section-caption">Đánh giá lúc {{ formatAcademicDateTime(props.report?.evaluatedAt) }}</p></div><StatusTag v-if="props.report" :label="reportLabels[props.report.reportStatus]" :severity="reportSeverities[props.report.reportStatus]" /></div>
        <div v-if="props.loading" class="page-state page-state-loading"><i class="pi pi-spin pi-spinner" aria-hidden="true" /><span>Đang tải báo cáo...</span></div>
        <FormAlert v-else-if="props.report?.reportStatus === 'FAILED'" tone="error" :message="props.report.failureReason || 'Không thể tải báo cáo hoàn thành dữ liệu.'" />
        <template v-else-if="props.report">
          <div class="summary-grid">
            <div v-for="item in summaryItems" :key="item[0]" class="summary-metric"><span>{{ item[0] }}</span><strong>{{ item[1] }}</strong></div>
          </div>
          <ul v-if="props.report.summary.details.length" class="report-details">
            <li v-for="detail in props.report.summary.details" :key="detail">{{ detail }}</li>
          </ul>
          <p v-else class="section-caption">Không có chi tiết bổ sung từ backend.</p>
        </template>
        <div v-else class="empty-state"><i class="pi pi-file" aria-hidden="true" /><span>Chưa có báo cáo để hiển thị.</span></div>
      </div>
      <SemesterNotificationPanel :notifications="props.notifications" :loading="props.notificationsLoading" :action-loading="props.notificationActionLoading" :error-message="props.notificationErrorMessage" @dispatch="emit('dispatchNotifications')" @retry="emit('retryNotifications')" />
      <div class="form-actions">
        <Button v-if="canLock" label="Khóa học kỳ" icon="pi pi-lock" severity="warn" :loading="props.actionLoading" @click="emit('lock')" />
        <Button v-if="canReopen" label="Mở lại học kỳ" icon="pi pi-lock-open" severity="info" :loading="props.actionLoading" @click="emit('reopen')" />
        <Button label="Đóng" icon="pi pi-times" severity="secondary" outlined :disabled="props.actionLoading" @click="emit('update:visible', false)" />
      </div>
    </div>
  </Dialog>
</template>
