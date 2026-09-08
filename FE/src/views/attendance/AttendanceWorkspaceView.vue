<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import Button from 'primevue/button'
import ConfirmDialog from 'primevue/confirmdialog'

import AttendanceContextPanel from '@/components/attendance/AttendanceContextPanel.vue'
import AttendanceExceptionDialog from '@/components/attendance/AttendanceExceptionDialog.vue'
import AttendanceHistoryPanel from '@/components/attendance/AttendanceHistoryPanel.vue'
import AttendanceSessionTable from '@/components/attendance/AttendanceSessionTable.vue'
import ClassAttendanceSummaryPanel from '@/components/attendance/ClassAttendanceSummaryPanel.vue'
import FormAlert from '@/components/common/FormAlert.vue'
import { useAttendanceReports } from '@/composables/useAttendanceReports'
import { useAttendanceSession } from '@/composables/useAttendanceSession'
import { useAuthSession } from '@/composables/useAuthSession'
import { fetchAcademicYears, fetchSchoolClasses, fetchSemesters } from '@/services/academicApi'
import { fetchAttendanceCalendar } from '@/services/attendanceApi'
import { isApiError } from '@/types/api'
import type { AcademicYear, SchoolClass, Semester } from '@/types/academic'
import type { AttendanceApiScope, AttendanceCalendarDay, AttendanceSessionPeriod } from '@/types/attendance'

type AttendanceTab = 'session' | 'history' | 'summary'

