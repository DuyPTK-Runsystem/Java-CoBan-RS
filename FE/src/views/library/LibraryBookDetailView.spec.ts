import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { reactive } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import ConfirmationService from 'primevue/confirmationservice'
import PrimeVue from 'primevue/config'
import { clearAuthSession, getAuthSession, saveAuthSession } from '@/services/authSession'
import { ApiError } from '@/types/api'
import LibraryBookDetailView from './LibraryBookDetailView.vue'

const mocks = vi.hoisted(() => ({
  addBookCopies: vi.fn(), archiveBook: vi.fn(), getBook: vi.fn(), listBookCopies: vi.fn(), updateBookCopy: vi.fn(), withdrawBookCopy: vi.fn(),
  getBarcodePng: vi.fn(), getBookCopyByBarcode: vi.fn(), push: vi.fn(), replace: vi.fn(),
  route: { params: { bookId: '1' } },
}))
vi.mock('vue-router', () => ({ useRoute: () => mocks.route, useRouter: () => ({ push: mocks.push, replace: mocks.replace }) }))
vi.mock('@/services/library/libraryCatalogApi', () => ({
  addBookCopies: mocks.addBookCopies, archiveBook: mocks.archiveBook, getBook: mocks.getBook,
  listBookCopies: mocks.listBookCopies, updateBookCopy: mocks.updateBookCopy, withdrawBookCopy: mocks.withdrawBookCopy,
  getBarcodePng: mocks.getBarcodePng, getBookCopyByBarcode: mocks.getBookCopyByBarcode,
}))

enableAutoUnmount(afterEach)

const book = (id = 1) => ({ id, isbn: null, title: `Book ${id}`, author: 'Author', publisher: null, publishedYear: null, category: null, listPrice: null, coverUrl: null, version: 7, totalCopyCount: 1, availableBorrowableCopyCount: 1 })
const copy = { id: 11, bookId: 1, barcode: 'LIB-000000011', status: 'AVAILABLE', shelfLocation: 'A-1', referenceOnly: false, version: 4 }
const button = { props: ['label', 'disabled'], emits: ['click'], template: '<button v-bind="$attrs" :disabled="disabled" @click="$emit(\'click\', $event)">{{ label }}</button>' }
const inputText = { props: ['modelValue'], emits: ['update:modelValue'], template: '<input v-bind="$attrs" :value="modelValue ?? \'\'" @input="$emit(\'update:modelValue\', $event.target.value)" />' }
const select = { props: ['modelValue', 'options'], emits: ['update:modelValue'], template: '<select v-bind="$attrs" :value="modelValue"><option v-for="option in options" :key="String(option.value)" :value="option.value">{{ option.label }}</option></select>' }
const pageState = { props: ['state', 'forbidden', 'errorMessage'], emits: ['retry'], template: '<div data-testid="page-state">{{ state }} {{ forbidden ? \'forbidden\' : \'\' }} {{ errorMessage }}<button @click="$emit(\'retry\')">Thử lại</button></div>' }
const formAlert = { props: ['message'], template: '<div>{{ message }}</div>' }
const copyTable = {
  props: ['copies', 'canManage'], emits: ['edit', 'setStatus', 'withdraw', 'barcode'],
  template: '<div data-testid="copy-table"><span v-for="item in copies" :key="item.id">{{ item.barcode }} {{ item.status }}</span><button v-if="canManage && copies.length && [\'AVAILABLE\', \'DAMAGED\'].includes(copies[0].status)" data-testid="edit-copy" @click="$emit(\'edit\', copies[0])">Edit copy</button><button v-if="canManage && copies.length && copies[0].status === \'AVAILABLE\'" data-testid="damage-copy" @click="$emit(\'setStatus\', copies[0], \'DAMAGED\')">Mark damaged</button><button v-if="canManage && copies.length && [\'AVAILABLE\', \'DAMAGED\'].includes(copies[0].status)" data-testid="withdraw-copy" @click="$emit(\'withdraw\', copies[0])">Withdraw copy</button></div>',
}
const addDialog = {
  props: ['visible', 'saving', 'errorMessage'], emits: ['save', 'cancel', 'update:visible'],
  template: '<div v-if="visible" data-testid="add-dialog"><span>{{ errorMessage }}</span><button data-testid="add-submit" :disabled="saving" @click="$emit(\'save\', { quantity: 2, shelfLocation: \'A-2\', referenceOnly: false })">Submit add</button></div>',
}
const metadataDialog = {
  props: ['visible', 'copy', 'saving', 'reloading', 'canReload', 'errorMessage'], emits: ['save', 'reload', 'update:visible'],
  template: '<div v-if="visible" data-testid="metadata-dialog"><span>{{ errorMessage }}</span><button data-testid="metadata-submit" @click="$emit(\'save\', { shelfLocation: \'\', referenceOnly: false })">Save metadata</button><button v-if="canReload" data-testid="metadata-reload" @click="$emit(\'reload\')">Reload copy</button></div>',
}
const barcodeDialog = { template: '<div />' }

