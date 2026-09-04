<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Select from 'primevue/select'

import EmptyState from '@/components/EmptyState.vue'
import FormAlert from '@/components/FormAlert.vue'
import TranscriptAnnualTable from '@/components/TranscriptAnnualTable.vue'
import TranscriptTermTable from '@/components/TranscriptTermTable.vue'
import { fetchAcademicYears, fetchSemesters } from '@/services/academicApi'
import {
  fetchStudentAttendanceHistory,
  fetchStudentAttendanceHistoryById,
} from '@/services/attendanceApi'
import { getAuthSession } from '@/services/authSession'
import {
  fetchMyAnnualStatus,
  fetchMyAnnualTranscript,
  fetchMyTermStatus,
  fetchMyTermTranscript,
  fetchStudentAnnualTranscript,
  fetchStudentTermTranscript,
} from '@/services/transcriptApi'
import type { AcademicYear, Semester } from '@/types/academic'
import { isApiError } from '@/types/api'
import type {
  ResStudentAnnualTranscriptDTO,
  ResStudentTermTranscriptDTO,
} from '@/types/transcript'

const router = (() => {
  try {
    return useRouter()
  } catch {
    return undefined
  }
})()

const route = (() => {
  try {
    return useRoute()
  } catch {
    return undefined
  }
})()
const routeQuery = computed(() => route?.query ?? {})

type TranscriptTab = 'term' | 'annual'

const activeTab = ref<TranscriptTab>('term')
const academicYears = ref<AcademicYear[]>([])
const semesters = ref<Semester[]>([])

const selectedAcademicYearId = ref<number | null>(null)
const selectedSemesterId = ref<number | null>(null)

const termTranscript = ref<ResStudentTermTranscriptDTO | null>(null)
const annualTranscript = ref<ResStudentAnnualTranscriptDTO | null>(null)
const excusedAbsences = ref<number | null>(null)
const unexcusedAbsences = ref<number | null>(null)

const contextLoading = ref(false)
const transcriptLoading = ref(false)
const errorMessage = ref('')
const isForbidden = ref(false)
const isNotFound = ref(false)

const session = computed(() => getAuthSession())
const token = computed(() => session.value?.accessToken ?? '')

const currentCalculationStatus = computed(() => {
  if (activeTab.value === 'term') {
    return termTranscript.value?.calculationStatus ?? null
  }
  return annualTranscript.value?.calculationStatus ?? null
})

const currentCalculatedAt = computed(() => {
  if (activeTab.value === 'term') {
    return termTranscript.value?.calculatedAt ?? null
  }
  return annualTranscript.value?.calculatedAt ?? null
})

const currentVersion = computed(() => {
  if (activeTab.value === 'term') {
    return termTranscript.value?.calculatedVersion ?? null
  }
  return annualTranscript.value?.calculatedVersion ?? null
})

function formatDateTime(isoString: string | null): string {
  if (!isoString) return '—'
  try {
    const d = new Date(isoString)
    if (isNaN(d.getTime())) return isoString
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  } catch {
    return isoString
  }
}

async function loadContext() {
  if (!token.value) return
  contextLoading.value = true
  errorMessage.value = ''
  try {
    const years = await fetchAcademicYears(token.value)
    academicYears.value = years
    if (years.length > 0) {
      const qYearId = routeQuery.value.academicYearId ? Number(routeQuery.value.academicYearId) : null
      const matchedYear = qYearId ? years.find((y) => y.id === qYearId) : null
      const activeYear = matchedYear ?? years.find((y) => y.status === 'ACTIVE') ?? years[0]
      selectedAcademicYearId.value = activeYear.id
    }
  } catch (err: unknown) {
    if (isApiError(err)) {
      errorMessage.value = err.message
    } else {
      errorMessage.value = 'Không thể tải danh sách năm học.'
    }
  } finally {
    contextLoading.value = false
  }
}

async function loadSemesters(academicYearId: number) {
  if (!token.value) return
  try {
    const sems = await fetchSemesters(token.value, academicYearId)
    semesters.value = sems
    if (sems.length > 0) {
      const qSemId = routeQuery.value.semesterId ? Number(routeQuery.value.semesterId) : null
      const matchedSem = qSemId ? sems.find((s) => s.id === qSemId) : null
      const activeSem = matchedSem ?? sems.find((s) => s.status === 'ACTIVE') ?? sems[0]
      selectedSemesterId.value = activeSem.id
    } else {
      selectedSemesterId.value = null
      termTranscript.value = null
      excusedAbsences.value = null
      unexcusedAbsences.value = null
    }
  } catch (err: unknown) {
    if (isApiError(err)) {
      errorMessage.value = err.message
    }
  }
}

