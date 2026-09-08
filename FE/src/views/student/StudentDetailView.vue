<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Tab from 'primevue/tab'
import TabList from 'primevue/tablist'
import TabPanel from 'primevue/tabpanel'
import TabPanels from 'primevue/tabpanels'
import Tabs from 'primevue/tabs'
import Tag from 'primevue/tag'

import StudentAttendanceHistoryPanel from '@/components/student/StudentAttendanceHistoryPanel.vue'
import StudentEnrollmentPanel from '@/components/student/StudentEnrollmentPanel.vue'
import type { NormalizedEnrollmentRecord } from '@/components/student/StudentEnrollmentPanel.vue'
import StudentProfilePanel from '@/components/student/StudentProfilePanel.vue'
import StudentTranscriptPanel from '@/components/student/StudentTranscriptPanel.vue'
import type { TranscriptSubTab } from '@/components/student/StudentTranscriptPanel.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { fetchAcademicYears, fetchSemesters } from '@/services/academicApi'
import { fetchStudentAttendanceHistoryById } from '@/services/attendanceApi'
import { recalculateTranscriptById } from '@/services/calculationTaskApi'
import { fetchStudentEnrollmentHistory } from '@/services/enrollmentApi'
import { getStudent } from '@/services/studentApi'
import {
  fetchStudentAnnualTranscript,
  fetchStudentTermTranscript,
} from '@/services/transcriptApi'
import type { AcademicYear, Semester } from '@/types/academic'
import { studentUiMessage } from '@/utils/studentUiMessage'
import { isApiError } from '@/types/api'
import type {
  StudentAttendanceHistoryItem,
  StudentAttendanceHistoryResponse,
  StudentAttendanceHistorySummary,
} from '@/types/attendance'
import type { StudentEnrollmentHistory, TransferHistory } from '@/types/enrollment'
import type { Student, StudentAcademicStatus } from '@/types/student'
import type { ResStudentAnnualTranscriptDTO, ResStudentTermTranscriptDTO, ResTranscriptCalculationStatusDTO } from '@/types/transcript'

const route = useRoute()
const router = useRouter()

const studentId = computed(() => {
  const id = Number(route.params.studentId)
  return Number.isFinite(id) && id > 0 ? id : 0
})

const activeTab = ref('profile')
const activeTranscriptSubTab = ref<TranscriptSubTab>('term')

const student = ref<Student | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const statusMessage = ref('')

const { roles: userRoles, requireAccessToken: token } = useAuthSession()
const canRecalculate = computed(() => {
  return userRoles.value.includes('ADMIN') || userRoles.value.includes('ACADEMIC_OFFICE')
})

function getStatusSeverity(status?: StudentAcademicStatus | string | null): 'success' | 'secondary' | 'info' {
  if (status === 'INACTIVE') return 'secondary'
  if (status === 'GRADUATED') return 'info'
  return 'success'
}

function getStatusLabel(status?: StudentAcademicStatus | string | null): string {
  if (status === 'INACTIVE') return 'Ngừng học'
  if (status === 'GRADUATED') return 'Tốt nghiệp'
  return 'Đang học'
}

// ---------------------------------------------------------------------------
// Context: Academic Years & Semesters
// ---------------------------------------------------------------------------
const academicYears = ref<AcademicYear[]>([])
const semesters = ref<Semester[]>([])
const selectedAcademicYearId = ref<number | null>(null)
const selectedSemesterId = ref<number | null>(null)

async function loadAcademicContext(): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  try {
    const years = await fetchAcademicYears(accessToken)
    academicYears.value = years
    if (years.length > 0) {
      const activeYear = years.find((y) => y.status === 'ACTIVE') ?? years[0]
      selectedAcademicYearId.value = activeYear.id
      await loadSemesters(activeYear.id)
    }
  } catch (err: unknown) {
    if (isApiError(err, 401)) return
    // Non-critical background context failure
  }
}

