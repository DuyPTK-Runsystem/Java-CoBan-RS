import type { Meta, StoryObj } from '@storybook/vue3'

import type { CapacityWarning } from '@/types/enrollment'
import CapacityWarningBanner from './CapacityWarningBanner.vue'

const meta = { title: 'AcademicCatalog/CapacityWarningBanner', component: CapacityWarningBanner, tags: ['autodocs'], parameters: { layout: 'padded' } } satisfies Meta<typeof CapacityWarningBanner>
export default meta
type Story = StoryObj<typeof meta>

export const ContractUnavailable: Story = { args: { available: false } }
export const NonBlockingWarning: Story = { args: { available: true, warningCount: 2, message: '2 lớp lệch sĩ số quá 20% so với trung bình khối.' } }
const warnings: CapacityWarning[] = [
  { classId: 101, academicYearId: 1, gradeLevelId: 1, activeStudentCount: 42, gradeAverage: 35, message: 'Sĩ số lớp vượt trung bình khối.' },
  { classId: 102, academicYearId: 1, gradeLevelId: 1, activeStudentCount: 18, gradeAverage: 35, message: 'Sĩ số lớp thấp hơn trung bình khối.' },
]
export const WarningsByClass: Story = { args: { available: true, warningCount: warnings.length, warnings, classes: [{ id: 101, academicYearId: 1, gradeLevelId: 1, classCode: '6A1', className: null, capacity: 35, status: 'ACTIVE' }, { id: 102, academicYearId: 1, gradeLevelId: 1, classCode: '6A2', className: null, capacity: 35, status: 'ACTIVE' }] } }
export const NoWarning: Story = { args: { available: true, warningCount: 0 } }
