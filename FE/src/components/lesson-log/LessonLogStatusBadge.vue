<script setup lang="ts">
import Tag from 'primevue/tag'
import type { LessonLogStatus, WeeklyReviewStatus } from '@/types/lessonLog'
const props = defineProps<{ status: LessonLogStatus | WeeklyReviewStatus }>()
const labels: Record<string, string> = { DRAFT: 'Nháp', SUBMITTED: 'Đã nộp', REVIEWED: 'Đã duyệt', AMENDED: 'Đã điều chỉnh', UNSIGNED: 'Chưa ký', SIGNED: 'Đã ký', STALE: 'Cần ký lại' }
const severity: Record<string, 'secondary' | 'info' | 'success' | 'warn'> = { DRAFT: 'secondary', SUBMITTED: 'info', REVIEWED: 'success', AMENDED: 'warn', UNSIGNED: 'secondary', SIGNED: 'success', STALE: 'warn' }
</script>
<template><Tag :value="labels[props.status] ?? props.status" :severity="severity[props.status] ?? 'secondary'" /></template>
