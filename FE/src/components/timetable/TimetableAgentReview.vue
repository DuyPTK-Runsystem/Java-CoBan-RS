<script setup lang="ts">
import { computed } from 'vue'
import Button from 'primevue/button'
import FormAlert from '@/components/common/FormAlert.vue'
import type { TimetablePeriod } from '@/types/timetable'
import type { TimetableAgentAssignmentOption, TimetableAgentEntry, TimetableAgentPhase, TimetableAgentProposal, TimetableAgentReceipt } from '@/types/timetableAgent'
import { SCHOOL_WEEKDAYS, getIsoWeekdayLabel } from '@/utils/isoWeekday'

const props = defineProps<{
  proposal: TimetableAgentProposal | null
  phase: TimetableAgentPhase
  periods: TimetablePeriod[]
  assignments: TimetableAgentAssignmentOption[]
  receipt: TimetableAgentReceipt | null
  canSave: boolean
  error?: string
  pendingRecovery?: boolean
  canRetryPending?: boolean
}>()
const emit = defineEmits<{ save: []; recover: []; retryPending: []; reload: [] }>()
const days = SCHOOL_WEEKDAYS.map(({ value: id, label: name }) => ({ id, name }))
const sessions = [{ id: 'MORNING', name: 'Sáng' }, { id: 'AFTERNOON', name: 'Chiều' }]
const busy = computed(() => ['generating', 'approving', 'executing', 'recovering'].includes(props.phase))
const showExplanation = computed(() => !!props.proposal
  && !['READY_FOR_REVIEW', 'APPROVED', 'SAVED'].includes(props.proposal.status))
const statusText = computed(() => {
  const phaseLabels: Partial<Record<TimetableAgentPhase, string>> = {
    generating: 'Đang tạo gợi ý…', approving: 'Đang chuẩn bị lưu gợi ý…', executing: 'Đang lưu gợi ý…', recovering: 'Đang kiểm tra kết quả lưu…',
    'response-lost': 'Chưa xác định kết quả lưu. Kiểm tra trạng thái trước khi tiếp tục.',
    'invalid-schema': 'Gợi ý chưa có cấu trúc hợp lệ.', 'provider-timeout': 'Quá thời gian tạo gợi ý.', denied: 'Bạn không có quyền thực hiện thao tác này.',
    unavailable: 'Gợi ý thời khoá biểu hiện chưa khả dụng.', stale: 'Dữ liệu đã thay đổi. Tạo phương án mới và duyệt lại.', expired: 'Phương án đã hết hạn. Tạo phương án mới và duyệt lại.',
  }
  if (phaseLabels[props.phase]) return phaseLabels[props.phase]
  const labels = { NEEDS_INPUT: 'Cần bổ sung thông tin', NO_SOLUTION_FOUND: 'Chưa tìm được phương án trong giới hạn', CONFLICTS: 'Phương án còn lỗi cần sửa', READY_FOR_REVIEW: 'Sẵn sàng để duyệt', APPROVED: 'Đã duyệt phương án', SAVED: 'Hành động đã lưu; kiểm tra kết quả lưu', STALE: 'Dữ liệu đã thay đổi', EXPIRED: 'Phương án hết hạn' }
  return props.proposal ? labels[props.proposal.status] : 'Chọn yêu cầu để tạo gợi ý.'
})
const previewClasses = computed(() => {
  const ids = new Set(props.proposal?.entries.map((e) => props.assignments.find((a) => a.id === e.assignmentId)?.classId).filter((id): id is number => id !== undefined))
  return [...ids].map((id) => ({ id, name: props.assignments.find((a) => a.classId === id)?.className ?? `Lớp #${id}` }))
})
function key(e: TimetableAgentEntry) { return `${e.assignmentId}:${e.periodId}:${e.functionalRoomId}:${e.validFrom}:${e.validTo}` }
function unchanged(e: TimetableAgentEntry) { return props.proposal?.diff.unchanged.some((p) => key(e) === key(p)) ?? false }
function label(e: TimetableAgentEntry) {
  const a = props.assignments.find((item) => item.id === e.assignmentId)
  const p = props.periods.find((item) => item.id === e.periodId)
  return `${a?.className ?? ''} · ${a?.subjectName ?? `Phân công #${e.assignmentId}`} · ${a?.teacherName ?? ''} · ${p?.dayOfWeek ? getIsoWeekdayLabel(p.dayOfWeek) : 'Khung giờ'} ${p?.session === 'AFTERNOON' ? 'Chiều' : 'Sáng'} tiết ${p?.periodIndex ?? ''}`
}
function cell(classId: number, day: number, session: string, index: number) {
  return props.proposal?.entries.filter((e) => {
    const a = props.assignments.find((item) => item.id === e.assignmentId)
    const p = props.periods.find((item) => item.id === e.periodId)
    return a?.classId === classId && p?.dayOfWeek === day && p?.session === session && p.periodIndex === index
  }) ?? []
}
</script>