async function loadTranscript() {
  if (!token.value) return
  errorMessage.value = ''
  isForbidden.value = false
  isNotFound.value = false
  transcriptLoading.value = true

  const studentIdParam = routeQuery.value.studentId ? Number(routeQuery.value.studentId) : null

  try {
    if (activeTab.value === 'term') {
      if (!selectedSemesterId.value) {
        termTranscript.value = null
        excusedAbsences.value = null
        unexcusedAbsences.value = null
        return
      }
      const termPromise = studentIdParam
        ? fetchStudentTermTranscript(token.value, studentIdParam, selectedSemesterId.value)
        : fetchMyTermTranscript(token.value, selectedSemesterId.value)

      const attendanceQuery = {
        academicYearId: selectedAcademicYearId.value,
        semesterId: selectedSemesterId.value,
        page: 0,
        size: 1,
      }

      const attendancePromise = studentIdParam
        ? fetchStudentAttendanceHistoryById(token.value, studentIdParam, attendanceQuery).catch(() => null)
        : fetchStudentAttendanceHistory(token.value, attendanceQuery).catch(() => null)

      const [termData, attendanceRes] = await Promise.all([
        termPromise,
        attendancePromise,
      ])
      termTranscript.value = termData
      if (attendanceRes?.summary) {
        excusedAbsences.value = attendanceRes.summary.excusedAbsenceCount ?? null
        unexcusedAbsences.value = attendanceRes.summary.unexcusedAbsenceCount ?? null
      } else {
        excusedAbsences.value = null
        unexcusedAbsences.value = null
      }
    } else {
      if (!selectedAcademicYearId.value) {
        annualTranscript.value = null
        return
      }
      const data = studentIdParam
        ? await fetchStudentAnnualTranscript(token.value, studentIdParam, selectedAcademicYearId.value)
        : await fetchMyAnnualTranscript(token.value, selectedAcademicYearId.value)
      annualTranscript.value = data
    }
  } catch (err: unknown) {
    if (activeTab.value === 'term') {
      termTranscript.value = null
      excusedAbsences.value = null
      unexcusedAbsences.value = null
    } else {
      annualTranscript.value = null
    }
    if (isApiError(err)) {
      if (err.status === 403) {
        isForbidden.value = true
      } else if (err.status === 404) {
        isNotFound.value = true
      } else {
        errorMessage.value = err.message
      }
    } else {
      errorMessage.value = 'Không thể tải bảng điểm.'
    }
  } finally {
    transcriptLoading.value = false
  }
}

async function checkCalculationStatus() {
  if (!token.value) return
  try {
    if (activeTab.value === 'term' && selectedSemesterId.value) {
      const statusRes = await fetchMyTermStatus(token.value, selectedSemesterId.value)
      if (termTranscript.value) {
        termTranscript.value.calculationStatus = statusRes.calculationStatus
        termTranscript.value.calculatedVersion = statusRes.calculatedVersion
        termTranscript.value.calculatedAt = statusRes.calculatedAt
      }
      if (statusRes.calculationStatus === 'FINISH') {
        await loadTranscript()
      }
    } else if (activeTab.value === 'annual' && selectedAcademicYearId.value) {
      const statusRes = await fetchMyAnnualStatus(token.value, selectedAcademicYearId.value)
      if (annualTranscript.value) {
        annualTranscript.value.calculationStatus = statusRes.calculationStatus
        annualTranscript.value.calculatedVersion = statusRes.calculatedVersion
        annualTranscript.value.calculatedAt = statusRes.calculatedAt
      }
      if (statusRes.calculationStatus === 'FINISH') {
        await loadTranscript()
      }
    }
  } catch {
    // Silent check error fallback to loadTranscript
    await loadTranscript()
  }
}

const userRoles = computed(() => session.value?.user.roles ?? [])
const isNonStudent = computed(() =>
  userRoles.value.some((r) => ['ADMIN', 'ACADEMIC_OFFICE', 'TEACHER'].includes(r))
)
const isFromClassTranscript = computed(() => {
  return (
    isNonStudent.value ||
    Boolean(routeQuery.value.studentId) ||
    routeQuery.value.from === 'class-transcripts'
  )
})

const userRoleBadge = computed(() => {
  const roles = session.value?.user.roles ?? []
  const username = session.value?.user.username ?? ''
  if (roles.includes('ADMIN')) return `🛡️ Quản trị viên (${username})`
  if (roles.includes('ACADEMIC_OFFICE')) return `🏛️ Giáo vụ (${username})`
  if (roles.includes('TEACHER')) return `👨‍🏫 Giáo viên (${username})`
  return `👤 ${username}`
})

const targetStudentName = computed(() => {
  if (routeQuery.value.studentName) return String(routeQuery.value.studentName)
  return ''
})

const targetStudentCode = computed(() => {
  if (routeQuery.value.studentCode) return String(routeQuery.value.studentCode)
  return ''
})

