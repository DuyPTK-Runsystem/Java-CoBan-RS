<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import LessonLogStatusBadge from './LessonLogStatusBadge.vue'
import type { LessonGrade, WeeklyReview } from '@/types/lessonLog'
const props = defineProps<{ visible: boolean; review: WeeklyReview }>(); const emit = defineEmits<{ 'update:visible': [boolean]; sign: [string, LessonGrade | null, string] }>(); const comment = ref(''); const reason = ref(''); const grade = ref<LessonGrade | null>(null); const grades = [{ label: 'A · Tốt', value: 'A' }, { label: 'B · Khá', value: 'B' }, { label: 'C · Trung bình', value: 'C' }, { label: 'D · Yếu', value: 'D' }]; watch(() => props.visible, (visible) => { if (visible) { comment.value = props.review.comment ?? ''; grade.value = props.review.grade; reason.value = '' } })
const requiresReason = computed(() => props.review.requiresReason || props.review.status === 'STALE')
function sign(): void { if (requiresReason.value && !reason.value.trim()) return; emit('sign', comment.value.trim(), grade.value, reason.value.trim()) }
</script>
<template><Dialog :visible="props.visible" modal header="Ký tổng kết tuần" :style="{ width: 'min(560px, 96vw)' }" @update:visible="emit('update:visible', $event)"><div class="review-form"><LessonLogStatusBadge :status="props.review.status" /><ul v-if="props.review.blockedReasons.length"><li v-for="item in props.review.blockedReasons" :key="item">{{ item }}</li></ul><label for="weekly-comment">Nhận xét tuần <Textarea id="weekly-comment" v-model="comment" rows="4" maxlength="4000" /></label><label for="weekly-grade">Xếp loại <Select id="weekly-grade" v-model="grade" :options="grades" option-label="label" option-value="value" placeholder="Chọn xếp loại" /></label><label v-if="requiresReason" for="weekly-reason">Lý do ký lại <span class="required-mark" aria-hidden="true">*</span><Textarea id="weekly-reason" v-model="reason" rows="3" maxlength="500" required aria-required="true" /></label><div class="dialog-actions"><Button label="Đóng" severity="secondary" text @click="emit('update:visible', false)" /><Button label="Ký tuần" :disabled="!props.review.canSignWeek || (requiresReason && !reason.trim())" @click="sign" /></div></div></Dialog></template>
<style scoped>.review-form{display:grid;gap:14px}.review-form label{display:grid;gap:6px;font-weight:600}.required-mark{color:var(--error,#ba1a1a)}.review-form ul{margin:0;padding-left:18px;color:#9a3412}.dialog-actions{display:flex;justify-content:flex-end;gap:8px}</style>
