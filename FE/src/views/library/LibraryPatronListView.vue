<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable from 'primevue/datatable'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Paginator from 'primevue/paginator'
import Select from 'primevue/select'
import { useRoute, useRouter } from 'vue-router'

import FormAlert from '@/components/common/FormAlert.vue'
import PageState from '@/components/common/PageState.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import LibraryCardIssueDialog from '@/components/library/LibraryCardIssueDialog.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import {
  activateLibraryPatron,
  issueLibraryCard,
  listLibraryPatrons,
  listPatronActivationCandidates,
  reissueLibraryCard,
  revokeLibraryCard,
  updateLibraryPatronStatus,
} from '@/services/library/libraryPatronApi'
import { USER_ROLE } from '@/types/user'
import { extractApiErrorMessage, isApiError } from '@/types/api'
import type {
  LibraryCardSummary,
  LibraryPatronActivationCandidate,
  LibraryPatronStatus,
  LibraryPatronSummary,
} from '@/types/library/patron'
import type { LoadingState } from '@/types/ui'

const route = useRoute()
const router = useRouter()
const { roles, requireAccessToken } = useAuthSession()
const canManage = computed(() => roles.value.some((role) => role === USER_ROLE.ADMIN || role === USER_ROLE.LIBRARIAN))

const patrons = ref<LibraryPatronSummary[]>([])
const page = ref(0)
const size = ref(20)
const totalItems = ref(0)
const keyword = ref('')
const statusFilter = ref<LibraryPatronStatus | ''>('')
const loadingState = ref<LoadingState>('loading')
const forbidden = ref(false)
const loadError = ref('')
const operationError = ref('')
const operationTone = ref<'error' | 'warning'>('error')
const statusMessage = ref('')
const operationPending = ref(false)
const activateDialogVisible = ref(false)
const candidateKeyword = ref('')
const candidates = ref<LibraryPatronActivationCandidate[]>([])
const candidatePage = ref(0)
const candidateTotal = ref(0)
const candidateLoading = ref(false)
const candidateError = ref('')
const selectedCandidate = ref<LibraryPatronActivationCandidate | null>(null)
const selectedPatron = ref<LibraryPatronSummary | null>(null)
const nextPatronStatus = ref<LibraryPatronStatus>('ACTIVE')
const statusReason = ref('')
const statusDialogVisible = ref(false)
const issueDialogVisible = ref(false)
const issueMode = ref<'issue' | 'reissue'>('issue')
const revokeDialogVisible = ref(false)
const revokeReason = ref('')
const revokeError = ref('')
let latestListRequest = 0
let latestCandidateRequest = 0

const statusOptions = [
  { label: 'Mọi trạng thái', value: '' },
  { label: 'Đang hoạt động', value: 'ACTIVE' },
  { label: 'Đình chỉ quyền mượn', value: 'BORROWING_SUSPENDED' },
  { label: 'Đã đóng', value: 'CLOSED' },
]
const patronStatusOptions = statusOptions.slice(1)
const statusLabels: Record<LibraryPatronStatus, string> = {
  ACTIVE: 'Đang hoạt động',
  BORROWING_SUSPENDED: 'Đình chỉ quyền mượn',
  CLOSED: 'Đã đóng',
}
const cardLabels: Record<LibraryCardSummary['status'], string> = {
  ACTIVE: 'Đang hoạt động',
  EXPIRED: 'Đã hết hạn',
  REVOKED: 'Đã thu hồi',
}

function queryString(key: string): string {
  const value = route.query[key]
  return typeof value === 'string' ? value : ''
}

function syncFromRoute(): void {
  keyword.value = queryString('keyword')
  const status = queryString('status')
  statusFilter.value = status === 'ACTIVE' || status === 'BORROWING_SUSPENDED' || status === 'CLOSED' ? status : ''
  const pageValue = Number(queryString('page'))
  page.value = Number.isInteger(pageValue) && pageValue >= 0 ? pageValue : 0
  const sizeValue = Number(queryString('size'))
  size.value = [10, 20, 50, 100].includes(sizeValue) ? sizeValue : 20
}

function updateQuery(nextPage: number, nextSize = size.value): void {
  const query = {
    keyword: keyword.value.trim() || undefined,
    status: statusFilter.value || undefined,
    page: nextPage || undefined,
    size: nextSize === 20 ? undefined : nextSize,
  }
  const resolved = router.resolve({ name: 'v2-library-patrons', query })
  if (resolved.fullPath === route.fullPath) void loadPatrons()
  else void router.replace({ query })
}

