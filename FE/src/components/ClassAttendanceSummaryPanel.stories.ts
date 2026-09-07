import type { Meta, StoryObj } from '@storybook/vue3'

import ClassAttendanceSummaryPanel from './ClassAttendanceSummaryPanel.vue'

const meta = {
  title: 'Attendance/ClassAttendanceSummaryPanel',
  component: ClassAttendanceSummaryPanel,
  tags: ['autodocs'],
  args: {
    classes: [{ id: 3, academicYearId: 1, gradeLevelId: 6, classCode: '6A1', className: 'Lớp 6A1', capacity: 35, status: 'ACTIVE' }],
    semesters: [{ id: 2, academicYearId: 1, code: 'HK1', name: 'Học kỳ 1', displayOrder: 1, startDate: '2026-09-01', endDate: '2026-12-31', automaticLockAt: null, status: 'ACTIVE', lockedAt: null, lockedBy: null, lockReason: null, reopenUntil: null }],
    classId: 3,
    semesterId: 2,
    from: '2026-09-01',
    to: '2026-09-30',
    response: { class: { id: 3, name: '6A1', gradeLevelId: 6 }, semesterId: 2, from: '2026-09-01', to: '2026-09-30', validSessionCount: 20, summary: { presentCount: 612, excusedAbsenceCount: 18, unexcusedAbsenceCount: 7, lateCount: 21, earlyLeaveCount: 4 }, students: [{ studentId: 11, studentCode: 'HS001', fullName: 'Nguyễn Minh An', validSessionCount: 20, presentCount: 20, excusedAbsenceCount: 0, unexcusedAbsenceCount: 0, lateCount: 0, earlyLeaveCount: 0, attendanceRate: 1 }], page: 0, size: 20, totalElements: 1, totalPages: 1 },
  },
  parameters: { layout: 'padded' },
} satisfies Meta<typeof ClassAttendanceSummaryPanel>

export default meta
type Story = StoryObj<typeof meta>

export const ReadOnlySummary: Story = {}
export const Empty: Story = { args: { response: null } }
export const Forbidden: Story = { args: { forbidden: true } }
