import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import LibraryMyCardView from './LibraryMyCardView.vue'

const mocks = vi.hoisted(() => ({
  getLibraryCardQr: vi.fn(), getMyLibraryCard: vi.fn(), getMyLibraryPatron: vi.fn(), listMyLibraryCardHistory: vi.fn(),
  replace: vi.fn(),
}))
vi.mock('vue-router', () => ({ useRouter: () => ({ replace: mocks.replace }) }))
vi.mock('@/services/library/libraryPatronApi', () => mocks)

enableAutoUnmount(afterEach)

const patron = { patronId: 4, userId: 15, displayName: 'Nguyễn An', status: 'ACTIVE', joinedAt: '2026-10-09', suspensionReasons: [], currentCard: null }
const card = { cardNo: 'LC-2026-000001', patronId: 4, status: 'ACTIVE', issuedAt: '2026-10-09', expiresAt: '2027-10-08', payloadVersion: 'v1', policyVersion: 'v1' }
const button = { props: ['label', 'disabled', 'loading'], emits: ['click'], template: '<button :disabled="disabled || loading" @click="$emit(\'click\')">{{ label }}</button>' }
const pageState = { props: ['state', 'errorMessage'], emits: ['retry'], template: '<div>{{ state }} {{ errorMessage }}</div>' }
const preview = { props: ['cardNo', 'displayName', 'expiresAt', 'qrUrl'], template: '<div data-testid="card-preview">{{ cardNo }} {{ displayName }} {{ expiresAt }} {{ qrUrl }}</div>' }

function mountCard() {
  return mount(LibraryMyCardView, {
    global: { stubs: { Button: button, FormAlert: true, LibraryCardPreview: preview, PageState: pageState, StatusTag: true } },
  })
}

describe('LibraryMyCardView', () => {
  beforeEach(() => {
    clearAuthSession()
    saveAuthSession({ accessToken: 'my-card-token', user: { id: 15, username: 'owner', roles: ['STUDENT'] } })
    mocks.getMyLibraryPatron.mockReset().mockResolvedValue(patron)
    mocks.getMyLibraryCard.mockReset().mockResolvedValue(card)
    mocks.listMyLibraryCardHistory.mockReset().mockResolvedValue([card])
    mocks.getLibraryCardQr.mockReset().mockResolvedValue(new Blob(['png'], { type: 'image/png' }))
    vi.stubGlobal('URL', { createObjectURL: vi.fn(() => 'blob:card-qr'), revokeObjectURL: vi.fn() })
  })
  afterEach(() => { clearAuthSession(); vi.unstubAllGlobals() })

  it('loads only the signed-in patron card and requests its QR with the session token', async () => {
    const wrapper = mountCard()
    await flushPromises()

    expect(mocks.getMyLibraryPatron).toHaveBeenCalledWith('my-card-token')
    expect(mocks.getMyLibraryCard).toHaveBeenCalledWith('my-card-token')
    expect(mocks.listMyLibraryCardHistory).toHaveBeenCalledWith('my-card-token')
    expect(mocks.getLibraryCardQr).toHaveBeenCalledWith(card.cardNo, 'my-card-token')
    expect(wrapper.get('[data-testid="card-preview"]').text()).toContain('blob:card-qr')
    expect(wrapper.text()).toContain('08/10/2027')
    wrapper.unmount()
  })
})
