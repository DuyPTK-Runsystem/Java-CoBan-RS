import type { Meta, StoryObj } from '@storybook/vue3'

import StudentSearchForm from './StudentSearchForm.vue'

const meta = {
  title: 'Student/StudentSearchForm',
  component: StudentSearchForm,
  tags: ['autodocs'],
  args: {
    loading: false,
  },
} satisfies Meta<typeof StudentSearchForm>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const Loading: Story = {
  args: {
    loading: true,
  },
}
