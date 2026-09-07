import type { Meta, StoryObj } from '@storybook/vue3'
import SemesterNotificationPanel from './SemesterNotificationPanel.vue'

const meta = {
  title: 'Academic/SemesterNotificationPanel',
  component: SemesterNotificationPanel,
  args: {
    notifications: [{ id: 1, semesterId: 11, recipientEmail: 'teacher@example.test', recipientRole: 'TEACHER', status: 'SENT', subject: 'Nhắc nhập điểm', attemptCount: 1, sentAt: '2027-01-15T10:00:00', errorMessage: null, createdAt: '2027-01-15T09:00:00', updatedAt: '2027-01-15T10:00:00' }],
  },
} satisfies Meta<typeof SemesterNotificationPanel>

export default meta
type Story = StoryObj<typeof meta>

export const Sent: Story = {}
export const Failed: Story = { args: { notifications: [{ ...meta.args.notifications[0], status: 'FAILED', errorMessage: 'SMTP tạm thời không phản hồi.' }] } }
export const Empty: Story = { args: { notifications: [] } }
export const Loading: Story = { args: { notifications: [], loading: true } }
