<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import Button from 'primevue/button'
import ConfirmDialog from 'primevue/confirmdialog'
import { type PageState as PrimePageState } from 'primevue/datatable'
import InputText from 'primevue/inputtext'
import Paginator from 'primevue/paginator'
import Select from 'primevue/select'
import { useConfirm } from 'primevue/useconfirm'
import { useRoute, useRouter } from 'vue-router'

import AddBookCopiesDialog from '@/components/library/AddBookCopiesDialog.vue'
import BarcodePrint from '@/components/library/BarcodePrint.vue'
import BookBarcodeDialog from '@/components/library/BookBarcodeDialog.vue'
import BookCopyMetadataDialog from '@/components/library/BookCopyMetadataDialog.vue'
import BookCopyTable from '@/components/library/BookCopyTable.vue'
import FormAlert from '@/components/common/FormAlert.vue'
import PageState from '@/components/common/PageState.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { useAuthSession } from '@/composables/useAuthSession'
import {
  addBookCopies,
  archiveBook,
  getBarcodePng,
  getBook,
  getBookCopyByBarcode,
  listBookCopies,
  updateBookCopy,
  withdrawBookCopy,
} from '@/services/library/libraryCatalogApi'
import { ApiError, extractApiErrorMessage, isApiError } from '@/types/api'
import { BOOK_COPY_STATUS_LABELS } from '@/types/library/catalog'
import type { BookCopy, BookCopyDetail, BookCopyStatus, BookDetail } from '@/types/library/catalog'
import type { LoadingState } from '@/types/ui'

const route = useRoute()
const router = useRouter()
const confirm = useConfirm()
const { roles, requireAccessToken } = useAuthSession()
const book = ref<BookDetail | null>(null)
const copies = ref<BookCopy[]>([])
const copyPage = ref(0)
const copyPageSize = ref(20)
const copyTotal = ref(0)
const copyStatusFilter = ref<BookCopyStatus | ''>('')
const referenceFilter = ref<boolean | ''>('')
const bookState = ref<LoadingState>('loading')
const copyState = ref<LoadingState>('loading')
const loadError = ref('')
const copyError = ref('')
const copyForbidden = ref(false)
const operationError = ref('')
const operationForbidden = ref(false)
const statusMessage = ref('')
const forbidden = ref(false)
const lookupText = ref('')
const lookupResult = ref<BookCopyDetail | null>(null)
const lookupError = ref('')
const lookupForbidden = ref(false)
const lookingUp = ref(false)
const addDialogVisible = ref(false)
const addSaving = ref(false)
const addError = ref('')
const addErrorTone = ref<'error' | 'warning'>('error')
const metadataDialogVisible = ref(false)
const metadataSaving = ref(false)
const metadataError = ref('')
const metadataErrorTone = ref<'error' | 'warning'>('error')
const metadataVersionConflict = ref(false)
const metadataReloading = ref(false)
const selectedCopy = ref<BookCopy | null>(null)
const selectedCopies = ref<BookCopy[]>([])
const printModalVisible = ref(false)
const barcodeDialogVisible = ref(false)
const barcodeCopy = ref<BookCopy | null>(null)
const barcodeLoading = ref(false)
const barcodeError = ref('')
const barcodeErrorTone = ref<'error' | 'warning'>('error')
const barcodeImageUrl = ref('')
const copyMutationPending = ref(false)
const copyConfirmationOpen = ref(false)
const archivePending = ref(false)
const archiveConfirmationOpen = ref(false)
const operationVersionConflict = ref(false)
let latestBookRequest = 0
let latestCopyRequest = 0
let latestLookupRequest = 0
let latestAddCopiesRequest = 0
let latestMetadataRequest = 0
let latestCopyMutationRequest = 0
let latestArchiveRequest = 0
let latestBarcodeRequest = 0
let pendingCopiesKey = ''
let pendingCopiesFingerprint = ''

