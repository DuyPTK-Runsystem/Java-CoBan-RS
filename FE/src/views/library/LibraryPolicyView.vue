<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import { getAuthSession } from '@/services/authSession'
import { getLibraryErrorMessage } from '@/services/library/libraryErrorMessages'
import { getLibraryCirculationPolicy, updateLibraryCirculationPolicy } from '@/services/library/libraryPolicyApi'
import { isApiError } from '@/types/api'
import type { LibraryCirculationPolicy, LibraryFineTier } from '@/types/library/policy'
import { USER_ROLE } from '@/types/user'

const token = computed(() => getAuthSession()?.accessToken ?? '')
const canEdit = computed(() => (getAuthSession()?.user.roles ?? []).some((role) => role === USER_ROLE.ADMIN || role === USER_ROLE.LIBRARIAN))
const policy = ref<LibraryCirculationPolicy | null>(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const conflict = ref(false)
const effectiveAtInput = ref('')
const form = reactive({ maxActiveLoans: 5, loanDurationDays: 14, maxRenewals: 2, renewalDurationDays: 7, reservationPickupDays: 3, fineTiers: [] as LibraryFineTier[], fineCapPerLoan: 500000, fineSuspensionThreshold: 500000 })

function localNow(): string {
  const parts = new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).formatToParts(new Date())
  const get = (key: string) => parts.find((part) => part.type === key)?.value ?? '00'
  return `${get('year')}-${get('month')}-${get('day')}T${get('hour')}:${get('minute')}`
}
function toOffsetDateTime(value: string): string { return `${value.length === 16 ? `${value}:00` : value}+07:00` }
function displayDate(value: string | null | undefined): string {
  if (!value) return '—'
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date(value))
}
function applyPolicy(value: LibraryCirculationPolicy): void {
  policy.value = value
  form.maxActiveLoans = value.maxActiveLoans
  form.loanDurationDays = value.loanDurationDays
  form.maxRenewals = value.maxRenewals
  form.renewalDurationDays = value.renewalDurationDays
  form.reservationPickupDays = value.reservationPickupDays
  form.fineTiers = value.fineTiers.map((tier) => ({ ...tier }))
  form.fineCapPerLoan = value.fineCapPerLoan
  form.fineSuspensionThreshold = value.fineSuspensionThreshold
  if (!effectiveAtInput.value) effectiveAtInput.value = localNow()
}
async function loadPolicy(): Promise<void> {
  if (!token.value) return
  loading.value = true; error.value = ''; conflict.value = false
  try { applyPolicy(await getLibraryCirculationPolicy(token.value)) }
  catch (cause) { error.value = getLibraryErrorMessage(cause, 'Không thể tải policy circulation.') }
  finally { loading.value = false }
}
function addTier(): void {
  const last = form.fineTiers.at(-1)
  if (!last) { form.fineTiers.push({ throughDay: null, dailyRate: 0 }); return }
  form.fineTiers.splice(form.fineTiers.length - 1, 0, { throughDay: (last.throughDay ?? 30) + 30, dailyRate: last.dailyRate })
}
function removeTier(index: number): void {
  if (form.fineTiers.length < 2) return
  form.fineTiers.splice(index, 1)
  form.fineTiers[form.fineTiers.length - 1]!.throughDay = null
}
function validateTiers(): string | null {
  if (!form.fineTiers.length) return 'Cần ít nhất một bậc fine.'
  for (const [index, tier] of form.fineTiers.entries()) {
    const last = index === form.fineTiers.length - 1
    if (!Number.isFinite(tier.dailyRate) || tier.dailyRate < 0) return `Đơn giá bậc ${index + 1} phải từ 0 trở lên.`
    if (last && tier.throughDay !== null) return 'Bậc cuối phải không giới hạn.'
    if (!last && (!Number.isInteger(tier.throughDay) || Number(tier.throughDay) <= 0 || (index > 0 && Number(tier.throughDay) <= Number(form.fineTiers[index - 1]?.throughDay)))) return `Mốc ngày bậc ${index + 1} phải tăng dần.`
  }
  return null
}
function previewFine(days: number): number {
  let total = 0; let previousEnd = 0
  form.fineTiers.forEach((tier) => { const end = tier.throughDay === null ? days : Math.min(days, tier.throughDay); if (end > previousEnd) total += (end - previousEnd) * tier.dailyRate; previousEnd = Math.max(previousEnd, end) })
  return Math.min(total, form.fineCapPerLoan)
}
async function savePolicy(): Promise<void> {
  if (!policy.value || !canEdit.value || !token.value) return
  error.value = validateTiers() ?? ''
  if (error.value) return
  if (!effectiveAtInput.value) { error.value = 'Chọn thời điểm bắt đầu hiệu lực policy.'; return }
  saving.value = true; error.value = ''; notice.value = ''; conflict.value = false
  try {
    const updated = await updateLibraryCirculationPolicy({ expectedVersion: policy.value.policyVersion, effectiveAt: toOffsetDateTime(effectiveAtInput.value), maxActiveLoans: form.maxActiveLoans, loanDurationDays: form.loanDurationDays, maxRenewals: form.maxRenewals, renewalDurationDays: form.renewalDurationDays, reservationPickupDays: form.reservationPickupDays, fineTiers: form.fineTiers.map((tier) => ({ ...tier })), fineCapPerLoan: form.fineCapPerLoan, fineSuspensionThreshold: form.fineSuspensionThreshold }, token.value)
    applyPolicy(updated); notice.value = `Đã tạo policy version ${updated.policyVersion}. Giao dịch cũ giữ nguyên snapshot.`
  } catch (cause) {
    conflict.value = isApiError(cause) && cause.code === 'VERSION_CONFLICT'
    error.value = conflict.value ? 'Policy đã được cập nhật. Tải version hiện tại trước khi lưu.' : getLibraryErrorMessage(cause, 'Không thể lưu policy.')
  } finally { saving.value = false }
}
onMounted(() => { effectiveAtInput.value = localNow(); void loadPolicy() })
</script>

