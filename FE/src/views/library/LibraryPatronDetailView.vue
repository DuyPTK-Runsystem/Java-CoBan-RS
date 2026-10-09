<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import { useRoute, useRouter } from 'vue-router'

import FormAlert from '@/components/common/FormAlert.vue'
import PageState from '@/components/common/PageState.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import LibraryCardIssueDialog from '@/components/library/LibraryCardIssueDialog.vue'
import LibraryCardPreview from '@/components/library/LibraryCardPreview.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { getLibraryPatron, getLibraryCardQr, issueLibraryCard, listLibraryPatronCards, reissueLibraryCard, revokeLibraryCard } from '@/services/library/libraryPatronApi'
import { extractApiErrorMessage, isApiError } from '@/types/api'
import type { LibraryCardHistory, LibraryPatronSummary } from '@/types/library/patron'
import type { LoadingState } from '@/types/ui'
import { formatLibraryDate } from '@/utils/libraryCardDates'

const route = useRoute()
const router = useRouter()
const { requireAccessToken } = useAuthSession()
const patron = ref<LibraryPatronSummary | null>(null)
const history = ref<LibraryCardHistory>([])
const state = ref<LoadingState>('loading')
const forbidden = ref(false)
const loadError = ref('')
const qrError = ref('')
const operationError = ref('')
const operationPending = ref(false)
const qrLoading = ref(false)
const qrUrl = ref('')
const issueDialogVisible = ref(false)
const issueMode = ref<'issue' | 'reissue'>('issue')
const revokeDialogVisible = ref(false)
const revokeReason = ref('')
const revokeError = ref('')
let latestLoad = 0
let latestQr = 0

const patronId = computed(() => {
  const value = Number(route.params.patronId)
  return Number.isSafeInteger(value) && value > 0 ? value : null
})
const activeCard = computed(() => patron.value?.currentCard?.status === 'ACTIVE' ? patron.value.currentCard : null)

function clearQrUrl(): void {
  if (qrUrl.value) URL.revokeObjectURL(qrUrl.value)
  qrUrl.value = ''
}

