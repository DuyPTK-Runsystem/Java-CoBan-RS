<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import Tag from 'primevue/tag'

import ScannerUploadDemo from '@/components/library/ScannerUploadDemo.vue'
import { getBookCopyByBarcode } from '@/services/library/libraryCatalogApi'
import { borrowLibraryCopies, markLibraryCopyLost, renewLibraryLoan, returnLibraryCopies } from '@/services/library/libraryCirculationApi'
import { getLibraryPatron, verifyLibraryCard } from '@/services/library/libraryPatronApi'
import { getLibraryCirculationPolicy } from '@/services/library/libraryPolicyApi'
import { getLibraryErrorMessage } from '@/services/library/libraryErrorMessages'
import { getAuthSession } from '@/services/authSession'
import type { BookCopyDetail } from '@/types/library/catalog'
import type { LibraryBorrowResponse, LibraryReturnResponse } from '@/types/library/circulation'
import type { LibraryPatronSummary } from '@/types/library/patron'
import type { LibraryCirculationPolicy } from '@/types/library/policy'
import { USER_ROLE } from '@/types/user'

interface ScanCopy extends BookCopyDetail {
  scanError?: string
}

const token = computed(() => getAuthSession()?.accessToken ?? '')
const roles = computed(() => getAuthSession()?.user.roles ?? [])
const canStaffCirculate = computed(() => roles.value.some((role) => role === USER_ROLE.ADMIN || role === USER_ROLE.LIBRARIAN))
const patronSearch = ref('')
const cardQrPayload = ref('')
const patronId = ref<number | null>(null)
const cardNo = ref('')
const patron = ref<LibraryPatronSummary | null>(null)
const copyInput = ref('')
const returnInput = ref('')
const lostInput = ref('')
const reason = ref('')
const loanIdInput = ref('')
const borrowCopies = ref<ScanCopy[]>([])
const returnBarcodes = ref<string[]>([])
const lookupBusy = ref(false)
const mutationBusy = ref(false)
const alert = ref<{ severity: 'success' | 'error' | 'warn' | 'info'; text: string } | null>(null)
const returnResult = ref<LibraryReturnResponse | null>(null)
const borrowResult = ref<LibraryBorrowResponse | null>(null)
const currentPolicy = ref<LibraryCirculationPolicy | null>(null)

const canBorrow = computed(() => canStaffCirculate.value && patron.value?.status === 'ACTIVE'
  && patron.value.currentCard?.status === 'ACTIVE' && Boolean(cardNo.value.trim()) && borrowCopies.value.length > 0
  && borrowCopies.value.every((copy) => copy.status === 'AVAILABLE' && !copy.referenceOnly))
const selectedLoan = computed(() => loanIdInput.value.trim())

const expectedDueDate = computed(() => {
  if (!currentPolicy.value) return 'Đang tải policy…'
  const date = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
  const [year, month, day] = date.split('-').map(Number)
  const expected = new Date(Date.UTC(year!, month! - 1, day! + currentPolicy.value.loanDurationDays))
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeZone: 'UTC' }).format(expected)
})

function showError(error: unknown, fallback: string): void {
  alert.value = { severity: 'error', text: getLibraryErrorMessage(error, fallback) }
}

async function lookupPatron(): Promise<void> {
  const parsed = Number(patronSearch.value.trim())
  if (!Number.isInteger(parsed) || parsed <= 0 || !token.value) {
    alert.value = { severity: 'warn', text: 'Nhập mã patron dạng số hoặc quét QR thẻ để tra cứu.' }
    return
  }
  lookupBusy.value = true
  alert.value = null
  try {
    const result = await getLibraryPatron(parsed, token.value)
    patron.value = result
    patronId.value = result.patronId
    cardNo.value = result.currentCard?.cardNo ?? ''
  } catch (error) {
    patron.value = null
    showError(error, 'Không thể tra cứu bạn đọc.')
  } finally {
    lookupBusy.value = false
  }
}

