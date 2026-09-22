<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import ConfirmDialog from 'primevue/confirmdialog'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import { useConfirm } from 'primevue/useconfirm'

import FormAlert from '@/components/common/FormAlert.vue'
import PageState from '@/components/common/PageState.vue'
import TeacherLoadPanel from '@/components/timetable/TeacherLoadPanel.vue'
import TimetableConflictPanel from '@/components/timetable/TimetableConflictPanel.vue'
import TimetableEntryDialog, { type AssignmentOption } from '@/components/timetable/TimetableEntryDialog.vue'
import TimetablePublishDialog from '@/components/timetable/TimetablePublishDialog.vue'
import TimetableWeekGrid from '@/components/timetable/TimetableWeekGrid.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { fetchSchoolClasses } from '@/services/academicApi'
import { fetchSubjectAssignmentsByClass } from '@/services/assignmentApi'
import { lookupFunctionalRooms } from '@/services/functionalRoomApi'
import { fetchTeachers } from '@/services/teacherApi'
import { listTeacherUnavailabilities } from '@/services/teacherUnavailabilityApi'
import {
  createTimetableRevision,
  getTimetableDetail,
  getTimetableEntries,
  getTimetablePeriods,
  getTimetableReview,
  publishTimetableRevision,
  updateTimetableEntries,
  validateTimetableRevision,
} from '@/services/timetableApi'
import { extractApiErrorMessage } from '@/types/api'
import type { SchoolClass } from '@/types/academic'
import type { SubjectTeachingAssignment } from '@/types/assignment'
import type { FunctionalRoom } from '@/types/functionalRoom'
import type { Teacher } from '@/types/teacher'
import type {
  TimetableDetail,
  TimetableEntry,
  TimetableIssue,
  TimetablePeriod,
  TimetableReview,
} from '@/types/timetable'
import type { LoadingState } from '@/types/ui'
import type { TeacherUnavailability } from '@/types/teacherUnavailability'

const route = useRoute()
const router = useRouter()
const confirm = useConfirm()
const { roles, requireAccessToken } = useAuthSession()

const timetableId = computed(() => Number.parseInt(String(route.params.timetableId), 10))

const detail = ref<TimetableDetail | null>(null)
const periods = ref<TimetablePeriod[]>([])
const entries = ref<TimetableEntry[]>([])
const subjectAssignments = ref<SubjectTeachingAssignment[]>([])
const review = ref<TimetableReview | null>(null)
const classes = ref<SchoolClass[]>([])
const teachers = ref<Teacher[]>([])
const functionalRooms = ref<FunctionalRoom[]>([])
const teacherUnavailabilities = ref<TeacherUnavailability[]>([])

const loadingState = ref<LoadingState>('loading')
const generalError = ref('')
const activeTab = ref<'GRID' | 'LOAD'>('GRID')

// Filter mode: CLASS, TEACHER, ROOM
const filterMode = ref<'CLASS' | 'TEACHER' | 'ROOM'>('CLASS')
const selectedClassId = ref<number | null>(null)
const selectedTeacherId = ref<number | null>(null)
const selectedRoomId = ref<number | null>(null)

// Entry dialog
const isEntryDialogVisible = ref(false)
const editingEntry = ref<TimetableEntry | null>(null)
const presetPeriod = ref<TimetablePeriod | null>(null)
const entryDialogLoading = ref(false)
const entryDialogError = ref('')

// Publish dialog
const isPublishDialogVisible = ref(false)
const publishLoading = ref(false)
const publishError = ref('')

// Validating state
const validating = ref(false)

const canEdit = computed(() => {
  if (!detail.value) return false
  // Validation is a review checkpoint, not a read-only transition. A draft
  // remains editable after VALIDATED; the next mutation invalidates the old
  // review and the backend remains authoritative for publish eligibility.
  if (!['DRAFT', 'VALIDATED'].includes(detail.value.status)) return false
  const caps = detail.value.capabilities as unknown
  if (Array.isArray(caps)) {
    return caps.includes('EDIT_ENTRIES') || caps.includes('CAN_EDIT')
  }
  return detail.value.capabilities?.canEdit ?? false
})

