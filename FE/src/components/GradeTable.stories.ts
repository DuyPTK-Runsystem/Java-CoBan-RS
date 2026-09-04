import type { Meta, StoryObj } from '@storybook/vue3'

import type { GradeLevel } from '@/types/academic'
import GradeTable from './GradeTable.vue'

const grades: GradeLevel[] = [
  { id: 1, code: 'GRADE_6', name: 'Khối 6', gradeLevel: 6, displayOrder: 1, nextGradeId: 2, active: true, description: 'Đầu cấp THCS' },
  { id: 2, code: 'GRADE_7', name: 'Khối 7', gradeLevel: 7, displayOrder: 2, nextGradeId: 3, active: true, description: null },
  { id: 3, code: 'GRADE_8', name: 'Khối 8', gradeLevel: 8, displayOrder: 3, nextGradeId: 4, active: false, description: 'Đang rà soát metadata' },
  { id: 4, code: 'GRADE_9', name: 'Khối 9', gradeLevel: 9, displayOrder: 4, nextGradeId: null, active: true, description: null },
]

const gradeStatistics = {
  1: { gradeLevelId: 1, activeClassCount: 4, activeStudentCount: 142 },
  2: { gradeLevelId: 2, activeClassCount: 4, activeStudentCount: 138 },
  3: { gradeLevelId: 3, activeClassCount: 0, activeStudentCount: 0 },
  4: { gradeLevelId: 4, activeClassCount: 3, activeStudentCount: 105 },
}

const meta = {
  title: 'AcademicCatalog/GradeTable',
  component: GradeTable,
  tags: ['autodocs'],
  args: { grades, loading: false },
  parameters: { layout: 'padded' },
} satisfies Meta<typeof GradeTable>

export default meta
type Story = StoryObj<typeof meta>

export const LifecycleAndStatsUnavailable: Story = {}
export const WithStatistics: Story = { args: { gradeStatistics } }
export const Loading: Story = { args: { loading: true } }
export const Empty: Story = { args: { grades: [] } }
