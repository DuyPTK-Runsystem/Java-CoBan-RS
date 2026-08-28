import type { Meta, StoryObj } from '@storybook/vue3'

import StudentEnrollmentHistoryDialog from './StudentEnrollmentHistoryDialog.vue'

const meta = { title: 'Enrollment/StudentEnrollmentHistoryDialog', component: StudentEnrollmentHistoryDialog, tags: ['autodocs'], args: { visible: true, studentCode: 'HS011', studentName: 'Nguyễn An', classes: [{ id: 101, academicYearId: 1, gradeLevelId: 1, classCode: '6A1', className: null, capacity: 35, status: 'ACTIVE' as const }, { id: 102, academicYearId: 1, gradeLevelId: 1, classCode: '6A2', className: null, capacity: 35, status: 'ACTIVE' as const }] }, parameters: { layout: 'padded' } } satisfies Meta<typeof StudentEnrollmentHistoryDialog>
export default meta
type Story = StoryObj<typeof meta>

export const Loading: Story = { args: { loading: true } }
export const Empty: Story = { args: { history: [] } }
export const ReadOnlyHistory: Story = { args: { history: [{ enrollment: { id: 9001, studentId: 11, studentCode: 'HS011', studentName: 'Nguyễn An', academicYearId: 1, currentClassId: 102, currentClassCode: '6A2', status: 'ACTIVE', enrolledAt: '2026-09-03T07:30:00', completedAt: null }, transfers: [{ transferId: 1, fromClassId: 101, toClassId: 102, effectiveAt: '2026-10-03T08:00:00', reason: 'Điều chỉnh sĩ số', approvedBy: 1 }] }] } }
