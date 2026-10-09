<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import { getAuthSession } from '@/services/authSession'
import { getLibraryErrorMessage } from '@/services/library/libraryErrorMessages'
import { listLibraryFines, payLibraryFine, waiveLibraryFine } from '@/services/library/libraryFineApi'
import type { LibraryFine, LibraryFinePage, LibraryFineStatus } from '@/types/library/fine'
import { USER_ROLE } from '@/types/user'

const token = computed(() => getAuthSession()?.accessToken ?? '')
const roles = computed(() => getAuthSession()?.user.roles ?? [])
const isStaff = computed(() => roles.value.some((role) => role === USER_ROLE.ADMIN || role === USER_ROLE.LIBRARIAN))
const isAdmin = computed(() => roles.value.includes(USER_ROLE.ADMIN))
const patronFilter = ref('')
const statusFilter = ref<LibraryFineStatus | null>(null)
const page = ref(0)
const pageSize = 10
const result = ref<LibraryFinePage | null>(null)
const loading = ref(false)
const busyFineId = ref<number | null>(null)
const error = ref('')
const notice = ref('')
const references = ref<Record<number, string>>({})
const reasons = ref<Record<number, string>>({})
const statusOptions = [
  { label: 'Mọi trạng thái', value: null },
  { label: 'Chưa thanh toán', value: 'UNPAID' },
  { label: 'Đã thanh toán', value: 'PAID' },
  { label: 'Đã miễn', value: 'WAIVED' },
]

function displayDate(value: string | null): string {
  if (!value) return '—'
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date(value))
}
async function loadFines(): Promise<void> {
  if (!token.value) return
  loading.value = true; error.value = ''
  try {
    const parsedPatron = patronFilter.value.trim() ? Number(patronFilter.value) : undefined
    result.value = await listLibraryFines({ patronId: isStaff.value && Number.isInteger(parsedPatron) && Number(parsedPatron) > 0 ? Number(parsedPatron) : undefined, status: statusFilter.value ?? undefined, page: page.value, pageSize }, token.value)
  } catch (cause) { error.value = getLibraryErrorMessage(cause, 'Không thể tải danh sách fine.') }
  finally { loading.value = false }
}
async function pay(fine: LibraryFine): Promise<void> {
  const reference = references.value[fine.fineId]?.trim()
  if (!reference || fine.status !== 'UNPAID' || fine.provisional || !token.value) return
  busyFineId.value = fine.fineId; error.value = ''; notice.value = ''
  try {
    const updated = await payLibraryFine(fine.fineId, { reference }, token.value)
    notice.value = `Fine #${updated.fineId} đã ghi nhận thanh toán đầy đủ.`
    await loadFines()
  } catch (cause) { error.value = getLibraryErrorMessage(cause, 'Không thể ghi nhận thanh toán.') }
  finally { busyFineId.value = null }
}
async function waive(fine: LibraryFine): Promise<void> {
  const reason = reasons.value[fine.fineId]?.trim()
  if (!reason || fine.status !== 'UNPAID' || fine.provisional || !isAdmin.value || !token.value) return
  busyFineId.value = fine.fineId; error.value = ''; notice.value = ''
  try {
    const updated = await waiveLibraryFine(fine.fineId, { reason }, token.value)
    notice.value = `Fine #${updated.fineId} đã được miễn toàn phần.`
    await loadFines()
  } catch (cause) { error.value = getLibraryErrorMessage(cause, 'Không thể miễn fine.') }
  finally { busyFineId.value = null }
}
function changePage(nextPage: number): void { if (nextPage < 0 || (result.value && nextPage >= result.value.meta.totalPages)) return; page.value = nextPage; void loadFines() }
onMounted(() => void loadFines())
</script>

