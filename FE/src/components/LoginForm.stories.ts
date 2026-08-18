import type { Meta, StoryObj } from '@storybook/vue3'

import LoginForm from './LoginForm.vue'

const meta = {
  title: 'Auth/LoginForm',
  component: LoginForm,
  tags: ['autodocs'],
  args: {
    submitting: false,
    errorMessage: '',
  },
} satisfies Meta<typeof LoginForm>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const Filled: Story = {
  args: {
    initialValues: {
      userName: 'academic.admin',
      password: 'secret12',
    },
  },
}

export const ValidationError: Story = {
  args: {
    errorMessage: 'Please review the highlighted login fields.',
    initialValues: {
      userName: '',
      password: 'short',
    },
  },
}
