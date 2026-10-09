<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Message from 'primevue/message'
import Select from 'primevue/select'
import Tag from 'primevue/tag'

import { getAuthSession } from '@/services/authSession'
import { getLibraryErrorMessage } from '@/services/library/libraryErrorMessages'
import { cancelLibraryReservation, createLibraryReservation, listLibraryReservations } from '@/services/library/libraryReservationApi'
import type { LibraryReservation, LibraryReservationPage, LibraryReservationStatus } from '@/types/library/reservation'
import { USER_ROLE } from '@/types/user'

const token = computed(() => getAuthSession()?.accessToken ?? '')
const roles = computed(() => getAuthSession()?.user.roles ?? [])
const isStaff = computed(() => roles.value.some((role) => role === USER_ROLE.ADMIN || role === USER_ROLE.LIBRARIAN))
const bookFilter = ref('')
const patronFilter = ref('')
const statusFilter = ref<LibraryReservationStatus | null>(null)
const newBookId = ref('')
const page = ref(0)
const pageSize = 10
const result = ref<LibraryReservationPage | null>(null)
const loading = ref(false)
const busyReservation = ref<number | null>(null)
const error = ref('')
const notice = ref('')
const statusOptions = [
  { label: 'Mọi trạng thái', value: null },
  { label: 'Đang chờ', value: 'WAITING' },
  { label: 'Sẵn sàng nhận', value: 'READY' },
  { label: 'Đã hoàn tất', value: 'FULFILLED' },
  { label: 'Đã hủy', value: 'CANCELLED' },
  { label: 'Đã hết hạn', value: 'EXPIRED' },
]

function displayDate(value: string | null): string {
  if (!value) return '—'
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date(value))
}

async function loadReservations(): Promise<void> {
  if (!token.value) return
  loading.value = true
  error.value = ''
  try {
    const bookId = bookFilter.value.trim() ? Number(bookFilter.value) : undefined
    const patronId = isStaff.value && patronFilter.value.trim() ? Number(patronFilter.value) : undefined
    result.value = await listLibraryReservations({
      bookId: Number.isInteger(bookId) && Number(bookId) > 0 ? Number(bookId) : undefined,
      patronId: Number.isInteger(patronId) && Number(patronId) > 0 ? Number(patronId) : undefined,
      status: statusFilter.value ?? undefined,
      page: page.value,
      pageSize,
    }, token.value)
  } catch (cause) {
    error.value = getLibraryErrorMessage(cause, 'Không thể tải danh sách reservation.')
  } finally {
    loading.value = false
  }
}

async function reserveBook(): Promise<void> {
  const bookId = Number(newBookId.value)
  if (!Number.isInteger(bookId) || bookId <= 0 || !token.value) return
  loading.value = true
  error.value = ''
  notice.value = ''
  try {
    const reservation = await createLibraryReservation({ bookId }, token.value)
    notice.value = `Đã tạo reservation #${reservation.reservationId}. Thứ tự queue do backend xác định.`
    newBookId.value = ''
    page.value = 0
    await loadReservations()
  } catch (cause) {
    error.value = getLibraryErrorMessage(cause, 'Không thể tạo reservation.')
  } finally {
    loading.value = false
  }
}

async function cancelReservation(reservation: LibraryReservation): Promise<void> {
  if (!token.value || !['WAITING', 'READY'].includes(reservation.status)) return
  busyReservation.value = reservation.reservationId
  error.value = ''
  notice.value = ''
  try {
    await cancelLibraryReservation(reservation.reservationId, token.value)
    notice.value = `Đã hủy reservation #${reservation.reservationId}. Lịch sử vẫn được lưu.`
    await loadReservations()
  } catch (cause) {
    error.value = getLibraryErrorMessage(cause, 'Không thể hủy reservation.')
  } finally {
    busyReservation.value = null
  }
}

function changePage(nextPage: number): void {
  if (nextPage < 0 || (result.value && nextPage >= result.value.meta.totalPages)) return
  page.value = nextPage
  void loadReservations()
}

onMounted(() => void loadReservations())
</script>