const studentDisplayName = computed(() => {
  if (targetStudentName.value && targetStudentCode.value) {
    return `${targetStudentName.value} (${targetStudentCode.value})`
  }
  if (targetStudentName.value) return targetStudentName.value
  if (routeQuery.value.studentId) return `Mã HS #${routeQuery.value.studentId}`
  return ''
})

function goBackToClassTranscripts() {
  const query: Record<string, string> = {}
  if (routeQuery.value.classId) query.classId = String(routeQuery.value.classId)
  if (selectedAcademicYearId.value) query.academicYearId = String(selectedAcademicYearId.value)
  if (selectedSemesterId.value) query.semesterId = String(selectedSemesterId.value)

  if (router) {
    router.push({
      path: '/v2/class-transcripts',
      query,
    })
  }
}

watch(selectedAcademicYearId, async (newYearId) => {
  if (newYearId) {
    await loadSemesters(newYearId)
    if (activeTab.value === 'annual') {
      await loadTranscript()
    }
  }
})

watch(selectedSemesterId, async (newSemId) => {
  if (newSemId && activeTab.value === 'term') {
    await loadTranscript()
  }
})

watch(activeTab, async () => {
  await loadTranscript()
})

onMounted(async () => {
  await loadContext()
})
</script>

<template>
  <div class="transcript-view-container">
    <div class="view-header">
      <div class="header-main">
        <div v-if="isFromClassTranscript" class="back-action-container">
          <Button
            label="Quay lại Bảng điểm theo lớp"
            icon="pi pi-arrow-left"
            severity="secondary"
            outlined
            size="small"
            class="back-btn"
            @click="goBackToClassTranscripts"
          />
        </div>
        <p class="eyebrow">
          {{ isFromClassTranscript ? 'Bảng điểm theo lớp · Chi tiết bảng điểm học sinh' : 'Tra cứu bảng điểm · Read-only' }}
        </p>
        <h1 class="page-title">
          {{ studentDisplayName ? `Bảng Điểm Học Sinh: ${studentDisplayName}` : 'Bảng Điểm Học Sinh' }}
        </h1>
        <p class="page-caption">
          {{ isFromClassTranscript ? 'Xem kết quả học tập chi tiết của học sinh theo từng học kỳ và cả năm học.' : 'Dữ liệu kết quả học tập chính thức từ hệ thống tính toán điểm trung bình.' }}
        </p>
      </div>
      <div v-if="isFromClassTranscript">
        <div class="role-badge">
          <span>{{ userRoleBadge }}</span>
        </div>
      </div>
    </div>

    <!-- CONTEXT SELECTORS -->
    <section class="context-card" aria-label="Bộ chọn phạm vi xem bảng điểm">
      <div class="context-grid">
        <div class="field-item">
          <label for="select-year" class="field-label">Năm học</label>
          <Select
            id="select-year"
            v-model="selectedAcademicYearId"
            :options="academicYears"
            option-label="code"
            option-value="id"
            placeholder="Chọn năm học"
            class="context-select"
          />
        </div>

        <div v-if="activeTab === 'term'" class="field-item">
          <label for="select-semester" class="field-label">Học kỳ</label>
          <Select
            id="select-semester"
            v-model="selectedSemesterId"
            :options="semesters"
            option-label="name"
            option-value="id"
            placeholder="Chọn học kỳ"
            class="context-select"
          />
        </div>

        <div class="action-item">
          <Button
            label="Làm mới"
            icon="pi pi-refresh"
            severity="secondary"
            :loading="transcriptLoading"
            @click="loadTranscript"
          />
        </div>
      </div>
    </section>

    <!-- TAB CONTROLS -->
    <div class="tab-strip" role="tablist">
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'term' }"
        role="tab"
        :aria-selected="activeTab === 'term'"
        @click="activeTab = 'term'"
      >
        Bảng điểm Học kỳ
      </button>
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'annual' }"
        role="tab"
        :aria-selected="activeTab === 'annual'"
        @click="activeTab = 'annual'"
      >
        Bảng điểm Cả năm
      </button>
    </div>

    <!-- CALCULATION STATUS BANNERS -->
    <div v-if="currentCalculationStatus === 'IN_PROGRESS'" class="notice warning">
      <div class="notice-content">
        <strong>⚠️ Đang cập nhật:</strong>
        <span>
          Bảng điểm đang được hệ thống tính toán lại tự động do có cập nhật điểm từ giáo viên.
        </span>
      </div>
      <Button
        label="Kiểm tra trạng thái"
        icon="pi pi-sync"
        size="small"
        severity="warning"
        @click="checkCalculationStatus"
      />
    </div>

    <!-- ERROR & FORBIDDEN STATES -->
    <FormAlert v-if="errorMessage" :message="errorMessage" type="error" />

    <div v-if="isForbidden" class="error-box">
      <div class="error-icon">⛔</div>
      <h2>Từ chối truy cập (403)</h2>
      <p>Bạn không có quyền xem bảng điểm theo ngữ cảnh đã chọn.</p>
    </div>

    <div v-else-if="isNotFound" class="error-box">
      <div class="error-icon">🔍</div>
      <h2>Không tìm thấy bảng điểm (404)</h2>
      <p>Chưa có dữ liệu bảng điểm hoặc học kỳ/năm học chưa được khởi tạo.</p>
    </div>

    <!-- MAIN TRANSCRIPT DATA DISPLAY -->
    <div v-else-if="transcriptLoading" class="loading-box">
      <i class="pi pi-spin pi-spinner" style="font-size: 2rem; color: #3b82f6;" />
      <p>Đang tải bảng điểm...</p>
    </div>

    <div v-else>
      <!-- VIEW TERM -->
      <div v-if="activeTab === 'term'">
        <template v-if="termTranscript">
          <TranscriptTermTable
            :subjects="termTranscript.subjects"
            :dtbhk="termTranscript.dtbhk"
            :excused-absences="excusedAbsences"
            :unexcused-absences="unexcusedAbsences"
          />
        </template>
        <EmptyState
          v-else
          title="Chưa có dữ liệu học kỳ"
          description="Vui lòng chọn học kỳ để xem bảng điểm."
        />
      </div>

      <!-- VIEW ANNUAL -->
      <div v-else-if="activeTab === 'annual'">
        <template v-if="annualTranscript">
          <TranscriptAnnualTable
            :subjects="annualTranscript.subjects"
            :regular-dtbcn="annualTranscript.regularDtbcn"
            :final-dtbcn="annualTranscript.finalDtbcn"
          />
        </template>
        <EmptyState
          v-else
          title="Chưa có dữ liệu cả năm"
          description="Vui lòng chọn năm học để xem bảng điểm cả năm."
        />
      </div>

      <!-- METADATA FOOTER STRIP -->
      <div v-if="currentCalculationStatus" class="metadata-strip">
        <div class="meta-left">
          Trạng thái:
          <span
            class="badge"
            :class="currentCalculationStatus === 'FINISH' ? 'badge-finish' : 'badge-progress'"
          >
            {{ currentCalculationStatus === 'FINISH' ? 'FINISH — Chính thức' : 'IN_PROGRESS — Đang tính' }}
          </span>
          <span v-if="currentVersion !== null && currentVersion !== undefined" class="version-label">
            &nbsp;|&nbsp; Phiên bản: <strong>v{{ currentVersion }}</strong>
          </span>
        </div>
        <div v-if="currentCalculatedAt" class="meta-right">
          Tính gần nhất: <strong>{{ formatDateTime(currentCalculatedAt) }}</strong>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.transcript-view-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 8px 0 40px;
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  border-bottom: 1px solid #cbd5e1;
  padding-bottom: 16px;
}