<template>
  <section class="library-page page-content policy-page">
    <header class="page-heading"><div><p class="eyebrow">Library v5 · Policy</p><h1>Chính sách lưu thông &amp; fine</h1><p class="subtitle">Numeric limits có version và effective time; mỗi giao dịch giữ policy snapshot.</p></div></header>
    <Message v-if="notice" severity="success" :closable="true" @close="notice = ''">{{ notice }}</Message>
    <Message v-if="error" severity="error" :closable="false">{{ error }} <Button v-if="conflict" label="Tải version mới" severity="secondary" text @click="loadPolicy" /></Message>
    <Message v-if="!canEdit" severity="warn" :closable="false">Policy editor dành cho ADMIN và LIBRARIAN.</Message>
    <div v-if="loading && !policy" class="content-surface">Đang tải policy…</div>
    <article v-else-if="policy" class="content-surface">
      <div class="surface-heading"><div><span class="step-label">Policy version</span><h2>Version {{ policy.policyVersion }}</h2></div><Tag :value="`Hiệu lực ${displayDate(policy.effectiveAt)}`" severity="success" /></div>
      <div class="policy-grid">
        <label>Tối đa loan đang hoạt động / patron<InputNumber v-model="form.maxActiveLoans" :min="1" :max="100" :disabled="!canEdit" /></label>
        <label>Thời hạn loan (ngày)<InputNumber v-model="form.loanDurationDays" :min="1" :max="365" :disabled="!canEdit" /></label>
        <label>Số lần gia hạn tối đa<InputNumber v-model="form.maxRenewals" :min="0" :max="100" :disabled="!canEdit" /></label>
        <label>Số ngày cộng mỗi lần gia hạn<InputNumber v-model="form.renewalDurationDays" :min="1" :max="365" :disabled="!canEdit" /></label>
        <label>Cửa sổ nhận reservation (ngày)<InputNumber v-model="form.reservationPickupDays" :min="1" :max="90" :disabled="!canEdit" /></label>
        <label>Ngưỡng nợ để đình chỉ (VND)<InputNumber v-model="form.fineSuspensionThreshold" :min="0" :max="1000000000" :step="1000" :disabled="!canEdit" /></label>
        <label class="effective">Thời điểm hiệu lực (Asia/Ho_Chi_Minh)<input v-model="effectiveAtInput" type="datetime-local" :disabled="!canEdit"></label>
      </div>
      <section class="tier-section">
        <div class="surface-heading">
          <div>
            <h3>Fine theo bậc</h3>
            <p class="helper">Bậc cuối áp dụng cho mọi ngày sau mốc trước đó.</p>
          </div>
          <Button label="Thêm bậc" icon="pi pi-plus" severity="secondary" outlined :disabled="!canEdit" @click="addTier" />
        </div>
        <div v-for="(tier, index) in form.fineTiers" :key="index" class="tier-row">
          <label>Bậc {{ index + 1 }} · đến ngày<InputNumber v-model="tier.throughDay" :min="1" :disabled="!canEdit || index === form.fineTiers.length - 1" :placeholder="index === form.fineTiers.length - 1 ? 'Không giới hạn' : ''" /></label>
          <label>Bậc {{ index + 1 }} · VND/ngày<InputNumber v-model="tier.dailyRate" :min="0" :max="100000000" :step="1000" :disabled="!canEdit" /></label>
          <Button icon="pi pi-trash" label="Xóa" severity="danger" text :disabled="!canEdit || form.fineTiers.length < 2" @click="removeTier(index)" />
        </div>
      </section>
      <div class="policy-foot"><label>Trần fine / loan (VND)<InputNumber v-model="form.fineCapPerLoan" :min="0" :max="1000000000" :step="1000" :disabled="!canEdit" /></label><div class="preview"><strong>Ước tính 8 ngày trễ</strong><span>{{ new Intl.NumberFormat('vi-VN').format(previewFine(8)) }} VND</span></div></div>
      <div class="audit-row"><span>Cập nhật bởi #{{ policy.updatedBy }} · {{ displayDate(policy.updatedAt) }}</span><Button label="Tạo version mới" icon="pi pi-save" :loading="saving" :disabled="!canEdit || saving" @click="savePolicy" /></div>
      <p class="helper">Lưu tạo version/effective time/audit mới. Loan, fine đã chốt và lịch sử cũ không bị thay đổi.</p>
    </article>
  </section>