async function loadSemesters(yearId: number): Promise<void> {
  const accessToken = token()
  if (!accessToken) return
  try {
    const sems = await fetchSemesters(accessToken, yearId)
    semesters.value = sems
    if (sems.length > 0) {
      const activeSem = sems.find((s) => s.status === 'ACTIVE') ?? sems[0]
      selectedSemesterId.value = activeSem.id
    } else {
      selectedSemesterId.value = null
    }
  } catch (err: unknown) {
    if (isApiError(err, 401)) return
    semesters.value = []
    selectedSemesterId.value = null
  }
}

// ---------------------------------------------------------------------------
// Tab 1: Profile & Account
// ---------------------------------------------------------------------------
async function loadStudent(): Promise<void> {
  const accessToken = token()
  if (!accessToken || !studentId.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    student.value = await getStudent(accessToken, studentId.value)
  } catch (err: unknown) {
    if (isApiError(err, 401)) return
    errorMessage.value = studentUiMessage(err, 'Không thể tải thông tin học sinh.')
  } finally {
    loading.value = false
  }
}

// ---------------------------------------------------------------------------
// Tab 2: Enrollment & Transfer History
// ---------------------------------------------------------------------------
const enrollments = ref<StudentEnrollmentHistory[]>([])
const enrollmentLoading = ref(false)

async function loadEnrollments(): Promise<void> {
  const accessToken = token()
  if (!accessToken || !studentId.value) return
  enrollmentLoading.value = true
  try {
    enrollments.value = await fetchStudentEnrollmentHistory(accessToken, studentId.value)
  } catch (err: unknown) {
    if (isApiError(err, 401)) return
    enrollments.value = []
  } finally {
    enrollmentLoading.value = false
  }
}

const normalizedEnrollments = computed<NormalizedEnrollmentRecord[]>(() => {
  return enrollments.value.map((item: StudentEnrollmentHistory) => {
    const enrollment = item.enrollment
    const rawYearId = enrollment.academicYearId
    const matchedYear = academicYears.value.find((y) => y.id === rawYearId)
    const academicYearDisplay = matchedYear?.code ?? String(rawYearId)
    return {
      enrollmentId: enrollment.id,
      classCode: enrollment.currentClassCode,
      academicYear: academicYearDisplay,
      enrolledAt: enrollment.enrolledAt,
      status: enrollment.status,
      transfers: item.transfers.map((transfer: TransferHistory) => ({
        transferId: transfer.transferId,
        fromClass: transfer.fromClassId ? `Lớp #${transfer.fromClassId}` : '—',
        toClass: `Lớp #${transfer.toClassId}`,
        effectiveAt: transfer.effectiveAt,
        reason: transfer.reason ?? '—',
      })),
    }
  })
})

const activeEnrollment = computed(() => {
  return normalizedEnrollments.value.find((e) => e.status === 'ACTIVE') ?? normalizedEnrollments.value[0] ?? null
})

const allTransfers = computed(() => {
  return normalizedEnrollments.value.flatMap((e) => e.transfers)
})

// ---------------------------------------------------------------------------
// Tab 3: Attendance History
// ---------------------------------------------------------------------------
const attendanceResponse = ref<StudentAttendanceHistoryResponse | null>(null)
const attendanceLoading = ref(false)
const attendancePage = ref(0)
const attendancePageSize = ref(10)

async function loadAttendance(): Promise<void> {
  const accessToken = token()
  if (!accessToken || !studentId.value) return
  attendanceLoading.value = true
  try {
    const query = {
      academicYearId: selectedAcademicYearId.value,
      semesterId: selectedSemesterId.value,
      page: attendancePage.value,
      size: attendancePageSize.value,
    }
    attendanceResponse.value = await fetchStudentAttendanceHistoryById(accessToken, studentId.value, query)
  } catch (err: unknown) {
    if (isApiError(err, 401)) return
    attendanceResponse.value = null
  } finally {
    attendanceLoading.value = false
  }
}

const attendanceSummary = computed<StudentAttendanceHistorySummary>(() => {
  return (
    attendanceResponse.value?.summary ?? {
      validSessionCount: 0,
      presentCount: 0,
      excusedAbsenceCount: 0,
      unexcusedAbsenceCount: 0,
      lateCount: 0,
      earlyLeaveCount: 0,
    }
  )
})

const attendanceRecords = computed<StudentAttendanceHistoryItem[]>(() => {
  return attendanceResponse.value?.items ?? []
})

