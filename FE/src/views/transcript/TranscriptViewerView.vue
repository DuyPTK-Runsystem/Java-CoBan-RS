<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Select from 'primevue/select'

import EmptyState from '@/components/common/EmptyState.vue'
import FormAlert from '@/components/common/FormAlert.vue'
import TranscriptAnnualTable from '@/components/transcript/TranscriptAnnualTable.vue'
import TranscriptTermTable from '@/components/transcript/TranscriptTermTable.vue'
import { useTranscriptContext } from '@/composables/useTranscriptContext'
import { useTranscriptTabState } from '@/composables/useTranscriptTabState'
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

const { activeTab, selectTab } = useTranscriptTabState('term')
const termTranscript = ref<ResStudentTermTranscriptDTO | null>(null)
const annualTranscript = ref<ResStudentAnnualTranscriptDTO | null>(null)
const excusedAbsences = ref<number | null>(null)
const unexcusedAbsences = ref<number | null>(null)

const transcriptLoading = ref(false)
const isForbidden = ref(false)
const isNotFound = ref(false)

const session = computed(() => getAuthSession())
const token = computed(() => session.value?.accessToken ?? '')
const {
  academicYears,
  semesters,
  selectedAcademicYearId,
  selectedSemesterId,
  errorMessage,
  loadContext,
  loadSemesters,
} = useTranscriptContext(token, routeQuery)

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
  if (roles.includes('ADMIN')) return `Quản trị viên (${username})`
  if (roles.includes('ACADEMIC_OFFICE')) return `Giáo vụ (${username})`
  if (roles.includes('TEACHER')) return `Giáo viên (${username})`
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
    if (!selectedSemesterId.value) {
      termTranscript.value = null
      excusedAbsences.value = null
      unexcusedAbsences.value = null
    }
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
        <h1 class="page-title">
          {{ studentDisplayName ? `Bảng Điểm Học Sinh: ${studentDisplayName}` : 'Bảng Điểm Học Sinh' }}
        </h1>
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
      <Button
        label="Bảng điểm Học kỳ"
        :severity="activeTab === 'term' ? 'primary' : 'secondary'"
        :outlined="activeTab !== 'term'"
        class="tab-btn"
        role="tab"
        :aria-selected="activeTab === 'term'"
        @click="selectTab('term')"
      />
      <Button
        label="Bảng điểm Cả năm"
        :severity="activeTab === 'annual' ? 'primary' : 'secondary'"
        :outlined="activeTab !== 'annual'"
        class="tab-btn"
        role="tab"
        :aria-selected="activeTab === 'annual'"
        @click="selectTab('annual')"
      />
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

<style scoped src="@/styles/transcript.css"></style>
