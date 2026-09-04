<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Select from 'primevue/select'

import ClassSubjectTranscriptTable from '@/components/ClassSubjectTranscriptTable.vue'
import ClassSummaryTranscriptTable from '@/components/ClassSummaryTranscriptTable.vue'
import EmptyState from '@/components/EmptyState.vue'
import FormAlert from '@/components/FormAlert.vue'
import { fetchAcademicYears, fetchSemesters } from '@/services/academicApi'
import { getAuthSession } from '@/services/authSession'
import {
  fetchAccessibleClasses,
  fetchClassAnnualTranscript,
  fetchClassTermTranscript,
} from '@/services/classTranscriptApi'
import type { AcademicYear, Semester } from '@/types/academic'
import { isApiError } from '@/types/api'
import type {
  AccessibleClassDTO,
  ResClassAnnualTranscriptDTO,
  ResClassTermTranscriptDTO,
  TranscriptPeriod,
  TranscriptViewMode,
} from '@/types/classTranscript'

const router = useRouter()
const route = (() => {
  try {
    return useRoute()
  } catch {
    return undefined
  }
})()
const routeQuery = computed(() => route?.query ?? {})

const activeScope = ref<TranscriptViewMode>('SUBJECT')
const activePeriod = ref<TranscriptPeriod>('TERM')

const academicYears = ref<AcademicYear[]>([])
const semesters = ref<Semester[]>([])
const accessibleClasses = ref<AccessibleClassDTO[]>([])

const selectedAcademicYearId = ref<number | null>(null)
const selectedSemesterId = ref<number | null>(null)
const selectedClassId = ref<number | null>(null)
const selectedSubjectId = ref<number | null>(null)

const classTermData = ref<ResClassTermTranscriptDTO | null>(null)
const classAnnualData = ref<ResClassAnnualTranscriptDTO | null>(null)

const contextLoading = ref(false)
const transcriptLoading = ref(false)
const errorMessage = ref('')
const isForbidden = ref(false)

const session = computed(() => getAuthSession())
const token = computed(() => session.value?.accessToken ?? '')

const userRoleBadge = computed(() => {
  const roles = session.value?.user.roles ?? []
  const username = session.value?.user.username ?? ''
  if (roles.includes('ADMIN')) return `🛡️ Quản trị viên (${username})`
  if (roles.includes('ACADEMIC_OFFICE')) return `🏛️ Giáo vụ (${username})`
  if (roles.includes('TEACHER')) return `👨‍🏫 Giáo viên (${username})`
  return `👤 ${username}`
})

// Extract available subjects from transcript data
const availableSubjects = computed(() => {
  const map = new Map<number, string>()
  if (activePeriod.value === 'TERM') {
    (classTermData.value?.students ?? []).forEach((stu) => {
      stu.subjects.forEach((s) => {
        if (!map.has(s.subjectId)) map.set(s.subjectId, s.subjectName)
      })
    })
  } else {
    (classAnnualData.value?.students ?? []).forEach((stu) => {
      stu.subjects.forEach((s) => {
        if (!map.has(s.subjectId)) map.set(s.subjectId, s.subjectName)
      })
    })
  }
  return Array.from(map.entries()).map(([id, name]) => ({ id, name }))
})

const selectedSubjectName = computed(() => {
  const found = availableSubjects.value.find((s) => s.id === selectedSubjectId.value)
  return found ? found.name : ''
})

const selectedClassName = computed(() => {
  const found = accessibleClasses.value.find((c) => c.id === selectedClassId.value)
  return found ? `${found.className} (${found.classCode})` : ''
})

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
    errorMessage.value = isApiError(err) ? err.message : 'Không thể tải danh sách năm học.'
  } finally {
    contextLoading.value = false
  }
}

async function loadYearDependencies(academicYearId: number) {
  if (!token.value) return
  contextLoading.value = true
  errorMessage.value = ''
  try {
    const [sems, classes] = await Promise.all([
      fetchSemesters(token.value, academicYearId),
      fetchAccessibleClasses(token.value, academicYearId),
    ])
    semesters.value = sems
    accessibleClasses.value = classes

    if (sems.length > 0) {
      const qSemId = routeQuery.value.semesterId ? Number(routeQuery.value.semesterId) : null
      const matchedSem = qSemId ? sems.find((s) => s.id === qSemId) : null
      const activeSem = matchedSem ?? sems.find((s) => s.status === 'ACTIVE') ?? sems[0]
      selectedSemesterId.value = activeSem.id
    } else {
      selectedSemesterId.value = null
    }

    if (classes.length > 0) {
      const qClassId = routeQuery.value.classId ? Number(routeQuery.value.classId) : null
      const matchedClass = qClassId ? classes.find((c) => c.id === qClassId) : null
      selectedClassId.value = matchedClass ? matchedClass.id : classes[0].id
    } else {
      selectedClassId.value = null
      classTermData.value = null
      classAnnualData.value = null
    }
  } catch (err: unknown) {
    errorMessage.value = isApiError(err) ? err.message : 'Không thể tải học kỳ hoặc lớp học.'
  } finally {
    contextLoading.value = false
  }
}

