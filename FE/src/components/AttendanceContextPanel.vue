<script setup lang="ts">
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'

import type { AcademicYear, SchoolClass, Semester } from '@/types/academic'
import type { AttendanceSessionPeriod } from '@/types/attendance'

const props = withDefaults(defineProps<{
  academicYears?: AcademicYear[]
  semesters?: Semester[]
  classes?: SchoolClass[]
  academicYearId?: number | null
  semesterId?: number | null
  classId?: number | null
  attendanceDate?: string
  sessionPeriod?: AttendanceSessionPeriod
  loading?: boolean
  calendarLoading?: boolean
  calendarStatus?: 'SCHEDULED' | 'NO_CLASS' | 'UNKNOWN'
  calendarMessage?: string
  showOpen?: boolean
  openLoading?: boolean
}>(), {
  academicYears: () => [],
  semesters: () => [],
  classes: () => [],
  academicYearId: null,
  semesterId: null,
  classId: null,
  attendanceDate: '',
  sessionPeriod: 'MORNING',
  loading: false,
  calendarLoading: false,
  calendarStatus: 'UNKNOWN',
  calendarMessage: '',
  showOpen: false,
  openLoading: false,
})

const emit = defineEmits<{
  'update:academicYearId': [value: number | null]
  'update:semesterId': [value: number | null]
  'update:classId': [value: number | null]
  'update:attendanceDate': [value: string]
  'update:sessionPeriod': [value: AttendanceSessionPeriod]
  open: []
}>()

const periodOptions: Array<{ label: string; value: AttendanceSessionPeriod }> = [
  { label: 'Buổi sáng', value: 'MORNING' },
  { label: 'Buổi chiều', value: 'AFTERNOON' },
]

const calendarLabel: Record<'SCHEDULED' | 'NO_CLASS' | 'UNKNOWN', string> = {
  SCHEDULED: 'Ngày học hợp lệ',
  NO_CLASS: 'Không có lớp trong lịch',
  UNKNOWN: 'Chưa kiểm tra lịch học',
}
</script>

<template>
  <section class="content-surface attendance-context-panel">
    <div class="section-heading">
      <div>
        <h2>Context điểm danh</h2>
        <p class="section-caption">Chọn đủ năm học, học kỳ, lớp, ngày và buổi trước khi mở session.</p>
      </div>
      <span class="field-hint">Quyền thao tác do backend quyết định</span>
    </div>
    <div class="attendance-context-form">
      <div class="field-group">
        <label for="attendance-year">Năm học</label>
        <Select
          id="attendance-year"
          :model-value="props.academicYearId"
          :options="props.academicYears"
          option-label="code"
          option-value="id"
          placeholder="Chọn năm học"
          :loading="props.loading"
          fluid
          @update:model-value="emit('update:academicYearId', $event)"
        />
      </div>
      <div class="field-group">
        <label for="attendance-semester">Học kỳ</label>
        <Select
          id="attendance-semester"
          :model-value="props.semesterId"
          :options="props.semesters"
          option-label="name"
          option-value="id"
          placeholder="Chọn học kỳ"
          :disabled="!props.academicYearId"
          fluid
          @update:model-value="emit('update:semesterId', $event)"
        />
      </div>
      <div class="field-group">
        <label for="attendance-class">Lớp</label>
        <Select
          id="attendance-class"
          :model-value="props.classId"
          :options="props.classes"
          option-label="classCode"
          option-value="id"
          placeholder="Chọn lớp"
          :disabled="!props.academicYearId"
          fluid
          @update:model-value="emit('update:classId', $event)"
        />
      </div>
      <div class="field-group">
        <label for="attendance-date">Ngày điểm danh</label>
        <InputText
          id="attendance-date"
          :model-value="props.attendanceDate"
          type="date"
          :disabled="!props.semesterId"
          fluid
          @update:model-value="emit('update:attendanceDate', $event ?? '')"
        />
      </div>
      <div class="field-group">
        <label for="attendance-period">Buổi</label>
        <Select
          id="attendance-period"
          :model-value="props.sessionPeriod"
          :options="periodOptions"
          option-label="label"
          option-value="value"
          :disabled="!props.attendanceDate"
          fluid
          @update:model-value="emit('update:sessionPeriod', $event)"
        />
      </div>
      <Button
        v-if="props.showOpen"
        class="attendance-open-action"
        label="Mở buổi điểm danh"
        icon="pi pi-external-link"
        :loading="props.openLoading"
        @click="emit('open')"
      />
    </div>
    <div :class="['attendance-calendar-banner', `attendance-calendar-${props.calendarStatus.toLocaleLowerCase()}`]" role="status" aria-live="polite">
      <i :class="props.calendarStatus === 'SCHEDULED' ? 'pi pi-check-circle' : props.calendarStatus === 'NO_CLASS' ? 'pi pi-ban' : 'pi pi-info-circle'" aria-hidden="true" />
      <div>
        <strong>{{ calendarLabel[props.calendarStatus] }}</strong>
        <span>{{ props.calendarLoading ? 'Đang kiểm tra lịch học...' : props.calendarMessage || 'Chọn context để kiểm tra ngày học hợp lệ.' }}</span>
      </div>
    </div>
  </section>
</template>