</template>

<style scoped>
.policy-page { display:grid; gap:1rem; }.content-surface { padding:1rem; border:1px solid var(--surface-border,#e2e8f0); border-radius:.75rem; background:var(--surface-card,#fff); }.surface-heading { display:flex; justify-content:space-between; align-items:center; gap:.75rem; margin-bottom:1rem; }.surface-heading h2,.surface-heading h3 { margin:.2rem 0; }.policy-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:1rem; }.policy-grid label,.tier-row label,.policy-foot label { display:grid; gap:.4rem; color:var(--text-color-secondary,#64748b); font-size:.84rem; font-weight:600; }.effective { max-width:24rem; }.effective input { min-height:2.5rem; padding:.45rem .6rem; border:1px solid var(--surface-border,#cbd5e1); border-radius:.4rem; background:var(--surface-card,#fff); color:var(--text-color,#334155); }.tier-section { padding-top:1rem; margin-top:1.3rem; border-top:1px solid var(--surface-border,#e2e8f0); }.tier-row { display:grid; grid-template-columns:1fr 1fr auto; align-items:end; gap:.75rem; margin-top:.7rem; }.policy-foot { display:flex; align-items:end; gap:1rem; margin-top:1rem; }.policy-foot label { min-width:min(100%,22rem); }.preview { display:grid; gap:.2rem; padding:.65rem .8rem; border-left:3px solid var(--primary-color,#4f46e5); background:var(--primary-50,#eef2ff); }.audit-row { display:flex; justify-content:space-between; align-items:center; gap:.8rem; margin-top:1.2rem; }.audit-row span,.helper { color:var(--text-color-secondary,#64748b); font-size:.82rem; }
@media(max-width:700px) { .policy-grid { grid-template-columns:1fr; }.tier-row { grid-template-columns:1fr 1fr; }.tier-row button { justify-self:start; }.policy-foot,.audit-row { align-items:stretch; flex-direction:column; } }
</style>
