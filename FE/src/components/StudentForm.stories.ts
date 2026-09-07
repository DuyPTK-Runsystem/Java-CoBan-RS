import type { Meta, StoryObj } from '@storybook/vue3'

import StudentForm from './StudentForm.vue'

const meta = {
  title: 'Student/StudentForm',
  component: StudentForm,
  tags: ['autodocs'],
  args: {
    mode: 'add',
    initialValue: {
      studentCode: '',
      studentName: '',
      dateOfBirth: null,
      address: '',
      averageScore: null,
    },
    saving: false,
    generating: false,
    errorMessage: '',
  },
} satisfies Meta<typeof StudentForm>

export default meta
type Story = StoryObj<typeof meta>

export const AddDefault: Story = {}

export const AddFilled: Story = {
  args: {
    initialValue: {
      studentCode: 'STU1234567',
      studentName: 'John Doe',
      dateOfBirth: new Date(2000, 7, 19),
      address: 'Ho Chi Minh City',
      averageScore: 6.7,
    },
  },
}

export const InvalidStudentCode: Story = {
  args: {
    initialValue: {
      studentCode: 'STU123',
      studentName: 'John Doe',
      dateOfBirth: new Date(2000, 7, 19),
      address: 'Ho Chi Minh City',
      averageScore: 6.7,
    },
  },
}

export const EditCodeDisabled: Story = {
  args: {
    mode: 'edit',
    initialValue: {
      studentId: 1,
      studentCode: 'STU1234567',
      studentName: 'John Doe',
      dateOfBirth: new Date(2000, 7, 19),
      address: 'Ho Chi Minh City',
      averageScore: 6.7,
    },
  },
}

export const Saving: Story = {
  args: {
    saving: true,
    initialValue: {
      studentCode: 'STU1234567',
      studentName: 'John Doe',
      dateOfBirth: new Date(2000, 7, 19),
      address: 'Ho Chi Minh City',
      averageScore: 6.7,
    },
  },
}

export const ApiError: Story = {
  args: {
    errorMessage: 'Unable to save student. Please try again.',
  },
}
