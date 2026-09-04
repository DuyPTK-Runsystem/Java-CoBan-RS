import type { Meta, StoryObj } from '@storybook/vue3'

import type { Semester } from '@/types/academic'
import SemesterTable from './SemesterTable.vue'

const semesters: Semester[] = [
  { id: 11, academicYearId: 1, code: 'HK1', name: 'Học kỳ I', displayOrder: 1, startDate: '2026-09-01', endDate: '2026-12-31', automaticLockAt: '2027-01-15T17:00:00', status: 'ACTIVE', lockedAt: null, lockedBy: null, lockReason: null, reopenUntil: null },
  { id: 12, academicYearId: 1, code: 'HK2', name: 'Học kỳ II', displayOrder: 2, startDate: '2027-01-01', endDate: '2027-05-31', automaticLockAt: '2027-06-15T17:00:00', status: 'DRAFT', lockedAt: null, lockedBy: null, lockReason: null, reopenUntil: null },
  { id: 13, academicYearId: 1, code: 'HK0', name: 'Học kỳ bổ sung', displayOrder: 3, startDate: '2026-06-01', endDate: '2026-08-15', automaticLockAt: null, status: 'LOCKED', lockedAt: '2026-08-20T09:30:00', lockedBy: 42, lockReason: 'Kết thúc kỳ bổ sung', reopenUntil: '2026-08-31T23:59:59' },
  { id: 14, academicYearId: 1, code: 'ARCHIVE', name: 'Học kỳ lịch sử', displayOrder: 4, startDate: '2025-09-01', endDate: '2025-12-31', automaticLockAt: null, status: 'CLOSED', lockedAt: '2026-01-10T09:00:00', lockedBy: 7, lockReason: 'Lưu trữ', reopenUntil: null },
]

const meta = {
  title: 'Academic/SemesterTable',
  component: SemesterTable,
  tags: ['autodocs'],
  args: { semesters, loading: false },
  parameters: { layout: 'padded' },
} satisfies Meta<typeof SemesterTable>

export default meta
type Story = StoryObj<typeof meta>

export const LifecycleMatrix: Story = {}
export const Loading: Story = { args: { loading: true } }
export const Empty: Story = { args: { semesters: [] } }
