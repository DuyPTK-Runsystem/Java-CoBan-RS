<script setup lang="ts">
import { computed } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Paginator from 'primevue/paginator'
import Select from 'primevue/select'
import Tag from 'primevue/tag'

import type { PlacementClassProfile, PlacementResult, PlacementResultsMeta, PlacementSession } from '@/types/placement'

const props = withDefaults(defineProps<{
  session: PlacementSession | null
  results?: PlacementResult[]
  resultsMeta?: PlacementResultsMeta | null
  studentNamesById?: Record<number, { studentCode: string; studentName: string }>
  loadingResults?: boolean
  reviewState?: 'ready' | 'loading' | 'empty' | 'forbidden' | 'conflict' | 'error'
  forbiddenMessage?: string
  editable?: boolean
  saving?: boolean
  academicYearLabel?: string
  gradeLabel?: string
}>(), {
  results: undefined,
  resultsMeta: null,
  studentNamesById: () => ({}),
  loadingResults: false,
  reviewState: 'ready',
  forbiddenMessage: 'Bạn không có quyền xem hoặc thao tác phiên xếp lớp này. Phiên đăng nhập vẫn được giữ.',
  editable: false,
  saving: false,
  academicYearLabel: '',
  gradeLabel: '',
})

const emit = defineEmits<{
  simulate: []
  confirm: []
  cancel: []
  retry: []
  'view-reason': [result: PlacementResult]
  'update-profile': [classId: number, profile: PlacementClassProfile]
  'page-change': [page: number, pageSize: number]
}>()

const profileLabels: Record<PlacementClassProfile, string> = {
  ADVANCED: 'Nâng cao',
  SUPPORT: 'Hỗ trợ học tập',
  REGULAR: 'Thường',
}

const profileDescriptions: Record<PlacementClassProfile, string> = {
  ADVANCED: 'Ưu tiên học sinh có điểm cao trong số chỗ cho phép',
  SUPPORT: 'Ưu tiên nhóm học sinh có điểm thấp trong số chỗ cho phép',
  REGULAR: 'Cân bằng học lực và tỷ lệ nam nữ của khối',
}

const sessionStatusLabels = {
  DRAFT: 'Bản nháp',
  SIMULATED: 'Đã mô phỏng',
  READY_FOR_CONFIRM: 'Sẵn sàng xác nhận',
  CONFIRMED: 'Đã xác nhận',
  CANCELLED: 'Đã hủy',
} as const

const resultLabels = { AUTO_ASSIGNED: 'Tự động', MANUAL_REQUIRED: 'Cần xếp thủ công' } as const
const resultSeverity = (status: PlacementResult['resultStatus']) => status === 'AUTO_ASSIGNED' ? 'success' : 'warn'

const displayResults = computed(() => props.results ?? props.session?.results ?? [])

const hasBlockingIssues = computed(() => {
  if (!props.session) return false
  if (props.session.status === 'SIMULATED') return true
  return displayResults.value.some((r) => r.issueSeverity === 'BLOCKING')
})

const hasManualRequired = computed(() => {
  return displayResults.value.some((r) => r.resultStatus === 'MANUAL_REQUIRED')
})

const canConfirm = computed(() => {
  return Boolean(
    props.session &&
    props.reviewState === 'ready' &&
    props.session.status === 'READY_FOR_CONFIRM' &&
    !hasBlockingIssues.value &&
    !props.saving,
  )
})

const scopeDescription = computed(() => {
  if (!props.session) return ''
  const yearText = props.academicYearLabel || `Năm học #${props.session.academicYearId}`
  const gradeText = props.gradeLabel || `Khối #${props.session.targetGradeId}`
  return `${yearText} · ${gradeText} · Phiên #${props.session.id}`
})

function getTargetClassName(targetClassId: number | null): string {
  if (!targetClassId) return '—'
  const target = props.session?.targetClasses.find((c) => c.classId === targetClassId)
  return target?.className || target?.classCode || `Lớp #${targetClassId}`
}
</script>

