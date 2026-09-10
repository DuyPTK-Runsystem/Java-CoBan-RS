<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'

import CapacityWarningBanner from '@/components/enrollment/CapacityWarningBanner.vue'
import ClassStudentTable from '@/components/attendance/ClassStudentTable.vue'
import EnrollmentContextPanel from '@/components/enrollment/EnrollmentContextPanel.vue'
import EnrollmentMutationDialog from '@/components/enrollment/EnrollmentMutationDialog.vue'
import FormAlert from '@/components/common/FormAlert.vue'
import PageState from '@/components/common/PageState.vue'
import StudentEnrollmentHistoryDialog from '@/components/student/StudentEnrollmentHistoryDialog.vue'
import TransferEnrollmentDialog from '@/components/enrollment/TransferEnrollmentDialog.vue'
import TransferScoreAssistDialog from '@/components/enrollment/TransferScoreAssistDialog.vue'
import UnassignedStudentTable from '@/components/enrollment/UnassignedStudentTable.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { fetchAcademicYears, fetchGrades, fetchSchoolClasses, fetchSemesters } from '@/services/academicApi'
import { createBulkEnrollment, createEnrollment, fetchClassStudents, fetchStudentEnrollmentHistory, fetchTransferScoreAssist, fetchUnassignedStudents, transferEnrollment, transferWithScores } from '@/services/enrollmentApi'
import { isApiError } from '@/types/api'
import type { AcademicYear, GradeLevel, SchoolClass, Semester } from '@/types/academic'
import type { BulkEnrollmentFormValues, CapacityWarning, ClassStudent, CreateEnrollmentFormValues, EnrollmentMutation, StudentEnrollmentHistory, TransferEnrollmentFormValues, TransferScoreAssistSnapshot, TransferWithScoresRequest, UnassignedStudent } from '@/types/enrollment'
import type { LoadingState } from '@/types/ui'
const router = useRouter()
const { requireAccessToken: token } = useAuthSession()
const academicYears = ref<AcademicYear[]>([])
const grades = ref<GradeLevel[]>([])
const classes = ref<SchoolClass[]>([])
const semesters = ref<Semester[]>([])
const unassignedStudents = ref<UnassignedStudent[]>([])
const classStudents = ref<ClassStudent[]>([])
const selectedAcademicYearId = ref<number | null>(null)
const selectedGradeId = ref<number | null>(null)
const selectedClassId = ref<number | null>(null)
const selectedSemesterId = ref<number | null>(null)
const selectedUnassignedStudents = ref<UnassignedStudent[]>([])
const academicYearLoading = ref(true)
const classLoading = ref(false)
const contextError = ref('')
const contextForbidden = ref(false)
const unassignedState = ref<LoadingState>('loading')
const unassignedError = ref('')
const unassignedForbidden = ref(false)
const rosterState = ref<LoadingState>('success')
const rosterError = ref('')
const rosterForbidden = ref(false)
const mutationError = ref('')
const transferError = ref('')
const historyError = ref('')
const statusMessage = ref('')
const saving = ref(false)
const transferSaving = ref(false)
const transferAssistLoading = ref(false)
const transferAssistSaving = ref(false)
const transferAssistError = ref('')
const transferAssistVisible = ref(false)
const transferAssistSnapshot = ref<TransferScoreAssistSnapshot | null>(null)
const transferAssistValues = ref<TransferEnrollmentFormValues | null>(null)
const historyLoading = ref(false)
const mutationVisible = ref(false)
const mutationMode = ref<'single' | 'bulk'>('single')
const mutationStudents = ref<UnassignedStudent[]>([])
const transferVisible = ref(false)
const transferStudent = ref<ClassStudent | null>(null)
const historyVisible = ref(false)
const historyStudent = ref<{ studentId: number; studentCode: string; studentName: string } | null>(null)
const history = ref<StudentEnrollmentHistory[]>([])
const warnings = ref<CapacityWarning[]>([])
let loadingAcademicYearId: number | null = null

