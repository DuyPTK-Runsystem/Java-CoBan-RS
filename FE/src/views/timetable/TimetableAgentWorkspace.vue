<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import TimetableAgentPanel from '@/components/timetable/TimetableAgentPanel.vue'
import TimetableAgentReview from '@/components/timetable/TimetableAgentReview.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { getAuthSession } from '@/services/authSession'
import { fetchSubjectAssignmentsByClass } from '@/services/assignmentApi'
import { approveTimetableAgentProposal, createTimetableAgentProposal, executeTimetableAgentProposal, getTimetableAgentActionByKey } from '@/services/timetableAgentApi'
import { extractApiErrorMessage, isApiError } from '@/types/api'
import type { SchoolClass } from '@/types/academic'
import type { Teacher } from '@/types/teacher'
import type { TimetableDetail, TimetableEntry, TimetablePeriod } from '@/types/timetable'
import type { TimetableAgentActionState, TimetableAgentAssignmentOption, TimetableAgentPendingAction, TimetableAgentPhase, TimetableAgentProposal, TimetableAgentReceipt, TimetableAgentRequest } from '@/types/timetableAgent'

const props = defineProps<{ detail: TimetableDetail; classes: SchoolClass[]; teachers: Teacher[]; periods: TimetablePeriod[]; entries: TimetableEntry[] }>()
const emit = defineEmits<{ saved: [receipt: TimetableAgentReceipt]; reload: [] }>()
const { requireAccessToken } = useAuthSession()
const proposal = ref<TimetableAgentProposal | null>(null)
const receipt = ref<TimetableAgentReceipt | null>(null)
const phase = ref<TimetableAgentPhase>('idle')
const error = ref('')
const assignments = ref<TimetableAgentAssignmentOption[]>([])
const assignmentsLoading = ref(false)
const approvedBinding = ref<string | null>(null)
const pending = ref<TimetableAgentPendingAction | null>(null)
const recoveryAllowsRetry = ref(false)
const retryAfter = ref<number | null>(null)
const now = ref(Date.now())
const timer = window.setInterval(() => { now.value = Date.now() }, 1000)
let requestEpoch = 0
let assignmentEpoch = 0
let pendingStorageKey: string | null = null
onBeforeUnmount(() => { window.clearInterval(timer); requestEpoch++; assignmentEpoch++ })
const busy = computed(() => ['generating', 'approving', 'executing', 'recovering'].includes(phase.value))
const editable = computed(() => ['DRAFT', 'VALIDATED'].includes(props.detail.status))
const available = computed(() => props.detail.canUseTimetableAgent === true && editable.value)
const expired = computed(() => !!proposal.value && (!Number.isFinite(Date.parse(proposal.value.expiresAt)) || Date.parse(proposal.value.expiresAt) <= now.value))
const stale = computed(() => !!proposal.value && (proposal.value.targetRevisionId !== props.detail.revisionId || proposal.value.expectedVersion !== props.detail.version))
const blocked = computed(() => proposal.value?.issues.some((i) => i.severity === 'BLOCKING') ?? true)
function binding(p: TimetableAgentProposal) { return `${p.proposalId}:${p.proposalVersion}:${p.proposalHash}:${p.snapshotId}:${p.targetRevisionId}:${p.expectedVersion}` }
const canApprove = computed(() => available.value && phase.value === 'idle' && !pending.value && !expired.value && !stale.value && !blocked.value
  && proposal.value?.status === 'READY_FOR_REVIEW' && proposal.value.capabilities.canApprove)
const canExecute = computed(() => available.value && phase.value === 'idle' && !pending.value && !expired.value && !stale.value && !blocked.value
  && proposal.value?.status === 'APPROVED' && proposal.value.capabilities.canExecute && approvedBinding.value === binding(proposal.value))
const displayPhase = computed(() => receipt.value || pending.value ? phase.value : stale.value ? 'stale' : expired.value ? 'expired' : phase.value)
const canRetryPending = computed(() => !!pending.value && available.value && !busy.value
  && (recoveryAllowsRetry.value || (retryAfter.value !== null && now.value >= retryAfter.value)))
const classOptions = computed(() => props.classes.map((c) => ({ id: c.id, name: c.name })))
const labelOptions = computed(() => {
  const byId = new Map(assignments.value.map((a) => [a.id, a]))
  props.entries.forEach((e) => { if (!byId.has(e.assignmentId)) byId.set(e.assignmentId, { id: e.assignmentId, classId: e.classId, className: e.className, subjectName: e.subjectName, teacherName: e.teacherName }) })
  return [...byId.values()]
})

