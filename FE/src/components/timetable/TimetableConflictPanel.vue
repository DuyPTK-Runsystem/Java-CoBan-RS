<script setup lang="ts">
import Button from 'primevue/button'
import Tag from 'primevue/tag'

import type { TimetableIssue } from '@/types/timetable'

defineProps<{
  issues: TimetableIssue[]
  blockingCount: number
  warningCount: number
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'focusEntry', issue: TimetableIssue): void
  (e: 'revalidate'): void
}>()

const dayLabels: Record<number, string> = {
  2: 'Thứ Hai', 3: 'Thứ Ba', 4: 'Thứ Tư', 5: 'Thứ Năm', 6: 'Thứ Sáu', 7: 'Thứ Bảy',
}

function issueTime(issue: TimetableIssue): string | null {
  if (!issue.dayOfWeek && !issue.session && !issue.periodIndex) return null
  const day = issue.dayOfWeek ? (dayLabels[issue.dayOfWeek] || `Thứ ${issue.dayOfWeek}`) : 'Chưa rõ ngày'
  const session = issue.session ? `buổi ${issue.session === 'MORNING' ? 'sáng' : 'chiều'}` : null
  const period = issue.periodIndex ? `tiết ${issue.periodIndex}` : null
  return [day, session, period].filter(Boolean).join(', ')
}

function issueContext(issue: TimetableIssue): string | null {
  const details = [
    issue.className ? `Lớp: ${issue.className}` : null,
    issue.teacherName ? `Giáo viên: ${issue.teacherName}` : null,
    issue.roomName ? `Phòng chức năng: ${issue.roomName}` : null,
  ].filter(Boolean)
  return details.length ? details.join(' · ') : null
}
</script>

<template>
  <div class="timetable-conflict-panel bg-white rounded-xl border border-gray-200 shadow-sm p-4 flex flex-col gap-4 min-w-0 max-w-full">
    <div class="timetable-conflict-header flex justify-between items-center gap-2 flex-wrap pb-3 border-b border-gray-100">
      <div class="flex items-center gap-2 flex-wrap min-w-0">
        <h3 class="font-bold text-gray-900 text-base">Cần xử lý</h3>
        <Tag :value="`${blockingCount} lỗi chặn`" severity="danger" />
        <Tag :value="`${warningCount} cảnh báo`" severity="warn" />
        <Tag
          v-if="blockingCount === 0 && warningCount === 0"
          value="Hợp lệ"
          severity="success"
        />
      </div>
      <Button
        label="Kiểm tra lại"
        icon="pi pi-refresh"
        size="small"
        text
        :loading="loading"
        @click="emit('revalidate')"
      />
    </div>

    <div v-if="issues.length === 0" class="p-6 text-center text-gray-500 text-sm">
      🎉 Không phát hiện xung đột lịch, trùng giáo viên, trùng lớp hay trùng phòng chức năng nào.
    </div>

    <div v-else class="timetable-conflict-issues flex flex-col gap-2 max-h-[480px] overflow-y-auto pr-1 pb-1">
      <div
        v-for="(issue, index) in issues"
        :key="index"
        class="timetable-conflict-issue p-3 rounded-lg border text-xs flex flex-col gap-1 transition min-w-0 max-w-full"
        :class="[
          issue.severity === 'BLOCKING'
            ? 'bg-red-50/60 border-red-200 text-red-950'
            : 'bg-amber-50/60 border-amber-200 text-amber-950',
        ]"
      >
        <div class="flex items-start justify-between gap-2 min-w-0">
          <span class="font-semibold flex items-start gap-1.5 min-w-0 break-words">
            <span>{{ issue.severity === 'BLOCKING' ? '⛔ Lỗi chặn' : '⚠️ Cảnh báo' }}</span>
            <span class="text-gray-500 text-[11px]">({{ issue.code }})</span>
          </span>
          <Button
            v-if="(issue.entryIds?.length ?? 0) > 0 || issue.entryId"
            label="Đến tiết học"
            size="small"
            text
            class="text-[11px] p-0 h-auto"
            @click="emit('focusEntry', issue)"
          />
        </div>

        <p class="text-xs text-gray-800 leading-relaxed break-words overflow-wrap-anywhere">{{ issue.message }}</p>

        <div v-if="issueTime(issue)" class="text-[11px] text-gray-700">Thời điểm: {{ issueTime(issue) }}</div>
        <div v-if="issueContext(issue)" class="text-[11px] text-gray-500 break-words overflow-wrap-anywhere">{{ issueContext(issue) }}</div>
      </div>
    </div>
  </div>
</template>