const canManage = computed(() => roles.value.some((role) => role === 'ADMIN' || role === 'LIBRARIAN'))
const bookId = computed(() => {
  const value = Number(route.params.bookId)
  return Number.isSafeInteger(value) && value > 0 ? value : null
})
const statusOptions = [
  { label: 'Mọi trạng thái', value: '' },
  { label: 'Có sẵn', value: 'AVAILABLE' },
  { label: 'Hư hỏng', value: 'DAMAGED' },
  { label: 'Đang được mượn', value: 'ON_LOAN' },
  { label: 'Đã giữ chỗ', value: 'RESERVED' },
  { label: 'Bị mất', value: 'LOST' },
  { label: 'Đã rút khỏi kho', value: 'WITHDRAWN' },
]
const referenceOptions = [
  { label: 'Mọi hình thức', value: '' },
  { label: 'Có thể cho mượn', value: false },
  { label: 'Chỉ đọc tại chỗ', value: true },
]

function clearBarcodeUrl(): void {
  if (!barcodeImageUrl.value) return
  URL.revokeObjectURL(barcodeImageUrl.value)
  barcodeImageUrl.value = ''
}

function showError(error: unknown, fallback: string): string {
  return extractApiErrorMessage(error, fallback)
}

function setOperationError(error: unknown, fallback: string): void {
  operationForbidden.value = isApiError(error, 403)
  operationVersionConflict.value = error instanceof ApiError && error.code === 'VERSION_CONFLICT'
  operationError.value = showError(error, fallback)
}

async function loadBook(): Promise<void> {
  const id = bookId.value
  const token = requireAccessToken()
  if (id === null || !token) {
    bookState.value = 'error'
    loadError.value = 'Mã đầu sách không hợp lệ.'
    return
  }
  const requestId = ++latestBookRequest
  bookState.value = 'loading'
  loadError.value = ''
  forbidden.value = false
  try {
    const value = await getBook(id, token)
    if (requestId !== latestBookRequest) return
    book.value = value
    bookState.value = 'success'
  } catch (error) {
    if (requestId !== latestBookRequest || isApiError(error, 401)) return
    forbidden.value = isApiError(error, 403)
    loadError.value = showError(error, 'Không thể tải thông tin đầu sách.')
    bookState.value = 'error'
  }
}

async function loadCopies(): Promise<void> {
  const id = bookId.value
  const token = requireAccessToken()
  if (id === null || !token) return
  const requestId = ++latestCopyRequest
  copyState.value = 'loading'
  copyForbidden.value = false
  copyError.value = ''
  try {
    const response = await listBookCopies(id, {
      status: copyStatusFilter.value || undefined,
      referenceOnly: referenceFilter.value === '' ? undefined : referenceFilter.value,
      page: copyPage.value,
      size: copyPageSize.value,
      sort: 'barcode,asc',
    }, token)
    if (requestId !== latestCopyRequest) return
    copies.value = response.result
    selectedCopies.value = []
    copyTotal.value = response.meta.totalItems
    copyState.value = 'success'
  } catch (error) {
    if (requestId !== latestCopyRequest || isApiError(error, 401)) return
    copyForbidden.value = isApiError(error, 403)
    copyError.value = showError(error, 'Không thể tải danh sách bản sao.')
    copyState.value = 'error'
  }
}

function changeCopyFilter(): void {
  copyPage.value = 0
  void loadCopies()
}

function handleCopyPage(event: PrimePageState): void {
  copyPage.value = Math.floor(event.first / event.rows)
  copyPageSize.value = event.rows
  void loadCopies()
}

async function lookupBarcode(): Promise<void> {
  const barcode = lookupText.value.trim()
  const token = requireAccessToken()
  if (!barcode || !token) return
  const contextBookId = bookId.value
  const requestId = ++latestLookupRequest
  lookingUp.value = true
  lookupError.value = ''
  lookupForbidden.value = false
  lookupResult.value = null
  try {
    const result = await getBookCopyByBarcode(barcode, token)
    if (requestId !== latestLookupRequest || contextBookId !== bookId.value) return
    lookupResult.value = result
  } catch (error) {
    if (requestId !== latestLookupRequest || contextBookId !== bookId.value) return
    if (!isApiError(error, 401)) {
      lookupForbidden.value = isApiError(error, 403)
      lookupError.value = showError(error, 'Không tìm thấy bản sao theo mã vạch.')
    }
  } finally {
    if (requestId === latestLookupRequest) lookingUp.value = false
  }
}

function openAddCopies(): void {
  if (addSaving.value || copyMutationPending.value || metadataSaving.value || archivePending.value) return
  addError.value = ''
  addErrorTone.value = 'error'
  addDialogVisible.value = true
}

