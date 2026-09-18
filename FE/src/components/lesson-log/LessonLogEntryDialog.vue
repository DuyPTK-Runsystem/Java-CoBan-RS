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

const props = withDefaults(defineProps<{ visible: boolean; entry: LessonLogEntry | null; readOnly?: boolean; saving?: boolean; errorMessage?: string; lateRecord?: boolean }>(), { readOnly: false, saving: false, errorMessage: '', lateRecord: false })
const emit = defineEmits<{ 'update:visible': [boolean]; saveDraft: [LessonLogRequestFields]; submit: [LessonLogRequestFields]; lateRecord: [LessonLogRequestFields, string] }>()
const form = ref<LessonLogRequestFields>({})
const lateReason = ref('')
watch(() => props.entry, (entry) => { form.value = entry ? { title: entry.title, content: entry.content, completionStatus: entry.completionStatus, presentCount: entry.presentCount, absentCount: entry.absentCount, absentStudentNotes: entry.absentStudentNotes, comments: entry.comments, homework: entry.homework, grade: entry.grade } : {} }, { immediate: true })
watch(() => props.visible, (visible) => { if (visible) lateReason.value = '' })
const statuses = [{ label: 'Đúng tiến độ', value: 'ON_SCHEDULE' }, { label: 'Chậm tiến độ', value: 'BEHIND_SCHEDULE' }, { label: 'Vượt tiến độ', value: 'AHEAD_OF_SCHEDULE' }]
const grades = [{ label: 'A · Tốt', value: 'A' }, { label: 'B · Khá', value: 'B' }, { label: 'C · Trung bình', value: 'C' }, { label: 'D · Yếu', value: 'D' }]
const draftValid = computed(() => Boolean(props.entry))
const submitValid = computed(() => draftValid.value && Boolean(form.value.title?.trim()) && Boolean(form.value.completionStatus) && Boolean(form.value.grade) && form.value.presentCount !== null && form.value.presentCount !== undefined && form.value.absentCount !== null && form.value.absentCount !== undefined && Number(form.value.presentCount) >= 0 && Number(form.value.absentCount) >= 0 && Number(form.value.presentCount) + Number(form.value.absentCount) === props.entry?.rosterCountSnapshot)
const lateRecordValid = computed(() => submitValid.value && Boolean(lateReason.value.trim()))
function saveDraft(): void { emit('saveDraft', { ...form.value }) }
function submit(): void { if (submitValid.value) emit('submit', { ...form.value }) }
function recordLate(): void { if (lateRecordValid.value) emit('lateRecord', { ...form.value }, lateReason.value.trim()) }
</script>
<template>
  <Dialog :visible="props.visible" modal :header="props.lateRecord ? 'Ghi bổ sung sổ đầu bài' : (props.entry?.entryId ? 'Cập nhật sổ đầu bài' : 'Ghi sổ đầu bài')" :style="{ width: 'min(720px, 96vw)' }" @update:visible="emit('update:visible', $event)">
    <div v-if="props.entry" class="entry-form">
      <p class="field-hint">{{ props.entry.className || '—' }} · {{ props.entry.subjectName || '—' }} · {{ props.entry.lessonDate }} · {{ props.entry.session === 'MORNING' ? 'Sáng' : 'Chiều' }} tiết {{ props.entry.periodIndex }}</p><p class="field-hint">Giáo viên: {{ props.entry.teacherName || '—' }} · Sĩ số snapshot: {{ props.entry.rosterCountSnapshot ?? '—' }}</p><FormAlert v-if="props.errorMessage" tone="error" :message="props.errorMessage" /><p v-if="props.entry.blockedReason" class="blocked">{{ props.entry.blockedReason }}</p>
      <label for="lesson-title">Tên bài <InputText id="lesson-title" v-model="form.title" :disabled="props.readOnly" maxlength="255" /></label><label for="lesson-content">Nội dung <Textarea id="lesson-content" v-model="form.content" :disabled="props.readOnly" rows="3" maxlength="4000" /></label><div class="form-grid"><label for="lesson-progress">Tiến độ <Select id="lesson-progress" v-model="form.completionStatus" :options="statuses" option-label="label" option-value="value" :disabled="props.readOnly" /></label><label for="lesson-grade">Đánh giá <Select id="lesson-grade" v-model="form.grade" :options="grades" option-label="label" option-value="value" :disabled="props.readOnly" /></label><label for="lesson-present">Có mặt <InputNumber id="lesson-present" v-model="form.presentCount" :min="0" :max="props.entry.rosterCountSnapshot" :disabled="props.readOnly" /></label><label for="lesson-absent">Vắng <InputNumber id="lesson-absent" v-model="form.absentCount" :min="0" :max="props.entry.rosterCountSnapshot" :disabled="props.readOnly" /></label></div><label for="lesson-absence-notes">Ghi chú vắng <Textarea id="lesson-absence-notes" v-model="form.absentStudentNotes" :disabled="props.readOnly" rows="2" maxlength="500" /></label><label for="lesson-comments">Nhận xét <Textarea id="lesson-comments" v-model="form.comments" :disabled="props.readOnly" rows="2" maxlength="4000" /></label><label for="lesson-homework">Dặn dò <Textarea id="lesson-homework" v-model="form.homework" :disabled="props.readOnly" rows="2" maxlength="500" /></label><label v-if="props.lateRecord" for="lesson-late-reason">Lý do ghi bổ sung <Textarea id="lesson-late-reason" v-model="lateReason" rows="3" maxlength="500" :disabled="props.saving" required /></label><p v-if="props.lateRecord" class="field-hint">Ghi bổ sung sẽ lưu bản ghi ở trạng thái Đã điều chỉnh và ghi lý do vào lịch sử.</p><p v-if="!props.readOnly && form.presentCount !== null && form.absentCount !== null && Number(form.presentCount) + Number(form.absentCount) !== props.entry.rosterCountSnapshot" class="validation">Có mặt và vắng phải bằng sĩ số snapshot.</p><div class="dialog-actions"><Button label="Đóng" severity="secondary" text @click="emit('update:visible', false)" /><template v-if="!props.readOnly && props.lateRecord"><Button label="Ghi bổ sung" icon="pi pi-save" :loading="props.saving" :disabled="!lateRecordValid" @click="recordLate" /></template><template v-else-if="!props.readOnly"><Button label="Lưu nháp" severity="secondary" :loading="props.saving" :disabled="!draftValid || !props.entry.canTeacherEdit" @click="saveDraft" /><Button v-if="props.entry.canSubmit" label="Nộp sổ" :loading="props.saving" :disabled="!submitValid" @click="submit" /></template></div>
    </div>
  </Dialog>
</template>
<style scoped>.entry-form{display:grid;gap:14px}.entry-form label{display:grid;gap:6px;color:#334155;font-size:14px;font-weight:600}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}.dialog-actions{display:flex;justify-content:flex-end;gap:8px}.field-hint{margin:0;color:#64748b;font-size:1.3em;line-height:1.4}.blocked,.validation{margin:0;padding:10px;border-radius:8px;background:#fff7ed;color:#9a3412}@media(max-width:560px){.form-grid{grid-template-columns:1fr}}</style>