const isOfficeRole = computed(() => roles.value.includes('ADMIN') || roles.value.includes('ACADEMIC_OFFICE'))
const canAddEntryInCurrentView = computed(() => canEdit.value && (
  (filterMode.value === 'CLASS' && selectedClassId.value !== null)
  || (filterMode.value === 'TEACHER' && selectedTeacherId.value !== null && isOfficeRole.value)
))

const canPublish = computed(() => {
  if (!detail.value) return false
  if (!['DRAFT', 'VALIDATED'].includes(detail.value.status)) return false
  const caps = detail.value.capabilities as unknown
  if (Array.isArray(caps)) {
    return caps.includes('PUBLISH') || caps.includes('CAN_PUBLISH')
  }
  return detail.value.capabilities?.canPublish ?? false
})

const canRevise = computed(() => {
  if (!detail.value) return false
  const caps = detail.value.capabilities as unknown
  if (Array.isArray(caps)) {
    return caps.includes('CREATE_REVISION') || caps.includes('CAN_REVISE')
  }
  return detail.value.capabilities?.canRevise ?? false
})

const statusLabel = computed(() => {
  switch (detail.value?.status) {
    case 'PUBLISHED': return 'Đã công bố'
    case 'ARCHIVED': return 'Lưu trữ'
    case 'DRAFT': return 'Bản nháp'
    case 'VALIDATED': return 'Đã kiểm tra'
    default: return 'Đang tải'
  }
})

const conflictedEntryIds = computed(() => {
  const ids = new Set<number>()
  review.value?.issues.forEach((issue) => {
    ;(issue.entryIds ?? (issue.entryId ? [issue.entryId] : [])).forEach((id) => ids.add(id))
  })
  return ids
})

const filteredEntries = computed(() => {
  return entries.value.filter((e) => {
    if (filterMode.value === 'CLASS') {
      return selectedClassId.value !== null && e.classId === selectedClassId.value
    }
    if (filterMode.value === 'TEACHER') {
      return selectedTeacherId.value !== null && e.teacherId === selectedTeacherId.value
    }
    if (filterMode.value === 'ROOM') {
      return selectedRoomId.value !== null && e.functionalRoomId === selectedRoomId.value
    }
    return false
  })
})

const busySlotKeys = computed(() => {
  const keys = new Set<string>()
  if (filterMode.value !== 'TEACHER' || !selectedTeacherId.value) return keys
  teacherUnavailabilities.value
    .filter((item) => item.status === 'APPROVED' && item.teacherId === selectedTeacherId.value && item.dayOfWeek)
    .forEach((item) => {
      item.periodIndexes.split(',').forEach((rawIndex) => {
        const periodIndex = Number.parseInt(rawIndex.trim(), 10)
        if (periodIndex >= 1 && periodIndex <= 4) {
          keys.add(`${item.dayOfWeek}-${item.session}-${periodIndex}`)
        }
      })
    })
  return keys
})

