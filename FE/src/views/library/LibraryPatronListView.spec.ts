import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import LibraryPatronListView from './LibraryPatronListView.vue'

const mocks = vi.hoisted(() => ({
  activateLibraryPatron: vi.fn(), issueLibraryCard: vi.fn(), listLibraryPatrons: vi.fn(), listPatronActivationCandidates: vi.fn(),
  reissueLibraryCard: vi.fn(), revokeLibraryCard: vi.fn(), updateLibraryPatronStatus: vi.fn(), replace: vi.fn(), push: vi.fn(),
  route: { query: {} as Record<string, unknown> },
}))
vi.mock('vue-router', () => ({ useRoute: () => mocks.route, useRouter: () => ({ replace: mocks.replace, push: mocks.push, resolve: () => ({ fullPath: '/v2/library/patrons' }) }) }))
vi.mock('@/services/library/libraryPatronApi', () => mocks)

enableAutoUnmount(afterEach)

const candidate = { userId: 25, displayName: 'Lê Bình', username: 'binh', roleCode: 'ADMIN' }
const button = { props: ['label', 'disabled', 'loading'], emits: ['click'], template: '<button :disabled="disabled || loading" @click="$emit(\'click\')">{{ label }}</button>' }
const inputText = { props: ['modelValue'], emits: ['update:modelValue'], template: '<input v-bind="$attrs" :value="modelValue ?? \'\'" @input="$emit(\'update:modelValue\', $event.target.value)" />' }
const select = { props: ['modelValue', 'options'], emits: ['update:modelValue'], template: '<select v-bind="$attrs" :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value)"><option v-for="item in options" :key="String(item.value)" :value="item.value">{{ item.label }}</option></select>' }
const dataTable = { props: ['value'], template: '<div><slot name="empty" v-if="!value.length" /></div>' }
const dialog = { props: ['visible'], emits: ['update:visible'], template: '<div v-if="visible"><slot /></div>' }
const pageState = { props: ['state', 'forbidden', 'errorMessage'], emits: ['retry'], template: '<div>{{ state }} {{ errorMessage }}</div>' }
const issueDialog = { props: ['visible', 'mode'], emits: ['submit', 'update:visible'], template: '<div v-if="visible" data-testid="issue-dialog">{{ mode }}<button data-testid="issue-submit" @click="$emit(\'submit\', { expiresAt: \'2099-12-31\', reason: \'Replace\' })">Submit card</button></div>' }

function mountList() {
  return mount(LibraryPatronListView, {
    global: { stubs: { Button: button, Column: true, DataTable: dataTable, Dialog: dialog, FormAlert: true, InputText: inputText, LibraryCardIssueDialog: issueDialog, PageState: pageState, Paginator: true, Select: select, StatusTag: true } },
  })
}

describe('LibraryPatronListView', () => {
  beforeEach(() => {
    clearAuthSession()
    saveAuthSession({ accessToken: 'patron-token', user: { id: 1, username: 'admin', roles: ['ADMIN'] } })
    mocks.route = { query: {} }
    mocks.replace.mockReset()
    mocks.push.mockReset()
    mocks.listLibraryPatrons.mockReset().mockResolvedValue({ meta: { page: 0, pageSize: 20, totalPages: 0, totalItems: 0 }, result: [] })
    mocks.listPatronActivationCandidates.mockReset().mockResolvedValue({ meta: { page: 0, pageSize: 10, totalPages: 1, totalItems: 1 }, result: [candidate] })
    mocks.activateLibraryPatron.mockReset().mockResolvedValue(undefined)
  })
  afterEach(() => clearAuthSession())

  it('activates a selected existing account even when its role is excluded from borrowing', async () => {
    const wrapper = mountList()
    await flushPromises()
    await wrapper.findAll('button').find((item) => item.text() === 'Kích hoạt bạn đọc')!.trigger('click')
    await flushPromises()

    expect(mocks.listPatronActivationCandidates).toHaveBeenCalledWith('', 0, 10, 'patron-token')
    expect(wrapper.text()).toContain('ADMIN')
    await wrapper.findAll('button').find((item) => item.text().includes('binh'))!.trigger('click')
    await wrapper.findAll('button').find((item) => item.text() === 'Kích hoạt hồ sơ')!.trigger('click')
    await flushPromises()

    expect(mocks.activateLibraryPatron).toHaveBeenCalledWith({ userId: candidate.userId }, 'patron-token')
  })
})
