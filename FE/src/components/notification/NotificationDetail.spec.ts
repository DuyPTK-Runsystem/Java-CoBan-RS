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

  it('hides read tracking from the notification sender view', () => {
    const wrapper = mount(NotificationDetail, {
      props: {
        notification: mockNotificationDraft,
        canManage: true,
      },
      global: {
        stubs: {
          Button: true,
          Tag: true,
          NotificationStatusBadge: true,
        },
      },
    })

    const metadataBlocks = wrapper.findAll('.notification-detail-meta > div')
    expect(metadataBlocks.some((block) => block.text().includes('Theo dõi lượt đọc'))).toBe(false)
    expect(wrapper.text()).not.toContain('Hạn thông báo')
    expect(wrapper.text()).not.toContain('Chưa có thống kê tổng hợp')
    expect(wrapper.text()).not.toContain('Chưa có dữ liệu tổng hợp theo từng người nhận.')
  })

  it('labels recipient read state as the current user state', () => {
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

    expect(wrapper.text()).toContain('Trạng thái của bạn')
    expect(wrapper.text()).toContain('Chưa đọc')
    expect(wrapper.findAll('.notification-detail-meta > div').some((block) => block.text().includes('Theo dõi lượt đọc'))).toBe(false)
    expect(wrapper.text()).not.toContain('Hạn thông báo')
  })

  it('renders the class display label from audience details without exposing the raw target id', () => {
    const rawClassId = 'class-id-987654'
    const wrapper = mount(NotificationDetail, {
      props: {
        notification: {
          ...mockNotificationDraft,
          targetReference: rawClassId,
          audienceDetails: {
            audienceType: 'CLASS',
            displayLabel: 'Lớp 10A2',
            recipientCount: 35,
            displayDataAvailable: true,
          },
        },
        canManage: true,
      },
      global: {
        stubs: {
          Button: true,
          Tag: true,
          NotificationStatusBadge: true,
        },
      },
    })

    expect(wrapper.text()).toContain('Lớp 10A2')
    expect(wrapper.text()).toContain('35 người nhận')
    expect(wrapper.text()).not.toContain(rawClassId)
  })

  it('renders individual recipient count and display names without exposing raw ids', () => {
    const rawRecipientIds = 'user-id-101, user-id-202'
    const wrapper = mount(NotificationDetail, {
      props: {
        notification: {
          ...mockNotificationDraft,
          audienceType: 'INDIVIDUAL',
          targetReference: rawRecipientIds,
          audienceDetails: {
            audienceType: 'INDIVIDUAL',
            displayLabel: 'Người nhận cụ thể',
            recipientCount: 2,
            recipientDisplayNames: ['Nguyễn An', 'Trần Bình'],
            displayDataAvailable: true,
          },
        },
        canManage: true,
      },
      global: {
        stubs: {
          Button: true,
          Tag: true,
          NotificationStatusBadge: true,
        },
      },
    })

    expect(wrapper.text()).toContain('Người nhận cụ thể')
    expect(wrapper.text()).toContain('2 người nhận')
    expect(wrapper.text()).toContain('Người nhận: Nguyễn An, Trần Bình')
    expect(wrapper.text()).not.toContain(rawRecipientIds)
  })

  it('uses a privacy-safe fallback when audience display data is unavailable', () => {
    const rawRecipientId = 'user-id-303'
    const wrapper = mount(NotificationDetail, {
      props: {
        notification: {
          ...mockNotificationDraft,
          audienceType: 'INDIVIDUAL',
          targetReference: rawRecipientId,
          audienceDetails: {
            audienceType: 'INDIVIDUAL',
            displayDataAvailable: false,
          },
        },
        canManage: true,
      },
      global: {
        stubs: {
          Button: true,
          Tag: true,
          NotificationStatusBadge: true,
        },
      },
    })

    expect(wrapper.text()).toContain('Cá nhân')
    expect(wrapper.text()).toContain('Thông tin người nhận chưa được cung cấp đầy đủ.')
    expect(wrapper.text()).not.toContain(rawRecipientId)
  })

  it('uses a generic fallback when audience details are absent', () => {
    const rawClassId = 'class-id-404'
    const wrapper = mount(NotificationDetail, {
      props: {
        notification: { ...mockNotificationDraft, targetReference: rawClassId },
        canManage: true,
      },
      global: {
        stubs: {
          Button: true,
          Tag: true,
          NotificationStatusBadge: true,
        },
      },
    })

    expect(wrapper.text()).toContain('Lớp học')
    expect(wrapper.text()).toContain('Chưa có thông tin chi tiết về đối tượng nhận.')
    expect(wrapper.text()).not.toContain(rawClassId)
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