async function loadTranscript() {
  if (!token.value || !selectedClassId.value) {
    classTermData.value = null
    classAnnualData.value = null
    return
  }

  transcriptLoading.value = true
  errorMessage.value = ''
  isForbidden.value = false

  try {
    if (activePeriod.value === 'TERM') {
      if (!selectedSemesterId.value) {
        classTermData.value = null
        return
      }
      const data = await fetchClassTermTranscript(
        token.value,
        selectedClassId.value,
        selectedSemesterId.value,
      )
      classTermData.value = data
    } else {
      if (!selectedAcademicYearId.value) {
        classAnnualData.value = null
        return
      }
      const data = await fetchClassAnnualTranscript(
        token.value,
        selectedClassId.value,
        selectedAcademicYearId.value,
      )
      classAnnualData.value = data
    }

    // Auto-select first subject if not selected
    if (availableSubjects.value.length > 0) {
      const exists = availableSubjects.value.some((s) => s.id === selectedSubjectId.value)
      if (!exists) {
        selectedSubjectId.value = availableSubjects.value[0].id
      }
    }
  } catch (err: unknown) {
    if (activePeriod.value === 'TERM') classTermData.value = null
    else classAnnualData.value = null

    if (isApiError(err)) {
      if (err.status === 403) isForbidden.value = true
      else errorMessage.value = err.message
    } else {
      errorMessage.value = 'Không thể tải bảng điểm lớp.'
    }
  } finally {
    transcriptLoading.value = false
  }
}

function handleSelectStudent(studentId: number) {
  const studentList = activePeriod.value === 'TERM'
    ? classTermData.value?.students
    : classAnnualData.value?.students
  const found = studentList?.find((s) => s.studentId === studentId)

  router.push({
    path: '/v2/transcripts',
    query: {
      studentId: String(studentId),
      studentName: found?.fullName,
      studentCode: found?.studentCode,
      classId: selectedClassId.value ? String(selectedClassId.value) : undefined,
      academicYearId: selectedAcademicYearId.value ? String(selectedAcademicYearId.value) : undefined,
      semesterId: selectedSemesterId.value ? String(selectedSemesterId.value) : undefined,
      from: 'class-transcripts',
    },
  })
}

watch(selectedAcademicYearId, async (newYearId) => {
  if (newYearId) {
    await loadYearDependencies(newYearId)
    await loadTranscript()
  }
})

watch([selectedSemesterId, selectedClassId, activePeriod], async () => {
  await loadTranscript()
})

onMounted(async () => {
  await loadContext()
})
</script>

