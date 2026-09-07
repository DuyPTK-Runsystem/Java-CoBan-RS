import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ScorebookStatusHeader from './ScorebookStatusHeader.vue'

const buttonStub = {
  props: ['label', 'disabled'],
  template: '<button :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
}

function mountHeader(status: 'DRAFT' | 'OPEN' | 'PUBLISHED' | 'CLOSED') {
  return mount(ScorebookStatusHeader, {
    props: { scorebook: { id: 12, status } },
    global: { stubs: { Button: buttonStub, Tag: true } },
  })
}

describe('ScorebookStatusHeader', () => {
  it('allows reopening a published scorebook', async () => {
    const wrapper = mountHeader('PUBLISHED')

    const reopenButton = wrapper.get('button:nth-of-type(2)')
    expect(reopenButton.text()).toBe('Mở lại sổ')
    expect(reopenButton.attributes('disabled')).toBeUndefined()

    await reopenButton.trigger('click')
    expect(wrapper.emitted('open')).toBeTruthy()
  })

  it('keeps a closed scorebook read-only', () => {
    const wrapper = mountHeader('CLOSED')

    expect(wrapper.get('button:nth-of-type(2)').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('Sổ điểm đang ở chế độ chỉ đọc.')
  })
})
