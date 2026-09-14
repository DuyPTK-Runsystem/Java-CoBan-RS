<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Paginator from 'primevue/paginator'
import FormAlert from '@/components/common/FormAlert.vue'
import { listLessonLogRevisions } from '@/services/lessonLogApi'
import { useAuthSession } from '@/composables/useAuthSession'
import { extractApiErrorMessage } from '@/types/api'
import type { LessonLogEntry, LessonLogRevision } from '@/types/lessonLog'

const props = defineProps<{ visible: boolean; entry: LessonLogEntry | null }>()
const emit = defineEmits<{ 'update:visible': [boolean] }>()
const { requireAccessToken } = useAuthSession(); const revisions = ref<LessonLogRevision[]>([]); const loading = ref(false); const error = ref(''); const page = ref(0); const totalItems = ref(0)
async function load(): Promise<void> { if (!props.entry) return; const token = requireAccessToken(); if (!token) return; loading.value = true; error.value = ''; try { const result = await listLessonLogRevisions(props.entry.entryId, page.value, 10, token); revisions.value = result.result; totalItems.value = result.meta.totalItems } catch (e) { error.value = extractApiErrorMessage(e, 'Không thể tải lịch sử sổ đầu bài') } finally { loading.value = false } }
watch(() => [props.visible, props.entry?.entryId], ([visible]) => { if (visible) { page.value = 0; void load() } }); onMounted(() => { if (props.visible) void load() })
</script>
<template>
  <Dialog :visible="props.visible" modal header="Lịch sử sổ đầu bài" :style="{ width: 'min(760px, 96vw)' }" @update:visible="emit('update:visible', $event)"><div class="audit-content"><FormAlert v-if="error" tone="error" :message="error" /><p v-if="loading">Đang tải lịch sử…</p><p v-else-if="!revisions.length">Chưa có bản ghi lịch sử.</p><ol v-else class="audit-list"><li v-for="item in revisions" :key="item.revisionId"><div><strong>{{ item.action }}</strong><span>{{ item.actorName }} · {{ item.createdAt }}</span></div><p v-if="item.reason">Lý do: {{ item.reason }}</p></li></ol><Paginator v-if="totalItems > 10" :first="page * 10" :rows="10" :total-records="totalItems" @page="(event) => { page = event.page; void load() }" /><div class="dialog-actions"><Button label="Đóng" text @click="emit('update:visible', false)" /></div></div></Dialog>
</template>
<style scoped>.audit-content{display:grid;gap:12px}.audit-content>p{color:#64748b}.audit-list{margin:0;padding-left:22px;display:grid;gap:10px}.audit-list li{padding:10px;background:#f8fafc;border-radius:8px}.audit-list div{display:flex;justify-content:space-between;gap:10px}.audit-list span,.audit-list p{font-size:13px;color:#64748b}.audit-list p{margin:6px 0 0}.dialog-actions{display:flex;justify-content:flex-end}</style>