async function verifyCardQr(): Promise<void> {
  const payload = cardQrPayload.value.trim()
  if (!payload) return
  if (!token.value) return
  lookupBusy.value = true
  alert.value = null
  try {
    const result = await verifyLibraryCard({ payload }, token.value)
    if (!result.valid || !result.card) {
      alert.value = { severity: 'error', text: result.message || 'QR thẻ không hợp lệ. Hãy kiểm tra thẻ hoặc nhập mã bạn đọc.' }
      return
    }
    const verifiedPatron = await getLibraryPatron(result.card.patronId, token.value)
    patron.value = verifiedPatron
    patronId.value = verifiedPatron.patronId
    patronSearch.value = String(verifiedPatron.patronId)
    cardNo.value = result.card.cardNo
    cardQrPayload.value = ''
    alert.value = { severity: 'success', text: 'Đã xác thực QR thẻ ở máy chủ và chọn bạn đọc.' }
  } catch (error) {
    showError(error, 'Không thể xác thực QR thẻ.')
  } finally {
    lookupBusy.value = false
  }
}

function handlePatronDecoded(value: string): void {
  if (/^\d+$/.test(value.trim())) {
    patronSearch.value = value.trim()
    cardQrPayload.value = ''
    return
  }
  cardQrPayload.value = value
}

async function addCopy(): Promise<void> {
  const barcode = copyInput.value.trim()
  if (!barcode || !token.value) return
  if (borrowCopies.value.some((copy) => copy.barcode === barcode)) {
    alert.value = { severity: 'warn', text: 'Barcode này đã có trong giỏ mượn.' }
    return
  }
  lookupBusy.value = true
  try {
    const copy = await getBookCopyByBarcode(barcode, token.value)
    borrowCopies.value.push(copy)
    copyInput.value = ''
    alert.value = null
  } catch (error) {
    showError(error, 'Không thể tra cứu bản sao.')
  } finally {
    lookupBusy.value = false
  }
}

function addReturnBarcode(): void {
  const barcode = returnInput.value.trim()
  if (!barcode) return
  if (returnBarcodes.value.includes(barcode)) {
    alert.value = { severity: 'warn', text: 'Barcode này đã có trong danh sách trả.' }
    return
  }
  returnBarcodes.value.push(barcode)
  returnInput.value = ''
  alert.value = null
}

async function borrow(): Promise<void> {
  if (!canBorrow.value || !patronId.value || !token.value) return
  mutationBusy.value = true
  alert.value = null
  try {
    borrowResult.value = await borrowLibraryCopies({
      patronId: patronId.value,
      cardNo: cardNo.value.trim(),
      copyBarcodes: borrowCopies.value.map((copy) => copy.barcode),
    }, token.value)
    alert.value = { severity: 'success', text: `Đã tạo ${borrowResult.value.items.length} loan. Mỗi loan lưu policy version tại thời điểm mượn.` }
    borrowCopies.value = []
  } catch (error) {
    showError(error, 'Không thể tạo loan. Không có bản sao nào được xác nhận nếu cả nhóm không hợp lệ.')
  } finally {
    mutationBusy.value = false
  }
}

async function returnCopies(): Promise<void> {
  if (!returnBarcodes.value.length || !token.value) return
  mutationBusy.value = true
  alert.value = null
  try {
    returnResult.value = await returnLibraryCopies({ copyBarcodes: [...returnBarcodes.value] }, token.value)
    alert.value = { severity: 'success', text: `Đã ghi nhận trả ${returnResult.value.items.length} bản sao.` }
    returnBarcodes.value = []
  } catch (error) {
    showError(error, 'Không thể xác nhận trả sách.')
  } finally {
    mutationBusy.value = false
  }
}

async function renew(): Promise<void> {
  const id = Number(selectedLoan.value)
  if (!Number.isInteger(id) || id <= 0 || !token.value) return
  mutationBusy.value = true
  alert.value = null
  try {
    const loan = await renewLibraryLoan(id, token.value)
    alert.value = { severity: 'success', text: `Đã gia hạn loan #${loan.loanId}; hạn mới ${loan.dueAt}.` }
  } catch (error) {
    showError(error, 'Không thể gia hạn loan.')
  } finally {
    mutationBusy.value = false
  }
}

async function markLost(): Promise<void> {
  const barcode = lostInput.value.trim()
  if (!barcode || !reason.value.trim() || !token.value) {
    alert.value = { severity: 'warn', text: 'Nhập barcode và lý do đánh dấu mất.' }
    return
  }
  mutationBusy.value = true
  alert.value = null
  try {
    const result = await markLibraryCopyLost(barcode, reason.value.trim(), token.value)
    const amount = new Intl.NumberFormat('vi-VN').format(Number(result.fine.amount))
    alert.value = { severity: 'success', text: `Đã ghi nhận mất sách cho lượt mượn #${result.loan.loanId}. Khoản phạt mất sách #${result.fine.fineId}: ${amount} ${result.fine.currency}.` }
    lostInput.value = ''
    reason.value = ''
  } catch (error) {
    showError(error, 'Không thể đánh dấu bản sao bị mất.')
  } finally {
    mutationBusy.value = false
  }
}