function mountDetail() {
  return mount(LibraryBookDetailView, {
    attachTo: document.createElement('div'),
    global: {
      plugins: [ConfirmationService, PrimeVue],
      stubs: { Button: button, FormAlert: formAlert, InputText: inputText, Select: select, Paginator: true, StatusTag: true, PageState: pageState, BookCopyTable: copyTable, AddBookCopiesDialog: addDialog, BookCopyMetadataDialog: metadataDialog, BookBarcodeDialog: barcodeDialog, BarcodePrint: true },
    },
  })
}

function setup(role: 'ADMIN' | 'LIBRARIAN' | 'STUDENT' = 'LIBRARIAN') {
  clearAuthSession()
  saveAuthSession({ accessToken: 'catalog-token', user: { id: 1, username: 'catalog-user', roles: [role] } })
  mocks.route = reactive({ params: { bookId: '1' } })
  mocks.getBook.mockReset().mockImplementation(async (id: number) => book(id))
  mocks.listBookCopies.mockReset().mockResolvedValue({ meta: { page: 0, pageSize: 20, totalPages: 1, totalItems: 1 }, result: [copy] })
  mocks.archiveBook.mockReset().mockResolvedValue(undefined)
  mocks.withdrawBookCopy.mockReset().mockResolvedValue(undefined)
  mocks.updateBookCopy.mockReset().mockResolvedValue(copy)
  mocks.addBookCopies.mockReset().mockResolvedValue({ createdCount: 2, copies: [] })
  mocks.push.mockReset()
  mocks.replace.mockReset()
}