async function saveCopies(values: { quantity: number; shelfLocation: string; referenceOnly: boolean }): Promise<void> {
  if (addSaving.value || copyMutationPending.value || metadataSaving.value || archivePending.value) return
  const token = requireAccessToken()
  if (!token || bookId.value === null) return
  const contextBookId = bookId.value
  const request: AddBookCopiesPayload = {
    quantity: values.quantity,
    shelfLocation: values.shelfLocation || null,
    referenceOnly: values.referenceOnly,
  }
  const fingerprint = JSON.stringify(request)
  if (pendingCopiesKey && pendingCopiesFingerprint !== fingerprint) {
    addError.value = 'Yêu cầu trước có thể đã được máy chủ tiếp nhận. Hãy gửi lại đúng thông tin cũ hoặc hủy thao tác rồi tạo yêu cầu mới.'
    return
  }
  if (!pendingCopiesKey) {
    pendingCopiesKey = crypto.randomUUID()
    pendingCopiesFingerprint = fingerprint
  }
  const idempotencyKey = pendingCopiesKey
  const requestId = ++latestAddCopiesRequest
  addSaving.value = true
  addError.value = ''
  try {
    const result = await addBookCopies(contextBookId, request, idempotencyKey, token)
    if (requestId !== latestAddCopiesRequest || contextBookId !== bookId.value) return
    pendingCopiesKey = ''
    pendingCopiesFingerprint = ''
    addDialogVisible.value = false
    statusMessage.value = `Đã thêm ${result.createdCount} bản sao.`
    copyPage.value = 0
    await Promise.all([loadBook(), loadCopies()])
  } catch (error) {
    if (requestId !== latestAddCopiesRequest || contextBookId !== bookId.value) return
    if (isApiError(error, 401)) return
    addErrorTone.value = isApiError(error, 403) ? 'warning' : 'error'
    addError.value = error instanceof ApiError && error.code === 'IDEMPOTENCY_CONFLICT'
      ? 'Yêu cầu thêm bản sao bị trùng khóa idempotency với nội dung khác. Hãy gửi lại thao tác.'
      : showError(error, 'Không thể thêm bản sao.')
  } finally {
    if (requestId === latestAddCopiesRequest) addSaving.value = false
  }
}

type AddBookCopiesPayload = { quantity: number; shelfLocation: string | null; referenceOnly: boolean }

function cancelAddCopies(): void {
  if (addSaving.value) return
  pendingCopiesKey = ''
  pendingCopiesFingerprint = ''
  addError.value = ''
}

function editCopy(copy: BookCopy): void {
  if (metadataSaving.value || metadataReloading.value || copyMutationPending.value || archivePending.value || addSaving.value) return
  selectedCopy.value = copy
  metadataError.value = ''
  metadataErrorTone.value = 'error'
  metadataVersionConflict.value = false
  metadataDialogVisible.value = true
}

async function saveCopyMetadata(values: { shelfLocation: string; referenceOnly: boolean }): Promise<void> {
  if (metadataSaving.value || metadataReloading.value || copyMutationPending.value || archivePending.value || addSaving.value) return
  const copy = selectedCopy.value
  const token = requireAccessToken()
  const contextBookId = bookId.value
  if (!copy || !token || contextBookId === null || copy.bookId !== contextBookId) return
  const requestId = ++latestMetadataRequest
  metadataSaving.value = true
  metadataError.value = ''
  try {
    const request = {
      ...(values.shelfLocation ? { shelfLocation: values.shelfLocation } : copy.shelfLocation ? { shelfLocation: null } : {}),
      referenceOnly: values.referenceOnly,
      expectedVersion: copy.version,
    }
    await updateBookCopy(copy.barcode, request, token)
    if (requestId !== latestMetadataRequest || contextBookId !== bookId.value) return
    metadataDialogVisible.value = false
    metadataVersionConflict.value = false
    statusMessage.value = 'Đã cập nhật thông tin bản sao.'
    await loadCopies()
  } catch (error) {
    if (requestId !== latestMetadataRequest || contextBookId !== bookId.value) return
    if (isApiError(error, 401)) return
    metadataErrorTone.value = isApiError(error, 403) ? 'warning' : 'error'
    metadataVersionConflict.value = error instanceof ApiError && error.code === 'VERSION_CONFLICT'
    metadataError.value = error instanceof ApiError && error.code === 'VERSION_CONFLICT'
      ? 'Thông tin bản sao đã thay đổi. Tải lại danh sách để xem dữ liệu mới.'
      : showError(error, 'Không thể cập nhật bản sao.')
  } finally {
    if (requestId === latestMetadataRequest) metadataSaving.value = false
  }
}