<template>
  <div class="class-transcript-view-container">
    <!-- VIEW HEADER -->
    <div class="view-header">
      <div>
        <p class="eyebrow">Tra cứu bảng điểm · Phân hệ Lớp học & Giáo viên chủ nhiệm</p>
        <h1 class="page-title">Bảng Điểm Lớp Học</h1>
        <p class="page-caption">
          Xem bảng điểm theo từng môn học và bảng điểm tổng kết lớp (Học kỳ & Cả năm).
        </p>
      </div>
      <div>
        <div class="role-badge">
          <span>{{ userRoleBadge }}</span>
        </div>
      </div>
    </div>

    <!-- CONTEXT BAR -->
    <section class="context-card">
      <div class="field-group">
        <label for="select-year">Năm học</label>
        <Select
          id="select-year"
          v-model="selectedAcademicYearId"
          :options="academicYears"
          option-label="code"
          option-value="id"
          placeholder="Chọn năm học"
          class="custom-select"
        />
      </div>

      <div v-if="activePeriod === 'TERM'" class="field-group">
        <label for="select-semester">Học kỳ</label>
        <Select
          id="select-semester"
          v-model="selectedSemesterId"
          :options="semesters"
          option-label="name"
          option-value="id"
          placeholder="Chọn học kỳ"
          class="custom-select"
        />
      </div>

      <div class="field-group">
        <label for="select-class">Lớp học</label>
        <Select
          id="select-class"
          v-model="selectedClassId"
          :options="accessibleClasses"
          option-label="className"
          option-value="id"
          placeholder="Chọn lớp"
          class="custom-select"
        />
      </div>

      <div v-if="activeScope === 'SUBJECT'" class="field-group">
        <label for="select-subject">Môn học</label>
        <Select
          id="select-subject"
          v-model="selectedSubjectId"
          :options="availableSubjects"
          option-label="name"
          option-value="id"
          placeholder="Chọn môn học"
          class="custom-select"
        />
      </div>

      <div class="field-group action-group">
        <Button
          label="Làm mới"
          icon="pi pi-refresh"
          severity="secondary"
          size="small"
          :loading="transcriptLoading"
          @click="loadTranscript"
        />
      </div>
    </section>

    <!-- NAVIGATION LEVEL 1: SCOPE -->
    <div class="nav-level-1">
      <button
        type="button"
        class="scope-btn"
        :class="{ active: activeScope === 'SUBJECT' }"
        @click="activeScope = 'SUBJECT'"
      >
        📖 1. BẢNG ĐIỂM THEO MÔN (Mỗi bảng là một môn)
      </button>
      <button
        type="button"
        class="scope-btn"
        :class="{ active: activeScope === 'SUMMARY' }"
        @click="activeScope = 'SUMMARY'"
      >
        📊 2. BẢNG ĐIỂM TỔNG KẾT (Tổng hợp toàn bộ các môn)
      </button>
    </div>

    <!-- NAVIGATION LEVEL 2: PERIOD -->
    <div class="nav-level-2">
      <button
        type="button"
        class="tab-item"
        :class="{ active: activePeriod === 'TERM' }"
        @click="activePeriod = 'TERM'"
      >
        📌 Bảng điểm Học kỳ
      </button>
      <button
        type="button"
        class="tab-item"
        :class="{ active: activePeriod === 'ANNUAL' }"
        @click="activePeriod = 'ANNUAL'"
      >
        🏆 Bảng điểm Cả năm
      </button>
    </div>

    <!-- ALERT ERROR -->
    <FormAlert v-if="errorMessage" :message="errorMessage" type="error" />

    <!-- FORBIDDEN ERROR -->
    <EmptyState
      v-if="isForbidden"
      title="Không có quyền truy cập"
      description="Bạn không phải là Giáo viên chủ nhiệm của lớp này hoặc chưa được phân quyền xem bảng điểm."
    />

    <!-- EMPTY CLASSES -->
    <EmptyState
      v-else-if="!contextLoading && accessibleClasses.length === 0"
      title="Chưa có lớp khả dụng"
      description="Tài khoản hiện không phụ trách lớp chủ nhiệm nào hoặc chưa có dữ liệu lớp trong năm học này."
    />

    <!-- LOADING STATE -->
    <div v-else-if="transcriptLoading" class="loading-state">
      <i class="pi pi-spin pi-spinner spinner-icon" />
      <span>Đang tải dữ liệu bảng điểm lớp...</span>
    </div>

    <!-- TABLE CONTENT -->
    <div v-else class="content-body">
      <!-- 1. BẢNG ĐIỂM THEO MÔN -->
      <ClassSubjectTranscriptTable
        v-if="activeScope === 'SUBJECT'"
        :mode="activePeriod"
        :subject-id="selectedSubjectId ?? 0"
        :subject-name="selectedSubjectName"
        :title="`Bảng điểm ${activePeriod === 'TERM' ? 'học kỳ' : 'cả năm'} - Lớp ${selectedClassName} - Môn ${selectedSubjectName}`"
        :term-data="classTermData"
        :annual-data="classAnnualData"
        @select-student="handleSelectStudent"
      />

      <!-- 2. BẢNG ĐIỂM TỔNG KẾT -->
      <ClassSummaryTranscriptTable
        v-else
        :mode="activePeriod"
        :title="`Bảng điểm tổng kết ${activePeriod === 'TERM' ? 'học kỳ' : 'cả năm'} - Lớp ${selectedClassName}`"
        :term-data="classTermData"
        :annual-data="classAnnualData"
        @select-student="handleSelectStudent"
      />
    </div>
  </div>
</template>

<style scoped>
.class-transcript-view-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border-bottom: 1px solid #cbd5e1;
  padding-bottom: 16px;
}

.eyebrow {
  margin: 0 0 4px;
  font-size: 12px;
  font-weight: 700;
  color: #1d4ed8;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.page-title {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 900;
  color: #0f172a;
}

.page-caption {
  margin: 0;
  color: #64748b;
  font-size: 14px;
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
}

.context-card {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 16px 20px;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-group label {
  font-size: 12px;
  font-weight: 700;
  color: #334155;
  text-transform: uppercase;
}

.custom-select {
  min-width: 190px;
}

.action-group {
  margin-left: auto;
}

.nav-level-1 {
  display: flex;
  gap: 12px;
  border-bottom: 2px solid #e2e8f0;
  padding-bottom: 8px;
}

.scope-btn {
  background: #ffffff;
  border: 2px solid #cbd5e1;
  border-radius: 8px;
  padding: 10px 18px;
  font-size: 14px;
  font-weight: 700;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s ease;
}

.scope-btn:hover {
  border-color: #1d4ed8;
  color: #1d4ed8;
}

.scope-btn.active {
  background: #eff6ff;
  border-color: #1d4ed8;
  color: #1d4ed8;
  box-shadow: 0 2px 4px rgba(29, 78, 216, 0.1);
}

.nav-level-2 {
  display: flex;
  gap: 10px;
}

.tab-item {
  background: none;
  border: 1px solid #cbd5e1;
  background-color: #f8fafc;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tab-item:hover {
  background-color: #e2e8f0;
  color: #1e293b;
}

.tab-item.active {
  background-color: #1d4ed8;
  border-color: #1d4ed8;
  color: #ffffff;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 48px 0;
  color: #64748b;
  font-size: 14px;
}

.spinner-icon {
  font-size: 20px;
  color: #1d4ed8;
}

.content-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>