<template>
  <section class="placement-review" aria-label="Workspace xếp lớp theo quy tắc">
    <header class="page-heading placement-review__heading">
      <div>
        <p class="eyebrow">XẾP LỚP TỰ ĐỘNG</p>
        <h1>Xếp lớp theo quy tắc</h1>
        <p v-if="props.session" class="section-caption">{{ scopeDescription }}</p>
      </div>
      <div class="page-heading-actions">
        <Button
          label="Mô phỏng lại"
          icon="pi pi-refresh"
          severity="secondary"
          :disabled="!props.session || props.reviewState !== 'ready' || props.session.status === 'CONFIRMED' || props.session.status === 'CANCELLED' || props.saving"
          :loading="props.saving"
          @click="emit('simulate')"
        />
        <Button
          label="Xác nhận phần tự động"
          icon="pi pi-check"
          :disabled="!canConfirm"
          :loading="props.saving"
          @click="emit('confirm')"
        />
      </div>
    </header>

    <div v-if="props.reviewState !== 'ready'" class="form-alert form-alert-error" role="alert">
      <span v-if="props.reviewState === 'loading'">Đang tải phiên xếp lớp…</span>
      <span v-else-if="props.reviewState === 'empty'">Chưa có phiên xếp lớp để xem.</span>
      <span v-else-if="props.reviewState === 'forbidden'">{{ props.forbiddenMessage }}</span>
      <span v-else-if="props.reviewState === 'conflict'">Phiên đã thay đổi ở nơi khác. Hãy tải lại dữ liệu trước khi tiếp tục.</span>
      <span v-else>Không thể tải phiên xếp lớp.</span>
      <Button v-if="props.reviewState === 'conflict' || props.reviewState === 'error'" label="Tải lại" size="small" text @click="emit('retry')" />
    </div>

    <template v-if="props.session && props.reviewState === 'ready'">
      <!-- Alert trạng thái Blocking -->
      <div v-if="hasBlockingIssues" class="form-alert form-alert-error" role="alert">
        <strong>Phiên có lỗi chặn xếp lớp (vượt sĩ số cho phép).</strong>
        <small>Vui lòng điều chỉnh lớp hoặc chuyển sang xếp lớp thủ công trước khi xác nhận.</small>
      </div>

      <!-- Alert có học sinh cần xử lý thủ công -->
      <div v-else-if="hasManualRequired" class="form-alert form-alert-warning" role="status">
        <strong>Có dữ liệu cần xử lý thủ công.</strong>
        <small>Phần tự động vẫn có thể xác nhận; giáo vụ tiếp tục xử lý các em này bằng quy trình xếp lớp hiện có.</small>
      </div>

      <!-- Alert đã xác nhận -->
      <div v-if="props.session.status === 'CONFIRMED'" class="form-alert form-alert-success" role="status">
        <strong>Phiên xếp lớp đã được xác nhận.</strong>
        <small>Học sinh tự động đã được ghi danh vào lớp. Dữ liệu hiện ở trạng thái chỉ đọc.</small>
      </div>

      <!-- Alert đã hủy -->
      <div v-if="props.session.status === 'CANCELLED'" class="form-alert form-alert-neutral" role="status">
        <strong>Phiên xếp lớp đã bị hủy.</strong>
        <small>Dữ liệu hiện ở trạng thái chỉ đọc.</small>
      </div>

      <section class="content-surface placement-review__profiles">
        <div class="section-heading">
          <div>
            <h2>Cách phân lớp</h2>
            <p class="section-caption">Cách phân lớp, sĩ số và mục tiêu nam nữ do hệ thống cung cấp.</p>
          </div>
          <Tag :value="sessionStatusLabels[props.session.status]" severity="info" />
        </div>
        <div class="placement-profile-grid">
          <article v-for="target in props.session.targetClasses" :key="target.classId" class="placement-profile-card">
            <div class="placement-profile-card__title">
              <strong>{{ target.classCode }}</strong>
              <Select
                v-if="props.editable && props.session.status === 'DRAFT'"
                :model-value="target.profile"
                :options="[{ label: 'Nâng cao', value: 'ADVANCED' }, { label: 'Hỗ trợ học tập', value: 'SUPPORT' }, { label: 'Thường', value: 'REGULAR' }]"
                option-label="label"
                option-value="value"
                :disabled="props.saving"
                @update:model-value="emit('update-profile', target.classId, $event)"
              />
              <Tag v-else :value="profileLabels[target.profile]" severity="secondary" />
            </div>
            <p>{{ profileDescriptions[target.profile] }}</p>
            <small>{{ target.capacity ?? '—' }} chỗ</small>
            <small>Mục tiêu: {{ target.genderTargetMale ?? '—' }} nam · {{ target.genderTargetFemale ?? '—' }} nữ</small>
          </article>
        </div>
      </section>

      <section class="content-surface">
        <div class="section-heading">
          <div>
            <h2>Kết quả mô phỏng</h2>
            <p class="section-caption">Thiếu dữ liệu hoặc bằng điểm ở ngưỡng không được chọn ngầm.</p>
          </div>
        </div>

        <div class="placement-table-scroll">
          <DataTable :value="displayResults" :loading="props.loadingResults" striped-rows responsive-layout="scroll">
            <Column header="Mã HS" style="width: 120px">
              <template #body="{ data }">
                {{ props.studentNamesById[data.studentId]?.studentCode || '—' }}
              </template>
            </Column>
            <Column header="Họ và tên" style="min-width: 180px">
              <template #body="{ data }">
                {{ props.studentNamesById[data.studentId]?.studentName || `Học sinh #${data.studentId}` }}
              </template>
            </Column>
            <Column header="Lớp đề xuất" style="min-width: 140px">
              <template #body="{ data }">
                {{ getTargetClassName(data.targetClassId) }}
              </template>
            </Column>
            <Column header="Điểm dùng để xếp lớp" style="width: 150px">
              <template #body="{ data }">
                {{ data.score !== null && data.score !== undefined ? data.score : '—' }}
              </template>
            </Column>
            <Column header="Trạng thái" style="width: 160px">
              <template #body="{ data }">
                <Tag :value="resultLabels[data.resultStatus]" :severity="resultSeverity(data.resultStatus)" />
              </template>
            </Column>
            <Column header="Vấn đề" style="width: 120px">
              <template #body="{ data }">
                <Tag
                  v-if="data.issueSeverity"
                  :value="data.issueSeverity === 'BLOCKING' ? 'Chặn' : 'Cảnh báo'"
                  :severity="data.issueSeverity === 'BLOCKING' ? 'danger' : 'warn'"
                />
                <span v-else>—</span>
              </template>
            </Column>
            <Column header="Lý do" style="width: 110px">
              <template #body="{ data }">
                <Button label="Xem lý do" text size="small" @click="emit('view-reason', data)" />
              </template>
            </Column>
          </DataTable>

          <Paginator
            v-if="props.resultsMeta && props.resultsMeta.totalItems > 0"
            :first="props.resultsMeta.page * props.resultsMeta.pageSize"
            :rows="props.resultsMeta.pageSize"
            :total-records="props.resultsMeta.totalItems"
            :rows-per-page-options="[10, 20, 50]"
            class="mt-3"
            @page="(event) => emit('page-change', event.page, event.rows)"
          />
        </div>
      </section>

      <footer class="placement-review__footer">
        <Button
          label="Hủy phiên"
          severity="secondary"
          outlined
          :disabled="props.session.status === 'CONFIRMED' || props.session.status === 'CANCELLED' || props.saving"
          @click="emit('cancel')"
        />
        <span class="section-caption">Vượt sĩ số cho phép là lỗi chặn khi xếp lớp tự động.</span>
      </footer>
    </template>
  </section>
