import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import NotificationDetail from './NotificationDetail.vue'
import { mockNotificationDraft, mockNotificationUnread } from '@/fixtures/notificationFixture'

describe('NotificationDetail', () => {
  it('renders notification content and title correctly', () => {
    const wrapper = mount(NotificationDetail, {
      props: {
        notification: mockNotificationUnread,
      },
      global: {
        stubs: {
          Button: true,
          Tag: true,
          NotificationStatusBadge: true,
        },
      },
    })

    expect(wrapper.text()).toContain(mockNotificationUnread.title)
    expect(wrapper.text()).toContain(mockNotificationUnread.body)
  })

  it('emits markRead when unread and markRead button is clicked', async () => {
    const wrapper = mount(NotificationDetail, {
      props: {
        notification: mockNotificationUnread,
      },
      global: {
        stubs: {
          Button: { props: ['label'], template: '<button @click="$emit(\'click\')">{{ label }}</button>' },
          Tag: true,
          NotificationStatusBadge: true,
        },
      },
    })

    const buttons = wrapper.findAll('button')
    const markReadBtn = buttons.find((b) => b.text().includes('Đánh dấu đã đọc'))
    expect(markReadBtn).toBeDefined()

    await markReadBtn!.trigger('click')
    expect(wrapper.emitted('markRead')).toBeTruthy()
  })

  it('shows publish and cancel buttons when notification is draft and canManage is true', async () => {
    const wrapper = mount(NotificationDetail, {
      props: {
        notification: mockNotificationDraft,
        canManage: true,
      },
      global: {
        stubs: {
          Button: { props: ['label'], template: '<button @click="$emit(\'click\')">{{ label }}</button>' },
          Tag: true,
          NotificationStatusBadge: true,
        },
      },
    })

    const buttons = wrapper.findAll('button')
    const publishBtn = buttons.find((b) => b.text().includes('Xuất bản'))
    const cancelBtn = buttons.find((b) => b.text().includes('Hủy thông báo'))

    expect(publishBtn).toBeDefined()
    expect(cancelBtn).toBeDefined()

    await publishBtn!.trigger('click')
    expect(wrapper.emitted('publish')).toBeTruthy()

    await cancelBtn!.trigger('click')
    expect(wrapper.emitted('cancel')).toBeTruthy()
  })

  it('emits back event when back button is clicked', async () => {
    const wrapper = mount(NotificationDetail, {
      props: {
        notification: mockNotificationUnread,
      },
      global: {
        stubs: {
          Button: { props: ['label'], template: '<button @click="$emit(\'click\')">{{ label }}</button>' },
          Tag: true,
          NotificationStatusBadge: true,
        },
      },
    })

    const backBtn = wrapper.findAll('button').find((b) => b.text().includes('Quay lại'))
    await backBtn!.trigger('click')
    expect(wrapper.emitted('back')).toBeTruthy()
  })
})