<template>
  <section class="library-page page-content reservation-page">
    <header class="page-heading"><div><p class="eyebrow">Library v5</p><h1>Hàng đợi reservation</h1><p class="subtitle">Theo dõi WAITING, READY, hạn nhận và lịch sử hủy.</p></div></header>
    <Message v-if="notice" severity="success" :closable="true" @close="notice = ''">{{ notice }}</Message>
    <Message v-if="error" severity="error" :closable="true" @close="error = ''">{{ error }}</Message>
    <form class="create-reservation content-surface" @submit.prevent="reserveBook">
      <label for="reserveBookId">Tạo reservation theo đầu sách</label>
      <InputText id="reserveBookId" v-model="newBookId" inputmode="numeric" placeholder="Book ID" />
      <Button label="Đặt giữ" icon="pi pi-bookmark" type="submit" :loading="loading" :disabled="!newBookId.trim()" />
    </form>
    <form class="filters content-surface" @submit.prevent="page = 0; loadReservations()">
      <label class="filter-field">Book ID<InputText v-model="bookFilter" inputmode="numeric" placeholder="Mọi đầu sách" /></label>
      <label v-if="isStaff" class="filter-field">Patron ID<InputText v-model="patronFilter" inputmode="numeric" placeholder="Mọi bạn đọc" /></label>
      <label class="filter-field">Trạng thái<Select v-model="statusFilter" :options="statusOptions" option-label="label" option-value="value" /></label>
      <div class="filter-actions"><Button label="Lọc" icon="pi pi-search" type="submit" :loading="loading" /><Button label="Xóa lọc" severity="secondary" outlined @click="bookFilter = ''; patronFilter = ''; statusFilter = null; page = 0; loadReservations()" /></div>
    </form>
    <div v-if="loading && !result" class="content-surface page-state">Đang tải hàng đợi…</div>
    <div v-else-if="!loading && result?.result.length === 0" class="content-surface page-state"><strong>Chưa có reservation phù hợp.</strong><span>Tạo reservation cho một đầu sách hoặc xóa bộ lọc.</span></div>
    <div v-else-if="result" class="content-surface table-surface">
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>Reservation</th><th>Ưu tiên</th><th>Đầu sách</th><th>Patron</th><th>Đặt lúc</th><th>Trạng thái</th><th>Bản sao</th><th>Hạn nhận</th><th /></tr>
          </thead>
          <tbody>
            <tr v-for="(reservation, index) in result.result" :key="reservation.reservationId">
              <td>#{{ reservation.reservationId }}</td><td>{{ (result.meta.page * result.meta.pageSize) + index + 1 }}</td><td>{{ reservation.bookTitle }}</td><td>#{{ reservation.patronId }}</td><td>{{ displayDate(reservation.reservedAt) }}</td><td><Tag :value="reservation.status" :severity="reservation.status === 'READY' ? 'success' : reservation.status === 'WAITING' ? 'warn' : 'secondary'" /></td><td>{{ reservation.allocatedCopyBarcode ?? '—' }}</td><td>{{ displayDate(reservation.pickupDueAt) }}</td><td><Button v-if="['WAITING', 'READY'].includes(reservation.status)" label="Hủy" icon="pi pi-times" severity="secondary" text :loading="busyReservation === reservation.reservationId" :disabled="busyReservation !== null" @click="cancelReservation(reservation)" /></td>
            </tr>
          </tbody>
        </table>
      </div>
      <footer class="pagination"><span>{{ result.meta.totalItems }} reservation · trang {{ result.meta.page + 1 }} / {{ Math.max(result.meta.totalPages, 1) }}</span><div><Button icon="pi pi-chevron-left" aria-label="Trang trước" severity="secondary" text :disabled="loading || page === 0" @click="changePage(page - 1)" /><Button icon="pi pi-chevron-right" aria-label="Trang sau" severity="secondary" text :disabled="loading || page + 1 >= result.meta.totalPages" @click="changePage(page + 1)" /></div></footer>
    </div>
    <p class="helper">Queue order là reservedAt rồi reservationId. Backend áp dụng FIFO, thời hạn nhận và hiệu lực policy.</p>
  </section>
</template>

<style scoped>
.reservation-page { display: grid; gap: 1rem; }
.content-surface { padding: 1rem; border: 1px solid var(--surface-border, #e2e8f0); border-radius: .75rem; background: var(--surface-card, #fff); }
.create-reservation { display: flex; align-items: end; gap: .65rem; flex-wrap: wrap; }
.create-reservation label { flex-basis: 100%; font-size: .85rem; font-weight: 600; }
.create-reservation input { width: min(100%, 24rem); }
.filters { display: flex; align-items: end; gap: .85rem; flex-wrap: wrap; }
.filter-field { display: grid; gap: .35rem; min-width: 12rem; font-size: .85rem; font-weight: 600; }
.filter-actions { display: flex; gap: .5rem; }
.table-surface { min-width: 0; padding: 0; overflow: hidden; }
.table-wrap { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; white-space: nowrap; }
th,td { padding: .75rem .85rem; border-bottom: 1px solid var(--surface-border, #e2e8f0); text-align: left; }
th { color: var(--text-color-secondary, #64748b); background: var(--surface-50, #f8fafc); font-size: .76rem; }
.pagination { display: flex; justify-content: space-between; align-items: center; padding: .5rem .85rem; color: var(--text-color-secondary, #64748b); font-size: .85rem; }
.pagination div { display: flex; }
.page-state { display: grid; gap: .4rem; color: var(--text-color-secondary, #64748b); }
.helper { color: var(--text-color-secondary, #64748b); font-size: .85rem; }
</style>
