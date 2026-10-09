<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import Button from 'primevue/button'

import FormAlert from '@/components/common/FormAlert.vue'
import PageState from '@/components/common/PageState.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import LibraryCardPreview from '@/components/library/LibraryCardPreview.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { getLibraryCardQr, getMyLibraryCard, getMyLibraryPatron, listMyLibraryCardHistory } from '@/services/library/libraryPatronApi'
import { extractApiErrorMessage, isApiError } from '@/types/api'
import type { LibraryCardHistory, LibraryCardSummary, LibraryPatronSummary } from '@/types/library/patron'
import type { LoadingState } from '@/types/ui'
import { formatLibraryDate } from '@/utils/libraryCardDates'

const { requireAccessToken } = useAuthSession()
const patron = ref<LibraryPatronSummary | null>(null)
const card = ref<LibraryCardSummary | null>(null)
const history = ref<LibraryCardHistory>([])
const state = ref<LoadingState>('loading')
const forbidden = ref(false)
const errorMessage = ref('')
const qrUrl = ref('')
const qrLoading = ref(false)
const qrError = ref('')
let requestSequence = 0

function clearQrUrl(): void {
  if (!qrUrl.value) return
  URL.revokeObjectURL(qrUrl.value)
  qrUrl.value = ''
}

async function loadMyCard(): Promise<void> {
  const token = requireAccessToken()
  if (!token) return
  const requestId = ++requestSequence
  state.value = 'loading'
  forbidden.value = false
  errorMessage.value = ''
  qrError.value = ''
  clearQrUrl()
  try {
    const ownPatron = await getMyLibraryPatron(token)
    if (requestId !== requestSequence) return
    patron.value = ownPatron
    const [ownCard, cardHistory] = await Promise.all([
      getMyLibraryCard(token).catch((error: unknown) => isApiError(error, 404) ? null : Promise.reject(error)),
      listMyLibraryCardHistory(token).catch((error: unknown) => isApiError(error, 404) ? [] : Promise.reject(error)),
    ])
    if (requestId !== requestSequence) return
    card.value = ownCard
    history.value = cardHistory
    state.value = ownCard ? 'success' : 'empty'
    if (ownCard?.status === 'ACTIVE') void loadQr(ownCard.cardNo, requestId)
  } catch (error) {
    if (requestId !== requestSequence || isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    errorMessage.value = isApiError(error, 404)
      ? 'Tài khoản của bạn chưa có hồ sơ bạn đọc được kích hoạt.'
      : extractApiErrorMessage(error, 'Không thể tải thẻ thư viện của bạn.')
    state.value = isApiError(error, 404) ? 'empty' : 'error'
  }
}

async function loadQr(cardNo: string, requestId: number): Promise<void> {
  const token = requireAccessToken()
  if (!token) return
  qrLoading.value = true
  qrError.value = ''
  try {
    const png = await getLibraryCardQr(cardNo, token)
    if (requestId !== requestSequence) return
    qrUrl.value = URL.createObjectURL(png)
  } catch (error) {
    if (requestId !== requestSequence || isApiError(error, 401)) return
    qrError.value = extractApiErrorMessage(error, 'Không thể tải mã QR. Thử tải lại để tiếp tục.')
  } finally { if (requestId === requestSequence) qrLoading.value = false }
}

function downloadQr(): void {
  if (!qrUrl.value || !card.value) return
  const anchor = document.createElement('a')
  anchor.href = qrUrl.value
  anchor.download = `${card.value.cardNo}-qr.png`
  anchor.click()
}

function reloadQr(): void {
  if (card.value?.status === 'ACTIVE') void loadQr(card.value.cardNo, requestSequence)
}

function cardStatusLabel(status: string): string { return ({ ACTIVE: 'Đang hoạt động', EXPIRED: 'Đã hết hạn', REVOKED: 'Đã thu hồi' } as Record<string, string>)[status] ?? status }
function cardSeverity(status: string): 'success' | 'warn' | 'danger' | 'secondary' { return status === 'ACTIVE' ? 'success' : status === 'EXPIRED' ? 'warn' : status === 'REVOKED' ? 'danger' : 'secondary' }

onMounted(() => { void loadMyCard() })
onBeforeUnmount(() => { requestSequence++; clearQrUrl() })
</script>

<template>
  <section class="library-page page-content">
    <div class="page-heading"><div><p class="eyebrow">Thư viện</p><h1>Thẻ thư viện của tôi</h1><p class="page-description">Xem thẻ điện tử của bạn. Mã QR được tải qua phiên đăng nhập và có thể tải về khi cần.</p></div><Button label="Làm mới" icon="pi pi-refresh" severity="secondary" outlined @click="loadMyCard" /></div>
    <PageState v-if="state === 'loading' || state === 'error' || state === 'empty'" :state="state" :forbidden="forbidden" :error-message="errorMessage" empty-heading="Chưa có thẻ thư viện" :empty-message="errorMessage || 'Hồ sơ của bạn chưa có thẻ đang hoạt động. Hãy liên hệ thư viện để được hỗ trợ.'" @retry="loadMyCard" />
    <template v-else-if="patron && card">
      <div class="content-surface my-card-surface">
        <div class="card-header"><div><p class="eyebrow">Bạn đọc {{ patron.patronId }}</p><h2>{{ patron.displayName }}</h2></div><StatusTag :label="cardStatusLabel(card.status)" :severity="cardSeverity(card.status)" /></div>
        <FormAlert v-if="qrError" tone="warning" :message="qrError" />
        <div class="self-card-content"><LibraryCardPreview :card-no="card.cardNo" :display-name="patron.displayName" :patron-id="patron.patronId" :status="card.status" :expires-at="card.expiresAt" :qr-url="qrUrl" :downloading-qr="qrLoading" /><div class="card-actions"><div class="card-fact"><span>Mã thẻ</span><strong>{{ card.cardNo }}</strong></div><div class="card-fact"><span>Ngày phát hành</span><strong>{{ formatLibraryDate(card.issuedAt) }}</strong></div><div class="card-fact"><span>Hiệu lực đến</span><strong>{{ formatLibraryDate(card.expiresAt) }}</strong></div><Button v-if="card.status === 'ACTIVE'" label="Tải ảnh QR PNG" icon="pi pi-download" :disabled="!qrUrl || qrLoading" @click="downloadQr" /><Button v-if="qrError" label="Tải lại mã QR" icon="pi pi-refresh" severity="secondary" outlined :loading="qrLoading" @click="reloadQr" /></div></div>
        <p v-if="patron.status === 'BORROWING_SUSPENDED'" class="suspension-note"><i class="pi pi-info-circle" aria-hidden="true" /> Hồ sơ đang bị đình chỉ quyền mượn. Thẻ vẫn hiển thị để bạn đối chiếu; hãy liên hệ thư viện để biết thêm thông tin.</p>
      </div>
      <div v-if="history.length > 1" class="content-surface history-surface"><div class="section-heading"><div><h2>Lịch sử thẻ</h2><p>Các thẻ trước đây được giữ trong lịch sử hồ sơ.</p></div></div><div class="history-list"><article v-for="oldCard in history" :key="oldCard.cardNo" class="history-row"><div><strong>{{ oldCard.cardNo }}</strong><small>Phát hành {{ formatLibraryDate(oldCard.issuedAt) }} · Hết hạn {{ formatLibraryDate(oldCard.expiresAt) }}</small></div><StatusTag :label="cardStatusLabel(oldCard.status)" :severity="cardSeverity(oldCard.status)" /></article></div></div>
    </template>
  </section>
</template>

<style scoped>
.page-description { margin: .35rem 0 0; color: var(--text-color-secondary); }
.my-card-surface, .history-surface { margin-top: 1.1rem; }
.card-header, .section-heading { display: flex; align-items: center; justify-content: space-between; gap: 1rem; }
.card-header h2, .section-heading h2 { margin: .2rem 0 0; font-size: 1.2rem; }
.self-card-content { display: flex; flex-wrap: wrap; align-items: center; gap: 2rem; margin-top: 1.2rem; }
.card-actions { display: grid; justify-items: start; gap: .85rem; }
.card-fact { display: grid; gap: .15rem; }
.card-fact span, .section-heading p { color: var(--text-color-secondary); font-size: .85rem; }
.section-heading p { margin: .25rem 0 0; }
.suspension-note { display: flex; gap: .5rem; align-items: flex-start; margin: 1.2rem 0 0; padding: .8rem 1rem; border-radius: .6rem; background: #fef3c7; color: #92400e; }
.history-list { display: grid; gap: .65rem; margin-top: .9rem; }
.history-row { display: flex; justify-content: space-between; align-items: center; gap: 1rem; padding: .9rem 1rem; border: 1px solid var(--surface-border); border-radius: .65rem; }
.history-row > div { display: grid; gap: .2rem; }
.history-row small { color: var(--text-color-secondary); }
@media (max-width: 640px) { .card-header, .section-heading { align-items: flex-start; flex-direction: column; } }
</style>