const assignmentOptions = computed<AssignmentOption[]>(() => {
  // Assignments must come from the teaching-assignment source, rather than
  // from existing entries: a new revision legitimately starts with no entries.
  const map = new Map<number, AssignmentOption>()
  subjectAssignments.value
    .filter((assignment) => assignment.status === 'ACTIVE'
      && (filterMode.value === 'CLASS'
        ? assignment.classId === selectedClassId.value
        : filterMode.value === 'TEACHER'
          ? assignment.teacherId === selectedTeacherId.value
          : false)
      && assignment.semesterId === detail.value?.semesterId
      && assignment.classId != null
      && assignment.subjectId != null
      && (!detail.value?.effectiveTo || assignment.validFrom <= detail.value.effectiveTo)
      && (!assignment.validTo || assignment.validTo >= detail.value.effectiveFrom))
    .forEach((assignment) => {
      if (!map.has(assignment.id)) {
        const teacher = teachers.value.find((item) => item.id === assignment.teacherId)
        map.set(assignment.id, {
          id: assignment.id,
          classId: assignment.classId!,
          className: assignment.className || assignment.classCode || `Lớp #${assignment.classId}`,
          subjectId: assignment.subjectId!,
          subjectName: assignment.subjectName || `Môn #${assignment.subjectId}`,
          teacherId: assignment.teacherId,
          teacherName: teacher?.teacherName || `Giáo viên #${assignment.teacherId}`,
        })
      }
    })
  return Array.from(map.values())
})

async function loadSubjectAssignments(semesterId: number, classId: number) {
  const token = requireAccessToken()
  if (!token) return
  subjectAssignments.value = await fetchSubjectAssignmentsByClass(token, classId, semesterId)
}

async function loadTeacherAssignments(semesterId: number, teacherId: number | null) {
  const token = requireAccessToken()
  if (!token || !teacherId) {
    subjectAssignments.value = []
    return
  }
  const assignmentLists = await Promise.all(
    classes.value.map((schoolClass) => fetchSubjectAssignmentsByClass(token, schoolClass.id, semesterId)),
  )
  subjectAssignments.value = assignmentLists.flat().filter((assignment) => assignment.teacherId === teacherId)
}

async function loadTeacherUnavailabilities(teacherId: number | null) {
  const token = requireAccessToken()
  if (!token || !detail.value || !teacherId) {
    teacherUnavailabilities.value = []
    return
  }
  try {
    teacherUnavailabilities.value = await listTeacherUnavailabilities(
      {
        semesterId: detail.value.semesterId,
        teacherId,
        status: 'APPROVED',
        from: detail.value.effectiveFrom,
        to: detail.value.effectiveTo ?? undefined,
      },
      token,
    )
  } catch (err) {
    teacherUnavailabilities.value = []
    generalError.value = extractApiErrorMessage(err, 'Không thể tải lịch bận của giáo viên')
  }
}

async function loadApprovedTeacherUnavailabilities() {
  const token = requireAccessToken()
  if (!token || !detail.value) return
  try {
    teacherUnavailabilities.value = await listTeacherUnavailabilities(
      {
        semesterId: detail.value.semesterId,
        status: 'APPROVED',
        from: detail.value.effectiveFrom,
        to: detail.value.effectiveTo ?? undefined,
      },
      token,
    )
  } catch (err) {
    teacherUnavailabilities.value = []
    generalError.value = extractApiErrorMessage(err, 'Không thể tải lịch bận của giáo viên')
  }
}

async function handleClassChange(classId: number | null) {
  selectedClassId.value = classId
  subjectAssignments.value = []
  if (!classId || !detail.value) return
  try {
    await loadSubjectAssignments(detail.value.semesterId, classId)
  } catch (err) {
    generalError.value = extractApiErrorMessage(err, 'Không thể tải phân công giảng dạy của lớp')
  }
}

function handleFilterModeChange(mode: 'CLASS' | 'TEACHER' | 'ROOM') {
  filterMode.value = mode
  if (mode === 'CLASS') {
    if (selectedClassId.value === null) selectedClassId.value = classes.value[0]?.id ?? null
    if (selectedClassId.value !== null && detail.value) {
      subjectAssignments.value = []
      void loadSubjectAssignments(detail.value.semesterId, selectedClassId.value)
    }
  } else if (mode === 'TEACHER') {
    if (selectedTeacherId.value === null) selectedTeacherId.value = teachers.value[0]?.id ?? null
    if (selectedTeacherId.value !== null && detail.value) {
      void loadTeacherAssignments(detail.value.semesterId, selectedTeacherId.value)
    }
  } else if (selectedRoomId.value === null) {
    selectedRoomId.value = functionalRooms.value[0]?.id ?? null
  }
}

