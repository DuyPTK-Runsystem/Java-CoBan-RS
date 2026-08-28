<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import Button from 'primevue/button'
import ConfirmDialog from 'primevue/confirmdialog'
import { useConfirm } from 'primevue/useconfirm'

import AttendanceContextPanel from '@/components/AttendanceContextPanel.vue'
import AttendanceExceptionDialog from '@/components/AttendanceExceptionDialog.vue'
import AttendanceHistoryPanel from '@/components/AttendanceHistoryPanel.vue'
import AttendanceSessionTable from '@/components/AttendanceSessionTable.vue'
import ClassAttendanceSummaryPanel from '@/components/ClassAttendanceSummaryPanel.vue'
import FormAlert from '@/components/FormAlert.vue'
import { fetchAcademicYears, fetchSchoolClasses, fetchSemesters } from '@/services/academicApi'
import {
  createOrGetAttendanceSession,
  deleteAttendanceException,
  fetchAttendanceCalendar,
  fetchAttendanceSessionStudents,
  fetchClassAttendanceSummary,
  fetchStudentAttendanceHistory,
  upsertAttendanceException,
} from '@/services/attendanceApi'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
import { isApiError } from '@/types/api'
import type { AcademicYear, SchoolClass, Semester } from '@/types/academic'
import type {
  AttendanceCalendarDay,
  AttendanceSession,
  AttendanceSessionPeriod,
  AttendanceStudent,
  ClassAttendanceSummaryResponse,
  StudentAttendanceHistoryResponse,
  UpsertAttendanceExceptionRequest,
} from '@/types/attendance'
import type { LoadingState } from '@/types/ui'
import { useRouter } from 'vue-router'

type AttendanceTab = 'session' | 'history' | 'summary'

const router = useRouter()
const confirm = useConfirm()
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
const calendarMessage = ref('Chọn đủ context để kiểm tra ngày học hợp lệ.')
const calendarLoading = ref(false)
const calendarError = ref('')
let calendarRequestKey = ''

const session = ref<AttendanceSession | null>(null)
const sessionStudents = ref<AttendanceStudent[]>([])
const sessionLoading = ref(false)
const sessionError = ref('')
const sessionForbidden = ref(false)
const sessionSaving = ref(false)
const exceptionVisible = ref(false)
const selectedStudent = ref<AttendanceStudent | null>(null)
const mutationError = ref('')
const statusMessage = ref('')

const historyAcademicYearId = ref<number | null>(null)
const historySemesterId = ref<number | null>(null)
const historySemesters = ref<Semester[]>([])
const historyFrom = ref('')
const historyTo = ref('')
const historyPage = ref(0)
const historyPageSize = ref(10)
const historyResponse = ref<StudentAttendanceHistoryResponse | null>(null)
const historyLoading = ref(false)
const historyError = ref('')
const historyForbidden = ref(false)

const summaryClassId = ref<number | null>(null)
const summarySemesterId = ref<number | null>(null)
const summaryFrom = ref('')
const summaryTo = ref('')
const summaryPage = ref(0)
const summaryPageSize = ref(20)
const summaryResponse = ref<ClassAttendanceSummaryResponse | null>(null)
const summaryLoading = ref(false)
const summaryError = ref('')
const summaryForbidden = ref(false)

const selectedClass = computed(() => classes.value.find((item) => item.id === selectedClassId.value) ?? null)
const selectedSemester = computed(() => semesters.value.find((item) => item.id === selectedSemesterId.value) ?? null)
const sessionReadOnly = computed(() => selectedClass.value?.status === 'CLOSED' || selectedSemester.value?.status === 'CLOSED' || selectedSemester.value?.status === 'LOCKED')
const sessionState = computed<LoadingState>(() => {
  if (sessionLoading.value) return 'loading'
  if (sessionError.value) return 'error'
  if (session.value && sessionStudents.value.length === 0) return 'empty'
  return 'success'
})

function token(): string | null {
  const auth = getAuthSession()
  if (auth) return auth.accessToken
  clearAuthSession()
  void router.replace({ name: 'login' })
  return null
}

