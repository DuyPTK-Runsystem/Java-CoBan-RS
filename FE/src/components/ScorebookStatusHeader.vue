<script setup lang="ts">
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import type { ScorebookStatus } from '@/types/scorebook'

const props = defineProps<{ scorebook: { id: number; status: ScorebookStatus } | null; loading?: boolean }>()
defineEmits<{ open: []; publish: []; reload: [] }>()

const statusLabels: Record<ScorebookStatus, string> = {
  DRAFT: 'Bản nháp',
  OPEN: 'Đang nhập điểm',
  PUBLISHED: 'Đã công bố',
  CLOSED: 'Đã khóa',
}
</script>

<template>
  <section class="content-surface">
    <div class="section-heading"><div><h2>Trạng thái sổ điểm</h2></div><Tag v-if="props.scorebook" :value="statusLabels[props.scorebook.status]" /></div>
    <div v-if="props.scorebook" class="page-heading-actions"><Button label="Tải lại" icon="pi pi-refresh" outlined :loading="props.loading" @click="$emit('reload')" /><Button v-if="props.scorebook.status === 'DRAFT' || props.scorebook.status === 'PUBLISHED'" :label="props.scorebook.status === 'PUBLISHED' ? 'Mở lại sổ' : 'Mở sổ'" icon="pi pi-lock-open" :loading="props.loading" @click="$emit('open')" /><Button v-if="props.scorebook.status === 'OPEN'" label="Công bố" icon="pi pi-check" severity="success" :loading="props.loading" @click="$emit('publish')" /></div>
    <p v-else class="field-hint">Chọn môn học để xem sổ điểm hiện có hoặc tạo mới.</p>
    <p v-if="props.scorebook?.status === 'PUBLISHED'" class="field-hint">Sổ điểm đã công bố. Bạn vẫn có thể nhập hoặc sửa điểm.</p>
    <p v-if="props.scorebook?.status === 'CLOSED'" class="field-hint">Sổ điểm đang ở chế độ chỉ đọc.</p>
  </section>
</template>
