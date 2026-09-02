<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import Button from 'primevue/button'
import ConfirmDialog from 'primevue/confirmdialog'
import { useConfirm } from 'primevue/useconfirm'
import { useRouter } from 'vue-router'

import AssessmentColumnDialog from '@/components/AssessmentColumnDialog.vue'
import AssessmentColumnPanel from '@/components/AssessmentColumnPanel.vue'
import BulkScoreEntryDialog from '@/components/BulkScoreEntryDialog.vue'
import FormAlert from '@/components/FormAlert.vue'
import ScorebookContextPanel from '@/components/ScorebookContextPanel.vue'
import ScorebookStatusHeader from '@/components/ScorebookStatusHeader.vue'
import ScoreEntryDialog from '@/components/ScoreEntryDialog.vue'
import ScoreGrid from '@/components/ScoreGrid.vue'
import SkillWeightPanel from '@/components/SkillWeightPanel.vue'
import {
  fetchAcademicYears,
  fetchClassSubjects,
  fetchSchoolClasses,
  fetchSemesters,
  fetchSubjects,
} from '@/services/academicApi'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import {
  bulkUpsertStudentScores,
  createAssessmentColumn,
  createScorebook,
  deactivateAssessmentColumn,
  fetchScorebook,
  fetchScorebookByClassSubject,
  fetchScoreGrid,
  openScorebook,
  publishScorebook,
  updateAssessmentColumn,
  upsertSkillWeight,
  upsertStudentScore,
} from '@/services/scorebookApi'
import type { AcademicYear, ClassSubject, SchoolClass, Semester, Subject } from '@/types/academic'
import { isApiError } from '@/types/api'
import type {
  AssessmentColumn,
  BulkUpsertStudentScoreRequest,
  CreateAssessmentColumnRequest,
  ScoreGridColumn,
  Scorebook,
  StudentScore,
  StudentScoreGrid,
  StudentScoreGridRow,
  UpdateAssessmentColumnRequest,
  UpsertSkillWeightRequest,
  UpsertStudentScoreRequest,
} from '@/types/scorebook'
import type { UserRole } from '@/types/user'

type LookupState = 'idle' | 'loading' | 'empty' | 'ready' | 'error'

const router = useRouter()
const confirm = useConfirm()
const academicYears = ref<AcademicYear[]>([])
const semesters = ref<Semester[]>([])
const classes = ref<SchoolClass[]>([])
const subjects = ref<Subject[]>([])
const classSubjects = ref<ClassSubject[]>([])
const selectedAcademicYearId = ref<number | null>(null)
const selectedSemesterId = ref<number | null>(null)
const selectedClassId = ref<number | null>(null)
const selectedClassSubjectId = ref<number | null>(null)
const scorebook = ref<Scorebook | null>(null)
const grid = ref<StudentScoreGrid | null>(null)
const page = ref(0)
const size = ref(10)
const loading = ref(true)
const gridLoading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const statusMessage = ref('')
const conflictMessage = ref('')
const forbidden = ref(false)
const lookupState = ref<LookupState>('idle')
const activeTab = ref<'grid' | 'columns'>('grid')
const columnDialogVisible = ref(false)
const columnDialogMode = ref<'create' | 'edit'>('create')
const selectedColumn = ref<AssessmentColumn | null>(null)
const scoreDialogVisible = ref(false)
const bulkDialogVisible = ref(false)
const selectedStudent = ref<StudentScoreGridRow | null>(null)
const selectedScore = ref<StudentScore | null>(null)
const selectedGridColumn = ref<ScoreGridColumn | null>(null)
const dialogError = ref('')
let contextRequestId = 0
let lookupRequestId = 0
let gridRequestId = 0

const selectedClassSubject = computed(() =>
  classSubjects.value.find((item) => item.id === selectedClassSubjectId.value) ?? null)
const selectedSubject = computed(() =>
  subjects.value.find((item) => item.id === selectedClassSubject.value?.subjectId) ?? null)