function messageFor(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

function resetSession(): void {
  session.value = null
  sessionStudents.value = []
  sessionError.value = ''
  sessionForbidden.value = false
  selectedStudent.value = null
  exceptionVisible.value = false
}

function resetReports(): void {
  historyResponse.value = null
  summaryResponse.value = null
  historyError.value = ''
  summaryError.value = ''
  historyForbidden.value = false
  summaryForbidden.value = false
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
    const [classList, semesterList] = await Promise.all([
      fetchSchoolClasses(accessToken, academicYearId),
      fetchSemesters(accessToken, academicYearId),
    ])
    if (selectedAcademicYearId.value !== academicYearId) return
    classes.value = classList
    semesters.value = semesterList
    const firstClass = classList.find((item) => item.status !== 'CLOSED') ?? classList[0]
    const activeSemester = semesterList.find((item) => item.status === 'ACTIVE') ?? semesterList[0]
    selectedClassId.value = firstClass?.id ?? null
    selectedSemesterId.value = activeSemester?.id ?? null
    if (!attendanceDate.value || !semesterList.some((item) => item.startDate <= attendanceDate.value && item.endDate >= attendanceDate.value)) {
      attendanceDate.value = activeSemester?.startDate ?? ''
    }
    historyAcademicYearId.value = academicYearId
    historySemesters.value = semesterList
    historySemesterId.value = activeSemester?.id ?? null
    summaryClassId.value = firstClass?.id ?? null
    summarySemesterId.value = activeSemester?.id ?? null
    summaryFrom.value = activeSemester?.startDate ?? ''
    summaryTo.value = activeSemester?.endDate ?? ''
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

function contextKey(): string {
  return `${selectedAcademicYearId.value ?? ''}:${selectedSemesterId.value ?? ''}:${selectedClassId.value ?? ''}:${attendanceDate.value}:${sessionPeriod.value}`
}

async function loadCalendar(): Promise<void> {
  const accessToken = token()
  if (!accessToken || selectedAcademicYearId.value === null || selectedSemesterId.value === null || !attendanceDate.value) {
    calendarStatus.value = 'UNKNOWN'
    calendarMessage.value = 'Chọn đủ context để kiểm tra ngày học hợp lệ.'
    return
  }
  const key = contextKey()
  calendarRequestKey = key
  calendarLoading.value = true
  calendarError.value = ''
  try {
    const days = await fetchAttendanceCalendar(accessToken, {
      academicYearId: selectedAcademicYearId.value,
      semesterId: selectedSemesterId.value,
      from: attendanceDate.value,
      to: attendanceDate.value,
    })
    if (calendarRequestKey !== key) return
    const day = days.find((item: AttendanceCalendarDay) => item.calendarDate === attendanceDate.value)
    const calendarSession = day?.sessions.find((item) => item.sessionPeriod === sessionPeriod.value)
    if (day?.dayType === 'NO_CLASS' || calendarSession?.sessionStatus === 'NO_CLASS') {
      calendarStatus.value = 'NO_CLASS'
      calendarMessage.value = day?.reason || calendarSession?.reason || 'Ngày/buổi này không có lịch học.'
    } else if (calendarSession?.sessionStatus === 'SCHEDULED') {
      calendarStatus.value = 'SCHEDULED'
      calendarMessage.value = 'Ngày và buổi học hợp lệ để mở attendance session.'
    } else {
      calendarStatus.value = 'UNKNOWN'
      calendarMessage.value = 'Chưa có cấu hình lịch cho ngày/buổi này; backend sẽ kiểm tra lần cuối.'
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

async function loadSessionStudents(sessionId: number): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  sessionLoading.value = true
  sessionError.value = ''
  sessionForbidden.value = false
  try {
    sessionStudents.value = await fetchAttendanceSessionStudents(accessToken, sessionId, 'teacher')
  } catch (error) {
    if (isApiError(error, 401)) return
    sessionForbidden.value = isApiError(error, 403)
    sessionError.value = messageFor(error, 'Không thể tải danh sách học sinh của buổi điểm danh.')
  } finally {
    sessionLoading.value = false
  }
}

async function openSession(): Promise<void> {
  if (selectedClassId.value === null || selectedSemesterId.value === null || !attendanceDate.value || sessionReadOnly.value) return
  if (calendarStatus.value === 'NO_CLASS') {
    sessionError.value = 'Không thể mở session cho ngày/buổi không có lịch học.'
    return
  }
  const accessToken = token()
  if (!accessToken) return
  sessionSaving.value = true
  sessionError.value = ''
  mutationError.value = ''
  statusMessage.value = ''
  try {
    const loadedSession = await createOrGetAttendanceSession(accessToken, {
      classId: selectedClassId.value,
      semesterId: selectedSemesterId.value,
      attendanceDate: attendanceDate.value,
      sessionPeriod: sessionPeriod.value,
    }, 'teacher')
    session.value = loadedSession
    await loadSessionStudents(loadedSession.sessionId)
    statusMessage.value = `Đã mở buổi điểm danh ${loadedSession.attendanceDate} · ${loadedSession.sessionPeriod === 'MORNING' ? 'sáng' : 'chiều'}.`
  } catch (error) {
    if (isApiError(error, 401)) return
    sessionForbidden.value = isApiError(error, 403)
    sessionError.value = messageFor(error, 'Không thể mở buổi điểm danh.')
  } finally {
    sessionSaving.value = false
  }
}

function openException(student: AttendanceStudent): void {
  if (sessionReadOnly.value) return
  selectedStudent.value = student
  mutationError.value = ''
  exceptionVisible.value = true
}

function closeException(): void {
  exceptionVisible.value = false
  selectedStudent.value = null
  mutationError.value = ''
}

async function saveException(request: UpsertAttendanceExceptionRequest): Promise<void> {
  const accessToken = token()
  if (!accessToken || !session.value || !selectedStudent.value || sessionReadOnly.value) return
  sessionSaving.value = true
  mutationError.value = ''
  try {
    await upsertAttendanceException(accessToken, session.value.sessionId, selectedStudent.value.studentId, request, 'teacher')
    await loadSessionStudents(session.value.sessionId)
    statusMessage.value = `Đã cập nhật ngoại lệ cho ${selectedStudent.value.studentCode}.`
    closeException()
  } catch (error) {
    if (isApiError(error, 401)) return
    mutationError.value = messageFor(error, 'Không thể lưu ngoại lệ điểm danh.')
  } finally {
    sessionSaving.value = false
  }
}

function confirmDelete(student: AttendanceStudent): void {
  if (!session.value || sessionReadOnly.value) return
  confirm.require({
    header: 'Xóa ngoại lệ điểm danh',
    message: `Xóa exception của ${student.studentCode} và trả về trạng thái có mặt mặc định?`,
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Xóa ngoại lệ',
    rejectLabel: 'Hủy',
    accept: () => { void removeException(student) },
  })
}

async function removeException(student: AttendanceStudent): Promise<void> {
  const accessToken = token()
  if (!accessToken || !session.value) return
  sessionSaving.value = true
  mutationError.value = ''
  try {
    await deleteAttendanceException(accessToken, session.value.sessionId, student.studentId, 'teacher')
    await loadSessionStudents(session.value.sessionId)
    statusMessage.value = `Đã xóa ngoại lệ của ${student.studentCode}; trạng thái trở về có mặt.`
  } catch (error) {
    if (isApiError(error, 401)) return
    mutationError.value = messageFor(error, 'Không thể xóa ngoại lệ điểm danh.')
  } finally {
    sessionSaving.value = false
  }
}

function validateDateRange(from: string, to: string): string {
  return from && to && from > to ? 'Từ ngày phải nhỏ hơn hoặc bằng đến ngày.' : ''
}

async function loadHistory(): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  const rangeError = validateDateRange(historyFrom.value, historyTo.value)
  if (rangeError) {
    historyError.value = rangeError
    historyResponse.value = null
    return
  }
  historyLoading.value = true
  historyError.value = ''
  historyForbidden.value = false
  try {
    historyResponse.value = await fetchStudentAttendanceHistory(accessToken, {
      academicYearId: historyAcademicYearId.value,
      semesterId: historySemesterId.value,
      from: historyFrom.value || undefined,
      to: historyTo.value || undefined,
      page: historyPage.value,
      size: historyPageSize.value,
    })
  } catch (error) {
    if (isApiError(error, 401)) return
    historyForbidden.value = isApiError(error, 403)
    historyError.value = messageFor(error, 'Không thể tải lịch sử chuyên cần.')
  } finally {
    historyLoading.value = false
  }
}

function searchHistory(): void {
  historyPage.value = 0
  void loadHistory()
}

function changeHistoryYear(value: number | null): void {
  historyAcademicYearId.value = value
  historySemesterId.value = null
  historyResponse.value = null
  if (value === null) {
    historySemesters.value = []
    return
  }
  const accessToken = token()
  if (!accessToken) return
  void fetchSemesters(accessToken, value).then((items) => {
    if (historyAcademicYearId.value !== value) return
    historySemesters.value = items
    historySemesterId.value = items.find((item) => item.status === 'ACTIVE')?.id ?? items[0]?.id ?? null
  }).catch((error: unknown) => {
    if (isApiError(error, 401)) return
    historyError.value = messageFor(error, 'Không thể tải học kỳ cho năm học đã chọn.')
  })
}

function changeHistoryPage(page: number, pageSize: number): void {
  historyPage.value = page
  historyPageSize.value = pageSize
  void loadHistory()
}

async function loadSummary(): Promise<void> {
  const accessToken = token()
  if (!accessToken || summaryClassId.value === null || summarySemesterId.value === null) return
  const rangeError = validateDateRange(summaryFrom.value, summaryTo.value)
  if (rangeError) {
    summaryError.value = rangeError
    summaryResponse.value = null
    return
  }
  if (!summaryFrom.value || !summaryTo.value) {
    summaryError.value = 'Từ ngày và đến ngày là bắt buộc.'
    summaryResponse.value = null
    return
  }
  summaryLoading.value = true
  summaryError.value = ''
  summaryForbidden.value = false
  try {
    summaryResponse.value = await fetchClassAttendanceSummary(accessToken, summaryClassId.value, {
      semesterId: summarySemesterId.value,
      from: summaryFrom.value,
      to: summaryTo.value,
      page: summaryPage.value,
      size: summaryPageSize.value,
    })
  } catch (error) {
    if (isApiError(error, 401)) return
    summaryForbidden.value = isApiError(error, 403)
    summaryError.value = messageFor(error, 'Không thể tải báo cáo chuyên cần của lớp.')
  } finally {
    summaryLoading.value = false
  }
}

function searchSummary(): void {
  summaryPage.value = 0
  void loadSummary()
}

function changeSummaryPage(page: number, pageSize: number): void {
  summaryPage.value = page
  summaryPageSize.value = pageSize
  void loadSummary()
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
    resetSession()
    void loadCalendar()
  }
})

onMounted(async () => {
  await loadContext()
  initialized.value = true
})
</script>

<template>
  <div class="page-heading attendance-page-heading">
    <div>
      <p class="eyebrow">Attendance workspace</p>
      <h1>Điểm danh</h1>
      <p>Mở buổi học, ghi nhận ngoại lệ và theo dõi chuyên cần trong một workspace.</p>
    </div>
    <div class="page-heading-actions"><Button label="Làm mới context" icon="pi pi-refresh" severity="secondary" outlined :loading="contextLoading" @click="loadContext" /></div>
  </div>

  <FormAlert v-if="statusMessage" tone="success" :message="statusMessage" />
  <FormAlert v-if="mutationError" tone="error" :message="mutationError" />
  <FormAlert v-if="contextError && !contextForbidden" tone="error" :message="contextError" />
  <FormAlert v-if="contextForbidden" tone="warning" message="Bạn không có quyền tải context điểm danh." />

  <div class="tab-strip attendance-tab-strip">
    <Button label="Điểm danh theo buổi" icon="pi pi-calendar" :severity="tab === 'session' ? 'primary' : 'secondary'" :outlined="tab !== 'session'" @click="changeTab('session')" />
    <Button label="Lịch sử của học sinh" icon="pi pi-history" :severity="tab === 'history' ? 'primary' : 'secondary'" :outlined="tab !== 'history'" @click="changeTab('history')" />
    <Button label="Báo cáo lớp" icon="pi pi-chart-bar" :severity="tab === 'summary' ? 'primary' : 'secondary'" :outlined="tab !== 'summary'" @click="changeTab('summary')" />
  </div>

  <template v-if="tab === 'session'">
    <AttendanceContextPanel
      v-model:academic-year-id="selectedAcademicYearId"
      v-model:semester-id="selectedSemesterId"
      v-model:class-id="selectedClassId"
      v-model:attendance-date="attendanceDate"
      v-model:session-period="sessionPeriod"
      :academic-years="academicYears"
      :semesters="semesters"
      :classes="classes"
      :loading="contextLoading"
      :calendar-loading="calendarLoading"
      :calendar-status="calendarStatus"
      :calendar-message="calendarMessage"
      :open-disabled="sessionReadOnly || Boolean(contextError)"
      @open="openSession"
    />
    <FormAlert v-if="calendarError" tone="warning" :message="calendarError" />
    <FormAlert v-if="sessionError && !sessionForbidden" tone="error" :message="sessionError" />
    <FormAlert v-if="sessionForbidden" tone="warning" message="Bạn không có quyền thao tác attendance session này." />
    <section class="content-surface attendance-session-surface">
      <div class="section-heading">
        <div><h2>Danh sách học sinh của buổi</h2><p class="section-caption">{{ session ? `Session #${session.sessionId} · ${session.attendanceDate} · ${session.sessionPeriod === 'MORNING' ? 'Sáng' : 'Chiều'}` : 'Chưa mở session' }}</p></div>
        <span v-if="session" class="field-hint">{{ sessionStudents.length }} học sinh · không tạo PRESENT record</span>
      </div>
      <div v-if="sessionState === 'loading'" class="page-state page-state-loading" role="status"><i class="pi pi-spin pi-spinner" aria-hidden="true" /><span>Đang tải danh sách điểm danh...</span></div>
      <AttendanceSessionTable v-else :students="sessionStudents" :loading="sessionLoading" :read-only="sessionReadOnly" @exception="openException" @delete="confirmDelete" />
      <p v-if="!session" class="section-caption attendance-session-hint">Chọn context hợp lệ rồi bấm “Mở buổi điểm danh” để tải roster.</p>
    </section>
  </template>

  <AttendanceHistoryPanel
    v-else-if="tab === 'history'"
    v-model:academic-year-id="historyAcademicYearId"
    v-model:semester-id="historySemesterId"
    v-model:from="historyFrom"
    v-model:to="historyTo"
    :academic-years="academicYears"
    :semesters="historySemesters"
    :response="historyResponse"
    :loading="historyLoading"
    :error-message="historyError"
    :forbidden="historyForbidden"
    :page="historyPage"
    :page-size="historyPageSize"
    @update:academic-year-id="changeHistoryYear"
    @search="searchHistory"
    @page-change="changeHistoryPage"
  />

  <ClassAttendanceSummaryPanel
    v-else
    v-model:class-id="summaryClassId"
    v-model:semester-id="summarySemesterId"
    v-model:from="summaryFrom"
    v-model:to="summaryTo"
    :classes="classes"
    :semesters="semesters"
    :response="summaryResponse"
    :loading="summaryLoading"
    :error-message="summaryError"
    :forbidden="summaryForbidden"
    :page="summaryPage"
    :page-size="summaryPageSize"
    @search="searchSummary"
    @page-change="changeSummaryPage"
  />

  <AttendanceExceptionDialog v-model:visible="exceptionVisible" :student="selectedStudent" :session="session" :saving="sessionSaving" :error-message="mutationError" @save="saveException" @cancel="closeException" />
  <ConfirmDialog />
</template>
