<script setup lang="ts">
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Tag from 'primevue/tag'

import type { TeacherLoad } from '@/types/timetable'

defineProps<{
  teacherLoads: TeacherLoad[]
  loading?: boolean
}>()

function getStatusSeverity(status: string): 'success' | 'warn' | 'info' {
  switch (status) {
    case 'TARGET_MET':
      return 'success'
    case 'LOAD_ABOVE_TARGET':
      return 'info'
    case 'LOAD_BELOW_TARGET':
    default:
      return 'warn'
  }
}

function getStatusLabel(status: string): string {
  switch (status) {
    case 'TARGET_MET':
      return 'Đạt chuẩn'
    case 'LOAD_ABOVE_TARGET':
      return 'Vượt định mức'
    case 'LOAD_BELOW_TARGET':
      return 'Thiếu tiết'
    default:
      return status
  }
}
</script>

<template>
  <div class="bg-white rounded-xl border border-gray-200 shadow-sm p-4 flex flex-col gap-4">
    <div class="flex justify-between items-center pb-2 border-b border-gray-100">
      <div>
        <h3 class="font-bold text-gray-900 text-base">Định mức tiết dạy giáo viên</h3>
        <p class="text-xs text-gray-500">
          Đối chiếu số tiết thực tế với quy định (Thông tư chuẩn 19 tiết/tuần, chủ nhiệm giảm 4, nuôi con nhỏ giảm 3).
        </p>
      </div>
    </div>

    <DataTable :value="teacherLoads" :loading="loading" responsive-layout="scroll" striped-rows>
      <template #empty>
        <div class="p-8 text-center text-gray-500">Không có dữ liệu định mức giáo viên</div>
      </template>

      <Column field="teacherName" header="Giáo viên" style="min-width: 180px" />
      <Column field="assignedPeriods" header="Số tiết xếp" style="width: 110px" class="text-center">
        <template #body="{ data }">
          <span class="font-bold text-sm text-gray-800">{{ data.assignedPeriods }}</span>
        </template>
      </Column>
      <Column field="basePeriods" header="Chuẩn gốc" style="width: 100px" class="text-center" />
      <Column header="Miễn giảm" style="min-width: 160px">
        <template #body="{ data }">
          <div v-if="data.reductions > 0" class="flex flex-col text-xs">
            <span class="font-semibold text-emerald-700">-{{ data.reductions }} tiết</span>
            <span
              v-for="(r, idx) in data.reductionDetails"
              :key="idx"
              class="text-[11px] text-gray-500"
            >
              · {{ r.ruleName }} (-{{ r.reductionPeriods }})
            </span>
          </div>
          <span v-else class="text-xs text-gray-400">Không</span>
        </template>
      </Column>
      <Column field="targetPeriods" header="Định mức sau giảm" style="width: 140px" class="text-center">
        <template #body="{ data }">
          <span class="font-semibold text-sm text-blue-800">{{ data.targetPeriods }}</span>
        </template>
      </Column>
      <Column field="difference" header="Chênh lệch" style="width: 110px" class="text-center">
        <template #body="{ data }">
          <span
            class="font-bold text-xs"
            :class="[
              data.difference === 0 ? 'text-emerald-600' : data.difference > 0 ? 'text-blue-600' : 'text-amber-600',
            ]"
          >
            {{ data.difference > 0 ? `+${data.difference}` : data.difference }}
          </span>
        </template>
      </Column>
      <Column header="Đánh giá" style="width: 140px" class="text-right">
        <template #body="{ data }">
          <Tag :value="getStatusLabel(data.evaluationStatus)" :severity="getStatusSeverity(data.evaluationStatus)" />
        </template>
      </Column>
    </DataTable>
  </div>
</template>