const selectedClass = computed(() => classes.value.find((schoolClass) => schoolClass.id === selectedClassId.value) ?? null)
const filteredClasses = computed(() => selectedGradeId.value === null ? classes.value : classes.value.filter((schoolClass) => schoolClass.gradeLevelId === selectedGradeId.value))
const classIsReadOnly = computed(() => selectedClass.value?.status === 'CLOSED')
const mutationClassLabel = computed(() => selectedClass.value?.classCode ?? '')
const targetClasses = computed(() => classes.value.filter((schoolClass) => schoolClass.id !== selectedClassId.value && schoolClass.status !== 'CLOSED'))

function messageFor(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

function classDisplayName(classId: number | null): string {
  if (classId === null) return 'hiện tại'
  const schoolClass = classes.value.find((item) => item.id === classId)
  return schoolClass?.className?.trim() || schoolClass?.classCode || `Lớp #${classId}`
}

function resetLists(): void {
  classes.value = []
  semesters.value = []
  classStudents.value = []
  unassignedStudents.value = []
  selectedGradeId.value = null
  selectedClassId.value = null
  selectedSemesterId.value = null
  selectedUnassignedStudents.value = []
  warnings.value = []
}

async function loadAcademicYearContext(academicYearId: number | null): Promise<void> {
  const accessToken = token()
  if (!accessToken || academicYearId === null) {
    resetLists()
    rosterState.value = 'success'
    unassignedState.value = 'success'
    return
  }
  if (loadingAcademicYearId === academicYearId) return
  loadingAcademicYearId = academicYearId
  classLoading.value = true
  unassignedState.value = 'loading'
  rosterState.value = 'success'
  contextError.value = ''
  contextForbidden.value = false
  unassignedError.value = ''
  unassignedForbidden.value = false
  selectedUnassignedStudents.value = []
  try {
    const [loadedClasses, loadedUnassigned, loadedSemesters] = await Promise.all([
      fetchSchoolClasses(accessToken, academicYearId),
      fetchUnassignedStudents(accessToken, academicYearId),
      fetchSemesters(accessToken, academicYearId),
    ])
    if (selectedAcademicYearId.value !== academicYearId) return
    classes.value = loadedClasses
    semesters.value = loadedSemesters
    selectedSemesterId.value = loadedSemesters.find((semester) => semester.status === 'ACTIVE')?.id ?? loadedSemesters[0]?.id ?? null
    unassignedStudents.value = loadedUnassigned
    const firstClass = loadedClasses.find((schoolClass) => schoolClass.status !== 'CLOSED') ?? loadedClasses[0]
    selectedGradeId.value = firstClass?.gradeLevelId ?? grades.value[0]?.id ?? null
    selectedClassId.value = loadedClasses.find((schoolClass) => schoolClass.gradeLevelId === selectedGradeId.value && schoolClass.status !== 'CLOSED')?.id
      ?? loadedClasses.find((schoolClass) => schoolClass.gradeLevelId === selectedGradeId.value)?.id
      ?? null
    unassignedState.value = 'success'
  } catch (error) {
    if (isApiError(error, 401)) return
    contextForbidden.value = isApiError(error, 403)
    unassignedForbidden.value = isApiError(error, 403)
    contextError.value = messageFor(error, 'Không thể tải context năm học và danh sách học sinh chưa xếp lớp.')
    unassignedError.value = contextError.value
    unassignedState.value = 'error'
    resetLists()
  } finally {
    classLoading.value = false
    if (loadingAcademicYearId === academicYearId) loadingAcademicYearId = null
  }
}

async function loadContext(): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  academicYearLoading.value = true
  contextError.value = ''
  contextForbidden.value = false
  try {
    const [loadedYears, loadedGrades] = await Promise.all([fetchAcademicYears(accessToken), fetchGrades(accessToken)])
    academicYears.value = loadedYears
    grades.value = loadedGrades
    selectedAcademicYearId.value = academicYears.value.find((year) => year.status === 'ACTIVE')?.id ?? academicYears.value[0]?.id ?? null
    await loadAcademicYearContext(selectedAcademicYearId.value)
  } catch (error) {
    if (isApiError(error, 401)) return
    contextForbidden.value = isApiError(error, 403)
    contextError.value = messageFor(error, 'Không thể tải danh sách năm học.')
    resetLists()
    unassignedState.value = 'error'
    rosterState.value = 'success'
  } finally {
    academicYearLoading.value = false
  }
}

