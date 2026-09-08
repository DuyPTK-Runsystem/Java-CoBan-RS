import type { Meta, StoryObj } from '@storybook/vue3'

import RegisterForm from './RegisterForm.vue'

const meta = {
  title: 'Auth/RegisterForm',
  component: RegisterForm,
  tags: ['autodocs'],
  args: {
    submitting: false,
    errorMessage: '',
  },
} satisfies Meta<typeof RegisterForm>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}

export const Filled: Story = {
  args: {
    initialValues: {
      userName: 'academic.admin',
      password: 'secret12',
      confirmPassword: 'secret12',
    },
  },
}

export const PasswordMismatch: Story = {
  args: {
    errorMessage: 'Passwords do not match.',
    initialValues: {
      userName: 'academic.admin',
      password: 'secret12',
      confirmPassword: 'different',
    },
  },
}

export const ValidationError: Story = {
  args: {
    errorMessage: 'Please complete all required fields.',
    initialValues: {
      userName: '',
      password: 'short',
      confirmPassword: '',
    },
  },
}
