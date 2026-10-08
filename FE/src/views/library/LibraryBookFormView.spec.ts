import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h, reactive, type PropType } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import type { BookDetail } from '@/types/library/catalog'
import { ApiError } from '@/types/api'
import LibraryBookFormView from './LibraryBookFormView.vue'

const mocks = vi.hoisted(() => ({ getBook: vi.fn(), createBook: vi.fn(), updateBook: vi.fn(), push: vi.fn(), replace: vi.fn(), route: { params: {} as Record<string, string> } }))
vi.mock('vue-router', () => ({ useRoute: () => mocks.route, useRouter: () => ({ push: mocks.push, replace: mocks.replace }) }))
vi.mock('@/services/library/libraryCatalogApi', () => ({ getBook: mocks.getBook, createBook: mocks.createBook, updateBook: mocks.updateBook }))

enableAutoUnmount(afterEach)

const formValues = { isbn: '', title: 'Java nhập môn', author: 'Nguyễn An', publisher: '', publishedYear: 2024, category: '', listPrice: null, coverUrl: '' }
const book = (id: number, version: number) => ({ id, isbn: null, title: `Book ${id}`, author: 'Author', publisher: null, publishedYear: null, category: null, listPrice: null, coverUrl: null, version, totalCopyCount: 0, availableBorrowableCopyCount: 0 })
const button = { props: ['label', 'disabled'], emits: ['click'], template: '<button v-bind="$attrs" :disabled="disabled" @click="$emit(\'click\', $event)">{{ label }}</button>' }
const bookForm = defineComponent({
  props: {
    initialValue: { type: Object as PropType<BookDetail | null>, default: null },
    saving: { type: Boolean, required: true },
    errorMessage: { type: String, required: true },
    errorTone: { type: String, required: true },
    fieldErrors: { type: Object as PropType<Record<string, string>>, required: true },
  },
  emits: ['save', 'cancel'],
  setup(props, { emit }) {
    return () => h('div', { 'data-testid': 'book-form', 'data-book-id': props.initialValue?.id }, [
      h('span', String(props.errorMessage ?? '')),
      h('button', { 'data-testid': 'submit', disabled: props.saving, onClick: () => emit('save', formValues) }, 'Save'),
    ])
  },
})
const pageState = { props: { state: String, forbidden: Boolean, errorMessage: String }, emits: ['retry'], template: '<div data-testid="form-state">{{ state }} {{ forbidden ? \'forbidden\' : \'\' }} {{ errorMessage }}<button @click="$emit(\'retry\')">Retry</button></div>' }

function setup(bookId?: string, role: 'LIBRARIAN' | 'STUDENT' | 'TEACHER' = 'LIBRARIAN') {
  clearAuthSession()
  saveAuthSession({ accessToken: 'form-token', user: { id: 1, username: role.toLowerCase(), roles: [role] } })
  mocks.route = reactive({ params: bookId ? { bookId } : {} })
  mocks.getBook.mockReset()
  mocks.createBook.mockReset()
  mocks.updateBook.mockReset()
  mocks.push.mockReset()
  mocks.replace.mockReset()
}

function mountFormView() {
  return mount(LibraryBookFormView, { global: { stubs: { Button: button, BookForm: bookForm, PageState: pageState } } })
}

describe('LibraryBookFormView', () => {
  beforeEach(() => setup())
  afterEach(() => clearAuthSession())

  it('ignores a late response for a previous edit route before saving the currently selected book', async () => {
    let resolveFirst!: (value: ReturnType<typeof book>) => void
    let resolveSecond!: (value: ReturnType<typeof book>) => void
    mocks.route.params.bookId = '1'
    mocks.getBook.mockReturnValueOnce(new Promise((resolve) => { resolveFirst = resolve }))
      .mockReturnValueOnce(new Promise((resolve) => { resolveSecond = resolve }))
    const wrapper = mountFormView()
    mocks.route.params.bookId = '2'
    await flushPromises()
    resolveSecond(book(2, 9))
    await flushPromises()
    resolveFirst(book(1, 4))
    await flushPromises()

    expect(wrapper.get('[data-testid="book-form"]').attributes('data-book-id')).toBe('2')
    await wrapper.get('[data-testid="submit"]').trigger('click')
    await flushPromises()
    expect(mocks.updateBook).toHaveBeenCalledWith(2, expect.objectContaining({ expectedVersion: 9 }), 'form-token')
  })

  it('does not submit twice while the first create request is pending', async () => {
    setup()
    let resolveCreate!: (value: ReturnType<typeof book>) => void
    mocks.createBook.mockReturnValue(new Promise((resolve) => { resolveCreate = resolve }))
    const wrapper = mountFormView()
    await flushPromises()
    await wrapper.get('[data-testid="submit"]').trigger('click')
    await wrapper.get('[data-testid="submit"]').trigger('click')
    expect(mocks.createBook).toHaveBeenCalledOnce()
    resolveCreate(book(3, 0))
    await flushPromises()
    expect(mocks.replace).toHaveBeenCalledWith({ name: 'v2-library-book-detail', params: { bookId: 3 } })
  })

  it.each(['STUDENT', 'TEACHER'] as const)('keeps %s out of the catalog management form', async (role) => {
    setup(undefined, role)
    const wrapper = mountFormView()
    await flushPromises()
    expect(wrapper.get('[data-testid="form-state"]').text()).toContain('forbidden')
    expect(wrapper.find('[data-testid="book-form"]').exists()).toBe(false)
  })

  it('offers an explicit reload after a version conflict and reloads the latest version', async () => {
    setup('5')
    mocks.getBook.mockResolvedValue(book(5, 4))
    mocks.updateBook.mockRejectedValue(new ApiError(409, 'Version conflict', { code: 'VERSION_CONFLICT', kind: 'conflict' }))
    const wrapper = mountFormView()
    await flushPromises()
    await wrapper.get('[data-testid="submit"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('đã thay đổi ở phiên khác')
    expect(wrapper.findAll('button').some((item) => item.text() === 'Tải dữ liệu mới')).toBe(true)
    await wrapper.findAll('button').find((item) => item.text() === 'Tải dữ liệu mới')!.trigger('click')
    await flushPromises()
    expect(mocks.getBook).toHaveBeenCalledTimes(2)
  })
})