async function loadRoster(classId: number | null): Promise<void> {
  const accessToken = token()
  if (!accessToken || classId === null) {
    classStudents.value = []
    rosterState.value = 'success'
    return
  }
  rosterState.value = 'loading'
  rosterError.value = ''
  rosterForbidden.value = false
  try {
    classStudents.value = await fetchClassStudents(accessToken, classId)
    rosterState.value = 'success'
  } catch (error) {
    if (isApiError(error, 401)) return
    rosterForbidden.value = isApiError(error, 403)
    rosterError.value = messageFor(error, 'Không thể tải roster của lớp.')
    rosterState.value = 'error'
  }
}

function openPlacement(student: UnassignedStudent): void {
  if (classIsReadOnly.value || !selectedClassId.value) return
  mutationMode.value = 'single'
  mutationStudents.value = [student]
  mutationError.value = ''
  mutationVisible.value = true
}

function openBulkPlacement(): void {
  if (classIsReadOnly.value || !selectedClassId.value || selectedUnassignedStudents.value.length === 0) return
  mutationMode.value = 'bulk'
  mutationStudents.value = [...selectedUnassignedStudents.value]
  mutationError.value = ''
  mutationVisible.value = true
}

function closeMutation(): void {
  mutationVisible.value = false
  mutationError.value = ''
}

async function reloadAfterMutation(): Promise<void> {
  await Promise.all([
    selectedAcademicYearId.value === null ? Promise.resolve() : loadUnassignedStudents(selectedAcademicYearId.value),
    loadRoster(selectedClassId.value),
  ])
  selectedUnassignedStudents.value = []
}

async function loadUnassignedStudents(academicYearId: number): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  unassignedState.value = 'loading'
  try {
    unassignedStudents.value = await fetchUnassignedStudents(accessToken, academicYearId)
    unassignedState.value = 'success'
  } catch (error) {
    if (isApiError(error, 401)) return
    unassignedForbidden.value = isApiError(error, 403)
    unassignedError.value = messageFor(error, 'Không thể tải lại danh sách học sinh chưa xếp lớp.')
    unassignedState.value = 'error'
  }
}

async function submitPlacement(values: CreateEnrollmentFormValues | BulkEnrollmentFormValues): Promise<void> {
  const accessToken = token()
  if (!accessToken || selectedAcademicYearId.value === null || selectedClassId.value === null) return
  saving.value = true
  mutationError.value = ''
  statusMessage.value = ''
  try {
    let result: EnrollmentMutation
    const enrolledAt = values.enrolledAt || null
    if (mutationMode.value === 'single') {
      const singleValues = values as CreateEnrollmentFormValues
      if (singleValues.studentId === null) {
        mutationError.value = 'Học sinh là bắt buộc.'
        return
      }
      result = await createEnrollment(accessToken, { studentId: singleValues.studentId, academicYearId: selectedAcademicYearId.value, classId: selectedClassId.value, enrolledAt })
    } else {
      const bulkValues = values as BulkEnrollmentFormValues
      result = await createBulkEnrollment(accessToken, { academicYearId: selectedAcademicYearId.value, classId: selectedClassId.value, studentIds: bulkValues.studentIds, enrolledAt })
    }
    warnings.value = result.warnings
    statusMessage.value = `Đã xếp ${result.enrollments.length} học sinh vào lớp ${mutationClassLabel.value}.`
    await reloadAfterMutation()
    closeMutation()
  } catch (error) {
    if (isApiError(error, 401)) return
    mutationError.value = messageFor(error, 'Không thể xếp học sinh vào lớp.')
  } finally {
    saving.value = false
  }
}

function openTransfer(student: ClassStudent): void {
  if (classIsReadOnly.value) return
  transferStudent.value = student
  transferError.value = ''
  transferVisible.value = true
}

