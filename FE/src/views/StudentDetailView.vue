<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Select from 'primevue/select'
import Tab from 'primevue/tab'
import TabList from 'primevue/tablist'
import TabPanel from 'primevue/tabpanel'
import TabPanels from 'primevue/tabpanels'
import Tabs from 'primevue/tabs'
import Tag from 'primevue/tag'

import EmptyState from '@/components/EmptyState.vue'
import ServerPagination from '@/components/ServerPagination.vue'
import TranscriptAnnualTable from '@/components/TranscriptAnnualTable.vue'
import TranscriptStatusCard from '@/components/TranscriptStatusCard.vue'
import TranscriptTermTable from '@/components/TranscriptTermTable.vue'
import { fetchAcademicYears, fetchSemesters } from '@/services/academicApi'
import { fetchStudentAttendanceHistoryById } from '@/services/attendanceApi'
import { clearAuthSession, getAuthSession } from '@/services/authSession'
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
import type {
  ResStudentAnnualTranscriptDTO,
  ResStudentTermTranscriptDTO,
  ResTranscriptCalculationStatusDTO,
} from '@/types/transcript'
import { formatStudentDate } from '@/utils/studentDate'

const route = useRoute()
const router = useRouter()

const studentId = computed(() => {
  const id = Number(route.params.studentId)
  return Number.isFinite(id) && id > 0 ? id : 0
})

const activeTab = ref('profile')
const activeTranscriptSubTab = ref<'term' | 'annual'>('term')

const student = ref<Student | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const statusMessage = ref('')

const session = computed(() => getAuthSession())
const userRoles = computed(() => session.value?.user.roles ?? [])
const canRecalculate = computed(() => {
  return userRoles.value.includes('ADMIN') || userRoles.value.includes('ACADEMIC_OFFICE')
})