async function reloadSelectedCopy(): Promise<void> {
  const selected = selectedCopy.value
  const contextBookId = bookId.value
  const token = requireAccessToken()
  if (!selected || contextBookId === null || !token || metadataReloading.value || metadataSaving.value) return
  const requestId = ++latestMetadataRequest
  metadataReloading.value = true
  try {
    const refreshed = await getBookCopyByBarcode(selected.barcode, token)
    if (requestId !== latestMetadataRequest || contextBookId !== bookId.value || selectedCopy.value?.barcode !== selected.barcode) return
    selectedCopy.value = {
      id: refreshed.id,
      bookId: refreshed.bookId,
      barcode: refreshed.barcode,
      shelfLocation: refreshed.shelfLocation,
      status: refreshed.status,
      referenceOnly: refreshed.referenceOnly,
      version: refreshed.version,
      createdAt: refreshed.createdAt,
    }
    metadataError.value = ''
    metadataVersionConflict.value = false
  } catch (error) {
    if (requestId !== latestMetadataRequest || contextBookId !== bookId.value || isApiError(error, 401)) return
    metadataErrorTone.value = isApiError(error, 403) ? 'warning' : 'error'
    metadataError.value = showError(error, 'Không thể tải thông tin mới nhất của bản sao.')
  } finally {
    if (requestId === latestMetadataRequest) metadataReloading.value = false
  }
}

function setCopyStatus(copy: BookCopy, status: 'AVAILABLE' | 'DAMAGED'): void {
  if (copyMutationPending.value || metadataSaving.value || addSaving.value || archivePending.value || copyConfirmationOpen.value || copy.bookId !== bookId.value) return
  const contextBookId = bookId.value
  if (contextBookId === null) return
  copyConfirmationOpen.value = true
  confirm.require({
    message: status === 'DAMAGED'
      ? `Đánh dấu bản sao ${copy.barcode} là hư hỏng?`
      : `Đánh dấu bản sao ${copy.barcode} có sẵn?`,
    header: 'Xác nhận cập nhật trạng thái',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Xác nhận',
    rejectLabel: 'Hủy',
    accept: async () => {
      copyConfirmationOpen.value = false
      if (copyMutationPending.value || metadataSaving.value || addSaving.value || archivePending.value || contextBookId !== bookId.value) return
      const token = requireAccessToken()
      if (!token) return
      const requestId = ++latestCopyMutationRequest
      copyMutationPending.value = true
      operationError.value = ''
      operationForbidden.value = false
      operationVersionConflict.value = false
      try {
        await updateBookCopy(copy.barcode, { status, expectedVersion: copy.version }, token)
        if (requestId !== latestCopyMutationRequest || contextBookId !== bookId.value) return
        statusMessage.value = 'Đã cập nhật trạng thái bản sao.'
        await Promise.all([loadBook(), loadCopies()])
      } catch (error) {
        if (requestId === latestCopyMutationRequest && contextBookId === bookId.value && !isApiError(error, 401)) {
          setOperationError(error, 'Không thể cập nhật trạng thái bản sao.')
        }
      } finally {
        if (requestId === latestCopyMutationRequest) copyMutationPending.value = false
      }
    },
    reject: () => { copyConfirmationOpen.value = false },
  })
}

function withdrawCopy(copy: BookCopy): void {
  if (copyMutationPending.value || metadataSaving.value || addSaving.value || archivePending.value || copyConfirmationOpen.value || copy.bookId !== bookId.value) return
  const contextBookId = bookId.value
  if (contextBookId === null) return
  copyConfirmationOpen.value = true
  confirm.require({
    message: `Rút bản sao ${copy.barcode} khỏi kho? Thao tác này không thể hoàn tác.`,
    header: 'Xác nhận rút bản sao',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Rút khỏi kho',
    rejectLabel: 'Hủy',
    acceptClass: 'p-button-danger',
    accept: async () => {
      copyConfirmationOpen.value = false
      if (copyMutationPending.value || metadataSaving.value || addSaving.value || archivePending.value || contextBookId !== bookId.value) return
      const token = requireAccessToken()
      if (!token) return
      const requestId = ++latestCopyMutationRequest
      copyMutationPending.value = true
      operationError.value = ''
      operationForbidden.value = false
      operationVersionConflict.value = false
      try {
        await withdrawBookCopy(copy.barcode, copy.version, token)
        if (requestId !== latestCopyMutationRequest || contextBookId !== bookId.value) return
        statusMessage.value = 'Đã rút bản sao khỏi kho.'
        await Promise.all([loadBook(), loadCopies()])
      } catch (error) {
        if (requestId === latestCopyMutationRequest && contextBookId === bookId.value && !isApiError(error, 401)) {
          setOperationError(error, 'Không thể rút bản sao khỏi kho.')
        }
      } finally {
        if (requestId === latestCopyMutationRequest) copyMutationPending.value = false
      }
    },
    reject: () => { copyConfirmationOpen.value = false },
  })
}

