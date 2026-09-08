import type { Meta, StoryObj } from '@storybook/vue3'

import type { GradeLevel, Semester, SubjectApplicability } from '@/types/academic'
import SubjectApplicabilityTable from './SubjectApplicabilityTable.vue'

const semesters: Semester[] = [{ id: 11, academicYearId: 1, code: 'HK1', name: 'Học kỳ 1', displayOrder: 1, startDate: '2026-09-01', endDate: '2026-12-31', automaticLockAt: null, status: 'ACTIVE', lockedAt: null, lockedBy: null, lockReason: null, reopenUntil: null }]
const grades: GradeLevel[] = [{ id: 1, code: 'GRADE_6', name: 'Khối 6', gradeLevel: 6, displayOrder: 1, nextGradeId: null, active: true, description: null }]
const applicabilities: SubjectApplicability[] = [{ id: 501, subjectId: 101, semesterId: 11, scopeType: 'GRADE', gradeLevelId: 1, classId: null, status: 'ACTIVE' }]

const meta = { title: 'AcademicCatalog/SubjectApplicabilityTable', component: SubjectApplicabilityTable, tags: ['autodocs'], args: { applicabilities, semesters, grades } } satisfies Meta<typeof SubjectApplicabilityTable>
export default meta
type Story = StoryObj<typeof meta>

export const ActiveConfiguration: Story = {}
export const Empty: Story = { args: { applicabilities: [] } }
