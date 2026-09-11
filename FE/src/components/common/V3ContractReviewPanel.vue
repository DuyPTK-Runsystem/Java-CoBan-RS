<script setup lang="ts">
import Button from 'primevue/button'

import type { V3ReviewContext, V3ReviewItem, V3ReviewState } from '@/types/v3Foundation'

const props = withDefaults(defineProps<{
  state: V3ReviewState
  context: V3ReviewContext
  items?: V3ReviewItem[]
}>(), {
  items: () => [],
})

const emit = defineEmits<{ retry: [] }>()

const stateCopy: Record<Exclude<V3ReviewState, 'ready'>, { heading: string; message: string }> = {
  loading: { heading: 'Đang tải dữ liệu', message: 'Vui lòng chờ trong giây lát.' },
  empty: { heading: 'Chưa có dữ liệu', message: 'Không có bản ghi phù hợp với điều kiện hiện tại.' },
  unauthorized: { heading: 'Phiên đăng nhập đã hết hạn', message: 'Hãy đăng nhập lại để tiếp tục.' },
  forbidden: { heading: 'Không đủ quyền xem', message: 'Tài khoản của bạn không có quyền xem nội dung này.' },
  'not-found': { heading: 'Không tìm thấy dữ liệu', message: 'Nội dung bạn yêu cầu có thể đã thay đổi hoặc không còn tồn tại.' },
  conflict: { heading: 'Dữ liệu đã thay đổi', message: 'Hãy tải lại để xem dữ liệu mới nhất trước khi tiếp tục.' },
}
</script>

<template>
  <section class="v3-contract-review" aria-labelledby="v3-contract-review-title">
    <div class="v3-contract-review__context">
      <span>{{ props.context.academicYear }}</span>
      <span>{{ props.context.semester }}</span>
      <span>{{ props.context.module }}</span>
    </div>
    <div class="v3-contract-review__heading">
      <div>
        <p class="v3-contract-review__eyebrow">Bản xem trước</p>
        <h2 id="v3-contract-review-title">Danh sách theo điều kiện</h2>
      </div>
      <span class="v3-contract-review__badge">Chưa phát hành</span>
    </div>

    <div v-if="props.state !== 'ready'" class="v3-contract-review__state" :data-state="props.state" role="status" aria-live="polite">
      <i v-if="props.state === 'loading'" class="pi pi-spin pi-spinner" aria-hidden="true" />
      <i v-else-if="props.state === 'unauthorized' || props.state === 'forbidden'" class="pi pi-lock" aria-hidden="true" />
      <i v-else-if="props.state === 'conflict'" class="pi pi-exclamation-triangle" aria-hidden="true" />
      <i v-else class="pi pi-info-circle" aria-hidden="true" />
      <div>
        <strong>{{ stateCopy[props.state].heading }}</strong>
        <p>{{ stateCopy[props.state].message }}</p>
      </div>
      <Button
        v-if="props.state === 'conflict' || props.state === 'not-found'"
        label="Tải lại"
        icon="pi pi-refresh"
        severity="secondary"
        @click="emit('retry')"
      />
    </div>

    <table v-else class="v3-contract-review__table">
      <thead>
        <tr><th>Đối tượng</th><th>Trạng thái</th><th>Phạm vi</th><th>Cập nhật</th></tr>
      </thead>
      <tbody>
        <tr v-for="item in props.items" :key="item.identity">
          <td>{{ item.identity }}</td><td>{{ item.status }}</td><td>{{ item.scope }}</td><td>{{ item.updatedAt }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.v3-contract-review { border: 1px solid var(--p-content-border-color); border-radius: 12px; background: var(--p-content-background); padding: 1.25rem; }
.v3-contract-review__context { display: flex; flex-wrap: wrap; gap: .5rem; color: var(--p-text-muted-color); font-size: .875rem; }
.v3-contract-review__context span + span::before { content: '·'; margin-right: .5rem; }
.v3-contract-review__heading { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin: 1rem 0; }
.v3-contract-review__heading h2 { margin: 0; font-size: 1.25rem; }
.v3-contract-review__eyebrow { margin: 0 0 .25rem; color: var(--p-primary-color); font-size: .875rem; font-weight: 600; }
.v3-contract-review__badge { border-radius: 999px; background: var(--p-highlight-background); color: var(--p-highlight-color); padding: .25rem .65rem; font-size: .8125rem; white-space: nowrap; }
.v3-contract-review__state { display: flex; align-items: center; gap: .75rem; min-height: 8rem; border-radius: 8px; background: var(--p-surface-50); padding: 1rem; }
.v3-contract-review__state > i { color: var(--p-primary-color); font-size: 1.5rem; }
.v3-contract-review__state strong, .v3-contract-review__state p { display: block; margin: 0; }
.v3-contract-review__state p { color: var(--p-text-muted-color); margin-top: .25rem; }
.v3-contract-review__state :deep(.p-button) { margin-left: auto; }
.v3-contract-review__table { width: 100%; border-collapse: collapse; text-align: left; }
.v3-contract-review__table th, .v3-contract-review__table td { border-bottom: 1px solid var(--p-content-border-color); padding: .75rem; }
@media (max-width: 640px) { .v3-contract-review__heading, .v3-contract-review__state { align-items: flex-start; flex-direction: column; } .v3-contract-review__state :deep(.p-button) { margin-left: 0; } .v3-contract-review__table { font-size: .875rem; } }
</style>