function archiveCurrentBook(): void {
  if (archivePending.value || archiveConfirmationOpen.value || copyMutationPending.value || metadataSaving.value || addSaving.value) return
  const currentBook = book.value
  if (!currentBook) return
  const contextBookId = bookId.value
  if (contextBookId === null || currentBook.id !== contextBookId) return
  archiveConfirmationOpen.value = true
  confirm.require({
    message: `Lưu trữ đầu sách “${currentBook.title}”? Sách sẽ ẩn khỏi danh mục nhưng lịch sử và các bản sao được giữ lại.`,
    header: 'Xác nhận lưu trữ đầu sách',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Lưu trữ',
    rejectLabel: 'Hủy',
    acceptClass: 'p-button-danger',
    accept: async () => {
      archiveConfirmationOpen.value = false
      if (archivePending.value || copyMutationPending.value || metadataSaving.value || addSaving.value || contextBookId !== bookId.value) return
      const token = requireAccessToken()
      if (!token) return
      const requestId = ++latestArchiveRequest
      archivePending.value = true
      operationError.value = ''
      operationForbidden.value = false
      operationVersionConflict.value = false
      try {
        await archiveBook(currentBook.id, currentBook.version, token)
        if (requestId !== latestArchiveRequest || contextBookId !== bookId.value) return
        await router.replace({ name: 'v2-library-books' })
      } catch (error) {
        if (requestId !== latestArchiveRequest || contextBookId !== bookId.value) return
        if (isApiError(error, 401)) return
        operationForbidden.value = isApiError(error, 403)
        operationVersionConflict.value = error instanceof ApiError && error.code === 'VERSION_CONFLICT'
        operationError.value = error instanceof ApiError && error.code === 'VERSION_CONFLICT'
          ? 'Đầu sách đã được thay đổi. Tải lại thông tin trước khi lưu trữ.'
          : showError(error, 'Không thể lưu trữ đầu sách.')
      } finally {
        if (requestId === latestArchiveRequest) archivePending.value = false
      }
    },
    reject: () => { archiveConfirmationOpen.value = false },
  })
}

async function reloadAfterConflict(): Promise<void> {
  operationVersionConflict.value = false
  operationError.value = ''
  operationForbidden.value = false
  await Promise.all([loadBook(), loadCopies()])
}

async function loadBarcode(): Promise<void> {
  const copy = barcodeCopy.value
  const contextBookId = bookId.value
  const token = requireAccessToken()
  if (!copy || !token || contextBookId === null) return
  const requestId = ++latestBarcodeRequest
  clearBarcodeUrl()
  barcodeLoading.value = true
  barcodeError.value = ''
  barcodeErrorTone.value = 'error'
  try {
    const image = await getBarcodePng(copy.barcode, token)
    if (requestId !== latestBarcodeRequest || !barcodeDialogVisible.value || barcodeCopy.value?.barcode !== copy.barcode || contextBookId !== bookId.value) return
    barcodeImageUrl.value = URL.createObjectURL(image)
  } catch (error) {
    if (requestId !== latestBarcodeRequest || !barcodeDialogVisible.value || barcodeCopy.value?.barcode !== copy.barcode || contextBookId !== bookId.value) return
    if (!isApiError(error, 401)) {
      barcodeErrorTone.value = isApiError(error, 403) ? 'warning' : 'error'
      barcodeError.value = showError(error, 'Không thể tạo ảnh mã vạch.')
    }
  } finally {
    if (requestId === latestBarcodeRequest) barcodeLoading.value = false
  }
}

