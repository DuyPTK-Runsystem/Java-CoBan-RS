<script setup lang="ts">
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import type { SubjectTeachingAssignment } from '@/types/assignment'
import type { SchoolClass, ClassSubject, Subject } from '@/types/academic'
const props = withDefaults(defineProps<{ assignments?: SubjectTeachingAssignment[]; classes?: SchoolClass[]; classSubjects?: ClassSubject[]; subjects?: Subject[]; loading?: boolean }>(), { assignments: () => [], classes: () => [], classSubjects: () => [], subjects: () => [], loading: false })
function className(id: number): string { const cs = props.classSubjects.find((x) => x.id === id); return props.classes.find((x) => x.id === cs?.classId)?.classCode ?? `Lớp #${cs?.classId ?? id}` }
function subjectName(id: number): string { const cs = props.classSubjects.find((x) => x.id === id); return props.subjects.find((x) => x.id === cs?.subjectId)?.name ?? `Môn #${cs?.subjectId ?? id}` }
</script>
<template><section class="content-surface"><div class="section-heading"><div><h2>Lịch giảng dạy theo giáo viên</h2><p class="section-caption">Các phân công GVBM đang hoạt động của giáo viên đã chọn.</p></div></div><div class="table-shell catalog-table"><DataTable :value="props.assignments" :loading="props.loading" data-key="id" striped-rows responsive-layout="scroll"><template #empty><div class="empty-state"><i class="pi pi-calendar" aria-hidden="true" /><strong>Chưa có phân công</strong></div></template><Column header="Lớp"><template #body="slot">{{ className(slot.data.classSubjectId) }}</template></Column><Column header="Môn học"><template #body="slot">{{ subjectName(slot.data.classSubjectId) }}</template></Column><Column header="Hiệu lực từ"><template #body="slot">{{ slot.data.validFrom }}</template></Column><Column header="Đến"><template #body="slot">{{ slot.data.validTo || 'Nay' }}</template></Column><Column header="Trạng thái"><template #body="slot">{{ slot.data.status }}</template></Column></DataTable></div></section></template>