function storageKey() {
  const actor = getAuthSession()?.user.id
  return actor === undefined ? null : `timetable-agent.pending.${actor}.${props.detail.revisionId}`
}
function retainPending(action: TimetableAgentPendingAction) {
  const key = storageKey()
  if (!key) throw new Error('Cần đăng nhập để lưu bản nháp.')
  sessionStorage.setItem(key, JSON.stringify(action))
  pendingStorageKey = key
  pending.value = action
}
function clearPending() {
  const key = pendingStorageKey
  if (key) sessionStorage.removeItem(key)
  pending.value = null
  recoveryAllowsRetry.value = false
  retryAfter.value = null
  pendingStorageKey = null
}
function restorePending() {
  const key = storageKey()
  if (!key) return
  try {
    const serialized = sessionStorage.getItem(key)
    if (!serialized) return
    const value: unknown = JSON.parse(serialized)
    if (typeof value !== 'object' || value === null) throw new Error('invalid')
    const action = value as Partial<TimetableAgentPendingAction>
    if (typeof action.proposalId !== 'string' || !action.proposalId || !Number.isInteger(action.proposalVersion)
      || action.targetRevisionId !== props.detail.revisionId || typeof action.idempotencyKey !== 'string' || !action.idempotencyKey) throw new Error('invalid')
    pending.value = action as TimetableAgentPendingAction
    pendingStorageKey = key
    phase.value = 'response-lost'
  } catch {
    error.value = 'Không thể đọc thông tin lần lưu trước. Kiểm tra bản nháp trước khi tiếp tục.'
    phase.value = 'response-lost'
  }
}
restorePending()
function inputChanged() {
  approvedBinding.value = null
  if (pending.value) return
  requestEpoch++
  proposal.value = null
  receipt.value = null
  phase.value = 'idle'
  error.value = ''
}
watch(() => [props.detail.version, props.detail.status, props.detail.canUseTimetableAgent], () => { approvedBinding.value = null })
watch(expired, (value) => { if (value) approvedBinding.value = null })

async function classesChanged(ids: number[]) {
  const epoch = ++assignmentEpoch
  assignments.value = []
  if (!ids.length) { assignmentsLoading.value = false; return }
  const token = requireAccessToken()
  if (!token) return
  assignmentsLoading.value = true
  try {
    const lists = await Promise.all(ids.map((id) => fetchSubjectAssignmentsByClass(token, id, props.detail.semesterId)))
    if (epoch !== assignmentEpoch) return
    assignments.value = lists.flat().filter((a) => a.status === 'ACTIVE' && a.semesterId === props.detail.semesterId && a.classId != null
      && ids.includes(a.classId) && (!props.detail.effectiveTo || a.validFrom <= props.detail.effectiveTo) && (!a.validTo || a.validTo >= props.detail.effectiveFrom))
      .map((a) => ({ id: a.id, classId: a.classId!, className: a.className || a.classCode || `Lớp #${a.classId}`, subjectName: a.subjectName || `Môn #${a.subjectId}`, teacherName: props.teachers.find((t) => t.id === a.teacherId)?.teacherName || `Giáo viên #${a.teacherId}` }))
  } catch (cause) {
    if (epoch === assignmentEpoch) error.value = extractApiErrorMessage(cause, 'Không thể tải phân công của lớp.')
  } finally { if (epoch === assignmentEpoch) assignmentsLoading.value = false }
}
function failure(cause: unknown) {
  approvedBinding.value = null
  error.value = extractApiErrorMessage(cause, 'Không thể hoàn tất yêu cầu. Giữ dữ liệu để thử lại.')
  phase.value = isApiError(cause, 403) ? 'denied' : isApiError(cause, 409) ? 'stale' : isApiError(cause, 504) ? 'provider-timeout'
    : isApiError(cause, 502) ? 'invalid-schema' : isApiError(cause, 503) ? 'unavailable' : 'idle'
  if (proposal.value && isApiError(cause, 409)) proposal.value = { ...proposal.value, status: 'STALE' }
}
async function generate(request: TimetableAgentRequest) {
  if (busy.value || pending.value || !available.value || assignmentsLoading.value) return
  const token = requireAccessToken()
  if (!token) return
  const epoch = ++requestEpoch
  approvedBinding.value = null; proposal.value = null; receipt.value = null; error.value = ''; phase.value = 'generating'
  try {
    const result = await createTimetableAgentProposal(request, token)
    if (epoch !== requestEpoch) return
    proposal.value = result
    phase.value = 'idle'
  } catch (cause) { if (epoch === requestEpoch) failure(cause) }
}
async function approve() {
  if (!canApprove.value || !proposal.value) return
  const token = requireAccessToken()
  if (!token) return
  const original = binding(proposal.value)
  const epoch = ++requestEpoch
  phase.value = 'approving'; error.value = ''
  try {
    const approved = await approveTimetableAgentProposal(proposal.value, token)
    if (epoch !== requestEpoch) return
    if (binding(approved) !== original || approved.status !== 'APPROVED') throw new Error('Phương án duyệt không khớp. Tạo phương án mới và duyệt lại.')
    proposal.value = approved; approvedBinding.value = original; phase.value = 'idle'
  } catch (cause) { if (epoch === requestEpoch) failure(cause) }
}
function saved(result: TimetableAgentReceipt, action: TimetableAgentPendingAction) {
  if (result.status !== 'SAVED_DRAFT' || result.proposalId !== action.proposalId || result.targetRevisionId !== action.targetRevisionId
    || !Number.isInteger(result.newVersion) || !result.actionId) throw new Error('Chưa có kết quả lưu hợp lệ từ máy chủ. Hãy kiểm tra lại trạng thái.')
  receipt.value = result; clearPending(); approvedBinding.value = null; phase.value = 'idle'; error.value = ''
  emit('saved', result)
}
function actionResult(result: TimetableAgentActionState, action: TimetableAgentPendingAction) {
  if (result.proposalId !== action.proposalId || result.targetRevisionId !== action.targetRevisionId || !result.actionId) {
    throw new Error('Kết quả hành động không khớp yêu cầu lưu trước. Kiểm tra lại trạng thái.')
  }
  if (result.status === 'SAVED_DRAFT') { saved(result, action); return }
  phase.value = 'response-lost'
  if (result.status === 'PENDING') {
    const expiry = Date.parse(result.leaseExpiresAt)
    retryAfter.value = Number.isFinite(expiry) ? expiry : null
    recoveryAllowsRetry.value = false
    error.value = 'Yêu cầu lưu đang được xử lý. Kiểm tra lại trạng thái trước khi tiếp tục.'
  } else if (result.status === 'FAILED') {
    recoveryAllowsRetry.value = result.retryable === true
    retryAfter.value = null
    error.value = 'Lần lưu trước chưa hoàn tất. Có thể thử lại chính yêu cầu đó khi hệ thống khả dụng.'
  } else throw new Error('Chưa có trạng thái hành động hợp lệ từ máy chủ.')
}
async function runPending(action: TimetableAgentPendingAction, token: string) {
  recoveryAllowsRetry.value = false; retryAfter.value = null
  phase.value = 'executing'; error.value = ''
  try { actionResult(await executeTimetableAgentProposal(action.proposalId, action.proposalVersion, action.idempotencyKey, token), action) }
  catch (cause) {
    approvedBinding.value = null
    if (isApiError(cause) && [400, 401, 403, 404, 409, 422].includes(cause.status)) {
      clearPending(); failure(cause); return
    }
    phase.value = 'response-lost'
    error.value = 'Chưa nhận được kết quả lưu. Hãy kiểm tra trạng thái hành động.'
  }
}
async function execute() {
  if (!canExecute.value || !proposal.value) return
  const token = requireAccessToken()
  if (!token) return
  const action: TimetableAgentPendingAction = { proposalId: proposal.value.proposalId, proposalVersion: proposal.value.proposalVersion, targetRevisionId: props.detail.revisionId, idempotencyKey: crypto.randomUUID() }
  try { retainPending(action) } catch (cause) { failure(cause); return }
  await runPending(action, token)
}
async function retryPending() {
  if (!canRetryPending.value || !pending.value) return
  const token = requireAccessToken()
  if (token) await runPending(pending.value, token)
}
async function recover() {
  if (!pending.value || busy.value) return
  const token = requireAccessToken()
  if (!token) return
  phase.value = 'recovering'; error.value = ''
  const action = pending.value
  try { actionResult(await getTimetableAgentActionByKey(action.idempotencyKey, token), action) }
  catch (cause) {
    phase.value = 'response-lost'
    recoveryAllowsRetry.value = isApiError(cause, 404)
    error.value = isApiError(cause, 404) ? 'Chưa thấy yêu cầu lưu trước. Có thể thử lại chính yêu cầu đó; hệ thống sẽ kiểm tra trạng thái trước khi xử lý.'
      : extractApiErrorMessage(cause, 'Chưa thể kiểm tra kết quả lưu. Thử kiểm tra lại.')
  }
}
</script>

