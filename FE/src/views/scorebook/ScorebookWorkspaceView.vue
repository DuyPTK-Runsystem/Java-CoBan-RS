<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import Button from 'primevue/button'
import ConfirmDialog from 'primevue/confirmdialog'
import Dialog from 'primevue/dialog'
import { useConfirm } from 'primevue/useconfirm'

import AssessmentColumnDialog from '@/components/scorebook/AssessmentColumnDialog.vue'
import AssessmentColumnPanel from '@/components/scorebook/AssessmentColumnPanel.vue'
import BulkScoreEntryDialog from '@/components/scorebook/BulkScoreEntryDialog.vue'
import FormAlert from '@/components/common/FormAlert.vue'
import ScorebookContextPanel from '@/components/scorebook/ScorebookContextPanel.vue'
import ScorebookStatusHeader from '@/components/scorebook/ScorebookStatusHeader.vue'
import ScoreChangeRequestForm from '@/components/score-change/ScoreChangeRequestForm.vue'
import ScoreEntryDialog from '@/components/scorebook/ScoreEntryDialog.vue'
import ScoreGrid from '@/components/scorebook/ScoreGrid.vue'
import SkillWeightPanel from '@/components/academic/SkillWeightPanel.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { useScorebookDialogs } from '@/composables/useScorebookDialogs'
import {
  fetchAcademicYears,
  fetchClassSubjects,
  fetchSchoolClasses,
  fetchSemesters,
  fetchSubjects,
} from '@/services/academicApi'
import { createScoreChangeRequest } from '@/services/scoreChangeRequestApi'
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
import { extractApiErrorMessage, isApiError } from '@/types/api'
import type {
  AssessmentColumn,
  BulkUpsertStudentScoreRequest,
  CreateAssessmentColumnRequest,
  Scorebook,
  StudentScoreGrid,
  UpdateAssessmentColumnRequest,
  UpsertSkillWeightRequest,
  UpsertStudentScoreRequest,
} from '@/types/scorebook'
import type { CreateScoreChangeRequest } from '@/types/scoreChangeRequest'

type LookupState = 'idle' | 'loading' | 'empty' | 'ready' | 'error'

const confirm = useConfirm()
const { roles, hasRoleContract, requireAccessToken: token } = useAuthSession()
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
const {
  columnDialogVisible, columnDialogMode, selectedColumn, scoreDialogVisible,
  scoreChangeRequestDialogVisible, bulkDialogVisible, selectedStudent,
  selectedScore, selectedGridColumn, scoreChangeRequestContext, dialogError,
  reset: resetDialogs, openColumn: openColumnDialog, openScore: openScoreDialog,
  openBulk: openBulkDialog, openChangeRequest: openScoreChangeRequest,
} = useScorebookDialogs()
let contextRequestId = 0
let lookupRequestId = 0
let gridRequestId = 0

const selectedClassSubject = computed(() =>
  classSubjects.value.find((item) => item.id === selectedClassSubjectId.value) ?? null)
const selectedClass = computed(() =>
  classes.value.find((item) => item.id === selectedClassId.value) ?? null)
const selectedSemester = computed(() =>
  semesters.value.find((item) => item.id === selectedSemesterId.value) ?? null)
const selectedSubject = computed(() =>
  subjects.value.find((item) => item.id === selectedClassSubject.value?.subjectId) ?? null)
const selectedContextLabel = computed(() => {
  if (!selectedClass.value || !selectedSubject.value || !selectedSemester.value) return ''
  return `${selectedClass.value.classCode} · ${selectedSubject.value.name} · ${selectedSemester.value.name}`
})
const canUseWorkspace = computed(() =>
  roles.value.some((role) => role === 'ADMIN' || role === 'ACADEMIC_OFFICE' || role === 'TEACHER'))
const canCreate = computed(() =>
  roles.value.some((role) => role === 'ADMIN' || role === 'ACADEMIC_OFFICE'))
const readOnlyColumns = computed(() =>
  scorebook.value?.status === 'PUBLISHED' || scorebook.value?.status === 'CLOSED')
const assessmentTypeLabels: Record<string, string> = { KTTT: 'Thường xuyên', 'KTĐK': 'Giữa kỳ', KTCK: 'Cuối kỳ' }

function clearMessages(): void {
  errorMessage.value = ''
  statusMessage.value = ''
  conflictMessage.value = ''
  forbidden.value = false
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
    errorMessage.value = extractApiErrorMessage(error, 'Không thể tải bảng điểm.')
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
    errorMessage.value = extractApiErrorMessage(error, 'Không thể tìm sổ điểm của môn học đã chọn.')
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
    errorMessage.value = extractApiErrorMessage(error, 'Không thể tải danh sách môn học của lớp.')
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
    errorMessage.value = extractApiErrorMessage(error, 'Không thể tải thông tin sổ điểm.')
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
    errorMessage.value = extractApiErrorMessage(error, 'Không thể tải danh mục sổ điểm.')
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
      errorMessage.value = extractApiErrorMessage(error, 'Không thể tải lại sổ điểm.')
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
  const apiMessage = extractApiErrorMessage(error, '')
  conflictMessage.value = apiMessage || 'Dữ liệu đã thay đổi ở nơi khác. Hệ thống đã tải lại phiên bản mới; vui lòng kiểm tra trước khi lưu lại.'
  dialogError.value = conflictMessage.value
  await reloadAuthoritative(false)
  return true
}