const attendanceRate = computed(() => {
  const valid = attendanceSummary.value.validSessionCount
  if (!valid || valid <= 0) return '0.0%'
  const present = attendanceSummary.value.presentCount
  return `${((present / valid) * 100).toFixed(1)}%`
})

function handleAttendancePageChange(page: number, pageSize: number): void {
  attendancePage.value = page
  attendancePageSize.value = pageSize
  void loadAttendance()
}

// ---------------------------------------------------------------------------
// Tab 4: Transcript & Calculation Task
// ---------------------------------------------------------------------------
const termTranscript = ref<ResStudentTermTranscriptDTO | null>(null)
const annualTranscript = ref<ResStudentAnnualTranscriptDTO | null>(null)
const transcriptLoading = ref(false)
const recalculating = ref(false)

async function loadTranscript(): Promise<void> {
  const accessToken = token()
  if (!accessToken || !studentId.value) return
  transcriptLoading.value = true
  try {
    if (activeTranscriptSubTab.value === 'term') {
      if (selectedSemesterId.value) {
        termTranscript.value = await fetchStudentTermTranscript(
          accessToken,
          studentId.value,
          selectedSemesterId.value,
        )
      } else {
        termTranscript.value = null
      }
    } else {
      if (selectedAcademicYearId.value) {
        annualTranscript.value = await fetchStudentAnnualTranscript(
          accessToken,
          studentId.value,
          selectedAcademicYearId.value,
        )
      } else {
        annualTranscript.value = null
      }
    }
  } catch (err: unknown) {
    if (isApiError(err, 401)) return
    if (activeTranscriptSubTab.value === 'term') {
      termTranscript.value = null
    } else {
      annualTranscript.value = null
    }
  } finally {
    transcriptLoading.value = false
  }
}

const transcriptStatus = computed<ResTranscriptCalculationStatusDTO | null>(() => {
  if (activeTranscriptSubTab.value === 'term' && termTranscript.value) {
    return {
      studentId: studentId.value,
      studentCode: student.value?.studentCode,
      academicYearId: termTranscript.value.academicYearId,
      semesterId: termTranscript.value.semesterId,
      calculationStatus: termTranscript.value.calculationStatus,
      sourceVersion: termTranscript.value.sourceVersion,
      calculatedVersion: termTranscript.value.calculatedVersion,
      calculatedAt: termTranscript.value.calculatedAt,
      isUpToDate: termTranscript.value.calculationStatus === 'FINISH',
    }
  }
  if (activeTranscriptSubTab.value === 'annual' && annualTranscript.value) {
    return {
      studentId: studentId.value,
      studentCode: student.value?.studentCode,
      academicYearId: annualTranscript.value.academicYearId,
      calculationStatus: annualTranscript.value.calculationStatus,
      sourceVersion: annualTranscript.value.sourceVersion,
      calculatedVersion: annualTranscript.value.calculatedVersion,
      calculatedAt: annualTranscript.value.calculatedAt,
      isUpToDate: annualTranscript.value.calculationStatus === 'FINISH',
    }
  }
  return null
})

async function triggerRecalculation(): Promise<void> {
  const accessToken = token()
  if (!accessToken || !studentId.value || !selectedAcademicYearId.value) return
  recalculating.value = true
  statusMessage.value = ''
  try {
    const task = await recalculateTranscriptById(accessToken, studentId.value, selectedAcademicYearId.value)
    statusMessage.value = `Yêu cầu tính lại điểm đã được tiếp nhận thành công (Mã tác vụ: #${task.taskId}, Trạng thái: ${({ PENDING: 'Đang chờ', RUNNING: 'Đang xử lý', SUCCEEDED: 'Thành công', FAILED: 'Thất bại', CANCELLED: 'Đã hủy' } as Record<string, string>)[task.status] ?? 'Chưa xác định'}). Hệ thống đang xử lý trong nền.`
    await loadTranscript()
  } catch (err: unknown) {
    if (isApiError(err, 401)) return
    errorMessage.value = studentUiMessage(err, 'Không thể gửi yêu cầu tính lại điểm.')
  } finally {
    recalculating.value = false
  }
}