function messageFor(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

const { roles: userRoles, requireAccessToken: token } = useAuthSession()
const tab = ref<AttendanceTab>('session')
const academicYears = ref<AcademicYear[]>([])
const semesters = ref<Semester[]>([])
const classes = ref<SchoolClass[]>([])
const selectedAcademicYearId = ref<number | null>(null)
const selectedSemesterId = ref<number | null>(null)
const selectedClassId = ref<number | null>(null)
const attendanceDate = ref('')
const sessionPeriod = ref<AttendanceSessionPeriod>('MORNING')
const contextLoading = ref(true)
const contextError = ref('')
const contextForbidden = ref(false)
const initialized = ref(false)
const calendarStatus = ref<'SCHEDULED' | 'NO_CLASS' | 'UNKNOWN'>('UNKNOWN')
const calendarMessage = ref('Chọn đầy đủ thông tin để kiểm tra ngày học hợp lệ.')
const calendarLoading = ref(false)
const calendarError = ref('')
let calendarRequestKey = ''

const attendanceScope = computed<AttendanceApiScope>(() => userRoles.value.includes('ADMIN') || userRoles.value.includes('ACADEMIC_OFFICE') ? 'office' : 'teacher')
const isStudent = computed(() => userRoles.value.includes('STUDENT'))
const canOpenSession = computed(() => userRoles.value.includes('ADMIN') || userRoles.value.includes('ACADEMIC_OFFICE'))
const showSessionTab = computed(() => !isStudent.value)
const showHistoryTab = computed(() => isStudent.value)
const showSummaryTab = computed(() => !isStudent.value)
const selectedClass = computed(() => classes.value.find((item) => item.id === selectedClassId.value) ?? null)
const selectedSemester = computed(() => semesters.value.find((item) => item.id === selectedSemesterId.value) ?? null)
const sessionReadOnly = computed(() => selectedClass.value?.status === 'CLOSED' || selectedSemester.value?.status === 'CLOSED' || selectedSemester.value?.status === 'LOCKED')

function contextKey(): string {
  return `${selectedAcademicYearId.value ?? ''}:${selectedSemesterId.value ?? ''}:${selectedClassId.value ?? ''}:${attendanceDate.value}:${sessionPeriod.value}`
}

const {
  historyAcademicYearId, historySemesterId, historySemesters, historyFrom, historyTo, historyPage, historyPageSize, historyResponse, historyLoading, historyError, historyForbidden,
  summaryClassId, summarySemesterId, summaryFrom, summaryTo, summaryPage, summaryPageSize, summaryResponse, summaryLoading, summaryError, summaryForbidden,
  reset: resetReports, applyYearContext, loadHistory, searchHistory, changeHistoryYear, changeHistoryPage, loadSummary, changeSummaryPage, initializeStudentContext,
} = useAttendanceReports({ accessToken: token, isStudent, classes, semesters })

const {
  session, students: sessionStudents, loading: sessionLoading, error: sessionError, forbidden: sessionForbidden, opening: sessionOpening, saving: sessionSaving,
  exceptionVisible, selectedStudent, mutationError, statusMessage, state: sessionState, reset: resetSession, load: loadSession, open: openSession,
  openException, closeException, saveException, confirmDelete,
} = useAttendanceSession({
  accessToken: token, scope: attendanceScope, isStudent, canOpenSession, classId: selectedClassId, semesterId: selectedSemesterId,
  attendanceDate, sessionPeriod, readOnly: sessionReadOnly, contextKey,
})

watch(isStudent, (value) => {
  tab.value = value ? 'history' : 'session'
}, { immediate: true })

async function loadCalendar(): Promise<void> {
  const accessToken = token()
  if (!accessToken || selectedAcademicYearId.value === null || selectedSemesterId.value === null || !attendanceDate.value) {
    calendarStatus.value = 'UNKNOWN'
    calendarMessage.value = 'Chọn đầy đủ thông tin để kiểm tra ngày học hợp lệ.'
    return
  }
  const key = contextKey()
  calendarRequestKey = key
  calendarLoading.value = true
  calendarError.value = ''
  try {
    const days = await fetchAttendanceCalendar(accessToken, {
      academicYearId: selectedAcademicYearId.value, semesterId: selectedSemesterId.value, from: attendanceDate.value, to: attendanceDate.value,
    })
    if (calendarRequestKey !== key) return
    const day = days.find((item: AttendanceCalendarDay) => item.calendarDate === attendanceDate.value)
    const calendarSession = day?.sessions.find((item) => item.sessionPeriod === sessionPeriod.value)
    if (day?.dayType === 'NO_CLASS' || calendarSession?.sessionStatus === 'NO_CLASS') {
      calendarStatus.value = 'NO_CLASS'
      calendarMessage.value = day?.reason || calendarSession?.reason || 'Ngày/buổi này không có lịch học.'
    } else if (calendarSession?.sessionStatus === 'SCHEDULED') {
      calendarStatus.value = 'SCHEDULED'
      calendarMessage.value = 'Ngày và buổi học hợp lệ để mở buổi điểm danh.'
    } else {
      calendarStatus.value = 'UNKNOWN'
      calendarMessage.value = 'Chưa có cấu hình lịch cho ngày/buổi này'
    }
  } catch (error) {
    if (isApiError(error, 401)) return
    calendarError.value = messageFor(error, 'Không thể kiểm tra lịch học.')
    calendarStatus.value = 'UNKNOWN'
    calendarMessage.value = 'Không thể preflight lịch học; hãy thử lại hoặc để backend xác nhận.'
  } finally {
    if (calendarRequestKey === key) calendarLoading.value = false
  }
}

async function loadYearContext(academicYearId: number | null): Promise<void> {
  const accessToken = token()
  resetSession()
  resetReports()
  semesters.value = []
  classes.value = []
  selectedSemesterId.value = null
  selectedClassId.value = null
  if (!accessToken || academicYearId === null) {
    contextLoading.value = false
    return
  }
  contextLoading.value = true
  contextError.value = ''
  contextForbidden.value = false
  try {
    const [classList, semesterList] = await Promise.all([fetchSchoolClasses(accessToken, academicYearId), fetchSemesters(accessToken, academicYearId)])
    if (selectedAcademicYearId.value !== academicYearId) return
    classes.value = classList
    semesters.value = semesterList
    const firstClass = classList.find((item) => item.status !== 'CLOSED') ?? classList[0]
    const activeSemester = semesterList.find((item) => item.status === 'ACTIVE') ?? semesterList[0]
    selectedClassId.value = firstClass?.id ?? null
    selectedSemesterId.value = activeSemester?.id ?? null
    if (!attendanceDate.value || !semesterList.some((item) => item.startDate <= attendanceDate.value && item.endDate >= attendanceDate.value)) attendanceDate.value = activeSemester?.startDate ?? ''
    applyYearContext(academicYearId, semesterList, firstClass, activeSemester)
    calendarStatus.value = 'UNKNOWN'
    calendarMessage.value = 'Đang kiểm tra lịch học...'
    await loadCalendar()
  } catch (error) {
    if (isApiError(error, 401)) return
    contextForbidden.value = isApiError(error, 403)
    contextError.value = messageFor(error, 'Không thể tải lớp và học kỳ theo năm học.')
  } finally {
    contextLoading.value = false
  }
}

async function loadContext(): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  contextLoading.value = true
  contextError.value = ''
  contextForbidden.value = false
  try {
    const years = await fetchAcademicYears(accessToken)
    academicYears.value = years
    selectedAcademicYearId.value = years.find((item) => item.status === 'ACTIVE')?.id ?? years[0]?.id ?? null
    await loadYearContext(selectedAcademicYearId.value)
  } catch (error) {
    if (isApiError(error, 401)) return
    contextForbidden.value = isApiError(error, 403)
    contextError.value = messageFor(error, 'Không thể tải danh sách năm học.')
  } finally {
    contextLoading.value = false
  }
}

async function loadStudentHistoryContext(): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  contextLoading.value = true
  contextError.value = ''
  contextForbidden.value = false
  try {
    academicYears.value = await fetchAcademicYears(accessToken)
    await initializeStudentContext(academicYears)
  } catch (error) {
    if (isApiError(error, 401)) return
    contextForbidden.value = isApiError(error, 403)
    contextError.value = messageFor(error, 'Không thể tải danh sách năm học.')
  } finally {
    contextLoading.value = false
  }
}

function changeTab(value: AttendanceTab): void {
  tab.value = value
  statusMessage.value = ''
  mutationError.value = ''
}