async function create(): Promise<void> {
  const accessToken = token()
  if (!accessToken || selectedClassSubjectId.value === null || !canCreate.value) return
  saving.value = true
  clearMessages()
  let created = false
  try {
    scorebook.value = await createScorebook(accessToken, { classSubjectId: selectedClassSubjectId.value })
    created = true
    lookupState.value = 'ready'
    scorebook.value = await openScorebook(accessToken, scorebook.value.id)
    await loadGrid(0, size.value)
    statusMessage.value = 'Đã tạo và mở sổ điểm.'
  } catch (error) {
    if (isApiError(error, 401)) return
    if (created) {
      await reloadAuthoritative(false)
      forbidden.value = isApiError(error, 403)
      errorMessage.value = `Đã tạo sổ điểm nhưng chưa thể mở. ${extractApiErrorMessage(error, 'Vui lòng thử mở lại sổ điểm.')}`
      return
    }
    if (isApiError(error, 409)) {
      await lookupSelectedScorebook()
      conflictMessage.value = 'Sổ điểm đã tồn tại và vừa được tải lại.'
      return
    }
    forbidden.value = isApiError(error, 403)
    errorMessage.value = extractApiErrorMessage(error, 'Không thể tạo sổ điểm.')
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
    errorMessage.value = extractApiErrorMessage(error, 'Không thể cập nhật trạng thái sổ điểm.')
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
    dialogError.value = extractApiErrorMessage(error, 'Không thể lưu cột điểm.')
  } finally {
    saving.value = false
  }
}

function confirmDeactivateColumn(column: AssessmentColumn): void {
  confirm.require({
    header: 'Xác nhận ngừng sử dụng cột điểm',
    message: `Cột ${column.columnName || assessmentTypeLabels[column.assessmentType] || column.assessmentType} sẽ không còn được dùng để nhập điểm. Bạn có muốn tiếp tục?`,
    acceptLabel: 'Ngừng sử dụng',
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
    statusMessage.value = 'Đã ngừng sử dụng cột điểm.'
  } catch (error) {
    if (isApiError(error, 401)) return
    if (await handleConflict(error)) return
    errorMessage.value = extractApiErrorMessage(error, 'Không thể ngừng sử dụng cột điểm.')
  } finally {
    saving.value = false
  }
}

async function submitScoreChangeRequest(request: CreateScoreChangeRequest): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  saving.value = true
  dialogError.value = ''
  try {
    await createScoreChangeRequest(accessToken, request)
    scoreChangeRequestDialogVisible.value = false
    statusMessage.value = 'Đã gửi yêu cầu sửa điểm. Yêu cầu đang chờ duyệt.'
  } catch (error) {
    if (isApiError(error, 401)) return
    if (isApiError(error, 409)) {
      dialogError.value = extractApiErrorMessage(error, 'Yêu cầu không thể gửi vì dữ liệu hoặc request đang chờ đã thay đổi.')
      return
    }
    dialogError.value = extractApiErrorMessage(error, 'Không thể gửi yêu cầu sửa điểm.')
  } finally {
    saving.value = false
  }
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
    dialogError.value = extractApiErrorMessage(error, 'Không thể lưu điểm.')
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
    dialogError.value = extractApiErrorMessage(error, 'Không thể lưu điểm hàng loạt.')
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
    dialogError.value = extractApiErrorMessage(error, 'Không thể lưu trọng số.')
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
      <h1>Sổ điểm</h1>
    </div>
    <div class="page-heading-actions">
      <Button label="Làm mới" icon="pi pi-refresh" severity="secondary" outlined :loading="loading" @click="load" />
    </div>
  </div>

  <ConfirmDialog />
  <FormAlert v-if="statusMessage" tone="success" :message="statusMessage" />
  <FormAlert v-if="conflictMessage" tone="warning" :message="conflictMessage" />
  <FormAlert v-if="errorMessage && !forbidden" tone="error" :message="errorMessage" />
  <FormAlert v-if="forbidden" tone="warning" :message="errorMessage || 'Bạn không có quyền thao tác sổ điểm này. Phiên đăng nhập vẫn được giữ nguyên.'" />

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
  <p v-if="selectedContextLabel" class="selected-scorebook-context">{{ selectedContextLabel }}</p>

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
        <Button label="Nhập điểm" icon="pi pi-table" :outlined="activeTab !== 'grid'" @click="activeTab = 'grid'" />
        <Button label="Cột điểm" icon="pi pi-sliders-h" :outlined="activeTab !== 'columns'" @click="activeTab = 'columns'" />
      </div>
      <ScoreGrid
        v-if="activeTab === 'grid'"
        :grid="grid"
        :loading="gridLoading"
        :read-only="scorebook.status === 'CLOSED'"
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
    :read-only="scorebook?.status === 'CLOSED'"
    :saving="saving"
    :error-message="dialogError"
    @save="saveScore"
    @cancel="scoreDialogVisible = false"
    @request-change="openScoreChangeRequest"
  />
  <Dialog
    v-model:visible="scoreChangeRequestDialogVisible"
    header="Tạo yêu cầu sửa điểm"
    modal
    :style="{ width: 'min(620px, calc(100vw - 32px))' }"
  >
    <ScoreChangeRequestForm
      v-if="scoreChangeRequestContext"
      :context="scoreChangeRequestContext"
      :loading="saving"
      @submit="submitScoreChangeRequest"
      @cancel="scoreChangeRequestDialogVisible = false"
    />
  </Dialog>
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

<style scoped src="@/styles/scorebook.css"></style>