async function openTransferScoreAssist(values: TransferEnrollmentFormValues): Promise<void> {
  const accessToken = token()
  if (!accessToken || !transferStudent.value || !values.targetClassId) return
  if (selectedSemesterId.value === null) {
    transferError.value = 'Chưa xác định được học kỳ hiện tại để hỗ trợ chuyển điểm.'
    return
  }
  transferVisible.value = false
  transferAssistValues.value = values
  transferAssistSnapshot.value = null
  transferAssistError.value = ''
  transferAssistVisible.value = true
  transferAssistLoading.value = true
  try {
    transferAssistSnapshot.value = await fetchTransferScoreAssist(accessToken, transferStudent.value.enrollmentId, values.targetClassId, selectedSemesterId.value)
  } catch (error) {
    if (isApiError(error, 401)) return
    transferAssistError.value = isApiError(error, 403)
      ? 'Bạn không có quyền xem bằng chứng điểm của học sinh này.'
      : messageFor(error, 'Không thể tải dữ liệu hỗ trợ chuyển điểm.')
  } finally {
    transferAssistLoading.value = false
  }
}

async function submitTransferWithScores(request: TransferWithScoresRequest): Promise<void> {
  const accessToken = token()
  if (!accessToken || !transferStudent.value || !transferAssistSnapshot.value) return
  const student = transferStudent.value
  const oldClassName = classDisplayName(selectedClassId.value)
  const newClassName = classDisplayName(request.targetClassId)
  transferAssistSaving.value = true
  transferAssistError.value = ''
  statusMessage.value = ''
  try {
    const hasTargetScores = request.scores.length > 0
    if (!hasTargetScores) {
      const result = await transferEnrollment(accessToken, student.enrollmentId, { targetClassId: request.targetClassId, effectiveAt: request.effectiveAt, reason: request.reason })
      warnings.value = result.warnings
    } else {
      const result = await transferWithScores(accessToken, student.enrollmentId, request)
      warnings.value = result.transfer.warnings
    }
    statusMessage.value = hasTargetScores
      ? `Đã chuyển ${student.studentCode}-${student.studentName} từ lớp ${oldClassName} sang ${newClassName} và lưu điểm lớp mới.`
      : `Đã chuyển ${student.studentCode}-${student.studentName} từ lớp ${oldClassName} sang ${newClassName}.`
    await reloadAfterMutation()
    transferAssistVisible.value = false
    transferAssistSnapshot.value = null
  } catch (error) {
    if (isApiError(error, 401)) return
    transferAssistError.value = isApiError(error, 403)
      ? 'Bạn không có quyền thực hiện chuyển lớp và lưu điểm.'
      : isApiError(error, 409)
        ? 'Dữ liệu điểm hoặc việc xếp lớp đã thay đổi. Bản nháp vẫn được giữ; hãy tải lại dữ liệu rồi kiểm tra trước khi thử lại.'
        : messageFor(error, 'Không thể chuyển lớp và lưu điểm lớp mới.')
  } finally {
    transferAssistSaving.value = false
  }
}

async function retryTransferScoreAssist(): Promise<void> {
  if (transferAssistValues.value) await openTransferScoreAssist(transferAssistValues.value)
}

function closeTransferScoreAssist(): void {
  if (transferAssistSaving.value) return
  transferAssistVisible.value = false
  transferAssistError.value = ''
}

function openHistory(student: { studentId: number; studentCode: string; studentName: string }): void {
  historyStudent.value = student
  history.value = []
  historyError.value = ''
  historyVisible.value = true
  void loadHistory(student.studentId)
}

async function loadHistory(studentId: number): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  historyLoading.value = true
  try {
    history.value = await fetchStudentEnrollmentHistory(accessToken, studentId)
  } catch (error) {
    if (isApiError(error, 401)) return
    historyError.value = messageFor(error, 'Không thể tải lịch sử enrollment của học sinh.')
  } finally {
    historyLoading.value = false
  }
}

watch(selectedAcademicYearId, (value, oldValue) => {
  if (value !== oldValue) void loadAcademicYearContext(value)
})
watch(selectedGradeId, (value, oldValue) => {
  if (value !== oldValue) {
    selectedClassId.value = filteredClasses.value.find((schoolClass) => schoolClass.status !== 'CLOSED')?.id ?? filteredClasses.value[0]?.id ?? null
  }
})
watch(selectedClassId, (value, oldValue) => {
  if (value !== oldValue) {
    selectedUnassignedStudents.value = []
    void loadRoster(value)
  }
})
onMounted(() => { void loadContext() })
</script>