function openBarcode(copy: BookCopy): void {
  barcodeCopy.value = copy
  barcodeDialogVisible.value = true
  void loadBarcode()
}

function downloadBarcode(): void {
  if (!barcodeImageUrl.value || !barcodeCopy.value) return
  const link = document.createElement('a')
  link.href = barcodeImageUrl.value
  link.download = `${barcodeCopy.value.barcode}.png`
  link.click()
}

function openPrintBarcodes(): void {
  if (selectedCopies.value.length === 0) return
  printModalVisible.value = true
}

watch(bookId, () => {
  confirm.close()
  latestBookRequest += 1
  latestCopyRequest += 1
  latestLookupRequest += 1
  latestAddCopiesRequest += 1
  latestMetadataRequest += 1
  latestCopyMutationRequest += 1
  latestArchiveRequest += 1
  latestBarcodeRequest += 1
  pendingCopiesKey = ''
  pendingCopiesFingerprint = ''
  addDialogVisible.value = false
  addSaving.value = false
  addError.value = ''
  addErrorTone.value = 'error'
  metadataDialogVisible.value = false
  metadataSaving.value = false
  metadataError.value = ''
  metadataErrorTone.value = 'error'
  metadataVersionConflict.value = false
  metadataReloading.value = false
  selectedCopy.value = null
  selectedCopies.value = []
  printModalVisible.value = false
  barcodeDialogVisible.value = false
  barcodeCopy.value = null
  barcodeLoading.value = false
  barcodeError.value = ''
  barcodeErrorTone.value = 'error'
  clearBarcodeUrl()
  copyMutationPending.value = false
  copyConfirmationOpen.value = false
  archivePending.value = false
  archiveConfirmationOpen.value = false
  operationVersionConflict.value = false
  operationError.value = ''
  operationForbidden.value = false
  statusMessage.value = ''
  lookupText.value = ''
  lookupResult.value = null
  lookupError.value = ''
  lookupForbidden.value = false
  lookingUp.value = false
  book.value = null
  copies.value = []
  copyTotal.value = 0
  copyPage.value = 0
  bookState.value = 'loading'
  copyState.value = 'loading'
  loadError.value = ''
  copyError.value = ''
  forbidden.value = false
  copyForbidden.value = false
  void loadBook()
  void loadCopies()
}, { immediate: true })
watch([copyStatusFilter, referenceFilter], changeCopyFilter)
watch(barcodeDialogVisible, (visible) => {
  if (!visible) {
    latestBarcodeRequest += 1
    barcodeLoading.value = false
    clearBarcodeUrl()
    barcodeCopy.value = null
  }
})
onBeforeUnmount(() => {
  confirm.close()
  latestBookRequest += 1
  latestCopyRequest += 1
  latestLookupRequest += 1
  latestAddCopiesRequest += 1
  latestMetadataRequest += 1
  latestCopyMutationRequest += 1
  latestArchiveRequest += 1
  latestBarcodeRequest += 1
  clearBarcodeUrl()
})
</script>

