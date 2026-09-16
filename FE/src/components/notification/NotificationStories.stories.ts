import type { Meta, StoryObj } from '@storybook/vue3'
import NotificationList from './NotificationList.vue'
import {
  mockNotificationCancelled,
  mockNotificationDraft,
  mockNotificationExpired,
  mockNotificationRead,
  mockNotificationUnread,
} from '@/fixtures/notificationFixture'

const meta = {
  title: 'Notification/Scenarios',
  component: NotificationList,
  tags: ['autodocs'],
} satisfies Meta<typeof NotificationList>

export default meta
type Story = StoryObj<typeof meta>

export const InboxUnread: Story = {
  args: {
    items: [mockNotificationUnread],
    loading: false,
    isManageView: false,
  },
}

export const InboxRead: Story = {
  args: {
    items: [mockNotificationRead],
    loading: false,
    isManageView: false,
  },
}

export const InboxEmpty: Story = {
  args: {
    items: [],
    loading: false,
    isManageView: false,
  },
}

export const InboxLoading: Story = {
  args: {
    items: [],
    loading: true,
    isManageView: false,
  },
}

export const ManageDraft: Story = {
  args: {
    items: [mockNotificationDraft],
    loading: false,
    isManageView: true,
  },
}

export const AudienceIndividual: Story = {
  args: {
    items: [mockNotificationCancelled], // targetReference: '10,11,12', INDIVIDUAL
    loading: false,
    isManageView: true,
  },
}

export const AudienceClass: Story = {
  args: {
    items: [mockNotificationRead], // targetReference: '10A1', CLASS
    loading: false,
    isManageView: false,
  },
}

export const AudienceSchool: Story = {
  args: {
    items: [mockNotificationUnread], // SCHOOL
    loading: false,
    isManageView: false,
  },
}

export const Expired: Story = {
  args: {
    items: [mockNotificationExpired],
    loading: false,
    isManageView: false,
  },
}

export const Forbidden: Story = {
  render: () => ({
    template: `
      <div class="p-6 bg-red-50 border border-red-200 text-red-700 rounded-lg">
        <h4 class="font-bold">Lỗi 403: Không có quyền truy cập</h4>
        <p class="text-sm">Bạn không thuộc danh sách người nhận hoặc không có quyền quản lý thông báo này.</p>
      </div>
    `,
  }),
}

export const PublishConflict: Story = {
  render: () => ({
    template: `
      <div class="p-6 bg-amber-50 border border-amber-200 text-amber-800 rounded-lg">
        <h4 class="font-bold">Lỗi 409: Xung đột phiên bản</h4>
        <p class="text-sm">Dữ liệu thông báo đã thay đổi hoặc đã được xuất bản bởi quản trị viên khác. Vui lòng tải lại trang.</p>
      </div>
    `,
  }),
}

export const ValidationError: Story = {
  render: () => ({
    template: `
      <div class="p-6 bg-red-50 border border-red-200 text-red-700 rounded-lg">
        <h4 class="font-bold">Lỗi 422: Dữ liệu không hợp lệ</h4>
        <p class="text-sm">Tiêu đề thông báo và nội dung không được để trống. Vui lòng chọn đối tượng nhận hợp lệ.</p>
      </div>
    `,
  }),
}

export const NetworkError: Story = {
  render: () => ({
    template: `
      <div class="p-6 bg-slate-50 border border-slate-300 text-slate-700 rounded-lg">
        <h4 class="font-bold">Lỗi kết nối mạng</h4>
        <p class="text-sm">Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại đường truyền mạng.</p>
      </div>
    `,
  }),
}

