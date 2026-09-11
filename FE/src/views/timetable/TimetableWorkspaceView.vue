<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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
import { fetchSchoolClasses, fetchSubjects } from '@/services/academicApi'
import { lookupFunctionalRooms } from '@/services/functionalRoomApi'
import { fetchTeachers } from '@/services/teacherApi'
import {
  createTimetableRevision,
  getTimetableDetail,
  getTimetableEntries,
  getTimetablePeriods,
  getTimetableReview,
  initTimetableCalendar,
  publishTimetableRevision,
  updateTimetableEntries,
  validateTimetableRevision,
} from '@/services/timetableApi'
import { extractApiErrorMessage } from '@/types/api'
import type { SchoolClass, Subject } from '@/types/academic'
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

const route = useRoute()
const router = useRouter()
const confirm = useConfirm()
const { requireAccessToken } = useAuthSession()

const timetableId = computed(() => Number.parseInt(String(route.params.timetableId), 10))

const detail = ref<TimetableDetail | null>(null)
const periods = ref<TimetablePeriod[]>([])
const entries = ref<TimetableEntry[]>([])
const review = ref<TimetableReview | null>(null)
const classes = ref<SchoolClass[]>([])
const teachers = ref<Teacher[]>([])
const subjects = ref<Subject[]>([])
const functionalRooms = ref<FunctionalRoom[]>([])

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
const calendarInitializing = ref(false)

const canEdit = computed(() => detail.value?.capabilities.canEdit ?? false)

const conflictedEntryIds = computed(() => {
  const ids = new Set<number>()
  review.value?.issues.forEach((issue) => {
    if (issue.entryId) ids.add(issue.entryId)
  })
  return ids
})

const filteredEntries = computed(() => {
  return entries.value.filter((e) => {
    if (filterMode.value === 'CLASS' && selectedClassId.value) {
      return e.classId === selectedClassId.value
    }
    if (filterMode.value === 'TEACHER' && selectedTeacherId.value) {
      return e.teacherId === selectedTeacherId.value
    }
    if (filterMode.value === 'ROOM' && selectedRoomId.value) {
      return e.functionalRoomId === selectedRoomId.value
    }
    return true
  })
})

const assignmentOptions = computed<AssignmentOption[]>(() => {
  // Built from existing entries and available teachers/classes/subjects
  const map = new Map<number, AssignmentOption>()
  entries.value.forEach((e) => {
    if (!map.has(e.assignmentId)) {
      map.set(e.assignmentId, {
        id: e.assignmentId,
        classId: e.classId,
        className: e.className,
        subjectId: e.subjectId,
        subjectName: e.subjectName,
        teacherId: e.teacherId,
        teacherName: e.teacherName,
      })
    }
  })
  // Synthesize standard combinations from loaded classes & teachers for options
  let synthId = 1000
  classes.value.forEach((c) => {
    subjects.value.forEach((s) => {
      const existing = Array.from(map.values()).find(
        (a) => a.classId === c.id && a.subjectId === s.id,
      )
      if (!existing && teachers.value[0]) {
        map.set(synthId, {
          id: synthId,
          classId: c.id,
          className: c.className || c.classCode,
          subjectId: s.id,
          subjectName: s.name,
          teacherId: teachers.value[0].id,
          teacherName: teachers.value[0].teacherName,
        })
        synthId++
      }
    })
  })
  return Array.from(map.values())
})

