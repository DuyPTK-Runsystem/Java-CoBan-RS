import type { Meta, StoryObj } from '@storybook/vue3'

import TransferEnrollmentDialog from './TransferEnrollmentDialog.vue'

const meta = { title: 'Enrollment/TransferEnrollmentDialog', component: TransferEnrollmentDialog, tags: ['autodocs'], args: { visible: true, currentClassId: 101, student: { studentId: 11, studentCode: 'HS011', studentName: 'Nguyễn An', enrollmentId: 701 }, targetClasses: [{ id: 101, academicYearId: 1, gradeLevelId: 1, classCode: '6A1', className: null, capacity: 35, status: 'ACTIVE' as const }, { id: 102, academicYearId: 1, gradeLevelId: 1, classCode: '6A2', className: null, capacity: 35, status: 'ACTIVE' as const }] }, parameters: { layout: 'padded' } } satisfies Meta<typeof TransferEnrollmentDialog>
export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {}
export const ApiError: Story = { args: { errorMessage: 'Không thể chuyển lớp do ràng buộc nghiệp vụ.' } }
