import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { clearAuthSession, saveAuthSession } from '@/services/authSession'
import LibraryCirculationView from './LibraryCirculationView.vue'

const mocks = vi.hoisted(() => ({
  borrowLibraryCopies: vi.fn(), markLibraryCopyLost: vi.fn(), renewLibraryLoan: vi.fn(), returnLibraryCopies: vi.fn(),
  getLibraryCirculationPolicy: vi.fn(), getBookCopyByBarcode: vi.fn(), getLibraryPatron: vi.fn(), verifyLibraryCard: vi.fn(),
}))
vi.mock('@/services/library/libraryCirculationApi', () => ({
  borrowLibraryCopies: mocks.borrowLibraryCopies,
  markLibraryCopyLost: mocks.markLibraryCopyLost,
  renewLibraryLoan: mocks.renewLibraryLoan,
  returnLibraryCopies: mocks.returnLibraryCopies,
}))
vi.mock('@/services/library/libraryPolicyApi', () => ({ getLibraryCirculationPolicy: mocks.getLibraryCirculationPolicy }))
vi.mock('@/services/library/libraryCatalogApi', () => ({ getBookCopyByBarcode: mocks.getBookCopyByBarcode }))
vi.mock('@/services/library/libraryPatronApi', () => ({
  getLibraryPatron: mocks.getLibraryPatron,
  verifyLibraryCard: mocks.verifyLibraryCard,
}))

enableAutoUnmount(afterEach)

const inputText = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" :value="modelValue ?? \'\'" @input="$emit(\'update:modelValue\', $event.target.value)" />',
}
const button = {
  props: ['label', 'disabled', 'loading'],
  emits: ['click'],
  template: '<button v-bind="$attrs" :disabled="disabled || loading" @click="$emit(\'click\')">{{ label }}</button>',
}
const message = { props: ['severity'], template: '<p data-testid="alert"><slot /></p>' }

function mountView() {
  return mount(LibraryCirculationView, {
    global: {
      stubs: { Button: button, InputText: inputText, Message: message, ScannerUploadDemo: true, Tag: true },
    },
  })
}

describe('LibraryCirculationView lost flow', () => {
  beforeEach(() => {
    clearAuthSession()
    saveAuthSession({ accessToken: 'circulation-token', user: { id: 1, username: 'librarian', roles: ['LIBRARIAN'] } })
    mocks.markLibraryCopyLost.mockReset().mockResolvedValue({
      loan: { loanId: 22, status: 'LOST' },
      fine: { fineId: 88, type: 'LOST_ITEM', amount: '173456.78', currency: 'VND' },
    })
    mocks.getLibraryCirculationPolicy.mockReset().mockResolvedValue({
      maxActiveLoans: 5, loanDurationDays: 14, maxRenewals: 2, renewalDurationDays: 7,
      policyVersion: 'LIB-POL-1', fineTiers: [], fineCapPerLoan: '500000', fineSuspensionThreshold: '500000',
      reservationPickupDays: 3,
    })
  })

  afterEach(() => clearAuthSession())

  it('shows the lost loan and LOST_ITEM fine returned by the API', async () => {
    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('input[aria-label="Barcode bản sao bị mất"]').setValue('LOST-31')
    await wrapper.get('input[aria-label="Lý do đánh dấu mất"]').setValue('confirmed missing')
    await wrapper.findAll('button').find((item) => item.text() === 'Xác nhận mất')!.trigger('click')
    await flushPromises()

    expect(mocks.markLibraryCopyLost).toHaveBeenCalledWith('LOST-31', 'confirmed missing', 'circulation-token')
    expect(wrapper.get('[data-testid="alert"]').text()).toContain('lượt mượn #22')
    expect(wrapper.get('[data-testid="alert"]').text()).toContain('Khoản phạt mất sách #88: 173.456,78 VND')
  })
})
