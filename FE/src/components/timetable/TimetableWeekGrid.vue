<script setup lang="ts">
import Button from 'primevue/button'

import type { SessionType, TimetableEntry, TimetablePeriod } from '@/types/timetable'

const props = defineProps<{
  periods: TimetablePeriod[]
  entries: TimetableEntry[]
  canEdit?: boolean
  viewMode?: 'CLASS' | 'TEACHER' | 'ROOM'
  conflictedEntryIds?: Set<number>
  busySlotKeys?: Set<string>
}>()

const emit = defineEmits<{
  (e: 'addEntry', period: TimetablePeriod): void
  (e: 'editEntry', entry: TimetableEntry): void
}>()

const days = [
  { day: 2, label: 'Thứ Hai' },
  { day: 3, label: 'Thứ Ba' },
  { day: 4, label: 'Thứ Tư' },
  { day: 5, label: 'Thứ Năm' },
  { day: 6, label: 'Thứ Sáu' },
  { day: 7, label: 'Thứ Bảy' },
]

const morningPeriods = [1, 2, 3, 4]
const afternoonPeriods = [1, 2, 3, 4]

function getPeriod(day: number, session: SessionType, index: number): TimetablePeriod | undefined {
  return (props.periods || []).find(
    (p) => p.dayOfWeek === day && p.session === session && p.periodIndex === index,
  )
}

function getEntries(day: number, session: SessionType, index: number): TimetableEntry[] {
  return (props.entries || []).filter(
    (e) => e.dayOfWeek === day && e.session === session && e.periodIndex === index,
  )
}

function isConflicted(entry: TimetableEntry): boolean {
  const id = entry.entryId ?? entry.id
  return (id !== undefined && props.conflictedEntryIds?.has(id)) ?? false
}

function slotLabel(day: typeof days[number], session: SessionType, index: number): string {
  return `${day.label}, buổi ${session === 'MORNING' ? 'sáng' : 'chiều'}, tiết ${index}`
}

function isBusy(day: number, session: SessionType, index: number): boolean {
  return props.busySlotKeys?.has(`${day}-${session}-${index}`) ?? false
}

function activateEntry(event: KeyboardEvent, entry: TimetableEntry) {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    emit('editEntry', entry)
  }
}
</script>

