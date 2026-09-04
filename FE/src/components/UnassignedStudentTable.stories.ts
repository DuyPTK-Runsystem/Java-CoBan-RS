import type { Meta, StoryObj } from '@storybook/vue3'

import UnassignedStudentTable from './UnassignedStudentTable.vue'

const students = [{ studentId: 11, studentCode: 'HS011', studentName: 'Nguyễn An' }, { studentId: 12, studentCode: 'HS012', studentName: 'Trần Bình' }]
const meta = { title: 'Enrollment/UnassignedStudentTable', component: UnassignedStudentTable, tags: ['autodocs'], args: { students }, parameters: { layout: 'padded' } } satisfies Meta<typeof UnassignedStudentTable>
export default meta
type Story = StoryObj<typeof meta>

export const Ready: Story = {}
export const Selected: Story = { args: { selectedStudents: [students[0]] } }
export const Empty: Story = { args: { students: [] } }
export const ClosedClassReadOnly: Story = { args: { readOnly: true } }
