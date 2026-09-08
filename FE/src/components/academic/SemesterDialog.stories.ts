import type { Meta, StoryObj } from '@storybook/vue3'

import type { AcademicYear } from '@/types/academic'
import SemesterDialog from './SemesterDialog.vue'

const academicYear: AcademicYear = { id: 1, code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE', notes: 'Năm học hiện tại' }

const meta = {
  title: 'Academic/SemesterDialog',
  component: SemesterDialog,
  tags: ['autodocs'],
  args: { visible: true, mode: 'create', academicYear, saving: false, errorMessage: '' },
  parameters: { layout: 'fullscreen' },
} satisfies Meta<typeof SemesterDialog>

export default meta
type Story = StoryObj<typeof meta>

export const Create: Story = {}
export const Edit: Story = {
  args: {
    mode: 'edit',
    initialValue: { id: 11, academicYearId: 1, code: 'HK1', name: 'Học kỳ I', displayOrder: 1, startDate: '2026-09-01', endDate: '2026-12-31', automaticLockAt: '2027-01-15T17:00:00', status: 'ACTIVE' },
  },
}
export const OverlapConflict: Story = {
  args: { errorMessage: 'Khoảng thời gian học kỳ bị chồng lấn với học kỳ hiện có trong năm học.' },
}