// ---------------------------------------------------------------------------
// Tab switching & Context watching
// ---------------------------------------------------------------------------
watch(activeTab, (tab) => {
  if (tab === 'enrollment' && enrollments.value.length === 0) {
    void loadEnrollments()
  } else if (tab === 'attendance' && !attendanceResponse.value) {
    void loadAttendance()
  } else if (tab === 'transcript' && !termTranscript.value && !annualTranscript.value) {
    void loadTranscript()
  }
})

watch(selectedAcademicYearId, async (newYearId) => {
  if (newYearId) {
    await loadSemesters(newYearId)
    if (activeTab.value === 'attendance') {
      await loadAttendance()
    } else if (activeTab.value === 'transcript') {
      await loadTranscript()
    }
  }
})

watch(selectedSemesterId, async () => {
  if (activeTab.value === 'attendance') {
    await loadAttendance()
  } else if (activeTab.value === 'transcript' && activeTranscriptSubTab.value === 'term') {
    await loadTranscript()
  }
})

watch(activeTranscriptSubTab, () => {
  void loadTranscript()
})

onMounted(async () => {
  await Promise.all([loadStudent(), loadAcademicContext()])
})
</script>

<template>
  <div class="student-detail-view" data-testid="student-detail-view">
    <!-- Header -->
    <div class="page-heading">
      <div>
        <div class="heading-badge-row">
          <Tag
            v-if="student"
            :value="getStatusLabel(student.status)"
            :severity="getStatusSeverity(student.status)"
          />
        </div>
        <h1 class="student-title">
          {{ student?.studentName || 'Đang tải hồ sơ...' }}
          <span v-if="student?.studentCode" class="student-code-text">({{ student.studentCode }})</span>
        </h1>
      </div>

      <div class="page-heading-actions">
        <Button
          label="Quay lại danh sách"
          icon="pi pi-arrow-left"
          severity="secondary"
          outlined
          @click="router.push('/v2/students')"
        />
        <Button
          v-if="student"
          label="Chỉnh sửa"
          icon="pi pi-pencil"
          @click="router.push(`/v2/students/${student.studentId}/edit`)"
        />
      </div>
    </div>

    <!-- Alert Notifications -->
    <div v-if="statusMessage" class="form-alert form-alert-info" role="status">
      {{ statusMessage }}
    </div>
    <div v-if="errorMessage" class="form-alert form-alert-error" role="alert">
      {{ errorMessage }}
    </div>

    <!-- 4 Tabs PrimeVue Workspace -->
    <Tabs v-model:value="activeTab" class="detail-tabs">
      <TabList>
        <Tab value="profile">
          <i class="pi pi-user mr-2" /> Hồ sơ cá nhân & Tài khoản
        </Tab>
        <Tab value="enrollment">
          <i class="pi pi-sitemap mr-2" /> Xếp lớp & Lịch sử chuyển lớp
        </Tab>
        <Tab value="attendance">
          <i class="pi pi-calendar-clock mr-2" /> Chuyên cần & Điểm danh
        </Tab>
        <Tab value="transcript">
          <i class="pi pi-chart-bar mr-2" /> Bảng điểm & Học bạ
        </Tab>
      </TabList>

      <TabPanels class="tab-panels-content">
        <!-- =============================================================== -->
        <!-- TAB 1: Profile & Account                                         -->
        <!-- =============================================================== -->
        <TabPanel value="profile">
          <StudentProfilePanel :student="student" />
        </TabPanel>

        <!-- =============================================================== -->
        <!-- TAB 2: Enrollment & Transfer History                            -->
        <!-- =============================================================== -->
        <TabPanel value="enrollment">
          <StudentEnrollmentPanel
            :active-enrollment="activeEnrollment"
            :enrollment-count="normalizedEnrollments.length"
            :all-transfers="allTransfers"
          />
        </TabPanel>

        <!-- =============================================================== -->
        <!-- TAB 3: Attendance History                                       -->
        <!-- =============================================================== -->
        <TabPanel value="attendance">
          <StudentAttendanceHistoryPanel
            :academic-years="academicYears"
            :semesters="semesters"
            :selected-academic-year-id="selectedAcademicYearId"
            :selected-semester-id="selectedSemesterId"
            :loading="attendanceLoading"
            :summary="attendanceSummary"
            :records="attendanceRecords"
            :total-elements="attendanceResponse?.totalElements ?? 0"
            :page="attendancePage"
            :page-size="attendancePageSize"
            :attendance-rate="attendanceRate"
            @update:selected-academic-year-id="selectedAcademicYearId = $event"
            @update:selected-semester-id="selectedSemesterId = $event"
            @search="loadAttendance"
            @page-change="handleAttendancePageChange"
          />
        </TabPanel>

        <!-- =============================================================== -->
        <!-- TAB 4: Transcript & Calculation Task                            -->
        <!-- =============================================================== -->
        <TabPanel value="transcript">
          <StudentTranscriptPanel
            :academic-years="academicYears"
            :semesters="semesters"
            :selected-academic-year-id="selectedAcademicYearId"
            :selected-semester-id="selectedSemesterId"
            :active-sub-tab="activeTranscriptSubTab"
            :term-transcript="termTranscript"
            :annual-transcript="annualTranscript"
            :transcript-status="transcriptStatus"
            :student-name="student?.studentName"
            :loading="transcriptLoading"
            :can-recalculate="canRecalculate"
            :recalculating="recalculating"
            @update:selected-academic-year-id="selectedAcademicYearId = $event"
            @update:selected-semester-id="selectedSemesterId = $event"
            @update:active-sub-tab="activeTranscriptSubTab = $event"
            @refresh="loadTranscript"
            @recalculate="triggerRecalculation"
          />
        </TabPanel>
      </TabPanels>
    </Tabs>
  </div>