describe('LibraryBookDetailView', () => {
  beforeEach(() => setup())
  afterEach(() => { clearAuthSession() })

  it('renders a real confirmation before archival and only calls the versioned API after acceptance', async () => {
    const wrapper = mountDetail()
    await flushPromises()
    await wrapper.findAll('button').find((item) => item.text() === 'Lưu trữ')!.trigger('click')
    await flushPromises()
    expect(document.body.textContent).toContain('Xác nhận lưu trữ đầu sách')
    expect(mocks.archiveBook).not.toHaveBeenCalled()
    Array.from(document.body.querySelectorAll('button')).find((candidate) => candidate.textContent?.trim() === 'Hủy')!.click()
    await flushPromises()
    expect(mocks.archiveBook).not.toHaveBeenCalled()

    await wrapper.findAll('button').find((item) => item.text() === 'Lưu trữ')!.trigger('click')
    await flushPromises()
    Array.from(document.body.querySelectorAll('button')).find((candidate) => candidate.textContent?.trim() === 'Lưu trữ')!.click()
    await flushPromises()
    expect(mocks.archiveBook).toHaveBeenCalledOnce()
    expect(mocks.archiveBook).toHaveBeenCalledWith(1, 7, 'catalog-token')
  })

  it('keeps the same idempotency key and payload when a retry follows an uncertain response', async () => {
    vi.stubGlobal('crypto', { randomUUID: vi.fn(() => 'catalog-intent-1') })
    mocks.addBookCopies.mockRejectedValueOnce(new Error('connection lost')).mockResolvedValueOnce({ createdCount: 2, copies: [] })
    const wrapper = mountDetail()
    await flushPromises()
    await wrapper.findAll('button').find((item) => item.text() === 'Thêm bản sao')!.trigger('click')
    await wrapper.get('[data-testid="add-submit"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="add-submit"]').trigger('click')
    await flushPromises()

    expect(mocks.addBookCopies).toHaveBeenCalledTimes(2)
    expect(mocks.addBookCopies.mock.calls[0]).toEqual([1, { quantity: 2, shelfLocation: 'A-2', referenceOnly: false }, 'catalog-intent-1', 'catalog-token'])
    expect(mocks.addBookCopies.mock.calls[1]).toEqual(mocks.addBookCopies.mock.calls[0])
  })

  it('submits a batch only once while the first add request is still pending', async () => {
    let resolveAdd!: (value: { createdCount: number; copies: [] }) => void
    mocks.addBookCopies.mockReturnValue(new Promise((resolve) => { resolveAdd = resolve }))
    const wrapper = mountDetail()
    await flushPromises()
    await wrapper.findAll('button').find((item) => item.text() === 'Thêm bản sao')!.trigger('click')
    const submit = wrapper.get('[data-testid="add-submit"]')
    await submit.trigger('click')
    await submit.trigger('click')
    expect(mocks.addBookCopies).toHaveBeenCalledOnce()
    resolveAdd({ createdCount: 2, copies: [] })
    await flushPromises()
  })

  it('clears an uncertain add intent when navigation changes to a different book', async () => {
    vi.stubGlobal('crypto', { randomUUID: vi.fn().mockReturnValueOnce('catalog-intent-1').mockReturnValueOnce('catalog-intent-2') })
    mocks.addBookCopies.mockRejectedValueOnce(new Error('connection lost')).mockResolvedValueOnce({ createdCount: 2, copies: [] })
    const wrapper = mountDetail()
    await flushPromises()
    await wrapper.findAll('button').find((item) => item.text() === 'Thêm bản sao')!.trigger('click')
    await wrapper.get('[data-testid="add-submit"]').trigger('click')
    await flushPromises()
    expect(mocks.addBookCopies.mock.calls[0]).toEqual([1, { quantity: 2, shelfLocation: 'A-2', referenceOnly: false }, 'catalog-intent-1', 'catalog-token'])

    mocks.route.params.bookId = '2'
    await flushPromises()
    expect(wrapper.find('[data-testid="add-dialog"]').exists()).toBe(false)
    await wrapper.findAll('button').find((item) => item.text() === 'Thêm bản sao')!.trigger('click')
    await wrapper.get('[data-testid="add-submit"]').trigger('click')
    await flushPromises()
    expect(mocks.addBookCopies.mock.calls[1]).toEqual([2, { quantity: 2, shelfLocation: 'A-2', referenceOnly: false }, 'catalog-intent-2', 'catalog-token'])
  })

  it('submits an allowed status change once even if a confirmation button is clicked twice rapidly', async () => {
    let resolveChange!: (value: typeof copy) => void
    mocks.updateBookCopy.mockReturnValue(new Promise((resolve) => { resolveChange = resolve }))
    const wrapper = mountDetail()
    await flushPromises()
    await wrapper.get('[data-testid="damage-copy"]').trigger('click')
    await flushPromises()
    const accept = Array.from(document.body.querySelectorAll('button')).find((candidate) => candidate.textContent?.trim() === 'Xác nhận')!
    accept.click()
    accept.click()
    await flushPromises()
    expect(mocks.updateBookCopy).toHaveBeenCalledOnce()
    expect(mocks.updateBookCopy).toHaveBeenCalledWith('LIB-000000011', { status: 'DAMAGED', expectedVersion: 4 }, 'catalog-token')
    resolveChange(copy)
    await flushPromises()
  })

  it('sends null when clearing a shelf location and includes the copy version', async () => {
    const wrapper = mountDetail()
    await flushPromises()
    await wrapper.get('[data-testid="edit-copy"]').trigger('click')
    await wrapper.get('[data-testid="metadata-submit"]').trigger('click')
    await flushPromises()
    expect(mocks.updateBookCopy).toHaveBeenCalledWith('LIB-000000011', { shelfLocation: null, referenceOnly: false, expectedVersion: 4 }, 'catalog-token')
  })

  it('keeps the current book and copy list when an older route response arrives last', async () => {
    let resolveOldBook!: (value: ReturnType<typeof book>) => void
    let resolveOldCopies!: (value: { meta: { page: number; pageSize: number; totalPages: number; totalItems: number }; result: Array<typeof copy> }) => void
    const oldCopy = { ...copy, id: 10, barcode: 'LIB-000000010' }
    const newCopy = { ...copy, id: 20, barcode: 'LIB-000000020' }
    mocks.getBook.mockReturnValueOnce(new Promise((resolve) => { resolveOldBook = resolve })).mockResolvedValueOnce(book(2))
    mocks.listBookCopies.mockReturnValueOnce(new Promise((resolve) => { resolveOldCopies = resolve }))
      .mockResolvedValueOnce({ meta: { page: 0, pageSize: 20, totalPages: 1, totalItems: 1 }, result: [newCopy] })
    const wrapper = mountDetail()
    mocks.route.params.bookId = '2'
    await flushPromises()
    resolveOldBook(book(1))
    resolveOldCopies({ meta: { page: 0, pageSize: 20, totalPages: 1, totalItems: 1 }, result: [oldCopy] })
    await flushPromises()

    expect(wrapper.text()).toContain('Book 2')
    expect(wrapper.text()).toContain('LIB-000000020')
    expect(wrapper.text()).not.toContain('LIB-000000010')
  })

  it('shows copy version conflict and a forbidden state while retaining the authenticated session', async () => {
    const wrapper = mountDetail()
    await flushPromises()
    mocks.updateBookCopy.mockRejectedValueOnce(new ApiError(409, 'Version conflict', { code: 'VERSION_CONFLICT', kind: 'conflict' }))
    await wrapper.get('[data-testid="edit-copy"]').trigger('click')
    await wrapper.get('[data-testid="metadata-submit"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="metadata-dialog"]').text()).toContain('Thông tin bản sao đã thay đổi')
    mocks.getBookCopyByBarcode.mockResolvedValue({ ...copy, version: 5, book: book(1) })
    await wrapper.get('[data-testid="metadata-reload"]').trigger('click')
    await flushPromises()
    expect(mocks.getBookCopyByBarcode).toHaveBeenCalledWith('LIB-000000011', 'catalog-token')

    mocks.getBook.mockRejectedValueOnce(new ApiError(403, 'Not allowed', { code: 'LIBRARY_RESOURCE_FORBIDDEN', kind: 'forbidden' }))
    mocks.route.params.bookId = '2'
    await flushPromises()
    expect(wrapper.get('[data-testid="page-state"]').text()).toContain('forbidden')
    expect(getAuthSession()?.accessToken).toBe('catalog-token')
  })

  it.each(['STUDENT', 'TEACHER'] as const)('hides copy management actions from a %s reader', async (role) => {
    setup(role)
    const wrapper = mountDetail()
    await flushPromises()
    expect(wrapper.find('[data-testid="add-submit"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="edit-copy"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="withdraw-copy"]').exists()).toBe(false)
    expect(wrapper.findAll('button').some((item) => item.text().startsWith('In mã vạch'))).toBe(false)
    expect(wrapper.text()).toContain('LIB-000000011')
  })

  it('renders the In mã vạch button alongside copy actions for managers', async () => {
    const wrapper = mountDetail()
    await flushPromises()
    expect(wrapper.findAll('button').some((item) => item.text() === 'In mã vạch (0)')).toBe(true)
    expect(wrapper.findAll('button').some((item) => item.text() === 'Thêm bản sao')).toBe(true)
  })
})
