<script setup lang="ts">
import type { CalendarDay, ClassWeekItem, SessionType } from '@/types/lessonLog'
import LessonLogStatusBadge from './LessonLogStatusBadge.vue'

const props = defineProps<{
  days: CalendarDay[]
  entries: ClassWeekItem[]
}>()

const emit = defineEmits<{
  select: [entry: ClassWeekItem]
}>()

function find(day: CalendarDay, session: SessionType, period: number): ClassWeekItem | undefined {
  return props.entries.find(
    (e) => e.lessonDate === day.date && e.session === session && e.periodIndex === period,
  )
}
</script>

<template>
  <div class="lesson-log-matrix-wrapper">
    <div class="lesson-log-matrix" role="grid" aria-label="Ma trận sổ đầu bài theo tuần">
      <!-- Top header row -->
      <div class="matrix-cell matrix-header matrix-sticky-header">
        Buổi / tiết
      </div>
      <div
        v-for="day in props.days"
        :key="day.date"
        class="matrix-cell matrix-header"
      >
        <span>{{ day.label }}</span>
        <small>{{ day.date.slice(5) }}</small>
      </div>

      <!-- Morning session -->
      <div class="matrix-session-header matrix-session-morning">
        Buổi Sáng
      </div>
      <template v-for="period in [1, 2, 3, 4]" :key="`MORNING-${period}`">
        <div class="matrix-cell matrix-label matrix-sticky-col">
          Sáng · tiết {{ period }}
        </div>
        <div
          v-for="day in props.days"
          :key="`${day.date}-MORNING-${period}`"
          class="matrix-cell matrix-slot"
          :class="{ 'matrix-off': day.kind !== 'SCHOOL_DAY' }"
        >
          <button
            v-if="find(day, 'MORNING', period)"
            type="button"
            class="matrix-entry"
            :class="{ 'matrix-entry-unlogged': find(day, 'MORNING', period)?.status === 'UNLOGGED' }"
            :aria-label="`${find(day, 'MORNING', period)?.status === 'UNLOGGED' ? 'Ghi' : 'Mở'} sổ đầu bài ${find(day, 'MORNING', period)?.subjectName || 'tiết đã phân công'}`"
            @click="emit('select', find(day, 'MORNING', period)!)"
          >
            <strong class="matrix-entry-title">
              {{ find(day, 'MORNING', period)?.subjectName || 'Tiết đã phân công' }}
            </strong>
            <span v-if="find(day, 'MORNING', period)?.teacherName" class="matrix-entry-meta">
              {{ find(day, 'MORNING', period)?.teacherName }}
            </span>
            <LessonLogStatusBadge :status="find(day, 'MORNING', period)!.status" />
          </button>
          <span v-else class="matrix-empty">
            {{ day.kind === 'SCHOOL_DAY' ? 'Chưa ghi' : day.kind === 'HOLIDAY' ? 'Nghỉ' : 'Ngoài kỳ' }}
          </span>
        </div>
      </template>

      <!-- Afternoon session -->
      <div class="matrix-session-header matrix-session-afternoon">
        Buổi Chiều
      </div>
      <template v-for="period in [1, 2, 3, 4]" :key="`AFTERNOON-${period}`">
        <div class="matrix-cell matrix-label matrix-sticky-col">
          Chiều · tiết {{ period }}
        </div>
        <div
          v-for="day in props.days"
          :key="`${day.date}-AFTERNOON-${period}`"
          class="matrix-cell matrix-slot"
          :class="{ 'matrix-off': day.kind !== 'SCHOOL_DAY' }"
        >
          <button
            v-if="find(day, 'AFTERNOON', period)"
            type="button"
            class="matrix-entry"
            :class="{ 'matrix-entry-unlogged': find(day, 'AFTERNOON', period)?.status === 'UNLOGGED' }"
            :aria-label="`${find(day, 'AFTERNOON', period)?.status === 'UNLOGGED' ? 'Ghi' : 'Mở'} sổ đầu bài ${find(day, 'AFTERNOON', period)?.subjectName || 'tiết đã phân công'}`"
            @click="emit('select', find(day, 'AFTERNOON', period)!)"
          >
            <strong class="matrix-entry-title">
              {{ find(day, 'AFTERNOON', period)?.subjectName || 'Tiết đã phân công' }}
            </strong>
            <span v-if="find(day, 'AFTERNOON', period)?.teacherName" class="matrix-entry-meta">
              {{ find(day, 'AFTERNOON', period)?.teacherName }}
            </span>
            <LessonLogStatusBadge :status="find(day, 'AFTERNOON', period)!.status" />
          </button>
          <span v-else class="matrix-empty">
            {{ day.kind === 'SCHOOL_DAY' ? 'Chưa ghi' : day.kind === 'HOLIDAY' ? 'Nghỉ' : 'Ngoài kỳ' }}
          </span>
        </div>
      </template>
    </div>
  </div>
</template>