watch(selectedTeacherId, (teacherId) => {
  if (filterMode.value === 'TEACHER') {
    void loadTeacherUnavailabilities(teacherId)
    if (isOfficeRole.value && detail.value) void loadTeacherAssignments(detail.value.semesterId, teacherId)
  }
})

watch(filterMode, (mode) => {
  if (mode === 'TEACHER') void loadTeacherUnavailabilities(selectedTeacherId.value)
  else teacherUnavailabilities.value = []
})

async function loadInitial() {
  const token = requireAccessToken()
  if (!token || Number.isNaN(timetableId.value)) return
  loadingState.value = 'loading'
  generalError.value = ''
  try {
    const [d, cl, tc, rm] = await Promise.all([
      getTimetableDetail(timetableId.value, token),
      fetchSchoolClasses(token),
      fetchTeachers(token),
      lookupFunctionalRooms(undefined, token),
    ])
    detail.value = d
    classes.value = cl
    teachers.value = tc
    functionalRooms.value = rm
    if (cl.length > 0) selectedClassId.value = cl[0].id

    // Load assignments independently from entries so the first entry can be added.
    await Promise.all([
      loadPeriods(d.semesterId),
      loadEntriesAndReview(d.revisionId),
      loadApprovedTeacherUnavailabilities(),
      selectedClassId.value ? loadSubjectAssignments(d.semesterId, selectedClassId.value) : Promise.resolve(),
    ])
    loadingState.value = 'idle'
  } catch (err) {
    loadingState.value = 'error'
    generalError.value = extractApiErrorMessage(err, 'Không thể tải chi tiết thời khóa biểu')
  }
}

async function loadPeriods(semesterId: number) {
  const token = requireAccessToken()
  if (!token) return
  try {
    periods.value = await getTimetablePeriods(semesterId, token)
  } catch {
    periods.value = []
  }
}

async function loadEntriesAndReview(revisionId: number) {
  const token = requireAccessToken()
  if (!token) return
  try {
    const [entryList, reviewData] = await Promise.all([
      getTimetableEntries(revisionId, undefined, token),
      getTimetableReview(revisionId, token),
    ])
    entries.value = entryList
    review.value = reviewData
  } catch (err) {
    generalError.value = extractApiErrorMessage(err, 'Lỗi khi tải tiết học hoặc kết quả kiểm tra')
  }
}

async function handleValidate() {
  if (!detail.value) return
  const token = requireAccessToken()
  if (!token) return
  validating.value = true
  generalError.value = ''
  try {
    const res = await validateTimetableRevision(detail.value.revisionId, token)
    review.value = res
    // Reload detail to refresh blocking/warning counts and status
    detail.value = await getTimetableDetail(timetableId.value, token)
  } catch (err) {
    generalError.value = extractApiErrorMessage(err, 'Không thể kiểm tra thời khóa biểu')
  } finally {
    validating.value = false
  }
}

function openAddEntryDialog(period: TimetablePeriod) {
  if (!canAddEntryInCurrentView.value) return
  presetPeriod.value = period
  editingEntry.value = null
  entryDialogError.value = ''
  isEntryDialogVisible.value = true
}

function openEditEntryDialog(entry: TimetableEntry) {
  presetPeriod.value = null
  editingEntry.value = entry
  entryDialogError.value = ''
  isEntryDialogVisible.value = true
}

