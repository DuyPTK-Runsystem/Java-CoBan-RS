import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import SemesterNotificationPanel from './SemesterNotificationPanel.vue'

const buttonStub = {
  props: ['label'],
  template: '<button>{{ label }}</button>',
}

const notification = (status: 'PENDING' | 'SENT' | 'FAILED', id: number) => ({
  id,
  semesterId: 11,
  recipientEmail: `${id}@school.edu.vn`,
  recipientRole: 'SUBJECT_TEACHER',
  status,
  subject: 'Nhắc nhập điểm',
  attemptCount: 1,
  sentAt: status === 'SENT' ? '2026-09-21T08:00:00' : null,
  errorMessage: status === 'FAILED' ? 'SMTP không phản hồi' : null,
  createdAt: '2026-09-21T08:00:00',
  updatedAt: '2026-09-21T08:00:00',
})

function mountPanel(notifications: ReturnType<typeof notification>[]) {
  return mount(SemesterNotificationPanel, {
    props: { notifications },
    global: { stubs: { Button: buttonStub } },
  })
}

describe('SemesterNotificationPanel', () => {
  it('shows a green outcome only when every recipient is sent', () => {
    const wrapper = mountPanel([notification('SENT', 1), notification('SENT', 2)])

    expect(wrapper.find('.form-alert-success').text()).toContain('2 người nhận')
    expect(wrapper.find('.form-alert-warning').exists()).toBe(false)
    expect(wrapper.find('.form-alert-error').exists()).toBe(false)
    expect(wrapper.text()).toContain('Đã chuyển gửi')
  })

  it('shows a warning for a partial outcome and keeps the failed recipient error', () => {
    const wrapper = mountPanel([notification('SENT', 1), notification('FAILED', 2)])

    expect(wrapper.find('.form-alert-warning').text()).toContain('1 thất bại')
    expect(wrapper.find('.form-alert-error').exists()).toBe(false)
    expect(wrapper.text()).toContain('SMTP không phản hồi')
    expect(wrapper.text()).toContain('Gửi thất bại')
  })

  it('shows an error when every recipient failed', () => {
    const wrapper = mountPanel([notification('FAILED', 1), notification('FAILED', 2)])

    expect(wrapper.find('.form-alert-error').text()).toContain('2 người nhận')
    expect(wrapper.find('.form-alert-success').exists()).toBe(false)
    expect(wrapper.find('.form-alert-warning').exists()).toBe(false)
  })
})
