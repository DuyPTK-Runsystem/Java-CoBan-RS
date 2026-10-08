<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import Button from 'primevue/button'
import Column from 'primevue/column'
import DataTable, { type PageState as PrimePageState } from 'primevue/datatable'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import Paginator from 'primevue/paginator'
import Select from 'primevue/select'
import StatusTag from '@/components/common/StatusTag.vue'
import PageState from '@/components/common/PageState.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { listBooks } from '@/services/library/libraryCatalogApi'
import { extractApiErrorMessage, isApiError } from '@/types/api'
import type { BookAvailability, BookSummary } from '@/types/library/catalog'
import type { LoadingState } from '@/types/ui'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const { roles, requireAccessToken } = useAuthSession()
const books = ref<BookSummary[]>([])
const page = ref(0)
const size = ref(20)
const totalItems = ref(0)
const keyword = ref('')
const category = ref('')
const year = ref<number | null>(null)
const availability = ref<BookAvailability | ''>('')
const sort = ref('title,asc')
const loadingState = ref<LoadingState>('loading')
const forbidden = ref(false)
const errorMessage = ref('')
let latestRequest = 0

const canManage = computed(() => roles.value.some((role) => role === 'ADMIN' || role === 'LIBRARIAN'))
const availabilityOptions = [
  { label: 'Mọi tình trạng', value: '' },
  { label: 'Còn sách có thể mượn', value: 'AVAILABLE' },
  { label: 'Chưa có sách có thể mượn', value: 'UNAVAILABLE' },
]
const sortOptions = [
  { label: 'Tên sách A–Z', value: 'title,asc' },
  { label: 'Tên sách Z–A', value: 'title,desc' },
  { label: 'Tác giả A–Z', value: 'author,asc' },
  { label: 'Năm xuất bản mới nhất', value: 'publishedYear,desc' },
  { label: 'Giá bìa tăng dần', value: 'listPrice,asc' },
]

function queryString(key: string): string {
  const value = route.query[key]
  return typeof value === 'string' ? value : ''
}

function syncFromRoute(): void {
  keyword.value = queryString('keyword')
  category.value = queryString('category')
  const yearValue = Number(queryString('publishedYear'))
  year.value = Number.isInteger(yearValue) && yearValue > 0 ? yearValue : null
  const availabilityValue = queryString('availability')
  availability.value = availabilityValue === 'AVAILABLE' || availabilityValue === 'UNAVAILABLE' ? availabilityValue : ''
  const pageValue = Number(queryString('page'))
  page.value = Number.isInteger(pageValue) && pageValue >= 0 ? pageValue : 0
  const sizeValue = Number(queryString('size'))
  size.value = [10, 20, 50, 100].includes(sizeValue) ? sizeValue : 20
  sort.value = sortOptions.some((option) => option.value === queryString('sort')) ? queryString('sort') : 'title,asc'
}

function routeQuery(nextPage: number, nextSize: number) {
  return {
    keyword: keyword.value.trim() || undefined,
    category: category.value.trim() || undefined,
    publishedYear: year.value ?? undefined,
    availability: availability.value || undefined,
    page: nextPage || undefined,
    size: nextSize === 20 ? undefined : nextSize,
    sort: sort.value === 'title,asc' ? undefined : sort.value,
  }
}

function updateRouteQuery(query: ReturnType<typeof routeQuery>): void {
  const nextRoute = router.resolve({ name: 'v2-library-books', query })
  if (nextRoute.fullPath === route.fullPath) {
    void loadBooks()
  } else {
    void router.replace({ query })
  }
}

