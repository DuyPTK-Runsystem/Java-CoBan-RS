import type { Meta, StoryObj } from '@storybook/vue3'

import StudentTable from './StudentTable.vue'

const students = [
  {
    studentId: 1,
    studentCode: 'STU1234567',
    studentName: 'John Doe',
    dateOfBirth: '2000-08-19',
    address: 'Ho Chi Minh City',
    averageScore: 6.7,
  },
  {
    studentId: 2,
    studentCode: 'STU7654321',
    studentName: 'Jane Doe',
    dateOfBirth: '2001-01-08',
    address: 'Da Nang',
    averageScore: 8.2,
  },
]

const meta = {
  title: 'Student/StudentTable',
  component: StudentTable,
  tags: ['autodocs'],
  args: {
    students,
    loading: false,
    totalRecords: students.length,
    page: 0,
    rowsPerPage: 10,
    sortField: 'studentCode',
    sortOrder: 1,
  },
} satisfies Meta<typeof StudentTable>

export default meta
type Story = StoryObj<typeof meta>

export const Populated: Story = {}

export const Loading: Story = {
  args: {
    loading: true,
  },
}

export const Empty: Story = {
  args: {
    students: [],
    totalRecords: 0,
  },
}
