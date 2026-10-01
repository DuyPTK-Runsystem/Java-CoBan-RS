<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Paginator from 'primevue/paginator'
import FormAlert from '@/components/common/FormAlert.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { listLessonLogRevisions } from '@/services/lessonLogApi'
import { useAuthSession } from '@/composables/useAuthSession'
import { extractApiErrorMessage } from '@/types/api'
import type { LessonLogEntry, LessonLogRevision } from '@/types/lessonLog'

const props = defineProps<{
  visible: boolean
  entry: LessonLogEntry | null
}>()

const emit = defineEmits<{
  'update:visible': [boolean]
}>()

const { requireAccessToken } = useAuthSession()
const revisions = ref<LessonLogRevision[]>([])
const loading = ref(false)
const error = ref('')
const page = ref(0)
const totalItems = ref(0)

async function load(): Promise<void> {
  if (!props.entry) return
  const token = requireAccessToken()
  if (!token) return
  loading.value = true
  error.value = ''
  try {
    const result = await listLessonLogRevisions(props.entry.entryId, page.value, 10, token)
    revisions.value = result.result
    totalItems.value = result.meta.totalItems
  } catch (e) {
    error.value = extractApiErrorMessage(e, 'Không thể tải lịch sử sổ đầu bài')
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.visible, props.entry?.entryId],
  ([visible]) => {
    if (visible) {
      page.value = 0
      void load()
    }
  },
)

onMounted(() => {
  if (props.visible) void load()
})
</script>

<template>
  <Dialog
    :visible="props.visible"
    modal
    header="Lịch sử sổ đầu bài"
    :style="{ width: 'min(760px, 96vw)' }"
    @update:visible="emit('update:visible', $event)"
  >
    <div class="audit-content">
      <FormAlert v-if="error" tone="error" :message="error" />

      <div v-if="loading" class="page-state page-state-loading" role="status">
        <i class="pi pi-spin pi-spinner" aria-hidden="true" />
        <span>Đang tải lịch sử…</span>
      </div>

      <EmptyState
        v-else-if="!revisions.length"
        heading="Chưa có lịch sử"
        message="Bản ghi này chưa có thao tác chỉnh sửa hoặc duyệt nào."
      />

      <ol v-else class="audit-list">
        <li v-for="item in revisions" :key="item.revisionId" class="audit-item">
          <div class="audit-item-header">
            <strong>{{ item.action }}</strong>
            <span class="audit-meta">{{ item.actorName }} · {{ item.createdAt }}</span>
          </div>
          <p v-if="item.reason" class="audit-reason">
            <strong>Lý do:</strong> {{ item.reason }}
          </p>
        </li>
      </ol>

      <Paginator
        v-if="totalItems > 10"
        :first="page * 10"
        :rows="10"
        :total-records="totalItems"
        @page="
          (event) => {
            page = event.page
            void load()
          }
        "
      />

      <div class="dialog-actions">
        <Button label="Đóng" text severity="secondary" @click="emit('update:visible', false)" />
      </div>
    </div>
  </Dialog>
</template>

<style scoped>
.audit-content {
  display: grid;
  gap: 16px;
}

.audit-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}

.audit-item {
  padding: 12px 14px;
  background: var(--surface-container-low, #f2f4f6);
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.audit-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.audit-item-header strong {
  font-size: 14px;
  color: #1e293b;
}

.audit-meta {
  font-size: 12px;
  color: #64748b;
}

.audit-reason {
  margin: 8px 0 0;
  font-size: 13px;
  color: #475569;
  line-height: 1.4;
}

.audit-reason strong {
  color: #334155;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
  padding-top: 14px;
  border-top: 1px solid #e2e8f0;
}
</style>
