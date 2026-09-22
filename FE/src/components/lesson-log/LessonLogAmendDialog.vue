<script setup lang="ts">
import { ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Textarea from 'primevue/textarea'
import LessonLogEntryDialog from './LessonLogEntryDialog.vue'
import type { LessonLogEntry, LessonLogRequestFields } from '@/types/lessonLog'

const props = defineProps<{ visible: boolean; entry: LessonLogEntry | null; saving?: boolean }>()
const emit = defineEmits<{ 'update:visible': [boolean]; amend: [LessonLogRequestFields, string] }>()
const reason = ref('')
const formVisible = ref(false)
const fields = ref<LessonLogRequestFields>({})
watch(() => props.visible, (visible) => { if (visible) { reason.value = ''; formVisible.value = false } })
function capture(payload: LessonLogRequestFields): void { fields.value = payload; formVisible.value = false }
function submit(): void { if (reason.value.trim() && props.entry) emit('amend', fields.value, reason.value.trim()) }
</script>

<template>
  <Dialog :visible="props.visible" modal header="Điều chỉnh sổ đầu bài" :style="{ width: 'min(580px, 96vw)' }" @update:visible="emit('update:visible', $event)">
    <div v-if="props.entry" class="amend-form"><p>Điều chỉnh sẽ lưu bản trước vào lịch sử và có thể khiến tuần đã ký chuyển sang <strong>Cần ký lại</strong>.</p><Button label="Mở nội dung để chỉnh" icon="pi pi-pencil" severity="secondary" @click="formVisible = true" /><label for="amend-reason">Lý do điều chỉnh <Textarea id="amend-reason" v-model="reason" rows="4" maxlength="500" required /></label><div class="dialog-actions"><Button label="Hủy" text @click="emit('update:visible', false)" /><Button label="Lưu điều chỉnh" icon="pi pi-save" :loading="props.saving" :disabled="!reason.trim() || !Object.keys(fields).length" @click="submit" /></div><LessonLogEntryDialog v-model:visible="formVisible" :entry="props.entry" :read-only="false" @save-draft="capture" /></div>
  </Dialog>
</template>
<style scoped>.amend-form{display:grid;gap:14px}.amend-form>p{margin:0;color:#475569}.amend-form label{display:grid;gap:6px;font-weight:600}.dialog-actions{display:flex;justify-content:flex-end;gap:8px}</style>
