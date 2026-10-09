<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import { getAuthSession } from '@/services/authSession'
import { getLibraryErrorMessage } from '@/services/library/libraryErrorMessages'
import { listLibraryBatchJobs, runLibraryOverdueFineBatch } from '@/services/library/libraryFineApi'
import type { LibraryBatchJob } from '@/types/library/fine'
import { USER_ROLE } from '@/types/user'

const token = computed(() => getAuthSession()?.accessToken ?? '')
const canRun = computed(() => (getAuthSession()?.user.roles ?? []).some((role) => role === USER_ROLE.ADMIN || role === USER_ROLE.LIBRARIAN))
const result = ref<LibraryBatchJob[] | null>(null)
const runDate = ref(localDate())
const loading = ref(false)
const running = ref(false)
const error = ref('')
const notice = ref('')

function localDate(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
}
function displayDate(value: string | null | undefined): string {
  if (!value) return '—'
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: value.includes('T') ? 'short' : undefined, timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date(value))
}
function severity(status: string): 'success' | 'warn' | 'danger' | 'info' {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'STARTED') return 'warn'
  return 'info'
}
async function loadJobs(): Promise<void> {
  if (!token.value) return
  loading.value = true; error.value = ''
  try { result.value = await listLibraryBatchJobs(token.value) }
  catch (cause) { error.value = getLibraryErrorMessage(cause, 'Không thể tải lịch sử batch job.') }
  finally { loading.value = false }
}
async function runBatch(): Promise<void> {
  if (!token.value || !canRun.value || !runDate.value) return
  running.value = true; error.value = ''; notice.value = ''
  try {
    const job = await runLibraryOverdueFineBatch({ runDate: runDate.value }, token.value)
    notice.value = `Đã gửi overdue fine batch #${job.runId} cho ${job.runDate}. Trạng thái: ${job.status}.`
    await loadJobs()
  } catch (cause) { error.value = getLibraryErrorMessage(cause, 'Không thể khởi chạy overdue fine batch.') }
  finally { running.value = false }
}
onMounted(() => void loadJobs())
</script>

<template>
  <section class="library-page page-content batch-page">
    <header class="page-heading"><div><p class="eyebrow">Library v5 · Operations</p><h1>Overdue fine batch</h1><p class="subtitle">Chạy hoặc theo dõi tính fine overdue theo ngày chỉ định.</p></div></header>
    <Message v-if="notice" severity="success" :closable="true" @close="notice = ''">{{ notice }}</Message>
    <Message v-if="error" severity="error" :closable="true" @close="error = ''">{{ error }}</Message>
    <Message v-if="!canRun" severity="warn" :closable="false">Chỉ ADMIN và LIBRARIAN được chạy batch bằng tay.</Message>
    <form class="run-panel content-surface" @submit.prevent="runBatch">
      <label for="run-date">Ngày tính fine (Asia/Ho_Chi_Minh)</label>
      <input id="run-date" v-model="runDate" type="date" required :disabled="!canRun || running">
      <Button label="Chạy overdue fine batch" icon="pi pi-play" type="submit" :loading="running" :disabled="!canRun || !runDate" />
      <p class="helper">Batch chạy lại cùng runDate phải idempotent; backend giữ trạng thái job, tiến độ và kết quả.</p>
    </form>
    <div v-if="loading && !result" class="content-surface">Đang tải lịch sử…</div>
    <div v-else-if="!loading && result?.length === 0" class="content-surface empty-state"><strong>Chưa có lần chạy batch nào.</strong><span>Lịch sử sẽ xuất hiện sau khi có job đầu tiên.</span></div>
    <div v-else-if="result" class="content-surface table-surface">
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>Execution</th><th>Run date</th><th>Trạng thái</th><th>Bắt đầu</th><th>Kết thúc</th></tr>
          </thead>
          <tbody>
            <tr v-for="job in result" :key="job.runId">
              <td>#{{ job.runId }} · {{ job.jobName }}</td><td>{{ displayDate(job.runDate) }}</td><td><Tag :value="job.status" :severity="severity(job.status)" /></td><td>{{ displayDate(job.startedAt) }}</td><td>{{ displayDate(job.completedAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <footer class="pagination"><span>{{ result.length }} lần chạy</span></footer>
    </div>
  </section>
</template>

<style scoped>
.batch-page { display:grid; gap:1rem; }.content-surface { padding:1rem; border:1px solid var(--surface-border,#e2e8f0); border-radius:.75rem; background:var(--surface-card,#fff); }.run-panel { display:grid; grid-template-columns:minmax(12rem,1fr) auto; align-items:end; gap:.65rem; }.run-panel label,.helper { color:var(--text-color-secondary,#64748b); font-size:.84rem; }.run-panel label { grid-column:1/-1; font-weight:600; }.run-panel input { min-height:2.5rem; padding:.4rem .65rem; border:1px solid var(--surface-border,#cbd5e1); border-radius:.4rem; background:var(--surface-card,#fff); color:var(--text-color,#334155); }.helper { grid-column:1/-1; margin:.2rem 0 0; }.table-surface { min-width:0; padding:0; overflow:hidden; }.table-wrap { overflow-x:auto; }table { width:100%; border-collapse:collapse; }th,td { padding:.75rem .85rem; border-bottom:1px solid var(--surface-border,#e2e8f0); text-align:left; }th { color:var(--text-color-secondary,#64748b); background:var(--surface-50,#f8fafc); font-size:.76rem; }.empty-state { display:grid; gap:.4rem; color:var(--text-color-secondary,#64748b); }
@media(max-width:600px) { .run-panel { grid-template-columns:1fr; }.run-panel label,.helper { grid-column:auto; }.run-panel button { justify-self:start; } }
</style>