</template>

<style scoped>
.student-detail-view {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
.heading-badge-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.student-title {
  margin: 0.25rem 0;
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}
.student-code-text {
  font-size: 1.125rem;
  font-weight: normal;
  color: var(--text-color-secondary, #64748b);
}
.detail-tabs {
  margin-top: 0.5rem;
}
.tab-panels-content {
  padding-top: 1rem;
}
.tab-content-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.25rem;
}
.tab-stack {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
.info-card {
  padding: 1.5rem;
}
.card-header {
  margin-bottom: 1.25rem;
}
.card-header h2 {
  margin: 0 0 0.25rem;
  font-size: 1.25rem;
  font-weight: 600;
}
.section-caption {
  margin: 0;
  font-size: 0.875rem;
  color: var(--text-color-secondary, #64748b);
}
.meta-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 1rem;
  margin: 0;
}
.meta-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.meta-item dt {
  font-size: 0.8125rem;
  color: var(--text-color-secondary, #64748b);
  font-weight: 500;
}
.meta-item dd {
  margin: 0;
  font-size: 0.9375rem;
  color: #1e293b;
}
.account-card-body {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
.account-badge-box {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.active-class-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
}
.active-class-pill {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.active-class-pill .label {
  font-size: 0.8125rem;
  color: var(--text-color-secondary, #64748b);
}
.filter-bar {
  padding: 1rem;
}
.filter-controls {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 1rem;
}
.filter-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 180px;
}
.filter-item label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #334155;
}
.filter-actions {
  display: flex;
  gap: 0.5rem;
}
.attendance-summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 1rem;
}
.metric-card {
  padding: 1rem;
  background: var(--surface-container-lowest, #ffffff);
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.metric-label {
  font-size: 0.8125rem;
  color: var(--text-color-secondary, #64748b);
}
.metric-value {
  font-size: 1.25rem;
  color: #1e293b;
}
.metric-success .metric-value {
  color: #16a34a;
}
.metric-warn .metric-value {
  color: #d97706;
}
.metric-danger .metric-value {
  color: #dc2626;
}
.metric-info .metric-value {
  color: #2563eb;
}
.sub-tab-strip {
  display: flex;
  gap: 0.5rem;
}
.table-pagination {
  margin-top: 1rem;
}
.font-mono {
  font-family: monospace;
}
.font-semibold {
  font-weight: 600;
}
.mr-2 {
  margin-right: 0.5rem;
}
.mb-2 {
  margin-bottom: 0.5rem;
}
</style>
