<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Tag from 'primevue/tag'

import ScoreChangeRequestDetail from '@/components/ScoreChangeRequestDetail.vue'
import ScoreChangeRequestForm, { type ScoreChangeRequestFormContext } from '@/components/ScoreChangeRequestForm.vue'
import ServerPagination from '@/components/ServerPagination.vue'
import { fetchAcademicYears, fetchClassSubjects, fetchSchoolClasses, fetchSemesters, fetchSubjects } from '@/services/academicApi'
import { approveScoreChangeRequest, cancelScoreChangeRequest, createScoreChangeRequest, fetchScoreChangeRequest, fetchScoreChangeRequests, rejectScoreChangeRequest } from '@/services/scoreChangeRequestApi'
import { fetchScorebookByClassSubject, fetchScoreGrid } from '@/services/scorebookApi'
import { getAuthSession } from '@/services/authSession'
import { isApiError } from '@/types/api'
import { formatScoreChangeRequestDateTime } from '@/utils/scoreChangeRequestDate'
import type { AcademicYear, ClassSubject, SchoolClass, Semester, Subject } from '@/types/academic'
import type { ScoreGridColumn, Scorebook, StudentScoreGrid, StudentScoreGridRow } from '@/types/scorebook'
import type { CreateScoreChangeRequest, ScoreChangeRequest, ScoreChangeRequestDetail as Detail, ScoreChangeRequestStatus } from '@/types/scoreChangeRequest'

const token = computed(() => getAuthSession()?.accessToken ?? '')
const roles = computed(() => getAuthSession()?.user.roles ?? [])
const canReview = computed(() => roles.value.includes('ADMIN') || roles.value.includes('ACADEMIC_OFFICE'))
const canUseModule = computed(() => canReview.value || roles.value.includes('TEACHER'))

const years = ref<AcademicYear[]>([])
const semesters = ref<Semester[]>([])
const classes = ref<SchoolClass[]>([])
const classSubjects = ref<ClassSubject[]>([])
const subjects = ref<Subject[]>([])
const scorebook = ref<Scorebook | null>(null)
const academicYearId = ref<number | null>(null)
const semesterId = ref<number | null>(null)
const classId = ref<number | null>(null)
const classSubjectId = ref<number | null>(null)
const contextLoading = ref(false)
const contextError = ref('')
const scoreGrid = ref<StudentScoreGrid | null>(null)
const scoreGridLoading = ref(false)
const scoreGridError = ref('')
const selectedStudentId = ref<number | null>(null)
const selectedColumnId = ref<number | null>(null)
const contextLabel = computed(() => {
  const year = years.value.find((item) => item.id === academicYearId.value)
  const semester = semesters.value.find((item) => item.id === semesterId.value)
  const schoolClass = classes.value.find((item) => item.id === classId.value)
  const classSubject = classSubjects.value.find((item) => item.id === classSubjectId.value)
  const subject = subjects.value.find((item) => item.id === classSubject?.subjectId)
  return [year?.code, semester?.name, schoolClass?.className ?? schoolClass?.classCode, subject?.name].filter(Boolean).join(' · ')
})
const classSubjectOptions = computed(() => classSubjects.value.map((item) => ({ ...item, label: subjects.value.find((subject) => subject.id === item.subjectId)?.name ?? 'Môn học' })))

const requests = ref<ScoreChangeRequest[]>([])
const page = ref(0)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)
const status = ref<ScoreChangeRequestStatus | undefined>()
const studentCode = ref('')
const listLoading = ref(false)
const listError = ref('')
const listState = ref<'ready' | 'empty' | 'forbidden' | 'not-found' | 'error'>('ready')
const detail = ref<Detail | null>(null)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const formVisible = ref(false)
const formLoading = ref(false)
const mutationError = ref('')
const rejectVisible = ref(false)
const rejectionReason = ref('')

