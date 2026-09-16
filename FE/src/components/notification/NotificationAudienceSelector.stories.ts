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
    targetReference: '10A1',
  },
}

export const AudienceIndividual: Story = {
  args: {
    audienceType: 'INDIVIDUAL',
    targetReference: '2, 5, 10',
  },
}

