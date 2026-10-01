<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import FormAlert from '@/components/common/FormAlert.vue'
import type { LessonLogEntry, LessonLogRequestFields } from '@/types/lessonLog'

const props = withDefaults(
  defineProps<{
    visible: boolean
    entry: LessonLogEntry | null
    readOnly?: boolean
    saving?: boolean
    errorMessage?: string
    lateRecord?: boolean
  }>(),
  {
    readOnly: false,
    saving: false,
    errorMessage: '',
    lateRecord: false,
  },
)

const emit = defineEmits<{
  'update:visible': [boolean]
  saveDraft: [LessonLogRequestFields]
  submit: [LessonLogRequestFields]
  lateRecord: [LessonLogRequestFields, string]
}>()

const form = ref<LessonLogRequestFields>({})
const lateReason = ref('')

watch(
  () => props.entry,
  (entry) => {
    form.value = entry
      ? {
          title: entry.title,
          content: entry.content,
          completionStatus: entry.completionStatus,
          presentCount: entry.presentCount,
          absentCount: entry.absentCount,
          absentStudentNotes: entry.absentStudentNotes,
          comments: entry.comments,
          homework: entry.homework,
          grade: entry.grade,
        }
      : {}
  },
  { immediate: true },
)

watch(
  () => props.visible,
  (visible) => {
    if (visible) lateReason.value = ''
  },
)

const statuses = [
  { label: 'Đúng tiến độ', value: 'ON_SCHEDULE' },
  { label: 'Chậm tiến độ', value: 'BEHIND_SCHEDULE' },
  { label: 'Vượt tiến độ', value: 'AHEAD_OF_SCHEDULE' },
]

const grades = [
  { label: 'A · Tốt', value: 'A' },
  { label: 'B · Khá', value: 'B' },
  { label: 'C · Trung bình', value: 'C' },
  { label: 'D · Yếu', value: 'D' },
]

const draftValid = computed(() => Boolean(props.entry))
const submitValid = computed(() => {
  return (
    draftValid.value &&
    Boolean(form.value.title?.trim()) &&
    Boolean(form.value.completionStatus) &&
    Boolean(form.value.grade) &&
    form.value.presentCount !== null &&
    form.value.presentCount !== undefined &&
    form.value.absentCount !== null &&
    form.value.absentCount !== undefined &&
    Number(form.value.presentCount) >= 0 &&
    Number(form.value.absentCount) >= 0 &&
    Number(form.value.presentCount) + Number(form.value.absentCount) ===
      props.entry?.rosterCountSnapshot
  )
})

const lateRecordValid = computed(() => submitValid.value && Boolean(lateReason.value.trim()))

function saveDraft(): void {
  emit('saveDraft', { ...form.value })
}

function submit(): void {
  if (submitValid.value) emit('submit', { ...form.value })
}

function recordLate(): void {
  if (lateRecordValid.value) emit('lateRecord', { ...form.value }, lateReason.value.trim())
}
</script>

