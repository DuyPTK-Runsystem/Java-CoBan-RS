import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import router from '@/router'
import { clearAuthSession, getAuthSession, saveAuthSession } from '@/services/authSession'
import { ApiError } from '@/types/api'
import LibraryBookListView from './LibraryBookListView.vue'

const mocks = vi.hoisted(() => ({ listBooks: vi.fn() }))
vi.mock('@/services/library/libraryCatalogApi', () => ({ listBooks: mocks.listBooks }))

enableAutoUnmount(afterEach)

const page = (result: unknown[] = [], totalItems = result.length) => ({ meta: { page: 0, pageSize: 20, totalPages: 1, totalItems }, result })
const inputText = { props: ['modelValue'], emits: ['update:modelValue'], template: '<input v-bind="$attrs" :value="modelValue ?? \'\'" @input="$emit(\'update:modelValue\', $event.target.value)" />' }
const inputNumber = { props: ['modelValue'], emits: ['update:modelValue'], template: '<input v-bind="$attrs" :value="modelValue ?? \'\'" @input="$emit(\'update:modelValue\', $event.target.value ? Number($event.target.value) : null)" />' }
const select = { props: ['modelValue', 'options'], emits: ['update:modelValue'], template: '<select v-bind="$attrs" :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="option in options" :key="String(option.value)" :value="option.value">{{ option.label }}</option></select>' }
const button = { props: ['label'], emits: ['click'], template: '<button v-bind="$attrs" @click="$emit(\'click\', $event)">{{ label }}</button>' }
const pageState = { props: ['state', 'forbidden', 'errorMessage'], emits: ['retry'], template: '<div data-testid="page-state"><span>{{ state }} {{ forbidden ? \'403\' : \'\' }} {{ errorMessage }}</span><button @click="$emit(\'retry\')">Thử lại</button></div>' }
const dataTable = { props: ['value'], template: '<div data-testid="results"><span v-for="book in value" :key="book.id">{{ book.title }}</span><slot v-if="!value?.length" name="empty" /></div>' }

function mountList() {
  return mount(LibraryBookListView, { global: { plugins: [router], stubs: { Button: button, Column: true, DataTable: dataTable, InputNumber: inputNumber, InputText: inputText, PageState: pageState, Paginator: true, Select: select, StatusTag: true } } })
}

describe('LibraryBookListView', () => {
  beforeEach(async () => {
    clearAuthSession()
    saveAuthSession({ accessToken: 'library-token', user: { id: 1, username: 'reader', roles: ['STUDENT'] } })
    mocks.listBooks.mockReset().mockResolvedValue(page())
    await router.push('/v2/library/books')
  })
  afterEach(async () => { clearAuthSession(); await router.push('/login') })

  it('applies filters from the URL, resets pagination and clears back to default query', async () => {
    await router.push({ name: 'v2-library-books', query: { page: '2', keyword: 'old', category: 'Novel' } })
    const wrapper = mountList()
    await flushPromises()
    expect(wrapper.findAll('button').some((element) => element.text() === 'Tạo đầu sách')).toBe(false)
    expect(mocks.listBooks).toHaveBeenLastCalledWith(expect.objectContaining({ page: 2, keyword: 'old', category: 'Novel' }), 'library-token')

    await wrapper.get('#book-search').setValue('  Java  ')
    await wrapper.get('#book-category').setValue('Tech')
    await wrapper.get('#book-year-filter').setValue('2024')
    await wrapper.get('#book-availability').setValue('AVAILABLE')
    await wrapper.get('#book-sort').setValue('publishedYear,desc')
    await wrapper.get('button').trigger('click')
    await flushPromises()
    expect(mocks.listBooks).toHaveBeenLastCalledWith({ keyword: 'Java', category: 'Tech', publishedYear: 2024, availability: 'AVAILABLE', page: 0, size: 20, sort: 'publishedYear,desc' }, 'library-token')
    expect(router.currentRoute.value.query.page).toBeUndefined()
    const afterFirstApply = mocks.listBooks.mock.calls.length
    await wrapper.findAll('button').find((element) => element.text() === 'Tìm kiếm')!.trigger('click')
    await flushPromises()
    expect(mocks.listBooks).toHaveBeenCalledTimes(afterFirstApply + 1)

    await wrapper.findAll('button').find((element) => element.text() === 'Xóa bộ lọc')!.trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({})
    expect(mocks.listBooks).toHaveBeenLastCalledWith({ keyword: undefined, category: undefined, publishedYear: undefined, availability: undefined, page: 0, size: 20, sort: 'title,asc' }, 'library-token')
  })

  it('ignores an older response after the route query triggers a newer load', async () => {
    let resolveOld!: (value: ReturnType<typeof page>) => void
    let resolveNew!: (value: ReturnType<typeof page>) => void
    mocks.listBooks.mockReturnValueOnce(new Promise((resolve) => { resolveOld = resolve }))
      .mockReturnValueOnce(new Promise((resolve) => { resolveNew = resolve }))
    await router.push({ name: 'v2-library-books', query: { keyword: 'old' } })
    const wrapper = mountList()
    await router.push({ name: 'v2-library-books', query: { keyword: 'new' } })
    await flushPromises()
    resolveNew(page([{ id: 2, title: 'Newest result', author: 'Author', totalCopyCount: 1, availableBorrowableCopyCount: 1 }], 1))
    await flushPromises()
    resolveOld(page([{ id: 1, title: 'Stale result', author: 'Author', totalCopyCount: 1, availableBorrowableCopyCount: 1 }], 1))
    await flushPromises()
    expect(wrapper.text()).toContain('Newest result')
    expect(wrapper.text()).not.toContain('Stale result')
  })

  it('shows empty, retryable transport error and 403 states without discarding the session', async () => {
    mocks.listBooks.mockResolvedValueOnce(page())
    const wrapper = mountList()
    await flushPromises()
    expect(wrapper.text()).toContain('Không tìm thấy đầu sách')

    mocks.listBooks.mockRejectedValueOnce(new Error('temporary network issue'))
    await router.push({ name: 'v2-library-books', query: { keyword: 'temporary' } })
    await flushPromises()
    expect(wrapper.text()).toContain('temporary network issue')

    mocks.listBooks.mockRejectedValueOnce(new ApiError(403, 'Library access denied', { kind: 'forbidden' }))
    await wrapper.get('[data-testid="page-state"] button').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="page-state"]').text()).toContain('403')
    expect(getAuthSession()?.accessToken).toBe('library-token')
  })
})
