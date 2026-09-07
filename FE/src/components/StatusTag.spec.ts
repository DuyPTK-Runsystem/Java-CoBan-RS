import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import StatusTag from './StatusTag.vue'

describe('StatusTag', () => {
  it.each([
    ['IN_PROGRESS', 'In progress', 'warn'],
    ['FINISH', 'Finished', 'success'],
  ] as const)('maps %s to a readable label and severity', (status, label, severity) => {
    const wrapper = mount(StatusTag, {
      props: { status },
      global: {
        stubs: {
          Tag: { props: ['value', 'severity'], template: '<span>{{ value }} {{ severity }}</span>' },
        },
      },
    })
    expect(wrapper.text()).toContain(label)
    expect(wrapper.text()).toContain(severity)
  })
})