async function loadInitial() {
  const token = requireAccessToken()
  if (!token || Number.isNaN(timetableId.value)) return
  loadingState.value = 'loading'
  generalError.value = ''
  try {
    const [d, cl, tc, sb, rm] = await Promise.all([
      getTimetableDetail(timetableId.value, token),
      fetchSchoolClasses(token),
      fetchTeachers(token),
      fetchSubjects(token),
      lookupFunctionalRooms(undefined, token),
    ])
    detail.value = d
    classes.value = cl
    teachers.value = tc
    subjects.value = sb
    functionalRooms.value = rm
    if (cl.length > 0) selectedClassId.value = cl[0].id

    // Load periods & entries & review for current revision
    await Promise.all([
      loadPeriods(d.semesterId),
      loadEntriesAndReview(d.revisionId),
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

async function handleInitCalendar() {
  if (!detail.value) return
  const token = requireAccessToken()
  if (!token) return
  calendarInitializing.value = true
  try {
    periods.value = await initTimetableCalendar(detail.value.semesterId, token)
  } catch (err) {
    generalError.value = extractApiErrorMessage(err, 'Không thể khởi tạo lịch chuẩn')
  } finally {
    calendarInitializing.value = false
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
  id?: number | null
  assignmentId: number
  periodId: number
  functionalRoomId?: number | null
  validFrom: string
  validTo: string
}) {
  if (!detail.value) return
  const token = requireAccessToken()
  if (!token) return
  entryDialogLoading.value = true
  entryDialogError.value = ''
  try {
    const updatedDetail = await updateTimetableEntries(
      detail.value.revisionId,
      {
        expectedVersion: detail.value.version,
        upserts: [
          {
            id: payload.id ?? null,
            assignmentId: payload.assignmentId,
            periodId: payload.periodId,
            functionalRoomId: payload.functionalRoomId,
            validFrom: payload.validFrom,
            validTo: payload.validTo,
          },
        ],
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
  if (!detail.value) return
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
  publishError.value = ''
  isPublishDialogVisible.value = true
}

async function handlePublishConfirm(payload: {
  expectedVersion: number
  expectedHeadVersion: number
  idempotencyKey: string
}) {
  if (!detail.value) return
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
  }
  activeTab.value = 'GRID'
}

onMounted(() => {
  void loadInitial()
})
</script>

<template>
  <div class="p-6 max-w-7xl mx-auto flex flex-col gap-6">
    <ConfirmDialog />

    <!-- Header bar -->
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
      <div class="flex items-center gap-3">
        <Button icon="pi pi-arrow-left" severity="secondary" rounded text @click="router.push('/v2/timetables')" />
        <div>
          <div class="flex items-center gap-2">
            <h1 class="text-2xl font-bold text-gray-900">{{ detail?.semesterName || 'Thời khóa biểu' }}</h1>
            <Tag
              v-if="detail"
              :value="`Bản ${detail.revisionNumber} · ${detail.status === 'PUBLISHED' ? 'Đã công bố' : detail.status === 'DRAFT' ? 'Bản nháp' : 'Lưu trữ'}`"
              :severity="detail.status === 'PUBLISHED' ? 'success' : detail.status === 'DRAFT' ? 'warn' : 'secondary'"
            />
          </div>
          <p class="text-xs text-gray-500">
            Hiệu lực: {{ detail?.effectiveFrom }} → {{ detail?.effectiveTo || 'Hiện tại' }}
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
          v-if="detail?.capabilities.canPublish"
          label="Công bố"
          icon="pi pi-send"
          severity="primary"
          @click="openPublishDialog"
        />
        <Button
          v-if="detail?.capabilities.canRevise"
          label="Tạo bản điều chỉnh"
          icon="pi pi-file-edit"
          severity="secondary"
          @click="handleCreateRevision"
        />
      </div>
    </div>

    <FormAlert v-if="generalError" :message="generalError" type="error" />

    <!-- Calendar init warning if zero periods exist -->
    <div
      v-if="periods.length === 0 && loadingState === 'idle'"
      class="p-4 bg-amber-50 border border-amber-200 rounded-xl flex justify-between items-center"
    >
      <div class="text-sm text-amber-900">
        ⚠️ Chưa có khung giờ chuẩn (2 buổi × 4 tiết) cho học kỳ này. Bạn cần khởi tạo khung giờ để xem và xếp lịch.
      </div>
      <Button
        label="Khởi tạo khung giờ chuẩn"
        icon="pi pi-calendar-plus"
        size="small"
        :loading="calendarInitializing"
        @click="handleInitCalendar"
      />
    </div>

    <!-- Mode tabs -->
    <div class="flex gap-2 border-b border-gray-200 pb-2">
      <button
        class="px-4 py-2 text-sm font-semibold rounded-lg transition"
        :class="[activeTab === 'GRID' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100']"
        @click="activeTab = 'GRID'"
      >
        Lịch tuần
      </button>
      <button
        class="px-4 py-2 text-sm font-semibold rounded-lg transition"
        :class="[activeTab === 'LOAD' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100']"
        @click="activeTab = 'LOAD'"
      >
        Định mức tiết dạy
      </button>
    </div>

    <PageState
      v-if="loadingState === 'loading' || loadingState === 'error'"
      :state="loadingState"
      :message="generalError"
      @retry="loadInitial"
    />

    <!-- Main Content Area -->
    <div v-else>
      <!-- TAB 1: GRID & CONFLICTS -->
      <div v-if="activeTab === 'GRID'" class="flex flex-col lg:flex-row gap-6 items-start">
        <div class="flex-1 flex flex-col gap-4 w-full">
          <!-- Filter bar -->
          <div class="bg-white p-3 rounded-xl border border-gray-200 shadow-sm flex flex-wrap gap-3 items-center">
            <span class="text-xs font-semibold text-gray-500 uppercase">Chế độ xem:</span>
            <div class="flex gap-2">
              <Button
                label="Theo Lớp"
                size="small"
                :severity="filterMode === 'CLASS' ? 'primary' : 'secondary'"
                text
                @click="filterMode = 'CLASS'"
              />
              <Button
                label="Theo Giáo viên"
                size="small"
                :severity="filterMode === 'TEACHER' ? 'primary' : 'secondary'"
                text
                @click="filterMode = 'TEACHER'"
              />
              <Button
                label="Theo Phòng"
                size="small"
                :severity="filterMode === 'ROOM' ? 'primary' : 'secondary'"
                text
                @click="filterMode = 'ROOM'"
              />
            </div>

            <div class="w-64">
              <Select
                v-if="filterMode === 'CLASS'"
                v-model="selectedClassId"
                :options="classes"
                option-label="className"
                option-value="id"
                placeholder="Chọn lớp học..."
                class="w-full"
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

          <!-- Week Grid -->
          <TimetableWeekGrid
            :periods="periods"
            :entries="filteredEntries"
            :can-edit="canEdit"
            :view-mode="filterMode"
            :conflicted-entry-ids="conflictedEntryIds"
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