async function loadPolicy(): Promise<void> {
  if (!token.value) return
  try { currentPolicy.value = await getLibraryCirculationPolicy(token.value) }
  catch { currentPolicy.value = null }
}

onMounted(() => void loadPolicy())
</script>

<template>
  <section class="library-page page-content circulation-page">
    <header class="page-heading">
      <div>
        <p class="eyebrow">Library v5 · Circulation</p>
        <h1>Quầy lưu thông</h1>
        <p class="subtitle">Tra cứu bạn đọc, tạo loan nhiều bản sao, nhận trả, gia hạn và ghi nhận sách mất.</p>
      </div>
    </header>

    <Message v-if="alert" :severity="alert.severity" :closable="true" @close="alert = null">{{ alert.text }}</Message>
    <Message v-if="!canStaffCirculate" severity="info" :closable="false">Bạn đọc có thể xem lịch sử và reservation; các thao tác tại quầy dành cho ADMIN/LIBRARIAN.</Message>

    <div class="circulation-layout">
      <div class="circulation-stack">
        <article class="content-surface">
          <div class="surface-heading"><div><span class="step-label">Bước 1</span><h2>Nhận diện bạn đọc</h2></div><Tag v-if="patron" :severity="patron.status === 'ACTIVE' ? 'success' : 'danger'" :value="patron.status" /></div>
          <form class="input-row" @submit.prevent="lookupPatron">
            <InputText v-model="patronSearch" aria-label="Mã patron" placeholder="Nhập mã patron dạng số" :disabled="lookupBusy || !canStaffCirculate" />
            <Button label="Tra cứu" icon="pi pi-search" type="submit" :loading="lookupBusy" :disabled="!canStaffCirculate" />
          </form>
          <ScannerUploadDemo id="patron-card" label="Ảnh QR thẻ hoặc mã bạn đọc" @decoded="handlePatronDecoded" />
          <form class="input-row qr-verify-row" @submit.prevent="verifyCardQr">
            <InputText v-model="cardQrPayload" aria-label="Nội dung QR thẻ đã đọc" placeholder="Nội dung QR sẽ hiển thị ở đây" :disabled="lookupBusy || !canStaffCirculate" />
            <Button label="Xác thực QR" icon="pi pi-verified" type="submit" severity="secondary" :loading="lookupBusy" :disabled="!cardQrPayload.trim() || !canStaffCirculate" />
          </form>
          <div v-if="patron" class="patron-result">
            <div><strong>{{ patron.displayName }}</strong><span>Patron #{{ patron.patronId }} · {{ patron.currentCard?.cardNo ?? 'Chưa có thẻ hoạt động' }}</span></div>
            <Tag :severity="patron.currentCard?.status === 'ACTIVE' ? 'success' : 'warn'" :value="patron.currentCard?.status ?? 'NO_CARD'" />
          </div>
          <p v-if="patron?.status === 'BORROWING_SUSPENDED'" class="helper warning">Đình chỉ chặn mượn, gia hạn và reservation mới; vẫn cho phép trả sách.</p>
        </article>

        <article class="content-surface">
          <div class="surface-heading"><div><span class="step-label">Bước 2</span><h2>Thêm bản sao vào loan</h2></div><Tag :value="`${borrowCopies.length} bản sao`" /></div>
          <form class="input-row" @submit.prevent="addCopy">
            <InputText v-model="copyInput" aria-label="Barcode bản sao" placeholder="Nhập barcode sách" :disabled="lookupBusy || !canStaffCirculate" />
            <Button label="Thêm bản sao" icon="pi pi-plus" type="submit" severity="secondary" :loading="lookupBusy" :disabled="!canStaffCirculate" />
          </form>
          <ScannerUploadDemo id="book-barcode" label="Ảnh barcode sách (QR hoặc Code 128)" :formats="['qr_code', 'code_128']" @decoded="copyInput = $event" />
          <ul v-if="borrowCopies.length" class="copy-list">
            <li v-for="copy in borrowCopies" :key="copy.barcode">
              <div><strong>{{ copy.book?.title ?? `Bản sao ${copy.barcode}` }}</strong><span>{{ copy.barcode }} · {{ copy.shelfLocation ?? 'Chưa có vị trí kệ' }}</span></div>
              <Tag :severity="copy.status === 'AVAILABLE' && !copy.referenceOnly ? 'success' : 'warn'" :value="copy.referenceOnly ? 'REFERENCE_ONLY' : copy.status" />
              <Button icon="pi pi-times" severity="secondary" text rounded :aria-label="`Xóa ${copy.barcode}`" @click="borrowCopies = borrowCopies.filter((item) => item.barcode !== copy.barcode)" />
            </li>
          </ul>
          <p v-else class="helper">Quét hoặc nhập tay barcode. Nhóm borrow được gửi all-or-nothing.</p>
          <div class="action-row"><Button label="Tạo loan" icon="pi pi-check" :loading="mutationBusy" :disabled="!canBorrow || mutationBusy" @click="borrow" /></div>
          <div v-if="borrowResult" class="return-result">
            <p v-for="loan in borrowResult.items" :key="loan.loanId">{{ loan.bookTitle }} · {{ loan.copyBarcode }} · hạn {{ loan.dueAt }} · policy v{{ loan.policyVersion }}</p>
          </div>
        </article>

        <article class="content-surface">
          <div class="surface-heading"><div><span class="step-label">Bước 3</span><h2>Nhận trả</h2></div><Tag :value="`${returnBarcodes.length} bản sao`" /></div>
          <form class="input-row" @submit.prevent="addReturnBarcode">
            <InputText v-model="returnInput" aria-label="Barcode trả sách" placeholder="Nhập hoặc quét barcode để trả" :disabled="!canStaffCirculate" />
            <Button label="Thêm" icon="pi pi-plus" type="submit" severity="secondary" :disabled="!canStaffCirculate" />
          </form>
          <ScannerUploadDemo id="return-barcode" label="Ảnh barcode để trả" :formats="['code_128', 'qr_code']" @decoded="returnInput = $event" />
          <p v-if="returnBarcodes.length" class="helper">{{ returnBarcodes.join(' · ') }} <Button label="Xóa danh sách" severity="secondary" text @click="returnBarcodes = []" /></p>
          <div class="action-row"><Button label="Xác nhận trả" icon="pi pi-undo" severity="secondary" :loading="mutationBusy" :disabled="!returnBarcodes.length || !canStaffCirculate || mutationBusy" @click="returnCopies" /></div>
          <div v-if="returnResult" class="return-result">
            <p v-for="item in returnResult.items" :key="item.loan.loanId">Loan #{{ item.loan.loanId }}: {{ item.loan.status }}<span v-if="item.fine"> · Fine #{{ item.fine.fineId }} {{ item.fine.amount }} {{ item.fine.currency }} ({{ item.fine.status }})</span></p>
          </div>
        </article>

        <article class="content-surface">
          <div class="surface-heading"><div><span class="step-label">Bước 4</span><h2>Gia hạn loan</h2></div></div>
          <form class="input-row" @submit.prevent="renew">
            <InputText v-model="loanIdInput" inputmode="numeric" aria-label="Loan ID gia hạn" placeholder="Nhập ID loan đang hoạt động" :disabled="!canStaffCirculate" />
            <Button label="Gia hạn" icon="pi pi-calendar-plus" type="submit" severity="secondary" :loading="mutationBusy" :disabled="!selectedLoan || !canStaffCirculate || mutationBusy" />
          </form>
          <p class="helper">Backend kiểm tra trạng thái thẻ, giới hạn gia hạn và reservation priority.</p>
        </article>

        <article class="content-surface">
          <div class="surface-heading"><div><span class="step-label">Bước 5</span><h2>Đánh dấu mất</h2></div></div>
          <div class="input-row"><InputText v-model="lostInput" aria-label="Barcode bản sao bị mất" placeholder="Barcode bản sao" :disabled="!canStaffCirculate" /><InputText v-model="reason" aria-label="Lý do đánh dấu mất" placeholder="Lý do" :disabled="!canStaffCirculate" /><Button label="Xác nhận mất" icon="pi pi-exclamation-triangle" severity="danger" :loading="mutationBusy" :disabled="!lostInput.trim() || !reason.trim() || !canStaffCirculate || mutationBusy" @click="markLost" /></div>
          <p class="helper">Backend chốt overdue đến ngày mất và tạo fine LOST_ITEM theo policy snapshot.</p>
        </article>
      </div>
      <aside class="content-surface summary-panel">
        <div class="surface-heading"><div><span class="step-label">Tóm tắt phiên</span><h2>Thao tác lưu thông</h2></div><Tag :value="canStaffCirculate ? 'QUẦY' : 'BẠN ĐỌC'" /></div>
        <div class="summary-number"><strong>{{ borrowCopies.length }}</strong><span>Bản sao đang chọn</span></div>
        <div class="summary-list">
          <div><span>Patron</span><strong>{{ patron?.displayName ?? 'Chưa chọn' }}</strong></div>
          <div><span>Thẻ</span><strong>{{ cardNo || 'Chưa xác thực' }}</strong></div>
          <div><span>Giới hạn loan</span><strong>{{ currentPolicy?.maxActiveLoans ?? '—' }} / patron</strong></div>
          <div><span>Hạn trả dự kiến</span><strong>{{ expectedDueDate }}</strong></div>
          <div><span>Gia hạn</span><strong>{{ currentPolicy ? `${currentPolicy.maxRenewals} lần · +${currentPolicy.renewalDurationDays} ngày` : '—' }}</strong></div>
          <div><span>Policy</span><strong>{{ currentPolicy ? `v${currentPolicy.policyVersion}` : 'Chưa tải' }}</strong></div>
        </div>
        <p class="helper">Ngày hạn và giới hạn hiển thị chỉ để tham khảo. Backend xác nhận policy, eligibility và state khi lưu.</p>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.circulation-page { display: grid; gap: 1rem; }
