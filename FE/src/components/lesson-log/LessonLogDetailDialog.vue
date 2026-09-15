<script setup lang="ts">
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import LessonLogStatusBadge from './LessonLogStatusBadge.vue'
import type { LessonLogEntry } from '@/types/lessonLog'

const props = withDefaults(defineProps<{ visible: boolean; entry: LessonLogEntry | null; saving?: boolean }>(), { saving: false })
const emit = defineEmits<{ 'update:visible': [boolean]; audit: [LessonLogEntry]; review: [LessonLogEntry]; amend: [LessonLogEntry] }>()

function display(value: string | null | undefined): string { return value?.trim() || '—' }
</script>

<template>
  <Dialog :visible="props.visible" modal header="Chi tiết sổ đầu bài" :style="{ width: 'min(720px, 96vw)' }" @update:visible="emit('update:visible', $event)">
    <div v-if="props.entry" class="detail-content">
      <div class="detail-heading"><div><h2>{{ display(props.entry.title) }}</h2><p>{{ display(props.entry.className) }} · {{ display(props.entry.subjectName) }} · {{ props.entry.lessonDate }}</p></div><LessonLogStatusBadge :status="props.entry.status" /></div>
      <dl class="detail-grid"><div><dt>Giáo viên</dt><dd>{{ display(props.entry.teacherName) }}</dd></div><div><dt>Buổi / tiết</dt><dd>{{ props.entry.session === 'MORNING' ? 'Sáng' : 'Chiều' }} · {{ props.entry.periodIndex }}</dd></div><div><dt>Tiến độ</dt><dd>{{ display(props.entry.completionStatus) }}</dd></div><div><dt>Xếp loại</dt><dd>{{ display(props.entry.grade) }}</dd></div><div><dt>Sĩ số</dt><dd>{{ props.entry.presentCount ?? '—' }} có mặt · {{ props.entry.absentCount ?? '—' }} vắng / {{ props.entry.rosterCountSnapshot ?? '—' }}</dd></div><div><dt>Hạn sửa</dt><dd>{{ display(props.entry.editWindowExpiresAt) }}</dd></div></dl>
      <div class="detail-block"><strong>Nội dung</strong><p>{{ display(props.entry.content) }}</p></div><div class="detail-block"><strong>Nhận xét</strong><p>{{ display(props.entry.comments) }}</p></div><div class="detail-block"><strong>Dặn dò</strong><p>{{ display(props.entry.homework) }}</p></div>
      <div v-if="props.entry.reviewComment" class="detail-block"><strong>Nhận xét duyệt</strong><p>{{ props.entry.reviewComment }}</p></div>
      <div class="dialog-actions"><Button v-if="props.entry.canReview" label="Duyệt" icon="pi pi-check" :loading="props.saving" @click="emit('review', props.entry!)" /><Button v-if="props.entry.canAmend" label="Điều chỉnh" icon="pi pi-pencil" severity="warn" @click="emit('amend', props.entry!)" /><Button label="Xem lịch sử" icon="pi pi-history" severity="secondary" @click="emit('audit', props.entry!)" /><Button label="Đóng" text @click="emit('update:visible', false)" /></div>
    </div>
  </Dialog>
</template>

<style scoped>
.detail-content{display:grid;gap:16px}.detail-heading{display:flex;justify-content:space-between;gap:12px;align-items:flex-start}.detail-heading h2{margin:0;font-size:20px}.detail-heading p{margin:5px 0 0;color:#64748b}.detail-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px;margin:0}.detail-grid div{padding:10px;border-radius:8px;background:#f8fafc}.detail-grid dt{font-size:12px;color:#64748b}.detail-grid dd{margin:3px 0 0;font-weight:600}.detail-block{padding-top:4px}.detail-block p{white-space:pre-wrap;margin:6px 0;color:#334155}.dialog-actions{display:flex;justify-content:flex-end;gap:8px}@media(max-width:560px){.detail-grid{grid-template-columns:1fr}.detail-heading{flex-direction:column}}
</style>
