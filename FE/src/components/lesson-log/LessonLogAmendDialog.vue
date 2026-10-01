<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import type { LessonLogEntry, LessonLogRequestFields } from '@/types/lessonLog'

const props = defineProps<{
  visible: boolean
  entry: LessonLogEntry | null
  saving?: boolean
}>()

const emit = defineEmits<{
  'update:visible': [boolean]
  amend: [LessonLogRequestFields, string]
}>()

const reason = ref('')
const form = ref<LessonLogRequestFields>({})

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

watch(
  () => [props.visible, props.entry],
  ([visible]) => {
    if (visible && props.entry) {
      reason.value = ''
      form.value = {
        title: props.entry.title,
        content: props.entry.content,
        completionStatus: props.entry.completionStatus,
        presentCount: props.entry.presentCount,
        absentCount: props.entry.absentCount,
        absentStudentNotes: props.entry.absentStudentNotes,
        comments: props.entry.comments,
        homework: props.entry.homework,
        grade: props.entry.grade,
      }
    }
  },
  { immediate: true },
)

const isValid = computed(() => {
  return (
    Boolean(props.entry) &&
    Boolean(reason.value.trim()) &&
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

function submit(): void {
  if (isValid.value && props.entry) {
    emit('amend', { ...form.value }, reason.value.trim())
  }
}
</script>

<template>
  <Dialog
    :visible="props.visible"
    modal
    header="Điều chỉnh sổ đầu bài"
    :style="{ width: 'min(720px, 96vw)' }"
    @update:visible="emit('update:visible', $event)"
  >
    <div v-if="props.entry" class="amend-form">
      <div class="amend-notice">
        <i class="pi pi-exclamation-triangle" aria-hidden="true" />
        <div>
          <strong>Lưu ý về điều chỉnh:</strong>
          <p>
            Thao tác điều chỉnh sẽ lưu bản ghi hiện tại vào lịch sử chỉnh sửa và có thể khiến tuần đã ký
            chuyển sang trạng thái <strong>Cần ký lại</strong>.
          </p>
        </div>
      </div>

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

      <div class="field-group">
        <label for="amend-reason">
          Lý do điều chỉnh <span class="required-mark" aria-hidden="true">*</span>
        </label>
        <Textarea
          id="amend-reason"
          v-model="reason"
          rows="3"
          maxlength="500"
          required
          aria-required="true"
          placeholder="Nêu rõ lý do cần điều chỉnh sổ đầu bài"
        />
      </div>

      <div class="field-group">
        <label for="amend-title">Tên bài học</label>
        <InputText
          id="amend-title"
          v-model="form.title"
          maxlength="255"
          placeholder="Nhập tên bài học"
        />
      </div>

      <div class="field-group">
        <label for="amend-content">Nội dung bài dạy</label>
        <Textarea
          id="amend-content"
          v-model="form.content"
          rows="3"
          maxlength="4000"
          placeholder="Nội dung tóm tắt kiến thức đã dạy"
        />
      </div>

      <div class="form-grid">
        <div class="field-group">
          <label for="amend-progress">Tiến độ</label>
          <Select
            id="amend-progress"
            v-model="form.completionStatus"
            :options="statuses"
            option-label="label"
            option-value="value"
            placeholder="Chọn tiến độ"
          />
        </div>

        <div class="field-group">
          <label for="amend-grade">Xếp loại tiết học</label>
          <Select
            id="amend-grade"
            v-model="form.grade"
            :options="grades"
            option-label="label"
            option-value="value"
            placeholder="Chọn xếp loại"
          />
        </div>

        <div class="field-group">
          <label for="amend-present">Có mặt</label>
          <InputNumber
            id="amend-present"
            v-model="form.presentCount"
            :min="0"
            :max="props.entry.rosterCountSnapshot ?? undefined"
          />
        </div>

        <div class="field-group">
          <label for="amend-absent">Vắng</label>
          <InputNumber
            id="amend-absent"
            v-model="form.absentCount"
            :min="0"
            :max="props.entry.rosterCountSnapshot ?? undefined"
          />
        </div>
      </div>

      <div class="field-group">
        <label for="amend-absence-notes">Ghi chú học sinh vắng</label>
        <Textarea
          id="amend-absence-notes"
          v-model="form.absentStudentNotes"
          rows="2"
          maxlength="500"
          placeholder="Tên học sinh vắng, có phép hay không phép"
        />
      </div>

      <div class="field-group">
        <label for="amend-comments">Nhận xét của giáo viên</label>
        <Textarea
          id="amend-comments"
          v-model="form.comments"
          rows="2"
          maxlength="4000"
          placeholder="Nhận xét tình hình học tập và kỷ luật của lớp"
        />
      </div>

      <div class="field-group">
        <label for="amend-homework">Dặn dò bài tập về nhà</label>
        <Textarea
          id="amend-homework"
          v-model="form.homework"
          rows="2"
          maxlength="500"
          placeholder="Bài tập hoặc nhiệm vụ chuẩn bị cho tiết sau"
        />
      </div>

      <p
        v-if="
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
          label="Hủy"
          severity="secondary"
          text
          @click="emit('update:visible', false)"
        />
        <Button
          label="Lưu điều chỉnh"
          icon="pi pi-save"
          severity="warn"
          :loading="props.saving"
          :disabled="!isValid"
          @click="submit"
        />
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
.amend-form {
  display: grid;
  gap: 16px;
}

.amend-notice {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #fffbeb;
  border-left: 3px solid #f59e0b;
  color: #92400e;
  font-size: 13px;
}

.amend-notice i {
  margin-top: 2px;
  font-size: 16px;
  color: #d97706;
}

.amend-notice p {
  margin: 4px 0 0;
  line-height: 1.4;
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

.validation-warning {
  margin: 0;
  padding: 10px 14px;
  border-radius: 8px;
  background: #fffbeb;
  color: #b45309;
  border-left: 3px solid #f59e0b;
  font-size: 13px;
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