async function handleSaveEntry(payload: {
  entryId?: number | null
  assignmentId: number
  periodIds: number[]
  functionalRoomId?: number | null
  validFrom: string
  validTo: string
}) {
  if (!detail.value || !canEdit.value) return
  const token = requireAccessToken()
  if (!token) return
  entryDialogLoading.value = true
  entryDialogError.value = ''
  try {
    const updatedDetail = await updateTimetableEntries(
      detail.value.revisionId,
      {
        expectedVersion: detail.value.version,
        upserts: payload.periodIds.map((periodId) => ({
          entryId: payload.entryId ?? null,
          assignmentId: payload.assignmentId,
          periodId,
          functionalRoomId: payload.functionalRoomId,
          validFrom: payload.validFrom,
          validTo: payload.validTo,
        })),
        deletedEntryIds: [],
      },
      token,
    )
    detail.value = updatedDetail
    isEntryDialogVisible.value = false
    await loadEntriesAndReview(detail.value.revisionId)
  } catch (err) {
    entryDialogError.value = extractApiErrorMessage(err, 'Không thể lưu tiết học')
  } finally {
    entryDialogLoading.value = false
  }
}

async function handleDeleteEntry(entryId: number) {
  if (!detail.value || !canEdit.value) return
  const token = requireAccessToken()
  if (!token) return
  try {
    const updatedDetail = await updateTimetableEntries(
      detail.value.revisionId,
      {
        expectedVersion: detail.value.version,
        upserts: [],
        deletedEntryIds: [entryId],
      },
      token,
    )
    detail.value = updatedDetail
    isEntryDialogVisible.value = false
    await loadEntriesAndReview(detail.value.revisionId)
  } catch (err) {
    entryDialogError.value = extractApiErrorMessage(err, 'Không thể xóa tiết học')
  }
}

function openPublishDialog() {
  if (!canPublish.value) return
  publishError.value = ''
  isPublishDialogVisible.value = true
}

async function handlePublishConfirm(payload: {
  expectedVersion: number
  expectedHeadVersion: number
  idempotencyKey: string
}) {
  if (!detail.value || !canPublish.value) return
  const token = requireAccessToken()
  if (!token) return
  publishLoading.value = true
  publishError.value = ''
  try {
    const updated = await publishTimetableRevision(
      detail.value.revisionId,
      {
        expectedVersion: payload.expectedVersion,
        expectedHeadVersion: payload.expectedHeadVersion,
      },
      payload.idempotencyKey,
      token,
    )
    detail.value = updated
    isPublishDialogVisible.value = false
    await loadEntriesAndReview(detail.value.revisionId)
  } catch (err) {
    publishError.value = extractApiErrorMessage(err, 'Không thể công bố thời khóa biểu')
  } finally {
    publishLoading.value = false
  }
}

function handleCreateRevision() {
  if (!detail.value) return
  confirm.require({
    message: 'Tạo bản điều chỉnh mới dựa trên thời khóa biểu hiện tại?',
    header: 'Tạo bản điều chỉnh',
    icon: 'pi pi-info-circle',
    acceptLabel: 'Tạo bản mới',
    rejectLabel: 'Hủy',
    accept: async () => {
      const token = requireAccessToken()
      if (!token || !detail.value) return
      try {
        const newRev = await createTimetableRevision(
          detail.value.revisionId,
          {
            expectedVersion: detail.value.version,
            effectiveFrom: new Date().toISOString().split('T')[0],
          },
          token,
        )
        detail.value = newRev
        await loadEntriesAndReview(newRev.revisionId)
      } catch (err) {
        generalError.value = extractApiErrorMessage(err, 'Không thể tạo bản điều chỉnh mới')
      }
    },
  })
}

function focusIssueEntry(issue: TimetableIssue) {
  if (issue.classId) {
    filterMode.value = 'CLASS'
    selectedClassId.value = issue.classId
  } else if (issue.teacherId) {
    filterMode.value = 'TEACHER'
    selectedTeacherId.value = issue.teacherId
  } else if (issue.functionalRoomId) {
    filterMode.value = 'ROOM'
    selectedRoomId.value = issue.functionalRoomId
  }
  activeTab.value = 'GRID'
}

onMounted(() => {
  void loadInitial()
})
</script>