const roles = computed<UserRole[]>(() => getAuthSession()?.user.roles ?? [])
const hasRoleContract = computed(() => getAuthSession()?.user.roles !== undefined)
const canUseWorkspace = computed(() =>
  roles.value.some((role) => role === 'ADMIN' || role === 'ACADEMIC_OFFICE' || role === 'TEACHER'))
const canCreate = computed(() =>
  roles.value.some((role) => role === 'ADMIN' || role === 'ACADEMIC_OFFICE'))
const readOnlyColumns = computed(() =>
  scorebook.value?.status === 'PUBLISHED' || scorebook.value?.status === 'CLOSED')

function token(): string | null {
  const session = getAuthSession()
  if (session) return session.accessToken
  clearAuthSession()
  void router.replace({ name: 'login' })
  return null
}

function messageFor(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

function clearMessages(): void {
  errorMessage.value = ''
  statusMessage.value = ''
  conflictMessage.value = ''
  forbidden.value = false
}

function resetDialogs(): void {
  columnDialogVisible.value = false
  scoreDialogVisible.value = false
  bulkDialogVisible.value = false
  selectedColumn.value = null
  selectedStudent.value = null
  selectedScore.value = null
  selectedGridColumn.value = null
  dialogError.value = ''
}

function resetScorebook(): void {
  scorebook.value = null
  grid.value = null
  page.value = 0
  lookupState.value = 'idle'
  resetDialogs()
}

async function loadGrid(nextPage = page.value, nextSize = size.value): Promise<void> {
  const accessToken = token()
  const currentScorebook = scorebook.value
  if (!accessToken || currentScorebook === null) return
  const requestId = ++gridRequestId
  gridLoading.value = true
  try {
    const response = await fetchScoreGrid(accessToken, currentScorebook.id, nextPage, nextSize)
    if (gridRequestId !== requestId || scorebook.value?.id !== currentScorebook.id) return
    grid.value = response
    page.value = response.page
    size.value = response.size
  } catch (error) {
    if (isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    errorMessage.value = messageFor(error, 'Không thể tải bảng điểm.')
  } finally {
    if (gridRequestId === requestId) gridLoading.value = false
  }
}

async function lookupSelectedScorebook(): Promise<void> {
  const accessToken = token()
  const classSubjectId = selectedClassSubjectId.value
  resetScorebook()
  clearMessages()
  if (!accessToken || classSubjectId === null) return
  const requestId = ++lookupRequestId
  lookupState.value = 'loading'
  try {
    const response = await fetchScorebookByClassSubject(accessToken, classSubjectId)
    if (lookupRequestId !== requestId || selectedClassSubjectId.value !== classSubjectId) return
    scorebook.value = response
    lookupState.value = 'ready'
    await loadGrid(0, size.value)
  } catch (error) {
    if (lookupRequestId !== requestId || selectedClassSubjectId.value !== classSubjectId) return
    if (isApiError(error, 401)) return
    if (isApiError(error, 404)) {
      lookupState.value = 'empty'
      return
    }
    forbidden.value = isApiError(error, 403)
    lookupState.value = 'error'
    errorMessage.value = messageFor(error, 'Không thể tìm sổ điểm của môn học đã chọn.')
  }
}

async function loadContext(): Promise<void> {
  const accessToken = token()
  const yearId = selectedAcademicYearId.value
  const semesterId = selectedSemesterId.value
  const classId = selectedClassId.value
  classSubjects.value = []
  selectedClassSubjectId.value = null
  resetScorebook()
  if (!accessToken || yearId === null || semesterId === null || classId === null) return
  const requestId = ++contextRequestId
  loading.value = true
  clearMessages()
  try {
    const items = await fetchClassSubjects(accessToken, classId, semesterId)
    if (contextRequestId !== requestId) return
    classSubjects.value = items
    selectedClassSubjectId.value = items[0]?.id ?? null
  } catch (error) {
    if (isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    errorMessage.value = messageFor(error, 'Không thể tải danh sách môn học của lớp.')
  } finally {
    if (contextRequestId === requestId) loading.value = false
  }
  if (contextRequestId === requestId && selectedClassSubjectId.value !== null) {
    await lookupSelectedScorebook()
  }
}

async function loadYearContext(yearId: number | null): Promise<void> {
  const accessToken = token()
  semesters.value = []
  classes.value = []
  classSubjects.value = []
  selectedSemesterId.value = null
  selectedClassId.value = null
  selectedClassSubjectId.value = null
  resetScorebook()
  if (!accessToken || yearId === null) return
  loading.value = true
  clearMessages()
  try {
    const [semesterItems, classItems] = await Promise.all([
      fetchSemesters(accessToken, yearId),
      fetchSchoolClasses(accessToken, yearId),
    ])
    if (selectedAcademicYearId.value !== yearId) return
    semesters.value = semesterItems
    classes.value = classItems
    selectedSemesterId.value = semesterItems.find((item) => item.status === 'ACTIVE')?.id
      ?? semesterItems[0]?.id
      ?? null
    selectedClassId.value = classItems.find((item) => item.status !== 'CLOSED')?.id
      ?? classItems[0]?.id
      ?? null
  } catch (error) {
    if (isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    errorMessage.value = messageFor(error, 'Không thể tải context scorebook.')
  } finally {
    loading.value = false
  }
  await loadContext()
}

async function load(): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  clearMessages()
  if (!hasRoleContract.value || !canUseWorkspace.value) {
    loading.value = false
    forbidden.value = true
    errorMessage.value = hasRoleContract.value
      ? 'Tài khoản không có quyền truy cập workspace sổ điểm.'
      : 'Phiên đăng nhập chưa có thông tin vai trò. Vui lòng đăng nhập lại.'
    return
  }
  loading.value = true
  try {
    const [yearItems, subjectItems] = await Promise.all([
      fetchAcademicYears(accessToken),
      fetchSubjects(accessToken, 'ACTIVE'),
    ])
    academicYears.value = yearItems
    subjects.value = subjectItems
    selectedAcademicYearId.value = yearItems.find((item) => item.status === 'ACTIVE')?.id
      ?? yearItems[0]?.id
      ?? null
  } catch (error) {
    if (isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    errorMessage.value = messageFor(error, 'Không thể tải danh mục scorebook.')
    loading.value = false
    return
  }
  await loadYearContext(selectedAcademicYearId.value)
}

async function reloadAuthoritative(showLoading = true): Promise<void> {
  const accessToken = token()
  const currentScorebook = scorebook.value
  if (!accessToken || currentScorebook === null) return
  if (showLoading) saving.value = true
  try {
    scorebook.value = await fetchScorebook(accessToken, currentScorebook.id)
    await loadGrid(page.value, size.value)
    rebindSelectedScore()
  } catch (error) {
    if (!isApiError(error, 401)) {
      forbidden.value = isApiError(error, 403)
      errorMessage.value = messageFor(error, 'Không thể tải lại sổ điểm.')
    }
  } finally {
    if (showLoading) saving.value = false
  }
}

function rebindSelectedScore(): void {
  if (!selectedStudent.value || !selectedGridColumn.value || !grid.value) return
  const row = grid.value.students.find((item) => item.studentId === selectedStudent.value?.studentId)
  selectedStudent.value = row ?? null
  selectedScore.value = row?.scores[String(selectedGridColumn.value.columnId)] ?? null
}

async function handleConflict(error: unknown): Promise<boolean> {
  if (!isApiError(error, 409)) return false
  conflictMessage.value = 'Dữ liệu đã thay đổi ở nơi khác. Hệ thống đã tải lại phiên bản mới; vui lòng kiểm tra trước khi lưu lại.'
  dialogError.value = conflictMessage.value
  await reloadAuthoritative(false)
  return true
}

async function create(): Promise<void> {
  const accessToken = token()
  if (!accessToken || selectedClassSubjectId.value === null || !canCreate.value) return
  saving.value = true
  clearMessages()
  try {
    scorebook.value = await createScorebook(accessToken, { classSubjectId: selectedClassSubjectId.value })
    lookupState.value = 'ready'
    await loadGrid(0, size.value)
    statusMessage.value = 'Đã tạo sổ điểm.'
  } catch (error) {
    if (isApiError(error, 401)) return
    if (isApiError(error, 409)) {
      await lookupSelectedScorebook()
      conflictMessage.value = 'Sổ điểm đã tồn tại và vừa được tải lại.'
      return
    }
    forbidden.value = isApiError(error, 403)
    errorMessage.value = messageFor(error, 'Không thể tạo sổ điểm.')
  } finally {
    saving.value = false
  }
}

async function lifecycle(action: 'open' | 'publish'): Promise<void> {
  const accessToken = token()
  if (!accessToken || scorebook.value === null) return
  saving.value = true
  clearMessages()
  try {
    scorebook.value = action === 'open'
      ? await openScorebook(accessToken, scorebook.value.id)
      : await publishScorebook(accessToken, scorebook.value.id)
    await loadGrid(page.value, size.value)
    statusMessage.value = action === 'open' ? 'Đã mở sổ điểm.' : 'Đã công bố sổ điểm.'
  } catch (error) {
    if (isApiError(error, 401)) return
    if (await handleConflict(error)) return
    forbidden.value = isApiError(error, 403)
    errorMessage.value = messageFor(error, 'Không thể cập nhật lifecycle sổ điểm.')
  } finally {
    saving.value = false
  }
}

function confirmPublish(): void {
  confirm.require({
    header: 'Xác nhận công bố sổ điểm',
    message: 'Sau khi công bố, sổ điểm chuyển sang chế độ chỉ đọc. Bạn có muốn tiếp tục?',
    acceptLabel: 'Công bố',
    rejectLabel: 'Hủy',
    accept: () => void lifecycle('publish'),
  })
}

function openColumnDialog(mode: 'create' | 'edit', column: AssessmentColumn | null = null): void {
  dialogError.value = ''
  columnDialogMode.value = mode
  selectedColumn.value = column
  columnDialogVisible.value = true
}

async function saveColumn(request: CreateAssessmentColumnRequest | UpdateAssessmentColumnRequest): Promise<void> {
  const accessToken = token()
  if (!accessToken || scorebook.value === null) return
  saving.value = true
  dialogError.value = ''
  try {
    if (columnDialogMode.value === 'create') {
      await createAssessmentColumn(accessToken, scorebook.value.id, request as CreateAssessmentColumnRequest)
    } else if (selectedColumn.value) {
      await updateAssessmentColumn(accessToken, selectedColumn.value.id, request as UpdateAssessmentColumnRequest)
    }
    columnDialogVisible.value = false
    await reloadAuthoritative(false)
    statusMessage.value = 'Đã cập nhật cấu hình cột.'
  } catch (error) {
    if (isApiError(error, 401)) return
    if (await handleConflict(error)) return
    dialogError.value = messageFor(error, 'Không thể lưu assessment column.')
  } finally {
    saving.value = false
  }
}

function confirmDeactivateColumn(column: AssessmentColumn): void {
  confirm.require({
    header: 'Xác nhận vô hiệu hóa cột',
    message: `Vô hiệu hóa cột ${column.columnName || column.assessmentType}?`,
    acceptLabel: 'Vô hiệu hóa',
    rejectLabel: 'Hủy',
    accept: () => void deactivateColumn(column),
  })
}

async function deactivateColumn(column: AssessmentColumn): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  saving.value = true
  clearMessages()
  try {
    await deactivateAssessmentColumn(accessToken, column.id)
    await reloadAuthoritative(false)
    statusMessage.value = 'Đã vô hiệu hóa assessment column.'
  } catch (error) {
    if (isApiError(error, 401)) return
    if (await handleConflict(error)) return
    errorMessage.value = messageFor(error, 'Không thể vô hiệu hóa assessment column.')
  } finally {
    saving.value = false
  }
}

function openScoreDialog(student: StudentScoreGridRow, column: ScoreGridColumn, score: StudentScore | null): void {
  dialogError.value = ''
  selectedStudent.value = student
  selectedGridColumn.value = column
  selectedScore.value = score
  scoreDialogVisible.value = true
}

function openBulkDialog(column: ScoreGridColumn): void {
  dialogError.value = ''
  selectedGridColumn.value = column
  bulkDialogVisible.value = true
}

async function saveScore(request: UpsertStudentScoreRequest): Promise<void> {
  const accessToken = token()
  if (!accessToken || !selectedStudent.value || !selectedGridColumn.value) return
  saving.value = true
  dialogError.value = ''
  try {
    await upsertStudentScore(
      accessToken,
      selectedGridColumn.value.columnId,
      selectedStudent.value.studentId,
      request,
    )
    scoreDialogVisible.value = false
    await loadGrid(page.value, size.value)
    statusMessage.value = 'Đã lưu điểm.'
  } catch (error) {
    if (isApiError(error, 401)) return
    if (await handleConflict(error)) return
    dialogError.value = messageFor(error, 'Không thể lưu điểm.')
  } finally {
    saving.value = false
  }
}

async function saveBulk(request: BulkUpsertStudentScoreRequest): Promise<void> {
  const accessToken = token()
  if (!accessToken || !selectedGridColumn.value) return
  saving.value = true
  dialogError.value = ''
  try {
    await bulkUpsertStudentScores(accessToken, selectedGridColumn.value.columnId, request)
    bulkDialogVisible.value = false
    await loadGrid(page.value, size.value)
    statusMessage.value = 'Đã lưu điểm hàng loạt.'
  } catch (error) {
    if (isApiError(error, 401)) return
    if (await handleConflict(error)) return
    dialogError.value = messageFor(error, 'Không thể lưu điểm hàng loạt.')
  } finally {
    saving.value = false
  }
}

async function saveSkillWeight(request: UpsertSkillWeightRequest): Promise<void> {
  const accessToken = token()
  if (!accessToken || !scorebook.value) return
  saving.value = true
  dialogError.value = ''
  try {
    scorebook.value = await upsertSkillWeight(accessToken, scorebook.value.id, request)
    await loadGrid(page.value, size.value)
    statusMessage.value = 'Đã lưu trọng số môn kỹ năng.'
  } catch (error) {
    if (isApiError(error, 401)) return
    if (await handleConflict(error)) return
    dialogError.value = messageFor(error, 'Không thể lưu trọng số.')
  } finally {
    saving.value = false
  }
}

function changePage(nextPage: number, nextSize: number): void {
  void loadGrid(nextPage, nextSize)
}

watch(selectedAcademicYearId, (value, previous) => {
  if (!loading.value && value !== previous) void loadYearContext(value)
})
watch([selectedSemesterId, selectedClassId], () => {
  if (!loading.value) void loadContext()
})
watch(selectedClassSubjectId, (value, previous) => {
  if (!loading.value && lookupState.value !== 'loading' && value !== previous) void lookupSelectedScorebook()
})
onMounted(() => { void load() })
</script>

<template>
  <div class="page-heading">
    <div>
      <p class="eyebrow">Scorebook workspace</p>
      <h1>Sổ điểm</h1>
      <p>Chọn context học vụ trước khi mở hoặc tạo sổ điểm.</p>
    </div>
    <div class="page-heading-actions">
      <Button label="Làm mới" icon="pi pi-refresh" severity="secondary" outlined :loading="loading" @click="load" />
    </div>
  </div>

  <ConfirmDialog />
  <FormAlert v-if="statusMessage" tone="success" :message="statusMessage" />
  <FormAlert v-if="conflictMessage" tone="warning" :message="conflictMessage" />
  <FormAlert v-if="errorMessage && !forbidden" tone="error" :message="errorMessage" />
  <FormAlert v-if="forbidden" tone="warning" :message="errorMessage || 'Bạn không có quyền thao tác scorebook này. Phiên đăng nhập vẫn được giữ nguyên.'" />

  <ScorebookContextPanel
    v-model:academic-year-id="selectedAcademicYearId"
    v-model:semester-id="selectedSemesterId"
    v-model:class-id="selectedClassId"
    v-model:class-subject-id="selectedClassSubjectId"
    :academic-years="academicYears"
    :semesters="semesters"
    :classes="classes"
    :class-subjects="classSubjects"
    :subjects="subjects"
    :loading="loading"
  />

  <div v-if="loading || lookupState === 'loading'" class="page-state page-state-loading" role="status">
    <i class="pi pi-spin pi-spinner" aria-hidden="true" />
    <span>Đang tải context và sổ điểm...</span>
  </div>

  <template v-else>
    <ScorebookStatusHeader
      :scorebook="scorebook"
      :loading="saving"
      @reload="reloadAuthoritative"
      @open="lifecycle('open')"
      @publish="confirmPublish"
    />

    <div v-if="lookupState === 'empty' && selectedClassSubject" class="content-surface page-state">
      <p>Chưa có sổ điểm cho môn học đã chọn.</p>
      <Button v-if="canCreate" label="Tạo sổ điểm" icon="pi pi-plus" :loading="saving" @click="create" />
      <span v-else class="field-hint">Giáo viên không thể tạo sổ điểm; vui lòng liên hệ giáo vụ.</span>
    </div>

    <template v-if="scorebook">
      <div class="tab-strip">
        <Button label="Bảng điểm" icon="pi pi-table" :outlined="activeTab !== 'grid'" @click="activeTab = 'grid'" />
        <Button label="Cấu hình cột" icon="pi pi-sliders-h" :outlined="activeTab !== 'columns'" @click="activeTab = 'columns'" />
      </div>
      <ScoreGrid
        v-if="activeTab === 'grid'"
        :grid="grid"
        :loading="gridLoading"
        :read-only="scorebook.status !== 'OPEN'"
        @edit="openScoreDialog"
        @bulk-edit="openBulkDialog"
        @page-change="changePage"
      />
      <template v-else>
        <AssessmentColumnPanel
          :columns="scorebook.columns"
          :read-only="readOnlyColumns"
          @create="openColumnDialog('create')"
          @edit="openColumnDialog('edit', $event)"
          @deactivate="confirmDeactivateColumn"
        />
        <SkillWeightPanel
          v-if="selectedSubject?.subjectType === 'SKILL'"
          :config="scorebook.skillWeightConfig"
          :read-only="readOnlyColumns"
          :saving="saving"
          :error-message="dialogError"
          @save="saveSkillWeight"
        />
      </template>
    </template>
  </template>

  <AssessmentColumnDialog
    v-model:visible="columnDialogVisible"
    :mode="columnDialogMode"
    :column="selectedColumn"
    :saving="saving"
    :error-message="dialogError"
    @save="saveColumn"
    @cancel="columnDialogVisible = false"
  />
  <ScoreEntryDialog
    v-model:visible="scoreDialogVisible"
    :student-name="selectedStudent?.studentName"
    :score="selectedScore"
    :saving="saving"
    :error-message="dialogError"
    @save="saveScore"
    @cancel="scoreDialogVisible = false"
  />
  <BulkScoreEntryDialog
    v-model:visible="bulkDialogVisible"
    :column="selectedGridColumn"
    :students="grid?.students ?? []"
    :saving="saving"
    :error-message="dialogError"
    @save="saveBulk"
    @cancel="bulkDialogVisible = false"
  />
</template>

<style scoped>
.scorebook-context-panel { margin-bottom: 20px; }
.scorebook-context-panel .search-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.page-state p { max-width: 680px; margin: 0; color: #475569; text-align: center; }
@media (max-width: 900px) { .scorebook-context-panel .search-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 680px) { .scorebook-context-panel .search-grid { grid-template-columns: 1fr; } }
</style>