async function loadPatrons(): Promise<void> {
  const token = requireAccessToken()
  if (!token) return
  const requestId = ++latestListRequest
  loadingState.value = 'loading'
  forbidden.value = false
  loadError.value = ''
  try {
    const response = await listLibraryPatrons({
      keyword: keyword.value.trim() || undefined,
      status: statusFilter.value || undefined,
      page: page.value,
      size: size.value,
      sort: 'joinedAt,DESC',
    }, token)
    if (requestId !== latestListRequest) return
    patrons.value = response.result
    totalItems.value = response.meta.totalItems
    loadingState.value = 'success'
  } catch (error) {
    if (requestId !== latestListRequest || isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    loadError.value = extractApiErrorMessage(error, 'Không thể tải danh sách bạn đọc.')
    loadingState.value = 'error'
  }
}

function applyFilters(): void { updateQuery(0) }
function clearFilters(): void { keyword.value = ''; statusFilter.value = ''; updateQuery(0) }
function handlePage(event: { page: number; rows: number }): void { updateQuery(event.page, event.rows) }

async function loadCandidates(nextPage = 0): Promise<void> {
  const token = requireAccessToken()
  if (!token) return
  const requestId = ++latestCandidateRequest
  candidateLoading.value = true
  candidateError.value = ''
  try {
    const response = await listPatronActivationCandidates(candidateKeyword.value, nextPage, 10, token)
    if (requestId !== latestCandidateRequest) return
    candidates.value = response.result
    candidatePage.value = response.meta.page
    candidateTotal.value = response.meta.totalItems
    selectedCandidate.value = null
  } catch (error) {
    if (requestId !== latestCandidateRequest || isApiError(error, 401)) return
    candidateError.value = extractApiErrorMessage(error, 'Không thể tải tài khoản đủ điều kiện kích hoạt.')
  } finally {
    if (requestId === latestCandidateRequest) candidateLoading.value = false
  }
}

function openActivation(): void {
  operationError.value = ''
  candidateKeyword.value = ''
  activateDialogVisible.value = true
  void loadCandidates(0)
}

async function activateSelected(): Promise<void> {
  const token = requireAccessToken()
  if (!token || !selectedCandidate.value || operationPending.value) return
  operationPending.value = true
  operationError.value = ''
  try {
    await activateLibraryPatron({ userId: selectedCandidate.value.userId }, token)
    statusMessage.value = `Đã kích hoạt hồ sơ bạn đọc cho ${selectedCandidate.value.displayName}.`
    activateDialogVisible.value = false
    selectedCandidate.value = null
    await loadPatrons()
  } catch (error) {
    if (isApiError(error, 401)) return
    operationTone.value = isApiError(error, 409) ? 'warning' : 'error'
    operationError.value = extractApiErrorMessage(error, 'Không thể kích hoạt hồ sơ bạn đọc.')
    await loadCandidates(candidatePage.value)
  } finally { operationPending.value = false }
}

function openStatusDialog(patron: LibraryPatronSummary): void {
  selectedPatron.value = patron
  nextPatronStatus.value = patron.status
  statusReason.value = ''
  operationError.value = ''
  statusDialogVisible.value = true
}

async function saveStatus(): Promise<void> {
  const token = requireAccessToken()
  const patron = selectedPatron.value
  if (!token || !patron || operationPending.value) return
  if (!statusReason.value.trim()) { operationError.value = 'Vui lòng nhập lý do thay đổi trạng thái.'; return }
  operationPending.value = true
  operationError.value = ''
  try {
    await updateLibraryPatronStatus(patron.patronId, { status: nextPatronStatus.value, reason: statusReason.value.trim() }, token)
    statusDialogVisible.value = false
    statusMessage.value = `Đã cập nhật trạng thái hồ sơ ${patron.displayName}.`
    await loadPatrons()
  } catch (error) {
    if (isApiError(error, 401)) return
    operationTone.value = isApiError(error, 409) ? 'warning' : 'error'
    operationError.value = extractApiErrorMessage(error, 'Không thể cập nhật trạng thái hồ sơ.')
  } finally { operationPending.value = false }
}

function openIssueDialog(patron: LibraryPatronSummary, mode: 'issue' | 'reissue'): void {
  selectedPatron.value = patron
  issueMode.value = mode
  operationError.value = ''
  issueDialogVisible.value = true
}

async function submitIssue(request: { expiresAt: string; reason?: string }): Promise<void> {
  const token = requireAccessToken()
  const patron = selectedPatron.value
  if (!token || !patron || operationPending.value) return
  operationPending.value = true
  operationError.value = ''
  try {
    if (issueMode.value === 'reissue' && patron.currentCard) {
      await reissueLibraryCard(patron.currentCard.cardNo, { expiresAt: request.expiresAt, reason: request.reason ?? '' }, token)
    } else {
      await issueLibraryCard({ patronId: patron.patronId, expiresAt: request.expiresAt }, token)
    }
    issueDialogVisible.value = false
    statusMessage.value = issueMode.value === 'reissue' ? 'Đã cấp lại thẻ thư viện.' : `Đã phát hành thẻ cho ${patron.displayName}.`
    await loadPatrons()
  } catch (error) {
    if (isApiError(error, 401)) return
    operationTone.value = isApiError(error, 409) ? 'warning' : 'error'
    operationError.value = extractApiErrorMessage(error, 'Không thể phát hành thẻ thư viện.')
  } finally { operationPending.value = false }
}

function openRevokeDialog(patron: LibraryPatronSummary): void {
  selectedPatron.value = patron
  revokeReason.value = ''
  revokeError.value = ''
  revokeDialogVisible.value = true
}

async function revokeSelectedCard(): Promise<void> {
  const token = requireAccessToken()
  const card = selectedPatron.value?.currentCard
  if (!token || !card || operationPending.value) return
  if (!revokeReason.value.trim()) { revokeError.value = 'Vui lòng nhập lý do thu hồi thẻ.'; return }
  operationPending.value = true
  revokeError.value = ''
  try {
    await revokeLibraryCard(card.cardNo, { reason: revokeReason.value.trim() }, token)
    revokeDialogVisible.value = false
    statusMessage.value = `Đã thu hồi thẻ ${card.cardNo}.`
    await loadPatrons()
  } catch (error) {
    if (isApiError(error, 401)) return
    revokeError.value = extractApiErrorMessage(error, 'Không thể thu hồi thẻ.')
  } finally { operationPending.value = false }
}

watch(() => route.query, () => { syncFromRoute(); void loadPatrons() }, { immediate: true })
</script>

<template>
  <section class="library-page page-content">
    <div class="page-heading">
      <div><p class="eyebrow">Thư viện</p><h1>Bạn đọc &amp; thẻ thư viện</h1><p class="page-description">Kích hoạt hồ sơ, quản lý trạng thái và vòng đời thẻ bạn đọc.</p></div>
      <Button label="Kích hoạt bạn đọc" icon="pi pi-user-plus" @click="openActivation" />
    </div>

    <FormAlert v-if="statusMessage" tone="success" :message="statusMessage" />
    <div class="content-surface patron-filter-surface">
      <div class="patron-filter-grid">
        <div class="field-group"><label for="patron-keyword">Từ khóa</label><InputText id="patron-keyword" v-model="keyword" placeholder="Tên bạn đọc hoặc mã hồ sơ" fluid @keyup.enter="applyFilters" /></div>
        <div class="field-group"><label for="patron-status-filter">Trạng thái hồ sơ</label><Select id="patron-status-filter" v-model="statusFilter" :options="statusOptions" option-label="label" option-value="value" fluid /></div>
      </div>
      <div class="filter-actions"><Button label="Tìm kiếm" icon="pi pi-search" @click="applyFilters" /><Button label="Xóa bộ lọc" icon="pi pi-filter-slash" severity="secondary" outlined @click="clearFilters" /></div>
    </div>

    <PageState v-if="loadingState === 'loading' || loadingState === 'error'" :state="loadingState" :forbidden="forbidden" :error-message="loadError" @retry="loadPatrons" />
    <div v-else class="content-surface patron-results-surface">
      <div class="section-heading"><div><h2>Danh sách bạn đọc</h2><p>{{ totalItems }} hồ sơ</p></div></div>
      <div class="table-shell">
        <DataTable :value="patrons" lazy data-key="patronId" striped-rows responsive-layout="scroll" table-style="min-width: 68rem">
          <template #empty><div class="empty-state"><i class="pi pi-users" aria-hidden="true" /><strong>Chưa có bạn đọc phù hợp</strong><p>Điều chỉnh bộ lọc hoặc kích hoạt hồ sơ từ một tài khoản hiện có.</p><Button v-if="canManage" label="Kích hoạt bạn đọc" icon="pi pi-user-plus" @click="openActivation" /></div></template>
          <Column header="Bạn đọc"><template #body="{ data }"><div class="primary-cell"><strong>{{ data.displayName }}</strong><span>Mã hồ sơ {{ data.patronId }} · Tài khoản {{ data.userId }}</span></div></template></Column>
          <Column header="Trạng thái hồ sơ"><template #body="{ data }"><StatusTag :label="statusLabels[data.status]" :severity="data.status === 'ACTIVE' ? 'success' : data.status === 'BORROWING_SUSPENDED' ? 'warn' : 'secondary'" /></template></Column>
          <Column header="Thẻ hiện tại"><template #body="{ data }"><StatusTag v-if="data.currentCard" :label="`${cardLabels[data.currentCard.status]} · ${data.currentCard.cardNo}`" :severity="data.currentCard.status === 'ACTIVE' ? 'success' : data.currentCard.status === 'EXPIRED' ? 'warn' : 'danger'" /><span v-else class="muted-copy">Chưa có thẻ</span></template></Column>
          <Column header="Ngày tham gia"><template #body="{ data }">{{ data.joinedAt ? new Date(data.joinedAt).toLocaleDateString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' }) : '—' }}</template></Column>
          <Column header="Thao tác" style="width: 21rem"><template #body="{ data }"><div class="row-actions"><Button label="Chi tiết" icon="pi pi-arrow-right" icon-pos="right" text @click="router.push({ name: 'v2-library-patron-detail', params: { patronId: data.patronId } })" /><Button label="Trạng thái" icon="pi pi-sliders-h" text @click="openStatusDialog(data)" /><template v-if="data.currentCard?.status === 'ACTIVE'"><Button label="Cấp lại" icon="pi pi-id-card" text @click="openIssueDialog(data, 'reissue')" /><Button label="Thu hồi" icon="pi pi-ban" severity="danger" text @click="openRevokeDialog(data)" /></template><Button v-else-if="data.status === 'ACTIVE'" label="Cấp thẻ" icon="pi pi-id-card" text @click="openIssueDialog(data, 'issue')" /></div></template></Column>
        </DataTable>
      </div>
      <Paginator v-if="totalItems > 0" :first="page * size" :rows="size" :total-records="totalItems" :rows-per-page-options="[10, 20, 50, 100]" @page="handlePage" />
    </div>

    <Dialog :visible="activateDialogVisible" modal header="Kích hoạt hồ sơ bạn đọc" :style="{ width: 'min(100% - 2rem, 720px)' }" :closable="!operationPending" @update:visible="activateDialogVisible = $event">
      <div class="activation-content">
        <p class="dialog-caption">Chọn tài khoản hiện hữu chưa có hồ sơ. Tài khoản ADMIN và LIBRARIAN vẫn có thể có hồ sơ, nhưng không đủ điều kiện mượn sách.</p>
        <div class="activation-search"><InputText v-model="candidateKeyword" placeholder="Tìm theo tên hoặc tài khoản" fluid @keyup.enter="loadCandidates(0)" /><Button label="Tìm" icon="pi pi-search" :disabled="candidateLoading" @click="loadCandidates(0)" /></div>
        <FormAlert v-if="operationError" :tone="operationTone" :message="operationError" />
        <div v-if="candidateLoading" class="page-state page-state-loading" role="status"><i class="pi pi-spin pi-spinner" aria-hidden="true" />Đang tìm tài khoản...</div>
        <FormAlert v-else-if="candidateError" tone="error" :message="candidateError" />
        <div v-else-if="!candidates.length" class="empty-state compact-empty"><strong>Không có tài khoản phù hợp</strong><p>Thử từ khóa khác hoặc kiểm tra các tài khoản đã kích hoạt.</p></div>
        <div v-else class="candidate-list" role="listbox" aria-label="Tài khoản có thể kích hoạt">
          <button v-for="candidate in candidates" :key="candidate.userId" type="button" class="candidate-option" :class="{ selected: selectedCandidate?.userId === candidate.userId }" role="option" :aria-selected="selectedCandidate?.userId === candidate.userId" @click="selectedCandidate = candidate"><span><strong>{{ candidate.displayName }}</strong><small>{{ candidate.username }} · {{ candidate.roleCode }}</small></span><i :class="selectedCandidate?.userId === candidate.userId ? 'pi pi-check-circle' : 'pi pi-circle'" aria-hidden="true" /></button>
        </div>
        <Paginator v-if="candidateTotal > 10" :first="candidatePage * 10" :rows="10" :total-records="candidateTotal" :rows-per-page-options="[10]" @page="loadCandidates($event.page)" />
        <div class="form-actions"><Button label="Đóng" severity="secondary" outlined :disabled="operationPending" @click="activateDialogVisible = false" /><Button label="Kích hoạt hồ sơ" icon="pi pi-check" :loading="operationPending" :disabled="!selectedCandidate" @click="activateSelected" /></div>
      </div>
    </Dialog>

    <Dialog :visible="statusDialogVisible" modal header="Thay đổi trạng thái bạn đọc" :style="{ width: 'min(100% - 2rem, 560px)' }" :closable="!operationPending" @update:visible="statusDialogVisible = $event">
      <div class="dialog-form"><p class="dialog-caption"><strong>{{ selectedPatron?.displayName }}</strong><span>Trạng thái thư viện không ảnh hưởng đăng nhập hoặc chức năng học vụ.</span></p><FormAlert v-if="operationError" :tone="operationTone" :message="operationError" /><div class="field-group"><label for="patron-next-status">Trạng thái mới</label><Select id="patron-next-status" v-model="nextPatronStatus" :options="patronStatusOptions" option-label="label" option-value="value" fluid /></div><div class="field-group"><label for="patron-status-reason">Lý do thay đổi <span class="required-mark" aria-hidden="true">*</span></label><InputText id="patron-status-reason" v-model="statusReason" maxlength="500" fluid /></div><div class="form-actions"><Button label="Hủy" severity="secondary" outlined :disabled="operationPending" @click="statusDialogVisible = false" /><Button label="Lưu trạng thái" icon="pi pi-check" :loading="operationPending" @click="saveStatus" /></div></div>
    </Dialog>

    <LibraryCardIssueDialog v-model:visible="issueDialogVisible" :mode="issueMode" :patron="selectedPatron" :saving="operationPending" :error-message="operationError" @submit="submitIssue" />

    <Dialog :visible="revokeDialogVisible" modal header="Thu hồi thẻ thư viện" :style="{ width: 'min(100% - 2rem, 560px)' }" :closable="!operationPending" @update:visible="revokeDialogVisible = $event">
      <div class="dialog-form"><p class="dialog-caption"><strong>{{ selectedPatron?.currentCard?.cardNo }}</strong><span>Thẻ đã thu hồi không thể kích hoạt lại.</span></p><FormAlert v-if="revokeError" tone="error" :message="revokeError" /><div class="field-group"><label for="card-revoke-reason">Lý do thu hồi <span class="required-mark" aria-hidden="true">*</span></label><InputText id="card-revoke-reason" v-model="revokeReason" maxlength="500" placeholder="Ví dụ: thẻ bị mất, hư hỏng hoặc thay thế" fluid /></div><div class="form-actions"><Button label="Hủy" severity="secondary" outlined :disabled="operationPending" @click="revokeDialogVisible = false" /><Button label="Thu hồi thẻ" icon="pi pi-ban" severity="danger" :loading="operationPending" @click="revokeSelectedCard" /></div></div>
    </Dialog>
  </section>
</template>

<style scoped>
.page-description { margin: .35rem 0 0; color: var(--text-color-secondary); }
.patron-filter-grid { display: grid; grid-template-columns: minmax(220px, 1.6fr) minmax(220px, 1fr); gap: 1rem; }
.filter-actions, .form-actions { display: flex; justify-content: flex-end; gap: .7rem; margin-top: 1rem; }
.patron-results-surface { margin-top: 1.25rem; }
.row-actions { display: flex; flex-wrap: wrap; align-items: center; gap: .15rem; }
.muted-copy, .dialog-caption { color: var(--text-color-secondary); }
.dialog-caption { margin: 0; line-height: 1.5; }
.activation-content, .dialog-form { display: grid; gap: 1rem; }
.activation-search { display: grid; grid-template-columns: 1fr auto; gap: .65rem; }
.candidate-list { display: grid; max-height: 19rem; overflow: auto; border: 1px solid var(--surface-border); border-radius: .65rem; }
.candidate-option { display: flex; justify-content: space-between; align-items: center; gap: 1rem; padding: .85rem 1rem; border: 0; border-bottom: 1px solid var(--surface-border); background: var(--surface-card); color: var(--text-color); text-align: left; cursor: pointer; }
.candidate-option:last-child { border-bottom: 0; }
.candidate-option:hover, .candidate-option.selected { background: var(--primary-50); }
.candidate-option > span { display: grid; gap: .2rem; }
.candidate-option small { color: var(--text-color-secondary); }
.candidate-option > i { color: var(--primary-color); }
.compact-empty { padding: 1rem; }
@media (max-width: 640px) { .patron-filter-grid { grid-template-columns: 1fr; } .filter-actions, .form-actions { justify-content: flex-start; flex-wrap: wrap; } }
</style>
