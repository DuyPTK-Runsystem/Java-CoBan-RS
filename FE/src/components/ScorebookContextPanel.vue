<script setup lang="ts">
import { computed } from 'vue'
import Select from 'primevue/select'

import type { AcademicYear, ClassSubject, SchoolClass, Semester, Subject } from '@/types/academic'

const props = defineProps<{
  academicYears: AcademicYear[]
  semesters: Semester[]
  classes: SchoolClass[]
  classSubjects: ClassSubject[]
  subjects: Subject[]
  academicYearId: number | null
  semesterId: number | null
  classId: number | null
  classSubjectId: number | null
  loading?: boolean
}>()

defineEmits<{
  'update:academicYearId': [value: number | null]
  'update:semesterId': [value: number | null]
  'update:classId': [value: number | null]
  'update:classSubjectId': [value: number | null]
}>()

const classOptions = computed(() => props.classes.map((schoolClass) => ({
  ...schoolClass,
  label: schoolClass.className
    ? `${schoolClass.classCode} · ${schoolClass.className}`
    : schoolClass.classCode,
})))

const subjectById = computed(() => new Map(props.subjects.map((subject) => [subject.id, subject])))

const classSubjectOptions = computed(() => props.classSubjects.map((classSubject) => {
  const subject = subjectById.value.get(classSubject.subjectId)
  return {
    ...classSubject,
    label: subject ? `${subject.code} · ${subject.name}` : `Môn học #${classSubject.subjectId}`,
  }
}))
</script>

<template>
  <section class="content-surface scorebook-context-panel">
    <div class="section-heading">
      <div>
        <h2>Context học vụ</h2>
        <p class="section-caption">Năm học → học kỳ → lớp → môn học.</p>
      </div>
      <span class="field-hint">Quyền thao tác do backend quyết định</span>
    </div>
    <div class="search-grid">
      <div class="field-group">
        <label for="scorebook-year">Năm học</label>
        <Select id="scorebook-year" :model-value="academicYearId" :options="academicYears" option-label="code" option-value="id" placeholder="Chọn năm học" :loading="loading" fluid @update:model-value="$emit('update:academicYearId', $event)" />
      </div>
      <div class="field-group">
        <label for="scorebook-semester">Học kỳ</label>
        <Select id="scorebook-semester" :model-value="semesterId" :options="semesters" option-label="name" option-value="id" placeholder="Chọn học kỳ" :disabled="academicYearId === null" fluid @update:model-value="$emit('update:semesterId', $event)" />
      </div>
      <div class="field-group">
        <label for="scorebook-class">Lớp</label>
        <Select id="scorebook-class" :model-value="classId" :options="classOptions" option-label="label" option-value="id" placeholder="Chọn lớp" :disabled="academicYearId === null" fluid @update:model-value="$emit('update:classId', $event)" />
      </div>
      <div class="field-group">
        <label for="scorebook-class-subject">Môn học</label>
        <Select id="scorebook-class-subject" :model-value="classSubjectId" :options="classSubjectOptions" option-label="label" option-value="id" placeholder="Chọn môn học" :disabled="classSubjectOptions.length === 0" fluid @update:model-value="$emit('update:classSubjectId', $event)" />
      </div>
    </div>
  </section>
</template>

