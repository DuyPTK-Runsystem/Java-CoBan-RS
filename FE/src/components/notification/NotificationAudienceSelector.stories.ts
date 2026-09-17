import type { Meta, StoryObj } from '@storybook/vue3'
import NotificationAudienceSelector from './NotificationAudienceSelector.vue'

const meta = {
  title: 'Notification/AudienceSelector',
  component: NotificationAudienceSelector,
  tags: ['autodocs'],
} satisfies Meta<typeof NotificationAudienceSelector>

export default meta
type Story = StoryObj<typeof meta>

export const AudienceSchool: Story = {
  args: {
    audienceType: 'SCHOOL',
    targetReference: '',
  },
}

export const AudienceClass: Story = {
  args: {
    audienceType: 'CLASS',
    targetReference: '101',
  },
}

export const AudienceIndividual: Story = {
  args: {
    audienceType: 'INDIVIDUAL',
    recipientUserIds: [2, 5, 10],
  },
}

export const AudienceIndividualFiltered: Story = {
  args: {
    audienceType: 'INDIVIDUAL',
    recipientUserIds: [2, 5],
  },
  parameters: {
    docs: {
      description: {
        story: 'Multi-select người nhận với tìm kiếm tên, role, lớp học sinh và lớp giáo viên; filter chỉ ảnh hưởng lookup, không đi vào payload composer.',
      },
    },
  },
}