</template>

<style scoped>
.placement-review { max-width: 1180px; margin: 0 auto; }
.placement-review__heading { margin-bottom: 20px; }
.placement-review__profiles { margin-bottom: 20px; }
.placement-profile-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; }
.placement-profile-card { border: 1px solid var(--p-content-border-color); border-radius: 10px; padding: 14px; }
.placement-profile-card__title { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.placement-profile-card p { min-height: 2.5rem; margin: 10px 0; color: var(--p-text-muted-color); font-size: .9rem; }
.placement-profile-card small { display: block; margin-top: 4px; color: var(--p-text-muted-color); }
.placement-table-scroll { overflow-x: auto; }
.placement-review__footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 18px; }
.form-alert { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 20px; padding: 12px 16px; border-radius: 8px; }
.form-alert small { flex-basis: 100%; }
.form-alert-error { background-color: var(--p-red-50, #fef2f2); border: 1px solid var(--p-red-200, #fecaca); color: var(--p-red-800, #991b1b); }
.form-alert-warning { background-color: var(--p-amber-50, #fffbeb); border: 1px solid var(--p-amber-200, #fde68a); color: var(--p-amber-800, #92400e); }
.form-alert-success { background-color: var(--p-green-50, #f0fdf4); border: 1px solid var(--p-green-200, #bbf7d0); color: var(--p-green-800, #166534); }
.form-alert-neutral { background-color: var(--p-surface-100, #f3f4f6); border: 1px solid var(--p-content-border-color, #e5e7eb); color: var(--p-text-muted-color, #4b5563); }
@media (max-width: 680px) { .placement-review__heading, .placement-review__footer { align-items: flex-start; flex-direction: column; } }
</style>