<template>
  <div class="bg-white rounded-xl border border-gray-200 shadow-sm overflow-x-auto">
    <table class="w-full border-collapse text-sm min-w-[900px]">
      <thead>
        <tr class="bg-gray-50 border-b border-gray-200 text-gray-700">
          <th class="p-3 w-28 text-left font-semibold">Buổi / Tiết</th>
          <th v-for="d in days" :key="d.day" class="p-3 text-center font-semibold border-l border-gray-200">
            {{ d.label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <!-- Morning session header -->
        <tr class="bg-amber-50/50 border-b border-gray-200">
          <td colspan="7" class="p-2 px-3 font-semibold text-amber-900 text-xs uppercase tracking-wider">
            Buổi Sáng
          </td>
        </tr>
        <tr v-for="idx in morningPeriods" :key="`M-${idx}`" class="border-b border-gray-200">
          <td class="p-2 font-medium text-gray-600 bg-gray-50/30">
            Tiết {{ idx }}
          </td>
          <td
            v-for="d in days"
            :key="`M-${idx}-${d.day}`"
            class="p-2 border-l border-gray-200 align-top min-h-[70px] relative group transition"
            :class="isBusy(d.day, 'MORNING', idx) ? 'timetable-busy-slot' : 'hover:bg-blue-50/20'"
            :title="isBusy(d.day, 'MORNING', idx) ? 'Giáo viên đăng kí bận vào tiết này' : undefined"
          >
            <div v-if="isBusy(d.day, 'MORNING', idx)" class="text-[10px] font-semibold text-slate-800 mb-1" aria-label="Giáo viên đã đăng ký bận">
              ⛔ Giáo viên bận
            </div>
            <div
              v-for="entry in getEntries(d.day, 'MORNING', idx)"
              :key="entry.entryId ?? entry.id"
              class="p-2 rounded-lg border text-xs mb-1 cursor-pointer transition shadow-xs"
              :class="[
                isConflicted(entry)
                  ? 'bg-red-50 border-red-300 text-red-900 hover:bg-red-100'
                  : 'bg-blue-50 border-blue-200 text-blue-900 hover:bg-blue-100',
              ]"
              role="button"
              tabindex="0"
              :aria-label="`${slotLabel(d, 'MORNING', idx)}: ${entry.subjectName}${isConflicted(entry) ? ', có xung đột' : ''}`"
              @click="emit('editEntry', entry)"
              @keydown="activateEntry($event, entry)"
            >
              <div class="font-semibold flex items-center justify-between">
                <span>{{ entry.subjectName }}</span>
                <span v-if="isConflicted(entry)" class="text-red-700 font-bold">⚠️ Có xung đột</span>
              </div>
              <div class="text-gray-600 text-[11px] mt-0.5">
                <span v-if="viewMode !== 'CLASS'">{{ entry.className }} · </span>
                <span v-if="viewMode !== 'TEACHER'">{{ entry.teacherName }}</span>
              </div>
              <div v-if="entry.roomCode" class="mt-1 inline-block px-1.5 py-0.5 bg-white/80 rounded text-[10px] text-gray-700 border border-gray-200 font-medium">
                🏛️ {{ entry.roomName || entry.roomCode }}
              </div>
            </div>

            <!-- Empty slot action -->
            <div
              v-if="canEdit && !isBusy(d.day, 'MORNING', idx) && getEntries(d.day, 'MORNING', idx).length === 0"
              class="h-full min-h-[50px] flex items-center justify-center transition"
            >
              <Button
                icon="pi pi-plus"
                size="small"
                severity="secondary"
                rounded
                text
                title="Thêm tiết"
                :aria-label="`Thêm tiết ${slotLabel(d, 'MORNING', idx)}`"
                @click="getPeriod(d.day, 'MORNING', idx) && emit('addEntry', getPeriod(d.day, 'MORNING', idx)!)"
              />
            </div>
          </td>
        </tr>

        <!-- Afternoon session header -->
        <tr class="bg-indigo-50/50 border-b border-gray-200">
          <td colspan="7" class="p-2 px-3 font-semibold text-indigo-900 text-xs uppercase tracking-wider">
            Buổi Chiều
          </td>
        </tr>
        <tr v-for="idx in afternoonPeriods" :key="`A-${idx}`" class="border-b border-gray-200">
          <td class="p-2 font-medium text-gray-600 bg-gray-50/30">
            Tiết {{ idx }}
          </td>
          <td
            v-for="d in days"
            :key="`A-${idx}-${d.day}`"
            class="p-2 border-l border-gray-200 align-top min-h-[70px] relative group transition"
            :class="isBusy(d.day, 'AFTERNOON', idx) ? 'timetable-busy-slot' : 'hover:bg-blue-50/20'"
            :title="isBusy(d.day, 'AFTERNOON', idx) ? 'Giáo viên đăng kí bận vào tiết này' : undefined"
          >
            <div v-if="isBusy(d.day, 'AFTERNOON', idx)" class="text-[10px] font-semibold text-slate-800 mb-1" aria-label="Giáo viên đã đăng ký bận">
              ⛔ Giáo viên bận
            </div>
            <div
              v-for="entry in getEntries(d.day, 'AFTERNOON', idx)"
              :key="entry.entryId ?? entry.id"
              class="p-2 rounded-lg border text-xs mb-1 cursor-pointer transition shadow-xs"
              :class="[
                isConflicted(entry)
                  ? 'bg-red-50 border-red-300 text-red-900 hover:bg-red-100'
                  : 'bg-indigo-50 border-indigo-200 text-indigo-900 hover:bg-indigo-100',
              ]"
              role="button"
              tabindex="0"
              :aria-label="`${slotLabel(d, 'AFTERNOON', idx)}: ${entry.subjectName}${isConflicted(entry) ? ', có xung đột' : ''}`"
              @click="emit('editEntry', entry)"
              @keydown="activateEntry($event, entry)"
            >
              <div class="font-semibold flex items-center justify-between">
                <span>{{ entry.subjectName }}</span>
                <span v-if="isConflicted(entry)" class="text-red-700 font-bold">⚠️ Có xung đột</span>
              </div>
              <div class="text-gray-600 text-[11px] mt-0.5">
                <span v-if="viewMode !== 'CLASS'">{{ entry.className }} · </span>
                <span v-if="viewMode !== 'TEACHER'">{{ entry.teacherName }}</span>
              </div>
              <div v-if="entry.roomCode" class="mt-1 inline-block px-1.5 py-0.5 bg-white/80 rounded text-[10px] text-gray-700 border border-gray-200 font-medium">
                🏛️ {{ entry.roomName || entry.roomCode }}
              </div>
            </div>

            <!-- Empty slot action -->
            <div
              v-if="canEdit && !isBusy(d.day, 'AFTERNOON', idx) && getEntries(d.day, 'AFTERNOON', idx).length === 0"
              class="h-full min-h-[50px] flex items-center justify-center transition"
            >
              <Button
                icon="pi pi-plus"
                size="small"
                severity="secondary"
                rounded
                text
                title="Thêm tiết"
                :aria-label="`Thêm tiết ${slotLabel(d, 'AFTERNOON', idx)}`"
                @click="getPeriod(d.day, 'AFTERNOON', idx) && emit('addEntry', getPeriod(d.day, 'AFTERNOON', idx)!)"
              />
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
