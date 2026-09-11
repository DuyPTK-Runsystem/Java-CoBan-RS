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
</script>

<template>
  <div class="bg-white rounded-xl border border-gray-200 shadow-sm p-4 flex flex-col gap-4">
    <div class="flex justify-between items-center pb-3 border-b border-gray-100">
      <div class="flex items-center gap-2">
        <h3 class="font-bold text-gray-900 text-base">Cần xử lý</h3>
        <Tag
          v-if="blockingCount > 0"
          :value="`${blockingCount} lỗi chặn`"
          severity="danger"
        />
        <Tag
          v-if="warningCount > 0"
          :value="`${warningCount} cảnh báo`"
          severity="warn"
        />
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

    <div v-else class="flex flex-col gap-2 max-h-[480px] overflow-y-auto pr-1">
      <div
        v-for="(issue, index) in issues"
        :key="index"
        class="p-3 rounded-lg border text-xs flex flex-col gap-1 transition"
        :class="[
          issue.severity === 'BLOCKING'
            ? 'bg-red-50/60 border-red-200 text-red-950'
            : 'bg-amber-50/60 border-amber-200 text-amber-950',
        ]"
      >
        <div class="flex items-center justify-between">
          <span class="font-semibold flex items-center gap-1.5">
            <span>{{ issue.severity === 'BLOCKING' ? '⛔ Lỗi chặn' : '⚠️ Cảnh báo' }}</span>
            <span class="text-gray-500 text-[11px]">({{ issue.code }})</span>
          </span>
          <Button
            v-if="issue.entryId"
            label="Đến tiết học"
            size="small"
            text
            class="text-[11px] p-0 h-auto"
            @click="emit('focusEntry', issue)"
          />
        </div>

        <p class="text-xs text-gray-800 leading-relaxed">{{ issue.message }}</p>

        <div v-if="issue.dayOfWeek || issue.periodIndex" class="text-[11px] text-gray-500">
          <span v-if="issue.dayOfWeek">Thứ {{ issue.dayOfWeek }} · </span>
          <span v-if="issue.session">{{ issue.session === 'MORNING' ? 'Sáng' : 'Chiều' }} · </span>
          <span v-if="issue.periodIndex">Tiết {{ issue.periodIndex }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
