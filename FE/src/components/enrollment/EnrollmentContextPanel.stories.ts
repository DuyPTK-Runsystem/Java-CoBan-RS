import type { Meta, StoryObj } from '@storybook/vue3'

import EnrollmentContextPanel from './EnrollmentContextPanel.vue'

const academicYears = [{ id: 1, code: '2026-2027', startDate: '2026-09-01', endDate: '2027-05-31', status: 'ACTIVE' as const, notes: null }]
const grades = [{ id: 1, code: 'GRADE_6', name: 'Khối 6', gradeLevel: 6 as const, displayOrder: 1, nextGradeId: 2, active: true, description: null }]
const classes = [{ id: 101, academicYearId: 1, gradeLevelId: 1, classCode: '6A1', className: 'Lớp chọn A', capacity: 35, status: 'ACTIVE' as const }, { id: 102, academicYearId: 1, gradeLevelId: 1, classCode: '6A2', className: null, capacity: 40, status: 'CLOSED' as const }]

const meta = { title: 'Enrollment/EnrollmentContextPanel', component: EnrollmentContextPanel, tags: ['autodocs'], args: { academicYears, grades, classes, academicYearId: 1, gradeId: 1, classId: 101 }, parameters: { layout: 'padded' } } satisfies Meta<typeof EnrollmentContextPanel>
export default meta
type Story = StoryObj<typeof meta>

export const ActiveClass: Story = {}
export const ClosedClassReadOnly: Story = { args: { classId: 102 } }
