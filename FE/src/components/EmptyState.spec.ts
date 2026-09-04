import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import EmptyState from './EmptyState.vue'
import { primeVueStubs } from '@/test/stubs'

describe('EmptyState', () => {
  it('renders the supplied empty content and emits its action', async () => {
    const wrapper = mount(EmptyState, {
      props: { heading: 'No classes', message: 'Create a class to get started.', actionLabel: 'Create class' },
      global: { stubs: primeVueStubs },
    })

    expect(wrapper.text()).toContain('No classes')
    expect(wrapper.text()).toContain('Create a class to get started.')
    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('action')).toHaveLength(1)
  })
})
