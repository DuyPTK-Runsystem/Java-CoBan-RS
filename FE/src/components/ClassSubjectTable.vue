<script setup lang="ts">
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'

import StatusTag from '@/components/StatusTag.vue'
import type { ClassSubject, ClassSubjectStatus, Semester, Subject, SubjectType } from '@/types/academic'

const props = withDefaults(defineProps<{ classSubjects?: ClassSubject[]; subjects?: Subject[]; semesters?: Semester[]; loading?: boolean; readOnly?: boolean }>(), { classSubjects: () => [], subjects: () => [], semesters: () => [], loading: false, readOnly: false })
const emit = defineEmits<{ changeStatus: [classSubject: ClassSubject] }>()
const statusLabels: Record<ClassSubjectStatus, string> = { ACTIVE: 'Đang hoạt động', INACTIVE: 'Ngừng hoạt động', COMPLETED: 'Đã hoàn tất' }
const statusSeverities: Record<ClassSubjectStatus, 'success' | 'secondary' | 'contrast'> = { ACTIVE: 'success', INACTIVE: 'secondary', COMPLETED: 'contrast' }
const subjectTypeLabels: Record<SubjectType, string> = { ACADEMIC: 'CHÍNH KHÓA', SKILL: 'KỸ NĂNG' }

function subjectFor(item: ClassSubject): Subject | undefined { return props.subjects.find((subject) => subject.id === item.subjectId) }
function subjectName(item: ClassSubject): string { return subjectFor(item)?.name ?? `Môn #${item.subjectId}` }
function subjectCode(item: ClassSubject): string { return subjectFor(item)?.code ?? 'Chưa resolve' }
function subjectType(item: ClassSubject): SubjectType | null { return subjectFor(item)?.subjectType ?? null }
function semesterLabel(semesterId: number): string { return props.semesters.find((semester) => semester.id === semesterId)?.code ?? `HK #${semesterId}` }
</script>

<template>
  <div class="table-shell catalog-table">
    <DataTable :value="props.classSubjects" :loading="props.loading" data-key="id" striped-rows responsive-layout="scroll">
      <template #empty><div class="empty-state"><i class="pi pi-book" aria-hidden="true" /><strong>Chưa có môn trong lớp</strong><p>Chọn đủ năm học, lớp và học kỳ để tải danh sách.</p></div></template>
      <Column header="#" style="width: 4rem"><template #body="slotProps">{{ slotProps.index + 1 }}</template></Column>
      <Column header="Môn học"><template #body="slotProps"><div class="primary-cell"><strong>{{ subjectName(slotProps.data) }}</strong><span>{{ subjectCode(slotProps.data) }} · subjectId {{ slotProps.data.subjectId }}</span></div></template></Column>
      <Column header="Loại"><template #body="slotProps"><StatusTag v-if="subjectType(slotProps.data)" :label="subjectTypeLabels[subjectType(slotProps.data) || 'ACADEMIC']" :severity="subjectType(slotProps.data) === 'ACADEMIC' ? 'success' : 'secondary'" /><span v-else class="table-action-note">Thiếu dữ liệu môn</span></template></Column>
      <Column header="Học kỳ"><template #body="slotProps">{{ semesterLabel(slotProps.data.semesterId) }}</template></Column>
      <Column header="Trạng thái lớp-môn"><template #body="slotProps"><StatusTag :label="statusLabels[slotProps.data.status]" :severity="statusSeverities[slotProps.data.status]" /></template></Column>
      <Column header="Thao tác" style="width: 12rem"><template #body="slotProps"><Button v-if="!props.readOnly && slotProps.data.status !== 'COMPLETED'" icon="pi pi-pencil" text rounded aria-label="Đổi trạng thái lớp-môn" title="Đổi trạng thái lớp-môn" @click="emit('changeStatus', slotProps.data)" /><span v-else class="table-action-note">Chỉ xem</span></template></Column>
    </DataTable>
  </div>
</template>
