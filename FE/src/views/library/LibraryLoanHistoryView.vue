<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import Select from 'primevue/select'
import Tag from 'primevue/tag'

import { listLibraryLoans, renewLibraryLoan } from '@/services/library/libraryCirculationApi'
import { getLibraryErrorMessage } from '@/services/library/libraryErrorMessages'
import { getAuthSession } from '@/services/authSession'
import type { LibraryLoan, LibraryLoanPage, LibraryLoanStatus } from '@/types/library/circulation'
import { USER_ROLE } from '@/types/user'

const token = computed(() => getAuthSession()?.accessToken ?? '')
const roles = computed(() => getAuthSession()?.user.roles ?? [])
const isStaff = computed(() => roles.value.some((role) => role === USER_ROLE.ADMIN || role === USER_ROLE.LIBRARIAN))
const patronFilter = ref('')
const statusFilter = ref<LibraryLoanStatus | null>(null)
const page = ref(0)
const pageSize = 10
const result = ref<LibraryLoanPage | null>(null)
const loading = ref(false)
const busyLoanId = ref<number | null>(null)
const error = ref('')
const notice = ref('')
const statusOptions = [
  { label: 'Mọi trạng thái', value: null },
  { label: 'Đang mượn', value: 'ACTIVE' },
  { label: 'Đã trả', value: 'RETURNED' },
  { label: 'Báo mất', value: 'LOST' },
]

function displayDate(value: string | null): string {
  if (!value) return '—'
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date(value))
}

function severity(status: LibraryLoanStatus): 'success' | 'info' | 'danger' {
  if (status === 'ACTIVE') return 'info'
  if (status === 'RETURNED') return 'success'
  return 'danger'
}

async function loadLoans(): Promise<void> {
  if (!token.value) return
  loading.value = true
  error.value = ''
  try {
    const patronId = isStaff.value && patronFilter.value.trim() ? Number(patronFilter.value) : undefined
    result.value = await listLibraryLoans({
      patronId: Number.isInteger(patronId) && Number(patronId) > 0 ? Number(patronId) : undefined,
      status: statusFilter.value ?? undefined,
      page: page.value,
      pageSize,
    }, token.value)
  } catch (cause) {
    error.value = getLibraryErrorMessage(cause, 'Không thể tải lịch sử loan.')
  } finally {
    loading.value = false
  }
}

async function renew(loan: LibraryLoan): Promise<void> {
  if (!token.value || loan.status !== 'ACTIVE') return
  busyLoanId.value = loan.loanId
  error.value = ''
  notice.value = ''
  try {
    const updated = await renewLibraryLoan(loan.loanId, token.value)
    notice.value = `Đã gia hạn loan #${updated.loanId}. Hạn mới ${displayDate(updated.dueAt)}.`
    await loadLoans()
  } catch (cause) {
    error.value = getLibraryErrorMessage(cause, 'Không thể gia hạn loan. Hãy kiểm tra policy và reservation priority.')
  } finally {
    busyLoanId.value = null
  }
}

function changePage(nextPage: number): void {
  if (nextPage < 0 || (result.value && nextPage >= result.value.meta.totalPages)) return
  page.value = nextPage
  void loadLoans()
}

onMounted(() => void loadLoans())
</script>

<template>
  <section class="library-page page-content library-list-page">
    <header class="page-heading"><div><p class="eyebrow">Library v5</p><h1>Lịch sử loan</h1><p class="subtitle">Theo dõi hạn trả, số lần gia hạn và lịch sử trạng thái.</p></div></header>
    <Message v-if="notice" severity="success" :closable="true" @close="notice = ''">{{ notice }}</Message>
    <Message v-if="error" severity="error" :closable="true" @close="error = ''">{{ error }}</Message>
    <form class="filters content-surface" @submit.prevent="page = 0; loadLoans()">
      <label v-if="isStaff" class="filter-field">Patron ID<InputText v-model="patronFilter" inputmode="numeric" placeholder="Mọi bạn đọc" /></label>
      <label class="filter-field">Trạng thái<Select v-model="statusFilter" :options="statusOptions" option-label="label" option-value="value" /></label>
      <div class="filter-actions"><Button label="Lọc" icon="pi pi-search" type="submit" :loading="loading" /><Button label="Xóa lọc" severity="secondary" outlined @click="patronFilter = ''; statusFilter = null; page = 0; loadLoans()" /></div>
    </form>
    <div v-if="loading && !result" class="content-surface page-state">Đang tải lịch sử loan…</div>
    <div v-else-if="!loading && result?.result.length === 0" class="content-surface page-state"><strong>Chưa có loan phù hợp.</strong><span>Khi có giao dịch, trạng thái sẽ xuất hiện tại đây.</span></div>
    <div v-else-if="result" class="content-surface table-surface">
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>Loan</th><th>Đầu sách</th><th>Barcode</th><th>Mượn</th><th>Hạn trả</th><th>Gia hạn</th><th>Trạng thái</th><th>Policy</th><th>Thao tác</th></tr>
          </thead>
          <tbody>
            <tr v-for="loan in result.result" :key="loan.loanId">
              <td>#{{ loan.loanId }}</td><td>{{ loan.bookTitle }}</td><td><code>{{ loan.copyBarcode }}</code></td><td>{{ displayDate(loan.borrowedAt) }}</td><td>{{ displayDate(loan.dueAt) }}</td><td>{{ loan.renewCount }}</td><td><Tag :value="loan.status" :severity="severity(loan.status)" /></td><td>{{ loan.policyVersion }}</td><td><Button v-if="loan.status === 'ACTIVE'" label="Gia hạn" icon="pi pi-calendar-plus" size="small" severity="secondary" :loading="busyLoanId === loan.loanId" :disabled="busyLoanId !== null" @click="renew(loan)" /></td>
            </tr>
          </tbody>
        </table>
      </div>
      <footer class="pagination"><span>{{ result.meta.totalItems }} loan · trang {{ result.meta.page + 1 }} / {{ Math.max(result.meta.totalPages, 1) }}</span><div><Button icon="pi pi-chevron-left" aria-label="Trang trước" severity="secondary" text :disabled="loading || page === 0" @click="changePage(page - 1)" /><Button icon="pi pi-chevron-right" aria-label="Trang sau" severity="secondary" text :disabled="loading || page + 1 >= result.meta.totalPages" @click="changePage(page + 1)" /></div></footer>
    </div>
  </section>
</template>

<style scoped>
.library-list-page { display: grid; gap: 1rem; }
.content-surface { padding: 1rem; border: 1px solid var(--surface-border, #e2e8f0); border-radius: .75rem; background: var(--surface-card, #fff); }
.filters { display: flex; align-items: end; gap: .85rem; flex-wrap: wrap; }
.filter-field { display: grid; gap: .35rem; min-width: 13rem; font-size: .85rem; font-weight: 600; }
.filter-actions { display: flex; gap: .5rem; }
.table-surface { min-width: 0; padding: 0; overflow: hidden; }
.table-wrap { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; white-space: nowrap; }
th,td { padding: .75rem .85rem; border-bottom: 1px solid var(--surface-border, #e2e8f0); text-align: left; }
th { color: var(--text-color-secondary, #64748b); background: var(--surface-50, #f8fafc); font-size: .76rem; }
.pagination { display: flex; justify-content: space-between; align-items: center; padding: .5rem .85rem; color: var(--text-color-secondary, #64748b); font-size: .85rem; }
.pagination div { display: flex; }
.page-state { display: grid; gap: .4rem; color: var(--text-color-secondary, #64748b); }
</style>
