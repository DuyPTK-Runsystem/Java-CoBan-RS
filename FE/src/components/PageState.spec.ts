import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import PageState from './PageState.vue'
import { primeVueStubs } from '@/test/stubs'

describe('PageState', () => {
  it('renders loading, empty, error with retry, forbidden and success states', async () => {
    const loading = mount(PageState, { props: { state: 'loading' } })
    expect(loading.text()).toContain('Loading...')

    const empty = mount(PageState, { props: { state: 'empty', emptyHeading: 'No results' } })
    expect(empty.text()).toContain('No results')

    const error = mount(PageState, {
      props: { state: 'error', errorMessage: 'Could not load records.' },
      global: { stubs: primeVueStubs },
    })
    expect(error.text()).toContain('Could not load records.')
    await error.get('button').trigger('click')
    expect(error.emitted('retry')).toHaveLength(1)

    const forbidden = mount(PageState, { props: { state: 'success', forbidden: true, forbiddenMessage: 'Access denied.' } })
    expect(forbidden.text()).toContain('Access denied.')

    const success = mount(PageState, { props: { state: 'success' }, slots: { default: '<p>Loaded content</p>' } })
    expect(success.text()).toContain('Loaded content')
  })
})
