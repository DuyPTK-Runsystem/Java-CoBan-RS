import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import NotificationStatusBadge from './NotificationStatusBadge.vue'

describe('NotificationStatusBadge', () => {
  it.each([
    ['DRAFT', 'Bản nháp', 'secondary'],
    ['SCHEDULED', 'Đã lên lịch', 'info'],
    ['PUBLISHED', 'Đã phát hành', 'success'],
    ['CANCELLED', 'Đã hủy', 'danger'],
    ['EXPIRED', 'Đã hết hạn', 'warn'],
  ] as const)('maps status %s to %s with severity %s', (status, label, severity) => {
    const wrapper = mount(NotificationStatusBadge, {
      props: { status },
      global: {
        stubs: {
          Tag: { props: ['value', 'severity'], template: '<span class="tag">{{ value }} - {{ severity }}</span>' },
        },
      },
    })
    expect(wrapper.text()).toContain(label)
    expect(wrapper.text()).toContain(severity)
  })
})

