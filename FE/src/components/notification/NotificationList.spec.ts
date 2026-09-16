import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import NotificationList from './NotificationList.vue'
import { mockNotificationRead, mockNotificationUnread } from '@/fixtures/notificationFixture'

describe('NotificationList', () => {
  it('displays loading indicator when loading is true', () => {
    const wrapper = mount(NotificationList, {
      props: { items: [], loading: true },
      global: { stubs: { Button: true, Tag: true, NotificationStatusBadge: true } },
    })
    expect(wrapper.text()).toContain('Đang tải danh sách thông báo...')
  })

  it('displays empty state when items list is empty', () => {
    const wrapper = mount(NotificationList, {
      props: { items: [], loading: false },
      global: { stubs: { Button: true, Tag: true, NotificationStatusBadge: true } },
    })
    expect(wrapper.text()).toContain('Không có thông báo nào.')
  })

  it('renders items and emits select event when clicked', async () => {
    const wrapper = mount(NotificationList, {
      props: {
        items: [mockNotificationUnread, mockNotificationRead],
        loading: false,
      },
      global: {
        stubs: {
          Button: { props: ['label'], template: '<button @click="$emit(\'click\')">{{ label }}</button>' },
          Tag: { props: ['value'], template: '<span>{{ value }}</span>' },
          NotificationStatusBadge: true,
        },
      },
    })

    expect(wrapper.text()).toContain(mockNotificationUnread.title)
    expect(wrapper.text()).toContain(mockNotificationRead.title)

    const items = wrapper.findAll('[data-testid="notification-item"]')
    expect(items).toHaveLength(2)

    await items[0].trigger('click')
    expect(wrapper.emitted('select')).toBeTruthy()
    expect(wrapper.emitted('select')![0][0]).toEqual(mockNotificationUnread)
  })

  it('emits markRead when mark-read button is clicked on unread item', async () => {
    const wrapper = mount(NotificationList, {
      props: {
        items: [mockNotificationUnread],
        loading: false,
        isManageView: false,
      },
      global: {
        stubs: {
          Button: {
            props: ['label'],
            template: '<button @click.stop="$emit(\'click\')">{{ label }}</button>',
          },
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
    expect(wrapper.emitted('markRead')![0][0]).toEqual(mockNotificationUnread)
  })
})

