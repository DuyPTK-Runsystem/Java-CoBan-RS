import type { Meta, StoryObj } from '@storybook/vue3'

import AttendanceContextPanel from './AttendanceContextPanel.vue'

const meta = {
  title: 'Attendance/AttendanceContextPanel',
  component: AttendanceContextPanel,
  tags: ['autodocs'],
  args: {
    academicYears: [{ id: 1, code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE', notes: null }],
    semesters: [{ id: 2, academicYearId: 1, code: 'HK1', name: 'Học kỳ 1', displayOrder: 1, startDate: '2026-09-01', endDate: '2026-12-31', automaticLockAt: null, status: 'ACTIVE', lockedAt: null, lockedBy: null, lockReason: null, reopenUntil: null }],
    classes: [{ id: 3, academicYearId: 1, gradeLevelId: 6, classCode: '6A1', className: 'Lớp 6A1', capacity: 35, status: 'ACTIVE' }],
    academicYearId: 1,
    semesterId: 2,
    classId: 3,
    attendanceDate: '2026-09-04',
    sessionPeriod: 'MORNING',
    calendarStatus: 'SCHEDULED',
    calendarMessage: 'Ngày và buổi học hợp lệ để mở attendance session.',
  },
  parameters: { layout: 'padded' },
} satisfies Meta<typeof AttendanceContextPanel>

export default meta
type Story = StoryObj<typeof meta>

export const Scheduled: Story = {}
export const NoClass: Story = { args: { calendarStatus: 'NO_CLASS', calendarMessage: 'Ngày này là ngày nghỉ, không ghi điểm danh.' } }
export const Loading: Story = { args: { calendarLoading: true } }
