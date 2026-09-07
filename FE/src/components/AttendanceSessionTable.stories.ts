import type { Meta, StoryObj } from '@storybook/vue3'

import AttendanceSessionTable from './AttendanceSessionTable.vue'

const meta = {
  title: 'Attendance/AttendanceSessionTable',
  component: AttendanceSessionTable,
  tags: ['autodocs'],
  args: {
    students: [
      { studentId: 11, studentCode: 'HS001', studentName: 'Nguyễn Minh An', attendanceRecordId: null, status: 'PRESENT', note: null, recordedBy: null, recordedAt: null, updatedBy: null, updatedAt: null },
      { studentId: 12, studentCode: 'HS002', studentName: 'Trần Gia Bảo', attendanceRecordId: 31, status: 'ABSENT', note: 'Chưa nộp giấy xin phép', recordedBy: 5, recordedAt: '2026-09-04T08:04:00', updatedBy: null, updatedAt: null },
      { studentId: 13, studentCode: 'HS003', studentName: 'Lê Khánh Chi', attendanceRecordId: 32, status: 'EXCUSED', note: 'Đau bụng', recordedBy: 5, recordedAt: '2026-09-04T08:07:00', updatedBy: null, updatedAt: null },
    ],
  },
  parameters: { layout: 'padded' },
} satisfies Meta<typeof AttendanceSessionTable>

export default meta
type Story = StoryObj<typeof meta>

export const Filled: Story = {}
export const Empty: Story = { args: { students: [] } }
export const ReadOnly: Story = { args: { readOnly: true } }
