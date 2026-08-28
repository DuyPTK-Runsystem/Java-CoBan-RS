import type { Meta, StoryObj } from '@storybook/vue3'

import AttendanceHistoryPanel from './AttendanceHistoryPanel.vue'

const meta = {
  title: 'Attendance/AttendanceHistoryPanel',
  component: AttendanceHistoryPanel,
  tags: ['autodocs'],
  args: {
    academicYears: [{ id: 1, code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE', notes: null }],
    semesters: [{ id: 2, academicYearId: 1, code: 'HK1', name: 'Học kỳ 1', displayOrder: 1, startDate: '2026-09-01', endDate: '2026-12-31', automaticLockAt: null, status: 'ACTIVE', lockedAt: null, lockedBy: null, lockReason: null, reopenUntil: null }],
    academicYearId: 1,
    semesterId: 2,
    from: '2026-09-01',
    to: '2026-09-30',
    response: { items: [{ attendanceDate: '2026-09-04', sessionPeriod: 'MORNING', classId: 3, className: '6A1', status: 'PRESENT', attendanceRecordId: null, exceptionStatus: null, note: null }], summary: { validSessionCount: 20, presentCount: 18, excusedAbsenceCount: 1, unexcusedAbsenceCount: 0, lateCount: 1, earlyLeaveCount: 0 }, page: 0, size: 10, totalElements: 1, totalPages: 1 },
  },
  parameters: { layout: 'padded' },
} satisfies Meta<typeof AttendanceHistoryPanel>

export default meta
type Story = StoryObj<typeof meta>

export const ReadOnlyHistory: Story = {}
export const Empty: Story = { args: { response: { items: [], summary: { validSessionCount: 0, presentCount: 0, excusedAbsenceCount: 0, unexcusedAbsenceCount: 0, lateCount: 0, earlyLeaveCount: 0 }, page: 0, size: 10, totalElements: 0, totalPages: 0 } } }
export const Forbidden: Story = { args: { forbidden: true } }