<template>
  <ConfirmDialog />
  <section class="library-page page-content">
    <div class="page-heading">
      <div><p class="eyebrow">Danh mục sách</p><h1>Chi tiết đầu sách</h1></div>
      <div class="page-heading-actions"><Button label="Quay lại danh mục" icon="pi pi-arrow-left" severity="secondary" text @click="router.push({ name: 'v2-library-books' })" /><template v-if="canManage && book"><Button label="Chỉnh sửa" icon="pi pi-pencil" severity="secondary" outlined :disabled="archivePending" @click="router.push({ name: 'v2-library-book-edit', params: { bookId: book.id } })" /><Button label="Lưu trữ" icon="pi pi-archive" severity="danger" outlined :loading="archivePending" :disabled="archiveConfirmationOpen || copyMutationPending || metadataSaving || addSaving" @click="archiveCurrentBook" /></template></div>
    </div>
    <PageState v-if="bookState !== 'success'" :state="bookState" :forbidden="forbidden" :error-message="loadError" @retry="loadBook" />
    <template v-else-if="book">
      <FormAlert v-if="operationError" :tone="operationForbidden ? 'warning' : 'error'" :message="operationError" />
      <FormAlert v-if="statusMessage" tone="success" :message="statusMessage" />
      <div v-if="operationVersionConflict" class="form-actions conflict-actions"><Button label="Tải dữ liệu mới" icon="pi pi-refresh" severity="secondary" outlined @click="reloadAfterConflict" /></div>
      <div class="content-surface book-detail-surface">
        <div :class="['book-detail-header', { 'with-cover': Boolean(book.coverUrl) }]">
          <div v-if="book.coverUrl" class="book-cover-frame"><img :src="book.coverUrl" :alt="`Bìa sách ${book.title}`" @error="($event.target as HTMLImageElement).hidden = true"><span class="cover-fallback">Không tải được ảnh bìa</span></div>
          <div class="book-detail-content">
            <div class="section-heading"><div><span v-if="book.category" class="book-category">{{ book.category }}</span><h2>{{ book.title }}</h2><p>{{ book.author }}</p></div></div>
            <dl class="detail-grid">
              <div class="detail-item"><dt>ISBN</dt><dd>{{ book.isbn || '—' }}</dd></div>
              <div class="detail-item"><dt>Nhà xuất bản</dt><dd>{{ book.publisher || '—' }}</dd></div>
              <div class="detail-item"><dt>Năm xuất bản</dt><dd>{{ book.publishedYear ?? '—' }}</dd></div>
              <div class="detail-item"><dt>Giá bìa</dt><dd>{{ book.listPrice === null ? '—' : `${book.listPrice.toLocaleString('vi-VN')} ₫` }}</dd></div>
              <div class="detail-item"><dt>Tổng số bản sao</dt><dd>{{ book.totalCopyCount }}</dd></div>
              <div class="detail-item"><dt>Có thể mượn</dt><dd><StatusTag :label="String(book.availableBorrowableCopyCount)" :severity="book.availableBorrowableCopyCount ? 'success' : 'secondary'" /></dd></div>
            </dl>
          </div>
        </div>
      </div>

      <div class="content-surface copy-section">
        <div class="section-heading copy-heading">
          <div><h2>Bản sao</h2><p>{{ copyTotal }} bản sao trong danh sách hiện tại</p></div>
          <div v-if="canManage" class="copy-heading-actions">
            <Button :label="`In mã vạch (${selectedCopies.length})`" icon="pi pi-print" severity="success" outlined :disabled="selectedCopies.length === 0" @click="openPrintBarcodes" />
            <Button label="Thêm bản sao" icon="pi pi-plus" :disabled="copyMutationPending || metadataSaving || archivePending" @click="openAddCopies" />
          </div>
        </div>
        <div class="copy-toolbar">
          <div class="field-group"><label for="copy-status-filter">Trạng thái</label><Select id="copy-status-filter" v-model="copyStatusFilter" :options="statusOptions" option-label="label" option-value="value" fluid /></div>
          <div class="field-group"><label for="copy-reference-filter">Hình thức</label><Select id="copy-reference-filter" v-model="referenceFilter" :options="referenceOptions" option-label="label" option-value="value" fluid /></div>
        </div>
        <PageState v-if="copyState === 'loading' || copyState === 'error'" :state="copyState" :forbidden="copyForbidden" :error-message="copyError" @retry="loadCopies" />
        <BookCopyTable v-else v-model:selection="selectedCopies" :copies="copies" :can-manage="canManage" :actions-disabled="copyMutationPending || metadataSaving || metadataReloading || archivePending || addSaving" @edit="editCopy" @set-status="setCopyStatus" @withdraw="withdrawCopy" @barcode="openBarcode" />
        <Paginator v-if="copyState === 'success' && copyTotal > 0" :first="copyPage * copyPageSize" :rows="copyPageSize" :total-records="copyTotal" :rows-per-page-options="[10, 20, 50, 100]" @page="handleCopyPage" />
      </div>

      <div class="content-surface barcode-lookup-surface">
        <div class="section-heading"><div><h2>Tra cứu mã vạch</h2><p>Nhập mã để mở đúng bản sao.</p></div></div>
        <form class="lookup-form" @submit.prevent="lookupBarcode"><InputText v-model="lookupText" placeholder="Ví dụ: LIB-000000001" aria-label="Mã vạch cần tra cứu" /><Button type="submit" label="Tra cứu" icon="pi pi-search" :loading="lookingUp" :disabled="!lookupText.trim()" /></form>
        <FormAlert v-if="lookupError" :tone="lookupForbidden ? 'warning' : 'error'" :message="lookupError" />
        <div v-if="lookupResult" class="lookup-result"><div><strong>{{ lookupResult.barcode }}</strong><span>{{ lookupResult.book?.title ?? book.title }} · {{ lookupResult.shelfLocation || 'Chưa xếp kệ' }}</span></div><StatusTag :label="BOOK_COPY_STATUS_LABELS[lookupResult.status]" :severity="lookupResult.status === 'AVAILABLE' ? 'success' : 'secondary'" /><Button icon="pi pi-barcode" text rounded aria-label="Xem ảnh mã vạch" @click="openBarcode(lookupResult)" /></div>
      </div>
    </template>
    <AddBookCopiesDialog v-model:visible="addDialogVisible" :saving="addSaving" :error-message="addError" :error-tone="addErrorTone" @save="saveCopies" @cancel="cancelAddCopies" />
    <BookCopyMetadataDialog v-model:visible="metadataDialogVisible" :copy="selectedCopy" :saving="metadataSaving" :reloading="metadataReloading" :can-reload="metadataVersionConflict" :error-message="metadataError" :error-tone="metadataErrorTone" @save="saveCopyMetadata" @reload="reloadSelectedCopy" />
    <BookBarcodeDialog v-model:visible="barcodeDialogVisible" :copy="barcodeCopy" :image-url="barcodeImageUrl" :loading="barcodeLoading" :error-message="barcodeError" :error-tone="barcodeErrorTone" @download="downloadBarcode" @retry="loadBarcode" />
    <BarcodePrint v-model:visible="printModalVisible" :copies="selectedCopies" :book-title="book?.title" />
  </section>
