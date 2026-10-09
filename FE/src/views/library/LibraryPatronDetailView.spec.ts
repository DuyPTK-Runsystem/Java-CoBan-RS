import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import LibraryPatronDetailView from './LibraryPatronDetailView.vue'

const mocks = vi.hoisted(() => ({
  getLibraryPatron: vi.fn(), getLibraryCardQr: vi.fn(), issueLibraryCard: vi.fn(), listLibraryPatronCards: vi.fn(),
  reissueLibraryCard: vi.fn(), revokeLibraryCard: vi.fn(), push: vi.fn(), route: { params: { patronId: '4' } },
}))
vi.mock('vue-router', () => ({ useRoute: () => mocks.route, useRouter: () => ({ push: mocks.push }) }))
vi.mock('@/services/library/libraryPatronApi', () => mocks)

enableAutoUnmount(afterEach)

const card = { cardNo: 'LC-2026-000001', patronId: 4, status: 'ACTIVE', issuedAt: '2026-10-09', expiresAt: '2027-10-08', payloadVersion: 'v1', policyVersion: 'v1' }
const patron = { patronId: 4, userId: 15, displayName: 'Nguyễn An', status: 'ACTIVE', joinedAt: '2026-10-09', suspensionReasons: [], currentCard: card }
const button = { props: ['label', 'disabled', 'loading'], emits: ['click'], template: '<button :disabled="disabled || loading" @click="$emit(\'click\')">{{ label }}</button>' }
const inputText = { props: ['modelValue'], emits: ['update:modelValue'], template: '<input v-bind="$attrs" :value="modelValue ?? \'\'" @input="$emit(\'update:modelValue\', $event.target.value)" />' }
const pageState = { props: ['state', 'forbidden', 'errorMessage'], emits: ['retry'], template: '<div>{{ state }} {{ errorMessage }}</div>' }
const formAlert = { props: ['message'], template: '<p>{{ message }}</p>' }
const dialog = { props: ['visible'], template: '<div v-if="visible"><slot /></div>' }
const issueDialog = {
  props: ['visible', 'mode'], emits: ['submit', 'update:visible'],
  template: '<div v-if="visible" data-testid="issue-dialog">{{ mode }}<button data-testid="issue-submit" @click="$emit(\'submit\', { expiresAt: \'2099-12-31\', reason: \'Damaged\' })">Submit card</button></div>',
}

function mountDetail() {
  return mount(LibraryPatronDetailView, {
    global: { stubs: { Button: button, Dialog: dialog, FormAlert: formAlert, InputText: inputText, LibraryCardIssueDialog: issueDialog, LibraryCardPreview: true, PageState: pageState, StatusTag: true } },
  })
}

describe('LibraryPatronDetailView', () => {
  beforeEach(() => {
    clearAuthSession()
    saveAuthSession({ accessToken: 'detail-token', user: { id: 1, username: 'librarian', roles: ['LIBRARIAN'] } })
    mocks.route = { params: { patronId: '4' } }
    mocks.getLibraryPatron.mockReset().mockResolvedValue(patron)
    mocks.listLibraryPatronCards.mockReset().mockResolvedValue([card])
    mocks.getLibraryCardQr.mockReset().mockResolvedValue(new Blob(['png'], { type: 'image/png' }))
    mocks.reissueLibraryCard.mockReset().mockResolvedValue(undefined)
    mocks.revokeLibraryCard.mockReset().mockResolvedValue(undefined)
    vi.stubGlobal('URL', { createObjectURL: vi.fn(() => 'blob:detail-qr'), revokeObjectURL: vi.fn() })
  })
  afterEach(() => clearAuthSession())

  it('reissues the active card atomically with the selected expiry and reason', async () => {
    const wrapper = mountDetail()
    await flushPromises()
    await wrapper.findAll('button').find((item) => item.text() === 'Cấp lại')!.trigger('click')
    await wrapper.get('[data-testid="issue-submit"]').trigger('click')
    await flushPromises()

    expect(mocks.reissueLibraryCard).toHaveBeenCalledWith(card.cardNo, { expiresAt: '2099-12-31', reason: 'Damaged' }, 'detail-token')
    expect(mocks.issueLibraryCard).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('requires a reason and sends the entered reason when revoking a card', async () => {
    const wrapper = mountDetail()
    await flushPromises()
    await wrapper.findAll('button').find((item) => item.text() === 'Thu hồi')!.trigger('click')
    await wrapper.findAll('button').find((item) => item.text() === 'Thu hồi thẻ')!.trigger('click')
    expect(wrapper.text()).toContain('Vui lòng nhập lý do thu hồi thẻ.')
    expect(mocks.revokeLibraryCard).not.toHaveBeenCalled()

    await wrapper.get('#detail-revoke-reason').setValue('  Damaged  ')
    await wrapper.findAll('button').find((item) => item.text() === 'Thu hồi thẻ')!.trigger('click')
    await flushPromises()
    expect(mocks.revokeLibraryCard).toHaveBeenCalledWith(card.cardNo, { reason: 'Damaged' }, 'detail-token')
    wrapper.unmount()
  })
})