<template>
  <div class="timetable-page p-6 max-w-7xl mx-auto flex flex-col gap-6">
    <ConfirmDialog />

    <!-- Header bar -->
    <div class="flex flex-col xl:flex-row justify-between items-start xl:items-center gap-4">
      <div class="flex items-center gap-3">
        <Button icon="pi pi-arrow-left" severity="secondary" rounded text @click="router.push('/v2/timetables')" />
        <div>
          <div class="flex items-center gap-2">
            <h1 class="text-2xl font-bold text-gray-900">Thời khóa biểu</h1>
            <Tag
              v-if="detail"
              :value="`Bản ${detail.revisionNumber} · ${statusLabel}`"
              :severity="detail.status === 'PUBLISHED' ? 'success' : detail.status === 'DRAFT' ? 'warn' : 'secondary'"
            />
          </div>
          <p class="text-xs text-gray-500">
            {{ detail?.semesterName }} · Hiệu lực: {{ detail?.effectiveFrom }} → {{ detail?.effectiveTo || 'Hiện tại' }}
          </p>
        </div>
      </div>

      <div class="flex flex-wrap gap-2 items-center">
        <Button
          v-if="canEdit"
          label="Kiểm tra lịch"
          icon="pi pi-check"
          severity="secondary"
          :loading="validating"
          @click="handleValidate"
        />
        <Button
          v-if="canPublish"
          label="Công bố"
          icon="pi pi-send"
          severity="primary"
          @click="openPublishDialog"
        />
        <Button
          v-if="canRevise"
          label="Tạo bản điều chỉnh"
          icon="pi pi-file-edit"
          severity="secondary"
          @click="handleCreateRevision"
        />
      </div>
    </div>

    <FormAlert v-if="generalError" :message="generalError" tone="error" />

    <!-- Calendar init warning if zero periods exist -->
    <div
      v-if="periods.length === 0 && loadingState === 'idle'"
      class="p-4 bg-amber-50 border border-amber-200 rounded-xl flex justify-between items-center"
    >
      <div class="text-sm text-amber-900">
        ⚠️ Chưa có khung giờ chuẩn (2 buổi × 4 tiết) cho học kỳ này. Hãy mở cấu hình lịch học kỳ để được hướng dẫn cập nhật.
      </div>
      <Button
        label="Mở hướng dẫn cấu hình"
        icon="pi pi-cog"
        size="small"
        @click="router.push('/v2/timetables/settings')"
      />
    </div>

    <!-- Mode tabs -->
    <div class="timetable-subtab-strip" role="tablist" aria-label="Nội dung thời khóa biểu">
      <button
        class="timetable-subtab"
        :class="{ 'is-active': activeTab === 'GRID' }"
        role="tab"
        :aria-selected="activeTab === 'GRID'"
        @click="activeTab = 'GRID'"
      >
        Lịch tuần
      </button>
      <button
        class="timetable-subtab"
        :class="{ 'is-active': activeTab === 'LOAD' }"
        role="tab"
        :aria-selected="activeTab === 'LOAD'"
        @click="activeTab = 'LOAD'"
      >
        Định mức tiết dạy
      </button>
    </div>

    <PageState
      v-if="loadingState === 'loading' || loadingState === 'error'"
      :state="loadingState"
      :error-message="generalError"
      @retry="loadInitial"
    />

    <!-- Main Content Area -->
    <div v-else>
      <!-- TAB 1: GRID & CONFLICTS -->
      <div v-if="activeTab === 'GRID'" class="flex flex-col lg:flex-row gap-6 items-start">
        <div class="flex-1 flex flex-col gap-4 w-full">
          <!-- Filter bar -->
          <div class="bg-white p-3 rounded-xl border border-gray-200 shadow-sm flex flex-wrap gap-3 items-center" aria-label="Bộ lọc lịch tuần">
            <span class="text-xs font-semibold text-gray-500 uppercase">Xem theo</span>
            <div class="flex gap-2">
              <Button
                label="Lớp"
                size="small"
                :severity="filterMode === 'CLASS' ? 'primary' : 'secondary'"
                text
                @click="handleFilterModeChange('CLASS')"
              />
              <Button
                label="Giáo viên"
                size="small"
                :severity="filterMode === 'TEACHER' ? 'primary' : 'secondary'"
                text
                @click="handleFilterModeChange('TEACHER')"
              />
              <Button
                label="Phòng chức năng"
                size="small"
                :severity="filterMode === 'ROOM' ? 'primary' : 'secondary'"
                text
                @click="handleFilterModeChange('ROOM')"
              />
            </div>

            <div class="w-64">
              <Select
                v-if="filterMode === 'CLASS'"
                :model-value="selectedClassId"
                :options="classes"
                option-label="className"
                option-value="id"
                placeholder="Chọn lớp học..."
                class="w-full"
                @update:model-value="handleClassChange"
              />
              <Select
                v-else-if="filterMode === 'TEACHER'"
                v-model="selectedTeacherId"
                :options="teachers"
                option-label="teacherName"
                option-value="id"
                placeholder="Chọn giáo viên..."
                class="w-full"
              />
              <Select
                v-else-if="filterMode === 'ROOM'"
                v-model="selectedRoomId"
                :options="functionalRooms"
                option-label="name"
                option-value="id"
                placeholder="Chọn phòng chức năng..."
                class="w-full"
              />
            </div>
          </div>

          <p v-if="filterMode !== 'CLASS' && !(filterMode === 'TEACHER' && isOfficeRole)" class="text-sm text-slate-600">
            {{ filterMode === 'TEACHER' ? 'Chỉ ADMIN/ACADEMIC_OFFICE được thêm tiết trong chế độ xem theo giáo viên.' : 'Chuyển sang xem theo lớp để thêm tiết học.' }}
          </p>

          <!-- Week Grid -->
          <TimetableWeekGrid
            :periods="periods"
            :entries="filteredEntries"
            :can-edit="canAddEntryInCurrentView"
            :view-mode="filterMode"
            :conflicted-entry-ids="conflictedEntryIds"
            :busy-slot-keys="busySlotKeys"
            @add-entry="openAddEntryDialog"
            @edit-entry="openEditEntryDialog"
          />
        </div>

        <!-- Sidebar Conflict Panel -->
        <div class="w-full lg:w-96">
          <TimetableConflictPanel
            :issues="review?.issues || []"
            :blocking-count="review?.blockingCount || 0"
            :warning-count="review?.warningCount || 0"
            :loading="validating"
            @focus-entry="focusIssueEntry"
            @revalidate="handleValidate"
          />
        </div>
      </div>

      <!-- TAB 2: TEACHER LOAD -->
      <div v-else-if="activeTab === 'LOAD'">
        <TeacherLoadPanel :teacher-loads="review?.teacherLoads || []" />
      </div>
    </div>

    <!-- Entry Dialog -->
    <TimetableEntryDialog
      v-model:visible="isEntryDialogVisible"
      :entry="editingEntry"
      :preset-period="presetPeriod"
      :periods="periods"
      :entries="entries"
      :teacher-unavailabilities="teacherUnavailabilities"
      :assignments="assignmentOptions"
      :default-valid-from="detail?.effectiveFrom"
      :default-valid-to="detail?.effectiveTo ?? undefined"
      :loading="entryDialogLoading"
      :error-message="entryDialogError"
      @save="handleSaveEntry"
      @delete="handleDeleteEntry"
    />

    <!-- Publish Dialog -->
    <TimetablePublishDialog
      v-model:visible="isPublishDialogVisible"
      :timetable="detail"
      :blocking-count="review?.blockingCount || 0"
      :warning-count="review?.warningCount || 0"
      :loading="publishLoading"
      :error-message="publishError"
      @publish="handlePublishConfirm"
    />
  </div>
</template>