const statusOptions = [{ label: 'Tất cả trạng thái', value: undefined }, { label: 'Chờ duyệt', value: 'PENDING' }, { label: 'Đã duyệt', value: 'APPROVED' }, { label: 'Đã áp dụng', value: 'APPLIED' }, { label: 'Bị từ chối', value: 'REJECTED' }, { label: 'Đã hủy', value: 'CANCELLED' }]
const statusLabels: Record<ScoreChangeRequestStatus, string> = { PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Bị từ chối', CANCELLED: 'Đã hủy', APPLIED: 'Đã áp dụng' }
const scoreLabels = { SCORED: 'Có điểm', ABSENT: 'Vắng', EXEMPTED: 'Được miễn', CANCELLED: 'Hủy' }
const studentOptions = computed(() => (scoreGrid.value?.students ?? []).map((student) => ({
  studentId: student.studentId,
  label: `${student.studentCode} · ${student.studentName}`,
})))
const columnOptions = computed(() => (scoreGrid.value?.columns ?? []).map((column) => ({
  columnId: column.columnId,
  label: column.columnName ?? `Cột điểm ${column.columnNo}`,
})))
const selectedStudent = computed<StudentScoreGridRow | null>(() => scoreGrid.value?.students.find((student) => student.studentId === selectedStudentId.value) ?? null)
const selectedColumn = computed<ScoreGridColumn | null>(() => scoreGrid.value?.columns.find((column) => column.columnId === selectedColumnId.value) ?? null)
const createContext = computed<ScoreChangeRequestFormContext | null>(() => {
  if (!selectedStudent.value || !selectedColumn.value) return null
  const score = selectedStudent.value.scores[String(selectedColumn.value.columnId)]
  return {
    studentCode: selectedStudent.value.studentCode,
    studentName: selectedStudent.value.studentName,
    columnId: selectedColumn.value.columnId,
    columnName: selectedColumn.value.columnName ?? `Cột điểm ${selectedColumn.value.columnNo}`,
    currentStatus: score?.scoreStatus,
    currentValue: score?.scoreValue ?? null,
  }
})

function messageFor(error: unknown, fallback: string): string {
  if (isApiError(error)) return error.message
  return fallback
}
function handleListError(error: unknown): void {
  listError.value = messageFor(error, 'Không thể tải danh sách yêu cầu sửa điểm.')
  listState.value = isApiError(error, 403) ? 'forbidden' : isApiError(error, 404) ? 'not-found' : 'error'
}
async function loadRequests(): Promise<void> {
  listLoading.value = true; listError.value = ''; listState.value = 'ready'
  try {
    const result = await fetchScoreChangeRequests(token.value, { status: status.value, studentCode: studentCode.value, page: page.value, size: pageSize.value })
    requests.value = result.content; totalElements.value = result.totalElements; totalPages.value = result.totalPages
    if (requests.value.length === 0) listState.value = 'empty'
  } catch (error) { handleListError(error) } finally { listLoading.value = false }
}
async function loadContext(): Promise<void> {
  if (!token.value) return
  contextLoading.value = true; contextError.value = ''
  try {
    const [loadedYears, loadedSubjects] = await Promise.all([fetchAcademicYears(token.value), fetchSubjects(token.value, 'ACTIVE')])
    years.value = loadedYears; subjects.value = loadedSubjects
    academicYearId.value = years.value[0]?.id ?? null
  } catch (error) { contextError.value = messageFor(error, 'Không thể tải bối cảnh học vụ.') } finally { contextLoading.value = false }
}
async function loadSemesters(): Promise<void> { if (academicYearId.value === null) return; semesters.value = await fetchSemesters(token.value, academicYearId.value); semesterId.value = semesters.value[0]?.id ?? null }
async function loadClasses(): Promise<void> { if (academicYearId.value === null) return; classes.value = await fetchSchoolClasses(token.value, academicYearId.value); classId.value = classes.value[0]?.id ?? null }
async function loadClassSubjects(): Promise<void> { if (classId.value === null || semesterId.value === null) return; classSubjects.value = await fetchClassSubjects(token.value, classId.value, semesterId.value); classSubjectId.value = classSubjects.value[0]?.id ?? null }
async function loadScoreGrid(): Promise<void> {
  if (!scorebook.value) { scoreGrid.value = null; return }
  scoreGridLoading.value = true; scoreGridError.value = ''
  try {
    scoreGrid.value = await fetchScoreGrid(token.value, scorebook.value.id, 0, 100)
    selectedStudentId.value = null
    selectedColumnId.value = null
  } catch (error) {
    scoreGrid.value = null
    scoreGridError.value = messageFor(error, 'Không thể tải danh sách học sinh và điểm hiện tại.')
  } finally { scoreGridLoading.value = false }
}
async function loadScorebook(): Promise<void> {
  if (classSubjectId.value === null) { scorebook.value = null; scoreGrid.value = null; return }
  try {
    scorebook.value = await fetchScorebookByClassSubject(token.value, classSubjectId.value)
    await loadScoreGrid()
  } catch (error) {
    scorebook.value = null; scoreGrid.value = null
    contextError.value = messageFor(error, 'Không thể tải sổ điểm.')
  }
}
async function openDetail(requestId: number): Promise<void> { detailVisible.value = true; detailLoading.value = true; detailError.value = ''; try { detail.value = await fetchScoreChangeRequest(token.value, requestId) } catch (error) { detailError.value = messageFor(error, 'Không thể tải chi tiết yêu cầu.') } finally { detailLoading.value = false } }
async function submitCreate(request: CreateScoreChangeRequest): Promise<void> { if (scorebook.value === null) return; formLoading.value = true; mutationError.value = ''; try { await createScoreChangeRequest(token.value, request); formVisible.value = false; page.value = 0; await loadRequests() } catch (error) { mutationError.value = messageFor(error, 'Không thể gửi yêu cầu sửa điểm.') } finally { formLoading.value = false } }
function openCreateForm(): void {
  selectedStudentId.value = null
  selectedColumnId.value = null
  mutationError.value = ''
  formVisible.value = true
}
async function applyMutation(action: 'approve' | 'cancel' | 'reject'): Promise<void> {
  if (!detail.value) return
  detailLoading.value = true; detailError.value = ''
  try {
    if (action === 'approve') detail.value = await approveScoreChangeRequest(token.value, detail.value.requestId)
    if (action === 'cancel') detail.value = await cancelScoreChangeRequest(token.value, detail.value.requestId)
    if (action === 'reject') detail.value = await rejectScoreChangeRequest(token.value, detail.value.requestId, { rejectionReason: rejectionReason.value.trim() })
    rejectVisible.value = false; rejectionReason.value = ''; await loadRequests()
  } catch (error) { detailError.value = messageFor(error, 'Không thể cập nhật yêu cầu.') } finally { detailLoading.value = false }
}
function confirmApprove(): void { if (window.confirm('Duyệt yêu cầu này sẽ áp dụng điểm theo xử lý của hệ thống. Bạn có chắc chắn muốn tiếp tục không?')) void applyMutation('approve') }
function confirmCancel(): void { if (window.confirm('Bạn có chắc chắn muốn hủy yêu cầu này không?')) void applyMutation('cancel') }
function openReject(): void { rejectionReason.value = ''; rejectVisible.value = true }
function submitReject(): void { if (!rejectionReason.value.trim()) return; void applyMutation('reject') }
function changePage(nextPage: number, nextSize: number): void { page.value = nextPage; pageSize.value = nextSize; void loadRequests() }

watch(academicYearId, () => { void Promise.all([loadSemesters(), loadClasses()]) })
watch([classId, semesterId], () => { void loadClassSubjects() })
watch(classSubjectId, () => { void loadScorebook() })
onMounted(() => { void loadContext(); void loadRequests() })
</script>

<template>
  <main class="content-page score-change-page">
    <header class="page-heading"><div><p class="eyebrow">V2 · SCORE CHANGE</p><h1>Yêu cầu sửa điểm</h1><p>Gửi và theo dõi yêu cầu sửa điểm theo bối cảnh lớp học, không cần nhớ mã kỹ thuật.</p></div><Button v-if="canUseModule && scorebook" label="Tạo yêu cầu" icon="pi pi-plus" :disabled="scoreGridLoading || !scoreGrid" @click="openCreateForm" /></header>
    <p v-if="!canUseModule" class="form-alert form-alert-warning" role="alert">Tài khoản hiện tại không thuộc phạm vi của chức năng này.</p>
    <section class="content-surface score-change-context"><div class="section-heading"><div><h2>Bối cảnh học vụ</h2><p class="section-caption">{{ contextLabel || 'Chọn bối cảnh để tạo yêu cầu' }}</p></div><span v-if="contextLoading" role="status">Đang tải...</span></div><div class="context-grid"><div class="field-group"><label for="change-year">Năm học</label><Select id="change-year" v-model="academicYearId" :options="years" option-label="code" option-value="id" placeholder="Chọn năm học" fluid /></div><div class="field-group"><label for="change-semester">Học kỳ</label><Select id="change-semester" v-model="semesterId" :options="semesters" option-label="name" option-value="id" placeholder="Chọn học kỳ" fluid /></div><div class="field-group"><label for="change-class">Lớp</label><Select id="change-class" v-model="classId" :options="classes" option-label="className" option-value="id" placeholder="Chọn lớp" fluid /></div><div class="field-group"><label for="change-subject">Môn học</label><Select id="change-subject" v-model="classSubjectId" :options="classSubjectOptions" option-label="label" option-value="id" placeholder="Chọn môn học" fluid /></div></div><p v-if="contextError" class="form-alert form-alert-error" role="alert">{{ contextError }}</p><p v-if="contextLabel && !scorebook" class="field-hint">Chưa có sổ điểm hoặc chưa thể tải cột điểm cho bối cảnh này.</p></section>
    <section class="content-surface"><div class="filter-bar"><div class="field-group"><label for="change-status">Trạng thái</label><Select id="change-status" v-model="status" :options="statusOptions" option-label="label" option-value="value" fluid /></div><div class="field-group"><label for="change-student">Mã học sinh</label><InputText id="change-student" v-model="studentCode" placeholder="Lọc theo mã học sinh" fluid /></div><Button label="Tìm kiếm" icon="pi pi-search" :loading="listLoading" @click="page = 0; loadRequests()" /></div><p v-if="listError" class="form-alert form-alert-error" role="alert">{{ listError }} <Button label="Thử lại" text @click="loadRequests" /></p><div v-if="listLoading" class="page-state page-state-loading" role="status">Đang tải danh sách yêu cầu...</div><div v-else-if="listState === 'empty'" class="empty-state"><i class="pi pi-inbox" aria-hidden="true" /><p>Chưa có yêu cầu sửa điểm phù hợp.</p></div><div v-else-if="listState === 'forbidden'" class="form-alert form-alert-warning" role="alert">Bạn không có quyền xem danh sách yêu cầu này.</div><div v-else-if="listState === 'not-found'" class="form-alert form-alert-warning" role="alert">Không tìm thấy dữ liệu yêu cầu.</div><div v-else-if="listState === 'error'" class="form-alert form-alert-error" role="alert">Không thể tải dữ liệu. Vui lòng thử lại.</div><div v-else class="table-scroll"><table><thead><tr><th>Học sinh</th><th>Cột điểm</th><th>Điểm đề xuất</th><th>Trạng thái</th><th>Ngày gửi</th><th>Thao tác</th></tr></thead><tbody><tr v-for="item in requests" :key="item.requestId"><td><strong>{{ item.studentCode }}</strong><br><span class="field-hint">{{ item.studentName }}</span></td><td>Cột điểm đã chọn</td><td>{{ item.proposedValue === null ? scoreLabels[item.proposedStatus] : item.proposedValue }}</td><td><Tag :value="statusLabels[item.status]" /></td><td>{{ formatScoreChangeRequestDateTime(item.requestedAt) }}</td><td><Button label="Xem chi tiết" text @click="openDetail(item.requestId)" /></td></tr></tbody></table></div><ServerPagination v-if="totalElements > 0" :page="page" :page-size="pageSize" :total-records="totalElements" @page-change="changePage" /></section>
    <Dialog v-model:visible="formVisible" modal header="Tạo yêu cầu sửa điểm" :style="{ width: 'min(620px, 94vw)' }"><p v-if="mutationError" class="form-alert form-alert-error" role="alert">{{ mutationError }}</p><p v-if="scoreGridError" class="form-alert form-alert-error" role="alert">{{ scoreGridError }}</p><div class="create-selection"><div class="field-group"><label for="create-student">Học sinh</label><Select id="create-student" v-model="selectedStudentId" :options="studentOptions" option-label="label" option-value="studentId" placeholder="Chọn học sinh" fluid :loading="scoreGridLoading" :disabled="formLoading || scoreGridLoading" /></div><div class="field-group"><label for="create-column">Cột điểm</label><Select id="create-column" v-model="selectedColumnId" :options="columnOptions" option-label="label" option-value="columnId" placeholder="Chọn cột điểm" fluid :loading="scoreGridLoading" :disabled="formLoading || scoreGridLoading" /></div></div><p v-if="!createContext" class="field-hint selection-hint">Chọn học sinh và cột điểm để xem điểm hiện tại trước khi đề xuất thay đổi.</p><ScoreChangeRequestForm v-if="createContext" :context="createContext" :loading="formLoading" @submit="submitCreate" @cancel="formVisible = false" /></Dialog>
    <Dialog v-model:visible="detailVisible" modal header="" :style="{ width: 'min(620px, 94vw)' }"><p v-if="detailError" class="form-alert form-alert-error" role="alert">{{ detailError }}</p><div v-if="detailLoading && !detail" class="page-state page-state-loading" role="status">Đang tải chi tiết...</div><ScoreChangeRequestDetail v-else-if="detail" :detail="detail" :can-review="canReview" :can-cancel="roles.includes('ADMIN') || roles.includes('TEACHER')" :loading="detailLoading" @approve="confirmApprove" @reject="openReject" @cancel="confirmCancel" /></Dialog>
    <Dialog v-model:visible="rejectVisible" modal header="Từ chối yêu cầu" :style="{ width: 'min(500px, 94vw)' }"><div class="field-group"><label for="rejection-reason">Lý do từ chối</label><textarea id="rejection-reason" v-model="rejectionReason" rows="4" maxlength="1000" class="p-textarea" /></div><div class="dialog-actions"><Button label="Đóng" text @click="rejectVisible = false" /><Button label="Xác nhận từ chối" severity="danger" :disabled="!rejectionReason.trim()" :loading="detailLoading" @click="submitReject" /></div></Dialog>
  </main>
</template>

<style scoped>
.score-change-page { max-width: 1240px; margin: 0 auto; }
.score-change-context { margin-bottom: 20px; }
.context-grid, .filter-bar { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; align-items: end; }
.filter-bar { grid-template-columns: minmax(200px, 220px) minmax(280px, 1fr) auto; margin-bottom: 18px; }
.filter-bar > .p-button { min-height: 42px; align-self: end; white-space: nowrap; }
.table-scroll { overflow-x: auto; } table { width: 100%; border-collapse: collapse; min-width: 760px; } th, td { padding: 12px 10px; border-bottom: 1px solid var(--surface-border); text-align: left; vertical-align: top; } th { color: var(--text-color-secondary); font-size: .85rem; }
.detail-grid { display: grid; gap: 12px; grid-template-columns: repeat(2, minmax(0, 1fr)); } dt { color: var(--text-color-secondary); font-size: .85rem; } dd { margin: 4px 0 0; white-space: pre-wrap; }
.create-selection { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; margin-bottom: 14px; }
.selection-preview { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; padding: 12px 14px; border: 1px solid var(--surface-border); border-radius: 8px; background: var(--surface-50); }
.selection-preview span { color: var(--text-color-secondary); }
.selection-hint { margin: 0 0 14px; }
.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 18px; }
@media (max-width: 800px) { .context-grid, .filter-bar { grid-template-columns: 1fr 1fr; } .filter-bar > :last-child { grid-column: 1 / -1; } }
@media (max-width: 560px) { .context-grid, .filter-bar, .detail-grid, .create-selection { grid-template-columns: 1fr; } }
</style>
