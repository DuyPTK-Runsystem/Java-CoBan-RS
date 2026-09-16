import type { Meta, StoryObj } from '@storybook/vue3'
import NotificationStatusBadge from './NotificationStatusBadge.vue'

const meta = {
  title: 'Notification/StatusBadge',
  component: NotificationStatusBadge,
  tags: ['autodocs'],
} satisfies Meta<typeof NotificationStatusBadge>

export default meta
type Story = StoryObj<typeof meta>

export const Draft: Story = { args: { status: 'DRAFT' } }
export const Scheduled: Story = { args: { status: 'SCHEDULED' } }
export const Published: Story = { args: { status: 'PUBLISHED' } }
export const Cancelled: Story = { args: { status: 'CANCELLED' } }
export const Expired: Story = { args: { status: 'EXPIRED' } }