async function loadBooks(): Promise<void> {
  const token = requireAccessToken()
  if (!token) return
  const requestId = ++latestRequest
  loadingState.value = 'loading'
  forbidden.value = false
  errorMessage.value = ''
  try {
    const response = await listBooks({
      keyword: keyword.value.trim() || undefined,
      category: category.value.trim() || undefined,
      publishedYear: year.value ?? undefined,
      availability: availability.value || undefined,
      page: page.value,
      size: size.value,
      sort: sort.value,
    }, token)
    if (requestId !== latestRequest) return
    books.value = response.result
    totalItems.value = response.meta.totalItems
    loadingState.value = 'success'
  } catch (error) {
    if (requestId !== latestRequest) return
    if (isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    errorMessage.value = extractApiErrorMessage(error, 'Không thể tải danh mục sách.')
    loadingState.value = 'error'
  }
}

function applyFilters(): void {
  updateRouteQuery(routeQuery(0, size.value))
}

function handlePage(event: PrimePageState): void {
  const nextPage = Math.floor(event.first / event.rows)
  updateRouteQuery(routeQuery(nextPage, event.rows))
}

function clearFilters(): void {
  keyword.value = ''
  category.value = ''
  year.value = null
  availability.value = ''
  sort.value = 'title,asc'
  updateRouteQuery(routeQuery(0, size.value))
}

watch(() => route.query, () => {
  syncFromRoute()
  void loadBooks()
}, { immediate: true })
</script>

<template>
  <section class="library-page page-content">
    <div class="page-heading">
      <div><p class="eyebrow">Thư viện</p><h1>Danh mục sách</h1><p class="page-description">Tìm đầu sách và xem các bản sao đang có trong thư viện.</p></div>
      <Button v-if="canManage" label="Tạo đầu sách" icon="pi pi-plus" @click="router.push({ name: 'v2-library-book-create' })" />
    </div>

    <div class="content-surface library-filter-surface">
      <div class="library-filter-grid">
        <div class="field-group"><label for="book-search">Từ khóa</label><InputText id="book-search" v-model="keyword" placeholder="Tên sách, tác giả hoặc ISBN" fluid @keyup.enter="applyFilters" /></div>
        <div class="field-group"><label for="book-category">Thể loại</label><InputText id="book-category" v-model="category" placeholder="Ví dụ: Văn học" fluid @keyup.enter="applyFilters" /></div>
        <div class="field-group"><label for="book-year-filter">Năm xuất bản</label><InputNumber id="book-year-filter" v-model="year" :min="1" :max="9999" :use-grouping="false" placeholder="Tất cả các năm" fluid @keydown.enter="applyFilters" /></div>
        <div class="field-group"><label for="book-availability">Khả năng mượn</label><Select id="book-availability" v-model="availability" :options="availabilityOptions" option-label="label" option-value="value" fluid /></div>
        <div class="field-group"><label for="book-sort">Sắp xếp</label><Select id="book-sort" v-model="sort" :options="sortOptions" option-label="label" option-value="value" fluid /></div>
      </div>
      <div class="library-filter-actions"><Button label="Tìm kiếm" icon="pi pi-search" @click="applyFilters" /><Button label="Xóa bộ lọc" icon="pi pi-times" severity="secondary" outlined @click="clearFilters" /></div>
    </div>

    <PageState v-if="loadingState === 'loading' || loadingState === 'error'" :state="loadingState" :forbidden="forbidden" :error-message="errorMessage" @retry="loadBooks" />
    <div v-else class="content-surface library-results-surface">
      <div class="section-heading"><div><h2>Kết quả tìm kiếm</h2><p>{{ totalItems }} đầu sách</p></div></div>
      <div class="table-shell">
        <DataTable :value="books" lazy data-key="id" striped-rows responsive-layout="scroll" table-style="min-width: 62rem">
          <template #empty><div class="empty-state"><i class="pi pi-book" aria-hidden="true" /><strong>Không tìm thấy đầu sách</strong><p>Thử đổi từ khóa hoặc bộ lọc để tìm sách.</p></div></template>
          <Column header="Đầu sách"><template #body="{ data }"><div class="primary-cell"><strong>{{ data.title }}</strong><span>{{ data.author }}<template v-if="data.publishedYear"> · {{ data.publishedYear }}</template></span></div></template></Column>
          <Column header="ISBN"><template #body="{ data }">{{ data.isbn || '—' }}</template></Column>
          <Column header="Thể loại"><template #body="{ data }">{{ data.category || '—' }}</template></Column>
          <Column header="Bản sao có thể mượn"><template #body="{ data }"><StatusTag :label="`${data.availableBorrowableCopyCount} / ${data.totalCopyCount}`" :severity="data.availableBorrowableCopyCount ? 'success' : 'secondary'" /></template></Column>
          <Column header="Thao tác" style="width: 10rem"><template #body="{ data }"><Button label="Xem chi tiết" icon="pi pi-arrow-right" icon-pos="right" text @click="router.push({ name: 'v2-library-book-detail', params: { bookId: data.id } })" /></template></Column>
        </DataTable>
      </div>
      <Paginator v-if="totalItems > 0" :first="page * size" :rows="size" :total-records="totalItems" :rows-per-page-options="[10, 20, 50, 100]" @page="handlePage" />
    </div>
  </section>
</template>

<style scoped>
.library-filter-grid {
  display: grid;
  grid-template-columns: minmax(180px, 1.35fr) minmax(130px, 1fr) minmax(110px, 0.75fr) minmax(240px, 1.55fr) minmax(170px, 1.1fr);
  gap: 1rem;
}
.library-filter-grid .field-group { min-width: 0; }
.library-filter-actions { display: flex; justify-content: flex-end; gap: .75rem; margin-top: 1rem; }
.library-results-surface { margin-top: 1.25rem; }
.page-description { margin: .35rem 0 0; color: var(--text-color-secondary); }
@media (max-width: 1120px) { .library-filter-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 760px) { .library-filter-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 540px) { .library-filter-grid { grid-template-columns: 1fr; } .library-filter-actions { justify-content: flex-start; flex-wrap: wrap; } }
</style>