<template>
  <div class="page-heading enrollment-page-heading">
    <div>
      <h1>Xếp lớp</h1>
    </div>
    <div class="page-heading-actions">
      <Button label="Làm mới context" icon="pi pi-refresh" severity="secondary" outlined :loading="academicYearLoading || classLoading" @click="loadContext" />
      <Button label="Xếp lớp tự động" icon="pi pi-bolt" @click="router.push({ name: 'v2-placement-new' })" />
    </div>
  </div>
  <FormAlert v-if="statusMessage" tone="success" :message="statusMessage" />
  <FormAlert v-if="contextError && !contextForbidden" tone="error" :message="contextError" />
  <EnrollmentContextPanel
    v-model:academic-year-id="selectedAcademicYearId"
    v-model:class-id="selectedClassId"
    :academic-years="academicYears"
    :grades="grades"
    :classes="filteredClasses"
    :loading="academicYearLoading"
    :class-loading="classLoading"
    :grade-id="selectedGradeId"
    @update:grade-id="selectedGradeId = $event"
  />
  <CapacityWarningBanner v-if="warnings.length > 0" :available="true" :warnings="warnings" :classes="classes" :warning-count="warnings.length" />
  <section class="content-surface">
    <div class="section-heading">
      <div><h2>Học sinh chưa xếp lớp</h2><p class="section-caption">Chọn một hoặc nhiều dòng để xếp vào lớp {{ mutationClassLabel || 'đang chọn' }}.</p></div>
      <Button label="Xếp học sinh đã chọn" icon="pi pi-check" :disabled="classIsReadOnly || selectedUnassignedStudents.length === 0" @click="openBulkPlacement" />
    </div>
    <PageState :state="unassignedState" :forbidden="unassignedForbidden" forbidden-message="Bạn không có quyền xem danh sách học sinh chưa xếp lớp." :error-message="unassignedError" empty-heading="Không có học sinh chưa xếp lớp" empty-message="Năm học hiện tại chưa có học sinh cần xếp lớp." @retry="() => selectedAcademicYearId && loadUnassignedStudents(selectedAcademicYearId)">
      <UnassignedStudentTable :students="unassignedStudents" :selected-students="selectedUnassignedStudents" :loading="unassignedState === 'loading'" :read-only="classIsReadOnly" @update:selected-students="selectedUnassignedStudents = $event" @place="openPlacement" @history="openHistory" />
    </PageState>
  </section>
  <section class="content-surface">
    <PageState :state="rosterState" :forbidden="rosterForbidden" forbidden-message="Bạn không có quyền xem roster của lớp." :error-message="rosterError" empty-heading="Lớp chưa có học sinh" empty-message="Lớp hiện tại chưa có học sinh nào." @retry="() => loadRoster(selectedClassId)">
      <ClassStudentTable :students="classStudents" :loading="rosterState === 'loading'" :read-only="classIsReadOnly" @transfer="openTransfer" @history="openHistory" />
    </PageState>
  </section>
  <EnrollmentMutationDialog v-model:visible="mutationVisible" :mode="mutationMode" :students="mutationStudents" :class-label="mutationClassLabel" :saving="saving" :error-message="mutationError" @submit="submitPlacement" @cancel="closeMutation" />
  <TransferEnrollmentDialog v-model:visible="transferVisible" :student="transferStudent" :current-class-id="selectedClassId" :target-classes="targetClasses" :saving="transferSaving" :error-message="transferError" @continue-score-assist="openTransferScoreAssist" />
  <TransferScoreAssistDialog v-model:visible="transferAssistVisible" :snapshot="transferAssistSnapshot" :loading="transferAssistLoading" :saving="transferAssistSaving" :error-message="transferAssistError" :effective-at="transferAssistValues?.effectiveAt ?? ''" :reason="transferAssistValues?.reason ?? ''" @confirm="submitTransferWithScores" @retry="retryTransferScoreAssist" @cancel="closeTransferScoreAssist" />
  <StudentEnrollmentHistoryDialog v-model:visible="historyVisible" :student-code="historyStudent?.studentCode" :student-name="historyStudent?.studentName" :history="history" :loading="historyLoading" :error-message="historyError" :classes="classes" />
</template>