<template>
  <Dialog
    :visible="props.visible"
    modal
    :header="
      props.lateRecord
        ? 'Ghi bổ sung sổ đầu bài'
        : props.entry?.entryId
          ? 'Cập nhật sổ đầu bài'
          : 'Ghi sổ đầu bài'
    "
    :style="{ width: 'min(720px, 96vw)' }"
    @update:visible="emit('update:visible', $event)"
  >
    <div v-if="props.entry" class="entry-form">
      <div class="entry-header-meta">
        <p class="field-hint">
          {{ props.entry.className || '—' }} · {{ props.entry.subjectName || '—' }} ·
          {{ props.entry.lessonDate }} · {{ props.entry.session === 'MORNING' ? 'Sáng' : 'Chiều' }}
          tiết {{ props.entry.periodIndex }}
        </p>
        <p class="field-hint">
          Giáo viên: {{ props.entry.teacherName || '—' }} · Sĩ số:
          {{ props.entry.rosterCountSnapshot ?? '—' }}
        </p>
      </div>

      <FormAlert v-if="props.errorMessage" tone="error" :message="props.errorMessage" />
      <p v-if="props.entry.blockedReason" class="blocked-message">
        {{ props.entry.blockedReason }}
      </p>

      <div class="field-group">
        <label for="lesson-title">Tên bài học</label>
        <InputText
          id="lesson-title"
          v-model="form.title"
          :disabled="props.readOnly"
          maxlength="255"
          placeholder="Nhập tên bài học"
        />
      </div>

      <div class="field-group">
        <label for="lesson-content">Nội dung bài dạy</label>
        <Textarea
          id="lesson-content"
          v-model="form.content"
          :disabled="props.readOnly"
          rows="3"
          maxlength="4000"
          placeholder="Nội dung tóm tắt kiến thức đã dạy"
        />
      </div>

      <div class="form-grid">
        <div class="field-group">
          <label for="lesson-progress">Tiến độ</label>
          <Select
            id="lesson-progress"
            v-model="form.completionStatus"
            :options="statuses"
            option-label="label"
            option-value="value"
            placeholder="Chọn tiến độ"
            :disabled="props.readOnly"
          />
        </div>

        <div class="field-group">
          <label for="lesson-grade">Xếp loại tiết học</label>
          <Select
            id="lesson-grade"
            v-model="form.grade"
            :options="grades"
            option-label="label"
            option-value="value"
            placeholder="Chọn xếp loại"
            :disabled="props.readOnly"
          />
        </div>

        <div class="field-group">
          <label for="lesson-present">Có mặt</label>
          <InputNumber
            id="lesson-present"
            v-model="form.presentCount"
            :min="0"
            :max="props.entry.rosterCountSnapshot ?? undefined"
            :disabled="props.readOnly"
          />
        </div>

        <div class="field-group">
          <label for="lesson-absent">Vắng</label>
          <InputNumber
            id="lesson-absent"
            v-model="form.absentCount"
            :min="0"
            :max="props.entry.rosterCountSnapshot ?? undefined"
            :disabled="props.readOnly"
          />
        </div>
      </div>

      <div class="field-group">
        <label for="lesson-absence-notes">Ghi chú học sinh vắng</label>
        <Textarea
          id="lesson-absence-notes"
          v-model="form.absentStudentNotes"
          :disabled="props.readOnly"
          rows="2"
          maxlength="500"
          placeholder="Tên học sinh vắng, có phép hay không phép"
        />
      </div>

      <div class="field-group">
        <label for="lesson-comments">Nhận xét của giáo viên</label>
        <Textarea
          id="lesson-comments"
          v-model="form.comments"
          :disabled="props.readOnly"
          rows="2"
          maxlength="4000"
          placeholder="Nhận xét tình hình học tập và kỷ luật của lớp"
        />
      </div>

      <div class="field-group">
        <label for="lesson-homework">Dặn dò bài tập về nhà</label>
        <Textarea
          id="lesson-homework"
          v-model="form.homework"
          :disabled="props.readOnly"
          rows="2"
          maxlength="500"
          placeholder="Bài tập hoặc nhiệm vụ chuẩn bị cho tiết sau"
        />
      </div>

      <div v-if="props.lateRecord" class="field-group">
        <label for="lesson-late-reason">
          Lý do ghi bổ sung <span class="required-mark" aria-hidden="true">*</span>
        </label>
        <Textarea
          id="lesson-late-reason"
          v-model="lateReason"
          rows="3"
          maxlength="500"
          :disabled="props.saving"
          required
          aria-required="true"
          placeholder="Nêu rõ lý do ghi bổ sung sau hạn"
        />
        <p class="field-hint">
          Ghi bổ sung sẽ lưu bản ghi ở trạng thái Đã điều chỉnh và ghi lý do vào lịch sử.
        </p>
      </div>

      <p
        v-if="
          !props.readOnly &&
          form.presentCount !== null &&
          form.absentCount !== null &&
          Number(form.presentCount) + Number(form.absentCount) !== props.entry.rosterCountSnapshot
        "
        class="validation-warning"
      >
        Tổng số học sinh có mặt và vắng ({{ Number(form.presentCount) + Number(form.absentCount) }})
        phải bằng sĩ số snapshot ({{ props.entry.rosterCountSnapshot }}).
      </p>

      <div class="dialog-actions">
        <Button
          label="Đóng"
          severity="secondary"
          text
          @click="emit('update:visible', false)"
        />
        <template v-if="!props.readOnly && props.lateRecord">
          <Button
            label="Ghi bổ sung"
            icon="pi pi-save"
            :loading="props.saving"
            :disabled="!lateRecordValid"
            @click="recordLate"
          />
        </template>
        <template v-else-if="!props.readOnly">
          <Button
            label="Lưu nháp"
            severity="secondary"
            :loading="props.saving"
            :disabled="!draftValid || !props.entry.canTeacherEdit"
            @click="saveDraft"
          />
          <Button
            v-if="props.entry.canSubmit"
            label="Nộp sổ"
            icon="pi pi-check"
            :loading="props.saving"
            :disabled="!submitValid"
            @click="submit"
          />
        </template>
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
.entry-form {
  display: grid;
  gap: 16px;
}

.entry-header-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 14px;
  background: var(--surface-container-low, #f2f4f6);
  border-radius: 8px;
}

.field-hint {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.required-mark {
  color: var(--error, #ba1a1a);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.blocked-message,
.validation-warning {
  margin: 0;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.5;
}

.blocked-message {
  background: #fef2f2;
  color: var(--error, #ba1a1a);
  border-left: 3px solid var(--error, #ba1a1a);
}

.validation-warning {
  background: #fffbeb;
  color: #b45309;
  border-left: 3px solid #f59e0b;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

@media (max-width: 560px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
