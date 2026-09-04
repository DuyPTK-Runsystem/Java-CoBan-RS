import type { Meta, StoryObj } from '@storybook/vue3'

import type { GradeLevel, SchoolClass } from '@/types/academic'
import SchoolClassTable from './SchoolClassTable.vue'

const grades: GradeLevel[] = [
  { id: 1, code: 'GRADE_6', name: 'Khối 6', gradeLevel: 6, displayOrder: 1, nextGradeId: 2, active: true, description: null },
  { id: 4, code: 'GRADE_9', name: 'Khối 9', gradeLevel: 9, displayOrder: 4, nextGradeId: null, active: true, description: null },
]
const schoolClasses: SchoolClass[] = [
  { id: 101, academicYearId: 1, gradeLevelId: 1, classCode: '6A1', className: 'Lớp chọn A', capacity: 35, status: 'ACTIVE' },
  { id: 102, academicYearId: 1, gradeLevelId: 1, classCode: '6A2', className: null, capacity: 40, status: 'PLANNED' },
  { id: 109, academicYearId: 1, gradeLevelId: 4, classCode: '9A4', className: null, capacity: null, status: 'CLOSED' },
]

const classStatistics: Record<number, ClassStatistic> = {
  101: {
    classId: 101,
    classCode: '6A1',
    className: 'Lớp chọn A',
    gradeLevelId: 1,
    capacity: 35,
    activeStudentCount: 32,
    gradeAverage: 35.5,
    warning: null,
  },
  102: {
    classId: 102,
    classCode: '6A2',
    className: null,
    gradeLevelId: 1,
    capacity: 40,
    activeStudentCount: 44,
    gradeAverage: 35.5,
    warning: {
      classId: 102,
      academicYearId: 1,
      gradeLevelId: 1,
      activeStudentCount: 44,
      gradeAverage: 35.5,
      message: 'Lớp 6A2 lệch +24% so với trung bình khối (35.5 học sinh)',
    },
  },
  109: {
    classId: 109,
    classCode: '9A4',
    className: null,
    gradeLevelId: 4,
    capacity: null,
    activeStudentCount: 0,
    gradeAverage: null,
    warning: null,
  },
}

const meta = { title: 'AcademicCatalog/SchoolClassTable', component: SchoolClassTable, tags: ['autodocs'], args: { schoolClasses, grades }, parameters: { layout: 'padded' } } satisfies Meta<typeof SchoolClassTable>
export default meta
type Story = StoryObj<typeof meta>

export const StatusAndStatsUnavailable: Story = {}
export const WithStatisticsAndWarnings: Story = { args: { classStatistics } }
export const Loading: Story = { args: { loading: true } }
export const Empty: Story = { args: { schoolClasses: [] } }
