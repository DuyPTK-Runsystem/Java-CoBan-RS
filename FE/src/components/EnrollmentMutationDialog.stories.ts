import type { Meta, StoryObj } from '@storybook/vue3'

import EnrollmentMutationDialog from './EnrollmentMutationDialog.vue'

const students = [{ studentId: 11, studentCode: 'HS011', studentName: 'Nguyễn An' }, { studentId: 12, studentCode: 'HS012', studentName: 'Trần Bình' }]
const meta = { title: 'Enrollment/EnrollmentMutationDialog', component: EnrollmentMutationDialog, tags: ['autodocs'], args: { visible: true, classLabel: '6A1', students }, parameters: { layout: 'padded' } } satisfies Meta<typeof EnrollmentMutationDialog>
export default meta
type Story = StoryObj<typeof meta>

export const Single: Story = { args: { mode: 'single', students: [students[0]] } }
export const Bulk: Story = { args: { mode: 'bulk' } }
export const ApiError: Story = { args: { mode: 'single', students: [students[0]], errorMessage: 'Lớp đã đạt giới hạn nghiệp vụ.' } }
