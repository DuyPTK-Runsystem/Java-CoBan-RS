<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import Button from 'primevue/button'
import { useRoute, useRouter } from 'vue-router'

import BookForm from '@/components/library/BookForm.vue'
import PageState from '@/components/common/PageState.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import { createBook, getBook, updateBook } from '@/services/library/libraryCatalogApi'
import { ApiError, extractApiErrorMessage, isApiError } from '@/types/api'
import type { BookDetail, BookFormValues } from '@/types/library/catalog'
import type { LoadingState } from '@/types/ui'

const route = useRoute()
const router = useRouter()
const { roles, requireAccessToken } = useAuthSession()
const book = ref<BookDetail | null>(null)
const loadingState = ref<LoadingState>('loading')
const loadingError = ref('')
const saving = ref(false)
const saveError = ref('')
const saveErrorTone = ref<'error' | 'warning'>('error')
const versionConflict = ref(false)
const fieldErrors = ref<Record<string, string>>({})
const isEdit = computed(() => typeof route.params.bookId === 'string')
const canManage = computed(() => roles.value.some((role) => role === 'ADMIN' || role === 'LIBRARIAN'))
let latestLoadRequest = 0
let latestSaveRequest = 0

function bookId(): number | null {
  const value = Number(route.params.bookId)
  return Number.isSafeInteger(value) && value > 0 ? value : null
}

async function loadBook(): Promise<void> {
  const requestId = ++latestLoadRequest
  const requestedBookId = bookId()
  if (!isEdit.value) {
    loadingState.value = 'success'
    return
  }
  const token = requireAccessToken()
  if (requestedBookId === null || !token) {
    loadingState.value = 'error'
    loadingError.value = 'Mã đầu sách không hợp lệ.'
    return
  }
  loadingState.value = 'loading'
  loadingError.value = ''
  try {
    const loadedBook = await getBook(requestedBookId, token)
    if (requestId !== latestLoadRequest || bookId() !== requestedBookId) return
    book.value = loadedBook
    loadingState.value = 'success'
    versionConflict.value = false
    saveError.value = ''
  } catch (error) {
    if (requestId !== latestLoadRequest || bookId() !== requestedBookId || isApiError(error, 401)) return
    loadingError.value = extractApiErrorMessage(error, 'Không thể tải thông tin đầu sách.')
    loadingState.value = 'error'
  }
}

function toRequest(values: BookFormValues) {
  return {
    isbn: values.isbn || null,
    title: values.title,
    author: values.author,
    publisher: values.publisher || null,
    publishedYear: values.publishedYear,
    category: values.category || null,
    listPrice: values.listPrice,
    coverUrl: values.coverUrl || null,
  }
}

async function save(values: BookFormValues): Promise<void> {
  if (saving.value) return
  const editing = isEdit.value
  const requestedBookId = editing ? bookId() : null
  const requestedBook = editing ? book.value : null
  if (editing && (!requestedBook || requestedBook.id !== requestedBookId)) return
  const token = requireAccessToken()
  if (!token) return
  const requestId = ++latestSaveRequest
  saving.value = true
  saveError.value = ''
  saveErrorTone.value = 'error'
  versionConflict.value = false
  fieldErrors.value = {}
  try {
    const request = toRequest(values)
    const saved = requestedBook
      ? await updateBook(requestedBook.id, { ...request, expectedVersion: requestedBook.version }, token)
      : await createBook(request, token)
    if (requestId !== latestSaveRequest || isEdit.value !== editing || (editing && bookId() !== requestedBookId)) return
    await router.replace({ name: 'v2-library-book-detail', params: { bookId: saved.id } })
  } catch (error) {
    if (requestId !== latestSaveRequest || isEdit.value !== editing || (editing && bookId() !== requestedBookId)) return
    if (isApiError(error, 401)) return
    if (error instanceof ApiError) {
      if (error.status === 403) saveErrorTone.value = 'warning'
      error.validationErrors.forEach(({ field, messages }) => { fieldErrors.value[field] = messages.join(' ') })
      if (error.code === 'VERSION_CONFLICT') {
        versionConflict.value = true
        saveError.value = 'Thông tin đầu sách đã thay đổi ở phiên khác. Tải dữ liệu mới để xem lại trước khi lưu.'
      } else if (error.code === 'DUPLICATE_ISBN') {
        fieldErrors.value.isbn = 'ISBN này đã được sử dụng cho một đầu sách khác.'
      } else {
        saveError.value = extractApiErrorMessage(error, 'Không thể lưu đầu sách.')
      }
    } else {
      saveError.value = extractApiErrorMessage(error, 'Không thể lưu đầu sách.')
    }
  } finally {
    if (requestId === latestSaveRequest) saving.value = false
  }
}

watch(() => route.params.bookId, () => {
  latestLoadRequest += 1
  latestSaveRequest += 1
  saving.value = false
  book.value = null
  loadingError.value = ''
  saveError.value = ''
  versionConflict.value = false
  fieldErrors.value = {}
  loadingState.value = isEdit.value ? 'loading' : 'success'
  void loadBook()
}, { immediate: true })
onBeforeUnmount(() => {
  latestLoadRequest += 1
  latestSaveRequest += 1
})
</script>

<template>
  <section class="library-page page-content">
    <div class="page-heading">
      <div><p class="eyebrow">Danh mục sách</p><h1>{{ isEdit ? 'Chỉnh sửa đầu sách' : 'Tạo đầu sách' }}</h1></div>
      <Button label="Quay lại danh mục" icon="pi pi-arrow-left" severity="secondary" text @click="router.push({ name: 'v2-library-books' })" />
    </div>
    <PageState v-if="isEdit && loadingState !== 'success'" :state="loadingState" :error-message="loadingError" @retry="loadBook" />
    <div v-else-if="!canManage" class="content-surface"><PageState state="error" forbidden /></div>
    <div v-else class="content-surface">
      <p class="dialog-caption">Thông tin đầu sách hiển thị trong danh mục. Bìa sách dùng URL HTTP hoặc HTTPS.</p>
      <div v-if="versionConflict" class="form-actions conflict-actions"><Button label="Tải dữ liệu mới" icon="pi pi-refresh" severity="secondary" outlined @click="loadBook" /></div>
      <BookForm :initial-value="book" :saving="saving" :error-message="saveError" :error-tone="saveErrorTone" :field-errors="fieldErrors" @save="save" @cancel="router.push({ name: isEdit ? 'v2-library-book-detail' : 'v2-library-books', params: isEdit ? { bookId: route.params.bookId } : {} })" />
    </div>
  </section>
</template>
