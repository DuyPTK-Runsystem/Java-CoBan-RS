<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import Button from 'primevue/button'

import CapacityWarningBanner from '@/components/CapacityWarningBanner.vue'
import ClassStudentTable from '@/components/ClassStudentTable.vue'
import EnrollmentContextPanel from '@/components/EnrollmentContextPanel.vue'
import EnrollmentMutationDialog from '@/components/EnrollmentMutationDialog.vue'
import FormAlert from '@/components/FormAlert.vue'
import PageState from '@/components/PageState.vue'
import StudentEnrollmentHistoryDialog from '@/components/StudentEnrollmentHistoryDialog.vue'
import TransferEnrollmentDialog from '@/components/TransferEnrollmentDialog.vue'
import UnassignedStudentTable from '@/components/UnassignedStudentTable.vue'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import { fetchAcademicYears, fetchGrades, fetchSchoolClasses } from '@/services/academicApi'
import { createBulkEnrollment, createEnrollment, fetchClassStudents, fetchStudentEnrollmentHistory, fetchUnassignedStudents, transferEnrollment } from '@/services/enrollmentApi'
import { isApiError } from '@/types/api'
import type { AcademicYear, GradeLevel, SchoolClass } from '@/types/academic'
import type { BulkEnrollmentFormValues, CapacityWarning, ClassStudent, CreateEnrollmentFormValues, EnrollmentMutation, StudentEnrollmentHistory, TransferEnrollmentFormValues, UnassignedStudent } from '@/types/enrollment'
import type { LoadingState } from '@/types/ui'
import { useRouter } from 'vue-router'

const router = useRouter()
const academicYears = ref<AcademicYear[]>([])
const grades = ref<GradeLevel[]>([])
const classes = ref<SchoolClass[]>([])
const unassignedStudents = ref<UnassignedStudent[]>([])
const classStudents = ref<ClassStudent[]>([])
const selectedAcademicYearId = ref<number | null>(null)
const selectedGradeId = ref<number | null>(null)
const selectedClassId = ref<number | null>(null)
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

function classDisplayName(classId: number | null): string {
  if (classId === null) return 'hiện tại'
  const schoolClass = classes.value.find((item) => item.id === classId)
  return schoolClass?.className?.trim() || schoolClass?.classCode || `Lớp #${classId}`
}

function resetLists(): void {
  classes.value = []
  classStudents.value = []
  unassignedStudents.value = []
  selectedGradeId.value = null
  selectedClassId.value = null
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
    const [loadedClasses, loadedUnassigned] = await Promise.all([
      fetchSchoolClasses(accessToken, academicYearId),
      fetchUnassignedStudents(accessToken, academicYearId),
    ])
    if (selectedAcademicYearId.value !== academicYearId) return
    classes.value = loadedClasses
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

async function submitTransfer(values: TransferEnrollmentFormValues): Promise<void> {
  const accessToken = token()
  if (!accessToken || !transferStudent.value) return
  if (!values.targetClassId) return
  const student = transferStudent.value
  const oldClassName = classDisplayName(selectedClassId.value)
  const newClassName = classDisplayName(values.targetClassId)
  transferSaving.value = true
  transferError.value = ''
  statusMessage.value = ''
  try {
    const result = await transferEnrollment(accessToken, student.enrollmentId, { targetClassId: values.targetClassId, effectiveAt: values.effectiveAt, reason: values.reason || null })
    warnings.value = result.warnings
    statusMessage.value = `Đã chuyển ${student.studentCode}-${student.studentName} từ lớp ${oldClassName} sang ${newClassName}.`
    await reloadAfterMutation()
    transferVisible.value = false
  } catch (error) {
    if (isApiError(error, 401)) return
    transferError.value = messageFor(error, 'Không thể chuyển lớp cho học sinh.')
  } finally {
    transferSaving.value = false
  }
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
      <p class="eyebrow">Enrollment workspace</p>
      <h1>Xếp lớp</h1>
      <p>Xếp học sinh vào lớp theo năm học, xem học sinh chưa xếp lớp</p>
    </div>
    <div class="page-heading-actions">
      <Button label="Làm mới context" icon="pi pi-refresh" severity="secondary" outlined :loading="academicYearLoading || classLoading" @click="loadContext" />
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
  <TransferEnrollmentDialog v-model:visible="transferVisible" :student="transferStudent" :current-class-id="selectedClassId" :target-classes="targetClasses" :saving="transferSaving" :error-message="transferError" @submit="submitTransfer" />
  <StudentEnrollmentHistoryDialog v-model:visible="historyVisible" :student-code="historyStudent?.studentCode" :student-name="historyStudent?.studentName" :history="history" :loading="historyLoading" :error-message="historyError" :classes="classes" />
</template>