<template>
  <section class="agent-review" aria-label="Xem gợi ý thời khoá biểu" :aria-busy="busy">
    <h3>Phương án đề xuất</h3>
    <FormAlert :tone="receipt ? 'success' : 'info'" :message="receipt ? 'Đã lưu bản nháp theo kết quả của máy chủ.' : statusText" />
    <FormAlert v-if="error" :message="error" />
    <div v-if="pendingRecovery" class="agent-actions">
      <Button label="Kiểm tra trạng thái lưu" :loading="phase === 'recovering'" :disabled="busy" @click="emit('recover')" />
      <Button v-if="canRetryPending" label="Thử lại yêu cầu lưu trước" :disabled="busy" severity="secondary" @click="emit('retryPending')" />
    </div>
    <template v-if="receipt">
      <p role="status">Bản nháp #{{ receipt.targetRevisionId }} · phiên bản {{ receipt.newVersion }} · {{ receipt.savedEntryCount }} tiết đã lưu.</p>
      <p>Thời điểm lưu: {{ receipt.committedAt }}</p>
      <Button label="Tải lại thời khoá biểu" :disabled="busy" @click="emit('reload')" />
    </template>
    <template v-if="proposal">
      <p v-if="showExplanation" class="agent-explanation">{{ proposal.explanation }}</p>
      <FormAlert v-for="(issue, i) in proposal.issues" :key="`${issue.code}-${i}`" :tone="issue.severity === 'BLOCKING' ? 'error' : 'warning'" :message="issue.message" />
      <p v-if="proposal.entries.length">Xanh: tiết mới hoặc thay đổi. Xám: tiết giữ nguyên. Mỗi tiết hiển thị khoảng ngày áp dụng.</p>
      <section v-for="c in previewClasses" :key="c.id" class="agent-class-grid">
        <h4>{{ c.name }}</h4>
        <div class="agent-table-wrap">
          <table>
            <caption class="sr-only">Phương án {{ c.name }}</caption><thead><tr><th scope="col">Buổi / tiết</th><th v-for="day in days" :key="day.id" scope="col">{{ day.name }}</th></tr></thead><tbody>
              <template v-for="session in sessions" :key="session.id">
                <tr v-for="index in 4" :key="`${session.id}-${index}`">
                  <th scope="row">{{ session.name }} · {{ index }}</th><td v-for="day in days" :key="day.id">
                    <article v-for="entry in cell(c.id, day.id, session.id, index)" :key="key(entry)" :class="unchanged(entry) ? 'agent-held' : 'agent-added'">
                      <strong>{{ assignments.find((a) => a.id === entry.assignmentId)?.subjectName }}</strong><small>{{ assignments.find((a) => a.id === entry.assignmentId)?.teacherName }}</small>
                      <small>{{ entry.validFrom }} → {{ entry.validTo }}</small><small>{{ unchanged(entry) ? 'Giữ nguyên' : 'Mới / thay đổi' }}</small>
                    </article><span v-if="!cell(c.id, day.id, session.id, index).length">—</span>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </section>
      <details v-if="proposal.entries.length" class="agent-diff">
        <summary>Thay đổi: thêm {{ proposal.diff.added.length }}, bỏ {{ proposal.diff.removed.length }}, giữ {{ proposal.diff.unchanged.length }} tiết</summary>
        <div v-for="group in [{name: 'Thêm / thay đổi', rows: proposal.diff.added}, {name: 'Bỏ', rows: proposal.diff.removed}, {name: 'Giữ nguyên', rows: proposal.diff.unchanged}]" :key="group.name"><h4>{{ group.name }}</h4><ul><li v-for="entry in group.rows" :key="key(entry)">{{ label(entry) }} · {{ entry.validFrom }} → {{ entry.validTo }}</li></ul></div>
      </details>
      <div v-if="!receipt" class="agent-actions">
        <Button label="Lưu gợi ý" :loading="phase === 'approving' || phase === 'executing'" :disabled="busy || !canSave" @click="canSave && emit('save')" />
      </div>
      <p v-if="!receipt" class="agent-hint">Lưu gợi ý sẽ duyệt phương án rồi lưu vào bản nháp. Công bố qua quy trình hiện có.</p>
    </template>
  </section>
</template>

<style scoped>
.agent-review{padding:1.25rem;border:1px solid #dce4e9;border-radius:12px;background:white;min-width:0}.agent-review h3{margin-top:0}.agent-explanation{white-space:pre-wrap;line-height:1.5}.agent-review :deep(.form-alert){margin-bottom:.75rem}.agent-actions{display:flex;flex-wrap:wrap;gap:.75rem;margin:1rem 0}.agent-hint{font-size:.875rem;color:#536773}.agent-table-wrap{overflow:auto}table{width:100%;min-width:660px;border-collapse:collapse;font-size:.8rem}th,td{border:1px solid #dce4e9;padding:.5rem;text-align:left;vertical-align:top}th{background:#f5f8fa}article{padding:.5rem;border-radius:4px;margin-bottom:.4rem}.agent-added{background:#e6f4ed}.agent-held{background:#edf1f5}small{display:block;margin-top:.25rem;color:#536773}.agent-diff{margin-top:1rem}.agent-diff summary{cursor:pointer;font-weight:600}.sr-only{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0,0,0,0)}
</style>
