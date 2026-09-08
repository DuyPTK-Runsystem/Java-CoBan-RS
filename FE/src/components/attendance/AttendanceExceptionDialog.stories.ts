import type { Meta, StoryObj } from '@storybook/vue3'

import AttendanceExceptionDialog from './AttendanceExceptionDialog.vue'

const meta = {
  title: 'Attendance/AttendanceExceptionDialog',
  component: AttendanceExceptionDialog,
  tags: ['autodocs'],
  args: {
    visible: true,
    student: { studentId: 12, studentCode: 'HS002', studentName: 'Trần Gia Bảo', attendanceRecordId: 31, status: 'ABSENT', note: 'Chưa nộp giấy xin phép', recordedBy: 5, recordedAt: '2026-09-04T08:04:00', updatedBy: null, updatedAt: null },
    session: { sessionId: 5012, classId: 3, semesterId: 2, attendanceDate: '2026-09-04', sessionPeriod: 'MORNING', createdBy: 5, createdAt: '2026-09-04T07:40:00' },
  },
  parameters: { layout: 'centered' },
} satisfies Meta<typeof AttendanceExceptionDialog>

export default meta
type Story = StoryObj<typeof meta>

export const EditExisting: Story = {}
export const NewException: Story = { args: { student: { ...meta.args.student, attendanceRecordId: null, status: 'PRESENT', note: null } } }
export const Saving: Story = { args: { saving: true } }
export const ApiError: Story = { args: { errorMessage: 'Buổi điểm danh đã bị khóa.' } }
