<script setup lang="ts">
import { computed } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'

import ServerPagination from '@/components/ServerPagination.vue'
import {
  type ScoreGridColumn,
  type StudentScore,
  type StudentScoreGrid,
  compareAssessmentColumns,
} from '@/types/scorebook'

const props = defineProps<{ grid: StudentScoreGrid | null; loading?: boolean; readOnly?: boolean }>()
const emit = defineEmits<{
  edit: [student: StudentScoreGrid['students'][number], column: ScoreGridColumn, score: StudentScore | null]
  'bulk-edit': [column: ScoreGridColumn]
  'page-change': [page: number, size: number]
}>()

const displayedColumns = computed(() => [...(props.grid?.columns ?? [])].sort(compareAssessmentColumns))
const assessmentTypeLabels: Record<string, string> = { KTTT: 'Thường xuyên', 'KTĐK': 'Giữa kỳ', KTCK: 'Cuối kỳ' }

function scoreFor(student: StudentScoreGrid['students'][number], column: ScoreGridColumn): StudentScore | null {
  return student.scores[String(column.columnId)] ?? null
}

function label(score: StudentScore | null): string {
  if (!score) return 'Chưa nhập'
  if (score.scoreStatus !== 'SCORED') return score.scoreStatus
  return score.scoreValue === null ? 'Chưa nhập' : score.scoreValue.toFixed(1)
}
</script>

<template>
  <section class="content-surface score-grid-panel">
    <div class="section-heading">
      <div>
        <h2>Bảng điểm</h2>
      </div>
    </div>
    <div v-if="props.loading" class="page-state page-state-loading" role="status">
      <i class="pi pi-spin pi-spinner" aria-hidden="true" /> Đang tải bảng điểm...
    </div>
    <div v-else-if="!props.grid || props.grid.students.length === 0" class="empty-state">
      <i class="pi pi-inbox" aria-hidden="true" />
      <p>Chưa có học sinh trong trang này.</p>
    </div>
    <div v-else class="scorebook-table-scroll">
      <DataTable :value="props.grid.students" striped-rows scrollable responsive-layout="scroll" class="scorebook-grid-table">
        <Column field="studentCode" header="Mã HS" frozen />
        <Column field="studentName" header="Họ và tên" frozen />
        <Column v-for="column in displayedColumns" :key="column.columnId">
          <template #header>
            <div class="scorebook-column-header">
              <span>{{ assessmentTypeLabels[column.assessmentType] ?? column.assessmentType }} · {{ column.columnName || `Cột ${column.columnNo}` }}</span>
              <Button aria-label="Nhập điểm cả cột" title="Nhập điểm cả cột" icon="pi pi-list" text size="small" :disabled="props.readOnly" @click="emit('bulk-edit', column)" />
            </div>
          </template>
          <template #body="slot">
            <button class="scorebook-score-cell" type="button" :aria-label="`${slot.data.studentName} · ${column.columnName || `Cột ${column.columnNo}`}`" @click="emit('edit', slot.data, column, scoreFor(slot.data, column))">
              {{ label(scoreFor(slot.data, column)) }}
            </button>
          </template>
        </Column>
      </DataTable>
    </div>
    <div v-if="props.grid" class="scorebook-pagination-bar">
      <span class="field-hint">{{ props.grid.totalElements }} học sinh</span>
      <ServerPagination
        :page="props.grid.page"
        :page-size="props.grid.size"
        :total-records="props.grid.totalElements"
        @page-change="(nextPage, nextSize) => emit('page-change', nextPage, nextSize)"
      />
    </div>
  </section>
</template>

<style scoped>
.score-grid-panel { min-width: 0; }
.scorebook-table-scroll { max-width: 100%; overflow-x: auto; }
.score-grid-table { min-width: 760px; }
.scorebook-column-header { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.scorebook-pagination-bar { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-top: 14px; }
.scorebook-score-cell { min-width: 78px; padding: 7px 9px; border: 0; border-radius: 6px; background: transparent; cursor: pointer; text-align: left; }
.scorebook-score-cell:hover:not(:disabled), .scorebook-score-cell:focus-visible { outline: 2px solid var(--primary); background: #eef2ff; }
.scorebook-score-cell:disabled { cursor: default; }
@media (max-width: 680px) { .scorebook-pagination-bar { align-items: flex-start; flex-direction: column; } }
</style>
