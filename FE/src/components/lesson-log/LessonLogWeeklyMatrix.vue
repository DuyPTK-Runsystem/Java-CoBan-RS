<script setup lang="ts">
import type { ClassWeekItem, CalendarDay, SessionType } from '@/types/lessonLog'
import LessonLogStatusBadge from './LessonLogStatusBadge.vue'
const props = defineProps<{ days: CalendarDay[]; entries: ClassWeekItem[] }>()
const emit = defineEmits<{ select: [entry: ClassWeekItem] }>()
const sessions: SessionType[] = ['MORNING', 'AFTERNOON']
function find(day: CalendarDay, session: SessionType, period: number) { return props.entries.find((e) => e.lessonDate === day.date && e.session === session && e.periodIndex === period) }
</script>
<template>
  <div class="lesson-matrix" role="grid" aria-label="Ma trận sổ đầu bài theo tuần">
    <div class="matrix-cell matrix-header">Buổi / tiết</div><div v-for="day in props.days" :key="day.date" class="matrix-cell matrix-header">{{ day.label }}<small>{{ day.date.slice(5) }}</small></div>
    <template v-for="session in sessions" :key="session">
      <template v-for="period in [1, 2, 3, 4]" :key="`${session}-${period}`">
        <div class="matrix-cell matrix-label">{{ session === 'MORNING' ? 'Sáng' : 'Chiều' }} · tiết {{ period }}</div>
        <div v-for="day in props.days" :key="`${day.date}-${session}-${period}`" class="matrix-cell matrix-slot" :class="{ 'matrix-off': day.kind !== 'SCHOOL_DAY' }">
          <button v-if="find(day, session, period)" type="button" class="matrix-entry" :disabled="find(day, session, period)!.status === 'UNLOGGED'" @click="emit('select', find(day, session, period)!)"><strong>{{ find(day, session, period)?.subjectName || 'Tiết đã phân công' }}</strong><LessonLogStatusBadge :status="find(day, session, period)!.status" /></button>
          <span v-else class="matrix-empty">{{ day.kind === 'SCHOOL_DAY' ? 'Chưa ghi' : day.kind === 'HOLIDAY' ? 'Nghỉ' : 'Ngoài kỳ' }}</span>
        </div>
      </template>
    </template>
  </div>
</template>
<style scoped>
.lesson-matrix{display:grid;grid-template-columns:140px repeat(7,minmax(118px,1fr));overflow:auto;border:1px solid #dbe3ec;border-radius:10px;background:#fff}.matrix-cell{min-height:76px;padding:8px;border-right:1px solid #e5e7eb;border-bottom:1px solid #e5e7eb}.matrix-header{min-height:52px;background:#f1f5f9;font-weight:700;text-align:center}.matrix-header small{display:block;color:#64748b;font-weight:400}.matrix-label{background:#f8fafc;font-size:13px;color:#475569}.matrix-slot{display:flex;align-items:center;justify-content:center}.matrix-off{background:#f8fafc;color:#94a3b8}.matrix-entry{display:grid;gap:6px;width:100%;padding:7px;border:1px solid #c7d2fe;border-radius:8px;background:#eef2ff;text-align:left;cursor:pointer}.matrix-entry strong{font-size:13px}.matrix-empty{font-size:12px;color:#64748b}
</style>
