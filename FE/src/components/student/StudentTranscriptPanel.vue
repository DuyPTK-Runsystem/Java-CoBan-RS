<script setup lang="ts">
import Button from 'primevue/button'
import Select from 'primevue/select'

import EmptyState from '@/components/common/EmptyState.vue'
import TranscriptAnnualTable from '@/components/transcript/TranscriptAnnualTable.vue'
import TranscriptStatusCard from '@/components/transcript/TranscriptStatusCard.vue'
import TranscriptTermTable from '@/components/transcript/TranscriptTermTable.vue'
import type { AcademicYear, Semester } from '@/types/academic'
import type {
  ResStudentAnnualTranscriptDTO,
  ResStudentTermTranscriptDTO,
  ResTranscriptCalculationStatusDTO,
} from '@/types/transcript'

export type TranscriptSubTab = 'term' | 'annual'

const props = defineProps<{
  academicYears: AcademicYear[]
  semesters: Semester[]
  selectedAcademicYearId: number | null
  selectedSemesterId: number | null
  activeSubTab: TranscriptSubTab
  termTranscript: ResStudentTermTranscriptDTO | null
  annualTranscript: ResStudentAnnualTranscriptDTO | null
  transcriptStatus: ResTranscriptCalculationStatusDTO | null
  studentName?: string
  loading: boolean
  canRecalculate: boolean
  recalculating: boolean
}>()

const emit = defineEmits<{
  (event: 'update:selectedAcademicYearId', value: number | null): void
  (event: 'update:selectedSemesterId', value: number | null): void
  (event: 'update:activeSubTab', value: TranscriptSubTab): void
  (event: 'refresh'): void
  (event: 'recalculate'): void
}>()
</script>

<template>
  <div class="tab-stack">
    <section class="content-surface filter-bar">
      <div class="filter-controls">
        <div class="filter-item">
          <label for="tc-year">Năm học</label>
          <Select
            id="tc-year"
            :model-value="props.selectedAcademicYearId"
            :options="props.academicYears"
            option-label="code"
            option-value="id"
            placeholder="Chọn năm học"
            @update:model-value="emit('update:selectedAcademicYearId', $event)"
          />
        </div>
        <div v-if="props.activeSubTab === 'term'" class="filter-item">
          <label for="tc-sem">Học kỳ</label>
          <Select
            id="tc-sem"
            :model-value="props.selectedSemesterId"
            :options="props.semesters"
            option-label="name"
            option-value="id"
            placeholder="Chọn học kỳ"
            @update:model-value="emit('update:selectedSemesterId', $event)"
          />
        </div>
        <div class="filter-actions">
          <Button label="Làm mới" icon="pi pi-refresh" severity="secondary" :loading="props.loading" @click="emit('refresh')" />
          <Button
            v-if="props.canRecalculate"
            label="Yêu cầu tính lại điểm"
            icon="pi pi-cog"
            severity="warning"
            :loading="props.recalculating"
            @click="emit('recalculate')"
          />
        </div>
      </div>
    </section>

    <div class="sub-tab-strip">
      <Button
        label="Bảng điểm Học kỳ"
        :severity="props.activeSubTab === 'term' ? 'primary' : 'secondary'"
        :outlined="props.activeSubTab !== 'term'"
        @click="emit('update:activeSubTab', 'term')"
      />
      <Button
        label="Bảng điểm Cả năm"
        :severity="props.activeSubTab === 'annual' ? 'primary' : 'secondary'"
        :outlined="props.activeSubTab !== 'annual'"
        @click="emit('update:activeSubTab', 'annual')"
      />
    </div>

    <TranscriptStatusCard
      v-if="props.transcriptStatus"
      :status="props.transcriptStatus"
      :student-name="props.studentName"
      :loading="props.loading"
      @refresh="emit('refresh')"
    />

    <section class="content-surface">
      <div v-if="props.activeSubTab === 'term'">
        <TranscriptTermTable
          v-if="props.termTranscript?.subjects?.length"
          :subjects="props.termTranscript.subjects"
          :dtbhk="props.termTranscript.dtbhk"
        />
        <EmptyState
          v-else
          icon="pi pi-file-excel"
          heading="Chưa có dữ liệu bảng điểm học kỳ"
          message="Học sinh chưa có bảng điểm chính thức cho học kỳ đã chọn."
        />
      </div>

      <div v-else>
        <TranscriptAnnualTable
          v-if="props.annualTranscript?.subjects?.length"
          :subjects="props.annualTranscript.subjects"
          :regular-dtbcn="props.annualTranscript.regularDtbcn"
          :final-dtbcn="props.annualTranscript.finalDtbcn"
        />
        <EmptyState
          v-else
          icon="pi pi-file-excel"
          heading="Chưa có dữ liệu bảng điểm cả năm"
          message="Học sinh chưa có bảng điểm chính thức cho năm học đã chọn."
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.tab-stack {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}
.filter-bar {
  padding: 1rem;
}
.filter-controls {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 1rem;
}
.filter-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 180px;
}
.filter-item label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #334155;
}
.filter-actions {
  display: flex;
  gap: 0.5rem;
}
.sub-tab-strip {
  display: flex;
  gap: 0.5rem;
}
</style>
