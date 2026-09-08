import type { Meta, StoryObj } from '@storybook/vue3'

import ClassStudentTable from './ClassStudentTable.vue'

const students = [{ studentId: 11, studentCode: 'HS011', studentName: 'Nguyễn An', enrollmentId: 701 }, { studentId: 12, studentCode: 'HS012', studentName: 'Trần Bình', enrollmentId: 702 }]
const meta = { title: 'Enrollment/ClassStudentTable', component: ClassStudentTable, tags: ['autodocs'], args: { students }, parameters: { layout: 'padded' } } satisfies Meta<typeof ClassStudentTable>
export default meta
type Story = StoryObj<typeof meta>

export const ActiveClass: Story = {}
export const ClosedClassReadOnly: Story = { args: { readOnly: true } }
export const Empty: Story = { args: { students: [] } }
