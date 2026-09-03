<script setup lang="ts">
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import type { ScorebookStatus } from '@/types/scorebook'

const props = defineProps<{ scorebook: { id: number; status: ScorebookStatus } | null; loading?: boolean }>()
defineEmits<{ open: []; publish: []; reload: [] }>()
</script>

<template>
  <section class="content-surface">
    <div class="section-heading"><div><h2>Lifecycle sổ điểm</h2><p class="section-caption">{{ props.scorebook ? `Scorebook #${props.scorebook.id}` : 'Chưa tạo scorebook' }}</p></div><Tag v-if="props.scorebook" :value="props.scorebook.status" /></div>
    <div v-if="props.scorebook" class="page-heading-actions"><Button label="Tải lại" icon="pi pi-refresh" outlined :loading="props.loading" @click="$emit('reload')" /><Button :label="props.scorebook.status === 'PUBLISHED' ? 'Mở lại sổ' : 'Mở sổ'" icon="pi pi-lock-open" :disabled="props.scorebook.status !== 'DRAFT' && props.scorebook.status !== 'PUBLISHED'" :loading="props.loading" @click="$emit('open')" /><Button label="Công bố" icon="pi pi-check" severity="success" :disabled="props.scorebook.status !== 'OPEN'" :loading="props.loading" @click="$emit('publish')" /></div>
    <p v-else class="field-hint">Chọn môn học để tìm sổ điểm hiện có hoặc tạo mới khi được cấp quyền.</p>
    <p v-if="props.scorebook?.status === 'PUBLISHED'" class="field-hint">Sổ điểm đã công bố; việc nhập và sửa điểm vẫn do backend kiểm tra.</p>
    <p v-if="props.scorebook?.status === 'CLOSED'" class="field-hint">Sổ điểm đang ở chế độ chỉ đọc.</p>
  </section>
</template>