.page-heading { margin-bottom: 0; }
.circulation-layout { display: grid; grid-template-columns: minmax(0, 1fr) minmax(18rem, .62fr); align-items: start; gap: 1rem; }
.circulation-stack { display: grid; gap: 1rem; min-width: 0; }
.content-surface { min-width: 0; padding: 1.1rem; border: 1px solid var(--surface-border, #e2e8f0); border-radius: .75rem; background: var(--surface-card, #fff); }
.surface-heading { display: flex; justify-content: space-between; align-items: center; gap: .8rem; margin-bottom: 1rem; }
.surface-heading h2 { margin: .2rem 0 0; font-size: 1.05rem; }
.step-label,.eyebrow { color: var(--text-color-secondary, #64748b); font-size: .75rem; font-weight: 700; text-transform: uppercase; letter-spacing: .04em; }
.input-row { display: flex; gap: .65rem; align-items: start; margin-bottom: .8rem; }
.input-row > :first-child { flex: 1; min-width: 0; }
.patron-result,.copy-list li { display: flex; align-items: center; justify-content: space-between; gap: .75rem; padding: .75rem; margin-top: .8rem; border: 1px solid var(--surface-border, #e2e8f0); border-radius: .5rem; }
.patron-result > div,.copy-list li > div { display: grid; gap: .15rem; min-width: 0; }
.patron-result span,.copy-list li span,.helper { color: var(--text-color-secondary, #64748b); font-size: .85rem; }
.copy-list { display: grid; gap: .5rem; padding: 0; margin: .8rem 0; list-style: none; }
.action-row { display: flex; justify-content: flex-end; margin-top: 1rem; }
.return-result { margin-top: .75rem; padding: .7rem .85rem; border-radius: .5rem; background: var(--green-50, #f0fdf4); }
.return-result p { margin: .2rem 0; }
.warning { color: var(--orange-700, #c2410c); }
.summary-panel { position: sticky; top: 1rem; }
.summary-number { display: flex; align-items: baseline; gap: .55rem; padding: .8rem 0; border-bottom: 1px solid var(--surface-border,#e2e8f0); }
.summary-number strong { font-size: 2rem; color: var(--primary-color,#4f46e5); }
.summary-number span,.summary-list span { color: var(--text-color-secondary,#64748b); font-size: .82rem; }
.summary-list { display: grid; gap: .75rem; padding: .85rem 0; }
.summary-list > div { display: flex; justify-content: space-between; align-items: baseline; gap: .8rem; }
.summary-list strong { text-align: right; overflow-wrap: anywhere; }
@media (max-width: 960px) { .circulation-layout { grid-template-columns: 1fr; } .summary-panel { position: static; grid-row: 1; } }
@media (max-width: 640px) { .input-row { flex-wrap: wrap; } .input-row > :first-child { flex-basis: 100%; } .content-surface { padding: .85rem; } }
</style>
