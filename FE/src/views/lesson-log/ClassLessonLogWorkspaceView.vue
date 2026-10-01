<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Button from 'primevue/button'
import Select from 'primevue/select'
import DatePicker from 'primevue/datepicker'

import LessonLogWeeklyMatrix from '@/components/lesson-log/LessonLogWeeklyMatrix.vue'
import LessonLogEntryDialog from '@/components/lesson-log/LessonLogEntryDialog.vue'
import LessonLogStatusBadge from '@/components/lesson-log/LessonLogStatusBadge.vue'
import LessonLogDetailDialog from '@/components/lesson-log/LessonLogDetailDialog.vue'
import LessonLogAmendDialog from '@/components/lesson-log/LessonLogAmendDialog.vue'
import LessonLogAuditDrawer from '@/components/lesson-log/LessonLogAuditDrawer.vue'
import WeeklyHomeroomReviewDialog from '@/components/lesson-log/WeeklyHomeroomReviewDialog.vue'
import LessonLogSubtabs from '@/components/lesson-log/LessonLogSubtabs.vue'
import FormAlert from '@/components/common/FormAlert.vue'
import EmptyState from '@/components/common/EmptyState.vue'

import {
  amendLessonLog,
  createLessonLog,
  getClassWeeklyLessonLogs,
  listLessonLogClasses,
  recordLateLessonLog,
  reviewLessonLog,
  signWeeklyReview,
  submitLessonLog,
  updateLessonLog,
} from '@/services/lessonLogApi'
import { fetchAcademicYears, fetchSemesters } from '@/services/academicApi'
import { useAuthSession } from '@/composables/useAuthSession'
import { extractApiErrorMessage, isApiError } from '@/types/api'
import type {
  ClassWeekItem,
  ClassWeeklyLessonLogResponse,
  LessonGrade,
  LessonLogEntry,
  LessonLogRequestFields,
} from '@/types/lessonLog'
import type { Semester } from '@/types/academic'

const { requireAccessToken, roles } = useAuthSession()