watch(selectedAcademicYearId, (value, previous) => {
  if (initialized.value && value !== previous) void loadYearContext(value)
})
watch([selectedSemesterId, selectedClassId, attendanceDate, sessionPeriod], () => {
  if (initialized.value) {
    void loadCalendar()
    void loadSession()
  }
})
watch([summaryClassId, summarySemesterId, summaryFrom, summaryTo], () => {
  if (initialized.value && showSummaryTab.value) void loadSummary()
})
watch([historyAcademicYearId, historySemesterId], () => {
  if (initialized.value && isStudent.value && historyAcademicYearId.value !== null && historySemesterId.value !== null) searchHistory()
})
onMounted(async () => {
  if (isStudent.value) await loadStudentHistoryContext()
  else await loadContext()
  initialized.value = true
  if (isStudent.value) void loadHistory()
  else {
    void loadSession()
    if (showSummaryTab.value) void loadSummary()
  }
})
</script>

<template>
  <div class="page-heading attendance-page-heading"><div><h1>Điểm danh</h1></div><div class="page-heading-actions"><Button label="Làm mới context" icon="pi pi-refresh" severity="secondary" outlined :loading="contextLoading" @click="isStudent ? loadStudentHistoryContext() : loadContext()" /></div></div>
  <FormAlert v-if="statusMessage" tone="success" :message="statusMessage" />
  <FormAlert v-if="mutationError" tone="error" :message="mutationError" />
  <FormAlert v-if="contextError && !contextForbidden" tone="error" :message="contextError" />
  <FormAlert v-if="contextForbidden" tone="warning" message="Bạn không có quyền tải context điểm danh." />
  <div class="tab-strip attendance-tab-strip">
    <Button v-if="showSessionTab" label="Điểm danh theo buổi" icon="pi pi-calendar" :severity="tab === 'session' ? 'primary' : 'secondary'" :outlined="tab !== 'session'" @click="changeTab('session')" />
    <Button v-if="showHistoryTab" label="Lịch sử của học sinh" icon="pi pi-history" :severity="tab === 'history' ? 'primary' : 'secondary'" :outlined="tab !== 'history'" @click="changeTab('history')" />
    <Button v-if="showSummaryTab" label="Báo cáo lớp" icon="pi pi-chart-bar" :severity="tab === 'summary' ? 'primary' : 'secondary'" :outlined="tab !== 'summary'" @click="changeTab('summary')" />
  </div>
  <template v-if="tab === 'session' && showSessionTab">
    <AttendanceContextPanel v-model:academic-year-id="selectedAcademicYearId" v-model:semester-id="selectedSemesterId" v-model:class-id="selectedClassId" v-model:attendance-date="attendanceDate" v-model:session-period="sessionPeriod" :academic-years="academicYears" :semesters="semesters" :classes="classes" :loading="contextLoading" :calendar-loading="calendarLoading" :calendar-status="calendarStatus" :calendar-message="calendarMessage" :show-open="canOpenSession" :open-loading="sessionOpening" @open="openSession" />
    <FormAlert v-if="calendarError" tone="warning" :message="calendarError" />
    <FormAlert v-if="sessionError && !sessionForbidden" tone="error" :message="sessionError" />
    <FormAlert v-if="sessionForbidden" tone="warning" message="Bạn không có quyền thao tác buổi điểm danh này." />
    <section class="content-surface attendance-session-surface"><div class="section-heading"><div><h2>Danh sách học sinh của buổi</h2></div><span v-if="session" class="field-hint">{{ sessionStudents.length }} học sinh</span></div><div v-if="sessionState === 'loading'" class="page-state page-state-loading" role="status"><i class="pi pi-spin pi-spinner" aria-hidden="true" /><span>Đang tải danh sách điểm danh...</span></div><AttendanceSessionTable v-else :students="sessionStudents" :loading="sessionLoading" :read-only="sessionReadOnly" @exception="openException" @delete="confirmDelete" /><p v-if="!session" class="section-caption attendance-session-hint">Chọn đầy đủ thông tin để tải danh sách học sinh của buổi điểm danh.</p></section>
  </template>
  <AttendanceHistoryPanel v-else-if="tab === 'history' && showHistoryTab" v-model:academic-year-id="historyAcademicYearId" v-model:semester-id="historySemesterId" v-model:from="historyFrom" v-model:to="historyTo" :academic-years="academicYears" :semesters="historySemesters" :response="historyResponse" :loading="historyLoading" :error-message="historyError" :forbidden="historyForbidden" :page="historyPage" :page-size="historyPageSize" @update:academic-year-id="changeHistoryYear" @search="searchHistory" @page-change="changeHistoryPage" />
  <ClassAttendanceSummaryPanel v-else-if="showSummaryTab" v-model:class-id="summaryClassId" v-model:semester-id="summarySemesterId" v-model:from="summaryFrom" v-model:to="summaryTo" :classes="classes" :semesters="semesters" :response="summaryResponse" :loading="summaryLoading" :error-message="summaryError" :forbidden="summaryForbidden" :page="summaryPage" :page-size="summaryPageSize" @page-change="changeSummaryPage" />
  <AttendanceExceptionDialog v-model:visible="exceptionVisible" :student="selectedStudent" :session="session" :saving="sessionSaving" :error-message="mutationError" @save="saveException" @cancel="closeException" />
  <ConfirmDialog />
</template>
