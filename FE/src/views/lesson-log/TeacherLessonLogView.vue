<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Button from 'primevue/button'
import DatePicker from 'primevue/datepicker'

import LessonLogEntryDialog from '@/components/lesson-log/LessonLogEntryDialog.vue'
import LessonLogDetailDialog from '@/components/lesson-log/LessonLogDetailDialog.vue'
import LessonLogAuditDrawer from '@/components/lesson-log/LessonLogAuditDrawer.vue'
import LessonLogStatusBadge from '@/components/lesson-log/LessonLogStatusBadge.vue'
import FormAlert from '@/components/common/FormAlert.vue'
import EmptyState from '@/components/common/EmptyState.vue'

import {
  createLessonLog,
  getMyLessonSchedule,
  submitLessonLog,
  updateLessonLog,
} from '@/services/lessonLogApi'
import { useAuthSession } from '@/composables/useAuthSession'
import { extractApiErrorMessage, isApiError } from '@/types/api'
import type { LessonLogEntry, LessonLogRequestFields, ScheduleItem } from '@/types/lessonLog'

const { requireAccessToken } = useAuthSession()

const selectedDate = ref(new Date())
const items = ref<ScheduleItem[]>([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const notice = ref('')

const dialogVisible = ref(false)
const detailVisible = ref(false)
const auditVisible = ref(false)
const selected = ref<LessonLogEntry | null>(null)
const editingNew = ref(false)

function dateString(): string {
  const d = selectedDate.value
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function time(value: string | null): string {
  return value
    ? new Date(value).toLocaleTimeString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit',
        timeZone: 'Asia/Ho_Chi_Minh',
      })
    : '—'
}

function dateTime(value: string | null): string {
  if (!value || /^\d{4}-\d{2}-\d{2}$/.test(value)) return value ?? '—'
  return new Date(value).toLocaleString('vi-VN', {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: 'Asia/Ho_Chi_Minh',
  })
}

async function load(): Promise<void> {
  const token = requireAccessToken()
  if (!token) return
  loading.value = true
  error.value = ''
  try {
    items.value = (await getMyLessonSchedule(dateString(), token)).items
  } catch (e) {
    error.value = extractApiErrorMessage(e, 'Không thể tải lịch dạy của bạn')
  } finally {
    loading.value = false
  }
}

function open(item: ScheduleItem): void {
  if (item.entry) {
    selected.value = item.entry
    editingNew.value = false
    dialogVisible.value = true
  } else if (item.canCreate) {
    selected.value = {
      ...item,
      entryId: 0,
      title: null,
      content: null,
      completionStatus: null,
      presentCount: null,
      absentCount: null,
      absentStudentNotes: null,
      comments: null,
      homework: null,
      grade: null,
      status: 'DRAFT',
      version: 0,
      canTeacherEdit: item.canCreate,
      canSubmit: false,
      canReview: false,
      canAmend: false,
      canLateRecord: item.canLateRecord,
      blockedReason: null,
      rubric: [],
      submittedAt: null,
      submittedBy: null,
      reviewedAt: null,
      reviewedBy: null,
      reviewComment: null,
    }
    editingNew.value = true
    dialogVisible.value = true
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
      ? 'Dữ liệu đã thay đổi. Hãy mở chi tiết để xem bản mới nhất trước khi ghi tiếp.'
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

function showDetail(entry: LessonLogEntry): void {
  selected.value = entry
  detailVisible.value = true
}

function showAudit(entry: LessonLogEntry): void {
  selected.value = entry
  detailVisible.value = false
  auditVisible.value = true
}

onMounted(() => void load())

const sorted = computed(() => [...items.value].sort((a, b) => a.periodIndex - b.periodIndex))
</script>

<template>
  <div class="page-heading lesson-log-page-heading">
    <div>
      <h1>Sổ đầu bài của tôi</h1>
      <p class="section-caption">Xem lịch phân công và ghi sổ đầu bài cho các tiết giảng dạy</p>
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

  <FormAlert v-if="error" tone="error" :message="error" />
  <FormAlert v-if="notice" tone="success" :message="notice" />

  <!-- Date Selection Panel -->
  <section class="content-surface lesson-log-context-panel" aria-label="Chọn ngày giảng dạy">
    <div class="field-group" style="max-width: 280px">
      <label for="teacher-date-picker">Ngày giảng dạy</label>
      <DatePicker
        id="teacher-date-picker"
        v-model="selectedDate"
        date-format="dd/mm/yy"
        show-icon
        fluid
        aria-label="Ngày dạy"
        @update:model-value="load"
      />
    </div>
  </section>

  <!-- Loading State -->
  <div v-if="loading" class="page-state page-state-loading" role="status">
    <i class="pi pi-spin pi-spinner" aria-hidden="true" />
    <span>Đang tải lịch dạy…</span>
  </div>

  <!-- Empty State -->
  <EmptyState
    v-else-if="!sorted.length"
    icon="pi pi-calendar-times"
    heading="Không có tiết dạy"
    message="Ngày này bạn không có tiết dạy nào được phân công trong thời khóa biểu."
  />

  <!-- Schedule Card List -->
  <div v-else class="teacher-schedule-list">
    <article
      v-for="item in sorted"
      :key="`${item.timetableEntryId}-${item.periodIndex}`"
      class="teacher-schedule-card"
    >
      <div class="teacher-schedule-header">
        <p class="teacher-schedule-meta">
          {{ item.session === 'MORNING' ? 'Buổi Sáng' : 'Buổi Chiều' }} · Tiết {{ item.periodIndex }} ·
          Kết thúc {{ time(item.lessonEndsAt) }}
        </p>
        <h2>
          {{ item.subjectName || '—' }}
          <span>· {{ item.className || '—' }}</span>
        </h2>
        <p class="teacher-schedule-meta">
          {{ item.teacherName || '—' }} · Hạn sửa:
          {{ dateTime(item.editWindowExpiresAt || '') }}
        </p>
        <p v-if="item.blockedReason" class="teacher-schedule-blocked">
          <i class="pi pi-lock" aria-hidden="true" />
          {{ item.blockedReason }}
        </p>
      </div>

      <div class="teacher-schedule-actions">
        <LessonLogStatusBadge :status="item.status" />
        <Button
          v-if="item.entry"
          label="Chi tiết"
          text
          severity="secondary"
          @click="showDetail(item.entry)"
        />
        <Button
          v-if="item.canCreate || item.entry?.canTeacherEdit"
          :label="item.entry ? 'Mở sổ' : 'Ghi sổ'"
          :icon="item.entry ? 'pi pi-pencil' : 'pi pi-plus'"
          @click="open(item)"
        />
      </div>
    </article>
  </div>

  <!-- Dialogs -->
  <LessonLogEntryDialog
    v-model:visible="dialogVisible"
    :entry="selected"
    :read-only="Boolean(selected && !selected.canTeacherEdit)"
    :saving="saving"
    :error-message="error"
    @save-draft="saveDraft"
    @submit="submit"
  />

  <LessonLogDetailDialog
    v-model:visible="detailVisible"
    :entry="selected"
    @audit="showAudit"
  />

  <LessonLogAuditDrawer v-model:visible="auditVisible" :entry="selected" />
</template>
