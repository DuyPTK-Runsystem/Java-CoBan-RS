import type { Meta, StoryObj } from '@storybook/vue3'

import type { AcademicYear } from '@/types/academic'
import AcademicYearTable from './AcademicYearTable.vue'

const academicYears: AcademicYear[] = [
  { id: 1, code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE', notes: 'Năm học hiện tại' },
  { id: 2, code: '2025-2026', startDate: '2025-09-01', endDate: '2026-05-31', status: 'CLOSED', notes: 'Đã lưu trữ' },
  { id: 3, code: '2027-2028', startDate: '2027-09-01', endDate: '2028-05-31', status: 'DRAFT', notes: null },
]

const meta = {
  title: 'Academic/AcademicYearTable',
  component: AcademicYearTable,
  tags: ['autodocs'],
  args: { academicYears, loading: false },
  parameters: { layout: 'padded' },
} satisfies Meta<typeof AcademicYearTable>

export default meta
type Story = StoryObj<typeof meta>

export const LifecycleMatrix: Story = {}
export const Loading: Story = { args: { loading: true } }
export const Empty: Story = { args: { academicYears: [] } }