</template>

<style scoped>
.page-heading-actions, .copy-heading, .lookup-result, .copy-heading-actions { display: flex; align-items: center; gap: .75rem; }
.page-heading-actions { flex-wrap: wrap; justify-content: flex-end; }
.book-detail-surface, .copy-section, .barcode-lookup-surface { margin-top: 1.25rem; }
.book-detail-header { display: grid; grid-template-columns: minmax(0, 1fr); gap: 1.5rem; }
.book-detail-header.with-cover { grid-template-columns: 180px minmax(0, 1fr); }
.book-detail-content { min-width: 0; }
.book-cover-frame { position: relative; min-height: 220px; display: grid; place-items: center; background: var(--surface-100); border-radius: .75rem; overflow: hidden; }
.book-cover-frame img { position: relative; z-index: 1; width: 100%; height: 100%; min-height: 220px; object-fit: cover; }
.cover-fallback { position: absolute; color: var(--text-color-secondary); font-size: .875rem; }
.book-detail-content .section-heading h2 { margin: .3rem 0; font-size: 1.5rem; }
.book-detail-content .section-heading p { margin: 0; color: var(--text-color-secondary); }
.book-category { color: var(--primary-color); font-size: .875rem; font-weight: 600; }
.copy-section .section-heading { justify-content: space-between; align-items: center; }
.copy-section .section-heading p { margin: .25rem 0 0; color: var(--text-color-secondary); }
.copy-toolbar { display: grid; grid-template-columns: minmax(0, 16rem) minmax(0, 16rem); gap: 1rem; margin: 1rem 0; }
.lookup-form { display: flex; max-width: 42rem; gap: .75rem; }
.lookup-form :deep(.p-inputtext) { flex: 1; }
.lookup-result { margin-top: 1rem; max-width: 42rem; border: 1px solid var(--surface-border); border-radius: .75rem; padding: .75rem 1rem; }
.lookup-result div { display: flex; flex: 1; flex-direction: column; gap: .25rem; }
.lookup-result span { color: var(--text-color-secondary); }
@media (max-width: 700px) {
  .book-detail-header.with-cover { grid-template-columns: 1fr; }
  .book-cover-frame { max-width: 180px; }
  .copy-toolbar { grid-template-columns: 1fr; }
  .page-heading-actions { justify-content: flex-start; }
  .library-page .page-heading-actions :deep(.p-button) { flex: 0 0 auto; width: auto; white-space: nowrap; }
  .copy-section .copy-heading { flex-wrap: wrap; align-items: flex-start; }
  .copy-section .copy-heading > div { min-width: 0; }
  .copy-section .copy-heading-actions { margin-left: auto; flex-wrap: wrap; }
  .lookup-form { flex-direction: column; }
  .lookup-result { flex-wrap: wrap; }
}
</style>