<template>
  <section aria-label="Gợi ý thời khoá biểu" class="agent-workspace">
    <div class="agent-steps" aria-label="Các bước">1. Ràng buộc → 2. Xem gợi ý → 3. Duyệt phương án → 4. Lưu bản nháp</div>
    <div class="agent-layout">
      <TimetableAgentPanel :target-revision-id="detail.revisionId" :expected-version="detail.version" :default-valid-from="detail.effectiveFrom" :default-valid-to="detail.effectiveTo ?? ''" :classes="classOptions" :assignments="assignments" :existing-entries="entries" :can-generate="available && !pending && phase !== 'response-lost' && (proposal?.capabilities.canGenerate ?? true)" :busy="busy || !!pending" :assignments-loading="assignmentsLoading" @generate="generate" @input-changed="inputChanged" @classes-changed="classesChanged" />
      <TimetableAgentReview :proposal="proposal" :receipt="receipt" :phase="displayPhase" :periods="periods" :assignments="labelOptions" :can-approve="canApprove" :can-execute="canExecute" :error="error" :pending-recovery="!!pending" :can-retry-pending="canRetryPending" @approve="approve" @execute="execute" @recover="recover" @retry-pending="retryPending" @reload="emit('reload')" />
    </div>
  </section>
</template>

<style scoped>
.agent-steps{padding:1rem;color:#536773;font-size:.9rem}.agent-layout{display:grid;grid-template-columns:minmax(280px,330px) minmax(0,1fr);gap:1.25rem}@media(max-width:950px){.agent-layout{grid-template-columns:1fr}}
</style>