.header-main {
  display: flex;
  flex-direction: column;
}

.back-action-container {
  margin-bottom: 8px;
}

.back-btn {
  font-size: 13px;
  font-weight: 600;
}

.role-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #e0f2fe;
  color: #0369a1;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 700;
  border: 1px solid #bae6fd;
  white-space: nowrap;
}

.eyebrow {
  margin: 0 0 4px;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: .06em;
}

.page-title {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 800;
  color: #0f172a;
}

.page-caption {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.context-card {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 16px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.context-grid {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 16px;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 220px;
}

.field-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.context-select {
  width: 100%;
}

.action-item {
  margin-left: auto;
}

.tab-strip {
  display: flex;
  gap: 8px;
  border-bottom: 2px solid #cbd5e1;
  margin-top: 4px;
}

.tab-btn {
  border: 0;
  background: transparent;
  color: #64748b;
  padding: 10px 20px;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  cursor: pointer;
  font-weight: 700;
  font-size: 14px;
  transition: all 0.15s;
}

.tab-btn:hover {
  color: #1e293b;
}

.tab-btn.active {
  color: #1d4ed8;
  border-bottom-color: #1d4ed8;
}

.notice {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 13px;
}

.notice.warning {
  background: #fefce8;
  border: 1px solid #fde68a;
  color: #92400e;
}

.notice-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.error-box, .loading-box {
  padding: 48px 24px;
  text-align: center;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
}

.error-icon {
  font-size: 36px;
  margin-bottom: 12px;
}

.metadata-strip {
  margin-top: 16px;
  padding: 12px 16px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  font-size: 12px;
  color: #64748b;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
}

.badge-finish {
  background: #f0fdf4;
  color: #15803d;
  border: 1px solid #bbf7d0;
}

.badge-progress {
  background: #fefce8;
  color: #b45309;
  border: 1px solid #fde68a;
}
</style>