function token(): string | null {
  const sess = getAuthSession()
  if (sess) return sess.accessToken
  clearAuthSession()
  void router.replace('/login')
  return null
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

interface NormalizedEnrollmentRecord {
  enrollmentId: number
  classCode: string
  academicYear: string | number
  enrolledAt: string
  status: string
  transfers: Array<{
    transferId: number
    fromClass: string
    toClass: string
    effectiveAt: string
    reason: string
  }>
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

function getAttendanceSeverity(status?: string | null): 'success' | 'danger' | 'warn' | 'info' | 'secondary' {
  if (status === 'PRESENT') return 'success'
  if (status === 'ABSENT' || status === 'UNEXCUSED_ABSENCE') return 'danger'
  if (status === 'EXCUSED' || status === 'EXCUSED_ABSENCE') return 'warn'
  if (status === 'LATE') return 'info'
  return 'secondary'
}

function getAttendanceLabel(status?: string | null): string {
  if (status === 'PRESENT') return 'Có mặt'
  if (status === 'ABSENT' || status === 'UNEXCUSED_ABSENCE') return 'Vắng không phép'
  if (status === 'EXCUSED' || status === 'EXCUSED_ABSENCE') return 'Vắng có phép'
  if (status === 'LATE') return 'Đi trễ'
  if (status === 'EARLY_LEAVE') return 'Về sớm'
  return status || '—'
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
          <div class="tab-content-grid">
            <!-- Section 1: Demographics -->
            <section class="content-surface info-card">
              <div class="card-header">
                <h2>Thông tin lý lịch học sinh</h2>
                <p class="section-caption">Dữ liệu nhân khẩu học chính thức được quản lý trên hệ thống.</p>
              </div>

              <dl class="meta-grid">
                <div class="meta-item">
                  <dt>Mã học sinh</dt>
                  <dd class="font-mono font-semibold">{{ student?.studentCode || '—' }}</dd>
                </div>
                <div class="meta-item">
                  <dt>Họ và tên</dt>
                  <dd class="font-semibold">{{ student?.studentName || '—' }}</dd>
                </div>
                <div class="meta-item">
                  <dt>Ngày sinh</dt>
                  <dd>{{ formatStudentDate(student?.dateOfBirth) }}</dd>
                </div>
                <div class="meta-item">
                  <dt>Địa chỉ thường trú</dt>
                  <dd>{{ student?.address || '—' }}</dd>
                </div>
                <div class="meta-item">
                  <dt>Trạng thái học vụ</dt>
                  <dd>
                    <Tag
                      :value="getStatusLabel(student?.status)"
                      :severity="getStatusSeverity(student?.status)"
                    />
                  </dd>
                </div>
              </dl>
            </section>

            <!-- Section 2: Linked Login Account -->
            <section class="content-surface info-card">
              <div class="card-header">
                <h2>Tài khoản đăng nhập liên kết (V3)</h2>
                <p class="section-caption">Tài khoản để học sinh đăng nhập và tra cứu thông tin của mình.</p>
              </div>

              <div v-if="student?.account" class="account-card-body">
                <div class="account-badge-box">
                  <i class="pi pi-id-card text-primary text-3xl mb-2" />
                  <Tag value="Tài khoản đang hoạt động" severity="success" />
                </div>
                <dl class="meta-grid">
                  <div class="meta-item">
                    <dt>Mã người dùng</dt>
                    <dd class="font-mono">{{ student.account.userId }}</dd>
                  </div>
                  <div class="meta-item">
                    <dt>Tên đăng nhập</dt>
                    <dd class="font-mono font-semibold">{{ student.account.username }}</dd>
                  </div>
                  <div class="meta-item">
                    <dt>Vai trò hệ thống</dt>
                    <dd><Tag :value="({ STUDENT: 'Học sinh', TEACHER: 'Giáo viên', ADMIN: 'Quản trị viên', ACADEMIC_OFFICE: 'Giáo vụ' } as Record<string, string>)[student.account.role] ?? 'Chưa xác định'" severity="info" /></dd>
                  </div>
                </dl>
              </div>

              <EmptyState
                v-else
                icon="pi pi-user-minus"
                heading="Chưa cấp tài khoản đăng nhập"
                message="Học sinh này chưa có tài khoản người dùng liên kết trong hệ thống."
              />
            </section>
          </div>
        </TabPanel>

        <!-- =============================================================== -->
        <!-- TAB 2: Enrollment & Transfer History                            -->
        <!-- =============================================================== -->
        <TabPanel value="enrollment">
          <div class="tab-stack">
            <!-- Current Class Overview -->
            <section v-if="activeEnrollment" class="content-surface active-enrollment-card">
              <div class="card-header">
                <h2>Thông tin lớp học hiện tại</h2>
              </div>
              <div class="active-class-grid">
                <div class="active-class-pill">
                  <span class="label">Lớp học:</span>
                  <strong class="text-xl text-primary">{{ activeEnrollment.classCode }}</strong>
                </div>
                <div class="active-class-pill">
                  <span class="label">Năm học:</span>
                  <strong>{{ activeEnrollment.academicYear }}</strong>
                </div>
                <div class="active-class-pill">
                  <span class="label">Ngày vào lớp:</span>
                  <strong>{{ formatStudentDate(activeEnrollment.enrolledAt) }}</strong>
                </div>
                <div class="active-class-pill">
                  <span class="label">Trạng thái:</span>
                  <Tag :value="({ ACTIVE: 'Đang học', COMPLETED: 'Đã hoàn thành', WITHDRAWN: 'Đã rút khỏi lớp' } as Record<string, string>)[activeEnrollment.status] ?? 'Chưa xác định'" severity="success" />
                </div>
              </div>
            </section>

            <!-- Transfers Timeline / Table -->
            <section class="content-surface">
              <div class="card-header">
                <h2>Lịch sử chuyển lớp & biến động học vụ</h2>
                <p class="section-caption">Ghi nhận chi tiết các lần chuyển lớp trong suốt quá trình học.</p>
              </div>

              <DataTable
                v-if="allTransfers.length > 0"
                :value="allTransfers"
                class="transfers-table"
              >
                <Column field="fromClass" header="Lớp chuyển từ" />
                <Column field="toClass" header="Lớp chuyển đến" />
                <Column field="effectiveAt" header="Ngày hiệu lực">
                  <template #body="{ data }">
                    {{ formatStudentDate(data.effectiveAt) }}
                  </template>
                </Column>
                <Column field="reason" header="Lý do chuyển lớp" />
              </DataTable>

              <EmptyState
                v-else-if="normalizedEnrollments.length > 0"
                icon="pi pi-arrows-alt"
                heading="Không có dữ liệu chuyển lớp"
                message="Học sinh chưa từng chuyển lớp kể từ khi nhập học."
              />

              <EmptyState
                v-else
                icon="pi pi-folder-open"
                heading="Chưa có dữ liệu phân lớp"
                message="Học sinh hiện chưa được xếp vào lớp học nào."
              />
            </section>
          </div>
        </TabPanel>

        <!-- =============================================================== -->
        <!-- TAB 3: Attendance History                                       -->
        <!-- =============================================================== -->
        <TabPanel value="attendance">
          <div class="tab-stack">
            <!-- Attendance Filter Bar -->
            <section class="content-surface filter-bar">
              <div class="filter-controls">
                <div class="filter-item">
                  <label for="att-year">Năm học</label>
                  <Select
                    id="att-year"
                    v-model="selectedAcademicYearId"
                    :options="academicYears"
                    option-label="code"
                    option-value="id"
                    placeholder="Chọn năm học"
                  />
                </div>
                <div class="filter-item">
                  <label for="att-sem">Học kỳ</label>
                  <Select
                    id="att-sem"
                    v-model="selectedSemesterId"
                    :options="semesters"
                    option-label="name"
                    option-value="id"
                    placeholder="Chọn học kỳ"
                  />
                </div>
                <div class="filter-actions">
                  <Button
                    label="Tìm kiếm"
                    icon="pi pi-search"
                    :loading="attendanceLoading"
                    @click="loadAttendance"
                  />
                </div>
              </div>
            </section>

            <!-- Attendance Metric Summary Cards -->
            <div class="attendance-summary-cards">
              <div class="metric-card">
                <span class="metric-label">Buổi học hợp lệ</span>
                <strong class="metric-value">{{ attendanceSummary.validSessionCount }}</strong>
              </div>
              <div class="metric-card metric-success">
                <span class="metric-label">Có mặt (Tỷ lệ)</span>
                <strong class="metric-value">{{ attendanceSummary.presentCount }} ({{ attendanceRate }})</strong>
              </div>
              <div class="metric-card metric-warn">
                <span class="metric-label">Vắng có phép</span>
                <strong class="metric-value">{{ attendanceSummary.excusedAbsenceCount }}</strong>
              </div>
              <div class="metric-card metric-danger">
                <span class="metric-label">Vắng không phép</span>
                <strong class="metric-value">{{ attendanceSummary.unexcusedAbsenceCount }}</strong>
              </div>
              <div class="metric-card metric-info">
                <span class="metric-label">Đi trễ</span>
                <strong class="metric-value">{{ attendanceSummary.lateCount }}</strong>
              </div>
            </div>

            <!-- Attendance Detail Table -->
            <section class="content-surface">
              <div class="card-header">
                <h2>Chi tiết các buổi điểm danh</h2>
              </div>

              <DataTable
                v-if="attendanceRecords.length > 0"
                :value="attendanceRecords"
                :loading="attendanceLoading"
              >
                <Column field="attendanceDate" header="Ngày điểm danh">
                  <template #body="{ data }">
                    {{ formatStudentDate(data.attendanceDate) }}
                  </template>
                </Column>
                <Column field="sessionPeriod" header="Buổi học">
                  <template #body="{ data }">
                    {{ data.sessionPeriod === 'MORNING' ? 'Sáng' : 'Chiều' }}
                  </template>
                </Column>
                <Column field="className" header="Lớp học" />
                <Column field="status" header="Trạng thái">
                  <template #body="{ data }">
                    <Tag
                      :value="getAttendanceLabel(data.status)"
                      :severity="getAttendanceSeverity(data.status)"
                    />
                  </template>
                </Column>
                <Column field="note" header="Ghi chú">
                  <template #body="{ data }">
                    {{ data.note || '—' }}
                  </template>
                </Column>
              </DataTable>

              <div v-if="attendanceRecords.length > 0" class="table-pagination">
                <ServerPagination
                  :page="attendancePage"
                  :page-size="attendancePageSize"
                  :total-records="attendanceResponse?.totalElements ?? 0"
                  @page-change="handleAttendancePageChange"
                />
              </div>

              <EmptyState
                v-else
                icon="pi pi-calendar-times"
                heading="Chưa có dữ liệu điểm danh"
                message="Không có bản ghi điểm danh nào trong khoảng thời gian đã chọn."
              />
            </section>
          </div>
        </TabPanel>

        <!-- =============================================================== -->
        <!-- TAB 4: Transcript & Calculation Task                            -->
        <!-- =============================================================== -->
        <TabPanel value="transcript">
          <div class="tab-stack">
            <!-- Transcript Filter Bar & Recalculate Trigger -->
            <section class="content-surface filter-bar">
              <div class="filter-controls">
                <div class="filter-item">
                  <label for="tc-year">Năm học</label>
                  <Select
                    id="tc-year"
                    v-model="selectedAcademicYearId"
                    :options="academicYears"
                    option-label="code"
                    option-value="id"
                    placeholder="Chọn năm học"
                  />
                </div>
                <div v-if="activeTranscriptSubTab === 'term'" class="filter-item">
                  <label for="tc-sem">Học kỳ</label>
                  <Select
                    id="tc-sem"
                    v-model="selectedSemesterId"
                    :options="semesters"
                    option-label="name"
                    option-value="id"
                    placeholder="Chọn học kỳ"
                  />
                </div>
                <div class="filter-actions">
                  <Button
                    label="Làm mới"
                    icon="pi pi-refresh"
                    severity="secondary"
                    :loading="transcriptLoading"
                    @click="loadTranscript"
                  />
                  <!-- Recalculate Action for ADMIN & ACADEMIC_OFFICE -->
                  <Button
                    v-if="canRecalculate"
                    label="Yêu cầu tính lại điểm"
                    icon="pi pi-cog"
                    severity="warning"
                    :loading="recalculating"
                    @click="triggerRecalculation"
                  />
                </div>
              </div>
            </section>

            <!-- Sub-tab toggles: Term vs Annual -->
            <div class="sub-tab-strip">
              <Button
                label="Bảng điểm Học kỳ"
                :severity="activeTranscriptSubTab === 'term' ? 'primary' : 'secondary'"
                :outlined="activeTranscriptSubTab !== 'term'"
                @click="activeTranscriptSubTab = 'term'"
              />
              <Button
                label="Bảng điểm Cả năm"
                :severity="activeTranscriptSubTab === 'annual' ? 'primary' : 'secondary'"
                :outlined="activeTranscriptSubTab !== 'annual'"
                @click="activeTranscriptSubTab = 'annual'"
              />
            </div>

            <!-- Transcript Calculation Status Card -->
            <TranscriptStatusCard
              v-if="transcriptStatus"
              :status="transcriptStatus"
              :student-name="student?.studentName"
              :loading="transcriptLoading"
              @refresh="loadTranscript"
            />

            <!-- Transcript Tables -->
            <section class="content-surface">
              <div v-if="activeTranscriptSubTab === 'term'">
                <TranscriptTermTable
                  v-if="termTranscript && termTranscript.subjects && termTranscript.subjects.length > 0"
                  :subjects="termTranscript.subjects"
                  :dtbhk="termTranscript.dtbhk"
                />
                <EmptyState
                  v-else
                  icon="pi pi-file-excel"
                  heading="Chưa có dữ liệu bảng điểm học kỳ"
                  message="Học sinh chưa có bảng điểm chính thức cho học kỳ đã chọn."
                />
              </div>

              <div v-else>
                <TranscriptAnnualTable
                  v-if="annualTranscript && annualTranscript.subjects && annualTranscript.subjects.length > 0"
                  :subjects="annualTranscript.subjects"
                  :regular-dtbcn="annualTranscript.regularDtbcn"
                  :final-dtbcn="annualTranscript.finalDtbcn"
                />
                <EmptyState
                  v-else
                  icon="pi pi-file-excel"
                  heading="Chưa có dữ liệu bảng điểm cả năm"
                  message="Học sinh chưa có bảng điểm chính thức cho năm học đã chọn."
                />
              </div>
            </section>
          </div>
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