<template>
  <section class="library-page page-content fine-page">
    <header class="page-heading"><div><p class="eyebrow">Library v5</p><h1>Fine</h1><p class="subtitle">Fine đang tăng provisional; thanh toán và miễn chỉ mở khi số tiền đã chốt sau trả hoặc mất sách.</p></div></header>
    <Message v-if="notice" severity="success" :closable="true" @close="notice = ''">{{ notice }}</Message>
    <Message v-if="error" severity="error" :closable="true" @close="error = ''">{{ error }}</Message>
    <form class="filters content-surface" @submit.prevent="page = 0; loadFines()">
      <label v-if="isStaff" class="filter-field">Patron ID<InputText v-model="patronFilter" inputmode="numeric" placeholder="Mọi bạn đọc" /></label>
      <label class="filter-field">Trạng thái<Select v-model="statusFilter" :options="statusOptions" option-label="label" option-value="value" /></label>
      <div class="filter-actions"><Button label="Lọc" icon="pi pi-search" type="submit" :loading="loading" /><Button label="Xóa lọc" severity="secondary" outlined @click="patronFilter = ''; statusFilter = null; page = 0; loadFines()" /></div>
    </form>
    <div v-if="loading && !result" class="content-surface page-state">Đang tải fine…</div>
    <div v-else-if="!loading && result?.result.length === 0" class="content-surface page-state"><strong>Chưa có fine phù hợp.</strong><span>Fine mới sẽ xuất hiện sau khi hệ thống tính hoặc chốt khoản phạt.</span></div>
    <div v-else-if="result" class="fine-list">
      <article v-for="fine in result.result" :key="fine.fineId" class="content-surface fine-card">
        <div class="fine-heading"><div><span class="step-label">Fine #{{ fine.fineId }} · Loan #{{ fine.loanId }}</span><h2>{{ new Intl.NumberFormat('vi-VN').format(Number(fine.amount)) }} {{ fine.currency }}</h2></div><div class="fine-tags"><Tag :value="fine.type" severity="info" /><Tag :value="fine.provisional ? 'PROVISIONAL' : fine.status" :severity="fine.provisional ? 'warn' : fine.status === 'PAID' ? 'success' : fine.status === 'WAIVED' ? 'secondary' : 'danger'" /></div></div>
        <dl><div><dt>Tính đến</dt><dd>{{ displayDate(fine.calculatedThrough) }}</dd></div><div><dt>Policy version</dt><dd>{{ fine.policyVersion }}</dd></div><div><dt>Thanh toán</dt><dd>{{ fine.paymentReference ?? displayDate(fine.paidAt) }}</dd></div><div><dt>Miễn</dt><dd>{{ fine.waiveReason ?? displayDate(fine.waivedAt) }}</dd></div></dl>
        <p v-if="fine.provisional" class="helper">Ước tính còn thay đổi đến khi loan được trả hoặc đánh dấu mất. Chưa thể thanh toán hoặc miễn.</p>
        <div v-else-if="fine.status === 'UNPAID' && isStaff" class="fine-actions">
          <form class="fine-action" @submit.prevent="pay(fine)"><label :for="`payment-${fine.fineId}`">Mã tham chiếu thanh toán toàn phần</label><div><InputText :id="`payment-${fine.fineId}`" v-model="references[fine.fineId]" required placeholder="Biên nhận / mã giao dịch" /><Button label="Ghi nhận đã thu" icon="pi pi-check" type="submit" :loading="busyFineId === fine.fineId" :disabled="!references[fine.fineId]?.trim() || busyFineId !== null" /></div></form>
          <form v-if="isAdmin" class="fine-action" @submit.prevent="waive(fine)"><label :for="`waive-${fine.fineId}`">Lý do miễn toàn phần (ADMIN)</label><div><InputText :id="`waive-${fine.fineId}`" v-model="reasons[fine.fineId]" required placeholder="Lý do miễn" /><Button label="Miễn fine" icon="pi pi-ban" severity="secondary" type="submit" :loading="busyFineId === fine.fineId" :disabled="!reasons[fine.fineId]?.trim() || busyFineId !== null" /></div></form>
        </div>
      </article>
      <footer class="pagination content-surface"><span>{{ result.meta.totalItems }} fine · trang {{ result.meta.page + 1 }} / {{ Math.max(result.meta.totalPages, 1) }}</span><div><Button icon="pi pi-chevron-left" aria-label="Trang trước" severity="secondary" text :disabled="loading || page === 0" @click="changePage(page - 1)" /><Button icon="pi pi-chevron-right" aria-label="Trang sau" severity="secondary" text :disabled="loading || page + 1 >= result.meta.totalPages" @click="changePage(page + 1)" /></div></footer>
    </div>
  </section>
</template>

<style scoped>
.fine-page { display:grid; gap:1rem; }.content-surface { padding:1rem; border:1px solid var(--surface-border,#e2e8f0); border-radius:.75rem; background:var(--surface-card,#fff); }.filters { display:flex; align-items:end; gap:.85rem; flex-wrap:wrap; }.filter-field { display:grid; gap:.35rem; min-width:13rem; font-size:.85rem; font-weight:600; }.filter-actions { display:flex; gap:.5rem; }.fine-list { display:grid; gap:.75rem; }.fine-heading { display:flex; justify-content:space-between; align-items:start; gap:.8rem; }.fine-heading h2 { margin:.3rem 0; }.fine-tags { display:flex; gap:.4rem; flex-wrap:wrap; }.step-label,.helper { color:var(--text-color-secondary,#64748b); font-size:.82rem; }.fine-card dl { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:.75rem; margin:.8rem 0; }.fine-card dl div { display:grid; gap:.2rem; }.fine-card dt { color:var(--text-color-secondary,#64748b); font-size:.76rem; }.fine-card dd { margin:0; overflow-wrap:anywhere; }.fine-actions { display:grid; gap:.8rem; padding-top:.8rem; border-top:1px solid var(--surface-border,#e2e8f0); }.fine-action { display:grid; gap:.35rem; }.fine-action label { font-size:.82rem; font-weight:600; }.fine-action div { display:flex; gap:.5rem; }.fine-action input { flex:1; min-width:0; }.pagination { display:flex; justify-content:space-between; align-items:center; color:var(--text-color-secondary,#64748b); font-size:.85rem; }.pagination div { display:flex; }.page-state { display:grid; gap:.4rem; color:var(--text-color-secondary,#64748b); }
@media(max-width:700px) { .fine-card dl { grid-template-columns:repeat(2,minmax(0,1fr)); }.fine-action div { flex-direction:column; }.fine-heading { flex-direction:column; } }
</style>
