<script setup lang="ts">
import Dialog from 'primevue/dialog'
import StatusTag from '@/components/StatusTag.vue'
import type { HomeroomAssignment } from '@/types/assignment'
import type { Teacher } from '@/types/teacher'
const props = withDefaults(defineProps<{ visible?: boolean; assignments?: HomeroomAssignment[]; teachers?: Teacher[]; loading?: boolean }>(), { visible: false, assignments: () => [], teachers: () => [], loading: false })
const emit = defineEmits<{ 'update:visible': [value: boolean] }>()
function teacherName(id: number): string { const t = props.teachers.find((item) => item.id === id); return t ? `${t.teacherCode} - ${t.teacherName}` : `Giáo viên #${id}` }
</script>
<template><Dialog :visible="props.visible" modal header="Lịch sử phân công GVCN" :style="{ width: 'min(100% - 2rem, 640px)' }" @update:visible="emit('update:visible', $event)"><div v-if="props.loading" class="page-state page-state-loading"><i class="pi pi-spin pi-spinner" aria-hidden="true" />Đang tải...</div><div v-else-if="props.assignments.length === 0" class="empty-state"><i class="pi pi-history" aria-hidden="true" /><strong>Chưa có lịch sử</strong></div><div v-else class="history-list"><article v-for="item in props.assignments" :key="item.id" class="history-card"><div class="section-heading"><strong>{{ teacherName(item.teacherId) }}</strong><StatusTag :label="item.status" :severity="item.status === 'ACTIVE' ? 'success' : 'secondary'" /></div><span>{{ item.validFrom }} → {{ item.validTo || 'nay' }}</span></article></div></Dialog></template>