async function loadDetail(): Promise<void> {
  const id = patronId.value
  const token = requireAccessToken()
  const requestId = ++latestLoad
  clearQrUrl()
  if (!id || !token) { state.value = 'error'; loadError.value = 'Mã hồ sơ bạn đọc không hợp lệ.'; return }
  state.value = 'loading'
  loadError.value = ''
  forbidden.value = false
  try {
    const [patronValue, cardHistory] = await Promise.all([getLibraryPatron(id, token), listLibraryPatronCards(id, token)])
    if (requestId !== latestLoad) return
    patron.value = patronValue
    history.value = cardHistory
    state.value = 'success'
    if (patronValue.currentCard?.status === 'ACTIVE') void loadQr(patronValue.currentCard.cardNo)
  } catch (error) {
    if (requestId !== latestLoad || isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    loadError.value = extractApiErrorMessage(error, 'Không thể tải hồ sơ bạn đọc.')
    state.value = 'error'
  }
}

async function loadQr(cardNo: string): Promise<void> {
  const token = requireAccessToken()
  if (!token) return
  const requestId = ++latestQr
  clearQrUrl()
  qrLoading.value = true
  qrError.value = ''
  try {
    const image = await getLibraryCardQr(cardNo, token)
    if (requestId !== latestQr) return
    qrUrl.value = URL.createObjectURL(image)
  } catch (error) {
    if (requestId !== latestQr || isApiError(error, 401)) return
    qrError.value = extractApiErrorMessage(error, 'Không thể tải ảnh QR của thẻ.')
  } finally { if (requestId === latestQr) qrLoading.value = false }
}

function downloadQr(): void {
  if (!qrUrl.value || !activeCard.value) return
  const anchor = document.createElement('a')
  anchor.href = qrUrl.value
  anchor.download = `${activeCard.value.cardNo}-qr.png`
  anchor.click()
}

function openIssue(mode: 'issue' | 'reissue'): void {
  issueMode.value = mode
  operationError.value = ''
  issueDialogVisible.value = true
}

async function submitIssue(request: { expiresAt: string; reason?: string }): Promise<void> {
  const token = requireAccessToken()
  const current = patron.value
  if (!token || !current || operationPending.value) return
  operationPending.value = true
  operationError.value = ''
  try {
    if (issueMode.value === 'reissue' && current.currentCard) {
      await reissueLibraryCard(current.currentCard.cardNo, { expiresAt: request.expiresAt, reason: request.reason ?? '' }, token)
    } else {
      await issueLibraryCard({ patronId: current.patronId, expiresAt: request.expiresAt }, token)
    }
    issueDialogVisible.value = false
    await loadDetail()
  } catch (error) {
    if (isApiError(error, 401)) return
    operationError.value = extractApiErrorMessage(error, 'Không thể phát hành thẻ thư viện.')
  } finally { operationPending.value = false }
}

function openRevoke(): void { revokeReason.value = ''; revokeError.value = ''; revokeDialogVisible.value = true }

async function revokeCard(): Promise<void> {
  const token = requireAccessToken()
  const card = activeCard.value
  if (!token || !card || operationPending.value) return
  if (!revokeReason.value.trim()) { revokeError.value = 'Vui lòng nhập lý do thu hồi thẻ.'; return }
  operationPending.value = true
  revokeError.value = ''
  try {
    await revokeLibraryCard(card.cardNo, { reason: revokeReason.value.trim() }, token)
    revokeDialogVisible.value = false
    await loadDetail()
  } catch (error) {
    if (isApiError(error, 401)) return
    revokeError.value = extractApiErrorMessage(error, 'Không thể thu hồi thẻ.')
  } finally { operationPending.value = false }
}

function cardStatusLabel(status: string): string { return ({ ACTIVE: 'Đang hoạt động', EXPIRED: 'Đã hết hạn', REVOKED: 'Đã thu hồi' } as Record<string, string>)[status] ?? status }
function cardSeverity(status: string): 'success' | 'warn' | 'danger' | 'secondary' { return status === 'ACTIVE' ? 'success' : status === 'EXPIRED' ? 'warn' : status === 'REVOKED' ? 'danger' : 'secondary' }

watch(() => route.params.patronId, () => { void loadDetail() }, { immediate: true })
onBeforeUnmount(() => { latestQr++; latestLoad++; clearQrUrl() })
</script>

<template>
  <section class="library-page page-content">
    <div class="page-heading">
      <div><p class="eyebrow">Thư viện · Bạn đọc</p><h1>Chi tiết hồ sơ bạn đọc</h1><p class="page-description">Thông tin hồ sơ và lịch sử cấp phát thẻ.</p></div>
      <Button label="Quay lại danh sách" icon="pi pi-arrow-left" severity="secondary" outlined @click="router.push({ name: 'v2-library-patrons' })" />
    </div>
    <PageState v-if="state === 'loading' || state === 'error'" :state="state" :forbidden="forbidden" :error-message="loadError" @retry="loadDetail" />
    <template v-else-if="patron">
      <div class="content-surface profile-surface">
        <div class="profile-heading"><div><p class="eyebrow">Mã hồ sơ {{ patron.patronId }} · Tài khoản {{ patron.userId }}</p><h2>{{ patron.displayName }}</h2></div><StatusTag :label="patron.status === 'ACTIVE' ? 'Đang hoạt động' : patron.status === 'BORROWING_SUSPENDED' ? 'Đình chỉ quyền mượn' : 'Đã đóng'" :severity="patron.status === 'ACTIVE' ? 'success' : patron.status === 'BORROWING_SUSPENDED' ? 'warn' : 'secondary'" /></div>
        <dl class="profile-data"><div><dt>Ngày tham gia</dt><dd>{{ formatLibraryDate(patron.joinedAt) }}</dd></div><div><dt>Tình trạng quyền mượn</dt><dd>{{ patron.status === 'BORROWING_SUSPENDED' ? 'Đang đình chỉ' : patron.status === 'ACTIVE' ? 'Không bị đình chỉ' : 'Hồ sơ đã đóng' }}</dd></div><div v-if="patron.suspensionReasons.length"><dt>Lý do đình chỉ</dt><dd>{{ patron.suspensionReasons.join(' · ') }}</dd></div></dl>
      </div>

      <div class="content-surface card-section">
        <div class="section-heading"><div><h2>Thẻ hiện tại</h2><p>Mã QR được lấy qua phiên đăng nhập và không lưu lâu dài.</p></div><div class="section-actions"><Button v-if="activeCard" label="Tải QR PNG" icon="pi pi-download" severity="secondary" outlined :disabled="!qrUrl" @click="downloadQr" /><Button v-if="activeCard" label="Cấp lại" icon="pi pi-id-card" severity="secondary" outlined @click="openIssue('reissue')" /><Button v-if="activeCard" label="Thu hồi" icon="pi pi-ban" severity="danger" outlined @click="openRevoke" /><Button v-if="patron.status === 'ACTIVE' && !activeCard" label="Phát hành thẻ" icon="pi pi-id-card" @click="openIssue('issue')" /></div></div>
        <FormAlert v-if="operationError" tone="error" :message="operationError" />
        <FormAlert v-if="qrError" tone="warning" :message="qrError" />
        <div v-if="activeCard" class="card-preview-area"><LibraryCardPreview :card-no="activeCard.cardNo" :display-name="patron.displayName" :patron-id="patron.patronId" :status="activeCard.status" :expires-at="activeCard.expiresAt" :qr-url="qrUrl" :downloading-qr="qrLoading" /><div class="card-meta"><p><span>Mã thẻ</span><strong>{{ activeCard.cardNo }}</strong></p><p><span>Ngày phát hành</span><strong>{{ formatLibraryDate(activeCard.issuedAt) }}</strong></p><p><span>Hiệu lực đến</span><strong>{{ formatLibraryDate(activeCard.expiresAt) }}</strong></p><p><span>Phiên bản chính sách</span><strong>{{ activeCard.policyVersion }}</strong></p></div></div>
        <div v-else class="no-card-state"><i class="pi pi-id-card" aria-hidden="true" /><strong>{{ patron.currentCard ? cardStatusLabel(patron.currentCard.status) : 'Chưa có thẻ đang hoạt động' }}</strong><p>{{ patron.status === 'ACTIVE' ? 'Có thể phát hành thẻ mới cho hồ sơ này.' : 'Hồ sơ cần ở trạng thái đang hoạt động trước khi phát hành thẻ.' }}</p></div>
      </div>

      <div class="content-surface history-section">
        <div class="section-heading"><div><h2>Lịch sử thẻ</h2><p>{{ history.length }} thẻ đã ghi nhận</p></div></div>
        <div v-if="history.length" class="history-list"><article v-for="card in history" :key="card.cardNo" class="history-row"><div><strong>{{ card.cardNo }}</strong><small>Phát hành {{ formatLibraryDate(card.issuedAt) }} · Hết hạn {{ formatLibraryDate(card.expiresAt) }}</small><small v-if="card.revokedAt">Thu hồi {{ formatLibraryDate(card.revokedAt) }}<template v-if="card.revokedReason"> · {{ card.revokedReason }}</template></small></div><StatusTag :label="cardStatusLabel(card.status)" :severity="cardSeverity(card.status)" /></article></div>
        <div v-else class="empty-state compact-empty"><strong>Chưa có lịch sử thẻ</strong><p>Các thẻ phát hành, thu hồi hoặc hết hạn sẽ xuất hiện ở đây.</p></div>
      </div>
    </template>

    <LibraryCardIssueDialog v-model:visible="issueDialogVisible" :mode="issueMode" :patron="patron" :saving="operationPending" :error-message="operationError" @submit="submitIssue" />
    <Dialog :visible="revokeDialogVisible" modal header="Thu hồi thẻ thư viện" :style="{ width: 'min(100% - 2rem, 540px)' }" :closable="!operationPending" @update:visible="revokeDialogVisible = $event"><div class="dialog-form"><p class="dialog-caption">Thẻ {{ activeCard?.cardNo }} sẽ mất hiệu lực ngay và không thể kích hoạt lại.</p><FormAlert v-if="revokeError" tone="error" :message="revokeError" /><div class="field-group"><label for="detail-revoke-reason">Lý do thu hồi <span class="required-mark" aria-hidden="true">*</span></label><InputText id="detail-revoke-reason" v-model="revokeReason" maxlength="500" fluid /></div><div class="form-actions"><Button label="Hủy" severity="secondary" outlined :disabled="operationPending" @click="revokeDialogVisible = false" /><Button label="Thu hồi thẻ" icon="pi pi-ban" severity="danger" :loading="operationPending" @click="revokeCard" /></div></div></Dialog>
  </section>
</template>

<style scoped>
.page-description { margin: .35rem 0 0; color: var(--text-color-secondary); }
.profile-surface, .card-section, .history-section { margin-top: 1.15rem; }
.profile-heading, .section-heading, .section-actions { display: flex; align-items: center; justify-content: space-between; gap: 1rem; }
.profile-heading h2 { margin: .2rem 0 0; font-size: 1.35rem; }
.profile-data { display: flex; flex-wrap: wrap; gap: 1.5rem 3rem; margin: 1.35rem 0 0; }
.profile-data div { min-width: 9rem; }
.profile-data dt, .card-meta span { color: var(--text-color-secondary); font-size: .83rem; }
.profile-data dd { margin: .22rem 0 0; font-weight: 600; }
.section-heading h2 { margin: 0; font-size: 1.1rem; }
.section-heading p { margin: .25rem 0 0; color: var(--text-color-secondary); font-size: .9rem; }
.card-preview-area { display: flex; flex-wrap: wrap; align-items: center; gap: 2rem; margin-top: 1rem; }
.card-meta { display: grid; gap: .75rem; min-width: 13rem; }
.card-meta p { display: grid; gap: .12rem; margin: 0; }
.history-list { display: grid; gap: .65rem; margin-top: .8rem; }
.history-row { display: flex; justify-content: space-between; align-items: center; gap: 1rem; padding: .9rem 1rem; border: 1px solid var(--surface-border); border-radius: .65rem; }
.history-row > div { display: grid; gap: .2rem; }
.history-row small, .dialog-caption { color: var(--text-color-secondary); }
.no-card-state { display: grid; justify-items: center; gap: .45rem; padding: 2rem 1rem; color: var(--text-color-secondary); text-align: center; }
.no-card-state i { color: var(--primary-color); font-size: 2.2rem; }
.no-card-state strong { color: var(--text-color); }
.compact-empty { padding: 1rem; }
.dialog-form { display: grid; gap: 1rem; }
.form-actions { display: flex; justify-content: flex-end; gap: .7rem; }
@media (max-width: 640px) { .profile-heading, .section-heading { align-items: flex-start; flex-direction: column; } .section-actions { flex-wrap: wrap; } .form-actions { justify-content: flex-start; flex-wrap: wrap; } }
</style>
