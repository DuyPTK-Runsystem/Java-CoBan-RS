import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import V3ContractReviewPanel from './V3ContractReviewPanel.vue'
import { v3FoundationReviewContext, v3FoundationReviewItems } from '@/fixtures/v3FoundationFixture'
import { primeVueStubs } from '@/test/stubs'

describe('V3ContractReviewPanel', () => {
  it('renders a Vietnamese conflict state and emits retry', async () => {
    const wrapper = mount(V3ContractReviewPanel, {
      props: { state: 'conflict', context: v3FoundationReviewContext },
      global: { stubs: primeVueStubs },
    })

    expect(wrapper.text()).toContain('Dữ liệu đã thay đổi')
    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('retry')).toHaveLength(1)
  })

  it('renders review-only rows without requiring a backend', () => {
    const wrapper = mount(V3ContractReviewPanel, {
      props: { state: 'ready', context: v3FoundationReviewContext, items: v3FoundationReviewItems },
      global: { stubs: primeVueStubs },
    })

    expect(wrapper.text()).toContain('Bản xem trước')
    expect(wrapper.findAll('tbody tr')).toHaveLength(2)
  })

  it('renders the authentication-required review state without a retry action', () => {
    const wrapper = mount(V3ContractReviewPanel, {
      props: { state: 'unauthorized', context: v3FoundationReviewContext },
      global: { stubs: primeVueStubs },
    })

    expect(wrapper.text()).toContain('Phiên đăng nhập đã hết hạn')
    expect(wrapper.find('button').exists()).toBe(false)
  })
})
