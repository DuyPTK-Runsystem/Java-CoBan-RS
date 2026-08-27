import type { Meta, StoryObj } from '@storybook/vue3'

import type { AcademicYear, GradeLevel, SchoolClass } from '@/types/academic'
import SchoolClassDialog from './SchoolClassDialog.vue'

const academicYears: AcademicYear[] = [{ id: 1, code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE', notes: null }]
const grades: GradeLevel[] = [{ id: 1, code: 'GRADE_6', name: 'Khối 6', gradeLevel: 6, displayOrder: 1, nextGradeId: null, active: true, description: null }]
const activeClass: SchoolClass = { id: 101, academicYearId: 1, gradeLevelId: 1, classCode: '6A1', className: 'Lớp chọn A', capacity: 35, status: 'ACTIVE' }
const closedClass: SchoolClass = { ...activeClass, id: 102, classCode: '6A9', status: 'CLOSED' }

const meta = { title: 'AcademicCatalog/SchoolClassDialog', component: SchoolClassDialog, tags: ['autodocs'], args: { visible: true, academicYears, grades }, parameters: { layout: 'fullscreen' } } satisfies Meta<typeof SchoolClassDialog>
export default meta
type Story = StoryObj<typeof meta>

export const Create: Story = {}
export const Edit: Story = { args: { mode: 'edit', initialValue: activeClass } }
export const ClosedReadOnly: Story = { args: { mode: 'edit', initialValue: closedClass } }
export const BackendConflict: Story = { args: { mode: 'edit', initialValue: activeClass, errorMessage: 'Mã lớp đã tồn tại trong năm học đã chọn.' } }
