<script setup lang="ts">
import Button from 'primevue/button'

import type { SessionType, TimetableEntry, TimetablePeriod } from '@/types/timetable'

const props = defineProps<{
  periods: TimetablePeriod[]
  entries: TimetableEntry[]
  canEdit?: boolean
  viewMode?: 'CLASS' | 'TEACHER' | 'ROOM'
  conflictedEntryIds?: Set<number>
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
  return props.periods.find(
    (p) => p.dayOfWeek === day && p.session === session && p.periodIndex === index,
  )
}

function getEntries(day: number, session: SessionType, index: number): TimetableEntry[] {
  return props.entries.filter(
    (e) => e.dayOfWeek === day && e.session === session && e.periodIndex === index,
  )
}

function isConflicted(entry: TimetableEntry): boolean {
  return props.conflictedEntryIds?.has(entry.id) ?? false
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
            class="p-2 border-l border-gray-200 align-top min-h-[70px] relative group hover:bg-blue-50/20 transition"
          >
            <div
              v-for="entry in getEntries(d.day, 'MORNING', idx)"
              :key="entry.id"
              class="p-2 rounded-lg border text-xs mb-1 cursor-pointer transition shadow-xs"
              :class="[
                isConflicted(entry)
                  ? 'bg-red-50 border-red-300 text-red-900 hover:bg-red-100'
                  : 'bg-blue-50 border-blue-200 text-blue-900 hover:bg-blue-100',
              ]"
              @click="emit('editEntry', entry)"
            >
              <div class="font-semibold flex items-center justify-between">
                <span>{{ entry.subjectName }}</span>
                <span v-if="isConflicted(entry)" class="text-red-600 font-bold" title="Trùng lịch">⚠️</span>
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
              v-if="canEdit && getEntries(d.day, 'MORNING', idx).length === 0"
              class="h-full min-h-[50px] flex items-center justify-center opacity-0 group-hover:opacity-100 transition"
            >
              <Button
                icon="pi pi-plus"
                size="small"
                severity="secondary"
                rounded
                text
                title="Thêm tiết"
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
            class="p-2 border-l border-gray-200 align-top min-h-[70px] relative group hover:bg-blue-50/20 transition"
          >
            <div
              v-for="entry in getEntries(d.day, 'AFTERNOON', idx)"
              :key="entry.id"
              class="p-2 rounded-lg border text-xs mb-1 cursor-pointer transition shadow-xs"
              :class="[
                isConflicted(entry)
                  ? 'bg-red-50 border-red-300 text-red-900 hover:bg-red-100'
                  : 'bg-indigo-50 border-indigo-200 text-indigo-900 hover:bg-indigo-100',
              ]"
              @click="emit('editEntry', entry)"
            >
              <div class="font-semibold flex items-center justify-between">
                <span>{{ entry.subjectName }}</span>
                <span v-if="isConflicted(entry)" class="text-red-600 font-bold" title="Trùng lịch">⚠️</span>
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
              v-if="canEdit && getEntries(d.day, 'AFTERNOON', idx).length === 0"
              class="h-full min-h-[50px] flex items-center justify-center opacity-0 group-hover:opacity-100 transition"
            >
              <Button
                icon="pi pi-plus"
                size="small"
                severity="secondary"
                rounded
                text
                title="Thêm tiết"
                @click="getPeriod(d.day, 'AFTERNOON', idx) && emit('addEntry', getPeriod(d.day, 'AFTERNOON', idx)!)"
              />
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