const semesterId = ref<number | null>(null)
const semesters = ref<Semester[]>([])
const classId = ref<number | null>(null)
const weekStart = ref(new Date())
const classes = ref<Array<{ id: number; name: string }>>([])
const data = ref<ClassWeeklyLessonLogResponse | null>(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const statusFilter = ref<'ALL' | 'UNLOGGED' | LessonLogEntry['status']>('ALL')

const detailVisible = ref(false)
const entryDialogVisible = ref(false)
const lateRecordVisible = ref(false)
const amendVisible = ref(false)
const auditVisible = ref(false)
const reviewVisible = ref(false)
const selected = ref<LessonLogEntry | null>(null)
const editingNew = ref(false)

const canManage = computed(() => Boolean(selected.value?.canReview || selected.value?.canAmend))
const canShowPolicy = computed(() =>
  roles.value.some((role) => role === 'ADMIN' || role === 'ACADEMIC_OFFICE'),
)

function iso(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function monday(d: Date): Date {
  const x = new Date(d)
  const day = x.getDay() || 7
  x.setDate(x.getDate() - day + 1)
  return x
}

async function loadClasses(): Promise<void> {
  const token = requireAccessToken()
  if (!token || semesterId.value === null) return
  try {
    classes.value = await listLessonLogClasses(semesterId.value, token)
    if (classes.value.length === 0) {
      classId.value = null
      data.value = null
      return
    }
    if (!classId.value || !classes.value.some((item) => item.id === classId.value)) {
      classId.value = classes.value[0].id
    }
    await load()
  } catch (e) {
    error.value = extractApiErrorMessage(e, 'Không thể tải danh sách lớp')
  }
}

async function load(): Promise<void> {
  const token = requireAccessToken()
  if (!token || !classId.value || semesterId.value === null) return
  loading.value = true
  error.value = ''
  try {
    data.value = await getClassWeeklyLessonLogs(
      classId.value,
      semesterId.value,
      iso(monday(weekStart.value)),
      token,
    )
  } catch (e) {
    error.value = extractApiErrorMessage(e, 'Không thể tải sổ tuần của lớp')
  } finally {
    loading.value = false
  }
}

async function loadAcademicContext(): Promise<void> {
  const token = requireAccessToken()
  if (!token) return
  try {
    const years = await fetchAcademicYears(token)
    const yearId = years.find((item) => item.status === 'ACTIVE')?.id ?? years[0]?.id
    if (yearId === undefined) return
    semesters.value = await fetchSemesters(token, yearId)
    semesterId.value =
      semesters.value.find((item) => item.status === 'ACTIVE')?.id ?? semesters.value[0]?.id ?? null
    if (semesterId.value !== null) await loadClasses()
  } catch (e) {
    error.value = extractApiErrorMessage(e, 'Không thể tải context học kỳ')
  }
}

async function onSemesterChange(): Promise<void> {
  classId.value = null
  data.value = null
  selected.value = null
  statusFilter.value = 'ALL'
  detailVisible.value = false
  entryDialogVisible.value = false
  lateRecordVisible.value = false
  amendVisible.value = false
  auditVisible.value = false
  reviewVisible.value = false
  error.value = ''
  await loadClasses()
}

function selectEntry(item: ClassWeekItem): void {
  selected.value = item
  if (item.status === 'UNLOGGED') {
    editingNew.value = item.entryId <= 0
    if (item.canLateRecord) {
      lateRecordVisible.value = true
      return
    }
    entryDialogVisible.value = true
    return
  }
  editingNew.value = false
  detailVisible.value = true
}

function showAudit(entry: LessonLogEntry): void {
  selected.value = entry
  detailVisible.value = false
  auditVisible.value = true
}

async function amend(fields: LessonLogRequestFields, reason: string): Promise<void> {
  const token = requireAccessToken()
  if (!token || !selected.value) return
  saving.value = true
  error.value = ''
  try {
    await amendLessonLog(
      selected.value.entryId,
      { expectedVersion: selected.value.version, reason, ...fields },
      token,
    )
    notice.value = 'Đã điều chỉnh sổ và lưu lý do vào lịch sử.'
    amendVisible.value = false
    await load()
  } catch (e) {
    error.value = isApiError(e, 409)
      ? 'Bản ghi đã thay đổi. Hãy mở lại chi tiết trước khi điều chỉnh.'
      : extractApiErrorMessage(e, 'Không thể điều chỉnh sổ')
  } finally {
    saving.value = false
  }
}

async function lateRecord(fields: LessonLogRequestFields, reason: string): Promise<void> {
  const token = requireAccessToken()
  if (!token || !selected.value || !selected.value.canLateRecord) return
  saving.value = true
  error.value = ''
  try {
    await recordLateLessonLog(
      {
        timetableEntryId: selected.value.timetableEntryId,
        lessonDate: selected.value.lessonDate,
        reason,
        ...fields,
      },
      token,
    )
    notice.value = 'Đã ghi bổ sung sổ đầu bài và lưu lý do vào lịch sử.'
    lateRecordVisible.value = false
    await load()
  } catch (e) {
    error.value = isApiError(e, 409)
      ? 'Tiết đã được ghi bởi người khác. Hãy tải lại sổ tuần.'
      : extractApiErrorMessage(e, 'Không thể ghi bổ sung sổ đầu bài')
  } finally {
    saving.value = false
  }
}

async function review(): Promise<void> {
  const token = requireAccessToken()
  if (!token || !selected.value) return
  saving.value = true
  error.value = ''
  try {
    await reviewLessonLog(
      selected.value.entryId,
      selected.value.version,
      'Duyệt sổ đầu bài',
      undefined,
      token,
    )
    notice.value = 'Đã duyệt sổ đầu bài.'
    detailVisible.value = false
    await load()
  } catch (e) {
    error.value = isApiError(e, 409)
      ? 'Bản ghi đã thay đổi. Nội dung đang chọn được giữ nguyên; hãy tải lại để duyệt phiên bản mới.'
      : extractApiErrorMessage(e, 'Không thể duyệt sổ')
  } finally {
    saving.value = false
  }
}

async function sign(comment: string, grade: LessonGrade | null, reason: string): Promise<void> {
  const token = requireAccessToken()
  if (!token || !classId.value || !data.value?.weeklyReview) return
  try {
    await signWeeklyReview(
      classId.value,
      {
        semesterId: semesterId.value,
        weekStart: data.value.weekStart,
        expectedVersion: data.value.weeklyReview.version,
        expectedEntries: data.value.weeklyReview.expectedEntries,
        weeklyComment: comment || undefined,
        weeklyGrade: grade ?? undefined,
        reason: reason || undefined,
      },
      token,
    )
    notice.value = 'Đã ký tổng kết tuần.'
    await load()
  } catch (e) {
    error.value = isApiError(e, 409)
      ? 'Tuần đã thay đổi. Bản nháp vẫn được giữ; hãy tải lại để ký theo phiên bản mới.'
      : extractApiErrorMessage(e, 'Không thể ký tổng kết tuần')
  }
}

async function saveDraft(fields: LessonLogRequestFields): Promise<boolean> {
  const token = requireAccessToken()
  if (!token || !selected.value) return false
  saving.value = true
  error.value = ''
  try {
    if (editingNew.value) {
      selected.value = await createLessonLog(
        {
          timetableEntryId: selected.value.timetableEntryId,
          lessonDate: selected.value.lessonDate,
          ...fields,
        },
        token,
      )
      editingNew.value = false
    } else {
      selected.value = await updateLessonLog(
        selected.value.entryId,
        { expectedVersion: selected.value.version, ...fields },
        token,
      )
    }
    notice.value = 'Đã lưu nháp.'
    await load()
    return true
  } catch (e) {
    error.value = isApiError(e, 409)
      ? 'Dữ liệu đã thay đổi. Hãy tải lại sổ tuần trước khi ghi tiếp.'
      : extractApiErrorMessage(e, 'Không thể lưu sổ đầu bài')
    return false
  } finally {
    saving.value = false
  }
}

async function submit(fields: LessonLogRequestFields): Promise<void> {
  const saved = await saveDraft(fields)
  if (!saved || !selected.value?.entryId) return
  const token = requireAccessToken()
  if (!token) return
  saving.value = true
  try {
    selected.value = await submitLessonLog(
      selected.value.entryId,
      selected.value.version,
      token,
    )
    notice.value = 'Đã nộp sổ đầu bài.'
    await load()
  } catch (e) {
    error.value = isApiError(e, 409)
      ? 'Đã lưu nội dung nhưng phiên bản đã thay đổi; sổ chưa được nộp.'
      : `Đã lưu nháp, chưa nộp. ${extractApiErrorMessage(e, 'Vui lòng kiểm tra lại')}`
  } finally {
    saving.value = false
  }
}

const filteredEntries = computed(() => {
  if (!data.value || statusFilter.value === 'ALL') return data.value?.items ?? []
  return data.value.items.filter((entry) => entry.status === statusFilter.value)
})

onMounted(() => void loadAcademicContext())
</script>

<template>
  <div class="page-heading lesson-log-page-heading">
    <div>
      <h1>Sổ đầu bài</h1>
      <p class="section-caption">Theo dõi và quản lý sổ đầu bài các lớp theo tuần học</p>
    </div>
    <div class="page-heading-actions">
      <Button
        label="Làm mới"
        icon="pi pi-refresh"
        severity="secondary"
        outlined
        :loading="loading"
        @click="load"
      />
    </div>
  </div>

  <LessonLogSubtabs :show-policy="canShowPolicy" />

  <FormAlert v-if="error" tone="error" :message="error" />
  <FormAlert v-if="notice" tone="success" :message="notice" />

  <!-- Dedicated Context Filter Panel -->
  <section class="content-surface lesson-log-context-panel" aria-label="Bộ lọc sổ đầu bài">
    <div class="lesson-log-context-form">
      <div class="field-group">
        <label for="semester-select">Học kỳ</label>
        <Select
          id="semester-select"
          v-model="semesterId"
          :options="semesters"
          option-label="name"
          option-value="id"
          placeholder="Chọn học kỳ"
          fluid
          @change="onSemesterChange"
        />
      </div>

      <div class="field-group">
        <label for="class-select">Lớp học</label>
        <Select
          id="class-select"
          v-model="classId"
          :options="classes"
          option-label="name"
          option-value="id"
          placeholder="Chọn lớp"
          :disabled="semesterId === null"
          fluid
          @change="load"
        />
      </div>

      <div class="field-group">
        <label for="week-picker">Tuần học (Thứ Hai)</label>
        <DatePicker
          id="week-picker"
          v-model="weekStart"
          date-format="dd/mm/yy"
          show-icon
          :disabled="semesterId === null || classId === null"
          fluid
          @update:model-value="load"
        />
      </div>
    </div>
  </section>

  <!-- Loading State -->
  <div v-if="loading" class="page-state page-state-loading" role="status">
    <i class="pi pi-spin pi-spinner" aria-hidden="true" />
    <span>Đang tải sổ tuần…</span>
  </div>

  <!-- Loaded Data Section -->
  <template v-else-if="data">
    <!-- KPI Summary Metrics -->
    <div class="lesson-log-summary-grid">
      <div class="lesson-log-summary-metric">
        <span class="metric-label">Tiết dự kiến</span>
        <strong class="metric-value">{{ data.summary.scheduled }}</strong>
      </div>
      <div
        class="lesson-log-summary-metric"
        :class="{ 'is-warning': data.summary.unlogged > 0 }"
      >
        <span class="metric-label">Chưa ghi</span>
        <strong class="metric-value">{{ data.summary.unlogged }}</strong>
      </div>
      <div class="lesson-log-summary-metric">
        <span class="metric-label">Bản nháp</span>
        <strong class="metric-value">{{ data.summary.draft }}</strong>
      </div>
      <div class="lesson-log-summary-metric is-success">
        <span class="metric-label">Đã nộp trở lên</span>
        <strong class="metric-value">
          {{ data.summary.submitted + data.summary.reviewed + data.summary.amended }}
        </strong>
      </div>
    </div>

    <!-- Weekly Review Bar -->
    <div v-if="data.weeklyReview" class="lesson-log-review-bar">
      <div class="lesson-log-review-info">
        <div class="lesson-log-review-meta">
          <LessonLogStatusBadge :status="data.weeklyReview.status" />
          <span v-if="data.weeklyReview.signedBy">
            Đã ký bởi: <strong>{{ data.weeklyReview.signedBy }}</strong> ·
            {{ data.weeklyReview.signedAt }}
          </span>
          <span v-else>Tuần chưa được ký tổng kết</span>
        </div>
        <ul v-if="data.weeklyReview.blockedReasons.length" class="lesson-log-review-blocked">
          <li v-for="reason in data.weeklyReview.blockedReasons" :key="reason">
            {{ reason }}
          </li>
        </ul>
      </div>
      <Button
        label="Ký tổng kết tuần"
        icon="pi pi-check"
        :disabled="!data.weeklyReview.canSignWeek"
        @click="reviewVisible = true"
      />
    </div>

    <!-- Matrix Toolbar & Filter -->
    <div class="lesson-log-matrix-toolbar">
      <div class="matrix-toolbar-title">
        <h2>Lịch ghi sổ trong tuần</h2>
      </div>
      <div class="lesson-log-filter-control">
        <label for="lesson-status-filter">Hiển thị:</label>
        <Select
          id="lesson-status-filter"
          v-model="statusFilter"
          :options="[
            { label: 'Tất cả trạng thái', value: 'ALL' },
            { label: 'Chưa ghi', value: 'UNLOGGED' },
            { label: 'Nháp', value: 'DRAFT' },
            { label: 'Đã nộp', value: 'SUBMITTED' },
            { label: 'Đã duyệt', value: 'REVIEWED' },
            { label: 'Đã điều chỉnh', value: 'AMENDED' },
          ]"
          option-label="label"
          option-value="value"
        />
        <span v-if="statusFilter !== 'ALL'" class="lesson-log-filter-hint">
          {{ filteredEntries.length }} bản ghi phù hợp
        </span>
      </div>
    </div>

    <!-- Weekly Matrix Grid -->
    <LessonLogWeeklyMatrix
      :days="data.calendarDays"
      :entries="filteredEntries"
      @select="selectEntry"
    />
  </template>

  <!-- Empty State -->
  <EmptyState
    v-else
    :heading="semesterId === null ? 'Chưa có học kỳ' : 'Chưa có dữ liệu sổ tuần'"
    :message="
      semesterId === null
        ? 'Vui lòng chọn học kỳ để bắt đầu theo dõi sổ đầu bài.'
        : 'Chưa tìm thấy dữ liệu sổ tuần cho lớp và khoảng thời gian đã chọn.'
    "
  />

  <!-- Dialogs -->
  <LessonLogEntryDialog
    v-model:visible="entryDialogVisible"
    :entry="selected"
    :read-only="Boolean(selected && !selected.canTeacherEdit)"
    :saving="saving"
    :error-message="error"
    @save-draft="saveDraft"
    @submit="submit"
  />

  <LessonLogEntryDialog
    v-model:visible="lateRecordVisible"
    :entry="selected"
    :late-record="true"
    :read-only="false"
    :saving="saving"
    :error-message="error"
    @late-record="lateRecord"
  />

  <LessonLogDetailDialog
    v-model:visible="detailVisible"
    :entry="selected"
    :saving="saving"
    @audit="showAudit"
    @review="review"
    @amend="
      amendVisible = true;
      detailVisible = false
    "
  />

  <LessonLogAmendDialog
    v-if="canManage"
    v-model:visible="amendVisible"
    :entry="selected"
    :saving="saving"
    @amend="amend"
  />

  <LessonLogAuditDrawer v-model:visible="auditVisible" :entry="selected" />

  <WeeklyHomeroomReviewDialog
    v-if="data?.weeklyReview"
    v-model:visible="reviewVisible"
    :review="data.weeklyReview"
    @sign="sign"
  />

  <!-- Sticky Bottom Selected Actions Bar -->
  <div
    v-if="selected && canManage"
    class="selected-actions-bar"
    role="toolbar"
    aria-label="Thao tác bản ghi đang chọn"
  >
    <div class="selected-actions-info">
      <span class="selected-actions-badge">Đang chọn</span>
      <span class="selected-actions-text">
        {{ selected.title || selected.subjectName || 'Tiết ' + selected.periodIndex }}
      </span>
      <span class="selected-actions-sub">
        ({{ selected.className || 'Lớp' }} ·
        {{ selected.session === 'MORNING' ? 'Sáng' : 'Chiều' }} tiết
        {{ selected.periodIndex }})
      </span>
    </div>
    <div class="selected-actions-buttons">
      <Button
        v-if="selected.canReview"
        label="Duyệt tiết này"
        icon="pi pi-check"
        :loading="saving"
        @click="review"
      />
      <Button
        v-if="selected.canAmend"
        label="Điều chỉnh"
        icon="pi pi-pencil"
        severity="warn"
        @click="amendVisible = true"
      />
      <Button
        label="Bỏ chọn"
        text
        severity="secondary"
        @click="selected = null"
      />
    </div>
  </div>
</template>
